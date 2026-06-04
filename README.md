# Orcak - Minecraft 插件

[![Minecraft Version](https://img.shields.io/badge/Minecraft-1.21.4-brightgreen.svg)](https://www.minecraft.net/)
[![Platform](https://img.shields.io/badge/Platform-Paper%20%7C%20Folia-blue.svg)](https://papermc.io/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

前言：功能大多都是生存服务器用得到的，但我一开始是给类2B2T服务器开发的，也不打算做成ESS这种插件。不知道为什么要取这个名字，莫名其妙想到的，就决定用了（

Orcak 是一个综合性的 Minecraft 生存服务器插件，提供自定义帮助系统、玩家数据统计、聊天管理，玩家跟随等功能。完全兼容 Folia 区域化多线程架构。

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

### 🎨 聊天颜色自定义
- **个性化设置**：玩家可自定义名字颜色和聊天消息颜色
- **多种格式**：支持英文名称（red, blue）、中文名称（红, 蓝）和颜色代码（&c, &9）
- **独立命令**：使用 `/chatcolor` 命令，简单便捷
- **持久化保存**：颜色偏好存储在数据库中，重启不丢失
- **默认配置**：管理员可在 config.yml 中设置全局默认颜色
- **即时生效**：设置后立即应用，无需重启服务器
- **智能补全**：支持 Tab 键自动补全颜色名称和代码

###  聊天频率限制与禁言系统
- **防刷屏保护**：限制玩家发送消息的频率（默认1秒/条）
- **防重复检测**：阻止短时间内发送相同内容（默认10秒窗口）
- **玩家禁言**：管理员可禁言/解禁特定玩家（`/orcak mute/unmute`）
- **代发消息**：管理员可以任何玩家名义发送消息（`/orcak say`）
- **管理员豁免**：OP 或有权限的管理员不受限制
- **友好提示**：可自定义警告消息，支持颜色代码
- **完全可配置**：所有参数均可在 config.yml 中调整

###  自杀命令
- **快捷自杀**：提供 `/514` 和 `/k` 两个快捷命令
- **即死效果**：直接将玩家生命值设置为0
- **简单便捷**：无需参数，输入命令即可自杀

### 🌍 区块实体管理
- **生物数量限制**：限制单个区块的最大生物数量（默认20个）
- **实体总数控制**：限制区块内所有实体的总数（默认100个）
- **智能清理**：可选择删除最旧的实体，保留新生成的
- **实时监控**：定期检查并自动清理超额实体
- **性能优化**：防止区块实体过多导致服务器卡顿

### 🗑️ 区块凋落物限制
- **物品数量控制**：限制单个区块的凋落物数量（默认2000个）
- **自动清理**：定期扫描并清理超额凋落物
- **详细日志**：可选的清理日志，记录物品类型和数量
- **服务器优化**：防止农场、刷怪塔等产生大量物品导致 lag

### ⚔️ 玩家伤害限制
- **单次伤害上限**：限制玩家一次攻击的最大伤害值（默认100）
- **默认关闭**：功能默认禁用，需要时手动开启
- **平衡战斗**：防止一击必杀，确保 PVP 公平性
- **管理员豁免**：OP 或有权限的管理员不受限制
- **动态提示**：显示原始伤害和限制值

### 🔧 管理员工具
- **数据同步**：批量或单独同步玩家的原版统计数据
- **数据编辑**：灵活修改玩家的游玩时间、击杀和死亡数据
- **禁言管理**：禁言/解禁违规玩家，维护聊天秩序
- **代发消息**：以指定玩家名义广播消息（不受禁言影响）
- **TAB 补全**：完整的命令自动补全支持，提升管理效率

### 📍 玩家位置锁定
- **坐标锁定**：可将玩家锁定在其当前位置或指定坐标，无法移动
- **持久化存储**：锁定状态保存到数据库，服务器重启后依然有效
- **灵活控制**：支持锁定到当前坐标或指定坐标（X, Y, Z, 世界）
- **便捷解锁**：提供专门命令解除位置锁定
- **权限管理**：需要特定权限才能使用锁定/解锁功能

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
| `/chatcolor <名字颜色> [消息颜色]` | 设置聊天颜色 | `orcak.command.chatcolor` |
| `/514` | 自杀 | 默认 |
| `/k` | 自杀（快捷方式） | 默认 |

### 管理员命令

| 命令 | 描述 | 权限 |
|------|------|------|
| `/orcak` | 显示管理命令帮助 | `orcak.admin` |
| `/orcak sync` | 同步所有玩家的原版数据 | `orcak.admin.sync` |
| `/orcak sync <玩家>` | 同步指定玩家的原版数据 | `orcak.admin.sync` |
| `/orcak set <玩家> <字段> <值>` | 手动修改玩家数据 | `orcak.admin.set` |
| `/orcak mute <玩家>` | 禁言指定玩家 | `orcak.admin.mute` |
| `/orcak unmute <玩家>` | 取消禁言指定玩家 | `orcak.admin.mute` |
| `/orcak say <玩家> <消息>` | 以指定玩家名义发送消息 | `orcak.admin.say` |
| `/orcak lockpos <玩家> [x] [y] [z] [世界]` | 锁定玩家坐标，无法移动 | `orcak.admin.lockpos` |
| `/orcak unlockpos <玩家>` | 解除玩家坐标锁定 | `orcak.admin.lockpos` |

**支持的字段：**
- `playtime` / `time` - 游玩时间（秒）
- `kills` / `kill` - 击杀数量
- `deaths` / `death` - 死亡数量

### 聊天颜色命令

#### 独立命令（推荐）

| 命令 | 描述 | 权限 |
|------|------|------|
| `/chatcolor <名字颜色> [消息颜色]` | 设置聊天颜色 | `orcak.command.chatcolor` |

**支持的格式：**
- **英文名称**：`red`, `blue`, `green`, `yellow`, `black`, `white` 等
- **中文名称**：`红`, `蓝`, `绿`, `黄`, `黑`, `白` 等
- **颜色代码**：`&c`, `&9`, `&a`, `&e` 等

**示例：**
```bash
# 使用英文名称
/chatcolor red green

# 使用中文名称
/chatcolor 红 绿

# 使用颜色代码
/chatcolor &c &a

# 只设置名字颜色
/chatcolor blue
/chatcolor 蓝
/chatcolor &9
```

#### 旧版命令（兼容）

| 命令 | 描述 | 权限 |
|------|------|------|
| `/orcak color <名字颜色> [消息颜色]` | 设置聊天颜色 | `orcak.command.color` |

**示例：**
```bash
# 只设置名字为红色
/orcak color &c

# 名字红色，消息绿色
/orcak color &c &a

# 名字金黄色，消息白色
/orcak color &6 &f
```

**可用颜色代码：**
- `&0` 黑色 | `&1` 深蓝 | `&2` 深绿 | `&3` 深青
- `&4` 深红 | `&5` 深紫 | `&6` 金黄 | `&7` 灰色
- `&8` 深灰 | `&9` 蓝色 | `&a` 绿色 | `&b` 青色
- `&c` 红色 | `&d` 粉色 | `&e` 黄色 | `&f` 白色

## ⚙️ 配置文件

### 目录结构
```
plugins/Orcak/
├── config.yml          # 插件主配置
├── players.db          # SQLite 数据库（自动生成）
└── command/
    └── help.txt        # 自定义帮助内容
```

### 配置文件说明

#### config.yml
```yaml
# 是否启用自定义help命令
enable-custom-help: true

# help.txt文件路径（相对于插件数据目录）
help-file-path: "command/help.txt"

# 聊天颜色设置
chat-colors:
  # 默认玩家名字颜色 (使用 Minecraft 颜色代码: &0-&9, &a-&f)
  default-name-color: "&f"
  # 默认聊天消息颜色
  default-message-color: "&7"

# 区块实体限制设置
chunk-entity-limits:
  # 是否启用区块实体数量限制
  enabled: true
  # 单个区块最大生物数量（包括动物、怪物等所有LivingEntity）
  max-mobs: 20
  # 单个区块最大实体总数（包括物品、经验球、矿车等所有实体）
  max-entities: 100
  # 检查间隔（tick），每隔多少tick检查一次区块实体数量
  check-interval: 20
  # 是否记录清理日志
  log-cleanup: false

# 聊天频率限制设置
chat-rate-limit:
  # 是否启用聊天频率限制
  enabled: true
  # 消息发送间隔时间（秒），默认1秒
  interval-seconds: 1
  # 是否启用防重复消息功能
  anti-duplicate: true
  # 重复消息检测的时间窗口（秒），在此时间内发送相同消息会被拦截
  duplicate-window: 10
  # 管理员是否不受限制
  bypass-for-admins: true
  # 当消息被拦截时是否发送提示给玩家
  send-warning: true
  # 警告消息内容
  warning-message: "&c请不要频繁发送消息！"
  duplicate-warning-message: "&c请不要重复发送相同的消息！"
  # 禁言警告消息内容
  mute-warning-message: "&c你已被禁言，无法发送消息！"

# 区块凋落物限制设置
chunk-item-limits:
  # 是否启用区块凋落物数量限制
  enabled: true
  # 单个区块最大凋落物数量（物品实体）
  max-items: 2000
  # 检查间隔（tick），每隔多少tick检查一次区块凋落物数量
  check-interval: 40
  # 当超过限制时，是否删除最旧的凋落物
  remove-oldest: true
  # 是否记录清理日志
  log-cleanup: false

# 玩家伤害限制设置
damage-limit:
  # 是否启用玩家单次伤害限制
  enabled: false
  # 玩家单次造成的最大伤害值（默认100）
  max-damage: 100.0
  # 管理员是否不受限制
  bypass-for-admins: true
  # 当伤害被限制时是否发送提示给攻击者
  send-warning: true
  # 警告消息内容（{damage}会被替换为实际伤害值，{max}会被替换为最大伤害值）
  warning-message: "&c你的攻击伤害过高！原始伤害: {damage}, 限制为: {max}"
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
| `orcak.command.chatcolor` | 使用 /chatcolor 命令 | 所有玩家 |
| `orcak.command.color` | 使用 /orcak color 命令（旧版） | 所有玩家 |
| `orcak.chat.bypass` | 绕过聊天频率限制 | OP |
| `orcak.damage.bypass` | 绕过伤害限制 | OP |
| `orcak.admin` | Orcak 管理员总权限 | OP |
| `orcak.admin.sync` | 同步原版数据 | OP |
| `orcak.admin.set` | 修改玩家数据 | OP |
| `orcak.admin.mute` | 禁言/取消禁言玩家 | OP |
| `orcak.admin.say` | 以指定玩家名义发送消息 | OP |
| `orcak.admin.lockpos` | 锁定/解锁玩家坐标 | OP |

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

### 自杀命令
```bash
# 使用 /514 自杀
/514

# 使用 /k 自杀（快捷方式）
/k

# 效果：直接将生命值设置为0，玩家死亡
```

### 设置聊天颜色
```bash
# 使用新命令（推荐）
# 使用英文名称
/chatcolor red green

# 使用中文名称
/chatcolor 红 绿

# 使用颜色代码
/chatcolor &c &a

# 只设置名字颜色
/chatcolor blue

# 使用旧版命令（兼容）
/orcak color &c &a

# 查看效果（聊天时会显示）
Steve: 这是一条测试消息
```

**聊天效果示例：**
- 红色名字 + 绿色消息：`§cSteve§a 你好世界！`
- 金黄色名字 + 白色消息：`§6Alex§f 大家好！`

### 配置区块实体限制
```yaml
# config.yml
chunk-entity-limits:
  enabled: true          # 启用限制
  max-mobs: 20           # 每个区块最多20个生物
  max-entities: 100      # 每个区块最多100个实体
  check-interval: 20     # 每20tick(1秒)检查一次
  log-cleanup: false     # 不记录清理日志
```

### 配置聊天频率限制
```yaml
# config.yml
chat-rate-limit:
  enabled: true              # 启用限制
  interval-seconds: 1        # 每条消息间隔1秒
  anti-duplicate: true       # 启用防重复
  duplicate-window: 10       # 10秒内不允许重复消息
  bypass-for-admins: true    # 管理员豁免
  send-warning: true         # 发送警告
  warning-message: "&c请不要频繁发送消息！"
  duplicate-warning-message: "&c请不要重复发送相同的消息！"
  mute-warning-message: "&c你已被禁言，无法发送消息！"  # 禁言提示
```

### 位置锁定功能
```bash
# 锁定玩家到当前坐标
/orcak lockpos Steve

# 锁定玩家到指定坐标
/orcak lockpos Steve 100 64 200 world

# 解除玩家位置锁定
/orcak unlockpos Steve
```

### 禁言与代发消息
```bash
# 禁言玩家
/orcak mute Steve

# 取消禁言
/orcak unmute Steve

# 以指定玩家名义发送消息（不受禁言影响）
/orcak say Steve 这是一条系统公告
```

### 配置凋落物限制
```yaml
# config.yml
chunk-item-limits:
  enabled: true          # 启用限制
  max-items: 2000        # 每个区块最多2000个凋落物
  check-interval: 40     # 每40tick(2秒)检查一次
  remove-oldest: true    # 删除最旧的凋落物
  log-cleanup: false     # 不记录清理日志
```

### 配置伤害限制
```yaml
# config.yml
damage-limit:
  enabled: false             # 默认关闭，需要时开启
  max-damage: 100.0          # 最大伤害100
  bypass-for-admins: true    # 管理员豁免
  send-warning: true         # 发送警告
  warning-message: "&c你的攻击伤害过高！原始伤害: {damage}, 限制为: {max}"
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
- 在线玩家的游玩时间实时累加（每秒更新）
- 击杀和死亡事件即时记录
- 无需等待下线即可查看最新数据

### 聊天颜色系统
- 提供 `/chatcolor` 独立命令，支持英文/中文名称和颜色代码
- 监听 `AsyncPlayerChatEvent` 动态应用颜色
- 支持 Folia 异步线程环境
- 颜色代码自动转换（& → §）
- 优先使用玩家自定义颜色，回退到全局默认配置
- 智能 Tab 补全支持

### 禁言与代发消息系统
- **禁言功能**：管理员可禁言/解禁玩家，禁言状态持久化存储
- **代发消息**：管理员可以任何玩家名义发送消息，不受禁言影响
- **权限控制**：严格限制仅管理员可使用（`orcak.admin.mute`, `orcak.admin.say`）
- **在线通知**：被禁言/解禁的玩家在线时会收到即时通知
- **日志记录**：代发消息会在控制台记录操作日志

### 区块管理系统
- **智能调度**：Folia 环境使用主线程同步任务，非 Folia 使用异步任务
- **延迟启动**：监听 WorldLoadEvent，确保世界完全加载后启动
- **双重保护**：事件拦截 + 定期扫描，全面控制实体数量
- **性能优化**：可配置的检查间隔，平衡性能和实时性

### 聊天频率限制
- **高优先级处理**：使用 HIGH 优先级确保及时拦截
- **时间窗口检测**：精确计算消息间隔和重复检测
- **禁言检查**：在频率检查之前优先检查禁言状态
- **线程安全**：使用 ConcurrentHashMap 存储玩家消息记录
- **Folia 兼容**：支持 Folia 服务器的异步调度

### 伤害限制系统
- **HIGHEST 优先级**：在其他插件之后处理，避免冲突
- **动态调整**：只修改超过限制的傷害，不影响正常战斗
- **占位符支持**：警告消息支持 {damage} 和 {max} 动态替换
- **管理员豁免**：灵活的权限控制系统

## 📝 开发信息

### 构建项目
```bash
mvn clean package
```

编译后的 JAR 文件位于 `target/Orcak-1.4.jar`

### 依赖项
- **Paper API** 1.21.4-R0.1-SNAPSHOT
- **SQLite JDBC** 3.45.1.0
- **JSON Simple** 1.1.1（用于解析原版统计文件）

### 项目结构
```
src/main/java/top/mcocet/orcak/
├── Orcak.java                     # 主类
├── ConfigManager.java             # 配置管理
├── DatabaseManager.java           # 数据库管理
├── PlayerStats.java               # 玩家数据模型
├── HelpCommandExecutor.java       # 帮助命令监听器
├── StatCommandExecutor.java       # 统计命令执行器
── OrcakCommand.java              # 管理命令执行器
├── ChatColorCommand.java          # 聊天颜色命令执行器
├── SuicideCommand.java            # 自杀命令执行器
├── PlayerDataListener.java        # 玩家数据事件监听器
├── ChatColorListener.java         # 聊天颜色监听器
├── ChatRateLimitListener.java     # 聊天频率限制监听器
├── ChunkEntityLimitListener.java  # 区块实体限制监听器
├── ChunkItemLimitListener.java    # 区块凋落物限制监听器
└── DamageLimitListener.java       # 伤害限制监听器
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