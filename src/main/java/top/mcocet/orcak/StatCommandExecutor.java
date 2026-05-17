package top.mcocet.orcak;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Stat命令执行器
 * 用于查看玩家的游戏统计数据
 */
public class StatCommandExecutor implements CommandExecutor, TabCompleter {
    
    private final Orcak plugin;
    private final DatabaseManager databaseManager;
    
    public StatCommandExecutor(Orcak plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player targetPlayer;
        
        // 判断是查看自己还是查看其他玩家
        if (args.length == 0) {
            // 查看自己的数据
            if (!(sender instanceof Player)) {
                sender.sendMessage("§c控制台无法使用此命令，请指定玩家名称：/stat <玩家名>");
                return true;
            }
            targetPlayer = (Player) sender;
        } else {
            // 查看其他玩家的数据
            String targetName = args[0];
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(targetName);
            
            if (!offlinePlayer.hasPlayedBefore()) {
                sender.sendMessage("§c未找到玩家：" + targetName);
                return true;
            }
            
            // 如果目标玩家在线，获取在线玩家对象
            if (offlinePlayer.isOnline()) {
                targetPlayer = offlinePlayer.getPlayer();
            } else {
                // 离线玩家，创建一个临时的Player对象用于显示
                sender.sendMessage("§e正在查询离线玩家数据...");
                targetPlayer = null; // 标记为离线
            }
            
            // 加载离线玩家数据
            PlayerStats stats = databaseManager.loadPlayerStats(offlinePlayer.getUniqueId(), offlinePlayer.getName());
            sendStatsMessage(sender, stats, offlinePlayer.getName(), !offlinePlayer.isOnline());
            return true;
        }
        
        // 获取并发送统计数据
        PlayerStats stats = databaseManager.getPlayerStats(targetPlayer.getUniqueId());
        if (stats == null) {
            // 如果缓存中没有，从数据库加载
            stats = databaseManager.loadPlayerStats(targetPlayer.getUniqueId(), targetPlayer.getName());
        }
        
        sendStatsMessage(sender, stats, targetPlayer.getName(), false);
        return true;
    }
    
    /**
     * 发送统计数据消息
     */
    private void sendStatsMessage(CommandSender sender, PlayerStats stats, String playerName, boolean isOffline) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String lastLoginTime = sdf.format(new Date(stats.getLastLoginTime()));
        
        // 格式化游玩时间
        long playTimeSeconds = stats.getPlayTime();
        long hours = playTimeSeconds / 3600;
        long minutes = (playTimeSeconds % 3600) / 60;
        long seconds = playTimeSeconds % 60;
        String playTimeFormatted = String.format("%d小时 %d分钟 %d秒", hours, minutes, seconds);
        
        // 计算K/D比率
        double kdRatio = stats.getDeaths() > 0 ? (double) stats.getKills() / stats.getDeaths() : stats.getKills();
        String kdRatioStr = String.format("%.2f", kdRatio);
        
        // 发送统计信息
        sender.sendMessage("§6========== §e玩家统计 §6==========");
        sender.sendMessage("§7玩家名称：§f" + playerName);
        sender.sendMessage("§7在线状态：" + (isOffline ? "§c离线" : "§a在线"));
        sender.sendMessage("§7上次登录：§f" + lastLoginTime);
        sender.sendMessage("§7游玩时间：§f" + playTimeFormatted);
        sender.sendMessage("§7击杀数量：§f" + stats.getKills());
        sender.sendMessage("§7死亡数量：§f" + stats.getDeaths());
        sender.sendMessage("§7K/D 比率：§f" + kdRatioStr);
        sender.sendMessage("§6================================");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            // 补全玩家名称
            String partialName = args[0].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(partialName))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
