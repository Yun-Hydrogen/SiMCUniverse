# ⚔️ killscore - 击杀加分模块 
> 模块ID: `killscore`
---

### 玩家命令
- `/si-killscore help` 展示插件帮助
- `/si-killscore show` 展示玩家自己拥有的 killscore 分数

### 管理员sudo节点命令
- `/si-killscore sudo add <player> <score>` 
为 一名玩家（加入过游戏的玩家，在线/离线均可 添加 指定分数。分数为**整数，可以取0和负值（当为负值时相当于减少分数）**。
- `/si-killscore sudo set <player> <score>`
为 一名玩家（加入过游戏的玩家，在线/离线均可 设置 指定分数。分数为 **整数，可以取0和负值**。
- `/si-killscore sudo reset`
重置 **所有玩家** 的 killscore 分数。
- `/si-killscore sudo reload`
重载 killscore 模块的配置文件。

---

### 原始配置文件
```yaml
# ================================
# SiMCUniverse - KillScore 模块主配置
# ================================

# KillScore 外显名称（用于界面展示）
killscore-name: "击杀分"

# 是否显示 KillScore 排行榜（计分板侧边栏）
scoreboard:
  enabled: true

# 是否在击杀获得分数时，通过 ActionBar 提示加分
actionbar:
  notify-on-gain: true

# 玩家击杀下列生物时加分（名称空间ID: 分值，必须正整数）
kills:
  minecraft:zombie: 10
  minecraft:skeleton: 10
  minecraft:creeper: 10

# 玩家每次死亡时，分数按倍率折算
death-multiplier:
  #是否启用
  enabled: false
  #折算倍率，0~1的浮点数
  value: 0.8
  # 可选：ceil（向上取整）| floor（向下取整）| round（四舍五入）
  rounding: round
```

### 示例配置文件
```yaml
# ================================
# SiMCUniverse - KillScore 模块主配置
# ================================

# KillScore 外显名称（用于界面展示）
killscore-name: 青辉石

# 是否显示 KillScore 排行榜（计分板侧边栏）
scoreboard:
  enabled: true

# 是否在击杀获得分数时，通过 ActionBar 提示加分
actionbar:
  notify-on-gain: true

# 玩家击杀下列生物时加分（名称空间ID -> 分值，必须正整数）
kills:
  # 主世界
  minecraft:zombie: 20
  minecraft:zombie_villager: 20
  minecraft:husk: 20
  minecraft:skeleton: 20
  minecraft:creeper: 20
  minecraft:spider: 20
  minecraft:drowned: 20
  minecraft:enderman: 30
  minecraft:cave_spider: 30
  # 末地
  minecraft:shulker: 50
  minecraft_ender_dragon: 1000
  # 下界
  minecraft:blaze: 35
  minecraft:piglin: 25
  minecraft:piglin_brute: 25
  minecraft:witcher_skeleton: 30
  minecraft:witch: 1000

# 玩家每次死亡时，分数按倍率折算
death-multiplier:
  enabled: false
  value: 0.5
  # 可选：ceil（向上取整）| floor（向下取整）| round（四舍五入）
  rounding: round
```