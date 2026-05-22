package top.mcocet.orcak;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 聊天频率限制监听器
 * 限制玩家发送消息的频率，并防止重复发送相同内容
 * 管理员不受限制
 */
public class ChatRateLimitListener implements Listener {
    
    private final Orcak plugin;
    private final ConfigManager configManager;
    private final DatabaseManager databaseManager;
    
    // 记录每个玩家最后一条消息的发送时间
    private final Map<UUID, Long> lastMessageTime = new ConcurrentHashMap<>();
    
    // 记录每个玩家最后发送的消息内容和时间
    private final Map<UUID, MessageRecord> lastMessageRecord = new ConcurrentHashMap<>();
    
    /**
     * 消息记录
     */
    private static class MessageRecord {
        private final String message;
        private final long timestamp;
        
        public MessageRecord(String message, long timestamp) {
            this.message = message;
            this.timestamp = timestamp;
        }
        
        public String getMessage() {
            return message;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
    }
    
    public ChatRateLimitListener(Orcak plugin, ConfigManager configManager, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.databaseManager = databaseManager;
    }
    
    /**
     * 监听玩家聊天事件
     * 使用HIGH优先级，确保在其他插件之前处理
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (!isRateLimitEnabled()) {
            return;
        }
        
        Player player = event.getPlayer();
        
        // 检查是否为管理员，如果是则跳过限制
        if (shouldBypassForAdmins() && isAdmin(player)) {
            return;
        }
        
        // 检查是否为全局禁言
        if (plugin.isGlobalMuted()) {
            event.setCancelled(true);
            
            if (shouldSendWarning()) {
                sendWarning(player, getGlobalMuteWarningMessage());
            }
            
            return;
        }
        
        // 检查玩家是否被禁言
        if (databaseManager.isPlayerMuted(player.getUniqueId())) {
            event.setCancelled(true);
            
            if (shouldSendWarning()) {
                sendWarning(player, getMuteWarningMessage());
            }
            
            return;
        }
        
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        
        // 检查发送频率
        if (isSendingTooFast(playerId, currentTime)) {
            event.setCancelled(true);
            
            if (shouldSendWarning()) {
                sendWarning(player, getWarningMessage());
            }
            
            return;
        }
        
        // 检查是否为重复消息
        if (isAntiDuplicateEnabled() && isDuplicateMessage(playerId, event.getMessage(), currentTime)) {
            event.setCancelled(true);
            
            if (shouldSendWarning()) {
                sendWarning(player, getDuplicateWarningMessage());
            }
            
            return;
        }
        
        // 更新记录
        updateRecords(playerId, event.getMessage(), currentTime);
    }
    
    /**
     * 检查玩家是否发送过快
     */
    private boolean isSendingTooFast(UUID playerId, long currentTime) {
        Long lastTime = lastMessageTime.get(playerId);
        
        if (lastTime == null) {
            return false;
        }
        
        long intervalMillis = getIntervalSeconds() * 1000L;
        long timeDiff = currentTime - lastTime;
        
        return timeDiff < intervalMillis;
    }
    
    /**
     * 检查是否为重复消息
     */
    private boolean isDuplicateMessage(UUID playerId, String message, long currentTime) {
        MessageRecord record = lastMessageRecord.get(playerId);
        
        if (record == null) {
            return false;
        }
        
        // 检查消息内容是否相同
        if (!record.getMessage().equals(message)) {
            return false;
        }
        
        // 检查是否在时间窗口内
        long windowMillis = getDuplicateWindow() * 1000L;
        long timeDiff = currentTime - record.getTimestamp();
        
        return timeDiff < windowMillis;
    }
    
    /**
     * 更新记录
     */
    private void updateRecords(UUID playerId, String message, long currentTime) {
        lastMessageTime.put(playerId, currentTime);
        lastMessageRecord.put(playerId, new MessageRecord(message, currentTime));
    }
    
    /**
     * 检查玩家是否为管理员
     */
    private boolean isAdmin(Player player) {
        // 检查是否有op权限或管理员权限
        return player.isOp() || player.hasPermission("orcak.chat.bypass");
    }
    
    /**
     * 发送警告消息给玩家
     */
    private void sendWarning(Player player, String message) {
        // 异步事件中需要使用同步任务发送消息
        if (plugin.isFolia()) {
            player.getScheduler().run(plugin, (task) -> {
                player.sendMessage(colorize(message));
            }, null);
        } else {
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                player.sendMessage(colorize(message));
            });
        }
    }
    
    /**
     * 将颜色代码转换为实际颜色
     */
    private String colorize(String message) {
        return message.replace('&', '§');
    }
    
    /**
     * 是否启用聊天频率限制
     */
    private boolean isRateLimitEnabled() {
        return configManager.getConfig().getBoolean("chat-rate-limit.enabled", true);
    }
    
    /**
     * 获取消息发送间隔时间（秒）
     */
    private int getIntervalSeconds() {
        return configManager.getConfig().getInt("chat-rate-limit.interval-seconds", 1);
    }
    
    /**
     * 是否启用防重复消息
     */
    private boolean isAntiDuplicateEnabled() {
        return configManager.getConfig().getBoolean("chat-rate-limit.anti-duplicate", true);
    }
    
    /**
     * 获取重复消息检测的时间窗口（秒）
     */
    private int getDuplicateWindow() {
        return configManager.getConfig().getInt("chat-rate-limit.duplicate-window", 5);
    }
    
    /**
     * 管理员是否不受限制
     */
    private boolean shouldBypassForAdmins() {
        return configManager.getConfig().getBoolean("chat-rate-limit.bypass-for-admins", true);
    }
    
    /**
     * 是否发送警告消息
     */
    private boolean shouldSendWarning() {
        return configManager.getConfig().getBoolean("chat-rate-limit.send-warning", true);
    }
    
    /**
     * 获取频率限制警告消息
     */
    private String getWarningMessage() {
        return configManager.getConfig().getString("chat-rate-limit.warning-message", "&c请不要频繁发送消息！");
    }
    
    /**
     * 获取重复消息警告消息
     */
    private String getDuplicateWarningMessage() {
        return configManager.getConfig().getString("chat-rate-limit.duplicate-warning-message", "&c请不要重复发送相同的消息！");
    }
    
    /**
     * 获取禁言警告消息
     */
    private String getMuteWarningMessage() {
        return configManager.getConfig().getString("chat-rate-limit.mute-warning-message", "&c你已被禁言，无法发送消息！");
    }
    
    /**
     * 获取全局禁言警告消息
     */
    private String getGlobalMuteWarningMessage() {
        return configManager.getConfig().getString("chat-rate-limit.global-mute-warning-message", "&c服务器已开启全员禁言！");
    }
}
