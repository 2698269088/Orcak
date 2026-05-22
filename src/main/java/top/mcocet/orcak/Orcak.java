package top.mcocet.orcak;

import org.bukkit.plugin.java.JavaPlugin;

public final class Orcak extends JavaPlugin {

    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private boolean isGlobalMuted = false;

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
        
        // 注册区块实体限制监听器
        getServer().getPluginManager().registerEvents(new ChunkEntityLimitListener(this, configManager), this);
        
        // 注册聊天频率限制监听器
        getServer().getPluginManager().registerEvents(new ChatRateLimitListener(this, configManager, databaseManager), this);
        
        // 注册区块凋落物限制监听器
        getServer().getPluginManager().registerEvents(new ChunkItemLimitListener(this, configManager), this);
        
        // 注册玩家伤害限制监听器
        getServer().getPluginManager().registerEvents(new DamageLimitListener(this, configManager), this);
        
        // 注册死亡消息监听器
        getServer().getPluginManager().registerEvents(new DeathMessageListener(this, configManager), this);
        
        // 注册游戏模式锁定监听器
        getServer().getPluginManager().registerEvents(new GameModeLockListener(this), this);
        
        // 注册stat命令执行器和补全器
        StatCommandExecutor statExecutor = new StatCommandExecutor(this, databaseManager);
        getCommand("stat").setExecutor(statExecutor);
        getCommand("stat").setTabCompleter(statExecutor);
        
        // 注册orcak母命令执行器和补全器
        OrcakCommand orcakExecutor = new OrcakCommand(this, databaseManager);
        getCommand("orcak").setExecutor(orcakExecutor);
        getCommand("orcak").setTabCompleter(orcakExecutor);
        
        // 注册跟随清理监听器
        getServer().getPluginManager().registerEvents(new FollowCleanupListener(orcakExecutor), this);
        
        // 注册chatcolor命令执行器和补全器
        ChatColorCommand chatColorExecutor = new ChatColorCommand(this, databaseManager);
        getCommand("chatcolor").setExecutor(chatColorExecutor);
        getCommand("chatcolor").setTabCompleter(chatColorExecutor);
        
        // 注册自杀命令执行器
        SuicideCommand suicideExecutor = new SuicideCommand(this);
        getCommand("514").setExecutor(suicideExecutor);
        getCommand("k").setExecutor(suicideExecutor);
        
        getLogger().info("Orcak插件已启用！");
    }

    @Override
    public void onDisable() {
        // 清理所有跟随任务
        // 注意：需要在获取命令执行器之前保存引用，或者在 OrcakCommand 中提供静态清理方法
        // 这里简化处理，直接依赖任务自动清理
        
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

    /**
     * 获取全局禁言状态
     */
    public boolean isGlobalMuted() {
        return isGlobalMuted;
    }

    /**
     * 设置全局禁言状态
     */
    public void setGlobalMuted(boolean globalMuted) {
        isGlobalMuted = globalMuted;
    }
}
