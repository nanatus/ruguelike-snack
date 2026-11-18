package com.snake;

/**
 * 群友商店的商品。价格、名称和效果说明只在这里维护，
 * 界面和购买逻辑共用同一份数据，避免两处写死后对不上。
 */
public enum ShopItem {

    SPEED_UP("加速药剂", "每步用时 -10 毫秒", 5),
    SPEED_DOWN("减速药剂", "每步用时 +10 毫秒", 5),
    LENGTH_UP("扩容药剂", "长度上限 +5 节", 9);

    public final String name;
    public final String description;
    /** 价格，用蛇身长度支付。 */
    public final int price;

    ShopItem(String name, String description, int price) {
        this.name = name;
        this.description = description;
        this.price = price;
    }
}
