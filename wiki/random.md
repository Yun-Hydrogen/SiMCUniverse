# 🎰 random - 抽奖模块
> 模块ID: `random`
---

### 玩家命令
- `/si-random`
打开抽奖 GUI

### 管理员sudo节点命令
- `/si-random sudo reload`
重载模块配置文件
- `/si-random sudo reset`
重置 **所有玩家** 的保底点数
- `/si-random sudo set <player> <point>`
设置 一个玩家（加入过游戏，无论是否在线） 的保底点数，**只能为自然数**。

### 原始配置文件
```yaml
# ================================
# SiMCUniverse - Random 模块主配置
# ================================

# 货币来源：killscore 或 livescore
currency-source: killscore

# 抽奖消耗
draw-cost:
  # 单抽消耗
  single: 10
  # 十连抽消耗
  ten: 90

# 十连抽规则：最后一抽仅在 gold/rainbow 两个池中抽取 （九蓝一金）
draw:
  ten-guarantee-gold: true

# 保底点数
pity:
  threshold: 100

# 抽奖GUI音效
custom-music:
  enabled: false
  # 默认为minecraft末影龙死亡音效，建议使用自定义命名空间，避免原版声音键被客户端其他资源包影响
  sound-id: minecraft:entity.ender_dragon.death
  # 是否要求只有加载了服务器资源包的玩家才能播放1音效。
  # 如果服务器自定义了音效资源包，为避免玩家未加载资源包时出现原版音效混播，建议开启
  require-pack-ready: true
  volume: 1.0
  pitch: 1.0

messages:
  module-disabled: "&cRandom 模块已禁用。"
  not-enough-currency: "&c你的%shop_currency%不足。需要 &e%cost%&c，当前 &e%balance%"
  sign-prompt: "&e请在弹出的告示牌中签署确认抽奖（可直接确认）。"
  sign-fallback: "&e告示牌输入不可用，直接在聊天输入任意内容即可确认抽奖。"
  reload-success: "&aRandom 配置已重载。"
  reset-success: "&a已重置所有玩家保底点数。"
  set-success: "&a已设置 %player% 的保底点数为 &e%point%"
  pity-not-enough: "&c保底点数不足。"
  pity-redeem-success: "&d保底兑换成功，点数已清零。"
```

