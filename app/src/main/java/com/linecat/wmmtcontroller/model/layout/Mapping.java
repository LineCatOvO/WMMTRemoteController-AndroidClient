package com.linecat.wmmtcontroller.model.layout;

/**
 * 映射类
 */
public class Mapping {
    private String operation;
    private String output;
    private String trigger; // press, release, axis
    private Double scale = 1.0; // 默认缩放为1
    private Boolean invert = false; // 默认不反转

    public Mapping() {}

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public String getTrigger() {
        return trigger;
    }

    public void setTrigger(String trigger) {
        this.trigger = trigger;
    }

    public Double getScale() {
        return scale;
    }

    public void setScale(Double scale) {
        this.scale = scale;
    }

    public Boolean getInvert() {
        return invert;
    }

    public void setInvert(Boolean invert) {
        this.invert = invert;
    }
}