package top.mcocet.orcak;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

/**
 * 聊天颜色监听器
 * 为玩家聊天消息应用自定义颜色
 */
public class ChatColorListener implements Listener {
    
    private final Orcak plugin;
    private final DatabaseManager databaseManager;
    
    public ChatColorListener(Orcak plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }
    
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        PlayerStats stats = databaseManager.getPlayerStats(player.getUniqueId());
        
        if (stats != null) {
            // 获取配置中的默认颜色
            String defaultNameColor = plugin.getConfigManager().getConfig()
                .getString("chat-colors.default-name-color", "&f");
            String defaultMessageColor = plugin.getConfigManager().getConfig()
                .getString("chat-colors.default-message-color", "&7");
            
            // 使用玩家自定义颜色或默认颜色
            String nameColor = stats.getNameColor() != null ? stats.getNameColor() : defaultNameColor;
            String messageColor = stats.getMessageColor() != null ? stats.getMessageColor() : defaultMessageColor;
            
            // 转换颜色代码 (& -> §)
            nameColor = translateColorCodes(nameColor);
            messageColor = translateColorCodes(messageColor);
            
            // 设置聊天格式: [名字颜色]玩家名 §r>[消息颜色] 消息内容
            String format = "<" + nameColor + "%1$s" + "§r>" + messageColor + " %2$s";
            event.setFormat(format);
        }
    }
    
    /**
     * 转换颜色代码 (& -> §)
     */
    private String translateColorCodes(String text) {
        if (text == null) return "";
        return text.replace('&', '§');
    }
}
