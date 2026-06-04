package top.mcocet.orcak;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * IP限制监听器
 * 限制单个IP的最大注册玩家数和同时在线登录数
 */
public class IPLimitListener implements Listener {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final DatabaseManager databaseManager;

    // 记录当前在线玩家的IP地址
    private final Map<String, String> playerIpMap = new ConcurrentHashMap<>();

    public IPLimitListener(JavaPlugin plugin, ConfigManager configManager, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.databaseManager = databaseManager;
        initializeIpTable();
    }

    private void initializeIpTable() {
        String createTableSQL = """
        CREATE TABLE IF NOT EXISTS player_ips (
            player_uuid TEXT PRIMARY KEY,
            player_name TEXT NOT NULL,
            ip_address TEXT NOT NULL,
            first_ip TEXT,
            first_login_time INTEGER DEFAULT 0,
            last_login_time INTEGER DEFAULT 0,
            login_count INTEGER DEFAULT 0
        );
        """;

        Connection conn = databaseManager.getConnection();
        if (conn == null) {
            plugin.getLogger().severe("数据库连接不可用，无法初始化IP表");
            return;
        }

        try (PreparedStatement pstmt = conn.prepareStatement(createTableSQL)) {
            pstmt.execute();
        } catch (SQLException e) {
            plugin.getLogger().severe("创建IP记录表失败: " + e.getMessage());
        }

        // 兼容旧数据库：添加 first_ip 列
        try (PreparedStatement pstmt = conn.prepareStatement(
                "ALTER TABLE player_ips ADD COLUMN first_ip TEXT;")) {
            pstmt.execute();
        } catch (SQLException e) {
            // 列已存在，忽略错误
        }
    }

    /**
     * 检查IP是否在排除列表中
     */
    private boolean isExcludedIp(String ip) {
        List<String> excludedIps = configManager.getConfig().getStringList("ip-limits.excluded-ips");
        return excludedIps != null && excludedIps.contains(ip);
    }

