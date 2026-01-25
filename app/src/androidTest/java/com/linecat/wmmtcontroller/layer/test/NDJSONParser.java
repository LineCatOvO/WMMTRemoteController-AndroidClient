package com.linecat.wmmtcontroller.layer.test;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * NDJSONParser 是一个测试辅助类，用于解析 NDJSON 格式的测试数据
 */
public class NDJSONParser {
    private final Gson gson = new Gson();

    /**
     * 从 Reader 中解析 NDJSON 数据为指定类型的对象列表
     *
     * @param reader 数据源
     * @param clazz 目标类型
     * @param <T>    目标类型泛型
     * @return 解析后的对象列表
     * @throws IOException 读取失败时抛出
     */
    public <T> List<T> parseFromReader(Reader reader, Class<T> clazz) throws IOException {
        List<T> result = new ArrayList<>();
        try (BufferedReader bufferedReader = new BufferedReader(reader)) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                T item = gson.fromJson(line, clazz);
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 从字符串中解析 NDJSON 数据为指定类型的对象列表
     *
     * @param ndjsonString NDJSON 格式的字符串
     * @param clazz        目标类型
     * @param <T>          目标类型泛型
     * @return 解析后的对象列表
     */
    public <T> List<T> parseFromString(String ndjsonString, Class<T> clazz) {
        List<T> result = new ArrayList<>();
        String[] lines = ndjsonString.split("\\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }
            T item = gson.fromJson(line, clazz);
            result.add(item);
        }
        return result;
    }

    /**
     * 将对象列表转换为 NDJSON 格式的字符串
     *
     * @param items 对象列表
     * @param <T>   对象类型泛型
     * @return NDJSON 格式的字符串
     */
    public <T> String toNDJSONString(List<T> items) {
        StringBuilder sb = new StringBuilder();
        for (T item : items) {
            sb.append(gson.toJson(item));
            sb.append("\n");
        }
        return sb.toString();
    }
}
