# 🧠 plugin - 插件总控
> 模块ID: `plugin-self`

### 功能说明
- 统一管理 SiMCUniverse 各子模块状态。
- 提供启用、禁用、重载、版本查询等运维能力。
- 支持重载插件主配置（`config.yml`）与全部模块配置。

### 玩家命令
- 无。

> `simcuniverse` 主命令默认需要管理员权限（OP）。

### 管理员sudo节点命令
- `/simcuniverse sudo help`
显示主命令帮助。
- `/simcuniverse sudo list`
显示当前所有模块状态（enabled/disabled）。
- `/simcuniverse sudo enable <module>`
启用指定模块。
- `/simcuniverse sudo disable <module>`
禁用指定模块。
- `/simcuniverse sudo reload <module>`
重载指定模块。
- `/simcuniverse sudo reload plugin-self`
重载插件主配置与全部模块配置。
- `/simcuniverse sudo version`
显示当前插件版本。


### 权限节点
- `simc.admin`
允许使用 `/simcuniverse` 主命令（默认 OP）。
> 各子模块的 `si-xxx sudo` 命令还会受对应模块权限控制。
- `simc.killscore.sudo`
允许运行 `killscore` 的sudo节点命令。
- `simc.livescore.sudo`
允许运行 `livescore` 的sudo节点命令。
- `simc.shop.sudo`
允许运行 `shop` 的sudo节点命令。
- `simc.random.sudo`
允许运行 `random` 的sudo节点命令。
- `simc.checkin.sudo`
允许运行 `checkin` 的sudo节点命令。
- `simc.task.sudo`
允许运行 `task` 的sudo节点命令。
- `simc.protection.sudo`
允许运行 `protection` 的sudo节点命令。
- `simc.quickenhance.sudo`
允许运行 `quickenhance` 的sudo节点命令。
---

### 原始配置文件（主配置）
```yaml
# SiMCUniverse 默认配置
plugin:
	language: zh_cn
	debug: false

# 玩家加入时聊天提示
join-tip:
	enabled: true
	messages:
		- "&6[SiMCUniverse] &e欢迎来到服务器！"
		- "&7可用指令：&f/si-checkin &7签到  &f/si-shop &7商店  &f/si-task &7任务  &f/si-random &7抽奖"
		- "&7 可使用快捷附魔：打开背包，拿起附魔书对着物品右键即可！（不限制等级）"
		- "&7更多功能敬请期待！"
```

### 常见运维流程
1. 查看模块状态
	 - `/simcuniverse sudo list`
2. 仅重载某一模块（推荐）
	 - `/simcuniverse sudo reload shop`
3. 修改主配置后整体重载
	 - `/simcuniverse sudo reload plugin-self`
4. 临时停用有问题模块
	 - `/simcuniverse sudo disable random`
	 - 问题排查后再启用：`/simcuniverse sudo enable random`

