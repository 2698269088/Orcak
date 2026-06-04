package top.mcocet.orcak;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class PositionLockListener implements Listener {
    private final Orcak plugin;

    public PositionLockListener(Orcak plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        // 检查玩家位置是否被锁定
        if (plugin.isPlayerPositionLocked(player)) {
            Location lockedLocation = plugin.getLockedPosition(player);

            // 检查玩家是否试图移动（忽略Y轴微小变化，因为可能会因跳跃或重力导致Y值改变）
            if (Math.abs(event.getFrom().getX() - lockedLocation.getX()) > 0.01 ||
                    Math.abs(event.getFrom().getZ() - lockedLocation.getZ()) > 0.01) {

                // 将玩家传送回锁定位置
                if (plugin.isFolia()) {
                    player.teleportAsync(lockedLocation);
                } else {
                    player.teleport(lockedLocation);
                }

                // 发送提示消息给玩家
                player.sendMessage("§c你的坐标已被锁定，无法移动！");
            }
        }
    }
}