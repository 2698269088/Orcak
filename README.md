# Orcak - Minecraft 插件

[![Minecraft Version](https://img.shields.io/badge/Minecraft-1.21.4-brightgreen.svg)](https://www.minecraft.net/)
[![Platform](https://img.shields.io/badge/Platform-Paper%20%7C%20Folia-blue.svg)](https://papermc.io/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

Orcak 是一个功能丰富的 Minecraft 服务器插件，提供自定义帮助系统、玩家数据统计、跨版本支持等功能。完全兼容 Folia 区域化多线程架构。

## ✨ 主要功能

### 📋 自定义帮助系统
- **智能命令拦截**：通过事件监听非侵入式接管 `/help` 命令
- **权限分流**：普通玩家显示自定义帮助，管理员使用原版帮助
- **灵活调用**：支持 `/help orcak` 强制查看插件帮助
- **配置文件驱动**：帮助内容存储在 `command/help.txt`，支持实时修改

### 📊 玩家数据统计系统
- **SQLite 数据库**：轻量级本地存储，自动管理玩家数据
- **实时统计**：记录游玩时间、击杀数、死亡数、最后登录时间
- **数据查询**：`/stat` 命令查看自己或他人的游戏数据
- **原版同步**：`/orcak sync` 一键从原版统计文件迁移数据
- **手动修改**：`/orcak set` 直接编辑数据库中的统计数据

### 🔧 管理员工具
- **数据同步**：批量或单独同步玩家的原版统计数据
- **数据编辑**：灵活修改玩家的游玩时间、击杀和死亡数据
- **TAB 补全**：完整的命令自动补全支持，提升管理效率

### 🌐 兼容性
- **Folia 支持**：完全兼容 Folia 的区域化多线程架构
- **线程安全**：使用 ReadWriteLock 保护数据库操作
- **跨版本**：配合 ViaVersion/ViaBackwards 支持多版本客户端

## 📦 安装方法

### 前置要求
- Minecraft 服务器核心：Paper 1.21.4+ 或 Folia 1.21.4+
- Java 21 或更高版本

### 安装步骤
1. 下载最新版本的 `Orcak.jar`
2. 将文件放入服务器的 `plugins` 文件夹
3. 重启服务器
4. 插件会自动创建配置文件和数据库

## 📖 命令列表

### 玩家命令

| 命令 | 描述 | 权限 |
|------|------|------|
| `/help` | 显示自定义帮助信息（普通玩家） | 默认 |
| `/help orcak` | 强制显示插件帮助内容 | 默认 |
| `/stat` | 查看自己的游戏统计数据 | `orcak.command.stat` |
| `/stat <玩家名>` | 查看指定玩家的游戏数据 | `orcak.command.stat` |

### 管理员命令

| 命令 | 描述 | 权限 |
|------|------|------|
| `/orcak` | 显示管理命令帮助 | `orcak.admin` |
| `/orcak sync` | 同步所有玩家的原版数据 | `orcak.admin.sync` |
| `/orcak sync <玩家>` | 同步指定玩家的原版数据 | `orcak.admin.sync` |
| `/orcak set <玩家> <字段> <值>` | 手动修改玩家数据 | `orcak.admin.set` |

**支持的字段：**
- `playtime` / `time` - 游玩时间（秒）
- `kills` / `kill` - 击杀数量
- `deaths` / `death` - 死亡数量

## ⚙️ 配置文件

### 目录结构
```
plugins/Orcak/
├── config.yml          # 插件主配置（未来扩展用）
├── players.db          # SQLite 数据库（自动生成）
└── command/
    └── help.txt        # 自定义帮助内容
```

### 编辑帮助内容
直接编辑 `plugins/Orcak/command/help.txt` 文件，修改后无需重启服务器，玩家下次输入 `/help` 即可看到更新。

**示例格式：**
```
===== 服务器帮助指南 =====

【基础指令】
/help - 显示此帮助信息
/spawn - 传送到出生点

【账户安全】
/register <密码> - 注册账号
/login <密码> - 登录账号

========================
```

## 🔐 权限节点

| 权限 | 描述 | 默认 |
|------|------|------|
| `orcak.bypass.help` | 绕过自定义帮助，使用原版 | OP |
| `orcak.command.stat` | 使用 /stat 命令 | 所有玩家 |
| `orcak.admin` | Orcak 管理员总权限 | OP |
| `orcak.admin.sync` | 同步原版数据 | OP |
| `orcak.admin.set` | 修改玩家数据 | OP |

## 💡 使用示例

### 查看统计数据
```bash
# 查看自己的数据
/stat

# 查看其他玩家的数据
/stat Steve
```

**输出示例：**
```
========== 玩家统计 ==========
玩家名称：Steve
在线状态：在线
上次登录：2026-05-18 12:30:45
游玩时间：5小时 23分钟 15秒
击杀数量：42
死亡数量：18
K/D 比率：2.33
================================
```

### 同步原版数据
```bash
# 同步所有玩家
/orcak sync

# 同步指定玩家
/orcak sync Alex
```

### 修改数据
```bash
# 设置玩家的游玩时间为 1 小时（3600秒）
/orcak set Steve playtime 3600

# 设置击杀数为 100
/orcak set Steve kills 100
```

## 🛠️ 技术特性

### 线程安全
- 使用 `ReadWriteLock` 保护数据库读写操作
- 支持 Folia 的并发环境
- 内存缓存减少数据库访问频率

### 数据持久化
- SQLite 数据库自动保存
- 玩家下线时自动保存会话数据
- 插件禁用时优雅关闭数据库连接

### 实时计算
- 在线玩家的游玩时间实时累加
- 击杀和死亡事件即时记录
- 无需等待下线即可查看最新数据

## 📝 开发信息

### 构建项目
```bash
mvn clean package
```

编译后的 JAR 文件位于 `target/Orcak-1.0.jar`

### 依赖项
- **Paper API** 1.21.4-R0.1-SNAPSHOT
- **SQLite JDBC** 3.45.1.0
- **JSON Simple** 1.1.1（用于解析原版统计文件）

### 项目结构
```
src/main/java/top/mcocet/orcak/
├── Orcak.java                 # 主类
├── ConfigManager.java         # 配置管理
├── DatabaseManager.java       # 数据库管理
├── PlayerStats.java           # 玩家数据模型
├── HelpCommandExecutor.java   # 帮助命令监听器
├── StatCommandExecutor.java   # 统计命令执行器
├── OrcakCommand.java          # 管理命令执行器
└── PlayerDataListener.java    # 玩家数据事件监听器
```

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

1. Fork 本仓库
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

## 📄 许可证

本项目采用 MIT 许可证 - 详见 [LICENSE](LICENSE) 文件

## 👥 作者

- **MCOCET** - [GitHub](https://github.com/2698269088)
