package top.mcocet.orcak;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

/**
 * 自定义帮助命令处理器
 * 当玩家执行配置的命令并加上 help 参数时，显示对应的帮助文档
 */
public class CustomHelpCommand implements CommandExecutor {

    private final Orcak plugin;
    private final ConfigManager configManager;
    private Set<String> helpCommands;
    private String helpFilesDirectory;

    public CustomHelpCommand(Orcak plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        reloadConfig();
    }

    /**
     * 重新加载配置
     */
    public void reloadConfig() {
        List<String> commands = configManager.getConfig().getStringList("custom-help.commands");
        helpCommands = new HashSet<>();
        if (commands != null) {
            for (String cmd : commands) {
                helpCommands.add(cmd.toLowerCase());
            }
        }
        helpFilesDirectory = configManager.getConfig().getString("custom-help.help-files-directory", "command");
        plugin.getLogger().info("自定义帮助命令已加载 " + helpCommands.size() + " 个命令");
    }

    /**
     * 检查是否应该处理此命令（命令以 help 结尾）
     */
    public boolean shouldHandle(String command, String[] args) {
        if (!configManager.getConfig().getBoolean("custom-help.enabled", true)) {
            return false;
        }

        // 检查命令是否在配置列表中
        if (!helpCommands.contains(command.toLowerCase())) {
            return false;
        }

        // 检查是否有 help 参数
        if (args.length == 0) {
            return false;
        }

        // 最后一个参数是否为 help
        String lastArg = args[args.length - 1].toLowerCase();
        return lastArg.equals("help") || lastArg.equals("?") || lastArg.equals("-h") || lastArg.equals("--help");
    }

    /**
     * 处理自定义帮助命令
     */
    public boolean handleHelp(CommandSender sender, String command, String[] args) {
        // 获取帮助文件名（命令名 + .txt）
        String fileName = command.toLowerCase() + ".txt";
        File helpFile = new File(plugin.getDataFolder(), helpFilesDirectory + "/" + fileName);

        if (!helpFile.exists()) {
            if (configManager.getConfig().getBoolean("custom-help.show-error-messages", true)) {
                String message = configManager.getConfig().getString("custom-help.file-not-found-message",
                        "&c未找到该命令的帮助文档！");
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
            }
            return true;
        }

        try {
            // 读取帮助文件内容
            String content = new String(Files.readAllBytes(helpFile.toPath()), StandardCharsets.UTF_8);

            // 发送帮助内容（支持颜色代码）
            String[] lines = content.split("\n");
            for (String line : lines) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', line));
            }

            plugin.getLogger().info("玩家 " + sender.getName() + " 查看了命令 /" + command + " 的帮助文档");

        } catch (IOException e) {
            plugin.getLogger().severe("读取帮助文件失败: " + fileName + " - " + e.getMessage());
            if (configManager.getConfig().getBoolean("custom-help.show-error-messages", true)) {
                sender.sendMessage("§c读取帮助文档失败，请联系管理员。");
            }
        }

        return true;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // 这个方法不会被直接调用，因为我们使用 shouldHandle + handleHelp 的模式
        return false;
    }

    /**
     * 获取配置的帮助命令列表
     */
    public Set<String> getHelpCommands() {
        return helpCommands;
    }
}