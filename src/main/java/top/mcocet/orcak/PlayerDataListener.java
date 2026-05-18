package top.mcocet.orcak;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

/**
 * 玩家数据监听器
 * 记录玩家的登录、登出、击杀和死亡事件
 */
public class PlayerDataListener implements Listener {
    
    private final Orcak plugin;
    private final DatabaseManager databaseManager;
    
    public PlayerDataListener(Orcak plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }
    
    /**
     * 玩家加入服务器
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        databaseManager.onPlayerLogin(uuid, player.getName());
        
        // 启动实时计时任务 (Folia 兼容)
        if (plugin.isFolia()) {
            player.getScheduler().runAtFixedRate(plugin, (task) -> {
                if (player.isOnline()) {
                    databaseManager.updatePlayTime(uuid);
                }
            }, null, 1L, 20L); // 初始延迟1 tick，之后每20 ticks (1秒) 执行一次
        } else {
            org.bukkit.Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
                if (player.isOnline()) {
                    databaseManager.updatePlayTime(uuid);
                }
            }, 0L, 20L);
        }
    }
    
    /**
     * 玩家离开服务器
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        databaseManager.onPlayerLogout(player.getUniqueId(), player.getName());
    }
    
    /**
     * 实体受到伤害（用于统计击杀）
     */
    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        // 检查是否是玩家击杀玩家
        if (event.getDamager() instanceof Player && event.getEntity() instanceof Player) {
            Player damager = (Player) event.getDamager();
            Player victim = (Player) event.getEntity();
            
            // 如果伤害足以致命，记录击杀（实际死亡在PlayerDeathEvent中处理更准确）
            // 这里先不处理，等待死亡事件
        }
    }
    
    /**
     * 玩家死亡事件
     */
    @EventHandler
    public void onPlayerDeath(org.bukkit.event.entity.PlayerDeathEvent event) {
        Player victim = event.getEntity();
        
        // 记录受害者死亡
        databaseManager.onPlayerDeath(victim.getUniqueId(), victim.getName());
        
        // 检查是否有击杀者
        if (victim.getKiller() != null) {
            Player killer = victim.getKiller();
            databaseManager.onPlayerKill(killer.getUniqueId(), killer.getName());
        }
    }
}
