package com.simc.modules.task;

import com.simc.SiMCUniverse;
import com.simc.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class TaskModule {
    private final SiMCUniverse plugin;

    private boolean enabled;

    private File moduleFolder;
    private File configFile;
    private File dataFile;
    private File confFolder;

    private YamlConfiguration config;
    private YamlConfiguration data;

    private final Map<String, String> messages = new HashMap<>();
    private final Map<String, TaskDefinition> tasks = new LinkedHashMap<>();
    private final Map<UUID, Map<String, TaskProgress>> playerProgress = new HashMap<>();

    private LocalTime dailyResetTime = LocalTime.MIDNIGHT;
    private DayOfWeek weeklyResetDay = DayOfWeek.MONDAY;
    private LocalTime weeklyResetTime = LocalTime.MIDNIGHT;

    private TaskCommand command;
    private TaskListener listener;

    public TaskModule(SiMCUniverse plugin) {
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
        plugin.getLogger().info("Task module initialized.");
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
        plugin.getLogger().info("Task module shutdown completed.");
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

        List<TaskDefinition> list = new ArrayList<>(tasks.values());
        int totalPages = Math.max(1, (list.size() + 44) / 45);
        int currentPage = Math.max(1, Math.min(page, totalPages));

        Inventory inv = Bukkit.createInventory(new TaskHolder(currentPage), 54,
                Utils.colorize("&8任务中心 &7[" + currentPage + "/" + totalPages + "]"));

        fillBottomBar(inv);

        int from = (currentPage - 1) * 45;
        int to = Math.min(list.size(), from + 45);
        int slot = 0;
        for (int i = from; i < to; i++) {
            TaskDefinition task = list.get(i);
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
        if (!(inv.getHolder() instanceof TaskHolder)) {
            return;
        }

        TaskHolder holder = (TaskHolder) inv.getHolder();

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

        List<TaskDefinition> list = new ArrayList<>(tasks.values());
        int index = (holder.page - 1) * 45 + slot;
        if (index < 0 || index >= list.size()) {
            return;
        }

        TaskDefinition task = list.get(index);
        TaskProgress progress = getFreshProgress(player.getUniqueId(), task);

        if (progress.completed) {
            player.sendMessage(msg("already-complete"));
            return;
        }

        if (task.type == TaskType.POSITION) {
            if (isPositionReached(player.getLocation(), task.position)) {
                completeTask(player, task, progress);
                player.sendMessage(msg("position-confirmed"));
                openMainGui(player, holder.page);
            } else {
                player.sendMessage(msg("position-not-reached"));
            }
            return;
        }

        player.sendMessage(msg("auto-task-tip"));
    }

    public void onBlockBreak(Player player, Material material) {
        updateMapTaskProgress(player, TaskType.BREAK, materialKey(material), 1);
    }

    public void onBlockPlace(Player player, Material material) {
        updateMapTaskProgress(player, TaskType.PLACE, materialKey(material), 1);
    }

    public void onInteract(Player player, Material material) {
        updateMapTaskProgress(player, TaskType.INTERACT, materialKey(material), 1);
    }

    public void onEntityKill(Player player, EntityType type) {
        if (type == null || type.getKey() == null) {
            return;
        }
        updateMapTaskProgress(player, TaskType.KILL, normalizeKey(type.getKey().toString()), 1);
    }

    public void onPickup(Player player, ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return;
        }
        updateMapTaskProgress(player, TaskType.PICKUP, materialKey(stack.getType()), stack.getAmount());
    }

    public void onGainXp(Player player, int amount) {
        if (amount <= 0) {
            return;
        }

        for (TaskDefinition task : tasks.values()) {
            if (task.type != TaskType.GAIN_XP) {
                continue;
            }

            TaskProgress progress = getFreshProgress(player.getUniqueId(), task);
            if (progress.completed) {
                continue;
            }

            progress.value = Math.min(task.requiredValue, progress.value + amount);
            if (progress.value >= task.requiredValue) {
                completeTask(player, task, progress);
            }
        }

        saveData();
    }

    public void onCraft(Player player, ItemStack result, int amount) {
        if (result == null || result.getType().isAir() || amount <= 0) {
            return;
        }
        updateMapTaskProgress(player, TaskType.CRAFT, materialKey(result.getType()), amount);
    }

    public void onMove(Player player, Location to) {
        if (to == null) {
            return;
        }

        for (TaskDefinition task : tasks.values()) {
            if (task.type != TaskType.POSITION || task.position == null) {
                continue;
            }

            TaskProgress progress = getFreshProgress(player.getUniqueId(), task);
            if (progress.completed || progress.value >= 1) {
                continue;
            }

            if (isPositionReached(to, task.position)) {
                progress.value = 1;
                player.sendMessage(format("position-reached", task));
            }
        }

        saveData();
    }

    public void resetTaskAllPlayers(String taskId) {
        String key = normalize(taskId);
        for (Map<String, TaskProgress> map : playerProgress.values()) {
            map.remove(key);
        }
        saveData();
    }

    public List<String> getTaskIds() {
        return new ArrayList<>(tasks.keySet());
    }

    private void updateMapTaskProgress(Player player, TaskType type, String targetKey, int amount) {
        if (targetKey == null || targetKey.isBlank() || amount <= 0) {
            return;
        }

        for (TaskDefinition task : tasks.values()) {
            if (task.type != type) {
                continue;
            }

            TaskProgress progress = getFreshProgress(player.getUniqueId(), task);
            if (progress.completed) {
                continue;
            }

            for (Map.Entry<String, Integer> req : task.requiredMap.entrySet()) {
                String reqBase = baseKey(req.getKey());
                if (!reqBase.equals(targetKey)) {
                    continue;
                }

                int now = progress.entries.getOrDefault(req.getKey(), 0);
                int next = Math.min(req.getValue(), now + amount);
                progress.entries.put(req.getKey(), next);
            }

            if (isMapTaskDone(task, progress)) {
                completeTask(player, task, progress);
            }
        }

        saveData();
    }

    private boolean isMapTaskDone(TaskDefinition task, TaskProgress progress) {
        for (Map.Entry<String, Integer> entry : task.requiredMap.entrySet()) {
            int now = progress.entries.getOrDefault(entry.getKey(), 0);
            if (now < entry.getValue()) {
                return false;
            }
        }
        return !task.requiredMap.isEmpty();
    }

    private void completeTask(Player player, TaskDefinition task, TaskProgress progress) {
        if (progress.completed) {
            return;
        }

        progress.completed = true;
        progress.periodKey = computePeriodKey(task.category);

        for (String cmd : task.rewards) {
            String finalCmd = cmd.replace("%player%", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd);
        }

        player.sendMessage(format("task-complete", task));
        sendToast(player, task);
        saveData();
    }

    private TaskProgress getFreshProgress(UUID uuid, TaskDefinition task) {
        Map<String, TaskProgress> map = playerProgress.computeIfAbsent(uuid, k -> new HashMap<>());
        TaskProgress progress = map.computeIfAbsent(task.id, k -> new TaskProgress());

        String period = computePeriodKey(task.category);
        if (task.category != TaskCategory.ACHIEVEMENT && !period.equals(progress.periodKey)) {
            progress.periodKey = period;
            progress.completed = false;
            progress.value = 0;
            progress.entries.clear();
        }

        if (progress.periodKey == null || progress.periodKey.isBlank()) {
            progress.periodKey = period;
        }

        return progress;
    }

    private ItemStack buildTaskItem(Player player, TaskDefinition task) {
        Material icon = parseMaterial(task.icon, Material.PAPER);
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        TaskProgress progress = getFreshProgress(player.getUniqueId(), task);

        meta.setDisplayName(Utils.colorize(task.title));
        List<String> lore = new ArrayList<>();
        lore.add(Utils.colorize("&7类别: &f" + categoryDisplay(task.category)));
        lore.add(Utils.colorize("&7类型: &f" + task.type.name()));
        lore.addAll(colorizeLines(task.description));
        lore.add(Utils.colorize("&7ID: &f" + task.id));
        lore.add(Utils.colorize("&7进度: &e" + progressText(task, progress)));
        lore.add(Utils.colorize(progress.completed ? "&a状态: 已完成" : "&e状态: 进行中"));

        if (task.type == TaskType.POSITION && task.position != null) {
            lore.add(Utils.colorize("&7目标坐标: &f" + task.position.x + "," + task.position.y + "," + task.position.z));
            lore.add(Utils.colorize("&7检测半径: &f" + task.position.radius));
            lore.add(Utils.colorize("&7提示: 到达后点击本任务确认"));
        }

        meta.setLore(lore);

        if (progress.completed) {
            meta.addEnchant(Enchantment.DURABILITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        item.setItemMeta(meta);
        return item;
    }

    private List<String> colorizeLines(List<String> lines) {
        List<String> out = new ArrayList<>();
        for (String line : lines) {
            out.add(Utils.colorize(line));
        }
        return out;
    }

    private String progressText(TaskDefinition task, TaskProgress progress) {
        if (task.type == TaskType.GAIN_XP) {
            return Math.min(progress.value, task.requiredValue) + "/" + task.requiredValue;
        }

        if (task.type == TaskType.POSITION) {
            return progress.value >= 1 ? "已到达，可确认" : "未到达";
        }

        int need = 0;
        int now = 0;
        for (Map.Entry<String, Integer> entry : task.requiredMap.entrySet()) {
            need += entry.getValue();
            now += Math.min(entry.getValue(), progress.entries.getOrDefault(entry.getKey(), 0));
        }
        return now + "/" + need;
    }

    private String categoryDisplay(TaskCategory category) {
        if (category == TaskCategory.DAILY) {
            return "日常任务";
        }
        if (category == TaskCategory.WEEKLY) {
            return "每周任务";
        }
        return "成就任务";
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

    private void sendToast(Player player, TaskDefinition task) {
        String title = "任务完成！";
        String desc = "完成任务：" + stripColor(task.title);

        String json = buildToastAdvancementJson(title, desc, task.icon);
        NamespacedKey key = new NamespacedKey(plugin,
                "task_toast_" + player.getUniqueId().toString().replace("-", "") + "_" + task.id);

        try {
            Bukkit.getUnsafe().loadAdvancement(key, json);
            Advancement advancement = Bukkit.getAdvancement(key);
            if (advancement != null) {
                AdvancementProgress progress = player.getAdvancementProgress(advancement);
                for (String criteria : progress.getRemainingCriteria()) {
                    progress.awardCriteria(criteria);
                }

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    try {
                        Bukkit.getUnsafe().removeAdvancement(key);
                    } catch (Exception ignored) {
                    }
                }, 20L);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to show task toast: " + e.getMessage());
        }
    }

    private String buildToastAdvancementJson(String title, String description, String iconMaterial) {
        String mat = iconMaterial == null || iconMaterial.isBlank() ? "minecraft:paper" : iconMaterial;
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

    private String stripColor(String text) {
        return text == null ? "" : text.replaceAll("§[0-9A-FK-ORa-fk-or]", "");
    }

    private boolean isPositionReached(Location location, PositionRequirement position) {
        if (location == null || position == null) {
            return false;
        }
        double dx = location.getX() - position.x;
        double dy = location.getY() - position.y;
        double dz = location.getZ() - position.z;
        return dx * dx + dy * dy + dz * dz <= (double) position.radius * position.radius;
    }

    private String materialKey(Material material) {
        if (material == null || material.getKey() == null) {
            return "";
        }
        return normalizeKey(material.getKey().toString());
    }

    private String normalizeKey(String key) {
        return key == null ? "" : key.toLowerCase(Locale.ROOT).trim();
    }

    private String baseKey(String key) {
        String normalized = normalizeKey(key);
        int idx = normalized.indexOf('{');
        return idx >= 0 ? normalized.substring(0, idx) : normalized;
    }

    private Material parseMaterial(String text, Material fallback) {
        if (text == null || text.isBlank()) {
            return fallback;
        }
        Material material = Material.matchMaterial(text);
        if (material != null) {
            return material;
        }
        String noNs = text.contains(":") ? text.substring(text.indexOf(':') + 1) : text;
        material = Material.matchMaterial(noNs);
        return material == null ? fallback : material;
    }

    public String msg(String key) {
        return Utils.colorize(messages.getOrDefault(key, ""));
    }

    public String format(String key, TaskDefinition task) {
        String text = messages.getOrDefault(key, "");
        if (task != null) {
            text = text.replace("%task_id%", task.id)
                    .replace("%task_title%", task.title);
        }
        return Utils.colorize(text);
    }

    private String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT).trim();
    }

    private void registerCommand() {
        if (plugin.getCommand("si-task") == null) {
            plugin.getLogger().warning("Command si-task is missing in plugin.yml");
            return;
        }

        if (command == null) {
            command = new TaskCommand(this);
        }
        plugin.getCommand("si-task").setExecutor(command);
        plugin.getCommand("si-task").setTabCompleter(command);
    }

    private void registerListener() {
        listener = new TaskListener(this);
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
    }

    private void loadAll() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Could not create plugin data folder.");
        }

        moduleFolder = new File(plugin.getDataFolder(), "task");
        if (!moduleFolder.exists() && !moduleFolder.mkdirs()) {
            plugin.getLogger().warning("Could not create task module folder.");
        }

        confFolder = new File(moduleFolder, "task_conf");
        if (!confFolder.exists() && !confFolder.mkdirs()) {
            plugin.getLogger().warning("Could not create task_conf folder.");
        }

        configFile = new File(moduleFolder, "config.yml");
        dataFile = new File(moduleFolder, "data.yml");

        if (!configFile.exists()) {
            plugin.saveResource("task/config.yml", false);
        }
        if (!dataFile.exists()) {
            data = new YamlConfiguration();
            data.set("players", null);
            saveData();
        }

        File sample = new File(confFolder, "daily_break_stone.yml");
        if (!sample.exists()) {
            plugin.saveResource("task/task_conf/daily_break_stone.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(configFile);
        data = YamlConfiguration.loadConfiguration(dataFile);

        loadMainConfig();
        loadTasks();
        loadData();

        saveConfig();
        saveData();
    }

    private void loadMainConfig() {
        messages.clear();
        messages.put("module-disabled", config.getString("messages.module-disabled", "&cTask 模块已禁用。"));
        messages.put("reload-success", config.getString("messages.reload-success", "&aTask 配置已重载。"));
        messages.put("reset-success", config.getString("messages.reset-success", "&a已重置任务 %task_id% 的全服进度。"));
        messages.put("task-complete", config.getString("messages.task-complete", "&a任务完成：%task_title%"));
        messages.put("already-complete", config.getString("messages.already-complete", "&e该任务已完成。"));
        messages.put("auto-task-tip", config.getString("messages.auto-task-tip", "&7该任务为自动统计，无需手动确认。"));
        messages.put("position-reached", config.getString("messages.position-reached", "&a你已到达目标区域，点击任务即可确认完成。"));
        messages.put("position-not-reached", config.getString("messages.position-not-reached", "&c你还未到达任务目标区域。"));
        messages.put("position-confirmed", config.getString("messages.position-confirmed", "&a已确认位置任务完成。"));

        dailyResetTime = parseTime(config.getString("daily-reset-time", "04:00:00"), LocalTime.of(4, 0));

        String weeklyRaw = config.getString("weekly-reset-time", "MONDAY:04:00:00");
        try {
            String[] split = weeklyRaw.split(":", 2);
            weeklyResetDay = DayOfWeek.valueOf(split[0].trim().toUpperCase(Locale.ROOT));
            weeklyResetTime = parseTime(split.length > 1 ? split[1] : "04:00:00", LocalTime.of(4, 0));
        } catch (Exception ignored) {
            weeklyResetDay = DayOfWeek.MONDAY;
            weeklyResetTime = LocalTime.of(4, 0);
        }
    }

    private LocalTime parseTime(String text, LocalTime fallback) {
        try {
            return LocalTime.parse(text);
        } catch (Exception e) {
            return fallback;
        }
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
            TaskDefinition task = parseTask(yml, file.getName());
            if (task != null) {
                tasks.put(task.id, task);
            }
        }
    }

    private TaskDefinition parseTask(YamlConfiguration yml, String fileName) {
        String id = normalize(yml.getString("id", ""));
        if (!id.matches("[a-z0-9_-]+")) {
            plugin.getLogger().warning("Skip task with invalid id in " + fileName);
            return null;
        }

        TaskDefinition task = new TaskDefinition();
        task.id = id;
        task.title = yml.getString("name", "&f未命名任务");
        task.description = yml.getStringList("description");
        task.icon = yml.getString("icon", "minecraft:paper");
        task.category = TaskCategory.from(yml.getString("category", "daily"));
        task.type = TaskType.from(yml.getString("type", "BREAK"));
        task.rewards = yml.getStringList("rewards");

        if (task.type == TaskType.GAIN_XP) {
            task.requiredValue = Math.max(1, yml.getInt("required", 1));
            return task;
        }

        if (task.type == TaskType.POSITION) {
            task.position = parsePositionRequirement(yml);
            if (task.position == null) {
                plugin.getLogger().warning("Skip POSITION task with invalid required in " + fileName);
                return null;
            }
            task.requiredValue = 1;
            return task;
        }

        task.requiredMap = parseRequiredMap(yml.getConfigurationSection("required"));
        if (task.requiredMap.isEmpty()) {
            plugin.getLogger().warning("Skip task with empty required map in " + fileName);
            return null;
        }

        return task;
    }

    private PositionRequirement parsePositionRequirement(YamlConfiguration yml) {
        Object required = yml.get("required");
        if (required == null) {
            return null;
        }

        try {
            if (required instanceof String) {
                String raw = ((String) required).replace("(", "").replace(")", "");
                String[] parts = raw.split(",");
                if (parts.length == 4) {
                    return new PositionRequirement(
                            Double.parseDouble(parts[0].trim()),
                            Double.parseDouble(parts[1].trim()),
                            Double.parseDouble(parts[2].trim()),
                            Math.max(1, Integer.parseInt(parts[3].trim()))
                    );
                }
            }

            if (required instanceof List<?>) {
                List<?> list = (List<?>) required;
                if (list.size() == 4) {
                    return new PositionRequirement(
                            Double.parseDouble(String.valueOf(list.get(0))),
                            Double.parseDouble(String.valueOf(list.get(1))),
                            Double.parseDouble(String.valueOf(list.get(2))),
                            Math.max(1, Integer.parseInt(String.valueOf(list.get(3))))
                    );
                }
            }

            ConfigurationSection sec = yml.getConfigurationSection("required");
            if (sec != null && sec.contains("x") && sec.contains("y") && sec.contains("z") && sec.contains("radius")) {
                return new PositionRequirement(
                        sec.getDouble("x"),
                        sec.getDouble("y"),
                        sec.getDouble("z"),
                        Math.max(1, sec.getInt("radius", 3))
                );
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    private LinkedHashMap<String, Integer> parseRequiredMap(ConfigurationSection section) {
        LinkedHashMap<String, Integer> map = new LinkedHashMap<>();
        if (section == null) {
            return map;
        }

        for (String key : section.getKeys(false)) {
            int amount = Math.max(1, section.getInt(key, 1));
            map.put(normalizeKey(key), amount);
        }

        return map;
    }

    private void loadData() {
        playerProgress.clear();

        ConfigurationSection players = data.getConfigurationSection("players");
        if (players == null) {
            return;
        }

        for (String uuidText : players.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidText);
                ConfigurationSection taskSec = players.getConfigurationSection(uuidText);
                if (taskSec == null) {
                    continue;
                }

                Map<String, TaskProgress> map = new HashMap<>();
                for (String taskId : taskSec.getKeys(false)) {
                    ConfigurationSection entry = taskSec.getConfigurationSection(taskId);
                    if (entry == null) {
                        continue;
                    }

                    TaskProgress progress = new TaskProgress();
                    progress.completed = entry.getBoolean("completed", false);
                    progress.periodKey = entry.getString("period", "");
                    progress.value = Math.max(0, entry.getInt("value", 0));

                    ConfigurationSection detail = entry.getConfigurationSection("entries");
                    if (detail != null) {
                        for (String key : detail.getKeys(false)) {
                            progress.entries.put(normalizeKey(key), Math.max(0, detail.getInt(key, 0)));
                        }
                    }

                    map.put(normalize(taskId), progress);
                }

                playerProgress.put(uuid, map);
            } catch (Exception ignored) {
                plugin.getLogger().warning("Invalid UUID in task data: " + uuidText);
            }
        }
    }

    private void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save task config: " + e.getMessage());
        }
    }

    private void saveData() {
        if (data == null || dataFile == null) {
            return;
        }

        data.set("players", null);

        for (Map.Entry<UUID, Map<String, TaskProgress>> playerEntry : playerProgress.entrySet()) {
            String base = "players." + playerEntry.getKey();
            for (Map.Entry<String, TaskProgress> taskEntry : playerEntry.getValue().entrySet()) {
                String path = base + "." + taskEntry.getKey();
                TaskProgress progress = taskEntry.getValue();
                data.set(path + ".completed", progress.completed);
                data.set(path + ".period", progress.periodKey);
                data.set(path + ".value", progress.value);
                data.set(path + ".entries", null);
                for (Map.Entry<String, Integer> detail : progress.entries.entrySet()) {
                    data.set(path + ".entries." + detail.getKey(), detail.getValue());
                }
            }
        }

        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save task data: " + e.getMessage());
        }
    }

    private String computePeriodKey(TaskCategory category) {
        if (category == TaskCategory.ACHIEVEMENT) {
            return "achievement";
        }

        LocalDateTime now = LocalDateTime.now();

        if (category == TaskCategory.DAILY) {
            LocalDate date = now.toLocalTime().isBefore(dailyResetTime)
                    ? now.toLocalDate().minusDays(1)
                    : now.toLocalDate();
            return date.toString();
        }

        LocalDate currentDate = now.toLocalDate();
        int diff = currentDate.getDayOfWeek().getValue() - weeklyResetDay.getValue();
        if (diff < 0) {
            diff += 7;
        }

        LocalDate resetDate = currentDate.minusDays(diff);
        LocalDateTime resetDateTime = LocalDateTime.of(resetDate, weeklyResetTime);
        if (now.isBefore(resetDateTime)) {
            resetDate = resetDate.minusWeeks(1);
        }

        WeekFields wf = WeekFields.ISO;
        int week = resetDate.get(wf.weekOfWeekBasedYear());
        int year = resetDate.get(wf.weekBasedYear());
        return year + "-W" + week;
    }

    public static class TaskHolder implements InventoryHolder {
        private final int page;

        public TaskHolder(int page) {
            this.page = page;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    public enum TaskCategory {
        DAILY,
        WEEKLY,
        ACHIEVEMENT;

        public static TaskCategory from(String raw) {
            String v = raw == null ? "" : raw.toLowerCase(Locale.ROOT).trim();
            if ("weekly".equals(v)) {
                return WEEKLY;
            }
            if ("achievement".equals(v)) {
                return ACHIEVEMENT;
            }
            return DAILY;
        }
    }

    public enum TaskType {
        BREAK,
        PLACE,
        INTERACT,
        KILL,
        GAIN_XP,
        POSITION,
        PICKUP,
        CRAFT;

        public static TaskType from(String raw) {
            try {
                return TaskType.valueOf(raw == null ? "BREAK" : raw.toUpperCase(Locale.ROOT).trim());
            } catch (Exception ignored) {
                return BREAK;
            }
        }
    }

    public static class TaskDefinition {
        private String id;
        private String title;
        private List<String> description = new ArrayList<>();
        private String icon;
        private TaskCategory category = TaskCategory.DAILY;
        private TaskType type = TaskType.BREAK;
        private LinkedHashMap<String, Integer> requiredMap = new LinkedHashMap<>();
        private int requiredValue = 1;
        private PositionRequirement position;
        private List<String> rewards = new ArrayList<>();
    }

    public static class PositionRequirement {
        private final double x;
        private final double y;
        private final double z;
        private final int radius;

        public PositionRequirement(double x, double y, double z, int radius) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.radius = radius;
        }
    }

    public static class TaskProgress {
        private boolean completed;
        private String periodKey;
        private int value;
        private final Map<String, Integer> entries = new HashMap<>();
    }
}
