package com.linecat.wmmtcontroller.input;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * 脚本配置文件类
 * 实现Input Script Runtime Specification中定义的Profile结构
 */
public class ScriptProfile {
    
    // 核心标识信息
    private String id;
    private String name;
    private String version;
    private String author;
    
    // 脚本运行时API版本
    private String engineApiVersion;
    
    // 描述信息
    private String description;
    
    // 脚本信息
    private String entryPoint;
    private String scriptCode;
    
    // 兼容性信息
    private CompatibilityInfo compatibility;
    
    // 依赖信息
    private List<String> dependencies;
    
    // 元数据
    private Date createdAt;
    private Date updatedAt;
    
    /**
     * 构造函数
     * @param name 配置文件名称
     * @param version 版本号
     * @param author 作者
     * @param entryPoint 脚本入口点
     * @param scriptCode 脚本代码
     */
    public ScriptProfile(String name, String version, String author, String entryPoint, String scriptCode) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.version = version;
        this.author = author;
        this.engineApiVersion = "1.0.0"; // 默认API版本
        this.entryPoint = entryPoint;
        this.scriptCode = scriptCode;
        this.createdAt = new Date();
        this.updatedAt = new Date();
        this.compatibility = new CompatibilityInfo();
    }
    
    /**
     * 获取配置文件ID
     * @return 唯一标识符
     */
    public String getId() {
        return id;
    }
    
    /**
     * 设置配置文件ID
     * @param id 唯一标识符
     */
    public void setId(String id) {
        this.id = id;
    }
    
    /**
     * 获取配置文件名称
     * @return 配置文件名称
     */
    public String getName() {
        return name;
    }
    
    /**
     * 设置配置文件名称
     * @param name 配置文件名称
     */
    public void setName(String name) {
        this.name = name;
        this.updatedAt = new Date();
    }
    
    /**
     * 获取版本号
     * @return 版本号（语义化版本）
     */
    public String getVersion() {
        return version;
    }
    
    /**
     * 设置版本号
     * @param version 版本号（语义化版本）
     */
    public void setVersion(String version) {
        this.version = version;
        this.updatedAt = new Date();
    }
    
    /**
     * 获取作者
     * @return 作者名称
     */
    public String getAuthor() {
        return author;
    }
    
    /**
     * 设置作者
     * @param author 作者名称
     */
    public void setAuthor(String author) {
        this.author = author;
        this.updatedAt = new Date();
    }
    
    /**
     * 获取脚本运行时API版本
     * @return API版本号
     */
    public String getEngineApiVersion() {
        return engineApiVersion;
    }
    
    /**
     * 设置脚本运行时API版本
     * @param engineApiVersion API版本号
     */
    public void setEngineApiVersion(String engineApiVersion) {
        this.engineApiVersion = engineApiVersion;
        this.updatedAt = new Date();
    }
    
    /**
     * 获取描述
     * @return 配置文件描述
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * 设置描述
     * @param description 配置文件描述
     */
    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = new Date();
    }
    
    /**
     * 获取脚本入口点
     * @return 脚本入口点
     */
    public String getEntryPoint() {
        return entryPoint;
    }
    
    /**
     * 设置脚本入口点
     * @param entryPoint 脚本入口点
     */
    public void setEntryPoint(String entryPoint) {
        this.entryPoint = entryPoint;
        this.updatedAt = new Date();
    }
    
    /**
     * 获取脚本代码
     * @return 脚本代码
     */
    public String getScriptCode() {
        return scriptCode;
    }
    
    /**
     * 设置脚本代码
     * @param scriptCode 脚本代码
     */
    public void setScriptCode(String scriptCode) {
        this.scriptCode = scriptCode;
        this.updatedAt = new Date();
    }
    
    /**
     * 获取兼容性信息
     * @return 兼容性信息
     */
    public CompatibilityInfo getCompatibility() {
        return compatibility;
    }
    
    /**
     * 设置兼容性信息
     * @param compatibility 兼容性信息
     */
    public void setCompatibility(CompatibilityInfo compatibility) {
        this.compatibility = compatibility;
        this.updatedAt = new Date();
    }
    
    /**
     * 获取依赖列表
     * @return 依赖列表
     */
    public List<String> getDependencies() {
        return dependencies;
    }
    
    /**
     * 设置依赖列表
     * @param dependencies 依赖列表
     */
    public void setDependencies(List<String> dependencies) {
        this.dependencies = dependencies;
        this.updatedAt = new Date();
    }
    
    /**
     * 获取创建时间
     * @return 创建时间
     */
    public Date getCreatedAt() {
        return createdAt;
    }
    
    /**
     * 获取更新时间
     * @return 更新时间
     */
    public Date getUpdatedAt() {
        return updatedAt;
    }
    
    /**
     * 兼容性信息类
     */
    public static class CompatibilityInfo {
        private List<String> supportedGamepads;
        private String minimumAndroidVersion;
        
        /**
         * 获取支持的游戏手柄列表
         * @return 支持的游戏手柄列表
         */
        public List<String> getSupportedGamepads() {
            return supportedGamepads;
        }
        
        /**
         * 设置支持的游戏手柄列表
         * @param supportedGamepads 支持的游戏手柄列表
         */
        public void setSupportedGamepads(List<String> supportedGamepads) {
            this.supportedGamepads = supportedGamepads;
        }
        
        /**
         * 获取最低Android版本要求
         * @return 最低Android版本
         */
        public String getMinimumAndroidVersion() {
            return minimumAndroidVersion;
        }
        
        /**
         * 设置最低Android版本要求
         * @param minimumAndroidVersion 最低Android版本
         */
        public void setMinimumAndroidVersion(String minimumAndroidVersion) {
            this.minimumAndroidVersion = minimumAndroidVersion;
        }
    }
}