### 示例奖品池配置
奖品池的配置在 `插件配置文件夹/random/random_pool/下`
**blue.yml**
```yaml
# Blue 等级 配置文件

#Blue等级物品概率，请确保所有等级的概率之和为1
chance: 0.8

#Blue等级物品列表，格式为 "物品ID": 数量
items:
  #矿物类
  "minecraft:iron_ingot": 8
  "minecraft:coal": 8
  "minecraft:gold_ingot": 2
  "minecraft:copper_ingot": 8
  "minecraft:raw_iron": 16
  "minecraft:raw_copper": 16
  "minecraft:raw_gold": 4
  "minecraft:lapis_lazuli": 4
  "minecraft:amethyst_shard": 1
  "minecraft:quartz": 4
  "minecraft:iron_nugget": 19
  "minecraft:gold_ingot": 10
  "minecraft:gunpowder": 4
  "minecraft:redstone": 4
  #食物类
  "minecraft:bread": 16
  "minecraft:cooked_beef": 4
  "minecraft:cooked_chicken": 4
  "minecraft:cooked_porkchop": 4
  "minecraft:cooked_mutton": 4
  "minecraft:pumpkin_pie": 4
  "minecraft:cookie": 16
  "minecraft:apple": 8
  "minecraft:golden_apple": 1
  "minecraft:milk_bucket": 1
  "minecraft:melon_slice": 16
  "minecraft:carrot": 8
  "minecraft:potato": 4
  "minecraft:golden_carrot": 2
  "minecraft:beetroot": 4
  "minecraft:cake": 2
  "minecraft:wheat_seeds": 16
  #工具类
  "minecraft:iron_pickaxe": 1
  "minecraft:iron_shovel": 1
  "minecraft:iron_axe": 1
  "minecraft:iron_sword": 1
  "minecraft:iron_hoe": 1
  "minecraft:stone_pickaxe": 1
  "minecraft:stone_shovel": 1
  "minecraft:stone_axe": 1
  "minecraft:stone_sword": 1
  "minecraft:stone_hoe": 1
  "minecraft:fishing_rod": 1
  "minecraft:flint_and_steel": 1
  "minecraft:bucket": 1
  "minecraft:compass": 1
  "minecraft:clock": 1
  "minecraft:map": 1
  "minecraft:name_tag": 1
  "minecraft:shears": 1
  "minecraft:torch": 16
  "minecraft:iron_helmet": 1
  "minecraft:iron_chestplate": 1
  "minecraft:iron_leggings": 1
  "minecraft:iron_boots": 1
  "minecraft:bow": 1
  "minecraft:arrow": 16
  #木制品
  "minecraft:oak_log": 4
  "minecraft:spruce_log": 4
  "minecraft:birch_log": 4
  "minecraft:jungle_log": 4
  "minecraft:acacia_log": 4
  "minecraft:dark_oak_log": 4
  "minecraft:stripped_oak_log": 4
  "minecraft:stick": 20
  #方块类
  "minecraft:cobblestone": 40
```
**gold.yml**
```yaml
# Gold 等级 配置文件

# Gold等级物品概率，请确保所有等级的概率之和为1
chance: 0.185

# Gold等级物品列表，格式为 "物品ID": 数量
items:
  #矿物类
  "minecraft:iron_ingot": 28
  "minecraft:coal": 32
  "minecraft:gold_ingot": 16
  "minecraft:copper_ingot": 20
  "minecraft:raw_iron": 39
  "minecraft:raw_copper": 50
  "minecraft:raw_gold": 30
  "minecraft:lapis_lazuli": 16
  "minecraft:amethyst_shard": 8
  "minecraft:quartz": 10
  "minecraft:gunpowder": 16
  "minecraft:redstone": 16
  "minecraft:netherite_scrap": 4
  #食物类
  "minecraft:cooked_beef": 12
  "minecraft:cooked_porkchop": 12
  "minecraft:cooked_mutton": 12
  "minecraft:golden_apple": 5
  "minecraft:golden_carrot": 7
  "minecraft:cake": 5
  "minecraft:enchanted_golden_apple": 1
  #工具类
  "minecraft:diamond_pickaxe": 1
  "minecraft:diamond_shovel": 1
  "minecraft:diamond_axe": 1
  "minecraft:diamond_sword": 1
  "minecraft:diamond_hoe": 1
  "minecraft:diamond_helmet": 1
  "minecraft:diamond_chestplate": 1
  "minecraft:diamond_leggings": 1
  "minecraft:diamond_boots": 1
  "minecraft:turtle_helmet": 1
  "minecraft:spectral_arrow": 32
  #木制品
  "minecraft:oak_log": 40
  "minecraft:spruce_log": 40
  "minecraft:birch_log": 40
  "minecraft:jungle_log": 40
  "minecraft:dark_oak_log": 40
  "minecraft:acacia_log": 40
  #其它
  "minecraft:experience_bottle": 8
```
**rainbow.yml**
```yaml
# Gold 等级 配置文件

# Gold等级物品概率，请确保所有等级的概率之和为1
chance: 0.03

# Gold等级物品列表，格式为 "物品ID": 数量
items:
  #矿物类
  "minecraft:iron_ingot": 64
  "minecraft:gold_ingot": 64
  "minecraft:copper_ingot": 64
  "minecraft:lapis_lazuli": 32
  "minecraft:amethyst_shard": 32
  "minecraft:quartz": 32
  "minecraft:gunpowder": 64
  "minecraft:redstone": 32
  "minecraft:netherite_ingot": 2
  #食物类
  "minecraft:cooked_beef": 48
  "minecraft:cooked_porkchop": 48
  "minecraft:cooked_mutton": 48
  "minecraft:golden_apple": 20
  "minecraft:golden_carrot": 32
  "minecraft:enchanted_golden_apple": 8
  #工具类
  "minecraft:netherite_pickaxe": 1
  "minecraft:netherite_shovel": 1
  "minecraft:netherite_axe": 1
  "minecraft:netherite_sword": 1
  "minecraft:netherite_hoe": 1
  "minecraft:netherite_helmet": 1
  "minecraft:netherite_chestplate": 1
  "minecraft:netherite_leggings": 1
  "minecraft:netherite_boots": 1
  "minecraft:spectral_arrow": 64
  "minecraft:elytra": 1
  #其它
  "minecraft:experience_bottle": 32
  "minecraft:totem_of_undying": 3
  "minecraft:heart_of_the_sea": 1
  "minecraft:netherite_upgrade_smithing_template": 16
  #附魔书
  "minecraft:enchanted_book{StoredEnchantments:[{id:\"minecraft:sharpness\",lvl:5}]}": 1
  "minecraft:enchanted_book{StoredEnchantments:[{id:\"minecraft:efficiency\",lvl:5}]}": 1
  "minecraft:enchanted_book{StoredEnchantments:[{id:\"minecraft:protection\",lvl:5}]}": 1
  "minecraft:enchanted_book{StoredEnchantments:[{id:\"minecraft:mending\",lvl:2}]}": 1
  "minecraft:enchanted_book{StoredEnchantments:[{id:\"minecraft:unbreaking\",lvl:3}]}": 1
```
