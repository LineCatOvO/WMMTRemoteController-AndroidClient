package com.linecat.wmmtcontroller.model.layout;

/**
 * 尺寸类
 */
public class Size {
    private String mode; // absolute, percent, aspect
    private Double width;
    private Double height;
    private Double aspectRatio;

    public Size() {}

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public Double getWidth() {
        return width;
    }

    public void setWidth(Double width) {
        this.width = width;
    }

    public Double getHeight() {
        return height;
    }

    public void setHeight(Double height) {
        this.height = height;
    }

    public Double getAspectRatio() {
        return aspectRatio;
    }

    public void setAspectRatio(Double aspectRatio) {
        this.aspectRatio = aspectRatio;
    }
}