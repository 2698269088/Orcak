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
    private String nameColor; // 玩家名字颜色
    private String messageColor; // 聊天消息颜色
    private boolean isMuted; // 是否被禁言
    
    public PlayerStats(UUID playerId, String playerName) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.playTime = 0;
        this.lastLoginTime = System.currentTimeMillis();
        this.kills = 0;
        this.deaths = 0;
        this.nameColor = "&f"; // 默认白色
        this.messageColor = "&7"; // 默认灰色
        this.isMuted = false; // 默认不禁言
    }
    
    public PlayerStats(UUID playerId, String playerName, long playTime, long lastLoginTime, int kills, int deaths) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.playTime = playTime;
        this.lastLoginTime = lastLoginTime;
        this.kills = kills;
        this.deaths = deaths;
        this.nameColor = "&f";
        this.messageColor = "&7";
        this.isMuted = false;
    }
    
    public PlayerStats(UUID playerId, String playerName, long playTime, long lastLoginTime, int kills, int deaths, String nameColor, String messageColor) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.playTime = playTime;
        this.lastLoginTime = lastLoginTime;
        this.kills = kills;
        this.deaths = deaths;
        this.nameColor = nameColor != null ? nameColor : "&f";
        this.messageColor = messageColor != null ? messageColor : "&7";
        this.isMuted = false;
    }
    
    public PlayerStats(UUID playerId, String playerName, long playTime, long lastLoginTime, int kills, int deaths, String nameColor, String messageColor, boolean isMuted) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.playTime = playTime;
        this.lastLoginTime = lastLoginTime;
        this.kills = kills;
        this.deaths = deaths;
        this.nameColor = nameColor != null ? nameColor : "&f";
        this.messageColor = messageColor != null ? messageColor : "&7";
        this.isMuted = isMuted;
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
    
    public String getNameColor() {
        return nameColor;
    }
    
    public void setNameColor(String nameColor) {
        this.nameColor = nameColor;
    }
    
    public String getMessageColor() {
        return messageColor;
    }
    
    public void setMessageColor(String messageColor) {
        this.messageColor = messageColor;
    }
    
    public boolean isMuted() {
        return isMuted;
    }
    
    public void setMuted(boolean muted) {
        this.isMuted = muted;
    }
    
    @Override
    public String toString() {
        return String.format("PlayerStats{player=%s, playTime=%ds, kills=%d, deaths=%d}", 
            playerName, playTime, kills, deaths);
    }
}
