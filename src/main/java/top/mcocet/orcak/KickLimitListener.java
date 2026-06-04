package top.mcocet.orcak;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 踢人次数限制监听器
 * 检测玩家被踢出游戏的次数，达到配置次数后自动Ban玩家
 */
public class KickLimitListener implements Listener {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;

    // 存储玩家的踢人记录: UUID -> KickRecord
    private final Map<UUID, KickRecord> kickRecords = new ConcurrentHashMap<>();

    // 存储被Ban的玩家: UUID -> 解封时间戳（0表示永久Ban）
    private final Map<UUID, Long> bannedPlayers = new ConcurrentHashMap<>();

    // 存储玩家累计被Ban次数: UUID -> 被Ban次数
    private final Map<UUID, Integer> banCountMap = new ConcurrentHashMap<>();

    public KickLimitListener(JavaPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    /**
     * 监听玩家被踢事件
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerKick(PlayerKickEvent event) {
        if (!configManager.getConfig().getBoolean("kick-limit.enabled", false)) {
            return;
        }

        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();

        // 获取配置
        int maxKicks = configManager.getConfig().getInt("kick-limit.max-kicks", 5);
        long timeWindowMs = (long) configManager.getConfig().getInt("kick-limit.time-window-minutes", 60) * 60 * 1000;

        // 获取或创建踢人记录
        KickRecord record = kickRecords.computeIfAbsent(playerId, k -> new KickRecord());

        // 清理过期的踢人记录
        record.cleanupOldKicks(currentTime - timeWindowMs);

        // 添加新的踢人记录
        record.addKick(currentTime);

        // 检查是否达到限制
        int kickCount = record.getKickCount();
        if (kickCount >= maxKicks) {
            // 达到限制，执行Ban
            banPlayer(playerId, player.getName());
        }

        plugin.getLogger().info("玩家 " + player.getName() + " 被踢出，当前累计踢人次数: " + kickCount + "/" + maxKicks);
    }

    /**
     * 监听玩家登录事件，检查是否被Ban
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (!configManager.getConfig().getBoolean("kick-limit.enabled", false)) {
            return;
        }

        UUID playerId = event.getPlayer().getUniqueId();

        // 检查是否在Ban列表中
        if (bannedPlayers.containsKey(playerId)) {
            long unbanTime = bannedPlayers.get(playerId);
            long currentTime = System.currentTimeMillis();

            if (unbanTime == 0) {
                // 永久Ban
                String message = configManager.getConfig().getString("kick-limit.permanent-ban-message",
                        "&c你已被服务器永久封禁！原因：累计被Ban次数过多");
                event.disallow(Result.KICK_BANNED, ChatColor.translateAlternateColorCodes('&', message));
            } else if (currentTime < unbanTime) {
                // 临时Ban，尚未到期
                long remainingMinutes = (unbanTime - currentTime) / 60000;
                String message = configManager.getConfig().getString("kick-limit.ban-message",
                        "&c你已被服务器封禁！原因：被踢出次数过多") + "\n&c剩余封禁时间: " + remainingMinutes + " 分钟";
                event.disallow(Result.KICK_BANNED, ChatColor.translateAlternateColorCodes('&', message));
            } else {
                // Ban已过期，移除Ban记录
                bannedPlayers.remove(playerId);
            }
        }
    }

    /**
     * Ban玩家
     */
    private void banPlayer(UUID playerId, String playerName) {
        // 获取配置
        int maxBansForPermanent = configManager.getConfig().getInt("kick-limit.max-bans-for-permanent", 3);
        long banDurationMinutes = configManager.getConfig().getLong("kick-limit.ban-duration-minutes", 30);
        boolean notifyPlayer = configManager.getConfig().getBoolean("kick-limit.notify-player", true);
        boolean logBan = configManager.getConfig().getBoolean("kick-limit.log-ban", true);
        String banMessage = configManager.getConfig().getString("kick-limit.ban-message",
                "&c你已被服务器封禁！原因：被踢出次数过多");
        String permanentBanMessage = configManager.getConfig().getString("kick-limit.permanent-ban-message",
                "&c你已被服务器永久封禁！原因：累计被Ban次数过多");

        // 增加被Ban次数
        int banCount = banCountMap.compute(playerId, (k, v) -> v == null ? 1 : v + 1);

        // 判断是否需要永久Ban
        boolean isPermanent = false;
        if (maxBansForPermanent > 0 && banCount >= maxBansForPermanent) {
            isPermanent = true;
        }

        // 计算解封时间（0表示永久Ban）
        long unbanTime = isPermanent ? 0 : System.currentTimeMillis() + (banDurationMinutes * 60 * 1000);

        // 添加到Ban列表
        bannedPlayers.put(playerId, unbanTime);

        // 清理踢人记录
        kickRecords.remove(playerId);

        // 获取玩家对象并通知
        Player player = Bukkit.getPlayer(playerId);
        if (player != null && notifyPlayer) {
            String message = isPermanent ? permanentBanMessage : banMessage;
            player.kickPlayer(ChatColor.translateAlternateColorCodes('&', message));
        }

        // 记录日志
        if (logBan) {
            String durationStr = isPermanent ? "永久" : banDurationMinutes + "分钟";
            plugin.getLogger().info("玩家 " + playerName + " 被Ban，累计Ban次数: " + banCount + "/" + maxBansForPermanent + "，封禁时长: " + durationStr);
        }
    }

    /**
     * 移除玩家的Ban状态
     */
    public void unbanPlayer(UUID playerId) {
        bannedPlayers.remove(playerId);
    }

    /**
     * 检查玩家是否被Ban
     */
    public boolean isPlayerBanned(UUID playerId) {
        if (!bannedPlayers.containsKey(playerId)) {
            return false;
        }

        long unbanTime = bannedPlayers.get(playerId);
        return unbanTime == 0 || System.currentTimeMillis() < unbanTime;
    }

    /**
     * 获取玩家的踢人次数
     */
    public int getKickCount(UUID playerId) {
        KickRecord record = kickRecords.get(playerId);
        return record != null ? record.getKickCount() : 0;
    }

    /**
     * 获取玩家累计被Ban次数
     */
    public int getBanCount(UUID playerId) {
        return banCountMap.getOrDefault(playerId, 0);
    }

    /**
     * 重置玩家的踢人次数
     */
    public void resetKickCount(UUID playerId) {
        kickRecords.remove(playerId);
    }

    /**
     * 重置玩家的累计被Ban次数
     */
    public void resetBanCount(UUID playerId) {
        banCountMap.remove(playerId);
    }

    /**
     * 踢人记录类
     */
    private static class KickRecord {
        // 存储踢人时间戳列表
        private final java.util.List<Long> kickTimes = new java.util.ArrayList<>();

        /**
         * 添加踢人记录
         */
        public synchronized void addKick(long timestamp) {
            kickTimes.add(timestamp);
        }

        /**
         * 清理过期的踢人记录
         */
        public synchronized void cleanupOldKicks(long cutoffTime) {
            kickTimes.removeIf(time -> time < cutoffTime);
        }

        /**
         * 获取踢人次数
         */
        public synchronized int getKickCount() {
            return kickTimes.size();
        }
    }
}