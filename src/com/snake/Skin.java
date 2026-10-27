package com.snake;

import java.awt.Color;

/**
 * 一套皮肤 = 整个应用（游戏区 + 所有界面 + 按钮）的色板。
 * 皮肤切换立即全应用，不存在「只换了游戏区」的情况。
 */
public final class Skin {

    public final String id;
    public final String name;
    public final String description;

    /** 地图底色。 */
    public final Color bg;
    public final Color grid;
    public final Color gridAxis;
    /** 经典模式的棋盘边框。 */
    public final Color wall;

    public final Color head;
    public final Color headEdge;
    public final Color bodyFrom;
    public final Color bodyTo;

    public final Color food;
    public final Color overlay;
    public final Color text;
    public final Color textDim;

    public final Color barBg;
    public final Color barEdge;
    public final Color score;
    public final Color hint;

    public final Color panelBg;
    public final Color panelBorder;
    public final Color button;
    public final Color buttonHover;
    public final Color buttonText;
    public final Color buttonTextDim;
    public final Color accent;

    public Skin(String id, String name, String description, int bg, int grid, int gridAxis, int wall,
                int head, int headEdge, int bodyFrom, int bodyTo, int food, int overlay,
                int text, int textDim, int barBg, int barEdge, int score, int hint,
                int panelBg, int panelBorder, int button, int buttonHover, int buttonText,
                int buttonTextDim, int accent) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.bg = new Color(bg);
        this.grid = new Color(grid);
        this.gridAxis = new Color(gridAxis);
        this.wall = new Color(wall);
        this.head = new Color(head);
        this.headEdge = new Color(headEdge);
        this.bodyFrom = new Color(bodyFrom);
        this.bodyTo = new Color(bodyTo);
        this.food = new Color(food);
        // overlay 是唯一带 alpha 的一色（六套皮肤都写成 0x000000A5 / 0x000000B0）。
        // new Color(int) 只取低 24 位、alpha 一律补 255，遮罩就变成全不透明，
        // 暂停菜单和结算画面会把棋盘整个盖死。必须走 hasalpha 分支。
        this.overlay = new Color(overlay, true);
        this.text = new Color(text);
        this.textDim = new Color(textDim);
        this.barBg = new Color(barBg);
        this.barEdge = new Color(barEdge);
        this.score = new Color(score);
        this.hint = new Color(hint);
        this.panelBg = new Color(panelBg);
        this.panelBorder = new Color(panelBorder);
        this.button = new Color(button);
        this.buttonHover = new Color(buttonHover);
        this.buttonText = new Color(buttonText);
        this.buttonTextDim = new Color(buttonTextDim);
        this.accent = new Color(accent);
    }

    /** 皮肤预览页与外观设置页用的主色样本。 */
    public Color[] swatch() {
        return new Color[]{bg, grid, bodyFrom, head, food, accent, text};
    }
}
