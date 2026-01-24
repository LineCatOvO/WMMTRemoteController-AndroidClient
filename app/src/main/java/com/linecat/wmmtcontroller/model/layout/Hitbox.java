package com.linecat.wmmtcontroller.model.layout;

/**
 * 碰撞箱类
 */
public class Hitbox {
    private String shape = "rect"; // rect, circle
    private Double padding = 0.0; // 默认无填充

    public Hitbox() {}

    public String getShape() {
        return shape;
    }

    public void setShape(String shape) {
        this.shape = shape;
    }

    public Double getPadding() {
        return padding;
    }

    public void setPadding(Double padding) {
        this.padding = padding;
    }
}