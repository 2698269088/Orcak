package top.mcocet.orcak;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

/**
 * 死亡消息监听器
 * 接管原版死亡消息，使用自定义配置的消息
 */
public class DeathMessageListener implements Listener {
    
    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    
    public DeathMessageListener(JavaPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        FileConfiguration config = configManager.getConfig();
        
        // 检查是否启用自定义死亡消息
        boolean enabled = config.getBoolean("death-messages.enabled", false);
        
        if (!enabled) {
            return;
        }
        
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        // 获取死亡原因
        EntityDamageEvent damageEvent = victim.getLastDamageCause();
        EntityDamageEvent.DamageCause damageCause = damageEvent != null 
                ? damageEvent.getCause() 
                : null;
        
        if (damageCause == null) {
            return;
        }
        
        // 如果是生物攻击但没有killer，尝试从伤害事件中获取攻击者
        Entity attacker = null;
        if (killer == null && damageEvent instanceof EntityDamageByEntityEvent) {
            attacker = ((EntityDamageByEntityEvent) damageEvent).getDamager();
        }
        
        // 根据伤害类型和情况获取自定义消息
        String customMessage = getCustomDeathMessage(config, victim, killer, damageCause, event);
        
        if (customMessage != null && !customMessage.isEmpty()) {
            // 替换占位符
            customMessage = replacePlaceholders(customMessage, victim, killer, event);

            
            // 设置自定义死亡消息（空字符串会隐藏原版消息）
            event.setDeathMessage(customMessage);

        }
    }
    
    /**
     * 根据伤害类型获取自定义死亡消息
     */
    private String getCustomDeathMessage(FileConfiguration config, Player victim, Player killer, 
                                         EntityDamageEvent.DamageCause damageCause, PlayerDeathEvent event) {
        String path = "death-messages.";
        
        // 判断是否有击杀者
        boolean hasKiller = killer != null;
        
        // 判断是否有物品（命名物品）
        String itemName = null;
        if (hasKiller && killer.getInventory().getItemInMainHand() != null 
                && killer.getInventory().getItemInMainHand().hasItemMeta()
                && killer.getInventory().getItemInMainHand().getItemMeta().hasDisplayName()) {
            itemName = killer.getInventory().getItemInMainHand().getItemMeta().getDisplayName();
        }
        
        boolean hasItem = itemName != null && !itemName.isEmpty();
        
        // 根据不同的伤害类型返回对应的配置路径
        switch (damageCause) {
            case ENTITY_ATTACK:
            case ENTITY_SWEEP_ATTACK:
                if (hasKiller) {
                    if (hasItem) {
                        return config.getString(path + "violence.player-item");
                    } else {
                        return config.getString(path + "violence.player");
                    }
                } else {
                    // 生物攻击
                    if (hasItem) {
                        return config.getString(path + "violence.mob-item");
                    } else {
                        return config.getString(path + "violence.mob");
                    }
                }
                
            case PROJECTILE:
                // 需要判断具体是什么投射物
                return config.getString(path + "violence.arrow");
                
            case FIRE:
            case FIRE_TICK:
                if (hasKiller) {
                    if (hasItem) {
                        return config.getString(path + "negative-effects.onFire-item");
                    } else {
                        return config.getString(path + "negative-effects.onFire-player");
                    }
                } else {
                    return config.getString(path + "negative-effects.onFire");
                }
                
            case LAVA:
                if (hasKiller) {
                    return config.getString(path + "dangerous-environment.lava-player");
                } else {
                    return config.getString(path + "dangerous-environment.lava");
                }
                
            case DROWNING:
                if (hasKiller) {
                    return config.getString(path + "dangerous-environment.drown-player");
                } else {
                    return config.getString(path + "dangerous-environment.drown");
                }
                
            case FALL:
                // 摔落伤害需要特殊处理
                return handleFallDamage(config, victim, killer);
                
            case VOID:
                if (hasKiller) {
                    return config.getString(path + "dangerous-environment.outOfWorld-player");
                } else {
                    return config.getString(path + "dangerous-environment.outOfWorld");
                }
                
            case SUFFOCATION:
                if (hasKiller) {
                    return config.getString(path + "dangerous-environment.inWall-player");
                } else {
                    return config.getString(path + "dangerous-environment.inWall");
                }
                
            case CONTACT:
                // 仙人掌等接触伤害
                return config.getString(path + "dangerous-environment.cactus");
                
            case CUSTOM:
            case MAGIC:
                if (hasKiller) {
                    return config.getString(path + "negative-effects.magic-player");
                } else {
                    return config.getString(path + "negative-effects.magic");
                }
                
            case STARVATION:
                if (hasKiller) {
                    return config.getString(path + "negative-effects.starve-player");
                } else {
                    return config.getString(path + "negative-effects.starve");
                }
                
            case POISON:
            case WITHER:
                if (hasKiller) {
                    return config.getString(path + "negative-effects.wither-player");
                } else {
                    return config.getString(path + "negative-effects.wither");
                }
                
            case THORNS:
                if (hasKiller) {
                    if (hasItem) {
                        return config.getString(path + "violence.thorns-item");
                    } else {
                        return config.getString(path + "violence.thorns");
                    }
                }
                break;
                
            case DRAGON_BREATH:
                if (hasKiller) {
                    return config.getString(path + "violence.dragonBreath-player");
                } else {
                    return config.getString(path + "violence.dragonBreath");
                }
                
            case FLY_INTO_WALL:
                if (hasKiller) {
                    return config.getString(path + "accidents.flyIntoWall-player");
                } else {
                    return config.getString(path + "accidents.flyIntoWall");
                }
                
            case HOT_FLOOR:
                if (hasKiller) {
                    return config.getString(path + "dangerous-environment.hotFloor-player");
                } else {
                    return config.getString(path + "dangerous-environment.hotFloor");
                }
                
            case CRAMMING:
                if (hasKiller) {
                    return config.getString(path + "dangerous-environment.cramming-player");
                } else {
                    return config.getString(path + "dangerous-environment.cramming");
                }
                
            case DRYOUT:
                if (hasKiller) {
                    return config.getString(path + "dangerous-environment.dryout-player");
                } else {
                    return config.getString(path + "dangerous-environment.dryout");
                }
                
            case FREEZE:
                if (hasKiller) {
                    return config.getString(path + "dangerous-environment.freeze-player");
                } else {
                    return config.getString(path + "dangerous-environment.freeze");
                }
                
            case LIGHTNING:
                if (hasKiller) {
                    return config.getString(path + "dangerous-environment.lightningBolt-player");
                } else {
                    return config.getString(path + "dangerous-environment.lightningBolt");
                }
                
            case BLOCK_EXPLOSION:
            case ENTITY_EXPLOSION:
                if (hasKiller) {
                    if (hasItem) {
                        return config.getString(path + "explosions.explosion-player-item");
                    } else {
                        return config.getString(path + "explosions.explosion-player");
                    }
                } else {
                    return config.getString(path + "explosions.explosion");
                }
                
            case KILL:
            case SUICIDE:
                if (hasKiller) {
                    return config.getString(path + "miscellaneous.genericKill-player");
                } else {
                    return config.getString(path + "miscellaneous.genericKill");
                }
                
            default:
                // 通用死亡消息
                if (hasKiller) {
                    return config.getString(path + "miscellaneous.generic-player");
                } else {
                    return config.getString(path + "miscellaneous.generic");
                }
        }
        
        return null;
    }
    
