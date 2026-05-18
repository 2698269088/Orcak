package top.mcocet.orcak;

import org.bukkit.plugin.java.JavaPlugin;

public final class Orcak extends JavaPlugin {

    private ConfigManager configManager;
    private DatabaseManager databaseManager;

    @Override
    public void onEnable() {
        // 初始化配置管理器
        configManager = new ConfigManager(this);
        
        // 创建help.txt文件（如果不存在）
        configManager.createHelpFile();
        
        // 注册help命令监听器
        getServer().getPluginManager().registerEvents(new HelpCommandExecutor(this, configManager), this);
        
        // 初始化数据库管理器
        databaseManager = new DatabaseManager(this);
        
        // 注册玩家数据监听器
        getServer().getPluginManager().registerEvents(new PlayerDataListener(this, databaseManager), this);
        
        // 注册聊天颜色监听器
        getServer().getPluginManager().registerEvents(new ChatColorListener(this, databaseManager), this);
        
        // 注册stat命令执行器和补全器
        StatCommandExecutor statExecutor = new StatCommandExecutor(this, databaseManager);
        getCommand("stat").setExecutor(statExecutor);
        getCommand("stat").setTabCompleter(statExecutor);
        
        // 注册orcak母命令执行器和补全器
        OrcakCommand orcakExecutor = new OrcakCommand(this, databaseManager);
        getCommand("orcak").setExecutor(orcakExecutor);
        getCommand("orcak").setTabCompleter(orcakExecutor);
        
        getLogger().info("Orcak插件已启用！");
    }

    @Override
    public void onDisable() {
        // 保存配置
        if (configManager != null) {
            configManager.saveConfig();
        }
        
        // 关闭数据库连接
        if (databaseManager != null) {
            databaseManager.close();
        }
        
        getLogger().info("Orcak插件已禁用！");
    }
    
    /**
     * 获取配置管理器实例
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    /**
     * 检查是否为 Folia 环境
     */
    public boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
