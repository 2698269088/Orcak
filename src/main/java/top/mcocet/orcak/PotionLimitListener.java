package top.mcocet.orcak;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.inventory.meta.PotionMeta;

import java.util.List;

/**
 * 药水效果限制监听器
 * 限制药水效果的等级和持续时间，防止高等级药水造成的整数溢出伤害
 * 例如：瞬间治疗125级会因溢出变成巨大伤害
 */
public class PotionLimitListener implements Listener {

    private final Orcak plugin;
    private final ConfigManager configManager;

    public PotionLimitListener(Orcak plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    /**
     * 监听实体药水效果事件（最高优先级，拦截所有药水效果应用）
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityPotionEffect(EntityPotionEffectEvent event) {
        if (!isPotionLimitEnabled()) {
            return;
        }

        // 只处理玩家
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();

        // 检查是否为管理员，如果是则跳过限制
        if (shouldBypassForAdmins() && isAdmin(player)) {
            return;
        }

        PotionEffect effect = event.getNewEffect();
        if (effect == null) {
            return;
        }

        PotionEffectType type = effect.getType();
        int level = effect.getAmplifier() + 1; // Bukkit的amplifier是0-based，需要+1
        int duration = effect.getDuration() / 20; // 转换为秒

        // 检查是否在黑名单中
        if (isEffectBlocked(type)) {
            event.setCancelled(true);
            logBlocked(player, type.getName(), level, duration, "在禁止列表中");
            notifyPlayer(player);
            return;
        }

        // 检查等级限制
        if (level > getMaxLevel()) {
            event.setCancelled(true);
            logBlocked(player, type.getName(), level, duration, "等级超过限制");
            notifyPlayer(player);
            return;
        }

        // 检查持续时间限制
        if (duration > getMaxDuration()) {
            event.setCancelled(true);
            logBlocked(player, type.getName(), level, duration, "持续时间超过限制");
            notifyPlayer(player);
            return;
        }
    }

    /**
     * 监听喷溅药水事件（额外层保护）
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPotionSplash(PotionSplashEvent event) {
        if (!isPotionLimitEnabled()) {
            return;
        }

        // 检查投掷者是否为管理员
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (shouldBypassForAdmins() && isAdmin(player)) {
                return;
            }
        }

        // 检查药水效果
        for (PotionEffect effect : event.getPotion().getEffects()) {
            PotionEffectType type = effect.getType();
            int level = effect.getAmplifier() + 1;
            int duration = effect.getDuration() / 20;

            // 如果药水效果超过限制，取消整个喷溅事件
            if (isEffectBlocked(type) || level > getMaxLevel() || duration > getMaxDuration()) {
                event.setCancelled(true);

                String reason = "";
                if (isEffectBlocked(type)) reason = "在禁止列表中";
                else if (level > getMaxLevel()) reason = "等级超过限制";
                else reason = "持续时间超过限制";

                logBlocked(event.getEntity() instanceof Player ? (Player) event.getEntity() : null,
                        type.getName(), level, duration, reason);
                break;
            }
        }
    }

    /**
     * 监听玩家饮用药水事件（额外层保护）
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerConsumePotion(PlayerItemConsumeEvent event) {
        if (!isPotionLimitEnabled()) {
            return;
        }

        Player player = event.getPlayer();

        // 检查是否为管理员，如果是则跳过限制
        if (shouldBypassForAdmins() && isAdmin(player)) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null || !item.getType().toString().contains("POTION")) {
            return;
        }

        // 尝试获取药水效果
        try {
            PotionMeta meta = (PotionMeta) item.getItemMeta();
            if (meta != null && meta.hasCustomEffects()) {
                for (PotionEffect effect : meta.getCustomEffects()) {
                    PotionEffectType type = effect.getType();
                    int level = effect.getAmplifier() + 1;
                    int duration = effect.getDuration() / 20;

                    if (isEffectBlocked(type) || level > getMaxLevel() || duration > getMaxDuration()) {
                        event.setCancelled(true);

                        String reason = "";
                        if (isEffectBlocked(type)) reason = "在禁止列表中";
                        else if (level > getMaxLevel()) reason = "等级超过限制";
                        else reason = "持续时间超过限制";

                        logBlocked(player, type.getName(), level, duration, reason);
                        notifyPlayer(player);
                        return;
                    }
                }
            }
        } catch (Exception e) {
            // 处理可能的类型转换异常
        }
    }

    /**
     * 检查玩家是否为管理员
     */
    private boolean isAdmin(Player player) {
        if (player == null) return false;
        return player.isOp() || player.hasPermission("orcak.potion.bypass");
    }

    /**
     * 检查效果是否在禁止列表中
     */
    private boolean isEffectBlocked(PotionEffectType type) {
        List<String> blockedEffects = configManager.getConfig().getStringList("potion-limit.blocked-effects");
        return blockedEffects.contains(type.getName());
    }

    /**
     * 记录被拦截的药水效果
     */
    private void logBlocked(Player player, String effectName, int level, int duration, String reason) {
        if (!shouldLogBlocked()) {
            return;
        }

        String playerName = player != null ? player.getName() : "未知";
        plugin.getLogger().warning(String.format(
                "[药水限制] 玩家 %s 的药水效果被拦截: 效果=%s, 等级=%d, 持续时间=%ds, 原因=%s",
                playerName, effectName, level, duration, reason));
    }

    /**
     * 通知玩家药水效果被拦截
     */
    private void notifyPlayer(Player player) {
        if (!shouldNotifyPlayer() || player == null) {
            return;
        }

        String message = colorize(getNotifyMessage());

        if (plugin.isFolia()) {
            player.getScheduler().run(plugin, (task) -> {
                player.sendMessage(message);
            }, null);
        } else {
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                player.sendMessage(message);
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
     * 是否启用药水限制
     */
    private boolean isPotionLimitEnabled() {
        return configManager.getConfig().getBoolean("potion-limit.enabled", true);
    }

    /**
     * 获取最大允许等级
     */
    private int getMaxLevel() {
        return configManager.getConfig().getInt("potion-limit.max-level", 5);
    }

    /**
     * 获取最大允许持续时间（秒）
     */
    private int getMaxDuration() {
        return configManager.getConfig().getInt("potion-limit.max-duration", 3600);
    }

    /**
     * 管理员是否不受限制
     */
    private boolean shouldBypassForAdmins() {
        return configManager.getConfig().getBoolean("potion-limit.bypass-for-admins", true);
    }

    /**
     * 是否记录被拦截的药水效果
     */
    private boolean shouldLogBlocked() {
        return configManager.getConfig().getBoolean("potion-limit.log-blocked", true);
    }

    /**
     * 是否通知玩家
     */
    private boolean shouldNotifyPlayer() {
        return configManager.getConfig().getBoolean("potion-limit.notify-player", true);
    }

    /**
     * 获取通知消息
     */
    private String getNotifyMessage() {
        return configManager.getConfig().getString("potion-limit.notify-message",
                "&c你的药水效果等级或持续时间超过服务器限制，已被拦截！");
    }
}