    /**
     * 监听玩家加入事件
     * 检查注册限制和登录限制，并记录玩家IP地址
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!configManager.getConfig().getBoolean("ip-limits.enabled", true)) {
            return;
        }

        Player player = event.getPlayer();
        String ip = player.getAddress().getAddress().getHostAddress();

        // 检查IP是否在排除列表中
        if (isExcludedIp(ip)) {
            playerIpMap.put(player.getName(), ip);
            updatePlayerIpRecord(player.getUniqueId().toString(), player.getName(), ip);
            return;
        }

        // 先查询该玩家是否已有注册记录
        boolean isRegistered = isPlayerRegistered(player.getUniqueId().toString());

        if (!isRegistered) {
            // 新玩家：检查该IP的注册数量是否已达上限
            int maxRegistrations = configManager.getConfig().getInt("ip-limits.max-registrations", 5);
            int currentRegistrations = getRegistrationCountByIp(ip);

            if (currentRegistrations >= maxRegistrations) {
                String message = configManager.getConfig().getString("ip-limits.register-limit-message",
                        "&c该IP已达到最大注册数量限制！");
                String finalMessage = ChatColor.translateAlternateColorCodes('&', message);
                plugin.getLogger().info("玩家 " + player.getName() + " (IP: " + ip + ") 被踢出：已达到最大注册数量限制 "
                        + currentRegistrations + "/" + maxRegistrations);
                // 延迟踢出，避免Folia/Luminol中PlayerJoinEvent内直接kick导致崩溃
                scheduleKick(player, finalMessage);
                return;
            }
        }

        // 检查该IP的当前在线登录数量是否已达上限
        int maxLogins = configManager.getConfig().getInt("ip-limits.max-logins", 3);
        int currentLogins = getOnlineLoginCountByIp(ip, player.getName());

        if (currentLogins >= maxLogins) {
            String message = configManager.getConfig().getString("ip-limits.login-limit-message",
                    "&c该IP已达到最大同时在线登录数量限制！");
            String finalMessage = ChatColor.translateAlternateColorCodes('&', message);
            plugin.getLogger().info("玩家 " + player.getName() + " (IP: " + ip + ") 被踢出：已达到最大同时在线登录数量限制 "
                    + currentLogins + "/" + maxLogins);
            // 延迟踢出，避免Folia/Luminol中PlayerJoinEvent内直接kick导致崩溃
            scheduleKick(player, finalMessage);
            return;
        }

        // 记录玩家IP
        playerIpMap.put(player.getName(), ip);

        // 更新数据库中的IP记录
        updatePlayerIpRecord(player.getUniqueId().toString(), player.getName(), ip);
    }

    /**
     * 延迟踢出玩家
     * 在Folia/Luminol环境中，PlayerJoinEvent内直接调用kickPlayer会导致内部状态不一致而崩溃
     */
    private void scheduleKick(Player player, String reason) {
        if (plugin instanceof Orcak && ((Orcak) plugin).isFolia()) {
            player.getScheduler().runDelayed(plugin, (task) -> {
                if (player.isOnline()) {
                    player.kickPlayer(reason);
                }
            }, null, 1L);
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (player.isOnline()) {
                    player.kickPlayer(reason);
                }
            });
        }
    }

    /**
     * 检查玩家是否已在数据库中有注册记录
     */
    private synchronized boolean isPlayerRegistered(String playerUuid) {
        String sql = "SELECT 1 FROM player_ips WHERE player_uuid = ?;";

        Connection conn = databaseManager.getConnection();
        if (conn == null) {
            plugin.getLogger().warning("数据库连接不可用，无法查询玩家注册状态");
            return false;
        }

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerUuid);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("查询玩家注册状态失败: " + e.getMessage());
        }
        return false;
    }

    /**
     * 监听玩家退出事件
     * 移除玩家IP记录
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        playerIpMap.remove(event.getPlayer().getName());
    }

    /**
     * 获取指定IP的注册玩家数量（基于首次登录IP）
     */
    private synchronized int getRegistrationCountByIp(String ip) {
        String sql = "SELECT COUNT(*) as count FROM player_ips WHERE first_ip = ?;";

        Connection conn = databaseManager.getConnection();
        if (conn == null) {
            plugin.getLogger().warning("数据库连接不可用，无法查询IP注册数量");
            return 0;
        }

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, ip);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("count");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("查询IP注册数量失败: " + e.getMessage());
        }
        return 0;
    }


    /**
     * 获取指定IP的当前在线登录数量（排除指定玩家）
     */
    private int getOnlineLoginCountByIp(String ip, String excludePlayerName) {
        int count = 0;
        for (Map.Entry<String, String> entry : playerIpMap.entrySet()) {
            if (entry.getValue().equals(ip) && !entry.getKey().equals(excludePlayerName)) {
                count++;
            }
        }
        return count;
    }

    /**
     * 更新玩家IP记录到数据库
     * 规则：
     * 1. 新玩家（数据库中没有记录）：记录当前IP为首次登录IP（注册IP）
     * 2. 已有玩家但 first_ip 为空：将当前IP作为首次登录IP（兼容旧数据）
     * 3. 已有玩家且 first_ip 已存在：只更新最后登录时间和登录次数
     */
    private synchronized void updatePlayerIpRecord(String playerUuid, String playerName, String ip) {
        Connection conn = databaseManager.getConnection();
        if (conn == null) {
            plugin.getLogger().warning("数据库连接不可用，无法更新IP记录: " + playerName);
            return;
        }

        long currentTime = System.currentTimeMillis();

        // 先查询该玩家是否已存在以及 first_ip 是否为空
        String selectSql = "SELECT first_ip FROM player_ips WHERE player_uuid = ?;";
        String existingFirstIp = null;
        boolean recordExists = false;

        try (PreparedStatement pstmt = conn.prepareStatement(selectSql)) {
            pstmt.setString(1, playerUuid);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    recordExists = true;
                    existingFirstIp = rs.getString("first_ip");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("查询玩家IP记录失败: " + e.getMessage());
            return;
        }

        if (!recordExists) {
            // 新玩家：首次登录，当前IP即为注册IP
            String insertSql = """
                INSERT INTO player_ips (player_uuid, player_name, ip_address, first_ip, first_login_time, last_login_time, login_count)
                VALUES (?, ?, ?, ?, ?, ?, 1);
                """;
            try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                pstmt.setString(1, playerUuid);
                pstmt.setString(2, playerName);
                pstmt.setString(3, ip);
                pstmt.setString(4, ip);
                pstmt.setLong(5, currentTime);
                pstmt.setLong(6, currentTime);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("插入玩家IP记录失败: " + e.getMessage());
            }
        } else {
            // 已有玩家
            if (existingFirstIp == null || existingFirstIp.isEmpty()) {
                // first_ip 为空，将当前IP补为首次登录IP
                String updateSql = """
                    UPDATE player_ips
                    SET player_name = ?, ip_address = ?, first_ip = ?, last_login_time = ?, login_count = login_count + 1
                    WHERE player_uuid = ?;
                    """;
                try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                    pstmt.setString(1, playerName);
                    pstmt.setString(2, ip);
                    pstmt.setString(3, ip);
                    pstmt.setLong(4, currentTime);
                    pstmt.setString(5, playerUuid);
                    pstmt.executeUpdate();
                } catch (SQLException e) {
                    plugin.getLogger().severe("更新玩家IP记录（补全first_ip）失败: " + e.getMessage());
                }
            } else {
                // first_ip 已存在，只更新常规字段
                String updateSql = """
                    UPDATE player_ips
                    SET player_name = ?, ip_address = ?, last_login_time = ?, login_count = login_count + 1
                    WHERE player_uuid = ?;
                    """;
                try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                    pstmt.setString(1, playerName);
                    pstmt.setString(2, ip);
                    pstmt.setLong(3, currentTime);
                    pstmt.setString(4, playerUuid);
                    pstmt.executeUpdate();
                } catch (SQLException e) {
                    plugin.getLogger().severe("更新玩家IP记录失败: " + e.getMessage());
                }
            }
        }
    }
}
