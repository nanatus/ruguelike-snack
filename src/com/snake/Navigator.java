package com.snake;

import java.awt.Window;

/**
 * 面板与窗口之间的唯一接口。面板只依赖这个接口，不认识 MainFrame，
 * 因此新增页面不会牵动已有的页面。
 */
public interface Navigator {

    String CARD_MENU = "menu";
    String CARD_SETUP = "setup";
    String CARD_SKINS = "skins";
    String CARD_DAILY = "daily";
    String CARD_SCORES = "scores";
    String CARD_ACHIEVEMENTS = "achv";
    String CARD_GAME = "game";

    /** 切到某个页面。 */
    void show(String card);

    /** 当前配置（可变，面板直接改它）。 */
    GameConfig config();

    ConfigStore store();

    Skin skin();

    /** 立即换肤，所有页面都会跟着更新。 */
    void applySkin(String skinId);

    /** 用给定配置开新局。 */
    void startNewGame(GameConfig config);

    /** 用存档恢复一局。 */
    void resumeGame(SaveState save);

    /** 打开设置对话框。 */
    void openSettings();

    void refreshMenu();

    /** 主窗口，用于把设置 / 皮肤预览对话框挂上去。 */
    Window window();
}
