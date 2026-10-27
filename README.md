# 贪吃蛇

纯 Java Swing 写的单机贪吃蛇，零第三方依赖，Java 8 可直接运行。

## 功能

- **两种模式**
  - 经典：有限棋盘，撞墙结束，也可以勾选「撞到边界从对面穿出」改成穿墙。
  - 无尽：无限地图，相机跟着蛇头走，只有撞到自己才结束。食物按可见区域自动生成，
    走出视野的旧食物会被清掉，再在视野内补到约每 40 格 1 个的密度；达到长度上限后
    蛇身不再变长，但仍会继续生成食物。
- **主菜单**：中间一列主选项（继续游戏 / 新的游戏 / 皮肤预览 / 设置 / 退出），
  左下角是每日挑战、成就、排行榜，右下角是累计统计。布局坐标全部在
  `MainMenuPanel.doLayout()` 里按 `preferredSize` 算，不套嵌套容器，
  换字体或改窗口大小都不会把按钮挤扁。主菜单没有动态背景。
- **ESC 暂停菜单**：继续、重新开始、保存并返回主菜单、不保存退出、设置、皮肤预览、退出程序。
  棋盘上方压一层半透明暗色遮罩，中间是实心圆角卡片，按钮一列排开。
  菜单用 `setComponentZOrder` 固定在棋盘之上，暂停期间方向键、WASD、Enter、空格全部归菜单管，
  空格不会让底下的棋盘偷偷继续跑。切换皮肤时整张卡片跟着换色。
- **猪头剧情与商店**：蛇第一次吃满长度上限时，前方视野内会出现一个猪头。碰到后进入 7 句逐句对话，
  每点击一次推进一句；听完后仅当前存档解锁「群友商店」。按 B 打开暂停式商店浮层，再按 B 或 Esc 关闭。
  商店货币是蛇身长度，当前版本货架为空。
- **存档续局**：随时保存并退出，下次从主菜单「继续游戏」原样恢复——棋盘、蛇身、方向、待输入方向、当前视野内的食物、分数、计时、猪头与商店解锁状态都会还原。
- **每日挑战**：以当天日期为种子生成固定局面，全天所有人一样；记录连续挑战天数。
- **随机一局**：在当前设置附近随机微调，速度、地图、食物数量都有变化。
- **排行榜**：本地 Top 10，记录分数、长度、模式、速度、食物数（无尽显示自动）、皮肤、用时和是否中途放弃。
- **成就**：12 个成就，分数、长度、局数、皮肤、连续挑战、续局等维度。
- **6 套皮肤**：经典、霓虹、海洋、樱花、鎏金、极简，实时预览。
- **背景**：可以导入自己的图片，菜单区和游戏区各自独立开关，还能调缩放方式、暗化程度和网格。菜单区只显示图片，不会跑演示蛇。
- **音效**：默认用 JDK 现场合成，不需要任何音频文件；也可以改用自己放进 classpath 的音频文件。

## 构建与运行

```
build.bat                       编译到 bin/，并校验字节码是 Java 8（major 52）
java -cp bin com.snake.App     运行
```

手动编译等价命令：

```
javac --release 8 -encoding UTF-8 -d bin @sources.txt
```

> **为什么必须 `--release 8`**
> 这台机器上的 `java` 是 JRE 1.8，只能加载 major version 52 的 class；
> 而 `javac` 是 17。所以要么 `javac --release 8`，要么用 JDK 8 编译。
> VS Code 的 Java 扩展没有兼容性级别设置，保存文件时会用 JDK 17 重新生成
> `bin`（major 61），跑起来就是 `UnsupportedClassVersionError`。
> 因此 `.vscode/settings.json` 把扩展的输出目录改成了 `bin-ide`，
> `bin` 只由 `build.bat` 产出。

## 操作

| 按键 | 作用 |
| --- | --- |
| 方向键 / WASD | 转向 |
| 空格 | 暂停 / 继续；游戏结束后重开 |
| ESC | 打开或关闭暂停菜单；其他页面按 ESC 返回主菜单 |
| B | 猪头对话结束后打开商店；再按 B 或 Esc 关闭 |
| 回车 | 确认当前选中的按钮 |

窗口切到别的程序时游戏会自动暂停。

## 数据存放

全部走 `java.util.prefs.Preferences`，节点是 `/com/snake/game`：

- 对局设置、当前皮肤、音效开关
- 背景设置（图片另存到 `${user.home}/.snake/background.png`）
- 局数、总分、最高分、最长长度、连续挑战天数、续局次数
- 排行榜、成就解锁时间
- 续局存档

设置里的「恢复默认」会清空以上全部内容。

## 自定义音效

把音频文件放进 classpath 的 `/sounds/` 目录，文件名用枚举名：

```
res/sounds/eat.wav
res/sounds/game_over.wav
...
```

支持的枚举：`EAT`、`DEATH`、`GAME_OVER`、`MENU`、`START`、`ACHIEVEMENT`、`WARN`。
每个名字都接受 `.wav`、`.au`、`.aiff`、`.ogg`，按这个顺序尝试。
前三种 JDK 自带解码器能读；OGG 和 MP3 一样需要外部 SPI。

JDK 读不了 MP3。要用 MP3 就把解码器（如 JLayer）放进 `lib/` 并加进 classpath，
`AudioSystem` 会自动发现。

某个音效缺文件或解码不了时，只有那一个音效回落到合成音，其余照常用外部文件。

## 源码结构

```
src/com/snake/
  App.java              入口，设置 LookAndFeel 后在 EDT 上开 MainFrame
  MainFrame.java        唯一窗口，CardLayout 切页，管焦点 / ESC / 失焦 / 关闭
  Navigator.java        页面与窗口的导航接口
  GameScreen.java       状态栏、暂停浮层、结算、计分、成就、存档
  GamePanel.java        双模式逻辑、无限相机、渲染、演示 AI
  GameConfig.java       对局配置、速度档位、挑战类型
  Point.java            不可变格子坐标，long 类型
  Direction.java        四方向枚举，带 opposite()
  ChallengeFactory.java 每日挑战 / 随机挑战
  ConfigStore.java      Preferences 持久化
  SaveState.java        续局存档
  ScoreRecord.java      榜单记录
  HighScores.java       Top 10
  Achievement(s).java   成就定义与解锁
  BackgroundSettings.java / BackgroundImage.java  背景设置与缩放
  Sound.java / SoundPlayer.java / Sfx.java
  SynthSoundPlayer.java / ResourceSoundPlayer.java
  Skins.java / Skin.java 皮肤调色板
  PigArt.java           猪头 Java2D 绘制（棋盘与剧情共用）
  ui/                   各个页面和设置控件（含猪头对话、群友商店浮层）
```
