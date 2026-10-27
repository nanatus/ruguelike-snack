package com.snake;

/**
 * 自定义背景图的设置项。图片本身由 {@link BackgroundImage} 管理，
 * 这里只管「要不要用、怎么用」。
 *
 * 菜单区与游戏区各自独立开关——有人只想给主菜单换背景，不一定想让游戏画面变花。
 */
public final class BackgroundSettings {

    public enum ScaleMode {
        COVER("等比填充裁切"),
        STRETCH("拉伸铺满");

        private final String label;

        ScaleMode(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    public boolean enabledInGame = false;
    public boolean enabledInMenu = false;
    /** 受管文件路径，见 {@link BackgroundImage#managedPath()}。 */
    public String path;
    public ScaleMode scale = ScaleMode.COVER;
    /** 暗化蒙版百分比，太亮的图会盖住蛇。 */
    public int dimPercent = 45;
    public boolean showGrid = true;

    public boolean hasImage() {
        return path != null && !path.isEmpty() && BackgroundImage.exists();
    }

    public void sanitize() {
        if (scale == null) {
            scale = ScaleMode.COVER;
        }
        dimPercent = GameConfig.clamp(dimPercent, 0, 80);
    }

    /** 恢复默认：两个区都不用背景图。 */
    public void reset() {
        enabledInGame = false;
        enabledInMenu = false;
        path = null;
        scale = ScaleMode.COVER;
        dimPercent = 45;
        showGrid = true;
    }
}
