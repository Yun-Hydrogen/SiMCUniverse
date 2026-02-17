package com.simc.modules.checkin;

import com.simc.SiMCUniverse;
import com.simc.utils.Utils;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class CheckinModule {
    private final SiMCUniverse plugin;

    private boolean enabled;

    private File moduleFolder;
    private File configFile;
    private File dataFile;
    private File confFolder;

    private YamlConfiguration config;
    private YamlConfiguration data;

    private final Map<String, String> messages = new HashMap<>();
    private final Map<String, CheckinTask> tasks = new LinkedHashMap<>();
    private final Map<UUID, Map<String, TaskProgress>> playerProgress = new HashMap<>();
    private final Map<String, LockOverride> lockOverrides = new HashMap<>();

    private CheckinCommand command;
    private CheckinListener listener;

    public CheckinModule(SiMCUniverse plugin) {
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
        plugin.getLogger().info("Checkin module initialized.");
    }

    public void shutdown() {
        if (!enabled) {
            return;
        }

        if (listener != null) {
            HandlerList.unregisterAll(listener);
            listener = null;
        }

        saveData();
        enabled = false;
        plugin.getLogger().info("Checkin module shutdown completed.");
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

    public void openMainGui(Player player, int page) {
        if (!enabled) {
            player.sendMessage(msg("module-disabled"));
            return;
        }

        List<CheckinTask> list = new ArrayList<>(tasks.values());
        int totalPages = Math.max(1, (list.size() + 44) / 45);
        int currentPage = Math.max(1, Math.min(page, totalPages));

        Inventory inv = Bukkit.createInventory(new CheckinHolder(currentPage), 54,
                Utils.colorize("&8签到任务 &7[" + currentPage + "/" + totalPages + "]"));

        fillBottomBar(inv);

        int from = (currentPage - 1) * 45;
        int to = Math.min(list.size(), from + 45);
        int slot = 0;

        for (int i = from; i < to; i++) {
            CheckinTask task = list.get(i);
            inv.setItem(slot, buildTaskItem(player, task));
            slot++;
        }

        if (currentPage > 1) {
            inv.setItem(48, createSimpleItem(Material.ARROW, "&a上一页", List.of("&7第 " + (currentPage - 1) + " 页")));
        }
        inv.setItem(49, createSimpleItem(Material.BARRIER, "&c关闭", List.of("&7点击关闭")));
        if (currentPage < totalPages) {
            inv.setItem(50, createSimpleItem(Material.ARROW, "&a下一页", List.of("&7第 " + (currentPage + 1) + " 页")));
        }

        player.openInventory(inv);
    }

    public void handleGuiClick(Player player, Inventory inv, int slot) {
        if (!(inv.getHolder() instanceof CheckinHolder)) {
            return;
        }

        CheckinHolder holder = (CheckinHolder) inv.getHolder();

        if (slot == 48) {
            openMainGui(player, holder.page - 1);
            return;
        }
        if (slot == 49) {
            player.closeInventory();
            return;
        }
        if (slot == 50) {
            openMainGui(player, holder.page + 1);
            return;
        }

        if (slot < 0 || slot >= 45) {
            return;
        }

        List<CheckinTask> list = new ArrayList<>(tasks.values());
        int index = (holder.page - 1) * 45 + slot;
        if (index < 0 || index >= list.size()) {
            return;
        }

        CheckinTask task = list.get(index);
        attemptCheckin(player, task);
        openMainGui(player, holder.page);
    }

    private void attemptCheckin(Player player, CheckinTask task) {
        if (task == null) {
            return;
        }

        if (isTaskLocked(player, task)) {
            player.sendMessage(format("task-locked", player, task, null));
            return;
        }

        TaskProgress progress = getProgress(player.getUniqueId(), task.id);
        String currentDayKey = getCurrentDayKey(task.resetTime);

        if (currentDayKey.equals(progress.lastDayKey)) {
            player.sendMessage(format("already-checkin", player, task, progress));
            return;
        }

        if (task.continuous && progress.progress > 0 && progress.lastDayKey != null) {
            String expectedPrev = getPreviousDayKey(task.resetTime);
            if (!expectedPrev.equals(progress.lastDayKey)) {
                progress.progress = 0;
            }
        }

        if (progress.progress >= task.totalDays) {
            if (task.loop) {
                progress.progress = 0;
            } else {
                player.sendMessage(format("task-completed", player, task, progress));
                return;
            }
        }

        int day = progress.progress + 1;
        List<String> rewards = task.dayCommands.getOrDefault(day, Collections.emptyList());
        for (String cmd : rewards) {
            String finalCmd = cmd.replace("%player%", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd);
        }

        progress.progress = day;
        progress.lastDayKey = currentDayKey;
        saveData();

        player.sendMessage(format("checkin-success", player, task, progress));
        sendToast(player, task, progress.progress, task.totalDays);
    }

    private boolean isTaskLocked(Player player, CheckinTask task) {
        LockOverride override = lockOverrides.getOrDefault(task.id, LockOverride.DEFAULT);
        if (override == LockOverride.LOCKED) {
            return true;
        }
        if (override == LockOverride.UNLOCKED) {
            return false;
        }

        if (task.unlockType == UnlockType.ALWAYS_LOCKED) {
            return true;
        }
        if (task.unlockType == UnlockType.NONE) {
            return false;
        }

        if (task.unlockType == UnlockType.AFTER_TASK) {
            if (task.unlockTaskId == null || task.unlockTaskId.isBlank()) {
                return true;
            }
            CheckinTask need = tasks.get(task.unlockTaskId);
            if (need == null) {
                return true;
            }
            TaskProgress dep = getProgress(player.getUniqueId(), need.id);
            return dep.progress < need.totalDays;
        }

        return false;
    }

    private ItemStack buildTaskItem(Player player, CheckinTask task) {
        Material icon = Material.matchMaterial(task.icon);
        if (icon == null) {
            icon = Material.CHEST;
        }

        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        TaskProgress progress = getProgress(player.getUniqueId(), task.id);
        boolean locked = isTaskLocked(player, task);

        meta.setDisplayName(Utils.colorize(task.title));
        List<String> lore = new ArrayList<>();
        for (String line : task.lore) {
            lore.add(Utils.colorize(line));
        }
        lore.add(Utils.colorize("&7ID: &f" + task.id));
        lore.add(Utils.colorize("&7进度: &e" + Math.min(progress.progress, task.totalDays) + "&7/&e" + task.totalDays));
        lore.add(Utils.colorize("&7重置时间: &f" + task.resetTime));
        lore.add(Utils.colorize(locked ? "&c状态: 未解锁" : "&a状态: 可签到"));
        meta.setLore(lore);

        if (locked) {
            meta.addEnchant(Enchantment.DURABILITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        item.setItemMeta(meta);
        return item;
    }

    private void fillBottomBar(Inventory inv) {
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, createSimpleItem(Material.GRAY_STAINED_GLASS_PANE, " ", Collections.emptyList()));
        }
    }

    private ItemStack createSimpleItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Utils.colorize(name));
            List<String> lines = new ArrayList<>();
            for (String line : lore) {
                lines.add(Utils.colorize(line));
            }
            meta.setLore(lines);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void sendToast(Player player, CheckinTask task, int current, int total) {
        playToastSound(player);

        String toastTitle = "今日已签到！";
        String toastContent = "已签到" + stripColor(task.title) + " " + current + "/" + total;
        String iconMaterial = task.icon;

        String json = buildToastAdvancementJson(toastTitle, toastContent, iconMaterial);
        NamespacedKey key = new NamespacedKey(plugin, "checkin_toast_" + player.getUniqueId().toString().replace("-", ""));

        try {
            Bukkit.getUnsafe().loadAdvancement(key, json);
            Advancement advancement = Bukkit.getAdvancement(key);
            if (advancement != null) {
                AdvancementProgress progress = player.getAdvancementProgress(advancement);
                if (!progress.isDone()) {
                    for (String criteria : progress.getRemainingCriteria()) {
                        progress.awardCriteria(criteria);
                    }
                }

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    try {
                        Bukkit.getUnsafe().removeAdvancement(key);
                    } catch (Exception ignored) {}
                }, 20L);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to send toast notification: " + e.getMessage());
        }
    }

    private String buildToastAdvancementJson(String title, String description, String iconMaterial) {
        String mat = iconMaterial == null || iconMaterial.isBlank() ? "minecraft:emerald" : iconMaterial;
        if (!mat.contains(":")) {
            mat = "minecraft:" + mat;
        }

        String escapedTitle = escapeJson(title);
        String escapedDesc = escapeJson(description);

        return "{" +
                "\"criteria\":{\"trigger\":{\"trigger\":\"minecraft:impossible\"}}," +
                "\"display\":{" +
                "\"icon\":{\"item\":\"" + mat + "\"}," +
                "\"title\":{\"text\":\"" + escapedTitle + "\"}," +
                "\"description\":{\"text\":\"" + escapedDesc + "\"}," +
                "\"frame\":\"task\"," +
                "\"show_toast\":true," +
                "\"announce_to_chat\":false," +
                "\"hidden\":true" +
                "}" +
                "}";
    }

    private String escapeJson(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String stripColor(String s) {
        return s == null ? "" : s.replaceAll("§[0-9A-FK-ORa-fk-or]", "");
    }

    private String iconSymbol(String raw) {
        if (raw == null || raw.isBlank()) {
            return "✦";
        }

        String value = raw.toLowerCase(Locale.ROOT).trim();
        Material material = Material.matchMaterial(value);
        if (material == null && !value.contains(":")) {
            material = Material.matchMaterial("minecraft:" + value);
        }

        if (material == null) {
            return "✦";
        }

        if (material == Material.EMERALD || material == Material.EMERALD_BLOCK) {
            return "◆";
        }
        if (material == Material.DIAMOND || material == Material.DIAMOND_BLOCK) {
            return "✦";
        }
        if (material == Material.GOLD_INGOT || material == Material.GOLD_BLOCK) {
            return "✧";
        }
        return "⬢";
    }

    private void playToastSound(Player player) {
        try {
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_IN, 1f, 1f);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
                }
            }, 6L);
        } catch (Exception ignored) {
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        }
    }

    public void resetTaskAllPlayers(String taskId) {
        String key = normalize(taskId);
        for (Map<String, TaskProgress> map : playerProgress.values()) {
            map.remove(key);
        }
        saveData();
    }

    public boolean setPlayerTaskProgress(UUID uuid, String taskId, int progressValue) {
        CheckinTask task = tasks.get(normalize(taskId));
        if (task == null) {
            return false;
        }

        int value = Math.max(0, Math.min(progressValue, task.totalDays));
        TaskProgress p = getProgress(uuid, task.id);
        p.progress = value;
        p.lastDayKey = null;
        saveData();
        return true;
    }

    public boolean setTaskLock(String taskId, LockOverride override) {
        String key = normalize(taskId);
        if (!tasks.containsKey(key)) {
            return false;
        }
        lockOverrides.put(key, override);
        saveData();
        return true;
    }

    public List<String> getTaskIds() {
        return new ArrayList<>(tasks.keySet());
    }

    private void registerCommand() {
        if (plugin.getCommand("si-checkin") == null) {
            plugin.getLogger().warning("Command si-checkin is missing in plugin.yml");
            return;
        }

        if (command == null) {
            command = new CheckinCommand(this);
        }
        plugin.getCommand("si-checkin").setExecutor(command);
        plugin.getCommand("si-checkin").setTabCompleter(command);
    }

    private void registerListener() {
        listener = new CheckinListener(this);
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
    }

    private void loadAll() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Could not create plugin data folder.");
        }

        moduleFolder = new File(plugin.getDataFolder(), "checkin");
        if (!moduleFolder.exists() && !moduleFolder.mkdirs()) {
            plugin.getLogger().warning("Could not create checkin module folder.");
        }

        confFolder = new File(moduleFolder, "checkin_conf");
        if (!confFolder.exists() && !confFolder.mkdirs()) {
            plugin.getLogger().warning("Could not create checkin_conf folder.");
        }

        configFile = new File(moduleFolder, "config.yml");
        dataFile = new File(moduleFolder, "data.yml");

        if (!configFile.exists()) {
            plugin.saveResource("checkin/config.yml", false);
        }
        if (!dataFile.exists()) {
            data = new YamlConfiguration();
            data.set("players", null);
            data.set("lock-overrides", null);
            saveData();
        }

        File sample = new File(confFolder, "daily_login.yml");
        if (!sample.exists()) {
            plugin.saveResource("checkin/checkin_conf/daily_login.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(configFile);
        data = YamlConfiguration.loadConfiguration(dataFile);

        loadMessages();
        loadTasks();
        loadData();

        saveConfig();
        saveData();
    }

    private void loadMessages() {
        messages.clear();
        messages.put("module-disabled", config.getString("messages.module-disabled", "&cCheckin 模块已禁用。"));
        messages.put("task-locked", config.getString("messages.task-locked", "&c该签到任务尚未解锁。"));
        messages.put("already-checkin", config.getString("messages.already-checkin", "&e今天已经签过到了。"));
        messages.put("checkin-success", config.getString("messages.checkin-success", "&a签到成功：%task_title% &7| 进度 %progress%/%total%"));
        messages.put("task-completed", config.getString("messages.task-completed", "&e该签到任务已完成。"));
        messages.put("reload-success", config.getString("messages.reload-success", "&aCheckin 配置已重载。"));
        messages.put("reset-success", config.getString("messages.reset-success", "&a已重置任务 %task_id% 的全服进度。"));
        messages.put("set-success", config.getString("messages.set-success", "&a已设置玩家 %player% 在任务 %task_id% 的进度为 %progress%。"));
        messages.put("lock-success", config.getString("messages.lock-success", "&e已始终锁定任务 %task_id%。"));
        messages.put("unlock-success", config.getString("messages.unlock-success", "&a已始终解锁任务 %task_id%。"));
    }

    private void loadTasks() {
        tasks.clear();

        File[] files = confFolder.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files == null) {
            return;
        }

        List<File> sorted = new ArrayList<>(List.of(files));
        sorted.sort(Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));

        for (File file : sorted) {
            YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
            String id = normalize(yml.getString("id", ""));
            if (!id.matches("[a-z0-9_-]+")) {
                plugin.getLogger().warning("Skip checkin task with invalid id in " + file.getName());
                continue;
            }

            CheckinTask task = new CheckinTask();
            task.id = id;
            task.title = yml.getString("title", "&f未命名签到");
            task.icon = yml.getString("icon", "minecraft:emerald");
            task.lore = yml.getStringList("lore");
            task.totalDays = Math.max(1, yml.getInt("days", 7));

            String reset = yml.getString("reset-time", "00:00:00");
            try {
                task.resetTime = LocalTime.parse(reset);
            } catch (Exception e) {
                task.resetTime = LocalTime.MIDNIGHT;
            }

            String unlock = yml.getString("unlock.type", "none").toLowerCase(Locale.ROOT);
            task.unlockType = "after_task".equals(unlock) ? UnlockType.AFTER_TASK
                    : "always_locked".equals(unlock) ? UnlockType.ALWAYS_LOCKED
                    : UnlockType.NONE;
            task.unlockTaskId = normalize(yml.getString("unlock.task-id", ""));

            task.loop = yml.getBoolean("repeat.loop", false);
            task.continuous = yml.getBoolean("repeat.continuous", false);

            task.dayCommands.clear();
            ConfigurationSection rewards = yml.getConfigurationSection("rewards");
            if (rewards != null) {
                for (String dayKey : rewards.getKeys(false)) {
                    int day;
                    try {
                        day = Integer.parseInt(dayKey);
                    } catch (NumberFormatException e) {
                        continue;
                    }
                    if (day <= 0) {
                        continue;
                    }
                    task.dayCommands.put(day, rewards.getStringList(dayKey));
                }
            }

            tasks.put(task.id, task);
        }
    }

    private void loadData() {
        playerProgress.clear();
        lockOverrides.clear();

        ConfigurationSection players = data.getConfigurationSection("players");
        if (players != null) {
            for (String uuidText : players.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidText);
                    ConfigurationSection taskSec = players.getConfigurationSection(uuidText);
                    if (taskSec == null) {
                        continue;
                    }
                    Map<String, TaskProgress> map = new HashMap<>();
                    for (String taskId : taskSec.getKeys(false)) {
                        ConfigurationSection v = taskSec.getConfigurationSection(taskId);
                        if (v == null) {
                            continue;
                        }
                        TaskProgress progress = new TaskProgress();
                        progress.progress = Math.max(0, v.getInt("progress", 0));
                        progress.lastDayKey = v.getString("last-day-key", null);
                        map.put(normalize(taskId), progress);
                    }
                    playerProgress.put(uuid, map);
                } catch (IllegalArgumentException ignored) {
                    plugin.getLogger().warning("Invalid UUID in checkin data: " + uuidText);
                }
            }
        }

        ConfigurationSection lockSec = data.getConfigurationSection("lock-overrides");
        if (lockSec != null) {
            for (String taskId : lockSec.getKeys(false)) {
                String raw = lockSec.getString(taskId, "default");
                lockOverrides.put(normalize(taskId), LockOverride.from(raw));
            }
        }
    }

    private void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save checkin config: " + e.getMessage());
        }
    }

    private void saveData() {
        if (data == null || dataFile == null) {
            return;
        }

        data.set("players", null);
        for (Map.Entry<UUID, Map<String, TaskProgress>> p : playerProgress.entrySet()) {
            String base = "players." + p.getKey();
            for (Map.Entry<String, TaskProgress> t : p.getValue().entrySet()) {
                data.set(base + "." + t.getKey() + ".progress", t.getValue().progress);
                data.set(base + "." + t.getKey() + ".last-day-key", t.getValue().lastDayKey);
            }
        }

        data.set("lock-overrides", null);
        for (Map.Entry<String, LockOverride> e : lockOverrides.entrySet()) {
            data.set("lock-overrides." + e.getKey(), e.getValue().raw);
        }

        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save checkin data: " + e.getMessage());
        }
    }

    private TaskProgress getProgress(UUID uuid, String taskId) {
        Map<String, TaskProgress> map = playerProgress.computeIfAbsent(uuid, k -> new HashMap<>());
        return map.computeIfAbsent(normalize(taskId), k -> new TaskProgress());
    }

    private String getCurrentDayKey(LocalTime resetTime) {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        LocalDate date = now.toLocalTime().isBefore(resetTime) ? now.toLocalDate().minusDays(1) : now.toLocalDate();
        return date.toString();
    }

    private String getPreviousDayKey(LocalTime resetTime) {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        LocalDate date = now.toLocalTime().isBefore(resetTime) ? now.toLocalDate().minusDays(2) : now.toLocalDate().minusDays(1);
        return date.toString();
    }

    public String msg(String key) {
        return Utils.colorize(messages.getOrDefault(key, ""));
    }

    public String format(String key, Player player, CheckinTask task, TaskProgress progress) {
        String text = messages.getOrDefault(key, "");
        if (player != null) {
            text = text.replace("%player%", player.getName());
        }
        if (task != null) {
            text = text.replace("%task_id%", task.id);
            text = text.replace("%task_title%", task.title);
            text = text.replace("%total%", String.valueOf(task.totalDays));
        }
        if (progress != null) {
            text = text.replace("%progress%", String.valueOf(progress.progress));
        }
        return Utils.colorize(text);
    }

    private String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT).trim();
    }

    public static class CheckinHolder implements InventoryHolder {
        private final int page;

        public CheckinHolder(int page) {
            this.page = page;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    public enum UnlockType {
        NONE,
        AFTER_TASK,
        ALWAYS_LOCKED
    }

    public enum LockOverride {
        DEFAULT("default"),
        LOCKED("locked"),
        UNLOCKED("unlocked");

        private final String raw;

        LockOverride(String raw) {
            this.raw = raw;
        }

        public static LockOverride from(String raw) {
            if ("locked".equalsIgnoreCase(raw)) {
                return LOCKED;
            }
            if ("unlocked".equalsIgnoreCase(raw)) {
                return UNLOCKED;
            }
            return DEFAULT;
        }
    }

    public static class CheckinTask {
        private String id;
        private String title;
        private String icon;
        private List<String> lore = new ArrayList<>();
        private int totalDays;
        private LocalTime resetTime = LocalTime.MIDNIGHT;
        private UnlockType unlockType = UnlockType.NONE;
        private String unlockTaskId;
        private boolean loop;
        private boolean continuous;
        private final Map<Integer, List<String>> dayCommands = new LinkedHashMap<>();
    }

    public static class TaskProgress {
        private int progress;
        private String lastDayKey;
    }
}
