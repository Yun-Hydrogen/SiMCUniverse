# 🎫 task -任务模块
> 模块ID:`task`
---

### 玩家命令
- `/si-task`
打开任务 GUI。

### 管理员sudo节点命令
- `/si-task sudo reload`
重载任务模块。
- `/si-task sudo reset <taskId>`
重置 **全部玩家** 某一任务的角度。

### 原始配置文件
```yaml
# ================================
# SiMCUniverse - Task 模块主配置
# ================================
# 说明：
# 1) 本文件用于设置任务模块提示消息与重置时间。
# 2) 任务配置文件请放在 task/task_conf/ 目录中，符合格式会自动加载。

messages:
  module-disabled: "&cTask 模块已禁用。"
  reload-success: "&aTask 配置已重载。"
  reset-success: "&a已重置任务 %task_id% 的全服进度。"
  task-complete: "&a任务完成：%task_title%"
  already-complete: "&e该任务已完成。"
  auto-task-tip: "&7该任务为自动统计，无需手动确认。"
  position-reached: "&a你已到达目标区域，点击任务即可确认完成。"
  position-not-reached: "&c你还未到达任务目标区域。"
  position-confirmed: "&a已确认位置任务完成。"

# 日常任务重置时间（格式 HH:MM:SS）
# 时区固定为北京时间 UTC+8
# 例：04:00:00 表示每天凌晨 4 点重置 daily 任务。
daily-reset-time: "04:00:00"

# 每周任务重置时间（格式 星期:HH:MM:SS）
# 时区固定为北京时间 UTC+8
# 星期支持：MONDAY/TUESDAY/WEDNESDAY/THURSDAY/FRIDAY/SATURDAY/SUNDAY
# 例：MONDAY:04:00:00 表示每周一凌晨 4 点重置 weekly 任务。
weekly-reset-time: "MONDAY:04:00:00"
```

