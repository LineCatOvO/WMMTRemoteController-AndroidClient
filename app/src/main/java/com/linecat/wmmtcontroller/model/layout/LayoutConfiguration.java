package com.linecat.wmmtcontroller.model.layout;

import java.util.List;

/**
 * 布局配置主类
 */
public class LayoutConfiguration {
    private String version;
    private List<UiElement> ui;
    private List<Operation> operation;
    private List<Mapping> mapping;

    public LayoutConfiguration() {}

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<UiElement> getUi() {
        return ui;
    }

    public void setUi(List<UiElement> ui) {
        this.ui = ui;
    }

    public List<Operation> getOperation() {
        return operation;
    }

    public void setOperation(List<Operation> operation) {
        this.operation = operation;
    }

    public List<Mapping> getMapping() {
        return mapping;
    }

    public void setMapping(List<Mapping> mapping) {
        this.mapping = mapping;
    }
}