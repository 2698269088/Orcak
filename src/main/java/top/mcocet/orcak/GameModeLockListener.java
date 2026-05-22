package top.mcocet.orcak;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 游戏模式锁定监听器
 * 防止被锁定的玩家游戏模式被修改
 */
public class GameModeLockListener implements Listener {
    
    private final Orcak plugin;
    
    // 存储被锁定的玩家：key=玩家UUID, value=锁定的游戏模式
    private static final Map<UUID, GameMode> lockedPlayers = new ConcurrentHashMap<>();
    
    public GameModeLockListener(Orcak plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        // 检查玩家是否被锁定
        if (lockedPlayers.containsKey(playerId)) {
            GameMode lockedMode = lockedPlayers.get(playerId);
            
            // 如果尝试修改为不同的游戏模式，则取消事件
            if (event.getNewGameMode() != lockedMode) {
                event.setCancelled(true);
                
                // 通知玩家
                player.sendMessage("§c你的游戏模式已被管理员锁定，无法修改！");
                player.sendMessage("§7当前锁定模式: " + getGameModeName(lockedMode));
                
                // 记录日志
                plugin.getLogger().info("[GameModeLock] " + player.getName() + " 尝试修改游戏模式但被阻止（锁定为: " + getGameModeName(lockedMode) + ")");
            }
        }
    }
    
    /**
     * 锁定玩家的游戏模式
     */
    public static void lockPlayer(Player player, GameMode gameMode) {
        lockedPlayers.put(player.getUniqueId(), gameMode);
    }
    
    /**
     * 解锁玩家的游戏模式
     */
    public static void unlockPlayer(Player player) {
        lockedPlayers.remove(player.getUniqueId());
    }
    
    /**
     * 检查玩家是否被锁定
     */
    public static boolean isLocked(Player player) {
        return lockedPlayers.containsKey(player.getUniqueId());
    }
    
    /**
     * 获取玩家锁定的游戏模式
     */
    public static GameMode getLockedMode(Player player) {
        return lockedPlayers.get(player.getUniqueId());
    }
    
    /**
     * 获取所有被锁定的玩家数量
     */
    public static int getLockedCount() {
        return lockedPlayers.size();
    }
    
    /**
     * 获取游戏模式的中文名称
     */
    private String getGameModeName(GameMode mode) {
        switch (mode) {
            case SURVIVAL:
                return "生存模式";
            case CREATIVE:
                return "创造模式";
            case ADVENTURE:
                return "冒险模式";
            case SPECTATOR:
                return "旁观模式";
            default:
                return mode.toString();
        }
    }
}
