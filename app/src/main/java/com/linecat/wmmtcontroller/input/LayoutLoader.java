package com.linecat.wmmtcontroller.input;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * 布局加载器
 * 负责加载和解析符合设计文档的 JSON 布局文件
 */
public class LayoutLoader {
    private static final String TAG = "LayoutLoader";
    private final Context context;

    public LayoutLoader(Context context) {
        this.context = context;
    }

    /**
     * 从 Assets 加载布局文件
     */
    public LayoutSnapshot loadLayoutFromAssets(String assetPath) throws IOException, JSONException {
        AssetManager assetManager = context.getAssets();
        try (InputStream inputStream = assetManager.open(assetPath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
            return parseLayoutJson(stringBuilder.toString());
        }
    }

    /**
     * 从 JSON 字符串解析布局
     */
    public LayoutSnapshot parseLayoutJson(String jsonString) throws JSONException {
        JSONObject root = new JSONObject(jsonString);

        // 解析 meta 部分
        JSONObject meta = root.optJSONObject("meta");
        String layoutName = meta != null ? meta.optString("name", "Unknown Layout") : "Unknown Layout";
        String layoutVersion = meta != null ? meta.optString("version", "1.0.0") : "1.0.0";

        // 解析 ui 层
        List<Region> uiRegions = parseUILayer(root.optJSONObject("ui"));

        // 解析 operation 层
        List<Region> operationRegions = parseOperationLayer(root.optJSONObject("operations"));

        // 解析 mapping 层
        List<Region> mappingRegions = parseMappingLayer(root.optJSONObject("mappings"));

        // 合并所有区域
        List<Region> allRegions = new ArrayList<>();
        allRegions.addAll(uiRegions);
        allRegions.addAll(operationRegions);
        allRegions.addAll(mappingRegions);

        // 创建布局快照
        return new LayoutSnapshot(allRegions);
    }

    /**
     * 解析 UI 层
     */
    private List<Region> parseUILayer(JSONObject uiJson) throws JSONException {
        List<Region> regions = new ArrayList<>();
        if (uiJson == null) {
            return regions;
        }

        // 遍历所有 UI 元素
        JSONArray uiElements = uiJson.optJSONArray("elements");
        if (uiElements != null) {
            for (int i = 0; i < uiElements.length(); i++) {
                JSONObject element = uiElements.getJSONObject(i);
                Region region = parseUIElement(element);
                if (region != null) {
                    regions.add(region);
                }
            }
        }

        return regions;
    }

    /**
     * 解析 UI 元素
     */
    private Region parseUIElement(JSONObject element) throws JSONException {
        String id = element.getString("id");
        String type = element.getString("type");
        float left = (float) element.getDouble("left");
        float top = (float) element.getDouble("top");
        float right = (float) element.getDouble("right");
        float bottom = (float) element.getDouble("bottom");
        int zIndex = element.optInt("zIndex", 0);
        float deadzone = (float) element.optDouble("deadzone", 0.0);
        String curve = element.optString("curve", "linear");

        Region.RegionType regionType = mapUITypeToRegionType(type);
        if (regionType == null) {
            Log.w(TAG, "Unknown UI element type: " + type);
            return null;
        }

        // 解析范围
        float[] range = null;
        JSONObject rangeJson = element.optJSONObject("range");
        if (rangeJson != null) {
            range = new float[]{(float) rangeJson.getDouble("min"), (float) rangeJson.getDouble("max")};
        }

        // 解析输出范围
        float[] outputRange = null;
        JSONObject outputRangeJson = element.optJSONObject("outputRange");
        if (outputRangeJson != null) {
            outputRange = new float[]{(float) outputRangeJson.getDouble("min"), (float) outputRangeJson.getDouble("max")};
        }

        // 创建区域
        return new Region(
                id,
                regionType,
                left,
                top,
                right,
                bottom,
                zIndex,
                deadzone,
                curve,
                range,
                outputRange,
                null, // operationType
                null, // mappingType
                null, // mappingKey
                null, // mappingAxis
                null, // mappingButton
                null, // customMappingTarget
                null  // customData
        );
    }

    /**
     * 解析 Operation 层
     */
    private List<Region> parseOperationLayer(JSONObject operationJson) throws JSONException {
        List<Region> regions = new ArrayList<>();
        if (operationJson == null) {
            return regions;
        }

        // 遍历所有 Operation 元素
        JSONArray operations = operationJson.optJSONArray("elements");
        if (operations != null) {
            for (int i = 0; i < operations.length(); i++) {
                JSONObject operation = operations.getJSONObject(i);
                Region region = parseOperationElement(operation);
                if (region != null) {
                    regions.add(region);
                }
            }
        }

        return regions;
    }

    /**
     * 解析 Operation 元素
     */
    private Region parseOperationElement(JSONObject operation) throws JSONException {
        String id = operation.getString("id");
        String type = operation.getString("type");
        int zIndex = operation.optInt("zIndex", 0);
        float deadzone = (float) operation.optDouble("deadzone", 0.0);
        String curve = operation.optString("curve", "linear");

        Region.OperationType operationType = mapOperationType(type);
        if (operationType == null) {
            Log.w(TAG, "Unknown operation type: " + type);
            return null;
        }

        // 解析范围
        float[] range = null;
        JSONObject rangeJson = operation.optJSONObject("range");
        if (rangeJson != null) {
            range = new float[]{(float) rangeJson.getDouble("min"), (float) rangeJson.getDouble("max")};
        }

        // 创建区域
        return new Region(
                id,
                Region.RegionType.OPERATION,
                0f, // left
                0f, // top
                1f, // right
                1f, // bottom
                zIndex,
                deadzone,
                curve,
                range,
                null, // outputRange
                operationType,
                null, // mappingType
                null, // mappingKey
                null, // mappingAxis
                null, // mappingButton
                null, // customMappingTarget
                null  // customData
        );
    }

    /**
     * 解析 Mapping 层
     */
    private List<Region> parseMappingLayer(JSONObject mappingJson) throws JSONException {
        List<Region> regions = new ArrayList<>();
        if (mappingJson == null) {
            return regions;
        }

        // 遍历所有 Mapping 元素
        JSONArray mappings = mappingJson.optJSONArray("elements");
        if (mappings != null) {
            for (int i = 0; i < mappings.length(); i++) {
                JSONObject mapping = mappings.getJSONObject(i);
                Region region = parseMappingElement(mapping);
                if (region != null) {
                    regions.add(region);
                }
            }
        }

        return regions;
    }

    /**
     * 解析 Mapping 元素
     */
    private Region parseMappingElement(JSONObject mapping) throws JSONException {
        String id = mapping.getString("id");
        String type = mapping.getString("type");
        String operation = mapping.getString("operation");
        int zIndex = mapping.optInt("zIndex", 0);
        String curve = mapping.optString("curve", "linear");

        Region.MappingType mappingType = mapMappingType(type);
        if (mappingType == null) {
            Log.w(TAG, "Unknown mapping type: " + type);
            return null;
        }

        // 解析输出范围
        float[] outputRange = null;
        JSONObject outputRangeJson = mapping.optJSONObject("outputRange");
        if (outputRangeJson != null) {
            outputRange = new float[]{(float) outputRangeJson.getDouble("min"), (float) outputRangeJson.getDouble("max")};
        }

        // 解析映射目标
        String mappingKey = null;
        String mappingAxis = null;
        String mappingButton = null;
        String customMappingTarget = null;

        JSONObject target = mapping.getJSONObject("target");
        switch (mappingType) {
            case KEYBOARD:
                mappingKey = target.getString("key");
                break;
            case GAMEPAD:
                if (target.has("axis")) {
                    mappingAxis = target.getString("axis");
                } else if (target.has("button")) {
                    mappingButton = target.getString("button");
                }
                break;
            case CUSTOM:
                customMappingTarget = target.getString("target");
                break;
        }

        // 创建区域
        return new Region(
                id,
                Region.RegionType.MAPPING,
                0f, // left
                0f, // top
                1f, // right
                1f, // bottom
                zIndex,
                0f, // deadzone
                curve,
                null, // range
                outputRange,
                null, // operationType
                mappingType,
                mappingKey,
                mappingAxis,
                mappingButton,
                customMappingTarget,
                null  // customData
        );
    }

    /**
     * 映射 UI 类型到区域类型
     */
    private Region.RegionType mapUITypeToRegionType(String uiType) {
        switch (uiType) {
            case "button":
                return Region.RegionType.BUTTON;
            case "axis":
                return Region.RegionType.AXIS;
            case "gesture":
                return Region.RegionType.GESTURE;
            case "gyroscope":
                return Region.RegionType.GYROSCOPE;
            default:
                return null;
        }
    }

    /**
     * 映射操作类型
     */
    private Region.OperationType mapOperationType(String operationType) {
        switch (operationType) {
            case "steering":
                return Region.OperationType.STEERING;
            case "throttle":
                return Region.OperationType.THROTTLE;
            case "brake":
                return Region.OperationType.BRAKE;
            case "button":
                return Region.OperationType.BUTTON;
            case "custom":
                return Region.OperationType.CUSTOM;
            default:
                return null;
        }
    }

    /**
     * 映射映射类型
     */
    private Region.MappingType mapMappingType(String mappingType) {
        switch (mappingType) {
            case "keyboard":
                return Region.MappingType.KEYBOARD;
            case "gamepad":
                return Region.MappingType.GAMEPAD;
            case "custom":
                return Region.MappingType.CUSTOM;
            default:
                return null;
        }
    }
}