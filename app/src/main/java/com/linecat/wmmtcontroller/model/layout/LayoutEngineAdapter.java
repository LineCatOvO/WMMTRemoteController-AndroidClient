package com.linecat.wmmtcontroller.model.layout;

import com.linecat.wmmtcontroller.input.LayoutEngine;
import com.linecat.wmmtcontroller.input.LayoutSnapshot;
import com.linecat.wmmtcontroller.model.RawInput;
import com.linecat.wmmtcontroller.model.InputState;

import java.util.List;

/**
 * 布局引擎适配器
 * 用于桥接新版布局系统和旧版 LayoutEngine
 */
public class LayoutEngineAdapter {
    private final LayoutEngine legacyLayoutEngine;  // 旧版引擎
    private NewLayoutLoader newLayoutLoader;        // 新版加载器

    public LayoutEngineAdapter(LayoutEngine legacyLayoutEngine) {
        this.legacyLayoutEngine = legacyLayoutEngine;
    }

    /**
     * 从新的布局配置 JSON 加载布局
     *
     * @param newFormatJson 新格式的布局 JSON
     */
    public void loadLayoutFromNewFormat(String newFormatJson) {
        try {
            if (newLayoutLoader == null) {
                newLayoutLoader = new NewLayoutLoader();
            }

            // 验证新格式的布局
            if (!newLayoutLoader.isValidLayoutConfiguration(newFormatJson)) {
                throw new IllegalArgumentException("Invalid new format layout configuration");
            }

            // 将新格式转换为旧格式并加载
            LayoutSnapshot snapshot = newLayoutLoader.loadLayoutSnapshot(newFormatJson);
            legacyLayoutEngine.setLayout(snapshot);

        } catch (Exception e) {
            // 如果新格式加载失败，记录错误并保持原有布局
            e.printStackTrace();
            // 可以选择抛出异常或使用默认布局
            throw new RuntimeException("Failed to load new format layout: " + e.getMessage(), e);
        }
    }

    /**
     * 验证新格式的布局配置
     *
     * @param newFormatJson 新格式的布局 JSON
     * @return 是否有效
     */
    public boolean isValidNewFormatLayout(String newFormatJson) {
        try {
            if (newLayoutLoader == null) {
                newLayoutLoader = new NewLayoutLoader();
            }
            return newLayoutLoader.isValidLayoutConfiguration(newFormatJson);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 执行布局处理（使用底层的旧版引擎）
     *
     * @param rawInput 原始输入
     * @param frameId  帧 ID
     * @return 输入状态
     */
    public InputState executeLayout(RawInput rawInput, long frameId) {
        return legacyLayoutEngine.executeLayout(rawInput, frameId);
    }

    /**
     * 获取底层的旧版布局引擎
     *
     * @return 旧版布局引擎
     */
    public LayoutEngine getLegacyLayoutEngine() {
        return legacyLayoutEngine;
    }
}