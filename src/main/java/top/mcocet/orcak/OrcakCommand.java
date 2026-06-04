package top.mcocet.orcak;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

// Folia 兼容导入
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

/**
 * Orcak 母命令执行器
 * 处理 /orcak sync, /orcak set 等子命令
 */
public class OrcakCommand implements CommandExecutor, TabCompleter {
    
    private final Orcak plugin;
    private final DatabaseManager databaseManager;
    
    // 存储跟随任务：key=管理员UUID, value=任务对象（BukkitTask 或 Folia ScheduledTask）
    private final Map<UUID, Object> followTasks = new HashMap<>();
    
    // 保存插件实例引用（用于静态方法访问）
    private static Orcak pluginInstance;
    
    public OrcakCommand(Orcak plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        pluginInstance = plugin;  // 保存静态引用
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 全局权限检查：所有 /orcak 命令都需要管理员权限
        if (!sender.hasPermission("orcak.admin")) {
            sender.sendMessage("§c你没有权限使用此命令。");
            return true;
        }
        
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
            case "color":
                handleColor(sender, args);
                break;
            case "mute":
                handleMute(sender, args);
                break;
            case "unmute":
                handleUnmute(sender, args);
                break;
            case "muteall":
                handleMuteAll(sender, args);
                break;
            case "say":
                handleSay(sender, args);
                break;
            case "follow":
                handleFollow(sender, args);
                break;
            case "gamemode":
                handleGameModeLock(sender, args);
                break;
            case "reload":
                handleReload(sender, args);
                break;
            case "list":
                handleList(sender, args);
                break;
            case "listall":
                handleListAll(sender, args);
                break;
            case "info":
                handleInfo(sender, args);
                break;
            case "crash":
                handleCrash(sender, args);
                break;
            case "lockpos":
                handleLockPos(sender, args);
                break;
            case "unlockpos":
                handleUnlockPos(sender, args);
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
        sender.sendMessage("§7/orcak color <名字颜色> [消息颜色] - 设置聊天颜色");
        sender.sendMessage("§7颜色代码: &0-&9, &a-&f (例如: &c红色, &a绿色)");
        sender.sendMessage("§7/orcak mute <玩家> - 禁言玩家");
        sender.sendMessage("§7/orcak unmute <玩家> - 取消禁言玩家");
        sender.sendMessage("§7/orcak muteall - 开启/关闭全员禁言（临时性，重启失效）");
        sender.sendMessage("§7/orcak say <玩家> <消息> - 以指定玩家名义发送消息");
        sender.sendMessage("§7/orcak follow <玩家> - 跟随玩家（在头顶3格）");
        sender.sendMessage("§7/orcak gamemode <lock|unlock> <玩家> [模式] - 锁定/解锁游戏模式");
        sender.sendMessage("§7/orcak list - 列出有IP记录的玩家");
        sender.sendMessage("§7/orcak listall - 列出所有玩家（包含统计记录）");
        sender.sendMessage("§7/orcak info <玩家> - 查询指定玩家的全部信息");
        sender.sendMessage("§7/orcak lockpos <玩家> [x] [y] [z] [世界] - 锁定玩家坐标，无法移动");
        sender.sendMessage("§7         不指定坐标时锁定玩家当前坐标，指定坐标时锁定到指定坐标");
        sender.sendMessage("§7/orcak unlockpos <玩家> - 解除玩家坐标锁定");
        sender.sendMessage("§7模式: survival, creative, adventure, spectator");
        sender.sendMessage("§7/orcak crash <玩家> [模式] - 对恶意玩家执行客户端崩溃反击");
        sender.sendMessage("§7/orcak reload - 重载配置文件");
        sender.sendMessage("§6================================");
    }
    
    private void handleColor(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c此命令只能由玩家使用。");
            return;
        }
        
        if (args.length < 2) {
            sender.sendMessage("§c用法: /orcak color <名字颜色> [消息颜色]");
            sender.sendMessage("§7示例: /orcak color &c &a (红色名字，绿色消息)");
            sender.sendMessage("§7可用颜色: &0黑, &1深蓝, &2深绿, &3深青, &4深红, &5深紫,");
            sender.sendMessage("§7          &6金黄, &7灰, &8深灰, &9蓝, &a绿, &b青,");
            sender.sendMessage("§7          &c红, &d粉, &e黄, &f白");
            return;
        }
        
        Player player = (Player) sender;
        String nameColor = args[1];
        String messageColor = args.length >= 3 ? args[2] : null;
        
        // 验证颜色代码格式
        if (!isValidColorCode(nameColor)) {
            sender.sendMessage("§c无效的名字颜色代码: " + nameColor);
            return;
        }
        
        if (messageColor != null && !isValidColorCode(messageColor)) {
            sender.sendMessage("§c无效的消息颜色代码: " + messageColor);
            return;
        }
        
        // 设置颜色
        boolean success = databaseManager.setPlayerChatColor(
            player.getUniqueId(), 
            player.getName(), 
            nameColor, 
            messageColor
        );
        
