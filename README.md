# 五子棋 Gobang

一个基于 Java Swing 的五子棋单机游戏，内置多种强度的电脑 AI，支持人机对弈、电脑自动对局和命令行模式。纯 JDK 实现，无任何第三方依赖。

## 功能特点

- **多种 AI 策略** — 提供三种不同强度的电脑 AI，可通过配置文件自由切换
- **自适应棋盘** — 支持自定义棋盘大小，界面和 AI 自动适配
- **完整的对局管理** — 悔棋（任意步）、自动代下、电脑托管
- **复盘功能** — 从头回放棋局，支持前进/后退，可从任意历史局面恢复对弈
- **棋局持久化** — 保存和载入对局，包含完整的历史步骤信息
- **平局检测** — 当双方均无法凑成五子时，自动判定平局
- **高性能** — 后台模拟 AI 对局约 2ms 即可完成一局，适合大规模胜率统计
- **命令行模式** — 支持在终端中以文本方式进行对弈

## 快速开始

```bash
# 编译
mvn compile

# 运行图形界面
java -cp target/classes GobangGame

# 运行命令行模式
java -cp target/classes ChessConsole

# 打包为 JAR
mvn package
java -jar target/gobang-game-1.0.0-SNAPSHOT.jar
```

## AI 策略

项目内置三种 AI，棋力由弱到强：

| AI | 类型 | 特点 |
|:---|:---|:---|
| **Default** | 单步评估 | 基线策略，攻防分离比较，同分随机选择 |
| **SmartEvalAI** | 单步评估（优化） | 增加组合威胁检测（双三、四三），智能同分打破，与 Default 同等速度 |
| **SmartSearchAI** | Alpha-Beta 搜索 | 4 层深度搜索，基于 SmartEvalAI 的评估函数，强制着法剪枝 |

在 `src/main/resources/ai.properties` 中配置黑白双方使用的 AI 类名：

```properties
black=SmartEvalAI
white=SmartSearchAI
```

### AI 扩展

实现 `AI` 接口并编写 `compute()` 方法，在 `ai.properties` 中配置类名即可接入。可通过 `AIBenchmarkTest` 或 `Chess.main()` 进行大规模自动对局来评估新 AI 的胜率和性能。

```bash
# 运行 AI 基准测试
mvn test

# 后台快速对局 1000 局（统计胜率和耗时）
java -cp target/classes Chess
```

## 游戏界面

### 游戏菜单
- **开始游戏** — 选择执黑（先手）、执白（后手）、自动对局或双人对局
- **重定义棋盘大小** — 输入格式 `宽x高`，重新定义后需开始新游戏

### 自动菜单
- **自动下一子** — 由电脑代下一步
- **电脑托管** — 将当前棋手交由电脑操控

### 历史菜单
- **悔棋** — 回退上一步（对手为电脑时自动回退两步）
- **保存棋局** / **载入棋局** — 持久化当前对局状态

### 复盘菜单
- **从头观看过程** — 进入复盘模式，逐步回放
- **前进** / **后退** — 也可用鼠标左键前进、右键后退
- **退出复盘模式** — 恢复到复盘前的棋局
- **从当前局面开始下棋** — 在历史节点上重新开始对弈

## 命令行模式

运行 `java -cp target/classes ChessConsole` 进入命令行交互。

| 命令 | 缩写 | 说明 |
|:---|:---|:---|
| `new` | `n` | 开始新棋局 |
| `chess m,n` | | 重定义棋盘为 m×n |
| `state` | `st` | 显示当前棋盘 |
| `cmove` | `c` | 电脑代下一步 |
| `comp` | | 电脑托管 |
| `roll` | `r` | 悔棋 |
| `review` | | 进入复盘模式 |
| `resume` | | 退出复盘 |
| `play` | | 从当前复盘局面开始对弈 |
| `m,n` | | 在坐标 (m,n) 处落子 |
| `save 文件名` | | 保存棋局 |
| `load 文件名` | | 载入棋局 |
| `exit` | | 退出 |

## 技术栈

- Java 17
- Apache Maven
- JUnit Jupiter 5（仅测试）
- Swing / AWT（GUI）

## 许可

本程序可自由复制使用。联系方式：hzjiyi@gmail.com
