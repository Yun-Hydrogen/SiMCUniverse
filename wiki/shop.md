# 🛒 shop - 商店模块
> 模块ID: `shop`

---

### 玩家命令
- `/si-shop`
打开商店GUI。

### 管理员sudo节点命令
- `/si-shop sudo reload`
重载模块配置文件。
- `/si-shop sudo resetshop`
**重置**商店商品配置。

### 原始配置文件
```yaml
# ================================
# SiMCUniverse - Shop 模块主配置
# ================================
# 说明：
# 商品页文件请放在 shop/shop_page/ 目录中，符合格式会自动加载。

# 是否注册 /shop 别名（通过监听重定向到 /si-shop）
register-shop-alias: false

# 快捷打开商店：手持指定物品右键即可打开 shop
# 默认启用，默认物品为小木棍（minecraft:stick）
quick-open:
  enabled: true
  item: minecraft:stick

# 经济货币来源：killscore 或 livescore
currency-source: killscore

#消息自定义配置
messages:
  module-disabled: "&cShop 模块已禁用。"
  no-pages: "&c商店页面未配置。"
  not-enough-currency: "&c你的%shop_currency%不足。需要 &e%cost% &c，当前 &e%balance%"
  exchange-success: "&a兑换成功！消耗 &e%cost% &a，当前%shop_currency%: &e%balance%"
  reload-success: "&aShop 配置已重载。"
  reset-confirm: "&e再次输入同样命令以确认重置商店页面（15秒内）。"
  reset-success: "&a商店页面已重置。"
  quick-open-tip: "&7快捷打开商店：手持 &e%shop_quick_open_item% &7右键"
```

### 商店页原始商品配置文件
**Page1.yml**
```yaml
# 商店第1页，最多27个商品。
#GUI标题
title: "商店"

#商品1
1:
  # 商品名称，支持颜色代码和HEX颜色代码
  name: "&a木材补给"
  # 商品描述，支持颜色代码和HEX颜色代码，支持多行显示
  lore:
    - "&7包含基础木材"
    - "&#66CCFF适合新手开荒"
  # 商品图标，使用名字空间格式（如 minecraft:oak_log）
  icon: "minecraft:oak_log"
  # 商品图标数量
  icon-amount: 16
  # 兑换消耗，整数
  cost: 10
  # 兑换类型，item表示物品交换，command表示执行命令
  exchange-type: item
  items:
    "minecraft:oak_log": 32
    "minecraft:torch": 16

2:
  name: "&b每日补给"
  lore:
    - "&7执行一组奖励命令"
  icon: "minecraft:paper"
  icon-amount: 1
  cost: 20
  exchange-type: command
  commands:
    - "tellraw %player% {\"text\":\"兑换成功!\",\"color\":\"green\"}"
    - "give %player% minecraft:bread 8"
```
- 商品页文件请放在 shop/shop_page/ 目录中，符合格式会自动加载。Page1.yml会被加载到第一页，Page2.yml会被加载到第二页，Page3.yml会被加载到第三页，以此类推。

