package com.linecat.wmmtcontroller.layer.test;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.linecat.wmmtcontroller.layer.InputAbstractionLayer;
import com.linecat.wmmtcontroller.layer.PlatformAdaptationLayer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * NDJSONParser 是一个测试辅助类，用于解析 NDJSON 格式的事件数据
 * 支持将解析后的数据喂给 InputAbstractionLayer，用于回放一致性 Golden 测试
 */
public class NDJSONParser {
    private static final Gson gson = new Gson();
    private static final JsonParser jsonParser = new JsonParser();

    /**
     * 从 InputStream 解析 NDJSON 格式的 RawEvent
     * @param inputStream 包含 NDJSON 数据的输入流
     * @return 解析后的 RawEvent 列表
     * @throws IOException 如果解析过程中发生 IO 错误
     */
    public static List<PlatformAdaptationLayer.RawEvent> parseRawEvents(InputStream inputStream) throws IOException {
        List<PlatformAdaptationLayer.RawEvent> events = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                // 解析 JSON 对象
                JsonObject json = jsonParser.parse(line).getAsJsonObject();
                String eventType = json.get("type").getAsString();

                // 根据事件类型创建相应的 RawEvent 对象
                switch (eventType) {
                    case "RawWindowEvent":
                        events.add(parseRawWindowEvent(json));
                        break;
                    case "RawPointerEvent":
                        events.add(parseRawPointerEvent(json));
                        break;
                    case "RawSensorEvent":
                        events.add(parseRawSensorEvent(json));
                        break;
                    case "RawDropEvent":
                        events.add(parseRawDropEvent(json));
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown event type: " + eventType);
                }
            }
        }

        return events;
    }

    /**
     * 解析 RawWindowEvent
     */
    private static PlatformAdaptationLayer.RawWindowEvent parseRawWindowEvent(JsonObject json) {
        long timeNanos = json.get("timeNanos").getAsLong();
        String kindStr = json.get("kind").getAsString();
        PlatformAdaptationLayer.RawWindowEvent.Kind kind = PlatformAdaptationLayer.RawWindowEvent.Kind.valueOf(kindStr);

        // 解析 metrics
        JsonObject metricsJson = json.get("metrics").getAsJsonObject();
        int widthPx = metricsJson.get("widthPx").getAsInt();
        int heightPx = metricsJson.get("heightPx").getAsInt();
        int densityDpi = metricsJson.get("densityDpi").getAsInt();
        int rotation = metricsJson.get("rotation").getAsInt();

        PlatformAdaptationLayer.RawWindowEvent.Metrics metrics = 
                new PlatformAdaptationLayer.RawWindowEvent.Metrics(widthPx, heightPx, densityDpi, rotation);

        return new PlatformAdaptationLayer.RawWindowEvent(timeNanos, kind, metrics);
    }

    /**
     * 解析 RawPointerEvent
     */
    private static PlatformAdaptationLayer.RawPointerEvent parseRawPointerEvent(JsonObject json) {
        long timeNanos = json.get("timeNanos").getAsLong();
        String actionStr = json.get("action").getAsString();
        PlatformAdaptationLayer.RawPointerEvent.Action action = PlatformAdaptationLayer.RawPointerEvent.Action.valueOf(actionStr);
        int changedId = json.get("changedId").getAsInt();

        // 解析 pointers
        List<PlatformAdaptationLayer.RawPointerEvent.Pointer> pointers = new ArrayList<>();
        for (JsonElement pointerJson : json.get("pointers").getAsJsonArray()) {
            JsonObject pointerObj = pointerJson.getAsJsonObject();
            int id = pointerObj.get("id").getAsInt();
            float x = pointerObj.get("x").getAsFloat();
            float y = pointerObj.get("y").getAsFloat();
            pointers.add(new PlatformAdaptationLayer.RawPointerEvent.Pointer(id, x, y));
        }

        // 解析 display metrics
        JsonObject displayJson = json.get("display").getAsJsonObject();
        int widthPx = displayJson.get("widthPx").getAsInt();
        int heightPx = displayJson.get("heightPx").getAsInt();
        int densityDpi = displayJson.get("densityDpi").getAsInt();
        int rotation = displayJson.get("rotation").getAsInt();

        PlatformAdaptationLayer.RawWindowEvent.Metrics displayMetrics = 
                new PlatformAdaptationLayer.RawWindowEvent.Metrics(widthPx, heightPx, densityDpi, rotation);

        return new PlatformAdaptationLayer.RawPointerEvent(timeNanos, action, changedId, pointers, displayMetrics);
    }

    /**
     * 解析 RawSensorEvent
     */
    private static PlatformAdaptationLayer.RawSensorEvent parseRawSensorEvent(JsonObject json) {
        long timeNanos = json.get("timeNanos").getAsLong();
        String sensorTypeStr = json.get("sensorType").getAsString();
        PlatformAdaptationLayer.RawSensorEvent.SensorType sensorType = 
                PlatformAdaptationLayer.RawSensorEvent.SensorType.valueOf(sensorTypeStr);

        // 解析 values
        List<JsonElement> valuesJson = json.get("values").getAsJsonArray().asList();
        float[] values = new float[valuesJson.size()];
        for (int i = 0; i < valuesJson.size(); i++) {
            values[i] = valuesJson.get(i).getAsFloat();
        }

        String accuracyStr = json.get("accuracy").getAsString();
        PlatformAdaptationLayer.RawSensorEvent.Accuracy accuracy = 
                PlatformAdaptationLayer.RawSensorEvent.Accuracy.valueOf(accuracyStr);

        return new PlatformAdaptationLayer.RawSensorEvent(timeNanos, sensorType, values, accuracy);
    }

    /**
     * 解析 RawDropEvent
     */
    private static PlatformAdaptationLayer.RawDropEvent parseRawDropEvent(JsonObject json) {
        long timeNanos = json.get("timeNanos").getAsLong();
        String kindStr = json.get("kind").getAsString();
        PlatformAdaptationLayer.RawDropEvent.Kind kind = PlatformAdaptationLayer.RawDropEvent.Kind.valueOf(kindStr);
        int droppedCount = json.get("droppedCount").getAsInt();

        return new PlatformAdaptationLayer.RawDropEvent(timeNanos, kind, droppedCount);
    }

    /**
     * 将 RawEvent 列表喂给 InputAbstractionLayer
     * @param events RawEvent 列表
     * @param inputAbstractionLayer InputAbstractionLayer 实例
     */
    public static void feedEventsToInputAbstractionLayer(
            List<PlatformAdaptationLayer.RawEvent> events,
            InputAbstractionLayer inputAbstractionLayer) {
        for (PlatformAdaptationLayer.RawEvent event : events) {
            if (event instanceof PlatformAdaptationLayer.RawWindowEvent) {
                inputAbstractionLayer.onRawWindowEvent((PlatformAdaptationLayer.RawWindowEvent) event);
            } else if (event instanceof PlatformAdaptationLayer.RawPointerEvent) {
                inputAbstractionLayer.onRawPointerEvent((PlatformAdaptationLayer.RawPointerEvent) event);
            } else if (event instanceof PlatformAdaptationLayer.RawSensorEvent) {
                inputAbstractionLayer.onRawSensorEvent((PlatformAdaptationLayer.RawSensorEvent) event);
            } else if (event instanceof PlatformAdaptationLayer.RawDropEvent) {
                inputAbstractionLayer.onRawDropEvent((PlatformAdaptationLayer.RawDropEvent) event);
            }
        }
    }
}