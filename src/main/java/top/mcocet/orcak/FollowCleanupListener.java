package top.mcocet.orcak;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * 跟随功能清理监听器
 * 当玩家退出游戏时自动清理跟随任务
 */
public class FollowCleanupListener implements Listener {
    
    private final OrcakCommand orcakCommand;
    
    public FollowCleanupListener(OrcakCommand orcakCommand) {
        this.orcakCommand = orcakCommand;
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // 如果该玩家正在跟随别人，停止跟随
        orcakCommand.stopFollowingIfActive(player);
    }
}
