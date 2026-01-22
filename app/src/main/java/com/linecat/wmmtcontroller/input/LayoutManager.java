package com.linecat.wmmtcontroller.input;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * 布局管理器
 * 负责管理应用私有目录中的布局项目
 * 每个布局项目是layout文件夹下的一个子文件夹，包含一个main.json文件
 */
public class LayoutManager {
    private static final String TAG = "LayoutManager";
    private static final String LAYOUT_DIR_NAME = "layout";
    private static final String MAIN_JSON_NAME = "main.json";
    
    private Context context;
    private File layoutsDir;
    
    /**
     * 构造函数
     * @param context 上下文
     */
    public LayoutManager(Context context) {
        this.context = context;
        initLayoutsDirectory();
    }
    
    /**
     * 初始化布局目录
     * 如果layout目录不存在，则创建它
     */
    private void initLayoutsDirectory() {
        // 获取应用外部存储目录下的layout文件夹
        // 路径格式：/storage/emulated/0/android/data/packageName/layout
        layoutsDir = new File(context.getExternalFilesDir(null), LAYOUT_DIR_NAME);
        
        // 如果目录不存在，创建它
        if (!layoutsDir.exists()) {
            boolean created = layoutsDir.mkdirs();
            if (created) {
                Log.d(TAG, "Layouts directory created: " + layoutsDir.getAbsolutePath());
            } else {
                Log.e(TAG, "Failed to create layouts directory: " + layoutsDir.getAbsolutePath());
            }
        } else {
            Log.d(TAG, "Layouts directory already exists: " + layoutsDir.getAbsolutePath());
        }
    }
    
    /**
     * 获取所有布局项目
     * @return 布局项目列表
     */
    public List<LayoutProject> getLayoutProjects() {
        List<LayoutProject> projects = new ArrayList<>();
        
        // 获取所有子文件夹
        File[] subdirs = layoutsDir.listFiles(File::isDirectory);
        if (subdirs == null) {
            return projects;
        }
        
        // 遍历每个子文件夹，读取main.json
        for (File subdir : subdirs) {
            File mainJsonFile = new File(subdir, MAIN_JSON_NAME);
            if (mainJsonFile.exists()) {
                try {
                    String mainJson = readFile(mainJsonFile);
                    JSONObject jsonObject = new JSONObject(mainJson);
                    
                    LayoutProject project = new LayoutProject(
                        subdir.getName(),
                        jsonObject.optString("name", subdir.getName()),
                        jsonObject.optString("description", ""),
                        jsonObject.optString("version", "1.0.0"),
                        mainJson
                    );
                    
                    projects.add(project);
                } catch (IOException | JSONException e) {
                    Log.e(TAG, "Error reading layout project: " + subdir.getName(), e);
                }
            }
        }
        
        return projects;
    }
    
    /**
     * 创建新的布局项目
     * @param projectId 项目ID（作为子文件夹名称）
     * @param mainJson main.json文件内容
     * @return 是否创建成功
     */
    public boolean createLayoutProject(String projectId, String mainJson) {
        try {
            // 验证main.json格式
            new JSONObject(mainJson);
            
            // 创建项目目录
            File projectDir = new File(layoutsDir, projectId);
            if (projectDir.exists()) {
                Log.e(TAG, "Layout project already exists: " + projectId);
                return false;
            }
            
            if (!projectDir.mkdirs()) {
                Log.e(TAG, "Failed to create project directory: " + projectId);
                return false;
            }
            
            // 写入main.json文件
            File mainJsonFile = new File(projectDir, MAIN_JSON_NAME);
            writeFile(mainJsonFile, mainJson);
            
            Log.d(TAG, "Layout project created: " + projectId);
            return true;
        } catch (JSONException e) {
            Log.e(TAG, "Invalid main.json format: " + e.getMessage());
            return false;
        } catch (IOException e) {
            Log.e(TAG, "Error creating layout project: " + projectId, e);
            return false;
        }
    }
    
