package top.mcocet.orcak;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.WorldLoadEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 区块实体数量限制监听器
 * 限制单个区块内的生物和实体数量，防止服务器卡顿
 */
public class ChunkEntityLimitListener implements Listener {
    
    private final Orcak plugin;
    private final ConfigManager configManager;
    
    // 标记定时任务是否已经启动
    private static final AtomicBoolean taskStarted = new AtomicBoolean(false);
    
    public ChunkEntityLimitListener(Orcak plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        
        // 不在构造函数中启动任务，等待世界加载完成
    }
    

    
    /**
     * 监听生物生成事件
     * 如果区块已达到生物数量上限，则阻止新生成
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!isLimitEnabled()) {
            return;
        }
        
        LivingEntity entity = event.getEntity();
        Chunk chunk = entity.getLocation().getChunk();
        
        // 检查是否超过生物限制
        if (isMobLimitExceeded(chunk)) {
            event.setCancelled(true);
            if (shouldLogCleanup()) {
                plugin.getLogger().info("阻止生物生成：区块 [" + chunk.getX() + ", " + chunk.getZ() + "] 已达到最大生物数量限制 (" + getMaxMobs() + ")");
            }
        }
    }
    
    /**
     * 监听区块加载事件
     */
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (!isLimitEnabled()) {
            return;
        }
        
        // 异步检查并清理区块实体
        checkAndCleanChunk(event.getChunk());
    }
    
    /**
     * 监听世界加载事件，在世界加载完成后启动定时任务
     */
    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        // 只在第一个世界加载时启动任务，避免重复启动
        if (taskStarted.compareAndSet(false, true)) {
            // 延迟10tick启动，确保世界完全加载
            org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, this::startCheckTask, 10L);
        }
    }
    
    /**
     * 启动定期检查任务
     */
    private void startCheckTask() {
        int checkInterval = getCheckInterval();
        
        if (plugin.isFolia()) {
            // Folia 环境：必须在主线程执行，因为需要访问区块数据
            org.bukkit.Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                if (!isLimitEnabled()) {
                    return;
                }
                
                // 遍历所有世界的所有已加载区块
                for (World world : plugin.getServer().getWorlds()) {
                    for (Chunk chunk : world.getLoadedChunks()) {
                        checkAndCleanChunk(chunk);
                    }
                }
            }, 0L, checkInterval);
        } else {
            // 非 Folia 环境：使用传统调度器
            org.bukkit.Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
                if (!isLimitEnabled()) {
                    return;
                }
                
                // 遍历所有世界的所有已加载区块
                for (World world : plugin.getServer().getWorlds()) {
                    for (Chunk chunk : world.getLoadedChunks()) {
                        checkAndCleanChunk(chunk);
                    }
                }
            }, 0L, checkInterval);
        }
    }
    
    /**
     * 检查并清理区块中的实体
     * 直接遍历区块中的所有实体进行计数和清理
     */
    private void checkAndCleanChunk(Chunk chunk) {
        if (!isLimitEnabled()) {
            return;
        }
        
        // 检查并清理生物数量
        cleanMobsInChunk(chunk);
        
        // 检查并清理实体总数
        cleanEntitiesInChunk(chunk);
    }
    
    /**
     * 清理区块中的生物（如果超过限制）
     * 直接统计当前区块中的所有生物，超过限制则删除
     */
    private void cleanMobsInChunk(Chunk chunk) {
        int maxMobs = getMaxMobs();
        List<Entity> mobs = new ArrayList<>();
        
        // 收集所有生物（非玩家的LivingEntity）
        for (Entity entity : chunk.getEntities()) {
            if (entity instanceof LivingEntity && !(entity instanceof org.bukkit.entity.Player)) {
                mobs.add(entity);
            }
        }
        
        // 如果超过限制，删除多余的生物
        if (mobs.size() > maxMobs) {
            int toRemove = mobs.size() - maxMobs;
            
            // 简单策略：从列表末尾开始删除（避免排序开销）
            for (int i = mobs.size() - 1; i >= 0 && toRemove > 0; i--) {
                Entity entity = mobs.get(i);
                if (entity != null && !entity.isDead()) {
                    entity.remove();
                    toRemove--;
                    
                    if (shouldLogCleanup()) {
                        plugin.getLogger().info("清理生物：" + entity.getType() + " 在区块 [" + 
                            chunk.getX() + ", " + chunk.getZ() + "]");
                    }
                }
            }
        }
    }
    
    /**
     * 清理区块中的实体总数（如果超过限制）
     * 直接统计当前区块中的所有实体，超过限制则删除
     */
    private void cleanEntitiesInChunk(Chunk chunk) {
        int maxEntities = getMaxEntities();
        List<Entity> entities = new ArrayList<>();
        
        // 收集所有非玩家实体
        for (Entity entity : chunk.getEntities()) {
            if (!(entity instanceof org.bukkit.entity.Player)) {
                entities.add(entity);
            }
        }
        
        // 如果超过限制，删除多余的实体
        if (entities.size() > maxEntities) {
            int toRemove = entities.size() - maxEntities;
            
            // 简单策略：从列表末尾开始删除（避免排序开销）
            for (int i = entities.size() - 1; i >= 0 && toRemove > 0; i--) {
                Entity entity = entities.get(i);
                if (entity != null && !entity.isDead()) {
                    entity.remove();
                    toRemove--;
                    
                    if (shouldLogCleanup()) {
                        plugin.getLogger().info("清理实体：" + entity.getType() + " 在区块 [" + 
                            chunk.getX() + ", " + chunk.getZ() + "]");
                    }
                }
            }
        }
    }
    

    
    /**
     * 检查区块是否超过生物限制
     * 直接统计当前区块中的生物数量
     */
    private boolean isMobLimitExceeded(Chunk chunk) {
        int maxMobs = getMaxMobs();
        int mobCount = 0;
        
        for (Entity entity : chunk.getEntities()) {
            if (entity instanceof LivingEntity && !(entity instanceof org.bukkit.entity.Player)) {
                mobCount++;
                // 提前退出优化
                if (mobCount >= maxMobs) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * 是否启用限制
     */
    private boolean isLimitEnabled() {
        return configManager.getConfig().getBoolean("chunk-entity-limits.enabled", true);
    }
    
    /**
     * 获取最大生物数量
     */
    private int getMaxMobs() {
        return configManager.getConfig().getInt("chunk-entity-limits.max-mobs", 50);
    }
    
    /**
     * 获取最大实体数量
     */
    private int getMaxEntities() {
        return configManager.getConfig().getInt("chunk-entity-limits.max-entities", 100);
    }
    
    /**
     * 获取检查间隔
     */
    private int getCheckInterval() {
        return configManager.getConfig().getInt("chunk-entity-limits.check-interval", 20);
    }
    
    /**
     * 是否记录清理日志
     */
    private boolean shouldLogCleanup() {
        return configManager.getConfig().getBoolean("chunk-entity-limits.log-cleanup", false);
    }
}
