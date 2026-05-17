package top.mcocet.orcak;

import java.util.UUID;

/**
 * 玩家统计数据类
 */
public class PlayerStats {
    
    private final UUID playerId;
    private final String playerName;
    private long playTime; // 游玩时间（秒）
    private long lastLoginTime; // 上次登录时间（时间戳）
    private int kills; // 击杀数量
    private int deaths; // 被击杀数量
    
    public PlayerStats(UUID playerId, String playerName) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.playTime = 0;
        this.lastLoginTime = System.currentTimeMillis();
        this.kills = 0;
        this.deaths = 0;
    }
    
    public PlayerStats(UUID playerId, String playerName, long playTime, long lastLoginTime, int kills, int deaths) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.playTime = playTime;
        this.lastLoginTime = lastLoginTime;
        this.kills = kills;
        this.deaths = deaths;
    }
    
    // Getters and Setters
    public UUID getPlayerId() {
        return playerId;
    }
    
    public String getPlayerName() {
        return playerName;
    }
    
    public long getPlayTime() {
        return playTime;
    }
    
    public void setPlayTime(long playTime) {
        this.playTime = playTime;
    }
    
    public void addPlayTime(long seconds) {
        this.playTime += seconds;
    }
    
    public long getLastLoginTime() {
        return lastLoginTime;
    }
    
    public void setLastLoginTime(long lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }
    
    public int getKills() {
        return kills;
    }
    
    public void setKills(int kills) {
        this.kills = kills;
    }
    
    public void addKill() {
        this.kills++;
    }
    
    public int getDeaths() {
        return deaths;
    }
    
    public void setDeaths(int deaths) {
        this.deaths = deaths;
    }
    
    public void addDeath() {
        this.deaths++;
    }
    
    @Override
    public String toString() {
        return String.format("PlayerStats{player=%s, playTime=%ds, kills=%d, deaths=%d}", 
            playerName, playTime, kills, deaths);
    }
}
