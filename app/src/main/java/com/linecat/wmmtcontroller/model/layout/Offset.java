package com.linecat.wmmtcontroller.model.layout;

/**
 * 偏移量类
 */
public class Offset {
    private Double x;
    private Double y;
    private String unit = "px"; // 默认单位为像素

    public Offset() {}

    public Double getX() {
        return x;
    }

    public void setX(Double x) {
        this.x = x;
    }

    public Double getY() {
        return y;
    }

    public void setY(Double y) {
        this.y = y;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }
}