package top.mcocet.orcak;

import org.bukkit.plugin.java.JavaPlugin;

public final class Orcak extends JavaPlugin {

    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private boolean isGlobalMuted = false;
    
    // 存储被锁定位置的玩家及其锁定位置
    private final java.util.Map<org.bukkit.entity.Player, org.bukkit.Location> lockedPositions = new java.util.HashMap<>();

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

        // 注册命令执行限制监听器
        getServer().getPluginManager().registerEvents(new CommandLimitListener(this, configManager), this);

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
        
        // 注册位置锁定监听器
        getServer().getPluginManager().registerEvents(new PositionLockListener(this), this);
        
        // 注册IP限制监听器
        getServer().getPluginManager().registerEvents(new IPLimitListener(this, configManager, databaseManager), this);
        
        // 注册踢人次数限制监听器
        getServer().getPluginManager().registerEvents(new KickLimitListener(this, configManager), this);

        // 初始化自定义帮助命令处理器
        CustomHelpCommand customHelpCommand = new CustomHelpCommand(this, configManager);

        // 注册命令拦截监听器
        getServer().getPluginManager().registerEvents(new HelpCommandListener(customHelpCommand), this);
        
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
        
        // 从数据库加载位置锁定信息
        loadPositionLocks();
        
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
    
    /**
     * 锁定玩家的位置
     */
    public void lockPlayerPosition(org.bukkit.entity.Player player, org.bukkit.Location location) {
        lockedPositions.put(player, location);
        
        // 更新数据库中的位置锁定信息
        top.mcocet.orcak.PlayerStats stats = databaseManager.getPlayerStats(player.getUniqueId());
        if (stats != null) {
            stats.setPosLocked(true);
            stats.setLockedWorld(location.getWorld().getName());
            stats.setLockedX(location.getX());
            stats.setLockedY(location.getY());
            stats.setLockedZ(location.getZ());
            databaseManager.savePlayerStats(stats);
        }
    }
    
    /**
     * 解锁玩家的位置
     */
    public void unlockPlayerPosition(org.bukkit.entity.Player player) {
        lockedPositions.remove(player);
        
        // 更新数据库中的位置锁定信息
        top.mcocet.orcak.PlayerStats stats = databaseManager.getPlayerStats(player.getUniqueId());
        if (stats != null) {
            stats.setPosLocked(false);
            stats.setLockedWorld("");
            stats.setLockedX(0.0);
            stats.setLockedY(0.0);
            stats.setLockedZ(0.0);
            databaseManager.savePlayerStats(stats);
        }
    }
    
    /**
     * 检查玩家位置是否被锁定
     */
    public boolean isPlayerPositionLocked(org.bukkit.entity.Player player) {
        // 首先检查内存中的锁定状态
        if (lockedPositions.containsKey(player)) {
            return true;
        }
        
        // 然后检查数据库中的锁定状态
        top.mcocet.orcak.PlayerStats stats = databaseManager.getPlayerStats(player.getUniqueId());
        return stats != null && stats.isPosLocked();
    }
    
    /**
     * 获取玩家的锁定位置
     */
    public org.bukkit.Location getLockedPosition(org.bukkit.entity.Player player) {
        // 首先检查内存中的位置
        if (lockedPositions.containsKey(player)) {
            return lockedPositions.get(player);
        }
        
        // 然后从数据库加载位置
        top.mcocet.orcak.PlayerStats stats = databaseManager.getPlayerStats(player.getUniqueId());
        if (stats != null && stats.isPosLocked()) {
            org.bukkit.World world = org.bukkit.Bukkit.getWorld(stats.getLockedWorld());
            if (world != null) {
                return new org.bukkit.Location(world, stats.getLockedX(), stats.getLockedY(), stats.getLockedZ());
            }
        }
        
        return null;
    }
    
    /**
     * 获取所有被锁定位置的玩家
     */
    public java.util.Set<org.bukkit.entity.Player> getLockedPlayers() {
        return lockedPositions.keySet();
    }
    
    /**
     * 从数据库加载所有位置锁定信息
     */
    public void loadPositionLocks() {
        // 遍历所有在线玩家并检查他们的位置锁定状态
        for (org.bukkit.entity.Player player : getServer().getOnlinePlayers()) {
            top.mcocet.orcak.PlayerStats stats = databaseManager.getPlayerStats(player.getUniqueId());
            if (stats != null && stats.isPosLocked()) {
                org.bukkit.World world = org.bukkit.Bukkit.getWorld(stats.getLockedWorld());
                if (world != null) {
                    org.bukkit.Location location = new org.bukkit.Location(world, stats.getLockedX(), stats.getLockedY(), stats.getLockedZ());
                    lockedPositions.put(player, location);
                }
            }
        }
    }
}