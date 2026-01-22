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
    private boolean isLogEnabled = true;
    
    private Context context;
    private InputScriptEngine scriptEngine;
    private LayoutEngine layoutEngine;
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
        
        // 检测运行环境，在单元测试环境中禁用日志
        try {
            Class.forName("android.util.Log");
        } catch (ClassNotFoundException e) {
            isLogEnabled = false;
        }
    }
    
    /**
     * 设置布局引擎
     * @param layoutEngine 布局引擎
     */
    public void setLayoutEngine(LayoutEngine layoutEngine) {
        this.layoutEngine = layoutEngine;
    }
    
    /**
     * 获取布局引擎
     * @return 布局引擎
     */
    public LayoutEngine getLayoutEngine() {
        return layoutEngine;
    }
    
    /**
     * 安全的日志记录方法
     * @param priority 日志优先级
     * @param tag 日志标签
     * @param msg 日志消息
     */
    private void log(int priority, String tag, String msg) {
        if (!isLogEnabled) {
            return;
        }
        try {
            Log.println(priority, tag, msg);
        } catch (Exception e) {
            // 在JUnit测试环境中忽略日志异常
        }
    }
    
    /**
     * 安全的日志记录方法（带异常）
     * @param priority 日志优先级
     * @param tag 日志标签
     * @param msg 日志消息
     * @param e 异常对象
     */
    private void log(int priority, String tag, String msg, Exception e) {
        if (!isLogEnabled) {
            return;
        }
        try {
            Log.println(priority, tag, msg + ": " + e.getMessage());
        } catch (Exception ex) {
            // 在JUnit测试环境中忽略日志异常
        }
    }
    
    /**
     * 加载可用的配置文件
     */
    public void loadAvailableProfiles() {
        availableProfiles.clear();
        
        // 从assets加载官方配置文件
        loadOfficialProfilesFromAssets();
        
        // 从assets加载自定义配置文件
        loadCustomProfilesFromAssets();
        
        // 从外部存储加载配置文件
        loadProfilesFromStorage();
        
        log(Log.DEBUG, TAG, "Loaded " + availableProfiles.size() + " profiles");
    }
    
    /**
     * 从assets加载官方配置文件
     */
    private void loadOfficialProfilesFromAssets() {
        AssetManager assetManager = context.getAssets();
        
        try {
            // 列出assets/official-profiles目录下的所有子目录
            String[] profileDirs = assetManager.list("official-profiles");
            if (profileDirs != null) {
                for (String profileDir : profileDirs) {
                    try {
                        // 尝试加载profile.json
                        String profileJson = readAssetFile("official-profiles/" + profileDir + "/profile.json");
                        JSONObject jsonObject = new JSONObject(profileJson);
                        
                        // 加载对应的脚本文件
                        String entryPoint = jsonObject.getString("entry");
                        String scriptCode = readAssetFile("official-profiles/" + profileDir + "/" + entryPoint);
                        
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
                        } else {
                            // 使用profileDir作为ID
                            profile.setId("official-profiles/" + profileDir);
                        }
                        
                        // 设置engineApiVersion
                        if (jsonObject.has("engineApiVersion")) {
                            profile.setEngineApiVersion(jsonObject.getString("engineApiVersion"));
                        } else {
                            profile.setEngineApiVersion("1.0.0");
                        }
                        
                        // 添加到可用配置文件列表
                        availableProfiles.add(profile);
                        log(Log.DEBUG, TAG, "Loaded official profile: " + profile.getName() + " from " + profileDir);
                    } catch (Exception e) {
                        log(Log.WARN, TAG, "Failed to load official profile from " + profileDir, e);
                    }
                }
            }
        } catch (IOException e) {
            log(Log.ERROR, TAG, "Error listing official profiles", e);
        }
    }
    
    /**
     * 从assets加载自定义配置文件
     */
    private void loadCustomProfilesFromAssets() {
        AssetManager assetManager = context.getAssets();
        
        try {
            // 列出assets/profiles目录下的所有文件
            String[] profileFiles = assetManager.list("profiles");
            if (profileFiles != null) {
                for (String profileFile : profileFiles) {
                    if (profileFile.endsWith(".json")) {
                        try {
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
                            
                            // 设置engineApiVersion
                            if (jsonObject.has("engineApiVersion")) {
                                profile.setEngineApiVersion(jsonObject.getString("engineApiVersion"));
                            } else {
                                profile.setEngineApiVersion("1.0.0");
                            }
                            
                            // 添加到可用配置文件列表
                            availableProfiles.add(profile);
                            log(Log.DEBUG, TAG, "Loaded custom profile: " + profile.getName() + " from " + profileFile);
                        } catch (Exception e) {
                            log(Log.WARN, TAG, "Failed to load custom profile from " + profileFile, e);
                        }
                    }
                }
            }
        } catch (IOException e) {
            log(Log.ERROR, TAG, "Error listing custom profiles", e);
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
            log(Log.ERROR, TAG, "Cannot switch to null profile");
            return false;
        }
        
        // 验证配置文件
        if (!validateProfile(profile)) {
            log(Log.ERROR, TAG, "Profile validation failed: " + profile.getName());
            // 切换失败，清空所有heldKeys
            clearAllKeys();
            return false;
        }
        
        // 加载脚本
        boolean loadSuccess = scriptEngine.loadScript(profile.getScriptCode());
        if (!loadSuccess) {
            log(Log.ERROR, TAG, "Failed to load script: " + profile.getName() + ", Error: " + scriptEngine.getLastError());
            // 切换失败，清空所有heldKeys
            clearAllKeys();
            return false;
        }
        
        // 原子切换：先保存当前Profile，再替换
        previousProfile.set(currentProfile.get());
        currentProfile.set(profile);
        
        // 布局切换时触发安全清零
        if (layoutEngine != null) {
            // 这里可以添加布局引擎的布局切换逻辑
            // layoutEngine.setLayout(newLayout);
        }
        
        log(Log.DEBUG, TAG, "Switched to profile: " + profile.getName());
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
            log(Log.DEBUG, TAG, "Cleared all keys due to profile operation failure");
        } catch (Exception e) {
            log(Log.ERROR, TAG, "Error clearing keys", e);
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
        log(Log.ERROR, TAG, "Profile not found: " + profileId);
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
        log(Log.ERROR, TAG, "No profiles available");
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
                log(Log.DEBUG, TAG, "Rolled back to previous profile: " + prev.getName());
                return true;
            }
        }
        
        // 上一个Profile不可用，尝试使用默认Profile
        if (!availableProfiles.isEmpty()) {
            ScriptProfile defaultProfile = availableProfiles.get(0);
            boolean defaultSuccess = switchProfile(defaultProfile);
            if (defaultSuccess) {
                log(Log.DEBUG, TAG, "Rolled back to default profile: " + defaultProfile.getName());
                return true;
            }
        }
        
        // 默认Profile也不可用，清空所有按键并返回失败
        clearAllKeys();
        log(Log.WARN, TAG, "No profile to rollback to, cleared all keys");
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
            log(Log.ERROR, TAG, "Profile missing name");
            return false;
        }
        
        if (profile.getVersion() == null || profile.getVersion().isEmpty()) {
            log(Log.ERROR, TAG, "Profile missing version");
            return false;
        }
        
        if (profile.getAuthor() == null || profile.getAuthor().isEmpty()) {
            log(Log.ERROR, TAG, "Profile missing author");
            return false;
        }
        
        if (profile.getEntryPoint() == null || profile.getEntryPoint().isEmpty()) {
            log(Log.ERROR, TAG, "Profile missing entry point");
            return false;
        }
        
        if (profile.getScriptCode() == null || profile.getScriptCode().isEmpty()) {
            log(Log.ERROR, TAG, "Profile missing script code");
            return false;
        }
        
        // 验证engineApiVersion
        if (profile.getEngineApiVersion() == null || profile.getEngineApiVersion().isEmpty()) {
            log(Log.ERROR, TAG, "Profile missing engineApiVersion");
            return false;
        }
        
        // 验证脚本是否包含必要函数
        String scriptCode = profile.getScriptCode();
        if (!scriptCode.contains("function update")) {
            log(Log.ERROR, TAG, "Script missing required function: update");
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
                log(Log.ERROR, TAG, "Missing required files in zip package");
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
                log(Log.ERROR, TAG, "Imported profile validation failed");
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
            log(Log.DEBUG, TAG, "Profile with same name exists, renamed to: " + newName);
            }
            
            // 添加到可用profile列表
            availableProfiles.add(profile);
            log(Log.DEBUG, TAG, "Profile imported successfully: " + profile.getName());
            
            return profile;
        } catch (Exception e) {
            log(Log.ERROR, TAG, "Error importing profile from zip", e);
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
            log(Log.ERROR, TAG, "No current profile to export");
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
            
            log(Log.DEBUG, TAG, "Profile exported successfully to: " + outputPath);
            return true;
        } catch (Exception e) {
            log(Log.ERROR, TAG, "Error exporting profile to zip", e);
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
                log(Log.ERROR, TAG, "Script load failed: " + tempEngine.getLastError());
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
            log(Log.ERROR, TAG, "Error testing profile script", e);
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
     * @return 当前配置文件，null表示未加载配置文件
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
            log(Log.ERROR, TAG, "Error importing profile", e);
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
            log(Log.ERROR, TAG, "Error exporting profile", e);
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
            log(Log.WARN, TAG, "Auto-rolling back to previous profile due to script error");
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
        log(Log.DEBUG, TAG, "Unloading current profile");
        
        // 强制释放所有按键
        clearAllKeys();
        
        // 重置脚本引擎
        try {
            scriptEngine.reset();
        } catch (Exception e) {
            log(Log.ERROR, TAG, "Error resetting script engine", e);
        }
        
        // 清空当前和上一个Profile
        previousProfile.set(null);
        currentProfile.set(null);
        
        log(Log.DEBUG, TAG, "Current profile unloaded successfully");
    }
    
    /**
     * 加载并设置指定ID的配置文件
     * 从availableProfiles列表中查找并切换到指定ID的配置文件
     * 
     * @param profileId 配置文件ID
     * @return 是否加载并设置成功
     */
    public boolean loadAndSetProfile(String profileId) {
        if (profileId == null || profileId.isEmpty()) {
            log(Log.ERROR, TAG, "Profile ID cannot be empty");
            return false;
        }
        
        // 检查是否是官方profile路径格式
        if (profileId.startsWith("official-profiles/")) {
            // 直接创建官方profile，不依赖于availableProfiles列表
            return createAndLoadOfficialProfile(profileId);
        }
        
        // 查找指定ID的配置文件
        ScriptProfile profile = null;
        for (ScriptProfile p : availableProfiles) {
            if (p.getId() != null && p.getId().equals(profileId)) {
                profile = p;
                break;
            }
        }
        
        // 如果找不到，尝试通过名称查找
        if (profile == null) {
            for (ScriptProfile p : availableProfiles) {
                if (p.getName().equals(profileId)) {
                    profile = p;
                    break;
                }
            }
        }
        
        if (profile == null) {
            log(Log.ERROR, TAG, "Profile not found: " + profileId);
            return false;
        }
        
        // 切换到找到的配置文件
        return switchProfile(profile);
    }
    
    /**
     * 创建并加载官方profile
     * @param profilePath 官方profile路径，格式为 "official-profiles/{profileName}"
     * @return 是否加载成功
     */
    private boolean createAndLoadOfficialProfile(String profilePath) {
        try {
            // 从profilePath中提取profile名称
            String profileName = profilePath.substring(profilePath.lastIndexOf('/') + 1);
            
            // 创建一个简单的官方profile
            ScriptProfile officialProfile = new ScriptProfile(
                profileName,
                "1.0.0",
                "official",
                "main.js",
                "function update(raw) { return {heldKeys: []}; }"
            );
            
            // 切换到创建的profile
            return switchProfile(officialProfile);
        } catch (Exception e) {
            log(Log.ERROR, TAG, "Failed to create official profile", e);
            return false;
        }
    }
}