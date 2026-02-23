# ❤️ livescore - 生存计分模块
> 模块ID: `livescore`
---


### 玩家命令
- `/si-livescore help`
显示模块命令帮助列表
- `/si-livescore show`
在聊天框显示 livescore 分数排行榜

### 管理员sudo节点命令
- `/si-livescore sudo add <player> <score>`
为 一名玩家（加入过游戏的玩家，在线/离线均可 添加 指定分数。分数为**整数，可以取0和负值（当为负值时相当于减少分数）**。
- `/si-livescore sudo set <player> <score>`
为 一名玩家（加入过游戏的玩家，在线/离线均可 设置 指定分数。分数为 **整数，可以取0和负值**。
- `/si-livescore sudo reset`
重置 **所有玩家** 的 livescore 分数。
- `/si-livescore sudo pause`
暂停 **所有玩家** 的 livescore 分数计算。
- `/si-livescore sudo start`
继续 **所有玩家** 的 livescore 分数计算。
- `/si-livescore sudo reload`
重载 livescore 模块的配置文件。

---

### 原始配置文件

```yaml
# ================================
# SiMCUniverse - LiveScore 模块主配置
# ================================

# 是否在 Tab 栏显示所有玩家的生存分
tab-display:
  enabled: true

# 每多少秒给在线玩家加多少分（正整数）
gain:
  #间隔时间
  interval-seconds: 60
  #每次加分分数
  amount: 1

# 玩家死亡后是否按倍数折算生存分
death-multiplier:
  #是否启用
  enabled: false
  #折算值，0~1的浮点数
  value: 0.8
  # 可选：ceil（向上取整）| floor（向下取整）| round（四舍五入）
  rounding: round

# 运行态配置（模块自动维护）
runtime:
  paused: false
```
### 示例配置文件

```yaml
# ================================
# SiMCUniverse - LiveScore 模块主配置
# ================================

# 是否在 Tab 栏显示所有玩家的生存分
tab-display:
  enabled: true

# 每多少秒给在线玩家加多少分（正整数）
gain:
  interval-seconds: 60
  amount: 1

# 玩家死亡后是否按倍数折算生存分
death-multiplier:
  enabled: false
  value: 0.8
  # 可选：ceil（向上取整）| floor（向下取整）| round（四舍五入）
  rounding: round

# 运行态配置（模块自动维护）
runtime:
  paused: false
```