package com.simc.modules.quickenhance;

import com.simc.SiMCUniverse;
import com.simc.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class QuickenhanceModule {
    private final SiMCUniverse plugin;

    private boolean enabled;

    private File moduleFolder;
    private File configFile;
    private YamlConfiguration config;

    private CostMode costMode = CostMode.VANILLA;
    private int maxCostLevel = 30;
    private int constantCostLevel = 5;
    private int linearMultiplier = 1;
    private int exponentialBase = 2;

    private String messagePickupTip;
    private String messageNotEnough;
    private String messageApplySuccess;
    private String messageNoAvailable;

    private QuickenhanceCommand command;
    private QuickenhanceListener listener;

    public QuickenhanceModule(SiMCUniverse plugin) {
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
        plugin.getLogger().info("Quickenhance module initialized.");
    }

    public void shutdown() {
        if (!enabled) {
            return;
        }

        if (listener != null) {
            HandlerList.unregisterAll(listener);
            listener = null;
        }

        enabled = false;
        plugin.getLogger().info("Quickenhance module shutdown completed.");
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

    public String msg(String key) {
        if ("pickup-tip".equals(key)) {
            return Utils.colorize(messagePickupTip);
        }
        if ("not-enough".equals(key)) {
            return Utils.colorize(messageNotEnough);
        }
        if ("apply-success".equals(key)) {
            return Utils.colorize(messageApplySuccess);
        }
        if ("no-available".equals(key)) {
            return Utils.colorize(messageNoAvailable);
        }
        return "";
    }

    public void notifyPickup(Player player) {
        if (player == null || messagePickupTip == null || messagePickupTip.isBlank()) {
            return;
        }
        player.sendMessage(Utils.colorize(messagePickupTip));
    }

    public boolean applyBook(Player player, ItemStack book, ItemStack target) {
        if (!enabled || player == null || book == null || target == null) {
            return false;
        }

        if (book.getType() != Material.ENCHANTED_BOOK) {
            return false;
        }

        if (target.getType() == Material.AIR) {
            return false;
        }

        if (!(book.getItemMeta() instanceof EnchantmentStorageMeta)) {
            player.sendMessage(msg("no-available"));
            return false;
        }

        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
        if (meta == null || meta.getStoredEnchants().isEmpty()) {
            player.sendMessage(msg("no-available"));
            return false;
        }

        Map<Enchantment, Integer> stored = new HashMap<>(meta.getStoredEnchants());
        Map<Enchantment, Integer> applied = new HashMap<>();

        for (Map.Entry<Enchantment, Integer> entry : stored.entrySet()) {
            Enchantment enchant = entry.getKey();
            int level = entry.getValue();
            if (enchant == null || level <= 0) {
                continue;
            }

            if (!enchant.canEnchantItem(target)) {
                continue;
            }

            boolean conflict = false;
            for (Enchantment existing : target.getEnchantments().keySet()) {
                if (existing.equals(enchant)) {
                    continue;
                }
                if (existing.conflictsWith(enchant)) {
                    conflict = true;
                    break;
                }
            }
            if (conflict) {
                continue;
            }

            int existingLevel = target.getEnchantmentLevel(enchant);
            if (existingLevel >= level) {
                continue;
            }

            applied.put(enchant, level);
        }

        if (applied.isEmpty()) {
            player.sendMessage(msg("no-available"));
            return false;
        }

        int cost = calculateCost(applied);
        if (cost > maxCostLevel) {
            cost = maxCostLevel;
        }

        if (cost > 0 && player.getLevel() < cost) {
            player.sendMessage(msg("not-enough").replace("%cost%", String.valueOf(cost)));
            return false;
        }

        for (Map.Entry<Enchantment, Integer> entry : applied.entrySet()) {
            target.addUnsafeEnchantment(entry.getKey(), entry.getValue());
        }

        if (cost > 0) {
            player.giveExpLevels(-cost);
        }

        player.sendMessage(msg("apply-success").replace("%cost%", String.valueOf(cost)));
        return true;
    }

    private int calculateCost(Map<Enchantment, Integer> enchants) {
        if (enchants.isEmpty()) {
            return 0;
        }

        switch (costMode) {
            case LINEAR:
                int sum = 0;
                for (int level : enchants.values()) {
                    sum += Math.max(1, level) * Math.max(1, linearMultiplier);
                }
                return sum;
            case EXPONENTIAL:
                int exp = 0;
                for (int level : enchants.values()) {
                    exp += (int) Math.pow(Math.max(2, exponentialBase), Math.max(1, level));
                }
                return exp;
            case CONSTANT:
                return Math.max(0, constantCostLevel);
            case VANILLA:
            default:
                int vanilla = 0;
                for (int level : enchants.values()) {
                    int lv = Math.max(1, level);
                    vanilla += 1 + lv * lv;
                }
                return vanilla;
        }
    }

    private void registerCommand() {
        if (plugin.getCommand("si-quickenhance") == null) {
            plugin.getLogger().warning("Command si-quickenhance is missing in plugin.yml");
            return;
        }

        if (command == null) {
            command = new QuickenhanceCommand(this);
        }
        plugin.getCommand("si-quickenhance").setExecutor(command);
        plugin.getCommand("si-quickenhance").setTabCompleter(command);
    }

    private void registerListener() {
        listener = new QuickenhanceListener(this);
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
    }

    private void loadAll() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Could not create plugin data folder.");
        }

        moduleFolder = new File(plugin.getDataFolder(), "quickenhance");
        if (!moduleFolder.exists() && !moduleFolder.mkdirs()) {
            plugin.getLogger().warning("Could not create quickenhance module folder.");
        }

        configFile = new File(moduleFolder, "config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("quickenhance/config.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(configFile);

        costMode = CostMode.from(config.getString("cost.mode", "vanilla"));
        maxCostLevel = Math.max(0, config.getInt("cost.max-level", 30));
        constantCostLevel = Math.max(0, config.getInt("cost.constant-level", 5));
        linearMultiplier = Math.max(1, config.getInt("cost.linear-multiplier", 1));
        exponentialBase = Math.max(2, config.getInt("cost.exponential-base", 2));

        messagePickupTip = config.getString("messages.pickup-tip", "&e你获得了附魔书，拖动到物品上并右键即可快速附魔。");
        messageNotEnough = config.getString("messages.not-enough-exp", "&c经验不足，需要 &e%cost% &c级。");
        messageApplySuccess = config.getString("messages.apply-success", "&a附魔成功，消耗 &e%cost% &a级经验。");
        messageNoAvailable = config.getString("messages.no-available", "&e该附魔书没有可转移的附魔。");

        saveConfig();
    }

    private void saveConfig() {
        if (config == null || configFile == null) {
            return;
        }
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save quickenhance config: " + e.getMessage());
        }
    }

    public enum CostMode {
        VANILLA,
        LINEAR,
        EXPONENTIAL,
        CONSTANT;

        public static CostMode from(String raw) {
            if (raw == null) {
                return VANILLA;
            }
            String v = raw.trim().toLowerCase();
            if ("linear".equals(v)) {
                return LINEAR;
            }
            if ("exp".equals(v) || "exponential".equals(v)) {
                return EXPONENTIAL;
            }
            if ("constant".equals(v)) {
                return CONSTANT;
            }
            return VANILLA;
        }
    }
}
