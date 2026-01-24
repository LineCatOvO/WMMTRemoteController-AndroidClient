package com.linecat.wmmtcontroller.model.layout;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

/**
 * 布局配置序列化器
 */
public class LayoutSerializer {

    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    /**
     * 将布局配置对象序列化为 JSON 字符串
     *
     * @param config 布局配置对象
     * @return JSON 字符串
     */
    public static String serialize(LayoutConfiguration config) {
        return gson.toJson(config);
    }

    /**
     * 将 JSON 字符串反序列化为布局配置对象
     *
     * @param jsonString JSON 字符串
     * @return 布局配置对象
     */
    public static LayoutConfiguration deserialize(String jsonString) {
        return gson.fromJson(jsonString, LayoutConfiguration.class);
    }

    /**
     * 验证 JSON 字符串是否符合布局配置格式
     *
     * @param jsonString 待验证的 JSON 字符串
     * @return 是否有效
     */
    public static boolean isValidLayoutConfig(String jsonString) {
        try {
            LayoutConfiguration config = deserialize(jsonString);
            return config != null &&
                   config.getVersion() != null &&
                   config.getUi() != null &&
                   config.getOperation() != null &&
                   config.getMapping() != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 验证版本号格式是否正确
     *
     * @param version 版本号
     * @return 是否有效
     */
    public static boolean isValidVersion(String version) {
        if (version == null) {
            return false;
        }
        // 检查版本号格式是否为 x.y.z
        return version.matches("^\\d+\\.\\d+\\.\\d+$");
    }
}