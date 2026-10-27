package com.snake;

import com.snake.ui.AchievementsPanel;
import com.snake.ui.DailyPanel;
import com.snake.ui.HighScorePanel;
import com.snake.ui.MainMenuPanel;
import com.snake.ui.SettingsDialog;
import com.snake.ui.SetupPanel;
import com.snake.ui.SkinPreviewPanel;
import com.snake.ui.UiKit;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

/**
 * 唯一的窗口。所有页面都是同一窗口里的卡片，切换时不会闪，也不用管焦点跨窗口的问题。
 *
 * 这个类只做三件事：持有所有页面、按 {@link Navigator} 的要求切卡片、把窗口事件
 * （关闭、失焦）转交给当前页面。页面的具体逻辑一律放在页面自己那边。
 */
public final class MainFrame extends JFrame implements Navigator {

    private static final long serialVersionUID = 1L;

    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);

    private final MainMenuPanel menuPanel;
    private final SetupPanel setupPanel;
    private final SkinPreviewPanel skinPanel;
    private final DailyPanel dailyPanel;
    private final HighScorePanel scorePanel;
    private final AchievementsPanel achvPanel;
    private final GameScreen gameScreen;

    private String current = CARD_MENU;

    public MainFrame() {
        super("贪吃蛇");
        Skin skin = ConfigStore.get().skin();

        gameScreen = new GameScreen(this, skin);
        menuPanel = new MainMenuPanel(this);
        setupPanel = new SetupPanel(this);
        skinPanel = new SkinPreviewPanel(this, skin);
        dailyPanel = new DailyPanel(this);
        scorePanel = new HighScorePanel(this);
        achvPanel = new AchievementsPanel(this);

        root.add(menuPanel, CARD_MENU);
        root.add(setupPanel, CARD_SETUP);
        root.add(skinPanel, CARD_SKINS);
        root.add(dailyPanel, CARD_DAILY);
        root.add(scorePanel, CARD_SCORES);
        root.add(achvPanel, CARD_ACHIEVEMENTS);
        root.add(gameScreen, CARD_GAME);

        setContentPane(root);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // 局中直接点 X：先结算存档，再退出，否则这一局就白打了。
                gameScreen.onWindowClosing();
                Sound.closePlayer();
                System.exit(0);
            }

            @Override
            public void windowDeactivated(WindowEvent e) {
                // 切到别的程序时不可能还在指望玩家操作，自动暂停。
                if (current.equals(CARD_GAME)) {
                    gameScreen.onWindowDeactivated();
                }
            }
        });

        // Esc 全局兜底：页面自己没处理时按层级退回上一层。
        root.registerKeyboardAction(e -> onEscape(), KeyStroke.getKeyStroke("ESCAPE"),
                JPanel.WHEN_IN_FOCUSED_WINDOW);

        Dimension d = root.getPreferredSize();
        setSize(Math.max(940, d.width), Math.max(620, d.height));
        setLocationRelativeTo(null);
        show(CARD_MENU);
    }

    private void onEscape() {
        if (current.equals(CARD_GAME)) {
            // 游戏页自己处理 Esc（开 / 关暂停菜单），这里不抢。
            return;
        }
        if (current.equals(CARD_MENU)) {
            confirmExit();
        } else {
            show(CARD_MENU);
        }
    }

    private void confirmExit() {
        if (UiKit.confirm(this, "退出", "确定要退出贪吃蛇吗？")) {
            Sound.closePlayer();
            System.exit(0);
        }
    }

    @Override
    public void show(String card) {
        current = card;
        cards.show(root, card);
        if (card.equals(CARD_MENU)) {
            menuPanel.refresh();
        } else {
            refreshPage(card);
        }
        Component focus = currentFocusable(card);
        if (focus != null) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    focus.requestFocusInWindow();
                }
            });
        }
    }

    /**
     * 次级页面的数据都来自别处（榜单由对局写入、每日挑战按天变、成就随时解锁），
     * 所以每次切进来都要重算一次，不能只在构造时算一次。
     */
    private void refreshPage(String card) {
        if (card.equals(CARD_DAILY)) {
            dailyPanel.refresh();
        } else if (card.equals(CARD_SCORES)) {
            scorePanel.refresh();
        } else if (card.equals(CARD_ACHIEVEMENTS)) {
            achvPanel.refresh();
        }
    }

    private Component currentFocusable(String card) {
        if (card.equals(CARD_MENU)) {
            return menuPanel.firstFocusable();
        }
        if (card.equals(CARD_SKINS)) {
            return skinPanel;
        }
        if (card.equals(CARD_SETUP)) {
            return setupPanel;
        }
        return null;
    }

    @Override
    public GameConfig config() {
        return ConfigStore.get().config();
    }

    @Override
    public ConfigStore store() {
        return ConfigStore.get();
    }

    @Override
    public Skin skin() {
        return ConfigStore.get().skin();
    }

    @Override
    public void applySkin(String skinId) {
        ConfigStore.get().setSkin(skinId);
        Skin s = ConfigStore.get().skin();
        menuPanel.applySkin(s);
        setupPanel.applySkin(s);
        skinPanel.applySkin(s);
        dailyPanel.applySkin(s);
        scorePanel.applySkin(s);
        achvPanel.applySkin(s);
        gameScreen.applySkin(s);
    }

    @Override
    public void startNewGame(GameConfig cfg) {
        ConfigStore.get().saveConfig();
        gameScreen.start(cfg, ConfigStore.get().skin());
        show(CARD_GAME);
    }

    @Override
    public void resumeGame(SaveState save) {
        gameScreen.resume(save);
        show(CARD_GAME);
    }

    @Override
    public void openSettings() {
        SettingsDialog.show(this, ConfigStore.get().skin());
        // 设置可以从主菜单或暂停菜单打开，只有当前确实停在主菜单上才需要刷新。
        if (current.equals(CARD_MENU)) {
            menuPanel.refresh();
        }
    }

    @Override
    public void refreshMenu() {
        // 皮肤、存档、统计都可能刚被别处改过，回来时统一重算一次。
        menuPanel.refresh();
    }

    @Override
    public Window window() {
        return this;
    }
}
