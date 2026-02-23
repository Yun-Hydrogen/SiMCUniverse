# 📚 quickenhance - 快捷附魔模块
> 模块ID: `quickenhance`

---

### 玩家命令
- 无玩家功能命令.

### 管理员sudo节点命令
- `/si-quickenhance sudo reload`
重载插件配置文件。

### 原始配置文件
```yml
# ================================
# SiMCUniverse - Quickenhance 模块主配置
# ================================
# 说明：
# 玩家手持附魔书（鼠标光标上）右键背包物品即可快速附魔。
# 经验消耗默认使用原版风格计算，并受最大等级限制。

cost:
  # 经验计算方式：vanilla | linear | exponential | constant
  mode: vanilla
  # 最大消耗等级上限（超过将被截断）
  max-level: 30
  # 线性模式：每级消耗 = 等级 * linear-multiplier
  linear-multiplier: 1
  # 指数模式：每级消耗 = exponential-base ^ 等级
  exponential-base: 2
  # 常数模式：固定消耗等级
  constant-level: 5

messages:
  pickup-tip: '&e你获得了附魔书，拖动到物品上并右键即可快速附魔。'
  not-enough-exp: '&c经验不足，需要 &e%cost% &c级。'
  apply-success: '&a附魔成功，消耗 &e%cost% &a级经验。'
  no-available: '&e该附魔书没有可转移的附魔。'
```