    /**
     * 更新布局项目
     * @param projectId 项目ID
     * @param mainJson main.json文件内容
     * @return 是否更新成功
     */
    public boolean updateLayoutProject(String projectId, String mainJson) {
        try {
            // 验证main.json格式
            new JSONObject(mainJson);
            
            // 检查项目是否存在
            File projectDir = new File(layoutsDir, projectId);
            if (!projectDir.exists() || !projectDir.isDirectory()) {
                Log.e(TAG, "Layout project not found: " + projectId);
                return false;
            }
            
            // 写入main.json文件
            File mainJsonFile = new File(projectDir, MAIN_JSON_NAME);
            writeFile(mainJsonFile, mainJson);
            
            Log.d(TAG, "Layout project updated: " + projectId);
            return true;
        } catch (JSONException e) {
            Log.e(TAG, "Invalid main.json format: " + e.getMessage());
            return false;
        } catch (IOException e) {
            Log.e(TAG, "Error updating layout project: " + projectId, e);
            return false;
        }
    }
    
    /**
     * 删除布局项目
     * @param projectId 项目ID
     * @return 是否删除成功
     */
    public boolean deleteLayoutProject(String projectId) {
        // 检查项目是否存在
        File projectDir = new File(layoutsDir, projectId);
        if (!projectDir.exists() || !projectDir.isDirectory()) {
            Log.e(TAG, "Layout project not found: " + projectId);
            return false;
        }
        
        // 删除项目目录及其所有内容
        boolean deleted = deleteDirectory(projectDir);
        if (deleted) {
            Log.d(TAG, "Layout project deleted: " + projectId);
        } else {
            Log.e(TAG, "Failed to delete layout project: " + projectId);
        }
        
        return deleted;
    }
    
    /**
     * 读取布局项目的main.json文件
     * @param projectId 项目ID
     * @return main.json文件内容，读取失败返回null
     */
    public String readLayoutProject(String projectId) {
        try {
            // 检查项目是否存在
            File projectDir = new File(layoutsDir, projectId);
            if (!projectDir.exists() || !projectDir.isDirectory()) {
                Log.e(TAG, "Layout project not found: " + projectId);
                return null;
            }
            
            // 读取main.json文件
            File mainJsonFile = new File(projectDir, MAIN_JSON_NAME);
            if (!mainJsonFile.exists()) {
                Log.e(TAG, "main.json not found in project: " + projectId);
                return null;
            }
            
            return readFile(mainJsonFile);
        } catch (IOException e) {
            Log.e(TAG, "Error reading layout project: " + projectId, e);
            return null;
        }
    }
    
    /**
     * 检查布局项目是否存在
     * @param projectId 项目ID
     * @return 是否存在
     */
    public boolean layoutProjectExists(String projectId) {
        File projectDir = new File(layoutsDir, projectId);
        File mainJsonFile = new File(projectDir, MAIN_JSON_NAME);
        return projectDir.exists() && projectDir.isDirectory() && mainJsonFile.exists();
    }
    
    /**
     * 读取文件内容
     * @param file 文件对象
     * @return 文件内容
     * @throws IOException IO异常
     */
    private String readFile(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (FileInputStream fis = new FileInputStream(file);
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader reader = new BufferedReader(isr)) {
            
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }
    
    /**
     * 写入文件内容
     * @param file 文件对象
     * @param content 文件内容
     * @throws IOException IO异常
     */
    private void writeFile(File file, String content) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file);
             OutputStreamWriter osw = new OutputStreamWriter(fos);) {
            
            osw.write(content);
        }
    }
    
    /**
     * 删除目录及其所有内容
     * @param directory 目录对象
     * @return 是否删除成功
     */
    private boolean deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        return directory.delete();
    }
    
    /**
     * 布局项目类
     * 表示一个布局项目的元数据
     */
    public static class LayoutProject {
        private String id;
        private String name;
        private String description;
        private String version;
        private String mainJson;
        
        /**
         * 构造函数
         * @param id 项目ID
         * @param name 项目名称
         * @param description 项目描述
         * @param version 项目版本
         * @param mainJson main.json文件内容
         */
        public LayoutProject(String id, String name, String description, String version, String mainJson) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.version = version;
            this.mainJson = mainJson;
        }
        
        // Getters and setters
        public String getId() {
            return id;
        }
        
        public void setId(String id) {
            this.id = id;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
        
        public String getVersion() {
            return version;
        }
        
        public void setVersion(String version) {
            this.version = version;
        }
        
        public String getMainJson() {
            return mainJson;
        }
        
        public void setMainJson(String mainJson) {
            this.mainJson = mainJson;
        }
        
        @Override
        public String toString() {
            return "LayoutProject{id='" + id + "', name='" + name + "'}";
        }
    }
}