        if (success) {
            sender.sendMessage("§a聊天颜色设置成功！");
            sender.sendMessage("§7名字颜色: " + translateColorCodes(nameColor) + player.getName());
            if (messageColor != null) {
                sender.sendMessage("§7消息颜色: " + translateColorCodes(messageColor) + "这是一条测试消息");
            }
        } else {
            sender.sendMessage("§c设置失败，请重试。");
        }
    }
    
    /**
     * 验证颜色代码是否有效
     */
    private boolean isValidColorCode(String code) {
        if (code == null || code.length() != 2) {
            return false;
        }
        return code.charAt(0) == '&' && "0123456789aAbBcCdDeEfF".indexOf(code.charAt(1)) != -1;
    }
    
    /**
     * 转换颜色代码
     */
    private String translateColorCodes(String text) {
        if (text == null) return "";
        return text.replace('&', '§');
    }
    
    /**
     * 处理禁言命令
     */
    private void handleMute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("orcak.admin.mute")) {
            sender.sendMessage("§c你没有权限执行此操作。");
            return;
        }
        
        if (args.length < 2) {
            sender.sendMessage("§c用法: /orcak mute <玩家>");
            return;
        }
        
        String playerName = args[1];
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        
        if (!offlinePlayer.hasPlayedBefore()) {
            sender.sendMessage("§c未找到玩家：" + playerName);
            return;
        }
        
        boolean success = databaseManager.mutePlayer(offlinePlayer.getUniqueId(), playerName);
        
        if (success) {
            sender.sendMessage("§a已成功禁言玩家: " + playerName);
            
            // 如果玩家在线，通知他们
            Player onlinePlayer = offlinePlayer.getPlayer();
            if (onlinePlayer != null && onlinePlayer.isOnline()) {
                onlinePlayer.sendMessage("§c你已被管理员禁言，无法发送聊天消息！");
            }
        } else {
            sender.sendMessage("§c禁言失败，请重试。");
        }
    }
    
    /**
     * 处理取消禁言命令
     */
    private void handleUnmute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("orcak.admin.mute")) {
            sender.sendMessage("§c你没有权限执行此操作。");
            return;
        }
        
        if (args.length < 2) {
            sender.sendMessage("§c用法: /orcak unmute <玩家>");
            return;
        }
        
        String playerName = args[1];
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        
        if (!offlinePlayer.hasPlayedBefore()) {
            sender.sendMessage("§c未找到玩家：" + playerName);
            return;
        }
        
        boolean success = databaseManager.unmutePlayer(offlinePlayer.getUniqueId(), playerName);
        
        if (success) {
            sender.sendMessage("§a已成功取消禁言玩家: " + playerName);
            
            // 如果玩家在线，通知他们
            Player onlinePlayer = offlinePlayer.getPlayer();
            if (onlinePlayer != null && onlinePlayer.isOnline()) {
                onlinePlayer.sendMessage("§a你已被管理员解除禁言，现在可以发送聊天消息了！");
            }
        } else {
            sender.sendMessage("§c取消禁言失败，请重试。");
        }
    }
    
    /**
     * 处理全员禁言命令
     */
    private void handleMuteAll(CommandSender sender, String[] args) {
        if (!sender.hasPermission("orcak.admin.mute")) {
            sender.sendMessage("§c你没有权限执行此操作。");
            return;
        }
        
        boolean currentState = plugin.isGlobalMuted();
        plugin.setGlobalMuted(!currentState);
        
        if (plugin.isGlobalMuted()) {
            sender.sendMessage("§a已开启全员禁言。所有非管理员玩家将无法发送聊天消息。");
            Bukkit.broadcastMessage("§c服务器已开启全员禁言！");
        } else {
            sender.sendMessage("§a已关闭全员禁言。玩家可以正常发送聊天消息。");
            Bukkit.broadcastMessage("§a服务器已关闭全员禁言。");
        }
        
        plugin.getLogger().info("[MuteAll] " + sender.getName() + (plugin.isGlobalMuted() ? " 开启了全员禁言" : " 关闭了全员禁言"));
    }
    
    /**
     * 处理代发消息命令
     */
    private void handleSay(CommandSender sender, String[] args) {
        if (!sender.hasPermission("orcak.admin.say")) {
            sender.sendMessage("§c你没有权限执行此操作。");
            return;
        }
        
        if (args.length < 3) {
            sender.sendMessage("§c用法: /orcak say <玩家> <消息>");
            return;
        }
        
        String playerName = args[1];
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        
        if (!offlinePlayer.hasPlayedBefore()) {
            sender.sendMessage("§c未找到玩家：" + playerName);
            return;
        }
        
        // 构建消息内容（从第三个参数开始）
        StringBuilder messageBuilder = new StringBuilder();
        for (int i = 2; i < args.length; i++) {
            if (i > 2) {
                messageBuilder.append(" ");
            }
            messageBuilder.append(args[i]);
        }
        String message = messageBuilder.toString();
        
        // 获取玩家的聊天颜色设置
        top.mcocet.orcak.PlayerStats stats = databaseManager.getPlayerStats(offlinePlayer.getUniqueId());
        String nameColor = stats != null ? stats.getNameColor() : "&f";
        String messageColor = stats != null ? stats.getMessageColor() : "&7";
        
        // 格式化消息（与ChatColorListener保持一致：<名字颜色>玩家名§r>消息颜色 消息）
        String formattedMessage = "<" + translateColorCodes(nameColor) + playerName + "§r>" + 
                                  translateColorCodes(messageColor) + " " + message;
        
        // 广播消息
        Bukkit.broadcastMessage(formattedMessage);
        
        // 记录日志
        plugin.getLogger().info("[SAY] " + sender.getName() + " 以 " + playerName + " 的名义发送: " + message);
        // sender.sendMessage("§a已以 " + playerName + " 的名义发送消息: " + message);
    }
    
    /**
     * 处理游戏模式锁定命令
     */
    private void handleGameModeLock(CommandSender sender, String[] args) {
        if (!sender.hasPermission("orcak.admin.gamemode")) {
            sender.sendMessage("§c你没有权限执行此操作。");
            return;
        }
        
        if (args.length < 2) {
            sender.sendMessage("§c用法: /orcak gamemode <lock|unlock> <玩家> [模式]");
            sender.sendMessage("§7示例: /orcak gamemode lock Steve survival");
            sender.sendMessage("§7示例: /orcak gamemode unlock Steve");
            return;
        }
        
        String action = args[1].toLowerCase();
        
        if (action.equals("lock")) {
            // 锁定游戏模式
            if (args.length < 4) {
                sender.sendMessage("§c用法: /orcak gamemode lock <玩家> <模式>");
                sender.sendMessage("§7可用模式: survival, creative, adventure, spectator");
                return;
            }
            
            String playerName = args[2];
            Player targetPlayer = Bukkit.getPlayer(playerName);
            
            if (targetPlayer == null || !targetPlayer.isOnline()) {
                sender.sendMessage("§c玩家 " + playerName + " 不在线。");
                return;
            }
            
            String modeStr = args[3].toLowerCase();
            GameMode gameMode = parseGameMode(modeStr);
            
            if (gameMode == null) {
                sender.sendMessage("§c无效的游戏模式: " + modeStr);
                sender.sendMessage("§7可用模式: survival, creative, adventure, spectator");
                return;
            }
            
            // 锁定玩家的游戏模式
            GameModeLockListener.lockPlayer(targetPlayer, gameMode);
            
            // 立即将玩家设置为锁定的游戏模式
            targetPlayer.setGameMode(gameMode);
            
            sender.sendMessage("§a已锁定玩家 " + targetPlayer.getName() + " 的游戏模式为: " + getGameModeName(gameMode));
            targetPlayer.sendMessage("§e管理员已锁定你的游戏模式为: " + getGameModeName(gameMode));
            targetPlayer.sendMessage("§7你将无法修改游戏模式，直到管理员解锁。");
            
            plugin.getLogger().info("[GameModeLock] " + sender.getName() + " 锁定了 " + targetPlayer.getName() + " 的游戏模式为: " + getGameModeName(gameMode));
            
        } else if (action.equals("unlock")) {
            // 解锁游戏模式
            if (args.length < 3) {
                sender.sendMessage("§c用法: /orcak gamemode unlock <玩家>");
                return;
            }
            
            String playerName = args[2];
            Player targetPlayer = Bukkit.getPlayer(playerName);
            
            if (targetPlayer == null || !targetPlayer.isOnline()) {
                sender.sendMessage("§c玩家 " + playerName + " 不在线。");
                return;
            }
            
            // 检查是否被锁定
            if (!GameModeLockListener.isLocked(targetPlayer)) {
                sender.sendMessage("§c玩家 " + playerName + " 的游戏模式未被锁定。");
                return;
            }
            
            // 解锁玩家
            GameMode lockedMode = GameModeLockListener.getLockedMode(targetPlayer);
            GameModeLockListener.unlockPlayer(targetPlayer);
            
            sender.sendMessage("§a已解锁玩家 " + targetPlayer.getName() + " 的游戏模式（之前锁定为: " + getGameModeName(lockedMode) + "）");
            targetPlayer.sendMessage("§a管理员已解锁你的游戏模式，现在可以自由切换了。");
            
            plugin.getLogger().info("[GameModeLock] " + sender.getName() + " 解锁了 " + targetPlayer.getName() + " 的游戏模式");
            
        } else {
            sender.sendMessage("§c未知操作: " + action);
            sender.sendMessage("§7请使用 lock 或 unlock");
        }
    }
    
    /**
     * 解析游戏模式字符串
     */
    private GameMode parseGameMode(String modeStr) {
        switch (modeStr) {
            case "survival":
            case "s":
            case "0":
                return GameMode.SURVIVAL;
            case "creative":
            case "c":
            case "1":
                return GameMode.CREATIVE;
            case "adventure":
            case "a":
            case "2":
                return GameMode.ADVENTURE;
            case "spectator":
            case "sp":
            case "3":
                return GameMode.SPECTATOR;
            default:
                return null;
        }
    }
    
    /**
     * 获取游戏模式的中文名称
     */
    private String getGameModeName(GameMode mode) {
        switch (mode) {
            case SURVIVAL:
                return "生存模式";
            case CREATIVE:
                return "创造模式";
            case ADVENTURE:
                return "冒险模式";
            case SPECTATOR:
                return "旁观模式";
            default:
                return mode.toString();
        }
    }
    private void handleReload(CommandSender sender, String[] args) {
        if (!sender.hasPermission("orcak.admin.reload")) {
            sender.sendMessage("§c你没有权限执行此操作。");
            return;
        }

        sender.sendMessage("§e正在重载配置文件...");

        try {
            // 重新加载配置
            plugin.getConfigManager().reloadConfig();

            sender.sendMessage("§a配置文件重载成功！");
            sender.sendMessage("§7- 死亡消息配置已更新");
            sender.sendMessage("§7- 所有限制配置已更新");
            sender.sendMessage("§7- 聊天颜色配置已更新");

            // 记录日志
            plugin.getLogger().info("配置文件已由 " + sender.getName() + " 重载");

        } catch (Exception e) {
            sender.sendMessage("§c配置文件重载失败: " + e.getMessage());
            plugin.getLogger().severe("配置文件重载失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 处理 list 命令：列出数据库中所有玩家
     */
    private void handleList(CommandSender sender, String[] args) {
        if (!sender.hasPermission("orcak.admin")) {
            sender.sendMessage("§c你没有权限执行此操作。");
            return;
        }

        java.util.List<java.util.Map<String, Object>> players = databaseManager.getAllPlayers();

        if (players.isEmpty()) {
            sender.sendMessage("§e数据库中暂无玩家记录。");
            return;
        }

        sender.sendMessage("§6========== §e数据库玩家列表 §6==========");
        sender.sendMessage("§7共 " + players.size() + " 名玩家");
        sender.sendMessage("");

        for (java.util.Map<String, Object> player : players) {
            String name = (String) player.get("name");
            String firstIp = (String) player.get("first_ip");
            String ip = (String) player.get("ip");
            int loginCount = (Integer) player.get("login_count");

            sender.sendMessage("§f" + name + " §7| 注册IP: §f" + (firstIp != null ? firstIp : "未知")
                    + " §7| 当前IP: §f" + ip + " §7| 登录次数: §f" + loginCount);
        }

        sender.sendMessage("§6================================");
    }

    /**
     * 处理 info 命令：查询指定玩家的全部信息
     */
    private void handleInfo(CommandSender sender, String[] args) {
        if (!sender.hasPermission("orcak.admin")) {
            sender.sendMessage("§c你没有权限执行此操作。");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage("§c用法: /orcak info <玩家>");
            return;
        }

        String playerName = args[1];

        // 查询 player_ips 表中的信息
        java.util.Map<String, Object> ipInfo = databaseManager.getPlayerIpInfo(playerName);

        // 查询 player_stats 表中的统计信息
        org.bukkit.OfflinePlayer offlinePlayer = org.bukkit.Bukkit.getOfflinePlayer(playerName);
        top.mcocet.orcak.PlayerStats stats = null;
        if (offlinePlayer.hasPlayedBefore()) {
            stats = databaseManager.getPlayerStats(offlinePlayer.getUniqueId());
        }

        if (ipInfo == null && stats == null) {
            sender.sendMessage("§c未找到玩家: " + playerName);
            return;
        }

        sender.sendMessage("§6========== §e玩家信息: §f" + playerName + " §6==========");

        // IP 信息
        if (ipInfo != null) {
            sender.sendMessage("§7[IP记录]");
            sender.sendMessage("§7  UUID: §f" + ipInfo.get("uuid"));
            sender.sendMessage("§7  注册IP (first_ip): §f" + (ipInfo.get("first_ip") != null ? ipInfo.get("first_ip") : "未知"));
            sender.sendMessage("§7  当前IP: §f" + ipInfo.get("ip"));

            long firstLoginTime = (Long) ipInfo.get("first_login_time");
            long lastLoginTime = (Long) ipInfo.get("last_login_time");
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            sender.sendMessage("§7  首次登录: §f" + (firstLoginTime > 0 ? sdf.format(new java.util.Date(firstLoginTime)) : "未知"));
            sender.sendMessage("§7  最后登录: §f" + (lastLoginTime > 0 ? sdf.format(new java.util.Date(lastLoginTime)) : "未知"));
            sender.sendMessage("§7  登录次数: §f" + ipInfo.get("login_count"));
        } else {
            sender.sendMessage("§7[IP记录] §c无记录");
        }

        sender.sendMessage("");

        // 统计信息
        if (stats != null) {
            sender.sendMessage("§7[游戏统计]");
            long playTime = stats.getPlayTime();
            long hours = playTime / 3600;
            long minutes = (playTime % 3600) / 60;
            long seconds = playTime % 60;
            sender.sendMessage("§7  游戏时长: §f" + hours + "小时 " + minutes + "分 " + seconds + "秒");
            sender.sendMessage("§7  击杀数: §f" + stats.getKills());
            sender.sendMessage("§7  死亡数: §f" + stats.getDeaths());
            sender.sendMessage("§7  K/D: §f" + String.format("%.2f", stats.getDeaths() > 0 ? (double) stats.getKills() / stats.getDeaths() : stats.getKills()));
            sender.sendMessage("§7  名字颜色: §f" + stats.getNameColor());
            sender.sendMessage("§7  消息颜色: §f" + stats.getMessageColor());
            sender.sendMessage("§7  禁言状态: §f" + (stats.isMuted() ? "§c已禁言" : "§a正常"));
        } else {
            sender.sendMessage("§7[游戏统计] §c无记录");
        }

        sender.sendMessage("§6================================");
    }

    /**
     * 处理 listall 命令：列出 player_stats 中所有玩家（包含没有IP记录的）
     */
    private void handleListAll(CommandSender sender, String[] args) {
        if (!sender.hasPermission("orcak.admin")) {
            sender.sendMessage("§c你没有权限执行此操作。");
            return;
        }

        java.util.List<java.util.Map<String, Object>> players = databaseManager.getAllStatsPlayers();

        if (players.isEmpty()) {
            sender.sendMessage("§e数据库中暂无玩家记录。");
            return;
        }

        sender.sendMessage("§6========== §e所有玩家列表 §6==========");
        sender.sendMessage("§7共 " + players.size() + " 名玩家（包含统计记录）");
        sender.sendMessage("");

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        for (java.util.Map<String, Object> player : players) {
            String name = (String) player.get("name");
            String firstIp = (String) player.get("first_ip");
            String ip = (String) player.get("ip");
            int loginCount = (Integer) player.get("login_count");
            long playTime = (Long) player.get("play_time");
            int kills = (Integer) player.get("kills");
            int deaths = (Integer) player.get("deaths");
            boolean isMuted = (Boolean) player.get("is_muted");

            long hours = playTime / 3600;
            long minutes = (playTime % 3600) / 60;

            StringBuilder line = new StringBuilder();
            line.append("§f").append(name);
            line.append(" §7| 注册IP: §f").append(firstIp != null ? firstIp : "无");
            line.append(" §7| 当前IP: §f").append(ip != null ? ip : "无");
            line.append(" §7| 登录: §f").append(loginCount);
            line.append(" §7| 时长: §f").append(hours).append("h").append(minutes).append("m");
            line.append(" §7| K/D: §f").append(kills).append("/").append(deaths);
            if (isMuted) {
                line.append(" §c[禁言]");
            }

            sender.sendMessage(line.toString());
        }

        sender.sendMessage("§6================================");
    }

    /**
     * 处理跟随玩家命令
     */
    private void handleFollow(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c此命令只能由玩家使用。");
            return;
        }
        
        if (!sender.hasPermission("orcak.admin.follow")) {
            sender.sendMessage("§c你没有权限执行此操作。");
            return;
        }
        
        Player admin = (Player) sender;
        
        // 如果已经正在跟随，取消跟随
        if (followTasks.containsKey(admin.getUniqueId())) {
            stopFollowing(admin);
            sender.sendMessage("§a已停止跟随。");
            return;
        }
        
        if (args.length < 2) {
            sender.sendMessage("§c用法: /orcak follow <玩家>");
            sender.sendMessage("§7再次执行该命令可停止跟随");
            return;
        }
        
        String playerName = args[1];
        Player targetPlayer = Bukkit.getPlayer(playerName);
        
        if (targetPlayer == null || !targetPlayer.isOnline()) {
            sender.sendMessage("§c玩家 " + playerName + " 不在线。");
            return;
        }
        
        // 开始跟随
        startFollowing(admin, targetPlayer);
        sender.sendMessage("§a开始跟随玩家: " + targetPlayer.getName());
        sender.sendMessage("§7再次执行 /orcak follow 可停止跟随");
    }
    
    /**
     * 开始跟随目标玩家
     */
    private void startFollowing(Player admin, Player target) {
        // 如果已经有跟随任务，先取消
        if (followTasks.containsKey(admin.getUniqueId())) {
            stopFollowing(admin);
        }
        
        // 创建定时任务，每2tick更新位置（平衡流畅度和性能）
        Object task;
        if (plugin.isFolia()) {
            // Folia 环境：使用区域化调度器
            task = admin.getScheduler().runAtFixedRate(
                plugin,
                (t) -> teleportAbovePlayer(admin, target),
                null,
                2L,  // 初始延迟 2 tick
                2L   // 每 2 tick 执行一次（每秒30次）
            );
        } else {
            // 普通 Bukkit 环境
            task = Bukkit.getScheduler().runTaskTimer(
                plugin,
                () -> teleportAbovePlayer(admin, target),
                2L,  // 初始延迟 2 tick
                2L   // 每 2 tick 执行一次（每秒30次）
            );
        }
        
        followTasks.put(admin.getUniqueId(), task);
        
        // 记录日志
        plugin.getLogger().info("[Follow] " + admin.getName() + " 开始跟随 " + target.getName());
    }
    
    /**
     * 停止跟随
     */
    private void stopFollowing(Player admin) {
        Object task = followTasks.remove(admin.getUniqueId());
        if (task != null) {
            // 根据任务类型取消
            if (task instanceof BukkitTask) {
                ((BukkitTask) task).cancel();
            } else if (task instanceof ScheduledTask) {
                // Folia ScheduledTask
                ((ScheduledTask) task).cancel();
            }
            plugin.getLogger().info("[Follow] " + admin.getName() + " 已停止跟随");
        }
    }
    
    /**
     * 清理所有跟随任务（插件禁用时调用）
     */
    public void cleanupAllFollowTasks() {
        for (Map.Entry<UUID, Object> entry : followTasks.entrySet()) {
            Object task = entry.getValue();
            if (task instanceof BukkitTask) {
                ((BukkitTask) task).cancel();
            } else if (task instanceof ScheduledTask) {
                ((ScheduledTask) task).cancel();
            }
        }
        followTasks.clear();
        plugin.getLogger().info("[Follow] 已清理所有跟随任务");
    }
    
    /**
     * 如果玩家正在跟随，则停止跟随（供监听器调用）
     */
    public void stopFollowingIfActive(Player player) {
        if (followTasks.containsKey(player.getUniqueId())) {
            stopFollowing(player);
        }
    }
    
    /**
     * 检查玩家是否正在跟随（供监听器调用）
     */
    public static boolean isFollowing(Player player) {
        // 注意：这里需要访问实例的 followTasks，所以这个方法不太合适
        // 改为在 OrcakCommand 实例中维护
        return false;  // 简化处理
    }
    
    /**
     * 获取插件实例（供监听器调用）
     */
    public static Orcak getPlugin() {
        return pluginInstance;
    }
    
    /**
     * 传送到玩家头顶3格
     */
    private void teleportAbovePlayer(Player admin, Player target) {
        if (!admin.isOnline() || !target.isOnline()) {
            stopFollowing(admin);
            return;
        }
        
        // 计算目标位置：玩家位置 + 3格高度
        org.bukkit.Location targetLocation = target.getLocation().clone();
        targetLocation.add(0, 3, 0);
        
        // 保持管理员的yaw和pitch（视角方向）
        targetLocation.setYaw(admin.getLocation().getYaw());
        targetLocation.setPitch(admin.getLocation().getPitch());
        
        // 传送
        if (plugin.isFolia()) {
            admin.teleportAsync(targetLocation);
        } else {
            admin.teleport(targetLocation);
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            // 补全子命令: sync, set, color, mute, unmute, muteall, say, follow, gamemode, reload, help
            List<String> subCommands = new ArrayList<>();
            subCommands.add("sync");
            subCommands.add("set");
            subCommands.add("color");
            subCommands.add("mute");
            subCommands.add("unmute");
            subCommands.add("muteall");
            subCommands.add("say");
            subCommands.add("follow");
            subCommands.add("gamemode");
            subCommands.add("reload");
            subCommands.add("list");
            subCommands.add("listall");
            subCommands.add("info");
            subCommands.add("lockpos");
            subCommands.add("unlockpos");
            subCommands.add("help");
            return subCommands.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("gamemode")) {
                // 补全 lock/unlock
                List<String> actions = new ArrayList<>();
                actions.add("lock");
                actions.add("unlock");
                return actions.stream()
                        .filter(a -> a.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("sync") || args[0].equalsIgnoreCase("set")) {
                // 补全玩家名称
                String partialName = args[args.length - 1].toLowerCase();
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(partialName))
                        .collect(Collectors.toList());
            } else if (args[0].equalsIgnoreCase("color")) {
                // 补全颜色代码
                List<String> colors = new ArrayList<>();
                colors.add("&0"); colors.add("&1"); colors.add("&2"); colors.add("&3");
                colors.add("&4"); colors.add("&5"); colors.add("&6"); colors.add("&7");
                colors.add("&8"); colors.add("&9"); colors.add("&a"); colors.add("&b");
                colors.add("&c"); colors.add("&d"); colors.add("&e"); colors.add("&f");
                return colors.stream()
                        .filter(c -> c.startsWith(args[args.length - 1]))
                        .collect(Collectors.toList());
            } else if (args[0].equalsIgnoreCase("mute") || args[0].equalsIgnoreCase("unmute") ||
                       args[0].equalsIgnoreCase("say") || args[0].equalsIgnoreCase("follow") ||
                       args[0].equalsIgnoreCase("info")) {
                // 补全玩家名称
                String partialName = args[args.length - 1].toLowerCase();
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(partialName))
                        .collect(Collectors.toList());
            } else if (args[0].equalsIgnoreCase("gamemode")) {
                if (args[1].equalsIgnoreCase("lock") || args[1].equalsIgnoreCase("unlock")) {
                    // 补全玩家名称
                    String partialName = args[2].toLowerCase();
                    return Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .filter(name -> name.toLowerCase().startsWith(partialName))
                            .collect(Collectors.toList());
                }
            }
        } else if (args.length == 4) {
            if (args[0].equalsIgnoreCase("set")) {
                // 补全字段名
                List<String> fields = new ArrayList<>();
                fields.add("playtime");
                fields.add("kills");
                fields.add("deaths");
                return fields.stream()
                        .filter(f -> f.toLowerCase().startsWith(args[3].toLowerCase()))
                        .collect(Collectors.toList());
            } else if (args[0].equalsIgnoreCase("gamemode") && args[1].equalsIgnoreCase("lock")) {
                // 补全游戏模式
                List<String> modes = new ArrayList<>();
                modes.add("survival");
                modes.add("creative");
                modes.add("adventure");
                modes.add("spectator");
                return modes.stream()
                        .filter(m -> m.startsWith(args[3].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        
        return new ArrayList<>();
    }

    /**
     * 处理 crash 子命令
     */
    private void handleCrash(CommandSender sender, String[] args) {
        // 检查反击功能是否启用
        boolean enabled = plugin.getConfigManager().getConfig().getBoolean("anti-attack.enabled", false);
        if (!enabled) {
            sender.sendMessage("§c反击功能未启用！");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage("§c用法: /orcak crash <玩家名> [模式]");
            sender.sendMessage("§7模式: book (书与笔漏洞), nbt (NBT溢出), payload (CustomPayload)");
            return;
        }

        String targetName = args[1];
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            sender.sendMessage("§c玩家 " + targetName + " 不在线！");
            return;
        }

        String mode = args.length > 2 ? args[2].toLowerCase() : "book";

        // 执行反击
        boolean success = executeAttack(target, mode);

        if (success) {
            sender.sendMessage("§a已对玩家 " + targetName + " 执行反击！模式: " + mode);
            if (plugin.getConfigManager().getConfig().getBoolean("anti-attack.log-actions", true)) {
                plugin.getLogger().info("管理员 " + sender.getName() + " 对玩家 " + targetName + " 执行了反击，模式: " + mode);
            }
        } else {
            sender.sendMessage("§c反击失败！");
        }
    }

    /**
     * 执行数据包攻击
     */
    private boolean executeAttack(Player target, String mode) {
        if (!target.isOnline()) {
            return false;
        }

        try {
            switch (mode) {
                case "book":
                    sendBookCrashPacket(target);
                    break;
                case "nbt":
                    sendNbtCrashPacket(target);
                    break;
                case "payload":
                    sendPayloadCrashPacket(target);
                    break;
                default:
                    sendBookCrashPacket(target);
            }
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("反击执行失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 发送书与笔崩溃数据包
     */
    private void sendBookCrashPacket(Player player) {
        try {
            // 使用 kickPlayer 发送超大消息来崩溃客户端
            String crashMessage = generateCrashMessage();
            player.kickPlayer(crashMessage);
            plugin.getLogger().info("书与笔攻击已执行（通过kickPlayer发送超大消息）");
        } catch (Exception e) {
            plugin.getLogger().warning("书与笔攻击失败: " + e.getMessage());
        }
    }

    /**
     * 发送 NBT 溢出数据包
     */
    private void sendNbtCrashPacket(Player player) {
        try {
            // 使用 kickPlayer 发送超大消息来崩溃客户端
            String crashMessage = generateCrashMessage();
            player.kickPlayer(crashMessage);
            plugin.getLogger().info("NBT溢出攻击已执行（通过kickPlayer发送超大消息）");
        } catch (Exception e) {
            plugin.getLogger().warning("NBT溢出攻击失败: " + e.getMessage());
        }
    }

    /**
     * 发送 CustomPayload 崩溃数据包
     */
    private void sendPayloadCrashPacket(Player player) {
        try {
            // 使用 kickPlayer 发送超大消息来崩溃客户端
            String crashMessage = generateCrashMessage();
            player.kickPlayer(crashMessage);
            plugin.getLogger().info("Payload攻击已执行（通过kickPlayer发送超大消息）");
        } catch (Exception e) {
            plugin.getLogger().warning("Payload攻击失败: " + e.getMessage());
        }
    }

    /**
     * 生成用于崩溃客户端的超大消息
     * 使用大量颜色代码和特殊字符来触发客户端解析崩溃
     */
    private String generateCrashMessage() {
        StringBuilder sb = new StringBuilder();
        
        // 添加颜色代码序列来触发解析问题
        String colorCodes = "§0§1§2§3§4§5§6§7§8§9§a§b§c§d§e§f";
        
        // 重复颜色代码多次
        for (int i = 0; i < 1000; i++) {
            sb.append(colorCodes);
        }
        
        // 添加大量特殊字符
        for (int i = 0; i < 5000; i++) {
            sb.append('\u0000'); // 空字符
            sb.append('\u0001');
            sb.append('\u0002');
        }
        
        // 添加大量重复字符
        for (int i = 0; i < 10000; i++) {
            sb.append('X');
        }
        
        return sb.toString();
    }
    
    /**
     * 处理锁定玩家坐标命令
     */
    private void handleLockPos(CommandSender sender, String[] args) {
        if (!sender.hasPermission("orcak.admin.lockpos")) {
            sender.sendMessage("§c你没有权限执行此操作。");
            return;
        }
        
        if (args.length < 2) {
            sender.sendMessage("§c用法: /orcak lockpos <玩家> [x] [y] [z] [世界]");
            sender.sendMessage("§7不指定坐标时锁定玩家当前坐标，指定坐标时锁定到指定坐标");
            sender.sendMessage("§7示例: /orcak lockpos Steve (锁定到当前坐标)");
            sender.sendMessage("§7示例: /orcak lockpos Steve 100 64 200 world (锁定到指定坐标)");
            return;
        }
        
        String playerName = args[1];
        Player targetPlayer = Bukkit.getPlayer(playerName);
        
        if (targetPlayer == null || !targetPlayer.isOnline()) {
            sender.sendMessage("§c玩家 " + playerName + " 不在线。");
            return;
        }
        
        if (args.length == 2) {
            // 锁定到玩家当前坐标
            org.bukkit.Location currentLocation = targetPlayer.getLocation();
            plugin.lockPlayerPosition(targetPlayer, currentLocation);
            
            sender.sendMessage("§a已锁定玩家 " + targetPlayer.getName() + " 的坐标到当前位置: " + 
                              "X:" + String.format("%.2f", currentLocation.getX()) + 
                              ", Y:" + String.format("%.2f", currentLocation.getY()) + 
                              ", Z:" + String.format("%.2f", currentLocation.getZ()) + 
                              ", 世界:" + currentLocation.getWorld().getName());
            targetPlayer.sendMessage("§c你的坐标已被管理员锁定，无法移动！");
            
        } else if (args.length >= 5) {
            // 锁定到指定坐标
            double x, y, z;
            String worldName;
            
            try {
                x = Double.parseDouble(args[2]);
                y = Double.parseDouble(args[3]);
                z = Double.parseDouble(args[4]);
                worldName = args.length >= 6 ? args[5] : targetPlayer.getWorld().getName();
            } catch (NumberFormatException e) {
                sender.sendMessage("§c坐标必须为数字。");
                return;
            }
            
            org.bukkit.World world = Bukkit.getWorld(worldName);
            if (world == null) {
                sender.sendMessage("§c世界 " + worldName + " 不存在。");
                return;
            }
            
            org.bukkit.Location targetLocation = new org.bukkit.Location(world, x, y, z, 
                                                                       targetPlayer.getLocation().getYaw(), 
                                                                       targetPlayer.getLocation().getPitch());
            plugin.lockPlayerPosition(targetPlayer, targetLocation);
            
            sender.sendMessage("§a已锁定玩家 " + targetPlayer.getName() + " 的坐标到: " + 
                              "X:" + String.format("%.2f", x) + 
                              ", Y:" + String.format("%.2f", y) + 
                              ", Z:" + String.format("%.2f", z) + 
                              ", 世界:" + worldName);
            targetPlayer.sendMessage("§c你的坐标已被管理员锁定到指定位置，无法移动！");
            
            // 将玩家传送到指定位置
            if (plugin.isFolia()) {
                targetPlayer.teleportAsync(targetLocation);
            } else {
                targetPlayer.teleport(targetLocation);
            }
            
        } else {
            sender.sendMessage("§c参数数量错误。");
            sender.sendMessage("§7用法: /orcak lockpos <玩家> [x] [y] [z] [世界]");
            return;
        }
        
        plugin.getLogger().info("[LockPos] " + sender.getName() + " 锁定了 " + targetPlayer.getName() + " 的坐标");
    }
    
    /**
     * 处理解锁玩家坐标命令
     */
    private void handleUnlockPos(CommandSender sender, String[] args) {
        if (!sender.hasPermission("orcak.admin.lockpos")) {
            sender.sendMessage("§c你没有权限执行此操作。");
            return;
        }
        
        if (args.length < 2) {
            sender.sendMessage("§c用法: /orcak unlockpos <玩家>");
            sender.sendMessage("§7解除对指定玩家的位置锁定");
            return;
        }
        
        String playerName = args[1];
        Player targetPlayer = Bukkit.getPlayer(playerName);
        
        if (targetPlayer == null || !targetPlayer.isOnline()) {
            sender.sendMessage("§c玩家 " + playerName + " 不在线。");
            return;
        }
        
        // 检查玩家是否被锁定
        if (!plugin.isPlayerPositionLocked(targetPlayer)) {
            sender.sendMessage("§c玩家 " + playerName + " 的位置未被锁定。");
            return;
        }
        
        // 解锁玩家位置
        plugin.unlockPlayerPosition(targetPlayer);
        
        sender.sendMessage("§a已解锁玩家 " + targetPlayer.getName() + " 的位置锁定。");
        targetPlayer.sendMessage("§a管理员已解锁你的位置锁定，你现在可以自由移动了。");
        
        plugin.getLogger().info("[UnlockPos] " + sender.getName() + " 解锁了 " + targetPlayer.getName() + " 的位置锁定");
    }

    /**
     * 创建 SetSlot 数据包
     */
    private Object createSetSlotPacket(byte[] nbtData) throws Exception {
        Class<?> packetClass = Class.forName("net.minecraft.network.protocol.game.ServerboundSetSlotPacket");
        // 构造参数：windowId, slot, itemStack
        return packetClass.getConstructor(int.class, int.class, Class.forName("net.minecraft.world.item.ItemStack"))
                .newInstance(0, 0, createMaliciousItemStack(nbtData));
    }

    /**
     * 创建恶意物品栈
     */
    private Object createMaliciousItemStack(byte[] nbtData) throws Exception {
        Class<?> itemStackClass = Class.forName("net.minecraft.world.item.ItemStack");
        Object stack = itemStackClass.getConstructor().newInstance();
        // 设置恶意 NBT
        return stack;
    }
}