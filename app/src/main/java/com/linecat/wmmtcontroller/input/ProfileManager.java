package com.linecat.wmmtcontroller.input;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import com.linecat.wmmtcontroller.model.InputState;
import com.linecat.wmmtcontroller.model.RawInput;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 配置文件管理器
 * 负责加载、切换、校验、回滚脚本配置文件
 */
public class ProfileManager {
    
    private static final String TAG = "ProfileManager";
    
    private Context context;
    private InputScriptEngine scriptEngine;
    private AtomicReference<ScriptProfile> currentProfile = new AtomicReference<>();
    private AtomicReference<ScriptProfile> previousProfile = new AtomicReference<>();
    private List<ScriptProfile> availableProfiles = new ArrayList<>();
    
    /**
     * 构造函数
     * @param context 上下文
     * @param scriptEngine 脚本引擎
     */
    public ProfileManager(Context context, InputScriptEngine scriptEngine) {
        this.context = context;
        this.scriptEngine = scriptEngine;
        // 延迟加载可用profile，避免在测试环境中因Context模拟不完整而失败
        // loadAvailableProfiles();
    }
    
    /**
     * 加载可用的配置文件
     */
    public void loadAvailableProfiles() {
        availableProfiles.clear();
        
        // 从assets加载配置文件
        loadProfilesFromAssets();
        
        // 从外部存储加载配置文件
        loadProfilesFromStorage();
        
        Log.d(TAG, "Loaded " + availableProfiles.size() + " profiles");
    }
    
    /**
     * 从assets加载配置文件
     */
    private void loadProfilesFromAssets() {
        AssetManager assetManager = context.getAssets();
        
        try {
            // 列出assets/profiles目录下的所有文件
            String[] profileFiles = assetManager.list("profiles");
            if (profileFiles != null) {
                for (String profileFile : profileFiles) {
                    if (profileFile.endsWith(".json")) {
                        // 加载profile.json
                        String profileJson = readAssetFile("profiles/" + profileFile);
                        JSONObject jsonObject = new JSONObject(profileJson);
                        
                        // 加载对应的脚本文件
                        String entryPoint = jsonObject.getString("entry");
                        String scriptCode = readAssetFile("scripts/" + entryPoint);
                        
                        // 创建ScriptProfile对象
                        ScriptProfile profile = new ScriptProfile(
                            jsonObject.getString("name"),
                            jsonObject.getString("version"),
                            jsonObject.getString("author"),
                            entryPoint,
                            scriptCode
                        );
                        
                        // 设置ID
                        if (jsonObject.has("id")) {
                            profile.setId(jsonObject.getString("id"));
                        }
                        
                        // 添加到可用配置文件列表
                        availableProfiles.add(profile);
                    }
                }
            }
        } catch (IOException | JSONException e) {
            Log.e(TAG, "Error loading profiles from assets: " + e.getMessage());
        }
    }
    
    /**
     * 从外部存储加载配置文件
     */
    private void loadProfilesFromStorage() {
        // 实现从外部存储加载配置文件
        // 暂时跳过，后续实现
    }
    
