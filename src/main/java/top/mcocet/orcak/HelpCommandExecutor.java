package top.mcocet.orcak;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

/**
 * Help命令监听器
 * 支持Folia的区域化线程调度
 * 通过监听命令事件来接管普通玩家的help命令
 */
public class HelpCommandExecutor implements Listener {
    
    private final Orcak plugin;
    private final ConfigManager configManager;
    
    public HelpCommandExecutor(Orcak plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }
    
    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        
        // 检查是否是 help 命令（支持 /help, /minecraft:help 等）
        if (!message.toLowerCase().startsWith("/help") && !message.toLowerCase().startsWith("/minecraft:help")) {
            return; // 不是 help 命令，不处理
        }
        
        // 解析命令参数
        String[] parts = message.split(" ");
        boolean isOrcakHelp = parts.length > 1 && parts[1].equalsIgnoreCase("orcak");
        
        // 如果玩家输入了 /help orcak，直接显示插件帮助内容
        if (isOrcakHelp) {
            event.setCancelled(true);
            sendCustomHelp(player);
            return;
        }
        
        // 如果玩家有 bypass 权限，不拦截，让原版处理
        if (player.isOp() || player.hasPermission("orcak.bypass.help")) {
            // plugin.getLogger().info("管理员 " + player.getName() + " 使用原版help命令");
            return; // 不取消事件，让原版处理
        }
        
        // 普通玩家，取消原版命令执行并显示自定义帮助
        event.setCancelled(true);
        sendCustomHelp(player);
    }
    
    /**
     * 发送自定义帮助内容
     */
    private void sendCustomHelp(Player player) {
        // 获取help.txt文件内容
        String helpContent = configManager.getHelpContent();

        // 将内容按行分割并发送给玩家
        String[] lines = helpContent.split("\n");
        int sentLines = 0;
        for (String line : lines) {
            if (!line.trim().isEmpty()) {
                player.sendMessage(line);
                sentLines++;
            }
        }
    }
}