### 示例配置文件
**daily_gain_xp.yml**
```yaml
# 示例：日常任务 - 获得经验
category: daily
name: "&b经验收集者"
description:
  - "&7获得指定经验值"
  - "&7实时统计，完成自动确认"
id: daily_gain_xp
icon: "minecraft:experience_bottle"
type: GAIN_XP

# 进度提示频率：every(进度每次更新时都在bossbar显示) | percent-10(每完成10%的进度显示一次) | percent-20(每完成20%的进度显示一次) | never(从不显示进度)
progress-notify: percent-10

# GAIN_XP 的 required 为数值
required: 120

rewards:
  - "tellraw %player% {\"text\":\"任务完成：经验收集者\",\"color\":\"blue\"}"
  - "give %player% minecraft:lapis_lazuli 16"
```
**daily_break_mine.yml**
```yaml
# ================================
# 任务配置示例：日常破坏方块任务
# ================================
# category：任务类别
#   - daily（日常）
#   - weekly（每周）
#   - achievement（成就）
category: daily

# name：任务名字（支持 & 颜色与 HEX 颜色，如 #66CCFF）
name: "#66CCFF矿石手艺"

# description：任务介绍（可多行，支持颜色）
description:
  - "&7挖掘各类矿石共计64个"
  - "#5EFFE2奖励600青辉石"

# id：内部显示名称（仅英文、小写数字、下划线、连接线）
id: daily_break_mine

# icon：图标物品（建议使用名字空间）
icon: "minecraft:iron_pickaxe"

# type：任务类型
# BREAK / PLACE / INTERACT / KILL / GAIN_XP / POSITION / PICKUP / CRAFT
type: BREAK

# required：需求
# 对于 BREAK/PLACE/INTERACT/KILL/PICKUP/CRAFT：
# 使用键值对，键为目标名字空间（可附加 NBT 字符串），值为数量
required:
  "minecraft:coal_ore": 1
  "minecraft:iron_ore": 1
  "minecraft:gold_ore": 1
  "minecraft:diamond_ore": 1
  "minecraft:emerald_ore": 1
  "minecraft:redstone_ore": 1
  "minecraft:lapis_ore": 1
  "minecraft:copper_ore": 1
  "minecraft:deepslate_coal_ore": 1
  "minecraft:deepslate_iron_ore": 1
  "minecraft:deepslate_gold_ore": 1
  "minecraft:deepslate_diamond_ore": 1
  "minecraft:deepslate_emerald_ore": 1
  "minecraft:deepslate_redstone_ore": 1
  "minecraft:deepslate_lapis_ore": 1
  "minecraft:deepslate_copper_ore": 1
  "minecraft:nether_gold_ore": 1
  "minecraft:nether_quartz_ore": 1

# total-required：总数量模式（仅 BREAK/PLACE/INTERACT/KILL/PICKUP/CRAFT 生效）
# false：按 required 中每一项各自数量完成
# 正整数：忽略 required 中各项数量，改为统计 required 中目标操作总和达到该值即完成
total-required: 64

# 进度提示频率：every(进度每次更新时都在bossbar显示) | percent-10(每完成10%的进度显示一次) | percent-20(每完成20%的进度显示一次) | never(从不显示进度)
progress-notify: percent-10

# rewards：奖励命令（无需 /），支持 %player% 占位符
rewards:
  - "si-killscore sudo add %player% 600"
```
**daily_interact_chest.yml**
```yaml
# 示例：日常任务 - 交互方块
category: daily
name: "#E2A600翻箱倒柜"
description:
  - "&7与箱子容器交互20次"
  - "#5EFFE2奖励：120青辉石"
id: daily_interact_chest
icon: "minecraft:chest"
type: INTERACT

required:
  "minecraft:chest": 1
  "minecraft:trapped_chest": 1
  "minecraft:ender_chest": 1
  "minecraft:barrel": 1
  "refurbished_furniture:oak_drawer": 1
  "refurbished_furniture:spruce_drawer": 1
  "refurbished_furniture:birch_drawer": 1
  "refurbished_furniture:jungle_drawer": 1
  "refurbished_furniture:acacia_drawer": 1
  "refurbished_furniture:dark_oak_drawer": 1
  "refurbished_furniture:crimson_drawer": 1
  "refurbished_furniture:warped_drawer": 1
  "refurbished_furniture:cherry_drawer": 1
  "refurbished_furniture:mangrove_drawer": 1
  "refurbished_furniture:oka_storage_cabinet": 1
  "refurbished_furniture:birch_storage_cabinet": 1
  "refurbished_furniture:spruce_storage_cabinet": 1
  "refurbished_furniture:acacia_storage_cabinet": 1
  "refurbished_furniture:dark_oak_storage_cabinet": 1
  "refurbished_furniture:crimson_storage_cabinet": 1
  "refurbished_furniture:warped_storage_cabinet": 1
  "refurbished_furniture:cherry_storage_cabinet": 1
  "refurbished_furniture:mangrove_storage_cabinet": 1
  
# total-required：总数量模式（仅 BREAK/PLACE/INTERACT/KILL/PICKUP/CRAFT 生效）
# false：按 required 中每一项各自数量完成（分别计数）
# 正整数：忽略 required 中每一项的单独需求数量，改为统计这些目标操作总和达到该值即完成
total-required: 20

# 进度提示频率：every(进度每次更新时都在bossbar显示) | percent-10(每完成10%的进度显示一次) | percent-20(每完成20%的进度显示一次) | never(从不显示进度)
progress-notify: percent-10


rewards:
  - "si-killscore sudo add %player% 120"
```
**daily_kill_mob_plus.yml**
```yml
# 示例：日常任务 - 击杀实体
category: daily
name: "&2惩罚时间到🌟~ (plus)"
description:
  - "&7击杀60个怪物，完成后可获得1200点青辉石奖励！"
  - "#5EFFE2奖励1200青辉石"
id: daily_kill_mob_plus
icon: "minecraft:diamond_sword"
type: KILL

required:
  # 主世界
  minecraft:zombie: 1
  minecraft:zombie_villager: 1
  minecraft:husk: 1
  minecraft:skeleton: 1
  minecraft:creeper: 1
  minecraft:spider: 1
  minecraft:drowned: 1
  minecraft:enderman: 1
  minecraft:cave_spider: 1
  # 末地
  minecraft:shulker: 1
  minecraft_ender_dragon: 1
  # 下界
  minecraft:blaze: 1
  minecraft:piglin: 1
  minecraft:piglin_brute: 1
  minecraft:witcher_skeleton: 1
  minecraft:witch: 1

# total-required：总数量模式（仅 BREAK/PLACE/INTERACT/KILL/PICKUP/CRAFT 生效）
# false：按 required 中每一项各自数量完成（分别计数）
# 正整数：忽略 required 中每一项的单独需求数量，改为统计这些目标操作总和达到该值即完成
total-required: 60

rewards:
  - "si-killscore sudo add %player% 1200"
```
**daily_patrol_base.yml**
```yml
category: daily
name: "#C65EFF巡查基地"
description:
  - "&7在基地中心半径20范围内巡查，然后点击完成任务"
  - "#5EFFE2奖励120青辉石"
id: daily_patrol_base
icon: "minecraft:compass"
type: POSITION

# POSITION 的 required 可写为坐标+半径字符串：(x,y,z,radius)
required: "(2195,72,2808,20)"

# 进度提示频率：every(进度每次更新时都在bossbar显示) | percent-10(每完成10%的进度显示一次) | percent-20(每完成20%的进度显示一次) | never(从不显示进度)
progress-notify: percent-10


rewards:
  - "si-killscore sudo add %player% 120"
```
**achievement_kill_mob_6600.yml**
```yaml
# 示例：成就任务 - 击杀实体
category: achievement
name: '&2 人在基地在'
description:
- '&7击杀6600个怪物，完成后可获得单种类无限弹药盒'
- '#5EFFE2单种类无限弹药盒'
id: achievement_kill_mob_6600
icon: minecraft:shield
type: KILL

required:
  # 主世界
  minecraft:zombie: 1
  minecraft:zombie_villager: 1
  minecraft:husk: 1
  minecraft:skeleton: 1
  minecraft:creeper: 1
  minecraft:spider: 1
  minecraft:drowned: 1
  minecraft:enderman: 1
  minecraft:cave_spider: 1
  # 末地
  minecraft:shulker: 1
  minecraft_ender_dragon: 1
  # 下界
  minecraft:blaze: 1
  minecraft:piglin: 1
  minecraft:piglin_brute: 1
  minecraft:witcher_skeleton: 1
  minecraft:witch: 1

# total-required：总数量模式（仅 BREAK/PLACE/INTERACT/KILL/PICKUP/CRAFT 生效）
# false：按 required 中每一项各自数量完成（分别计数）
# 正整数：忽略 required 中每一项的单独需求数量，改为统计这些目标操作总和达到该值即完成
total-required: 6600

rewards:
- give %player% tacz:ammo_box{Creative:1b} 1
progress-notify: percent-10
```
更多示例任务会在插件首次加载后释放到任务配置文件夹中。
