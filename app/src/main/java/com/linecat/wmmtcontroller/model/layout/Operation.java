package com.linecat.wmmtcontroller.model.layout;

/**
 * 操作类
 */
public class Operation {
    private String id;
    private String type; // binary, axis
    private Range range;
    private Double defaultVal; // default 是关键字，所以使用 defaultVal

    public Operation() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Range getRange() {
        return range;
    }

    public void setRange(Range range) {
        this.range = range;
    }

    public Double getDefaultVal() {
        return defaultVal;
    }

    public void setDefaultVal(Double defaultVal) {
        this.defaultVal = defaultVal;
    }
}