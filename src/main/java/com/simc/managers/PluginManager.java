package com.simc.managers;

import com.simc.SiMCUniverse;
import com.simc.listeners.PlayerListener;
import com.simc.modules.checkin.CheckinModule;
import com.simc.modules.game.GameModule;
import com.simc.modules.killscore.KillScoreModule;
import com.simc.modules.livescore.LiveScoreModule;
import com.simc.modules.protection.ProtectionModule;
import com.simc.modules.quickenhance.QuickenhanceModule;
import com.simc.modules.random.RandomModule;
import com.simc.modules.shop.ShopModule;
import com.simc.modules.task.TaskModule;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class PluginManager {
    private final SiMCUniverse plugin;
    private KillScoreModule killScoreModule;
    private LiveScoreModule liveScoreModule;
    private ShopModule shopModule;
    private RandomModule randomModule;
    private CheckinModule checkinModule;
    private TaskModule taskModule;
    private ProtectionModule protectionModule;
    private QuickenhanceModule quickenhanceModule;
    private GameModule gameModule;

    public PluginManager(SiMCUniverse plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        registerListeners();
        initializeModules();
        plugin.getLogger().info("PluginManager initialized.");
    }

    public void shutdown() {
        if (killScoreModule != null) {
            killScoreModule.shutdown();
        }
        if (liveScoreModule != null) {
            liveScoreModule.shutdown();
        }
        if (shopModule != null) {
            shopModule.shutdown();
        }
        if (randomModule != null) {
            randomModule.shutdown();
        }
        if (checkinModule != null) {
            checkinModule.shutdown();
        }
        if (taskModule != null) {
            taskModule.shutdown();
        }
        if (protectionModule != null) {
            protectionModule.shutdown();
        }
        if (quickenhanceModule != null) {
            quickenhanceModule.shutdown();
        }
        if (gameModule != null) {
            gameModule.shutdown();
        }
        plugin.getLogger().info("PluginManager shutdown completed.");
    }

    private void registerListeners() {
        plugin.getServer().getPluginManager().registerEvents(new PlayerListener(plugin), plugin);
    }

    private void initializeModules() {
        killScoreModule = new KillScoreModule(plugin);
        killScoreModule.initialize();

        liveScoreModule = new LiveScoreModule(plugin);
        liveScoreModule.initialize();

        shopModule = new ShopModule(plugin);
        shopModule.initialize();

        randomModule = new RandomModule(plugin);
        randomModule.initialize();

        checkinModule = new CheckinModule(plugin);
        checkinModule.initialize();

        taskModule = new TaskModule(plugin);
        taskModule.initialize();

        protectionModule = new ProtectionModule(plugin);
        protectionModule.initialize();

        quickenhanceModule = new QuickenhanceModule(plugin);
        quickenhanceModule.initialize();

        gameModule = new GameModule(plugin);
        gameModule.initialize();
    }

    public KillScoreModule getKillScoreModule() {
        return killScoreModule;
    }

    public LiveScoreModule getLiveScoreModule() {
        return liveScoreModule;
    }

    public ShopModule getShopModule() {
        return shopModule;
    }

    public RandomModule getRandomModule() {
        return randomModule;
    }

    public CheckinModule getCheckinModule() {
        return checkinModule;
    }

    public TaskModule getTaskModule() {
        return taskModule;
    }

    public ProtectionModule getProtectionModule() {
        return protectionModule;
    }

    public QuickenhanceModule getQuickenhanceModule() {
        return quickenhanceModule;
    }

    public GameModule getGameModule() {
        return gameModule;
    }

    public Map<String, Boolean> listModuleStatus() {
        Map<String, Boolean> result = new LinkedHashMap<>();
        result.put("killscore", killScoreModule != null && killScoreModule.isEnabled());
        result.put("livescore", liveScoreModule != null && liveScoreModule.isEnabled());
        result.put("shop", shopModule != null && shopModule.isEnabled());
        result.put("random", randomModule != null && randomModule.isEnabled());
        result.put("checkin", checkinModule != null && checkinModule.isEnabled());
        result.put("task", taskModule != null && taskModule.isEnabled());
        result.put("protection", protectionModule != null && protectionModule.isEnabled());
        result.put("quickenhance", quickenhanceModule != null && quickenhanceModule.isEnabled());
        result.put("game", gameModule != null && gameModule.isEnabled());
        return result;
    }

    public boolean enableModule(String moduleName) {
        String key = normalize(moduleName);
        if ("killscore".equals(key) && killScoreModule != null) {
            killScoreModule.enable();
            return true;
        }
        if ("livescore".equals(key) && liveScoreModule != null) {
            liveScoreModule.enable();
            return true;
        }
        if ("shop".equals(key) && shopModule != null) {
            shopModule.enable();
            return true;
        }
        if ("random".equals(key) && randomModule != null) {
            randomModule.enable();
            return true;
        }
        if ("checkin".equals(key) && checkinModule != null) {
            checkinModule.enable();
            return true;
        }
        if ("task".equals(key) && taskModule != null) {
            taskModule.enable();
            return true;
        }
        if ("protection".equals(key) && protectionModule != null) {
            protectionModule.enable();
            return true;
        }
        if ("quickenhance".equals(key) && quickenhanceModule != null) {
            quickenhanceModule.enable();
            return true;
        }
        if ("game".equals(key) && gameModule != null) {
            gameModule.enable();
            return true;
        }
        return false;
    }

    public boolean disableModule(String moduleName) {
        String key = normalize(moduleName);
        if ("killscore".equals(key) && killScoreModule != null) {
            killScoreModule.disable();
            return true;
        }
        if ("livescore".equals(key) && liveScoreModule != null) {
            liveScoreModule.disable();
            return true;
        }
        if ("shop".equals(key) && shopModule != null) {
            shopModule.disable();
            return true;
        }
        if ("random".equals(key) && randomModule != null) {
            randomModule.disable();
            return true;
        }
        if ("checkin".equals(key) && checkinModule != null) {
            checkinModule.disable();
            return true;
        }
        if ("task".equals(key) && taskModule != null) {
            taskModule.disable();
            return true;
        }
        if ("protection".equals(key) && protectionModule != null) {
            protectionModule.disable();
            return true;
        }
        if ("quickenhance".equals(key) && quickenhanceModule != null) {
            quickenhanceModule.disable();
            return true;
        }
        if ("game".equals(key) && gameModule != null) {
            gameModule.disable();
            return true;
        }
        return false;
    }

    public boolean reloadModule(String moduleName) {
        String key = normalize(moduleName);
        if ("killscore".equals(key) && killScoreModule != null) {
            killScoreModule.reload();
            return true;
        }
        if ("livescore".equals(key) && liveScoreModule != null) {
            liveScoreModule.reload();
            return true;
        }
        if ("shop".equals(key) && shopModule != null) {
            shopModule.reload();
            return true;
        }
        if ("random".equals(key) && randomModule != null) {
            randomModule.reload();
            return true;
        }
        if ("checkin".equals(key) && checkinModule != null) {
            checkinModule.reload();
            return true;
        }
        if ("task".equals(key) && taskModule != null) {
            taskModule.reload();
            return true;
        }
        if ("protection".equals(key) && protectionModule != null) {
            protectionModule.reload();
            return true;
        }
        if ("quickenhance".equals(key) && quickenhanceModule != null) {
            quickenhanceModule.reload();
            return true;
        }
        if ("game".equals(key) && gameModule != null) {
            gameModule.reload();
            return true;
        }
        return false;
    }

    public void reloadPluginSelf() {
        plugin.reloadConfig();
        if (killScoreModule != null) {
            killScoreModule.reload();
        }
        if (liveScoreModule != null) {
            liveScoreModule.reload();
        }
        if (shopModule != null) {
            shopModule.reload();
        }
        if (randomModule != null) {
            randomModule.reload();
        }
        if (checkinModule != null) {
            checkinModule.reload();
        }
        if (taskModule != null) {
            taskModule.reload();
        }
        if (protectionModule != null) {
            protectionModule.reload();
        }
        if (quickenhanceModule != null) {
            quickenhanceModule.reload();
        }
        if (gameModule != null) {
            gameModule.reload();
        }
    }

    private String normalize(String moduleName) {
        return moduleName == null ? "" : moduleName.toLowerCase(Locale.ROOT).trim();
    }
}