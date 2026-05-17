package top.mcocet.orcak;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.*;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * SQLite数据库管理类
 * 负责玩家数据的存储和读取
 */
public class DatabaseManager {
    
    private final JavaPlugin plugin;
    private Connection connection;
    private final Map<UUID, PlayerStats> playerStatsCache;
    private final Map<UUID, Long> playerLoginTimes; // 记录玩家登录时间
    
    public DatabaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.playerStatsCache = new ConcurrentHashMap<>();
        this.playerLoginTimes = new ConcurrentHashMap<>();
        initializeDatabase();
    }
    
    /**
     * 初始化数据库连接和表结构
     */
    private void initializeDatabase() {
        try {
            // 创建数据库文件路径
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            
            File dbFile = new File(dataFolder, "players.db");
            
            // 加载SQLite驱动并建立连接
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            
            // 启用WAL模式以提高并发性能
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA journal_mode=WAL;");
                stmt.execute("PRAGMA foreign_keys=ON;");
            }
            
            // 创建玩家数据表
            createTables();
            
            plugin.getLogger().info("SQLite数据库初始化成功！");
            
        } catch (ClassNotFoundException e) {
            plugin.getLogger().severe("无法找到SQLite驱动: " + e.getMessage());
        } catch (SQLException e) {
            plugin.getLogger().severe("数据库初始化失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 创建数据表
     */
    private void createTables() throws SQLException {
        String createTableSQL = """
            CREATE TABLE IF NOT EXISTS player_stats (
                player_uuid TEXT PRIMARY KEY,
                player_name TEXT NOT NULL,
                play_time INTEGER DEFAULT 0,
                last_login_time INTEGER DEFAULT 0,
                kills INTEGER DEFAULT 0,
                deaths INTEGER DEFAULT 0,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            );
            """;
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTableSQL);
        }
    }
    
    /**
     * 获取数据库连接
     */
    public Connection getConnection() {
        return connection;
    }
    
    /**
     * 保存或更新玩家统计数据
     */
    public synchronized void savePlayerStats(PlayerStats stats) {
        String sql = """
            INSERT INTO player_stats (player_uuid, player_name, play_time, last_login_time, kills, deaths, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
            ON CONFLICT(player_uuid) DO UPDATE SET
                player_name = excluded.player_name,
                play_time = excluded.play_time,
                last_login_time = excluded.last_login_time,
                kills = excluded.kills,
                deaths = excluded.deaths,
                updated_at = CURRENT_TIMESTAMP;
            """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, stats.getPlayerId().toString());
            pstmt.setString(2, stats.getPlayerName());
            pstmt.setLong(3, stats.getPlayTime());
            pstmt.setLong(4, stats.getLastLoginTime());
            pstmt.setInt(5, stats.getKills());
            pstmt.setInt(6, stats.getDeaths());
            pstmt.executeUpdate();
            
            // 更新缓存
            playerStatsCache.put(stats.getPlayerId(), stats);
            
        } catch (SQLException e) {
            plugin.getLogger().severe("保存玩家数据失败: " + stats.getPlayerName() + " - " + e.getMessage());
        }
    }
    
    /**
     * 从数据库加载玩家统计数据
     */
    public synchronized PlayerStats loadPlayerStats(UUID playerId, String playerName) {
        // 先检查缓存
        if (playerStatsCache.containsKey(playerId)) {
            return playerStatsCache.get(playerId);
        }
        
        String sql = "SELECT * FROM player_stats WHERE player_uuid = ?;";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerId.toString());
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    PlayerStats stats = new PlayerStats(
                        playerId,
                        playerName,
                        rs.getLong("play_time"),
                        rs.getLong("last_login_time"),
                        rs.getInt("kills"),
                        rs.getInt("deaths")
                    );
                    
                    // 加入缓存
                    playerStatsCache.put(playerId, stats);
                    return stats;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("加载玩家数据失败: " + playerName + " - " + e.getMessage());
        }
        
        // 如果数据库中不存在，创建新的统计数据
        PlayerStats newStats = new PlayerStats(playerId, playerName);
        playerStatsCache.put(playerId, newStats);
        return newStats;
    }
    
    /**
     * 记录玩家登录
     */
    public void onPlayerLogin(UUID playerId, String playerName) {
        playerLoginTimes.put(playerId, System.currentTimeMillis());
        
        // 加载或创建玩家数据
        PlayerStats stats = loadPlayerStats(playerId, playerName);
        stats.setLastLoginTime(System.currentTimeMillis());
        savePlayerStats(stats);
    }
    
    /**
     * 记录玩家登出
     */
    public void onPlayerLogout(UUID playerId, String playerName) {
        Long loginTime = playerLoginTimes.remove(playerId);
        if (loginTime != null) {
            long sessionTime = (System.currentTimeMillis() - loginTime) / 1000; // 转换为秒
            
            PlayerStats stats = playerStatsCache.get(playerId);
            if (stats != null) {
                stats.addPlayTime(sessionTime);
                savePlayerStats(stats);
            }
        }
    }
    
    /**
     * 记录玩家击杀
     */
    public void onPlayerKill(UUID killerId, String killerName) {
        PlayerStats stats = playerStatsCache.get(killerId);
        if (stats != null) {
            stats.addKill();
            savePlayerStats(stats);
        }
    }
    
    /**
     * 记录玩家死亡
     */
    public void onPlayerDeath(UUID playerId, String playerName) {
        PlayerStats stats = playerStatsCache.get(playerId);
        if (stats != null) {
            stats.addDeath();
            savePlayerStats(stats);
        }
    }
    
    /**
     * 获取玩家统计数据（包含实时在线时长）
     */
    public PlayerStats getPlayerStats(UUID playerId) {
        PlayerStats stats = playerStatsCache.get(playerId);
        if (stats != null) {
            // 如果玩家在线，计算当前会话的实时时长
            Long loginTime = playerLoginTimes.get(playerId);
            if (loginTime != null) {
                long currentSessionTime = (System.currentTimeMillis() - loginTime) / 1000;
                // 返回一个包含实时时长的副本，避免污染缓存中的基础数据
                PlayerStats realTimeStats = new PlayerStats(
                    stats.getPlayerId(), 
                    stats.getPlayerName(), 
                    stats.getPlayTime() + currentSessionTime, 
                    stats.getLastLoginTime(), 
                    stats.getKills(), 
                    stats.getDeaths()
                );
                return realTimeStats;
            }
        }
        return stats;
    }
    
    /**
     * 关闭数据库连接
     */
    public void close() {
        // 保存所有在线玩家的数据
        playerStatsCache.values().forEach(this::savePlayerStats);
        
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("数据库连接已关闭");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("关闭数据库连接失败: " + e.getMessage());
        }
    }
    
    /**
     * 从原版 JSON 统计文件同步数据到插件数据库（单个玩家）
     */
    public synchronized int syncFromVanilla(UUID playerId, String playerName) {
        File statsDir = new File(plugin.getServer().getWorlds().get(0).getWorldFolder(), "stats");
        File statFile = new File(statsDir, playerId.toString() + ".json");
        
        if (!statFile.exists()) {
            return -1; // 文件不存在
        }
        
        try {
            return parseAndSaveStats(statFile, playerId, playerName);
        } catch (Exception e) {
            plugin.getLogger().severe("同步玩家 " + playerName + " 的原版数据失败: " + e.getMessage());
            return 0; // 发生错误
        }
    }
    
    /**
     * 同步所有玩家的原版数据
     */
    public synchronized int syncAllFromVanilla() {
        File statsDir = new File(plugin.getServer().getWorlds().get(0).getWorldFolder(), "stats");
        if (!statsDir.exists()) {
            return 0;
        }
        
        File[] files = statsDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null || files.length == 0) {
            return 0;
        }
        
        int successCount = 0;
        for (File file : files) {
            try {
                String uuidStr = file.getName().replace(".json", "");
                UUID playerId = UUID.fromString(uuidStr);
                String playerName = plugin.getServer().getOfflinePlayer(playerId).getName();
                if (playerName == null) playerName = "Unknown";
                
                if (parseAndSaveStats(file, playerId, playerName) == 1) {
                    successCount++;
                }
            } catch (Exception e) {
                plugin.getLogger().warning("同步文件 " + file.getName() + " 时出错: " + e.getMessage());
            }
        }
        return successCount;
    }
    
    /**
     * 解析并保存统计数据的通用方法
     */
    private int parseAndSaveStats(File statFile, UUID playerId, String playerName) throws Exception {
        JSONParser parser = new JSONParser();
        JSONObject root = (JSONObject) parser.parse(new FileReader(statFile));
        JSONObject stats = (JSONObject) root.get("stats");
        
        long playTimeTicks = 0;
        int kills = 0;
        int deaths = 0;
        
        if (stats != null) {
            // 解析游玩时间 (minecraft:play_time)
            JSONObject general = (JSONObject) stats.get("minecraft:custom");
            if (general != null) {
                Number time = (Number) general.get("minecraft:play_one_minute");
                if (time != null) playTimeTicks = time.longValue();
            }
            
            // 解析击杀数 (minecraft:killed)
            JSONObject killed = (JSONObject) stats.get("minecraft:killed");
            if (killed != null) {
                for (Object value : killed.values()) {
                    kills += ((Number) value).intValue();
                }
            }
            
            // 解析死亡数 (minecraft:deaths)
            Number deathCount = (Number) ((JSONObject) stats.getOrDefault("minecraft:custom", new JSONObject())).get("minecraft:deaths");
            if (deathCount != null) deaths = deathCount.intValue();
            
            // 解析玩家击杀数 (minecraft:player_kills)
            Number playerKills = (Number) ((JSONObject) stats.getOrDefault("minecraft:custom", new JSONObject())).get("minecraft:player_kills");
            if (playerKills != null) kills = playerKills.intValue(); // 优先使用专门的玩家击杀数
        }
        
        long playTimeSeconds = playTimeTicks / 20;
        
        PlayerStats existingStats = loadPlayerStats(playerId, playerName);
        existingStats.setPlayTime(playTimeSeconds);
        existingStats.setKills(kills);
        existingStats.setDeaths(deaths);
        savePlayerStats(existingStats);
        
        return 1;
    }
    
    /**
     * 手动修改玩家统计数据
     */
    public synchronized boolean updatePlayerStat(UUID playerId, String playerName, String field, long value) {
        PlayerStats stats = loadPlayerStats(playerId, playerName);
        
        switch (field.toLowerCase()) {
            case "playtime":
            case "time":
                stats.setPlayTime(value);
                break;
            case "kills":
            case "kill":
                stats.setKills((int) value);
                break;
            case "deaths":
            case "death":
                stats.setDeaths((int) value);
                break;
            default:
                return false;
        }
        
        savePlayerStats(stats);
        return true;
    }
}
