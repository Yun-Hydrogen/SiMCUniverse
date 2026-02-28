# 🎮 game - 游戏玩法模块
> 模块ID: `game`
---

### 当前已实现玩法（function）
- `basedefend`：据点防御

### 玩家命令
- `/si-game basedefend`
显示 `basedefend` 最近一次结算的玩家伤害排行。

### 管理员sudo节点命令
- `/si-game sudo reload`
重载 game 模块配置与玩法配置。

- `/si-game sudo <functionID> list`
列出某玩法下所有有效游戏配置（输出游戏名与游戏ID）。

- `/si-game sudo <functionID> start <gameid>`
启动指定玩法下的指定游戏。

- `/si-game sudo <functionID> stop <gameid>`
中止指定玩法下的指定游戏。

- `/si-game sudo <functionID> restart <gameid>`
重启指定玩法下的指定游戏。

- `/si-game sudo <functionID> make-victory <gameid>`
强制指定游戏立即胜利（游戏必须正在进行）。

- `/si-game sudo <functionID> make-defeat <gameid>`
强制指定游戏立即失败（游戏必须正在进行）。

---

### 原始主配置文件
```yaml
# ================================
# SiMCUniverse - Game 模块主配置
# ================================
# 说明：
# 1) 本文件用于设置 Game 模块提示消息。
# 2) 各玩法配置在 game/<functionID>/ 目录中。

messages:
  module-disabled: "&cGame 模块已禁用。"
  unknown-function: "&c未知玩法: %function_id%"
  unknown-game: "&c未知游戏ID: %game_id%"
  already-running: "&e该游戏已在运行: %game_id%"
  not-running: "&c该游戏未在运行: %game_id%"
  reload-success: "&aGame 配置已重载。"
  start-success: "&a已启动游戏 %game_name% (&f%game_id%&a)"
  stop-success: "&e已中止游戏 %game_id%"
  force-victory-success: "&a已强制胜利: %game_id%"
  force-defeat-success: "&e已强制失败: %game_id%"
  list-title: "&6[%function_id%] 可用游戏列表:"
  list-line: "&7- &f%game_id% &8| &e%game_name%"

  victory-broadcast: "&a据点防御挑战成功：%game_name%"
  defeat-broadcast: "&c据点防御挑战失败：%game_name%"

  # BossBar 标题占位符：
  # %game_name% %time_left% %bars_left% %hp_left% %hp_total%
  # 说明：BossBar 进度条显示“当前这管血”的剩余进度；每掉一整管会切换到下一管并变色。
  bossbar-title: "&6%game_name% &7| &f剩余时间: &e%time_left%s &7| &f%hp_left%&7/&f%hp_total% %bars_left%"

  no-last-ranking: "&7该玩法还没有最近一次结算记录。"
  ranking-title: "&6=== basedefend 最近一次伤害排行 ==="
  ranking-line: "&e%rank%. &f%player% &7- &c%damage%"
```

---

### basedefend 据点防御游戏配置文件 示例配置
```yaml
# ================================
# SiMCUniverse - Game/basedefend 示例配置
# ================================

# 全局唯一游戏id，不可重复，推荐小写字母 + 数字 + 下划线/连接线组合。
id: basedefend_demo

# 游戏名称
name: "&c赤夜据点防御"

# 据点球心位置
center:
  world: world
  x: 0
  y: 64
  z: 0

# 半径
radius: 30

# 需要造成的伤害总量
required-total-damage: 50000
# 叠加血条数
hp-bars: 50
#时间限制（秒）
time-limit-seconds: 300

# 生成和统计的生物，支持 NBT：minecraft:zombie{Health:40.0f,CustomName:'{"text":"狂暴僵尸"}'}
counted-mobs:
  - minecraft:zombie
  - minecraft:skeleton
  - minecraft:creeper
  - minecraft:zombie{Health:40.0f,CustomName:'{"text":"狂暴僵尸"}'}

# 每波刷怪个数（固定数量）
spawn-count-per-wave: 18
# 波次间隔（秒）
spawn-wave-interval-seconds: 1

# 边界粒子
boundary-particle:
  enabled: true # 是否启用
  interval-ticks: 20 # 刷新时间间隔（tick）
  points: 64 # 个数

sounds:
  running: "" # 游戏进行时的音效，循环播放
  victory: "minecraft:ui.toast.challenge_complete" # 游戏胜利时的音效
  defeat: "minecraft:entity.wither.death" # 游戏失败时的音效
  interval-ticks: 40 # 循环播放间隔
  volume: 1.0
  pitch: 1.0
  finish-loops: 3 # 当游戏胜利/失败时，对应音效重复播放次数

on-victory:
  - "tellraw @a {\"text\":\"据点防御胜利！\",\"color\":\"green\"}"

on-defeat:
  - "tellraw @a {\"text\":\"据点防御失败！\",\"color\":\"red\"}"
```
