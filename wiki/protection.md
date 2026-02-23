# 🛡️ protection - 保护模块
> 模块ID: `protection`

### 玩家命令
- 无玩家功能命令（以事件触发为主）

### 管理员sudo节点命令
- `/si-protection sudo reload`
重载模块配置文件。
- `/si-protection sudo set <spawnprotection|joinprotection> <seconds>`
设置 重生保护/加入游戏保护 的 保护时长。

### 模块原始配置文件
```yml
# ================================
# SiMCUniverse - Protection 模块主配置
# ================================
# 说明：
# 玩家加入游戏时获得 join-protection-seconds 秒伤害免疫。
# 玩家重生时获得 spawn-protection-seconds 秒伤害免疫。
# 设置为 0 表示关闭对应保护。

# 玩家加入保护时长（秒）
join-protection-seconds: 45

# 玩家重生保护时长（秒）
spawn-protection-seconds: 120
```