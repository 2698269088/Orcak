package top.mcocet.orcak;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.permissions.Permission;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 命令执行限制监听器
 * 检测玩家在指定时间内执行特定命令的次数，达到阈值后自动踢出
 */
public class CommandLimitListener implements Listener {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;

    // 存储玩家的命令执行记录: UUID -> CommandRecord
    private final Map<UUID, CommandRecord> commandRecords = new ConcurrentHashMap<>();

    // 缓存监控命令列表
    private Set<String> monitoredCommands = new HashSet<>();

    public CommandLimitListener(JavaPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        reloadMonitoredCommands();
    }

    /**
     * 重新加载监控命令列表
     */
    public void reloadMonitoredCommands() {
        List<String> commands = configManager.getConfig().getStringList("command-limit.monitored-commands");
        monitoredCommands = new HashSet<>();
        if (commands != null) {
            for (String cmd : commands) {
                monitoredCommands.add(cmd.toLowerCase());
            }
        }
        plugin.getLogger().info("命令限制监听器已加载 " + monitoredCommands.size() + " 个监控命令");
    }

    /**
     * 监听命令执行事件
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (!configManager.getConfig().getBoolean("command-limit.enabled", false)) {
            return;
        }

        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // 检查是否为管理员豁免
        if (configManager.getConfig().getBoolean("command-limit.bypass-for-admins", true)) {
            if (isAdmin(player)) {
                return;
            }
        }

        // 获取命令（去除斜杠并转小写）
        String command = event.getMessage().trim().toLowerCase();
        if (command.startsWith("/")) {
            command = command.substring(1);
        }

        // 获取命令名称（不包含参数）
        String commandName = command.split(" ")[0];

        // 检查是否在监控列表中
        if (!monitoredCommands.contains(commandName)) {
            return;
        }

        long currentTime = System.currentTimeMillis();

        // 获取配置
        long timeWindowMs = (long) configManager.getConfig().getInt("command-limit.time-window-minutes", 5) * 60 * 1000;
        int maxExecutions = configManager.getConfig().getInt("command-limit.max-executions", 20);

        // 获取或创建命令记录
        CommandRecord record = commandRecords.computeIfAbsent(playerId, k -> new CommandRecord());

        // 检查时间窗口是否已过期（从第一条命令开始计算）
        if (record.getFirstCommandTime() > 0 && currentTime - record.getFirstCommandTime() > timeWindowMs) {
            // 时间窗口已过期，重置记录
            record.reset();
        }

        // 添加命令执行记录
        record.addCommand(currentTime);

        // 检查是否达到限制
        int executionCount = record.getCommandCount();
        if (executionCount >= maxExecutions) {
            // 达到限制，执行踢出
            kickPlayer(playerId, player.getName());
        }

        plugin.getLogger().info("玩家 " + player.getName() + " 执行命令: /" + commandName + ", 当前累计次数: " + executionCount + "/" + maxExecutions);
    }

    /**
     * 检查玩家是否为管理员
     */
    private boolean isAdmin(Player player) {
        // 检查是否有 op 权限
        if (player.isOp()) {
            return true;
        }

        // 检查是否有 orcak.admin 权限
        if (player.hasPermission(new Permission("orcak.admin"))) {
            return true;
        }

        // 检查是否有 bukkit.command 相关权限（管理员通常有）
        if (player.hasPermission(new Permission("bukkit.command"))) {
            return true;
        }

        return false;
    }

    /**
     * 踢出玩家
     */
    private void kickPlayer(UUID playerId, String playerName) {
        // 获取配置
        boolean notifyPlayer = configManager.getConfig().getBoolean("command-limit.notify-player", true);
        boolean logKick = configManager.getConfig().getBoolean("command-limit.log-kick", true);
        String kickMessage = configManager.getConfig().getString("command-limit.kick-message",
                "&c你已被踢出服务器！原因：短时间内执行命令过于频繁");

        // 获取玩家对象
        Player player = Bukkit.getPlayer(playerId);

        // 清理命令记录
        commandRecords.remove(playerId);

        // 通知玩家并踢出
        if (player != null && player.isOnline()) {
            String message = ChatColor.translateAlternateColorCodes('&', kickMessage);
            // 使用延迟踢出避免事件处理中直接操作的问题
            scheduleKick(player, message);
        }

        // 记录日志
        if (logKick) {
            plugin.getLogger().info("玩家 " + playerName + " 因短时间内执行命令过于频繁被踢出");
        }
    }

    /**
     * 延迟踢出玩家（避免在事件处理中直接踢人导致的问题）
     */
    private void scheduleKick(Player player, String reason) {
        if (plugin instanceof Orcak && ((Orcak) plugin).isFolia()) {
            player.getScheduler().runDelayed(plugin, (task) -> {
                if (player.isOnline()) {
                    player.kickPlayer(reason);
                }
            }, null, 1L);
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (player.isOnline()) {
                    player.kickPlayer(reason);
                }
            });
        }
    }

    /**
     * 获取玩家的命令执行次数
     */
    public int getCommandCount(UUID playerId) {
        CommandRecord record = commandRecords.get(playerId);
        return record != null ? record.getCommandCount() : 0;
    }

    /**
     * 重置玩家的命令执行记录
     */
    public void resetCommandCount(UUID playerId) {
        commandRecords.remove(playerId);
    }

    /**
     * 命令记录类
     */
    private static class CommandRecord {
        private long firstCommandTime = 0;
        private int commandCount = 0;

        /**
         * 添加命令执行记录
         */
        public synchronized void addCommand(long timestamp) {
            if (firstCommandTime == 0) {
                firstCommandTime = timestamp;
            }
            commandCount++;
        }

        /**
         * 重置记录
         */
        public synchronized void reset() {
            firstCommandTime = 0;
            commandCount = 0;
        }

        /**
         * 获取第一条命令的时间
         */
        public synchronized long getFirstCommandTime() {
            return firstCommandTime;
        }

        /**
         * 获取命令执行次数
         */
        public synchronized int getCommandCount() {
            return commandCount;
        }
    }
}
