package com.linecat.wmmtcontroller.model;

/**
 * 连接信息数据模型类
 * 用于存储客户端连接的地址和端口等信息
 */
public class ConnectionInfo {
    // 主键ID
    private long id;
    // 连接地址
    private String address;
    // 端口号
    private int port;
    // 是否使用TLS
    private boolean useTls;
    // 是否为默认连接
    private boolean isDefault;
    // 创建时间
    private long createTime;
    
    /**
     * 默认构造方法
     */
    public ConnectionInfo() {
    }
    
    /**
     * 构造方法
     * @param address 连接地址
     * @param port 端口号
     */
    public ConnectionInfo(String address, int port) {
        this.address = address;
        this.port = port;
        this.useTls = false;
        this.isDefault = false;
        this.createTime = System.currentTimeMillis();
    }
    
    /**
     * 获取主键ID
     * @return 主键ID
     */
    public long getId() {
        return id;
    }
    
    /**
     * 设置主键ID
     * @param id 主键ID
     */
    public void setId(long id) {
        this.id = id;
    }
    
    /**
     * 获取连接地址
     * @return 连接地址
     */
    public String getAddress() {
        return address;
    }
    
    /**
     * 设置连接地址
     * @param address 连接地址
     */
    public void setAddress(String address) {
        this.address = address;
    }
    
    /**
     * 获取端口号
     * @return 端口号
     */
    public int getPort() {
        return port;
    }
    
    /**
     * 设置端口号
     * @param port 端口号
     */
    public void setPort(int port) {
        this.port = port;
    }
    
    /**
     * 是否使用TLS
     * @return 是否使用TLS
     */
    public boolean isUseTls() {
        return useTls;
    }
    
    /**
     * 设置是否使用TLS
     * @param useTls 是否使用TLS
     */
    public void setUseTls(boolean useTls) {
        this.useTls = useTls;
    }
    
    /**
     * 是否为默认连接
     * @return 是否为默认连接
     */
    public boolean isDefault() {
        return isDefault;
    }
    
    /**
     * 设置是否为默认连接
     * @param aDefault 是否为默认连接
     */
    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }
    
    /**
     * 获取创建时间
     * @return 创建时间
     */
    public long getCreateTime() {
        return createTime;
    }
    
    /**
     * 设置创建时间
     * @param createTime 创建时间
     */
    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }
    
    @Override
    public String toString() {
        return "ConnectionInfo{" +
                "id=" + id +
                ", address='" + address + '\'' +
                ", port=" + port +
                ", useTls=" + useTls +
                ", isDefault=" + isDefault +
                ", createTime=" + createTime +
                '}';
    }
}