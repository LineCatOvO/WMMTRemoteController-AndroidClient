package com.linecat.wmmtcontroller.model.layout;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.linecat.wmmtcontroller.input.LayoutSnapshot;
import com.linecat.wmmtcontroller.input.LayoutLoader;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 新版布局加载器
 * 使用新的实体类结构加载布局配置
 */
public class NewLayoutLoader {

    private final Gson gson;

    public NewLayoutLoader() {
        this.gson = new Gson();
    }

    /**
     * 从 JSON 字符串加载布局配置
     *
     * @param jsonString JSON 字符串
     * @return LayoutConfiguration 对象
     * @throws JsonSyntaxException 如果 JSON 格式无效
     */
    public LayoutConfiguration loadLayoutConfiguration(String jsonString) throws JsonSyntaxException {
        return gson.fromJson(jsonString, LayoutConfiguration.class);
    }

    /**
     * 从 JSON 字符串加载布局快照
     *
     * @param jsonString JSON 字符串
     * @return LayoutSnapshot 对象
     * @throws JsonSyntaxException 如果 JSON 格式无效
     */
    public LayoutSnapshot loadLayoutSnapshot(String jsonString) throws JsonSyntaxException {
        LayoutConfiguration config = loadLayoutConfiguration(jsonString);
        return LayoutToRegionConverter.convertToLayoutSnapshot(config);
    }

    /**
     * 验证 JSON 字符串是否符合新的布局配置格式
     *
     * @param jsonString 待验证的 JSON 字符串
     * @return 是否有效
     */
    public boolean isValidLayoutConfiguration(String jsonString) {
        try {
            LayoutConfiguration config = loadLayoutConfiguration(jsonString);
            return LayoutSerializer.isValidLayoutConfig(jsonString) &&
                   config.getVersion() != null &&
                   config.getUi() != null &&
                   config.getOperation() != null &&
                   config.getMapping() != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 将布局配置转换为 JSON 字符串
     *
     * @param config 布局配置对象
     * @return JSON 字符串
     */
    public String serializeLayoutConfiguration(LayoutConfiguration config) {
        return gson.toJson(config);
    }
}