### 示例商品页配置文件
**Page1.yml**
```yaml
# 商店第1页，最多27个商品。
title: "材料购买"

#商品1
1:
  # 商品名称，支持颜色代码和HEX颜色代码
  name: " #AAAAAA 火药 x16"
  # 商品描述，支持颜色代码和HEX颜色代码，支持多行显示
  lore:
    - "#66CCFF 用 100 青辉石 兑换 16个 火药"
    - "#66CCFF 可以用于合成TNT、烟花火箭和弹药"
  # 商品图标，使用名字空间格式（如 minecraft:oak_log）
  icon: "minecraft:gunpowder"
  # 商品图标数量
  icon-amount: 16
  # 兑换消耗，整数
  cost: 100
  exchange-type: item
  items:
    "minecraft:gunpowder": 16


2:
  name: "#E37249 铜锭 x64"
  lore:
    - "#66CCFF 用 520 青辉石 兑换 64个 铜锭"
    - "可以用于制作热武器和弹药"
  icon: "minecraft:copper_ingot"
  icon-amount: 64
  cost: 520
  exchange-type: item
  items:
    "minecraft:copper_ingot": 64

3:
  name: "#D1D1D1 铁锭 x16"
  lore:
    - "#66CCFF 用 400 青辉石 兑换 16个 铁锭"
    - "#66CCFF 可以用于制作热兵器、弹药等"
  icon: "minecraft:iron_ingot"
  icon-amount: 16
  cost: 400
  exchange-type: item
  items:
    "minecraft:iron_ingot": 16

4:
  name: "#FFCA38 金锭 x16"
  lore:
    - "#66CCFF 用 800 青辉石 兑换 16个 金锭"
    - "#66CCFF 可以用于制作热兵器、弹药等"
  icon: "minecraft:gold_ingot"
  icon-amount: 16
  cost: 800
  exchange-type: item
  items:
    "minecraft:gold_ingot": 16

5:
  name: "#66CCFF 钻石 x5"
  lore:
    - "#66CCFF 用 800 青辉石 兑换 5个 钻石"
    - "#66CCFF 可以用于制作护甲、工具和配件等"
  icon: "minecraft:diamond"
  icon-amount: 5
  cost: 800
  exchange-type: item
  items:
    "minecraft:diamond": 5

6:
  name: "#4D3935 下界合金锭 x3"
  lore:
    - "#66CCFF 用 2400 青辉石 兑换 3个 下界合金锭"
    - "#66CCFF 可以用于制作工具、护甲等"
  icon: "minecraft:netherite_ingot"
  icon-amount: 3
  cost: 2400
  exchange-type: item
  items:
    "minecraft:netherite_ingot": 3

7:
  name: "#FF8C00 烈焰棒 x5"
  lore:
    - "#66CCFF 用 300 青辉石 兑换 5个 烈焰棒"
    - "#66CCFF 可以用于制作热兵器和配件等"
  icon: "minecraft:blaze_rod"
  icon-amount: 5
  cost: 300
  exchange-type: item
  items:
    "minecraft:blaze_rod": 5

8:
  name: "#8B4513 原木 x16"
  lore:
    - "#66CCFF 用 300 青辉石 兑换 16个 原木"
    - "#66CCFF 可以用于建筑和制作各种物品"
  icon: "minecraft:oak_log"
  icon-amount: 16
  cost: 300
  exchange-type: item
  items:
    "minecraft:oak_log": 16

9:
  name: "#F0E68C 石英 x9"
  lore:
    - "#66CCFF 用 300 青辉石 兑换 9个 石英"
    - "#66CCFF 可以用于制造热兵器和建筑等"
  icon: "minecraft:quartz"
  icon-amount: 9
  cost: 300
  exchange-type: item
  items:
    "minecraft:quartz": 9

10:
  name: "#4169E1 青金石 x16"
  lore:
    - "#66CCFF 用 800 青辉石 兑换 16个 青金石"
    - "#66CCFF 可以用于染色、附魔和制造热兵器等"
  icon: "minecraft:lapis_lazuli"
  icon-amount: 16
  cost: 800
  exchange-type: item
  items:
    "minecraft:lapis_lazuli": 16

11:
  name: "#DC143C 红石 x10"
  lore:
    - "#66CCFF 用 800 青辉石 兑换 10个 红石"
    - "#66CCFF 可以用于红石电路和制造热兵器配件"
  icon: "minecraft:redstone"
  icon-amount: 10
  cost: 800
  exchange-type: item
  items:
    "minecraft:redstone": 10

12:
  name: "#BA55D3 紫水晶碎片 x10"
  lore:
    - "#66CCFF 用 1000 青辉石 兑换 10个 紫水晶碎片"
    - "#66CCFF 可以用于制造热兵器配件"
  icon: "minecraft:amethyst_shard"
  icon-amount: 10
  cost: 1000
  exchange-type: item
  items:
    "minecraft:amethyst_shard": 10

13:
  name: "#8B4513 皮革 x10"
  lore:
    - "#66CCFF 用 500 青辉石 兑换 10个 皮革"
    - "#66CCFF 可以用于制作护甲、书籍、背包和热兵器配件"
  icon: "minecraft:leather"
  icon-amount: 10
  cost: 500
  exchange-type: item
  items:
    "minecraft:leather": 10

14:
  name: "#3CB371 不死图腾 x1"
  lore:
    - "#66CCFF 用 1500 青辉石 兑换 1个 不死图腾"
    - "#66CCFF 可以在遭受致命伤害时保护你一次"
  icon: "minecraft:totem_of_undying"
  icon-amount: 1
  cost: 1500
  exchange-type: item
  items:
    "minecraft:totem_of_undying": 1

15:
  name: "#C0C0C0 下界之星 x1"
  lore:
    - "#66CCFF 用 3000 青辉石 兑换 1个 下界之星"
    - "#66CCFF 可以用于制作信标和武器配件"
  icon: "minecraft:nether_star"
  icon-amount: 1
  cost: 3000
  exchange-type: item
  items:
    "minecraft:nether_star": 1

16:
  name: "#FFFF00 附魔金苹果 x2"
  lore:
    - "#66CCFF 用 1200 青辉石 兑换 2个 附魔金苹果"
    - "#66CCFF 提供强力的增益效果"
  icon: "minecraft:enchanted_golden_apple"
  icon-amount: 2
  cost: 1200
  exchange-type: item
  items:
    "minecraft:enchanted_golden_apple": 2

17:
  name: "#FFD700 金苹果 x3"
  lore:
    - "#66CCFF 用 900 青辉石 兑换 3个 金苹果"
    - "#66CCFF 可以恢复生命值并提供增益效果"
  icon: "minecraft:golden_apple"
  icon-amount: 3
  cost: 900
  exchange-type: item
  items:
    "minecraft:golden_apple": 3

18:
  name: "#87CEEB 鞘翅 x1"
  lore:
    - "#66CCFF 用 5000 青辉石 兑换 1个 鞘翅"
    - "#66CCFF 可以让你在空中滑翔"
  icon: "minecraft:elytra"
  icon-amount: 1
  cost: 5000
  exchange-type: item
  items:
    "minecraft:elytra": 1

19:
  name: "##da96ff 哭泣的黑曜石 x5"
  lore:
    - "#66CCFF 用 1200 青辉石 兑换 5个 哭泣的黑曜石"
    - "#66CCFF 可以制作重生锚点、武器配件等"
  icon: "minecraft:crying_obsidian"
  icon-amount: 5
  cost: 1200
  exchange-type: item
  items:
    "minecraft:crying_obsidian": 5
```
**Page2.yml**
```yaml
title: "技能支援"

1:
  name: "#FFC98A谈判到此为止！"
  lore:
    - "&7在玩家正上方丢下一颗带爆炸的区域烟雾弹"
  icon: "minecraft:tnt"
  icon-amount: 1
  cost: 100
  exchange-type: command
  commands:
    - "execute at %player% run summon tnt ~ ~10 ~ {Fuse:80}"
    - "execute at %player% run summon minecraft:area_effect_cloud ~ ~1 ~ {Potion:long_awkward,Age:0,Duration:200,DurationOnUse:200,Particle:largeexplode,Color:16777215,Radius:3,RadiusOnUse:3,RadiusPerTick:0}"
    - "tellraw %player% {\"text\":\"谈判到此为止！请注意避开爆炸范围！\",\"color\":\"red\",\"bold\":true}"

2:
  name: "#FFA98A爆裂咏叹调"
  lore:
    - "&7在玩家正上方召唤一堆即将爆炸的TNT"
    - "#FF0000召唤后记得跑远点哦~"
  icon: "minecraft:tnt"
  icon-amount: 16
  cost: 180
  exchange-type: command
  commands: 
    - "execute at %player% run summon tnt ~ ~10 ~ {Fuse:120}"
    - "execute at %player% run summon tnt ~ ~12 ~ {Fuse:120}"
    - "execute at %player% run summon tnt ~ ~14 ~ {Fuse:120}"
    - "execute at %player% run summon tnt ~ ~16 ~ {Fuse:120}"
    - "execute at %player% run summon tnt ~ ~18 ~ {Fuse:120,ExplosionPower:3}"
    - "tellraw %player% {\"text\":\"炸弹已投放，请注意避开爆炸范围！\",\"color\":\"red\",\"bold\":true}"

3:
  name: "#FFB98A让每个怪物都飞起来！"
  lore:
    - "&7将周围7m内除了玩家以外的所有实体向上击飞"
  icon: "minecraft:feather"
  icon-amount: 1
  cost: 200
  exchange-type: command
  commands:
    - "execute at %player% run playsound minecraft:entity.firework_rocket.launch master %player% ~ ~ ~ 10 1"
    - "execute at %player% run effect give @e[distance=..7,type=!player] minecraft:levitation 1 30 true"
    - "tellraw %player% {\"text\":\"让他们飞起来！\",\"color\":\"green\",\"bold\":true}"
```
