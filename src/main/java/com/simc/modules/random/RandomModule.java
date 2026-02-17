package com.simc.modules.random;

import com.simc.SiMCUniverse;
import com.simc.modules.killscore.KillScoreModule;
import com.simc.modules.livescore.LiveScoreModule;
import com.simc.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Method;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class RandomModule {
    private final SiMCUniverse plugin;

    private boolean enabled;

    private File moduleFolder;
    private File configFile;
    private File dataFile;
    private File poolFolder;

    private YamlConfiguration config;
    private YamlConfiguration data;

    private String currencySource;
    private int singleCost;
    private int tenCost;
    private boolean tenGuaranteeGold;
    private int pityThreshold;
    private boolean customMusicEnabled;
    private String customMusicSoundId;
    private float customMusicVolume;
    private float customMusicPitch;
    private boolean customMusicRequirePackReady;
    private final Set<UUID> resourcePackReadyPlayers = new HashSet<>();

    private final Map<String, String> messages = new HashMap<>();
    private final Map<UUID, Integer> pityPoints = new HashMap<>();

    private final Map<Rarity, Double> rarityChance = new EnumMap<>(Rarity.class);
    private final Map<Rarity, List<RewardItem>> pools = new EnumMap<>(Rarity.class);

    private final Map<UUID, DrawRequest> pendingDraw = new HashMap<>();
    private final Map<UUID, TempSignState> signStates = new HashMap<>();
    private final Map<UUID, BukkitTask> animationTasks = new HashMap<>();

    private RandomCommand command;
    private RandomListener listener;

    public RandomModule(SiMCUniverse plugin) {
        this.plugin = plugin;
        for (Rarity rarity : Rarity.values()) {
            pools.put(rarity, new ArrayList<>());
            rarityChance.put(rarity, rarity.defaultChance);
        }
    }

    public void initialize() {
        if (enabled) {
            return;
        }
        loadAll();
        registerCommand();
        registerListener();
        enabled = true;
        plugin.getLogger().info("Random module initialized.");
    }

    public void shutdown() {
        if (!enabled) {
            return;
        }

        for (BukkitTask task : animationTasks.values()) {
            task.cancel();
        }
        animationTasks.clear();

        for (TempSignState state : signStates.values()) {
            state.restore();
        }
        signStates.clear();

        pendingDraw.clear();

        if (listener != null) {
            HandlerList.unregisterAll(listener);
            listener = null;
        }

        saveData();
        enabled = false;
        plugin.getLogger().info("Random module shutdown completed.");
    }

    public void reload() {
        loadAll();
    }

    public void enable() {
        initialize();
    }

    public void disable() {
        shutdown();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public SiMCUniverse getPlugin() {
        return plugin;
    }

    public String getMessage(String key) {
        return msg(key);
    }

    public String formatSetMessage(String playerName, int point) {
        String text = messages.getOrDefault("set-success", "&a已设置 %player% 的保底点数为 &e%point%");
        text = text.replace("%player%", playerName == null ? "unknown" : playerName);
        text = text.replace("%point%", String.valueOf(point));
        return Utils.colorize(replace(text, null, -1, -1, point));
    }

    public void openMainGui(Player player) {
        if (!enabled) {
            player.sendMessage(msg("module-disabled"));
            return;
        }

        Inventory inv = Bukkit.createInventory(new RandomHolder(GuiType.MAIN, null), 27, Utils.colorize("&8抽奖主界面"));
        fillBorder(inv, Material.GRAY_STAINED_GLASS_PANE);

        inv.setItem(11, createButton(Material.LIME_STAINED_GLASS_PANE,
                "&a单抽",
                List.of("&7花费: &e" + singleCost + " %shop_currency%", "&7点击进行单抽")));

        inv.setItem(15, createButton(Material.YELLOW_STAINED_GLASS_PANE,
                "&6十连抽",
                List.of("&7花费: &e" + tenCost + " %shop_currency%",
                        tenGuaranteeGold ? "&7第十抽必得 &6gold" : "&7无必得设置",
                        "&7点击进行十连")));

        inv.setItem(13, createButton(Material.BOOK,
                "&b概率查看",
                List.of("&7点击查看 blue/gold/rainbow 概率")));

        int point = getPityPoint(player.getUniqueId());
        inv.setItem(22, createButton(Material.NETHER_STAR,
                "&d保底点数",
                List.of("&7当前: &e" + point, "&7阈值: &e" + pityThreshold)));

        inv.setItem(24, createButton(Material.AMETHYST_SHARD,
                point >= pityThreshold ? "&5可领取保底" : "&8保底未达成",
                List.of(point >= pityThreshold ? "&7点击从 rainbow 池自选物品" : "&7达到阈值后可自选 rainbow 物品")));

        inv.setItem(26, createButton(Material.SUNFLOWER,
                "&e当前货币",
                List.of("&7%shop_currency%: &f" + getCurrency(player.getUniqueId()))));

        player.openInventory(inv);
    }

    public void playOpenGuiSound(Player player) {
        if (!customMusicEnabled) {
            return;
        }
        playCustomMusic(player);
    }

    public void openChanceGui(Player player) {
        Inventory inv = Bukkit.createInventory(new RandomHolder(GuiType.CHANCE, null), 27, Utils.colorize("&8概率查看"));
        fillBorder(inv, Material.BLACK_STAINED_GLASS_PANE);

        inv.setItem(11, chanceItem(Rarity.BLUE, Material.LIGHT_BLUE_STAINED_GLASS_PANE));
        inv.setItem(13, chanceItem(Rarity.GOLD, Material.YELLOW_STAINED_GLASS_PANE));
        inv.setItem(15, chanceItem(Rarity.RAINBOW, Material.PURPLE_STAINED_GLASS_PANE));
        inv.setItem(22, createButton(Material.ARROW, "&a返回", List.of("&7返回主界面")));

        player.openInventory(inv);
    }

    public void openPoolGui(Player player, Rarity rarity) {
        openPoolGui(player, rarity, 1);
    }

    public void openPoolGui(Player player, Rarity rarity, int page) {
        List<RewardItem> list = pools.getOrDefault(rarity, Collections.emptyList());
        int totalPages = Math.max(1, (list.size() + 44) / 45);
        int currentPage = Math.max(1, Math.min(page, totalPages));

        Inventory inv = Bukkit.createInventory(new RandomHolder(GuiType.POOL, rarity, currentPage), 54,
                Utils.colorize("&8" + rarity.display + " 池子预览 &7[" + currentPage + "/" + totalPages + "]"));

        fillBorder(inv, Material.GRAY_STAINED_GLASS_PANE);

        int from = (currentPage - 1) * 45;
        int to = Math.min(list.size(), from + 45);

        int slot = 0;
        for (int i = from; i < to; i++) {
            RewardItem rewardItem = list.get(i);
            inv.setItem(slot, rewardItem.toDisplayItem());
            slot++;
        }

        if (currentPage > 1) {
            inv.setItem(48, createButton(Material.ARROW, "&a上一页", List.of("&7第 " + (currentPage - 1) + " 页")));
        }
        inv.setItem(49, createButton(Material.ARROW, "&a返回", List.of("&7返回概率界面")));
        if (currentPage < totalPages) {
            inv.setItem(50, createButton(Material.ARROW, "&a下一页", List.of("&7第 " + (currentPage + 1) + " 页")));
        }

        player.openInventory(inv);
    }

    public void openPitySelectGui(Player player) {
        int point = getPityPoint(player.getUniqueId());
        if (point < pityThreshold) {
            player.sendMessage(msg("pity-not-enough"));
            return;
        }

        Inventory inv = Bukkit.createInventory(new RandomHolder(GuiType.PITY_SELECT, Rarity.RAINBOW), 54,
                Utils.colorize("&8保底自选 - rainbow"));
        fillBorder(inv, Material.PURPLE_STAINED_GLASS_PANE);

        List<RewardItem> list = pools.getOrDefault(Rarity.RAINBOW, Collections.emptyList());
        int slot = 0;
        for (RewardItem rewardItem : list) {
            if (slot >= 45) {
                break;
            }
            inv.setItem(slot, rewardItem.toDisplayItem());
            slot++;
        }
        inv.setItem(49, createButton(Material.BARRIER, "&c关闭", List.of("&7关闭界面")));
        player.openInventory(inv);
    }

    public void requestDraw(Player player, int count) {
        int cost = (count == 10) ? tenCost : singleCost;
        int current = getCurrency(player.getUniqueId());
        if (current < cost) {
            player.sendMessage(format("not-enough-currency", player, cost, current));
            return;
        }

        pendingDraw.put(player.getUniqueId(), new DrawRequest(count, cost));
        player.sendMessage(msg("sign-prompt"));
        openSignInput(player);
    }

    public void onSignSubmit(Player player) {
        DrawRequest request = pendingDraw.remove(player.getUniqueId());
        if (request == null) {
            return;
        }
        doDraw(player, request);
    }

    public void onChatSubmit(Player player) {
        onSignSubmit(player);
    }

    public boolean hasPendingDraw(UUID uuid) {
        return pendingDraw.containsKey(uuid);
    }

    public void handleGuiClick(Player player, Inventory inventory, int slot) {
        if (!(inventory.getHolder() instanceof RandomHolder)) {
            return;
        }

        RandomHolder holder = (RandomHolder) inventory.getHolder();
        switch (holder.guiType) {
            case MAIN:
                if (slot == 11) {
                    requestDraw(player, 1);
                    return;
                }
                if (slot == 15) {
                    requestDraw(player, 10);
                    return;
                }
                if (slot == 13) {
                    openChanceGui(player);
                    return;
                }
                if (slot == 24) {
                    openPitySelectGui(player);
                }
                return;
            case CHANCE:
                if (slot == 11) {
                    openPoolGui(player, Rarity.BLUE);
                    return;
                }
                if (slot == 13) {
                    openPoolGui(player, Rarity.GOLD);
                    return;
                }
                if (slot == 15) {
                    openPoolGui(player, Rarity.RAINBOW);
                    return;
                }
                if (slot == 22) {
                    openMainGui(player);
                }
                return;
            case POOL:
                if (slot == 48 && holder.page > 1) {
                    openPoolGui(player, holder.rarity, holder.page - 1);
                    return;
                }
                if (slot == 49) {
                    openChanceGui(player);
                    return;
                }
                int totalPages = Math.max(1, (pools.getOrDefault(holder.rarity, Collections.emptyList()).size() + 44) / 45);
                if (slot == 50 && holder.page < totalPages) {
                    openPoolGui(player, holder.rarity, holder.page + 1);
                }
                return;
            case PITY_SELECT:
                if (slot == 49) {
                    player.closeInventory();
                    return;
                }
                if (slot >= 0 && slot < 45) {
                    choosePityReward(player, slot);
                }
                return;
            case RESULT:
                if (slot == 22) {
                    openMainGui(player);
                }
                return;
            default:
        }
    }

    private void choosePityReward(Player player, int slot) {
        List<RewardItem> list = pools.getOrDefault(Rarity.RAINBOW, Collections.emptyList());
        if (slot >= list.size()) {
            return;
        }

        RewardItem reward = list.get(slot);
        giveReward(player, reward);
        setPityPoint(player.getUniqueId(), 0);
        player.sendMessage(msg("pity-redeem-success"));
        player.closeInventory();
    }

    public void clearPlayerTempState(UUID uuid) {
        pendingDraw.remove(uuid);
        TempSignState state = signStates.remove(uuid);
        if (state != null) {
            state.restore();
        }

        BukkitTask task = animationTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }

        resourcePackReadyPlayers.remove(uuid);
    }

    public void cancelAnimation(UUID uuid) {
        BukkitTask task = animationTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
    }

    private void doDraw(Player player, DrawRequest request) {
        int currency = getCurrency(player.getUniqueId());
        if (currency < request.cost) {
            player.sendMessage(format("not-enough-currency", player, request.cost, currency));
            return;
        }

        List<DrawResult> results = new ArrayList<>();
        for (int i = 0; i < request.count; i++) {
            if (request.count == 10 && tenGuaranteeGold && i == 9) {
                results.add(drawOneGuaranteedGold());
            } else {
                results.add(drawOne());
            }
        }

        setCurrency(player.getUniqueId(), currency - request.cost);

        addPity(player.getUniqueId(), request.count);

        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1f, 1f);
        openResultGui(player, results);
    }

    private DrawResult drawOne() {
        Rarity rarity = chooseRarity();
        RewardItem reward = randomRewardFrom(rarity);
        return new DrawResult(rarity, reward);
    }

    private DrawResult drawOneGuaranteedGold() {
        RewardItem reward = randomRewardFrom(Rarity.GOLD);
        return new DrawResult(Rarity.GOLD, reward);
    }

    private Rarity chooseRarity() {
        double r = ThreadLocalRandom.current().nextDouble();
        double current = 0;
        for (Rarity rarity : Rarity.values()) {
            current += rarityChance.getOrDefault(rarity, 0d);
            if (r <= current) {
                return rarity;
            }
        }
        return Rarity.BLUE;
    }

    private RewardItem randomRewardFrom(Rarity rarity) {
        List<RewardItem> list = pools.getOrDefault(rarity, Collections.emptyList());
        if (list.isEmpty()) {
            for (Rarity fallback : Rarity.values()) {
                if (!pools.getOrDefault(fallback, Collections.emptyList()).isEmpty()) {
                    List<RewardItem> fallbackList = pools.get(fallback);
                    return fallbackList.get(ThreadLocalRandom.current().nextInt(fallbackList.size()));
                }
            }
            return new RewardItem("minecraft:stone", 1);
        }
        return list.get(ThreadLocalRandom.current().nextInt(list.size()));
    }

    private void openResultGui(Player player, List<DrawResult> results) {
        cancelAnimation(player.getUniqueId());

        int size = 27;
        Inventory inv = Bukkit.createInventory(new RandomHolder(GuiType.RESULT, null), size, Utils.colorize("&8抽奖结果"));
        fillBorder(inv, Material.BLACK_STAINED_GLASS_PANE);

        List<Integer> slots = results.size() == 1
                ? List.of(13)
                : List.of(2, 3, 4, 5, 6, 11, 12, 13, 14, 15);

        for (int i = 0; i < results.size() && i < slots.size(); i++) {
            inv.setItem(slots.get(i), rarityPane(results.get(i).rarity));
        }

        player.openInventory(inv);

        boolean hasRainbow = results.stream().anyMatch(r -> r.rarity == Rarity.RAINBOW);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }

            boolean viewingResult = isResultInventory(player.getOpenInventory().getTopInventory());
            if (viewingResult) {
                for (int i = 0; i < results.size() && i < slots.size(); i++) {
                    inv.setItem(slots.get(i), results.get(i).reward.toDisplayItem());
                }
            }

            for (DrawResult result : results) {
                giveReward(player, result.reward);
            }

            if (hasRainbow) {
                playFireworkLaunchTriple(player);
            }

            if (viewingResult) {
                inv.setItem(22, createButton(Material.ARROW, "&a返回", List.of("&7返回抽奖主界面")));
            }

            if (viewingResult) {
                startBlinkAnimation(player, inv, results, slots);
            }
        }, 60L);
    }

    private void playFireworkLaunchTriple(Player player) {
        for (int i = 0; i < 3; i++) {
            long delay = i * 8L;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1f, 1f);
                }
            }, delay);
        }
    }

    private void startBlinkAnimation(Player player, Inventory inv, List<DrawResult> results, List<Integer> slots) {
        final int[] count = {0};
        final boolean[] showReward = {false};

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline() || !isResultInventory(player.getOpenInventory().getTopInventory())) {
                cancelAnimation(player.getUniqueId());
                return;
            }

            showReward[0] = !showReward[0];
            for (int i = 0; i < results.size() && i < slots.size(); i++) {
                inv.setItem(slots.get(i), showReward[0] ? results.get(i).reward.toDisplayItem() : rarityPane(results.get(i).rarity));
            }

            count[0]++;
            if (count[0] >= 6) {
                for (int i = 0; i < results.size() && i < slots.size(); i++) {
                    inv.setItem(slots.get(i), results.get(i).reward.toDisplayItem());
                }
                cancelAnimation(player.getUniqueId());
            }
        }, 20L, 20L);

        animationTasks.put(player.getUniqueId(), task);
    }

    public boolean isRandomInventory(Inventory inventory) {
        return inventory != null && inventory.getHolder() instanceof RandomHolder;
    }

    private boolean isResultInventory(Inventory inventory) {
        return inventory != null
                && inventory.getHolder() instanceof RandomHolder
                && ((RandomHolder) inventory.getHolder()).guiType == GuiType.RESULT;
    }

    private void openSignInput(Player player) {
        try {
            Block block = player.getLocation().getBlock().getRelative(0, 2, 0);
            TempSignState state = new TempSignState(block, block.getType(), block.getBlockData());
            block.setType(Material.OAK_SIGN, false);
            if (!(block.getState() instanceof Sign)) {
                state.restore();
                player.sendMessage(msg("sign-fallback"));
                return;
            }
            Sign sign = (Sign) block.getState();
            sign.setLine(0, "请签署");
            sign.setLine(1, "任意内容");
            sign.setLine(2, "直接确认也可");
            sign.update(true, false);
            signStates.put(player.getUniqueId(), state);
            try {
                Method method = player.getClass().getMethod("openSign", Sign.class);
                method.invoke(player, sign);
            } catch (Exception reflectionIgnored) {
                player.sendMessage(msg("sign-fallback"));
            }
        } catch (Throwable ex) {
            player.sendMessage(msg("sign-fallback"));
        }
    }

    public void handleSignComplete(Player player) {
        TempSignState state = signStates.remove(player.getUniqueId());
        if (state != null) {
            Bukkit.getScheduler().runTask(plugin, state::restore);
        }
        onSignSubmit(player);
    }

    private void registerCommand() {
        if (plugin.getCommand("si-random") == null) {
            plugin.getLogger().warning("Command si-random is missing in plugin.yml");
            return;
        }
        if (command == null) {
            command = new RandomCommand(this);
        }
        plugin.getCommand("si-random").setExecutor(command);
        plugin.getCommand("si-random").setTabCompleter(command);
    }

    private void registerListener() {
        listener = new RandomListener(this);
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
    }

    private void loadAll() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Could not create plugin data folder.");
        }

        moduleFolder = new File(plugin.getDataFolder(), "random");
        if (!moduleFolder.exists() && !moduleFolder.mkdirs()) {
            plugin.getLogger().warning("Could not create random module folder.");
        }

        poolFolder = new File(moduleFolder, "random_pool");
        if (!poolFolder.exists() && !poolFolder.mkdirs()) {
            plugin.getLogger().warning("Could not create random_pool folder.");
        }

        configFile = new File(moduleFolder, "config.yml");
        dataFile = new File(moduleFolder, "data.yml");

        if (!configFile.exists()) {
            plugin.saveResource("random/config.yml", false);
        }
        if (!dataFile.exists()) {
            data = new YamlConfiguration();
            data.set("pity", new HashMap<>());
            saveData();
        }

        ensurePoolFile("blue.yml");
        ensurePoolFile("gold.yml");
        ensurePoolFile("rainbow.yml");

        config = YamlConfiguration.loadConfiguration(configFile);
        data = YamlConfiguration.loadConfiguration(dataFile);

        currencySource = config.getString("currency-source", "killscore").toLowerCase(Locale.ROOT);
        singleCost = Math.max(1, config.getInt("draw-cost.single", 10));
        tenCost = Math.max(1, config.getInt("draw-cost.ten", 90));
        tenGuaranteeGold = config.getBoolean("draw.ten-guarantee-gold", true);
        pityThreshold = Math.max(1, config.getInt("pity.threshold", 100));
        customMusicEnabled = config.getBoolean("custom-music.enabled", false);
        customMusicSoundId = config.getString("custom-music.sound-id", "minecraft:entity.ender_dragon.death");
        customMusicVolume = (float) clamp(config.getDouble("custom-music.volume", 1.0), 0.0, 10.0);
        customMusicPitch = (float) clamp(config.getDouble("custom-music.pitch", 1.0), 0.5, 2.0);
        customMusicRequirePackReady = config.getBoolean("custom-music.require-pack-ready", true);

        loadMessages();
        loadPity();
        loadPools();

        saveConfig();
        saveData();
    }

    private void ensurePoolFile(String fileName) {
        File file = new File(poolFolder, fileName);
        if (!file.exists()) {
            plugin.saveResource("random/random_pool/" + fileName, false);
        }
    }

    private void loadMessages() {
        messages.clear();
        messages.put("module-disabled", config.getString("messages.module-disabled", "&cRandom 模块已禁用。"));
        messages.put("not-enough-currency", config.getString("messages.not-enough-currency", "&c你的%shop_currency%不足。需要 &e%cost%&c，当前 &e%balance%"));
        messages.put("sign-prompt", config.getString("messages.sign-prompt", "&e请在弹出的告示牌中签署确认抽奖（可直接确认）。"));
        messages.put("sign-fallback", config.getString("messages.sign-fallback", "&e告示牌输入不可用，直接在聊天输入任意内容即可确认抽奖。"));
        messages.put("reload-success", config.getString("messages.reload-success", "&aRandom 配置已重载。"));
        messages.put("reset-success", config.getString("messages.reset-success", "&a已重置所有玩家保底点数。"));
        messages.put("set-success", config.getString("messages.set-success", "&a已设置 %player% 的保底点数为 &e%point%"));
        messages.put("pity-not-enough", config.getString("messages.pity-not-enough", "&c保底点数不足。"));
        messages.put("pity-redeem-success", config.getString("messages.pity-redeem-success", "&d保底兑换成功，点数已清零。"));
    }

    private void loadPity() {
        pityPoints.clear();
        ConfigurationSection sec = data.getConfigurationSection("pity");
        if (sec == null) {
            return;
        }
        for (String key : sec.getKeys(false)) {
            try {
                pityPoints.put(UUID.fromString(key), sec.getInt(key, 0));
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("Invalid UUID in random pity data: " + key);
            }
        }
    }

    private void loadPools() {
        for (Rarity rarity : Rarity.values()) {
            pools.get(rarity).clear();
        }

        for (Rarity rarity : Rarity.values()) {
            File file = new File(poolFolder, rarity.fileName);
            YamlConfiguration poolCfg = YamlConfiguration.loadConfiguration(file);

            rarityChance.put(rarity, clamp(poolCfg.getDouble("chance", rarity.defaultChance), 0d, 1d));

            ConfigurationSection items = poolCfg.getConfigurationSection("items");
            if (items != null) {
                for (String key : items.getKeys(false)) {
                    Object raw = items.get(key);
                    Integer amount = raw instanceof Number ? ((Number) raw).intValue() : parseInt(String.valueOf(raw));
                    if (amount != null && amount > 0) {
                        pools.get(rarity).add(new RewardItem(key, amount));
                    }
                }
            }
        }

        normalizeChances();
    }

    private void normalizeChances() {
        double sum = 0;
        for (Rarity rarity : Rarity.values()) {
            sum += rarityChance.getOrDefault(rarity, 0d);
        }

        if (sum <= 0) {
            rarityChance.put(Rarity.BLUE, 0.8);
            rarityChance.put(Rarity.GOLD, 0.18);
            rarityChance.put(Rarity.RAINBOW, 0.02);
            return;
        }

        for (Rarity rarity : Rarity.values()) {
            rarityChance.put(rarity, rarityChance.get(rarity) / sum);
        }
    }

    private void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save random config: " + e.getMessage());
        }
    }

    private void playCustomMusic(Player player) {
        if (customMusicSoundId == null || customMusicSoundId.isBlank()) {
            return;
        }
        if (customMusicRequirePackReady && !resourcePackReadyPlayers.contains(player.getUniqueId())) {
            return;
        }
        try {
            player.playSound(player.getLocation(), customMusicSoundId, SoundCategory.MASTER, customMusicVolume, customMusicPitch);
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to play custom random music: " + customMusicSoundId + " - " + ex.getMessage());
        }
    }

    public void handleResourcePackStatus(Player player, String statusName) {
        if (player == null || statusName == null) {
            return;
        }

        switch (statusName) {
            case "SUCCESSFULLY_LOADED":
                resourcePackReadyPlayers.add(player.getUniqueId());
                if (isRandomInventory(player.getOpenInventory().getTopInventory())) {
                    playCustomMusic(player);
                }
                break;
            case "DECLINED":
            case "FAILED_DOWNLOAD":
            case "FAILED_RELOAD":
                resourcePackReadyPlayers.remove(player.getUniqueId());
                break;
            default:
                // ACCEPTED / DOWNLOADED 等状态不处理
        }
    }

    public void stopCustomMusic(Player player) {
        if (player == null || customMusicSoundId == null || customMusicSoundId.isBlank()) {
            return;
        }
        try {
            player.stopSound(customMusicSoundId, SoundCategory.MASTER);
        } catch (Exception ignored) {
            // ignore
        }
    }

    private void saveData() {
        if (data == null || dataFile == null) {
            return;
        }

        data.set("pity", null);
        for (Map.Entry<UUID, Integer> entry : pityPoints.entrySet()) {
            data.set("pity." + entry.getKey(), entry.getValue());
        }

        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save random data: " + e.getMessage());
        }
    }

    public void resetAllPity() {
        pityPoints.clear();
        saveData();
    }

    public void setPityPoint(UUID uuid, int value) {
        pityPoints.put(uuid, Math.max(0, value));
        saveData();
    }

    public int getPityPoint(UUID uuid) {
        return pityPoints.getOrDefault(uuid, 0);
    }

    private void addPity(UUID uuid, int add) {
        pityPoints.put(uuid, getPityPoint(uuid) + add);
        saveData();
    }

    private ItemStack chanceItem(Rarity rarity, Material material) {
        double chance = rarityChance.getOrDefault(rarity, 0d) * 100.0;
        return createButton(material,
                rarity.display,
                List.of("&7概率: &e" + String.format(Locale.US, "%.2f", chance) + "%", "&7点击查看该等级池"));
    }

    private ItemStack rarityPane(Rarity rarity) {
        Material mat = rarity == Rarity.BLUE ? Material.LIGHT_BLUE_STAINED_GLASS_PANE
                : rarity == Rarity.GOLD ? Material.YELLOW_STAINED_GLASS_PANE
                : Material.PURPLE_STAINED_GLASS_PANE;
        return createButton(mat, rarity.display, List.of("&7结果揭晓中..."));
    }

    private void fillBorder(Inventory inv, Material mat) {
        for (int i = 0; i < inv.getSize(); i++) {
            if (i < 9 || i >= inv.getSize() - 9 || i % 9 == 0 || i % 9 == 8) {
                inv.setItem(i, createButton(mat, " ", Collections.emptyList()));
            }
        }
    }

    private ItemStack createButton(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Utils.colorize(replace(name, null, -1, -1, -1)));
            List<String> lines = new ArrayList<>();
            for (String l : lore) {
                lines.add(Utils.colorize(replace(l, null, -1, -1, -1)));
            }
            meta.setLore(lines);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void giveReward(Player player, RewardItem reward) {
        ItemStack stack = parseItemSpec(reward.itemSpec, reward.amount);
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(stack);
        for (ItemStack value : overflow.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), value);
        }
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.6f);
    }

    private ItemStack parseItemSpec(String spec, int amount) {
        String trimmed = spec.trim();
        String base = trimmed;
        String nbt = null;

        int idx = trimmed.indexOf('{');
        if (idx > 0 && trimmed.endsWith("}")) {
            base = trimmed.substring(0, idx);
            nbt = trimmed.substring(idx);
        }

        Material mat = Material.matchMaterial(base);
        if (mat == null) {
            mat = Material.STONE;
        }

        ItemStack item = new ItemStack(mat, Math.max(1, Math.min(64, amount)));
        if (nbt != null) {
            try {
                item = Bukkit.getUnsafe().modifyItemStack(item, nbt);
            } catch (Exception ignored) {
            }
        }
        return item;
    }

    private String msg(String key) {
        return Utils.colorize(replace(messages.getOrDefault(key, ""), null, -1, -1, -1));
    }

    public String format(String key, Player player, int cost, int balance) {
        return Utils.colorize(replace(messages.getOrDefault(key, ""), player, cost, balance, getPityPoint(player.getUniqueId())));
    }

    private String replace(String text, Player player, int cost, int balance, int point) {
        String result = text == null ? "" : text;
        result = result.replace("%shop_currency%", currencyDisplayName());
        if (player != null) {
            result = result.replace("%player%", player.getName());
        }
        if (cost >= 0) {
            result = result.replace("%cost%", String.valueOf(cost));
        }
        if (balance >= 0) {
            result = result.replace("%balance%", String.valueOf(balance));
        }
        if (point >= 0) {
            result = result.replace("%point%", String.valueOf(point));
        }
        return result;
    }

    private String currencyDisplayName() {
        if ("livescore".equals(currencySource)) {
            return "生存分";
        }
        KillScoreModule kill = plugin.getPluginManagerInstance().getKillScoreModule();
        if (kill != null && kill.getKillScoreName() != null && !kill.getKillScoreName().isBlank()) {
            return kill.getKillScoreName();
        }
        return "击杀分";
    }

    private int getCurrency(UUID uuid) {
        if ("livescore".equals(currencySource)) {
            LiveScoreModule live = plugin.getPluginManagerInstance().getLiveScoreModule();
            return live == null ? 0 : live.getScore(uuid);
        }
        KillScoreModule kill = plugin.getPluginManagerInstance().getKillScoreModule();
        return kill == null ? 0 : kill.getScore(uuid);
    }

    private void setCurrency(UUID uuid, int value) {
        if ("livescore".equals(currencySource)) {
            LiveScoreModule live = plugin.getPluginManagerInstance().getLiveScoreModule();
            if (live != null) {
                live.setScore(uuid, value);
            }
            return;
        }

        KillScoreModule kill = plugin.getPluginManagerInstance().getKillScoreModule();
        if (kill != null) {
            kill.setScore(uuid, value);
        }
    }

    private Integer parseInt(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private enum Rarity {
        BLUE("blue.yml", "&bBLUE", 0.8),
        GOLD("gold.yml", "&6GOLD", 0.18),
        RAINBOW("rainbow.yml", "&dRAINBOW", 0.02);

        private final String fileName;
        private final String display;
        private final double defaultChance;

        Rarity(String fileName, String display, double defaultChance) {
            this.fileName = fileName;
            this.display = display;
            this.defaultChance = defaultChance;
        }
    }

    public static class RandomHolder implements InventoryHolder {
        private final GuiType guiType;
        private final Rarity rarity;
        private final int page;

        public RandomHolder(GuiType guiType, Rarity rarity) {
            this(guiType, rarity, 1);
        }

        public RandomHolder(GuiType guiType, Rarity rarity, int page) {
            this.guiType = guiType;
            this.rarity = rarity;
            this.page = page;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    public enum GuiType {
        MAIN,
        CHANCE,
        POOL,
        RESULT,
        PITY_SELECT
    }

    private static class RewardItem {
        private final String itemSpec;
        private final int amount;

        private RewardItem(String itemSpec, int amount) {
            this.itemSpec = itemSpec;
            this.amount = amount;
        }

        private ItemStack toDisplayItem() {
            Material material = Material.matchMaterial(itemSpec.contains("{") ? itemSpec.substring(0, itemSpec.indexOf('{')) : itemSpec);
            if (material == null) {
                material = Material.STONE;
            }
            return new ItemStack(material, Math.max(1, Math.min(64, amount)));
        }
    }

    private static class DrawResult {
        private final Rarity rarity;
        private final RewardItem reward;

        private DrawResult(Rarity rarity, RewardItem reward) {
            this.rarity = rarity;
            this.reward = reward;
        }
    }

    private static class DrawRequest {
        private final int count;
        private final int cost;

        private DrawRequest(int count, int cost) {
            this.count = count;
            this.cost = cost;
        }
    }

    private static class TempSignState {
        private final Block block;
        private final Material oldType;
        private final org.bukkit.block.data.BlockData oldData;

        private TempSignState(Block block, Material oldType, org.bukkit.block.data.BlockData oldData) {
            this.block = block;
            this.oldType = oldType;
            this.oldData = oldData;
        }

        private void restore() {
            block.setType(oldType, false);
            if (oldData != null) {
                block.setBlockData(oldData, false);
            }
        }
    }
}
