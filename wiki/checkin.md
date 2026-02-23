# 📅 checkin - 签到模块
> 模块ID: `checkin`
---

### 玩家命令
- `/si-checkin`
打开签到 GUI。

### 管理员sudo节点命令
- `/si-checkin sudo reload`
模块配置文件重载。
- `/si-checkin sudo reset <checkinId>`
**重置** 某一个签到 **全部玩家** 的进度。
- `/si-checkin sudo set <player> <checkinId> <progress>`
为 一名玩家（加入过游戏的玩家，在线/离线均可 **指定** 指定 某一个签到任务的 天数进度，进度天数值**只能为自然数，且不超过改签到的总天数**。
- `/si-checkin sudo lock <checkinId>`
对 **全部玩家** 锁定 一个任务。
- `/si-checkin sudo unlock <checkinId>`
对 **全部玩家** 解锁 一个任务。

### 原始配置文件
```yaml
# ================================
# SiMCUniverse - Checkin 模块主配置
# ================================
# 说明：
# 签到任务定义文件请放在 checkin/checkin_conf/ 目录中，符合格式会自动加载。

messages:
  module-disabled: '&cCheckin 模块已禁用。'
  task-locked: '&c该签到任务尚未解锁。'
  already-checkin: '&e今天已经签过到了。'
  checkin-success: '&a签到成功：%task_title% &7| 进度 %progress%/%total%'
  task-completed: '&e该签到任务已完成。'
  reload-success: '&aCheckin 配置已重载。'
  reset-success: '&a已重置任务 %task_id% 的全服进度。'
  set-success: '&a已设置玩家 %player% 在任务 %task_id% 的进度为 %progress%。'
  lock-success: '&e已始终锁定任务 %task_id%。'
  unlock-success: '&a已始终解锁任务 %task_id%。'
```

### 示例签到配置文件
**happy_chinese_new_year.yml**
```yaml
# 签到任务示例（符合规范的 yml 会被动态加载）

#签到id，必须唯一，且只能包含小写字母、数字和下划线
id: happy_chinese_new_year

# 签到任务标题，支持颜色代码（&）和 HEX 颜色（#66CCFF）
title: "#FF7405 欢度春节！"
# 签到任务图标物品，使用名字空间格式（如 minecraft:emerald）
icon: "minecraft:firework_rocket"
# 签到任务介绍，支持多行和颜色代码
lore:
  - "#FF5CC1 新春快乐！"
  - "#FFBD13到2-28，每天登录就送#8DBCFF10抽！"

# 签到天数，正整数，表示需要签到多少天完成任务
days: 1
# 重置时间，格式为 HH:mm:ss，表示每天几点重置签到状态
reset-time: "00:00:00"

# 解锁条件，type 可选 none（无条件解锁）、after_task（完成指定任务后解锁）、always_locked（始终锁定）
unlock:
  # none | after_task | always_locked
  type: none
  task-id: ""

# 签到类型
repeat:
  # 是否循环签到，true 表示完成后重置继续签到，false 表示完成后不再可签到
  loop: true
  # 是否要求连续签到，true 表示必须连续签到才能完成任务，false 表示不要求连续签到
  continuous: false

# 签到奖励，按照签到天数对应命令列表
rewards:
  1:
    - "si-killscore sudo add %player% 1200"
    - "tellraw %player% {\"text\":\"已获得春节奖励！\",\"color\":\"light_red\"}"
  2:
    - "si-killscore sudo add %player% 1200"
    - "tellraw %player% {\"text\":\"已获得春节奖励！\",\"color\":\"light_red\"}"
```
- 签到配置文件yml请放在 `checkin/checkin_conf/` 目录中，文件名建议为**英文和连字符/下划线组合且唯一**，符合格式会自动加载。