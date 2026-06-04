package top.mcocet.orcak;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

/**
 * 命令预处理监听器
 * 拦截命令并检查是否需要显示自定义帮助
 */
public class HelpCommandListener implements Listener {

    private final CustomHelpCommand customHelpCommand;

    public HelpCommandListener(CustomHelpCommand customHelpCommand) {
        this.customHelpCommand = customHelpCommand;
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        // 获取命令（去除斜杠）
        String message = event.getMessage().trim();
        if (!message.startsWith("/")) {
            return;
        }

        String commandStr = message.substring(1);
        String[] parts = commandStr.split(" ");

        if (parts.length == 0) {
            return;
        }

        // 获取命令名和参数
        String command = parts[0].toLowerCase();
        String[] args = new String[parts.length - 1];
        System.arraycopy(parts, 1, args, 0, args.length);

        // 检查是否应该处理帮助请求
        if (customHelpCommand.shouldHandle(command, args)) {
            // 拦截命令，显示帮助
            customHelpCommand.handleHelp(event.getPlayer(), command, args);
            event.setCancelled(true);
        }
    }
}