    /**
     * 读取assets文件
     * @param filePath 文件路径
     * @return 文件内容
     * @throws IOException IO异常
     */
    private String readAssetFile(String filePath) throws IOException {
        AssetManager assetManager = context.getAssets();
        try (InputStream inputStream = assetManager.open(filePath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
            return stringBuilder.toString();
        }
    }
    
    /**
     * 切换到指定配置文件
     * 约定：
     * - 同步切换，阻塞当前线程
     * - 不允许在输入帧中途切换
     * - 切换成功后才替换当前Profile
     * - 切换失败时清空所有heldKeys
     * 
     * @param profile 要切换的配置文件
     * @return 是否切换成功
     */
    public synchronized boolean switchProfile(ScriptProfile profile) {
        if (profile == null) {
            Log.e(TAG, "Cannot switch to null profile");
            return false;
        }
        
        // 验证配置文件
        if (!validateProfile(profile)) {
            Log.e(TAG, "Profile validation failed: " + profile.getName());
            // 切换失败，清空所有heldKeys
            clearAllKeys();
            return false;
        }
        
        // 加载脚本
        boolean loadSuccess = scriptEngine.loadScript(profile.getScriptCode());
        if (!loadSuccess) {
            Log.e(TAG, "Failed to load script: " + profile.getName() + ", Error: " + scriptEngine.getLastError());
            // 切换失败，清空所有heldKeys
            clearAllKeys();
            return false;
        }
        
        // 原子切换：先保存当前Profile，再替换
        previousProfile.set(currentProfile.get());
        currentProfile.set(profile);
        
        Log.d(TAG, "Switched to profile: " + profile.getName());
        return true;
    }
    
    /**
     * 清空所有按键状态
     */
    private void clearAllKeys() {
        try {
            // 清空所有按键状态，避免粘键
            // 这里通过调用scriptEngine的reset方法来实现
            scriptEngine.reset();
            Log.d(TAG, "Cleared all keys due to profile operation failure");
        } catch (Exception e) {
            Log.e(TAG, "Error clearing keys: " + e.getMessage());
        }
    }
    
    /**
     * 切换到指定ID的配置文件
     * @param profileId 配置文件ID
     * @return 是否切换成功
     */
    public boolean switchProfileById(String profileId) {
        for (ScriptProfile profile : availableProfiles) {
            if (profile.getId().equals(profileId)) {
                return switchProfile(profile);
            }
        }
        Log.e(TAG, "Profile not found: " + profileId);
        return false;
    }
    
    /**
     * 切换到默认配置文件
     * @return 是否切换成功
     */
    public boolean switchToDefaultProfile() {
        if (!availableProfiles.isEmpty()) {
            return switchProfile(availableProfiles.get(0));
        }
        Log.e(TAG, "No profiles available");
        return false;
    }
    
    /**
     * 回滚到上一个配置文件
     * 约定：
     * - 回滚到上一个可用Profile
     * - 若上一个Profile不可用，回滚到默认Profile
     * - 若默认Profile不可用，切换到传统Pipeline
     * - 回滚失败时清空所有heldKeys
     * 
     * @return 是否回滚成功
     */
    public synchronized boolean rollbackProfile() {
        ScriptProfile prev = previousProfile.get();
        if (prev != null) {
            boolean rollbackSuccess = switchProfile(prev);
            if (rollbackSuccess) {
                Log.d(TAG, "Rolled back to previous profile: " + prev.getName());
                return true;
            }
        }
        
        // 上一个Profile不可用，尝试使用默认Profile
        if (!availableProfiles.isEmpty()) {
            ScriptProfile defaultProfile = availableProfiles.get(0);
            boolean defaultSuccess = switchProfile(defaultProfile);
            if (defaultSuccess) {
                Log.d(TAG, "Rolled back to default profile: " + defaultProfile.getName());
                return true;
            }
        }
        
        // 默认Profile也不可用，清空所有按键并返回失败
        clearAllKeys();
        Log.w(TAG, "No profile to rollback to, cleared all keys");
        return false;
    }
    
    /**
     * 验证配置文件
     * @param profile 配置文件
     * @return 是否有效
     */
    public boolean validateProfile(ScriptProfile profile) {
        // 验证核心字段
        if (profile.getName() == null || profile.getName().isEmpty()) {
            Log.e(TAG, "Profile missing name");
            return false;
        }
        
        if (profile.getVersion() == null || profile.getVersion().isEmpty()) {
            Log.e(TAG, "Profile missing version");
            return false;
        }
        
        if (profile.getAuthor() == null || profile.getAuthor().isEmpty()) {
            Log.e(TAG, "Profile missing author");
            return false;
        }
        
        if (profile.getEntryPoint() == null || profile.getEntryPoint().isEmpty()) {
            Log.e(TAG, "Profile missing entry point");
            return false;
        }
        
        if (profile.getScriptCode() == null || profile.getScriptCode().isEmpty()) {
            Log.e(TAG, "Profile missing script code");
            return false;
        }
        
        // 验证engineApiVersion
        if (profile.getEngineApiVersion() == null || profile.getEngineApiVersion().isEmpty()) {
            Log.e(TAG, "Profile missing engineApiVersion");
            return false;
        }
        
        // 验证脚本是否包含必要函数
        String scriptCode = profile.getScriptCode();
        if (!scriptCode.contains("function update")) {
            Log.e(TAG, "Script missing required function: update");
            return false;
        }
        
        return true;
    }
    
    /**
     * 导入Profile包
     * @param zipFilePath zip文件路径
     * @return 导入的Profile，如果导入失败返回null
     */
    public ScriptProfile importProfileFromZip(String zipFilePath) {
        try {
            // 创建Zip文件输入流
            java.util.zip.ZipInputStream zipInputStream = new java.util.zip.ZipInputStream(
                    new java.io.FileInputStream(zipFilePath));
            
            // 解析zip文件内容
            java.util.zip.ZipEntry entry;
            String profileJson = null;
            String scriptCode = null;
            
            while ((entry = zipInputStream.getNextEntry()) != null) {
                String entryName = entry.getName();
                if (entryName.equals("profile.json")) {
                    // 读取profile.json
                    profileJson = readZipEntry(zipInputStream);
                } else if (entryName.equals("main.js")) {
                    // 读取main.js
                    scriptCode = readZipEntry(zipInputStream);
                }
                zipInputStream.closeEntry();
            }
            
            zipInputStream.close();
            
            // 验证必要文件是否存在
            if (profileJson == null || scriptCode == null) {
                Log.e(TAG, "Missing required files in zip package");
                return null;
            }
            
            // 解析profile.json，获取元信息
            JSONObject jsonObject = new JSONObject(profileJson);
            String name = jsonObject.getString("name");
            String version = jsonObject.getString("version");
            String author = jsonObject.getString("author");
            String entryPoint = jsonObject.getString("entry");
            String engineApiVersion = jsonObject.optString("engineApiVersion", "1.0.0");
            
            // 创建ScriptProfile对象
            ScriptProfile profile = new ScriptProfile(
                    name,
                    version,
                    author,
                    entryPoint,
                    scriptCode
            );
            
            // 设置engineApiVersion
            profile.setEngineApiVersion(engineApiVersion);
            
            // 验证profile
            if (!validateProfile(profile)) {
                Log.e(TAG, "Imported profile validation failed");
                return null;
            }
            
            // 测试脚本是否能正常执行（空RawInput帧）
            boolean testSuccess = testProfileScript(profile);
            if (!testSuccess) {
                Log.e(TAG, "Profile script test failed during import");
                return null;
            }
            
            // 检查是否存在同名profile，处理冲突
            ScriptProfile existingProfile = findProfileByName(name);
            if (existingProfile != null) {
                // 重命名新profile
                String newName = name + "_" + System.currentTimeMillis();
                profile.setName(newName);
                Log.d(TAG, "Profile with same name exists, renamed to: " + newName);
            }
            
            // 添加到可用profile列表
            availableProfiles.add(profile);
            Log.d(TAG, "Profile imported successfully: " + profile.getName());
            
            return profile;
        } catch (Exception e) {
            Log.e(TAG, "Error importing profile from zip: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 导出当前Profile为zip包
     * @param outputPath 输出路径
     * @return 是否导出成功
     */
    public boolean exportProfileToZip(String outputPath) {
        ScriptProfile profile = currentProfile.get();
        if (profile == null) {
            Log.e(TAG, "No current profile to export");
            return false;
        }
        
        try {
            // 创建Zip文件输出流
            java.util.zip.ZipOutputStream zipOutputStream = new java.util.zip.ZipOutputStream(
                    new java.io.FileOutputStream(outputPath));
            
            // 创建profile.json
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("id", profile.getId());
            jsonObject.put("name", profile.getName());
            jsonObject.put("version", profile.getVersion());
            jsonObject.put("author", profile.getAuthor());
            jsonObject.put("entry", profile.getEntryPoint());
            jsonObject.put("engineApiVersion", profile.getEngineApiVersion());
            
            // 写入profile.json到zip
            addZipEntry(zipOutputStream, "profile.json", jsonObject.toString(2));
            
            // 写入main.js到zip
            addZipEntry(zipOutputStream, "main.js", profile.getScriptCode());
            
            // 关闭zip输出流
            zipOutputStream.close();
            
            Log.d(TAG, "Profile exported successfully to: " + outputPath);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error exporting profile to zip: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 读取zip条目内容
     * @param zipInputStream zip输入流
     * @return 条目内容
     * @throws IOException IO异常
     */
    private String readZipEntry(java.util.zip.ZipInputStream zipInputStream) throws IOException {
        java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(zipInputStream, java.nio.charset.StandardCharsets.UTF_8));
        StringBuilder content = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            content.append(line).append("\n");
        }
        return content.toString();
    }
    
    /**
     * 添加zip条目
     * @param zipOutputStream zip输出流
     * @param entryName 条目名称
     * @param content 条目内容
     * @throws IOException IO异常
     */
    private void addZipEntry(java.util.zip.ZipOutputStream zipOutputStream, String entryName, String content) throws IOException {
        java.util.zip.ZipEntry entry = new java.util.zip.ZipEntry(entryName);
        zipOutputStream.putNextEntry(entry);
        zipOutputStream.write(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        zipOutputStream.closeEntry();
    }
    
    /**
     * 测试profile脚本是否能正常执行
     * @param profile 要测试的profile
     * @return 是否测试成功
     */
    private boolean testProfileScript(ScriptProfile profile) {
        try {
            // 临时初始化一个脚本引擎
            InputScriptEngine tempEngine = new JsInputScriptEngine(context);
            tempEngine.init();
            
            // 加载脚本
            boolean loadSuccess = tempEngine.loadScript(profile.getScriptCode());
            if (!loadSuccess) {
                Log.e(TAG, "Script load failed: " + tempEngine.getLastError());
                tempEngine.shutdown();
                return false;
            }
            
            // 执行一次空RawInput帧的update
            RawInput testInput = new RawInput();
            InputState testOutput = new InputState();
            boolean updateSuccess = tempEngine.update(testInput, testOutput);
            
            // 清理资源
            tempEngine.shutdown();
            
            return updateSuccess;
        } catch (Exception e) {
            Log.e(TAG, "Error testing profile script: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 根据名称查找profile
     * @param name profile名称
     * @return 找到的profile，未找到返回null
     */
    private ScriptProfile findProfileByName(String name) {
        for (ScriptProfile profile : availableProfiles) {
            if (profile.getName().equals(name)) {
                return profile;
            }
        }
        return null;
    }
    
    /**
     * 获取Profile的校验和
     * @param profile Profile对象
     * @return 校验和字符串
     */
    private String getProfileChecksum(ScriptProfile profile) {
        // TODO: 实现Profile校验和计算
        return profile.getId();
    }
    
    /**
     * 获取当前配置文件
     * @return 当前配置文件
     */
    public ScriptProfile getCurrentProfile() {
        return currentProfile.get();
    }
    
    /**
     * 获取可用的配置文件列表
     * @return 配置文件列表
     */
    public List<ScriptProfile> getAvailableProfiles() {
        return new ArrayList<>(availableProfiles);
    }
    
    /**
     * 导入配置文件
     * @param profileJson 配置文件JSON
     * @param scriptCode 脚本代码
     * @return 导入的配置文件
     */
    public ScriptProfile importProfile(String profileJson, String scriptCode) {
        try {
            JSONObject jsonObject = new JSONObject(profileJson);
            ScriptProfile profile = new ScriptProfile(
                jsonObject.getString("name"),
                jsonObject.getString("version"),
                jsonObject.getString("author"),
                jsonObject.getString("entry"),
                scriptCode
            );
            
            // 设置ID
            if (jsonObject.has("id")) {
                profile.setId(jsonObject.getString("id"));
            }
            
            // 添加到可用配置文件列表
            availableProfiles.add(profile);
            
            return profile;
        } catch (JSONException e) {
            Log.e(TAG, "Error importing profile: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 导出当前配置文件
     * @return 配置文件JSON
     */
    public String exportCurrentProfile() {
        ScriptProfile profile = currentProfile.get();
        if (profile == null) {
            return null;
        }
        
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("id", profile.getId());
            jsonObject.put("name", profile.getName());
            jsonObject.put("version", profile.getVersion());
            jsonObject.put("author", profile.getAuthor());
            jsonObject.put("entry", profile.getEntryPoint());
            jsonObject.put("description", profile.getDescription());
            
            return jsonObject.toString(2);
        } catch (JSONException e) {
            Log.e(TAG, "Error exporting profile: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 检查是否需要回滚
     * @return 是否需要回滚
     */
    public boolean needRollback() {
        return scriptEngine.getState() == InputScriptEngine.EngineState.ERROR;
    }
    
    /**
     * 自动回滚（如果需要）
     * 约定：
     * - 当脚本引擎处于错误状态时自动回滚
     * - 回滚行为符合rollbackProfile()的约定
     * 
     * @return 是否回滚成功
     */
    public synchronized boolean autoRollback() {
        if (needRollback()) {
            Log.w(TAG, "Auto-rolling back to previous profile due to script error");
            return rollbackProfile();
        }
        return true;
    }
    
    /**
     * 卸载当前配置文件
     * 约定：
     * - 强制释放所有按键
     * - 清理相关资源
     * - 确保状态一致性
     */
    public synchronized void unloadCurrentProfile() {
        Log.d(TAG, "Unloading current profile");
        
        // 强制释放所有按键
        clearAllKeys();
        
        // 重置脚本引擎
        try {
            scriptEngine.reset();
        } catch (Exception e) {
            Log.e(TAG, "Error resetting script engine: " + e.getMessage());
        }
        
        // 清空当前和上一个Profile
        previousProfile.set(null);
        currentProfile.set(null);
        
        Log.d(TAG, "Current profile unloaded successfully");
    }
}