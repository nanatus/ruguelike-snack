package com.snake;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * 程序入口。
 *
 * 整个游戏只有一个窗口 {@link MainFrame}，所有页面都在里面用卡片切换，
 * 所以这里只负责：统一外观 → 在事件分发线程上开窗 → 异常兜底。
 */
public final class App {

    private App() {
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("使用默认外观: " + e.getMessage());
        }
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                // 存档损坏之类的异常绝不能把整个应用带崩，
                // 弹个窗让玩家知道发生了什么比直接闪退好。
                try {
                    new MainFrame().setVisible(true);
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    javax.swing.JOptionPane.showMessageDialog(null,
                            "游戏启动失败：\n" + e,
                            "出错了", javax.swing.JOptionPane.ERROR_MESSAGE);
                    System.exit(1);
                }
            }
        });
    }
}
