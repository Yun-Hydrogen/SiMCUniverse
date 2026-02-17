package com.simc.modules.shop;

import com.simc.SiMCUniverse;
import com.simc.modules.killscore.KillScoreModule;
import com.simc.modules.livescore.LiveScoreModule;
import com.simc.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class ShopModule {
    private final SiMCUniverse plugin;

    private boolean enabled;

    private File moduleFolder;
    private File configFile;
    private File pageFolder;

    private YamlConfiguration config;

    private boolean aliasShopEnabled;
    private String currencySource;

    private Map<String, String> messages = new HashMap<>();
    private final Map<Integer, ShopPage> pages = new LinkedHashMap<>();

    private ShopListener listener;
    private ShopCommand command;

    public ShopModule(SiMCUniverse plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        if (enabled) {
            return;
        }

        loadAll();
        registerCommand();
        registerListener();
        enabled = true;
        plugin.getLogger().info("Shop module initialized.");
    }

    public void shutdown() {
        if (!enabled) {
            return;
        }

        if (listener != null) {
            HandlerList.unregisterAll(listener);
            listener = null;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isShopInventory(player.getOpenInventory().getTopInventory())) {
                player.closeInventory();
            }
        }
        enabled = false;
        plugin.getLogger().info("Shop module shutdown completed.");
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

    public boolean isAliasShopEnabled() {
        return aliasShopEnabled;
    }

    public void openShop(Player player, int page) {
        if (!enabled) {
            player.sendMessage(getMessage("module-disabled"));
            return;
        }

        if (pages.isEmpty()) {
            player.sendMessage(getMessage("no-pages"));
            return;
        }

        int target = normalizePage(page);
        ShopPage shopPage = pages.get(target);
        if (shopPage == null) {
            player.sendMessage(getMessage("no-pages"));
            return;
        }

        String pageTitle = (shopPage.title == null || shopPage.title.isBlank()) ? "商店" : shopPage.title;
        String title = Utils.colorize(pageTitle + " &7[" + target + "/" + pages.size() + "]");
        ShopInventoryHolder holder = new ShopInventoryHolder(target);
        Inventory inventory = Bukkit.createInventory(holder, 36, title);
        holder.setInventory(inventory);

        for (int i = 27; i <= 35; i++) {
            inventory.setItem(i, createSimpleItem(Material.GRAY_STAINED_GLASS_PANE, " ", Collections.emptyList()));
        }

        for (Map.Entry<Integer, ShopProduct> entry : shopPage.products.entrySet()) {
            int slot = entry.getKey() - 1;
            if (slot < 0 || slot >= 27) {
                continue;
            }
            inventory.setItem(slot, buildDisplayItem(entry.getValue()));
        }

        if (target > 1) {
            inventory.setItem(30, createSimpleItem(Material.ARROW, "&a上一页", List.of("&7点击前往第 " + (target - 1) + " 页")));
        }
        if (target < pages.size()) {
            inventory.setItem(32, createSimpleItem(Material.ARROW, "&a下一页", List.of("&7点击前往第 " + (target + 1) + " 页")));
        }

        int balance = getCurrency(player.getUniqueId());
        inventory.setItem(31, createSimpleItem(Material.SUNFLOWER, "&e当前%shop_currency%: &f" + balance,
                List.of("&7货币来源: &f%shop_currency%")));
        inventory.setItem(34, createSimpleItem(Material.BARRIER, "&c关闭", List.of("&7点击关闭商店")));

        player.openInventory(inventory);
    }

    public void handleInventoryClick(Player player, int page, int slot) {
        if (!enabled) {
            return;
        }

        if (slot == 30 && page > 1) {
            openShop(player, page - 1);
            return;
        }
        if (slot == 32 && page < pages.size()) {
            openShop(player, page + 1);
            return;
        }
        if (slot == 34) {
            player.closeInventory();
            return;
        }

        if (slot < 0 || slot >= 27) {
            return;
        }

        ShopPage shopPage = pages.get(page);
        if (shopPage == null) {
            return;
        }

        ShopProduct product = shopPage.products.get(slot + 1);
        if (product == null) {
            return;
        }

        processPurchase(player, product);
        openShop(player, page);
    }

    public void processAliasCommand(Player player, String message) {
        String lower = message.toLowerCase(Locale.ROOT);
        if (!lower.startsWith("/shop")) {
            return;
        }
        String tail = message.length() > 5 ? message.substring(5).trim() : "";
        String commandLine = tail.isEmpty() ? "si-shop" : "si-shop " + tail;
        player.performCommand(commandLine);
    }

    public void resetShopPages() {
        if (!pageFolder.exists() && !pageFolder.mkdirs()) {
            plugin.getLogger().warning("Could not create shop page folder when resetting.");
            return;
        }

        File[] files = pageFolder.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                if (!file.delete()) {
                    plugin.getLogger().warning("Failed to delete page file: " + file.getName());
                }
            }
        }

        plugin.saveResource("shop/shop_page/Page1.yml", true);
        plugin.saveResource("shop/shop_page/Page2.yml", true);
        plugin.saveResource("shop/shop_page/Page3.yml", true);

        loadPages();
    }

    public boolean isShopInventory(Inventory inventory) {
        return inventory != null && inventory.getHolder() instanceof ShopInventoryHolder;
    }

    public int getOpenInventoryPage(Inventory inventory) {
        if (inventory != null && inventory.getHolder() instanceof ShopInventoryHolder) {
            return ((ShopInventoryHolder) inventory.getHolder()).page;
        }
        return 1;
    }

    private void processPurchase(Player player, ShopProduct product) {
        int balance = getCurrency(player.getUniqueId());
        if (balance < product.cost) {
            player.sendMessage(formatMessage("not-enough-currency", player, product.cost, balance));
            return;
        }

        setCurrency(player.getUniqueId(), balance - product.cost);

        if (product.exchangeType == ExchangeType.ITEM) {
            giveItems(player, product);
        } else {
            executeCommands(player, product);
        }

        int latest = getCurrency(player.getUniqueId());
        player.sendMessage(formatMessage("exchange-success", player, product.cost, latest));
    }

    private void giveItems(Player player, ShopProduct product) {
        for (Map.Entry<String, Integer> entry : product.itemRewards.entrySet()) {
            ItemStack stack = parseItemSpec(entry.getKey(), entry.getValue());
            if (stack == null || stack.getType() == Material.AIR) {
                continue;
            }

            Map<Integer, ItemStack> overflow = player.getInventory().addItem(stack);
            for (ItemStack remain : overflow.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), remain);
            }
        }
    }

    private void executeCommands(Player player, ShopProduct product) {
        for (String cmd : product.commandRewards) {
            String finalCmd = cmd.replace("%player%", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd);
        }
    }

    private int normalizePage(int page) {
        if (pages.isEmpty()) {
            return 1;
        }
        int max = pages.keySet().stream().max(Integer::compareTo).orElse(1);
        return Math.max(1, Math.min(page, max));
    }

    private void registerCommand() {
        if (plugin.getCommand("si-shop") == null) {
            plugin.getLogger().warning("Command si-shop is missing in plugin.yml");
            return;
        }

        if (command == null) {
            command = new ShopCommand(this);
        }

        plugin.getCommand("si-shop").setExecutor(command);
        plugin.getCommand("si-shop").setTabCompleter(command);
    }

    private void registerListener() {
        listener = new ShopListener(this);
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
    }

    private void loadAll() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Could not create plugin data folder.");
        }

        moduleFolder = new File(plugin.getDataFolder(), "shop");
        if (!moduleFolder.exists() && !moduleFolder.mkdirs()) {
            plugin.getLogger().warning("Could not create shop module folder.");
        }

        configFile = new File(moduleFolder, "config.yml");
        pageFolder = new File(moduleFolder, "shop_page");

        if (!configFile.exists()) {
            plugin.saveResource("shop/config.yml", false);
        }
        if (!pageFolder.exists() && !pageFolder.mkdirs()) {
            plugin.getLogger().warning("Could not create shop_page folder.");
        }

        File page1 = new File(pageFolder, "Page1.yml");
        if (!page1.exists()) {
            plugin.saveResource("shop/shop_page/Page1.yml", false);
            plugin.saveResource("shop/shop_page/Page2.yml", false);
            plugin.saveResource("shop/shop_page/Page3.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(configFile);
        aliasShopEnabled = config.getBoolean("register-shop-alias", false);
        currencySource = config.getString("currency-source", "killscore").toLowerCase(Locale.ROOT);

        messages.clear();
        messages.put("module-disabled", config.getString("messages.module-disabled", "&cShop 模块已禁用。"));
        messages.put("no-pages", config.getString("messages.no-pages", "&c商店页面未配置。"));
        messages.put("not-enough-currency", config.getString("messages.not-enough-currency", "&c你的%shop_currency%不足。需要 &e%cost% &c，当前 &e%balance%"));
        messages.put("exchange-success", config.getString("messages.exchange-success", "&a兑换成功！消耗 &e%cost% &a，当前%shop_currency%: &e%balance%"));
        messages.put("reload-success", config.getString("messages.reload-success", "&aShop 配置已重载。"));
        messages.put("reset-confirm", config.getString("messages.reset-confirm", "&e再次输入同样命令以确认重置商店页面（15秒内）。"));
        messages.put("reset-success", config.getString("messages.reset-success", "&a商店页面已重置。"));

        loadPages();
        saveConfigFile();
    }

    private void loadPages() {
        pages.clear();

        File[] files = pageFolder.listFiles((dir, name) -> name.matches("Page\\d+\\.yml"));
        if (files == null || files.length == 0) {
            return;
        }

        List<File> sortedFiles = new ArrayList<>(List.of(files));
        sortedFiles.sort(Comparator.comparingInt(this::extractPageNumber));

        int maxPage = sortedFiles.stream().map(this::extractPageNumber).max(Integer::compareTo).orElse(1);
        for (int i = 1; i <= maxPage; i++) {
            File pageFile = new File(pageFolder, "Page" + i + ".yml");
            if (!pageFile.exists()) {
                pages.put(i, new ShopPage(i));
                continue;
            }

            YamlConfiguration pageCfg = YamlConfiguration.loadConfiguration(pageFile);
            ShopPage page = new ShopPage(i);
            page.title = pageCfg.getString("title", "商店");
            for (int slot = 1; slot <= 27; slot++) {
                ConfigurationSection section = pageCfg.getConfigurationSection(String.valueOf(slot));
                if (section == null) {
                    continue;
                }

                ShopProduct product = parseProduct(section);
                if (product != null) {
                    page.products.put(slot, product);
                }
            }
            pages.put(i, page);
        }
    }

    private ShopProduct parseProduct(ConfigurationSection section) {
        String name = section.getString("name", "&f未命名商品");
        List<String> lore = section.getStringList("lore");

        String icon = section.getString("icon", "minecraft:stone");
        int iconAmount = clamp(section.getInt("icon-amount", 1), 1, 64);
        int cost = Math.max(1, section.getInt("cost", 1));

        String typeText = section.getString("exchange-type", "item").toLowerCase(Locale.ROOT);
        ExchangeType exchangeType = "command".equals(typeText) ? ExchangeType.COMMAND : ExchangeType.ITEM;

        Map<String, Integer> itemRewards = new LinkedHashMap<>();
        ConfigurationSection itemSection = section.getConfigurationSection("items");
        if (itemSection != null) {
            for (String key : itemSection.getKeys(false)) {
                Object raw = itemSection.get(key);
                Integer value = raw instanceof Number ? ((Number) raw).intValue() : parseInt(String.valueOf(raw));
                if (value != null && value > 0) {
                    itemRewards.put(key, value);
                }
            }
        } else {
            List<String> itemList = section.getStringList("items");
            for (String line : itemList) {
                int idx = line.lastIndexOf(":");
                if (idx <= 0 || idx >= line.length() - 1) {
                    continue;
                }
                String spec = line.substring(0, idx);
                Integer amount = parseInt(line.substring(idx + 1));
                if (amount != null && amount > 0) {
                    itemRewards.put(spec, amount);
                }
            }
        }

        List<String> commands = section.getStringList("commands");

        ShopProduct product = new ShopProduct();
        product.name = name;
        product.lore = lore;
        product.iconSpec = icon;
        product.iconAmount = iconAmount;
        product.cost = cost;
        product.exchangeType = exchangeType;
        product.itemRewards = itemRewards;
        product.commandRewards = commands;
        return product;
    }

    private ItemStack buildDisplayItem(ShopProduct product) {
        ItemStack icon = parseItemSpec(product.iconSpec, product.iconAmount);
        if (icon == null || icon.getType() == Material.AIR) {
            icon = new ItemStack(Material.BARRIER);
        }

        ItemMeta meta = icon.getItemMeta();
        if (meta == null) {
            return icon;
        }

        meta.setDisplayName(Utils.colorize(replacePlaceholders(product.name, product.cost, -1, null)));

        List<String> lore = new ArrayList<>();
        for (String line : product.lore) {
            lore.add(Utils.colorize(replacePlaceholders(line, product.cost, -1, null)));
        }
        lore.add(Utils.colorize(replacePlaceholders("&7消耗: &e" + product.cost + " %shop_currency%", product.cost, -1, null)));
        lore.add(Utils.colorize("&7类型: &f" + (product.exchangeType == ExchangeType.ITEM ? "item" : "command")));
        meta.setLore(lore);
        icon.setItemMeta(meta);
        return icon;
    }

    private ItemStack createSimpleItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Utils.colorize(replacePlaceholders(name, -1, -1, null)));
            List<String> lines = new ArrayList<>();
            for (String line : lore) {
                lines.add(Utils.colorize(replacePlaceholders(line, -1, -1, null)));
            }
            meta.setLore(lines);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack parseItemSpec(String spec, int amount) {
        if (spec == null || spec.isBlank()) {
            return null;
        }

        String trimmed = spec.trim();
        String baseId = trimmed;
        String nbt = null;

        int nbtStart = trimmed.indexOf('{');
        if (nbtStart > 0 && trimmed.endsWith("}")) {
            baseId = trimmed.substring(0, nbtStart);
            nbt = trimmed.substring(nbtStart);
        }

        Material material = Material.matchMaterial(baseId);
        if (material == null) {
            material = Material.STONE;
        }

        ItemStack item = new ItemStack(material, clamp(amount, 1, 64));
        if (nbt != null) {
            try {
                item = Bukkit.getUnsafe().modifyItemStack(item, nbt);
            } catch (Exception ignored) {
                // ignore invalid nbt
            }
        }

        return item;
    }

    public String getMessage(String key) {
        return Utils.colorize(replacePlaceholders(messages.getOrDefault(key, ""), -1, -1, null));
    }

    public String formatMessage(String key, Player player, int cost, int balance) {
        String raw = messages.getOrDefault(key, "");
        return Utils.colorize(replacePlaceholders(raw, cost, balance, player));
    }

    private String replacePlaceholders(String text, int cost, int balance, Player player) {
        String result = text == null ? "" : text;
        result = result.replace("%shop_currency%", getCurrencyDisplayName());
        if (cost >= 0) {
            result = result.replace("%cost%", String.valueOf(cost));
        }
        if (balance >= 0) {
            result = result.replace("%balance%", String.valueOf(balance));
        }
        if (player != null) {
            result = result.replace("%player%", player.getName());
        }
        return result;
    }

    private String getCurrencyDisplayName() {
        if ("livescore".equals(currencySource)) {
            return "生存分";
        }

        KillScoreModule kill = plugin.getPluginManagerInstance().getKillScoreModule();
        if (kill != null) {
            String name = kill.getKillScoreName();
            if (name != null && !name.isBlank()) {
                return name;
            }
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

    private int extractPageNumber(File file) {
        String name = file.getName();
        try {
            String number = name.replace("Page", "").replace(".yml", "");
            return Integer.parseInt(number);
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }

    private void saveConfigFile() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save shop config: " + e.getMessage());
        }
    }

    private Integer parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static class ShopPage {
        private final int pageIndex;
        private String title = "商店";
        private final Map<Integer, ShopProduct> products = new HashMap<>();

        private ShopPage(int pageIndex) {
            this.pageIndex = pageIndex;
        }
    }

    private enum ExchangeType {
        ITEM,
        COMMAND
    }

    private static class ShopProduct {
        private String name;
        private List<String> lore;
        private String iconSpec;
        private int iconAmount;
        private int cost;
        private ExchangeType exchangeType;
        private Map<String, Integer> itemRewards;
        private List<String> commandRewards;
    }

    public static class ShopInventoryHolder implements org.bukkit.inventory.InventoryHolder {
        private final int page;
        private Inventory inventory;

        public ShopInventoryHolder(int page) {
            this.page = page;
        }

        public void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
