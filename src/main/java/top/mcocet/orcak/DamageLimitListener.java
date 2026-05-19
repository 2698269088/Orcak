package top.mcocet.orcak;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * 玩家伤害限制监听器
 * 限制玩家单次攻击造成的最大伤害值，防止一击必杀等不平衡情况
 * 默认关闭，管理员不受限制
 */
public class DamageLimitListener implements Listener {
    
    private final Orcak plugin;
    private final ConfigManager configManager;
    
    public DamageLimitListener(Orcak plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }
    
    /**
     * 监听实体被实体伤害事件
     * 使用HIGHEST优先级，确保在其他插件之后处理，避免冲突
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!isDamageLimitEnabled()) {
            return;
        }
        
        Entity damager = event.getDamager();
        
        // 只处理玩家造成的伤害
        if (!(damager instanceof Player)) {
            return;
        }
        
        Player player = (Player) damager;
        
        // 检查是否为管理员，如果是则跳过限制
        if (shouldBypassForAdmins() && isAdmin(player)) {
            return;
        }
        
        double originalDamage = event.getDamage();
        double maxDamage = getMaxDamage();
        
        // 如果原始伤害超过限制，则调整伤害值
        if (originalDamage > maxDamage) {
            event.setDamage(maxDamage);
            
            // 发送警告消息给玩家
            if (shouldSendWarning()) {
                sendWarning(player, originalDamage, maxDamage);
            }
        }
    }
    
    /**
     * 检查玩家是否为管理员
     */
    private boolean isAdmin(Player player) {
        // 检查是否有op权限或管理员权限
        return player.isOp() || player.hasPermission("orcak.damage.bypass");
    }
    
    /**
     * 发送警告消息给玩家
     */
    private void sendWarning(Player player, double originalDamage, double maxDamage) {
        String messageTemplate = getWarningMessage();
        
        // 替换占位符
        final String message = messageTemplate.replace("{damage}", String.format("%.1f", originalDamage))
            .replace("{max}", String.format("%.1f", maxDamage));
        
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
     * 是否启用伤害限制
     */
    private boolean isDamageLimitEnabled() {
        return configManager.getConfig().getBoolean("damage-limit.enabled", false);
    }
    
    /**
     * 获取最大伤害值
     */
    private double getMaxDamage() {
        return configManager.getConfig().getDouble("damage-limit.max-damage", 100.0);
    }
    
    /**
     * 管理员是否不受限制
     */
    private boolean shouldBypassForAdmins() {
        return configManager.getConfig().getBoolean("damage-limit.bypass-for-admins", true);
    }
    
    /**
     * 是否发送警告消息
     */
    private boolean shouldSendWarning() {
        return configManager.getConfig().getBoolean("damage-limit.send-warning", true);
    }
    
    /**
     * 获取警告消息模板
     */
    private String getWarningMessage() {
        return configManager.getConfig().getString("damage-limit.warning-message", 
            "&c你的攻击伤害过高！原始伤害: {damage}, 限制为: {max}");
    }
}
