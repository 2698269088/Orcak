package top.mcocet.orcak;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 聊天颜色设置命令
 * 支持英文颜色名称和颜色代码两种格式
 */
public class ChatColorCommand implements CommandExecutor, TabCompleter {
    
    private final Orcak plugin;
    private final DatabaseManager databaseManager;
    
    // 英文颜色名称到颜色代码的映射
    private static final Map<String, String> COLOR_NAME_MAP = new HashMap<>();
    
    static {
        COLOR_NAME_MAP.put("black", "&0");
        COLOR_NAME_MAP.put("dark_blue", "&1");
        COLOR_NAME_MAP.put("dark_green", "&2");
        COLOR_NAME_MAP.put("dark_aqua", "&3");
        COLOR_NAME_MAP.put("dark_red", "&4");
        COLOR_NAME_MAP.put("dark_purple", "&5");
        COLOR_NAME_MAP.put("gold", "&6");
        COLOR_NAME_MAP.put("gray", "&7");
        COLOR_NAME_MAP.put("dark_gray", "&8");
        COLOR_NAME_MAP.put("blue", "&9");
        COLOR_NAME_MAP.put("green", "&a");
        COLOR_NAME_MAP.put("aqua", "&b");
        COLOR_NAME_MAP.put("red", "&c");
        COLOR_NAME_MAP.put("light_purple", "&d");
        COLOR_NAME_MAP.put("yellow", "&e");
        COLOR_NAME_MAP.put("white", "&f");
        
        // 中文颜色名称映射
        COLOR_NAME_MAP.put("黑", "&0");
        COLOR_NAME_MAP.put("深蓝", "&1");
        COLOR_NAME_MAP.put("深绿", "&2");
        COLOR_NAME_MAP.put("深青", "&3");
        COLOR_NAME_MAP.put("深红", "&4");
        COLOR_NAME_MAP.put("深紫", "&5");
        COLOR_NAME_MAP.put("金黄", "&6");
        COLOR_NAME_MAP.put("灰", "&7");
        COLOR_NAME_MAP.put("深灰", "&8");
        COLOR_NAME_MAP.put("蓝", "&9");
        COLOR_NAME_MAP.put("绿", "&a");
        COLOR_NAME_MAP.put("青", "&b");
        COLOR_NAME_MAP.put("红", "&c");
        COLOR_NAME_MAP.put("粉", "&d");
        COLOR_NAME_MAP.put("黄", "&e");
        COLOR_NAME_MAP.put("白", "&f");
    }
    
    public ChatColorCommand(Orcak plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c此命令只能由玩家使用。");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }
        
        if (args.length < 1) {
            player.sendMessage("§c用法: /chatcolor <名字颜色> [消息颜色]");
            player.sendMessage("§7示例: /chatcolor red green");
            player.sendMessage("§7示例: /chatcolor &c &a");
            return true;
        }
        
        String nameColorArg = args[0];
        String messageColorArg = args.length >= 2 ? args[1] : null;
        
        // 解析颜色
        String nameColor = parseColor(nameColorArg);
        if (nameColor == null) {
            player.sendMessage("§c无效的名字颜色: " + nameColorArg);
            player.sendMessage("§7可用颜色: black, dark_blue, dark_green, dark_aqua, dark_red,");
            player.sendMessage("§7          dark_purple, gold, gray, dark_gray, blue, green,");
            player.sendMessage("§7          aqua, red, light_purple, yellow, white");
            player.sendMessage("§7或使用颜色代码: &0-&9, &a-&f");
            return true;
        }
        
        String messageColor = null;
        if (messageColorArg != null) {
            messageColor = parseColor(messageColorArg);
            if (messageColor == null) {
                player.sendMessage("§c无效的消息颜色: " + messageColorArg);
                player.sendMessage("§7可用颜色: black, dark_blue, dark_green, dark_aqua, dark_red,");
                player.sendMessage("§7          dark_purple, gold, gray, dark_gray, blue, green,");
                player.sendMessage("§7          aqua, red, light_purple, yellow, white");
                player.sendMessage("§7或使用颜色代码: &0-&9, &a-&f");
                return true;
            }
        }
        
        // 设置颜色
        boolean success = databaseManager.setPlayerChatColor(
            player.getUniqueId(), 
            player.getName(), 
            nameColor, 
            messageColor
        );
        
        if (success) {
            player.sendMessage("§a聊天颜色设置成功！");
            player.sendMessage("§7名字颜色: " + translateColorCodes(nameColor) + player.getName());
            if (messageColor != null) {
                player.sendMessage("§7消息颜色: " + translateColorCodes(messageColor) + "这是一条测试消息");
            }
        } else {
            player.sendMessage("§c设置失败，请重试。");
        }
        
        return true;
    }
    
    /**
     * 解析颜色参数（支持英文名称和颜色代码）
     */
    private String parseColor(String colorArg) {
        if (colorArg == null) {
            return null;
        }
        
        // 如果是颜色代码格式（&X）
        if (colorArg.startsWith("&") && colorArg.length() == 2) {
            if (isValidColorCode(colorArg)) {
                return colorArg;
            }
            return null;
        }
        
        // 尝试匹配英文或中文颜色名称
        String lowerArg = colorArg.toLowerCase();
        return COLOR_NAME_MAP.getOrDefault(lowerArg, null);
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
     * 发送帮助信息
     */
    private void sendHelp(Player player) {
        player.sendMessage("§6========== §e聊天颜色设置 §6==========");
        player.sendMessage("§7用法: /chatcolor <名字颜色> [消息颜色]");
        player.sendMessage("§7示例: /chatcolor red green (红色名字，绿色消息)");
        player.sendMessage("§7示例: /chatcolor &c &a (红色名字，绿色消息)");
        player.sendMessage("§7§n支持的英文颜色:");
        player.sendMessage("§7black, dark_blue, dark_green, dark_aqua");
        player.sendMessage("§7dark_red, dark_purple, gold, gray");
        player.sendMessage("§7dark_gray, blue, green, aqua");
        player.sendMessage("§7red, light_purple, yellow, white");
        player.sendMessage("§7§n支持的颜色代码:");
        player.sendMessage("§7&0黑, &1深蓝, &2深绿, &3深青, &4深红, &5深紫");
        player.sendMessage("§7&6金黄, &7灰, &8深灰, &9蓝, &a绿, &b青");
        player.sendMessage("§7&c红, &d粉, &e黄, &f白");
        player.sendMessage("§6================================");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 || args.length == 2) {
            // 补全颜色名称和颜色代码
            List<String> completions = new ArrayList<>();
            
            // 添加英文颜色名称
            completions.addAll(COLOR_NAME_MAP.keySet().stream()
                .filter(name -> name.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList()));
            
            // 添加颜色代码
            String[] colorCodes = {"&0", "&1", "&2", "&3", "&4", "&5", "&6", "&7", 
                                  "&8", "&9", "&a", "&b", "&c", "&d", "&e", "&f"};
            for (String code : colorCodes) {
                if (code.startsWith(args[args.length - 1])) {
                    completions.add(code);
                }
            }
            
            return completions;
        }
        
        return new ArrayList<>();
    }
}
