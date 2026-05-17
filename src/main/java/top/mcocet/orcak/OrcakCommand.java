package top.mcocet.orcak;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Orcak 母命令执行器
 * 处理 /orcak sync, /orcak set 等子命令
 */
public class OrcakCommand implements CommandExecutor, TabCompleter {
    
    private final Orcak plugin;
    private final DatabaseManager databaseManager;
    
    public OrcakCommand(Orcak plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "sync":
                handleSync(sender, args);
                break;
            case "set":
                handleSet(sender, args);
                break;
            case "help":
            default:
                sendHelp(sender);
                break;
        }
        
        return true;
    }
    
    private void handleSync(CommandSender sender, String[] args) {
        if (!sender.hasPermission("orcak.admin.sync")) {
            sender.sendMessage("§c你没有权限执行此操作。");
            return;
        }
        
        if (args.length < 2) {
            // 同步所有玩家
            sender.sendMessage("§e正在同步所有玩家的原版数据，这可能需要一些时间...");
            int count = databaseManager.syncAllFromVanilla();
            sender.sendMessage("§a同步完成！共成功同步 " + count + " 名玩家的数据。");
            return;
        }
        
        // 同步指定玩家
        String playerName = args[1];
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        
        if (!offlinePlayer.hasPlayedBefore()) {
            sender.sendMessage("§c未找到玩家：" + playerName);
            return;
        }
        
        sender.sendMessage("§e正在同步 " + playerName + " 的原版数据...");
        int result = databaseManager.syncFromVanilla(offlinePlayer.getUniqueId(), playerName);
        
        if (result == 1) {
            sender.sendMessage("§a成功同步 " + playerName + " 的数据！");
        } else if (result == -1) {
            sender.sendMessage("§c该玩家没有原版统计数据文件。");
        } else {
            sender.sendMessage("§c同步过程中发生错误，请查看控制台日志。");
        }
    }
    
    private void handleSet(CommandSender sender, String[] args) {
        if (!sender.hasPermission("orcak.admin.set")) {
            sender.sendMessage("§c你没有权限执行此操作。");
            return;
        }
        
        if (args.length < 4) {
            sender.sendMessage("§c用法: /orcak set <玩家名> <字段> <值>");
            sender.sendMessage("§7字段可选: playtime, kills, deaths");
            return;
        }
        
        String playerName = args[1];
        String field = args[2];
        
        long value;
        try {
            value = Long.parseLong(args[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§c数值格式不正确。");
            return;
        }
        
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        if (!offlinePlayer.hasPlayedBefore()) {
            sender.sendMessage("§c未找到玩家：" + playerName);
            return;
        }
        
        boolean success = databaseManager.updatePlayerStat(offlinePlayer.getUniqueId(), playerName, field, value);
        
        if (success) {
            sender.sendMessage("§a已成功修改 " + playerName + " 的 " + field + " 为 " + value);
        } else {
            sender.sendMessage("§c未知的字段：" + field);
        }
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6========== §eOrcak 管理命令 §6==========");
        sender.sendMessage("§7/orcak sync [玩家] - 同步原版数据（不填则同步全部）");
        sender.sendMessage("§7/orcak set <玩家> <字段> <值> - 手动修改数据");
        sender.sendMessage("§7字段: playtime, kills, deaths");
        sender.sendMessage("§6================================");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            // 补全子命令: sync, set, help
            List<String> subCommands = new ArrayList<>();
            subCommands.add("sync");
            subCommands.add("set");
            subCommands.add("help");
            return subCommands.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2 || args.length == 3) {
            if (args[0].equalsIgnoreCase("sync") || args[0].equalsIgnoreCase("set")) {
                // 补全玩家名称
                String partialName = args[args.length - 1].toLowerCase();
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(partialName))
                        .collect(Collectors.toList());
            }
        } else if (args.length == 4 && args[0].equalsIgnoreCase("set")) {
            // 补全字段名
            List<String> fields = new ArrayList<>();
            fields.add("playtime");
            fields.add("kills");
            fields.add("deaths");
            return fields.stream()
                    .filter(f -> f.toLowerCase().startsWith(args[3].toLowerCase()))
                    .collect(Collectors.toList());
        }
        
        return new ArrayList<>();
    }
}
