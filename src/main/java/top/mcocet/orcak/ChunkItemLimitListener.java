package top.mcocet.orcak;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 区块凋落物数量限制监听器
 * 限制单个区块内的物品实体（凋落物）数量，防止服务器卡顿和 lag
 */
public class ChunkItemLimitListener implements Listener {
    
    private final Orcak plugin;
    private final ConfigManager configManager;
    
    // 记录每个区块的凋落物生成时间，用于删除最旧的凋落物
    private final Map<String, List<ItemSpawnRecord>> chunkItemRecords = new ConcurrentHashMap<>();
    
    // 标记定时任务是否已经启动
    private static final AtomicBoolean taskStarted = new AtomicBoolean(false);
    
    public ChunkItemLimitListener(Orcak plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        
        // 不在构造函数中启动任务，等待世界加载完成
    }
    
    /**
     * 凋落物生成记录
     */
    private static class ItemSpawnRecord {
        private final Item item;
        private final long spawnTime;
        
        public ItemSpawnRecord(Item item) {
            this.item = item;
            this.spawnTime = System.currentTimeMillis();
        }
        
        public Item getItem() {
            return item;
        }
        
        public long getSpawnTime() {
            return spawnTime;
        }
    }
    
    /**
     * 监听物品生成事件
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        if (!isLimitEnabled()) {
            return;
        }
        
        Item item = event.getEntity();
        Chunk chunk = item.getLocation().getChunk();
        
        // 检查是否超过凋落物限制
        if (isItemLimitExceeded(chunk)) {
            event.setCancelled(true);
            if (shouldLogCleanup()) {
                plugin.getLogger().info("阻止凋落物生成：区块 [" + chunk.getX() + ", " + chunk.getZ() + 
                    "] 已达到最大凋落物数量限制 (" + getMaxItems() + ")");
            }
        } else {
            // 记录凋落物生成时间
            recordItemSpawn(item);
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
        
        // 异步检查并清理区块凋落物
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
     * 检查并清理区块中的凋落物
     */
    private void checkAndCleanChunk(Chunk chunk) {
        if (!isLimitEnabled()) {
            return;
        }
        
        String chunkKey = getChunkKey(chunk);
        
        // 清理无效的凋落物记录
        cleanupInvalidRecords(chunkKey);
        
        // 检查并清理凋落物数量
        cleanItemsInChunk(chunk);
    }
    
    /**
     * 清理区块中的凋落物（如果超过限制）
     */
    private void cleanItemsInChunk(Chunk chunk) {
        int maxItems = getMaxItems();
        List<Item> items = new ArrayList<>();
        
        // 收集所有物品实体
        for (org.bukkit.entity.Entity entity : chunk.getEntities()) {
            if (entity instanceof Item) {
                items.add((Item) entity);
            }
        }
        
        // 如果超过限制，删除多余的凋落物
        if (items.size() > maxItems) {
            int toRemove = items.size() - maxItems;
            
            if (shouldRemoveOldest()) {
                // 按生成时间排序，删除最旧的
                items.sort(Comparator.comparingLong(this::getItemSpawnTime));
            }
            
            int removedCount = 0;
            for (int i = 0; i < toRemove && i < items.size(); i++) {
                Item item = items.get(i);
                if (item != null && !item.isDead()) {
                    item.remove();
                    removeItemRecord(item);
                    removedCount++;
                    
                    if (shouldLogCleanup()) {
                        plugin.getLogger().info("清理凋落物：" + item.getItemStack().getType() + 
                            " x" + item.getItemStack().getAmount() + " 在区块 [" + 
                            chunk.getX() + ", " + chunk.getZ() + "]");
                    }
                }
            }
            
            if (removedCount > 0 && shouldLogCleanup()) {
                plugin.getLogger().info("区块 [" + chunk.getX() + ", " + chunk.getZ() + 
                    "] 共清理了 " + removedCount + " 个凋落物");
            }
        }
    }
    
    /**
     * 获取凋落物的生成时间
     */
    private long getItemSpawnTime(Item item) {
        String chunkKey = getChunkKey(item.getLocation().getChunk());
        List<ItemSpawnRecord> records = chunkItemRecords.get(chunkKey);
        
        if (records != null) {
            for (ItemSpawnRecord record : records) {
                if (record.getItem() == item) {
                    return record.getSpawnTime();
                }
            }
        }
        
        // 如果没有记录，返回当前时间（新凋落物优先保留）
        return System.currentTimeMillis();
    }
    
    /**
     * 记录凋落物生成
     */
    private void recordItemSpawn(Item item) {
        String chunkKey = getChunkKey(item.getLocation().getChunk());
        chunkItemRecords.computeIfAbsent(chunkKey, k -> new ArrayList<>())
            .add(new ItemSpawnRecord(item));
    }
    
    /**
     * 移除凋落物记录
     */
    private void removeItemRecord(Item item) {
        String chunkKey = getChunkKey(item.getLocation().getChunk());
        List<ItemSpawnRecord> records = chunkItemRecords.get(chunkKey);
        
        if (records != null) {
            records.removeIf(record -> record.getItem() == item);
        }
    }
    
    /**
     * 清理无效的凋落物记录
     */
    private void cleanupInvalidRecords(String chunkKey) {
        List<ItemSpawnRecord> records = chunkItemRecords.get(chunkKey);
        
        if (records != null) {
            records.removeIf(record -> record.getItem() == null || record.getItem().isDead());
        }
    }
    
    /**
     * 检查区块是否超过凋落物限制
     */
    private boolean isItemLimitExceeded(Chunk chunk) {
        int maxItems = getMaxItems();
        int itemCount = 0;
        
        for (org.bukkit.entity.Entity entity : chunk.getEntities()) {
            if (entity instanceof Item) {
                itemCount++;
            }
        }
        
        return itemCount >= maxItems;
    }
    
    /**
     * 获取区块的唯一标识
     */
    private String getChunkKey(Chunk chunk) {
        return chunk.getWorld().getName() + ":" + chunk.getX() + ":" + chunk.getZ();
    }
    
    /**
     * 是否启用限制
     */
    private boolean isLimitEnabled() {
        return configManager.getConfig().getBoolean("chunk-item-limits.enabled", true);
    }
    
    /**
     * 获取最大凋落物数量
     */
    private int getMaxItems() {
        return configManager.getConfig().getInt("chunk-item-limits.max-items", 2000);
    }
    
    /**
     * 获取检查间隔
     */
    private int getCheckInterval() {
        return configManager.getConfig().getInt("chunk-item-limits.check-interval", 40);
    }
    
    /**
     * 是否删除最旧的凋落物
     */
    private boolean shouldRemoveOldest() {
        return configManager.getConfig().getBoolean("chunk-item-limits.remove-oldest", true);
    }
    
    /**
     * 是否记录清理日志
     */
    private boolean shouldLogCleanup() {
        return configManager.getConfig().getBoolean("chunk-item-limits.log-cleanup", false);
    }
}
