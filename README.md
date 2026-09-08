# 鲜序

鲜序是一款离线使用的 Android 冰箱食材管理 App，适合记录家里还有什么、放在哪里，以及哪些食材该优先吃掉。

## 可以做什么

- 内置 322 种常见食材，支持中文名称、别名搜索和分类选择。
- 按冷藏层、果蔬抽屉、冷冻抽屉和常温区展示食材，用圆环和颜色提示临期状态。
- 同一种食材可以分批记录，也可以部分吃掉、开封、拆分、转冷冻或标记丢弃。
- 日期可按包装日期、储存参考或自己的计划设置；没有合适资料时也可以先不设日期。
- 支持系统通知、优先食用清单，以及本地 JSON 导入和导出。
- 不需要账号和网络，库存、设置与提醒记录都保存在本机。

日期提示用于帮助安排储存和食用，不代表 App 能判断食物实际是否安全或新鲜；有包装说明时请优先按包装要求处理。

## 资料来源

食材期限和储存条件主要参考食品安全机构、政府部门及高校推广资料：

- [FoodSafety.gov：冷藏及冷冻储存表](https://www.foodsafety.gov/food-safety-charts/cold-food-storage-charts)
- [USDA FSIS：冷藏与食品安全](https://www.fsis.usda.gov/food-safety/safe-food-handling-and-preparation/food-safety-basics/refrigeration)
- [USDA FSIS：冷冻与食品安全](https://www.fsis.usda.gov/food-safety/safe-food-handling-and-preparation/food-safety-basics/freezing-and-food-safety)
- [NDSU Extension：Food Storage Guide](https://www.ndsu.edu/agriculture/extension/publications/food-storage-guide-answers-question)
- [NDSU Extension：Food Freezing Guide](https://www.ndsu.edu/agriculture/extension/publications/food-freezing-guide)
- [National Center for Home Food Preservation：冷冻食品储存](https://nchfp.uga.edu/how/freeze/freeze-general-information/how-long-can-i-store-frozen-foods/)
- [上海市农业农村委：农产品家庭保存](https://nyncw.sh.gov.cn/tpxw/20240208/6b8742f61f7746bb9fe55fdafb234be7.html)
- [香港食物安全中心：雪柜及冰格贮存食物提示](https://www.cfs.gov.hk/sc_chi/multimedia/multimedia_pub/multimedia_pub_fsf_218_01.html)

完整的 21 条来源、适用条件和核查记录保存在 [食材目录](app/src/main/assets/food_catalog.v1.json) 与 [开发执行文档](docs/鲜序_Android开发执行文档.md) 中。

## 构建

使用 Android Studio 打开项目根目录，或在已经配置好 JDK 和 Android SDK 的 PowerShell 中运行：

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

生成的安装包位于 `app/build/outputs/apk/debug/app-debug.apk`。

项目使用 Kotlin、Jetpack Compose、Navigation 3、Room、DataStore 和 WorkManager。
