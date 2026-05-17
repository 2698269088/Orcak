package top.mcocet.orcak;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 配置文件管理类
 * 支持Folia的线程安全配置管理
 */
public class ConfigManager {
    
    private final JavaPlugin plugin;
    private FileConfiguration config;
    private File configFile;
    private final ReadWriteLock configLock = new ReentrantReadWriteLock();
    
    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        saveDefaultConfig();
        reloadConfig();
    }
    
    /**
     * 保存默认配置文件（如果不存在）
     */
    public void saveDefaultConfig() {
        if (configFile == null) {
            configFile = new File(plugin.getDataFolder(), "config.yml");
        }
        
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
    }
    
    /**
     * 重新加载配置文件（线程安全）
     */
    public void reloadConfig() {
        configLock.writeLock().lock();
        try {
            if (configFile == null) {
                configFile = new File(plugin.getDataFolder(), "config.yml");
            }
            
            config = YamlConfiguration.loadConfiguration(configFile);
            
            // 从jar中读取默认配置
            InputStream defaultStream = plugin.getResource("config.yml");
            if (defaultStream != null) {
                YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                    new java.io.InputStreamReader(defaultStream, StandardCharsets.UTF_8)
                );
                config.setDefaults(defaultConfig);
            }
        } finally {
            configLock.writeLock().unlock();
        }
    }
    
    /**
     * 获取配置文件（线程安全）
     */
    public FileConfiguration getConfig() {
        configLock.readLock().lock();
        try {
            if (config == null) {
                reloadConfig();
            }
            return config;
        } finally {
            configLock.readLock().unlock();
        }
    }
    
    /**
     * 保存配置文件（线程安全）
     */
    public void saveConfig() {
        configLock.writeLock().lock();
        try {
            if (config == null || configFile == null) {
                return;
            }
            
            try {
                getConfig().save(configFile);
            } catch (IOException e) {
                plugin.getLogger().severe("无法保存配置文件: " + e.getMessage());
            }
        } finally {
            configLock.writeLock().unlock();
        }
    }
    
    /**
     * 创建command目录和help.txt文件
     */
    public void createHelpFile() {
        File commandDir = new File(plugin.getDataFolder(), "command");
        if (!commandDir.exists()) {
            commandDir.mkdirs();
        }
        
        File helpFile = new File(commandDir, "help.txt");
        if (!helpFile.exists()) {
            try {
                helpFile.createNewFile();
                // 写入默认的help内容
                String defaultHelp = "===== 服务器帮助 =====\n" +
                    "/help - 显示此帮助信息\n" +
                    "/tpsp - 传送到出生点\n" +
                    "/stat - 查看玩家信息\n" +
                    "==================\n";
                Files.write(helpFile.toPath(), defaultHelp.getBytes(StandardCharsets.UTF_8));
                plugin.getLogger().info("已创建默认help.txt文件");
            } catch (IOException e) {
                plugin.getLogger().severe("无法创建help.txt文件: " + e.getMessage());
            }
        }
    }
    
    /**
     * 读取help.txt文件内容（线程安全）
     */
    public String getHelpContent() {
        File helpFile = new File(plugin.getDataFolder(), "command/help.txt");
        if (!helpFile.exists()) {
            createHelpFile();
        }
        
        try {
            return new String(Files.readAllBytes(helpFile.toPath()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            plugin.getLogger().severe("无法读取help.txt文件: " + e.getMessage());
            return "读取帮助文件失败，请联系管理员。";
        }
    }
}
