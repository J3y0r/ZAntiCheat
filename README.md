# ZAntiCheat

ZAntiCheat 是一个面向 Bukkit / Spigot / Paper / Folia 服务端的轻量级反作弊插件，核心目标是用较低的运行成本检测常见作弊行为，并为服务器管理员提供可配置、可扩展的处罚与告警流程。

插件入口类为 `cn.jeyor1337.zanticheat.Main`。

## 特性

- 支持 Minecraft 1.8 到 1.21 服务端环境
- 支持 Folia 调度模型
- 兼容 Geyser / Floodgate 场景，并可区分 Java 与 Bedrock 玩家检测策略
- 覆盖移动、战斗、交互、数据包、背包、玩家行为等多类检测
- 支持配置化启用、禁用、阈值、回退与处罚命令
- 支持 Discord Webhook、数据库与 Redis 消息桥相关能力
- 提供公开 API，便于其他插件临时关闭检测或监听违规 / 处罚事件

## 支持的检测类型

当前检测项按领域划分为：

- Movement：飞行、速度、跳跃、摔落伪造、水上行走、爬梯、载具、鞘翅、三叉戟等
- Combat：KillAura、Reach、Criticals、AutoClicker、Velocity、FakeLag 等
- Interaction：AirPlace、FastPlace、BlockPlace、GhostBreak、FastBreak、Scaffold 等
- Packet：MorePackets、Timer、BadPackets 等
- Inventory：Sorting、ItemSwap 等
- Player：AutoBot、SkinBlinker 等

服务端内可通过以下命令查看当前启用状态：

```text
/zac checks
```

## 命令

主命令：

```text
/zanticheat
/zac
/anticheat
/ac
```

可用子命令：

```text
/zac reload
/zac checks
/zac alerts
/zac teleport <world> <x> <y> <z> [yaw] [pitch]
/zac tps
/zac client <player>
/zac ping <player>
/zac cps <player>
```

举报命令：

```text
/report <player>
/jb <player>
```

## 权限

| 权限 | 说明 |
| --- | --- |
| `zanticheat.checks` | 使用检查列表命令 |
| `zanticheat.reload` | 重载配置 |
| `zanticheat.alerts.notify` | 接收检测告警 |
| `zanticheat.alerts.toggle` | 切换个人告警显示 |
| `zanticheat.alerts.teleport` | 使用告警传送功能 |
| `zanticheat.alerts` | 包含全部告警相关权限 |
| `zanticheat.client` | 查看玩家客户端品牌 |
| `zanticheat.tps` | 查看插件计算的 TPS |
| `zanticheat.ping` | 查看玩家延迟 |
| `zanticheat.cps` | 查看玩家 CPS |
| `zanticheat.report` | 使用举报命令 |
| `zanticheat.bypass` | 绕过全部检测 |
| `zanticheat.*` | 包含全部插件权限 |

## 构建

推荐本地使用 Java 21 运行 Gradle，以贴近 CI 环境；项目 Gradle toolchain 会将插件编译为 Java 8 字节码。

```bash
./gradlew shadowJar
```

默认输出目录由 `gradle.properties` 中的 `destinationPath` 控制，默认值为：

```properties
destinationPath = build/libs/
```

如果需要直接部署到本地测试服，可以把该路径改为服务器的 `plugins/` 目录。

## 发布到 GitHub Packages

```bash
./gradlew publish
```

发布需要环境变量：

```text
GITHUB_ACTOR
GITHUB_TOKEN
```

## 开发环境

项目依赖以下本地或随仓库提交的构件：

- `comp/spigot-1.8.8-R0.1-SNAPSHOT-latest.jar`
- `impl/LightInjector-1.0.2-forked.jar`
- `impl/multiversion/` 下的多版本实现

主要外部依赖包括：

- Netty
- MySQL Connector/J
- c3p0
- Jedis
- FoliaLib
- Floodgate API
- ConfigUpdater
- Lombok

## 配置

插件配置集中在 `config.yml` 中，包含：

- 全局启用状态
- 消息文本与占位符
- 告警与处罚策略
- Discord Webhook
- 数据库与 Redis 支持
- Geyser / Floodgate 相关处理
- TPS、Ping、版本兼容相关保护
- 每个检查项的启用状态、阈值、回退和处罚命令

修改配置后可使用：

```text
/zac reload
```

## 开发 API

公开 API 包名：

```text
cn.jeyor1337.zanticheat.api
```

可监听事件：

```text
LACViolationEvent
LACPunishmentEvent
```

可通过 `ZACApi` 对指定玩家临时关闭某个检查：

```java
ZACApi antiCheatApi = ZACApi.getInstance();
antiCheatApi.disableDetection(player, checkName, durationMils);
```

## 兼容性说明

插件会监听移动、战斗、方块交互、背包和数据包行为。任何修改玩家移动、攻击、挖掘、放置、击退或数据包节奏的插件，都可能影响检测结果。

已在项目中声明软依赖或兼容处理的插件 / 环境包括：

- Geyser-Spigot
- Floodgate
- ViaVersion
- GSit
- mcMMO
- ValhallaMMO
- VeinMiner
- AureliumSkills
- ExecutableItems
- EnchantsSquared
- Folia

## 测试建议

当前仓库没有自动化测试目录。涉及检测逻辑、数据包处理、调度、版本适配或配置项的修改，建议至少在代表性版本上进行手动测试：

- Minecraft 1.8
- Minecraft 1.9
- Minecraft 1.12
- Minecraft 1.21

对于 Folia、Geyser / Floodgate 或第三方兼容场景，应单独启动对应环境验证。
