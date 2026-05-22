package top.mcocet.orcak;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * 自杀命令执行器
 * 支持 /514 和 /k 命令
 */
public class SuicideCommand implements CommandExecutor {
    
    private final Orcak plugin;
    
    public SuicideCommand(Orcak plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c此命令只能由玩家使用。");
            return true;
        }
        
        Player player = (Player) sender;
        
        // 执行自杀
        player.setHealth(0);
        
        // 发送消息
        player.sendMessage("§7你选择了结束自己的生命...");
        
        return true;
    }
}
