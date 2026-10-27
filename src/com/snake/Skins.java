package com.snake;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 预设皮肤清单。classic 是原版的深绿配色，保留为默认。
 */
public final class Skins {

    private static final List<Skin> ALL = build();

    private Skins() {
    }

    public static List<Skin> all() {
        return ALL;
    }

    public static Skin byId(String id) {
        if (id != null) {
            for (Skin s : ALL) {
                if (s.id.equals(id)) {
                    return s;
                }
            }
        }
        return defaultSkin();
    }

    public static Skin defaultSkin() {
        return ALL.get(0);
    }

    private static List<Skin> build() {
        List<Skin> list = new ArrayList<Skin>();

        // 原版深绿
        list.add(new Skin("classic", "经典绿", "原版配色，深色背景配翠绿蛇身",
                0x0E1621, 0x1B2836, 0x2B3E52, 0x3E5C74,
                0x4ADE80, 0x0B3D24, 0x22C55E, 0x16A34A,
                0xEF4444, 0xA50000A5, 0xE6EDF3, 0x8B9AAB,
                0x0A111A, 0x1B2836, 0xE6EDF3, 0x8B9AAB,
                0x121D2B, 0x24344A, 0x1B2836, 0x2C3E54, 0xE6EDF3, 0x8B9AAB, 0x4ADE80));

        // 赛博霓虹
        list.add(new Skin("neon", "霓虹紫", "赛博朋克，紫粉渐变配青柠高光",
                0x0B0620, 0x1E1440, 0x33246B, 0x5B3FA8,
                0x39FF88, 0x0B3D24, 0xA855F7, 0x4C1D95,
                0x22D3EE, 0xA50000A5, 0xF5F3FF, 0xA78BFA,
                0x0A0518, 0x2A1B52, 0xF5F3FF, 0xA78BFA,
                0x160B33, 0x3A2470, 0x241545, 0x3A2470, 0xF5F3FF, 0xA78BFA, 0x22D3EE));

        // 深蓝海水
        list.add(new Skin("ocean", "深蓝海", "冷色深蓝，蛇身泛着海蓝渐变",
                0x071426, 0x10233C, 0x1C3A5E, 0x2E5A87,
                0x38BDF8, 0x0C4A6E, 0x0EA5E9, 0x075985,
                0xFBBF24, 0xA50000A5, 0xE0F2FE, 0x7C9CB8,
                0x050F1C, 0x17304D, 0xE0F2FE, 0x7C9CB8,
                0x0B1D33, 0x1E3E5F, 0x12283F, 0x1E3E5F, 0xE0F2FE, 0x7C9CB8, 0x38BDF8));

        // 樱粉
        list.add(new Skin("sakura", "樱粉", "柔粉色调，蛇身由粉渐变到淡紫",
                0x1A0F1B, 0x2E1B30, 0x4A2A4C, 0x7A4A7C,
                0xFBCFE8, 0x7A2A5E, 0xF472B6, 0x9D4EDD,
                0xFDE047, 0xA50000A5, 0xFDF2F8, 0xC48BB0,
                0x140A16, 0x3A1F3C, 0xFDF2F8, 0xC48BB0,
                0x241426, 0x4A2A4C, 0x331B36, 0x4A2A4C, 0xFDF2F8, 0xC48BB0, 0xF472B6));

        // 鎏金
        list.add(new Skin("gold", "鎏金", "琥珀与暗棕，古典质感",
                0x1A1206, 0x33260D, 0x54401A, 0x7A5C28,
                0xFCD34D, 0x4A3208, 0xF59E0B, 0x92400E,
                0xEF4444, 0xA50000A5, 0xFEF3C7, 0xB99B5E,
                0x130E04, 0x4A3813, 0xFEF3C7, 0xB99B5E,
                0x241A08, 0x5A4318, 0x33240B, 0x5A4318, 0xFEF3C7, 0xB99B5E, 0xFCD34D));

        // 极简黑白
        list.add(new Skin("mono", "极简黑白", "无彩色，看形状不看颜色",
                0x111111, 0x262626, 0x3D3D3D, 0x6B6B6B,
                0xFFFFFF, 0x000000, 0xD4D4D4, 0x525252,
                0xF5F5F5, 0xB00000B0, 0xF5F5F5, 0x9A9A9A,
                0x0A0A0A, 0x333333, 0xF5F5F5, 0x9A9A9A,
                0x1A1A1A, 0x3A3A3A, 0x242424, 0x3F3F3F, 0xF5F5F5, 0x9A9A9A, 0xF5F5F5));

        return Collections.unmodifiableList(list);
    }
}