    /**
     * 处理摔落伤害的特殊情况
     */
    private String handleFallDamage(FileConfiguration config, Player victim, Player killer) {
        String path = "death-messages.fall-variants.";
        
        // 这里简化处理，实际应该检测玩家最后离开的地面方块类型
        // 由于Minecraft API限制，精确判断比较困难
        
        boolean hasKiller = killer != null;
        
        if (hasKiller) {
            // 有击杀者的情况，可能是被击退摔死
            return config.getString(path + "fell-killer");
        } else {
            // 普通摔死
            return config.getString(path + "fell-accident-generic");
        }
    }
    
    /**
     * 替换消息中的占位符
     */
    private String replacePlaceholders(String message, Player victim, Player killer, PlayerDeathEvent event) {
        if (message == null || message.isEmpty()) {
            return message;
        }
        
        FileConfiguration config = configManager.getConfig();
        
        // 获取颜色配置
        String victimColor = config.getString("death-messages.colors.victim-color", "&d");
        String killerColor = config.getString("death-messages.colors.killer-color", "&b");
        String itemColor = config.getString("death-messages.colors.item-color", "&a");
        String messageColor = config.getString("death-messages.colors.message-color", "&f");
        
        // 转换颜色代码
        victimColor = colorize(victimColor);
        killerColor = colorize(killerColor);
        itemColor = colorize(itemColor);
        messageColor = colorize(messageColor);
        
        // 重置代码（用于在名称后恢复默认颜色）
        String reset = "§r";
        
        // 在整个消息前添加消息颜色
        message = messageColor + message;
        
        // 替换死者名称（带颜色）
        message = message.replace("{victim}", victimColor + victim.getDisplayName() + reset + messageColor);
        
        // 获取实际的攻击者（可能是玩家或生物）
        EntityDamageEvent damageEvent = victim.getLastDamageCause();
        Entity attacker = null;
        if (killer != null) {
            attacker = killer;
        } else if (damageEvent instanceof EntityDamageByEntityEvent) {
            attacker = ((EntityDamageByEntityEvent) damageEvent).getDamager();
        }
        
        // 替换击杀者名称（带颜色）
        if (attacker != null) {
            String attackerName = attacker.getName();
            // 如果是生物，使用自定义名称或类型名称
            if (!(attacker instanceof Player) && attacker.getCustomName() != null) {
                attackerName = attacker.getCustomName();
            }
            message = message.replace("{killer}", killerColor + attackerName + reset + messageColor);
        } else {
            message = message.replace("{killer}", killerColor + "未知" + reset + messageColor);
        }
        
        // 替换物品名称（带颜色）
        if (killer != null && killer.getInventory().getItemInMainHand() != null 
                && killer.getInventory().getItemInMainHand().hasItemMeta()
                && killer.getInventory().getItemInMainHand().getItemMeta().hasDisplayName()) {
            String itemName = killer.getInventory().getItemInMainHand().getItemMeta().getDisplayName();
            message = message.replace("{item}", itemColor + itemName + reset + messageColor);
        } else {
            message = message.replace("{item}", itemColor + "未知物品" + reset + messageColor);
        }
        
        return message;
    }
    
    /**
     * 将颜色代码转换为实际颜色
     */
    private String colorize(String message) {
        return message.replace('&', '§');
    }
}
