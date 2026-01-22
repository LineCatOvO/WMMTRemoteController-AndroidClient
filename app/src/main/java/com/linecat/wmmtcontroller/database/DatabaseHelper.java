package com.linecat.wmmtcontroller.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.linecat.wmmtcontroller.model.ConnectionInfo;

/**
 * 数据库助手类，用于管理本地SQLite数据库
 * 存储连接信息等配置数据
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "DatabaseHelper";
    
    // 数据库配置
    private static final String DATABASE_NAME = "wmmt_controller.db";
    private static final int DATABASE_VERSION = 1;
    
    // 连接信息表
    private static final String TABLE_CONNECTION_INFO = "connection_info";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_ADDRESS = "address";
    private static final String COLUMN_PORT = "port";
    private static final String COLUMN_USE_TLS = "use_tls";
    private static final String COLUMN_IS_DEFAULT = "is_default";
    private static final String COLUMN_CREATE_TIME = "create_time";
    
    // 创建连接信息表的SQL语句
    private static final String CREATE_TABLE_CONNECTION_INFO = "CREATE TABLE " + TABLE_CONNECTION_INFO + "(" +
            COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_ADDRESS + " TEXT NOT NULL, " +
            COLUMN_PORT + " INTEGER NOT NULL, " +
            COLUMN_USE_TLS + " INTEGER NOT NULL DEFAULT 0, " +
            COLUMN_IS_DEFAULT + " INTEGER NOT NULL DEFAULT 0, " +
            COLUMN_CREATE_TIME + " INTEGER NOT NULL" +
            ")";
    
    /**
     * 构造方法
     * @param context 上下文
     */
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {
        // 创建连接信息表
        Log.d(TAG, "Creating table: " + TABLE_CONNECTION_INFO);
        db.execSQL(CREATE_TABLE_CONNECTION_INFO);
        
        // 插入默认连接信息
        insertDefaultConnectionInfo(db);
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // 数据库升级逻辑
        Log.d(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CONNECTION_INFO);
        onCreate(db);
    }
    
    /**
     * 插入默认连接信息
     * @param db SQLiteDatabase实例
     */
    private void insertDefaultConnectionInfo(SQLiteDatabase db) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_ADDRESS, "localhost");
        values.put(COLUMN_PORT, 8080);
        values.put(COLUMN_USE_TLS, 0);
        values.put(COLUMN_IS_DEFAULT, 1);
        values.put(COLUMN_CREATE_TIME, System.currentTimeMillis());
        
        db.insert(TABLE_CONNECTION_INFO, null, values);
    }
    
    /**
     * 获取默认连接信息
     * @return 默认连接信息
     */
    public ConnectionInfo getDefaultConnectionInfo() {
        SQLiteDatabase db = this.getReadableDatabase();
        ConnectionInfo connectionInfo = null;
        
        Cursor cursor = db.query(
                TABLE_CONNECTION_INFO,
                null,
                COLUMN_IS_DEFAULT + " = ?",
                new String[]{"1"},
                null,
                null,
                null,
                "1"
        );
        
        if (cursor != null && cursor.moveToFirst()) {
            connectionInfo = cursorToConnectionInfo(cursor);
            cursor.close();
        }
        
        db.close();
        return connectionInfo;
    }
    
    /**
     * 获取所有连接信息
     * @return 连接信息列表
     */
    public java.util.List<ConnectionInfo> getAllConnectionInfos() {
        java.util.List<ConnectionInfo> connectionInfos = new java.util.ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        Cursor cursor = db.query(
                TABLE_CONNECTION_INFO,
                null,
                null,
                null,
                null,
                null,
                COLUMN_CREATE_TIME + " DESC"
        );
        
        if (cursor != null) {
            while (cursor.moveToNext()) {
                connectionInfos.add(cursorToConnectionInfo(cursor));
            }
            cursor.close();
        }
        
        db.close();
        return connectionInfos;
    }
    
    /**
     * 保存连接信息
     * @param connectionInfo 连接信息
     * @return 保存成功的ID
     */
    public long saveConnectionInfo(ConnectionInfo connectionInfo) {
        SQLiteDatabase db = this.getWritableDatabase();
        long id = -1;
        
        try {
            db.beginTransaction();
            
            // 如果是默认连接，先取消其他默认连接
            if (connectionInfo.isDefault()) {
                ContentValues defaultValues = new ContentValues();
                defaultValues.put(COLUMN_IS_DEFAULT, 0);
                db.update(TABLE_CONNECTION_INFO, defaultValues, null, null);
            }
            
            // 保存连接信息
            ContentValues values = new ContentValues();
            values.put(COLUMN_ADDRESS, connectionInfo.getAddress());
            values.put(COLUMN_PORT, connectionInfo.getPort());
            values.put(COLUMN_USE_TLS, connectionInfo.isUseTls() ? 1 : 0);
            values.put(COLUMN_IS_DEFAULT, connectionInfo.isDefault() ? 1 : 0);
            
            if (connectionInfo.getId() > 0) {
                // 更新现有记录
                db.update(
                        TABLE_CONNECTION_INFO,
                        values,
                        COLUMN_ID + " = ?",
                        new String[]{String.valueOf(connectionInfo.getId())}
                );
                id = connectionInfo.getId();
            } else {
                // 插入新记录
                values.put(COLUMN_CREATE_TIME, System.currentTimeMillis());
                id = db.insert(TABLE_CONNECTION_INFO, null, values);
            }
            
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "Error saving connection info: " + e.getMessage(), e);
        } finally {
            db.endTransaction();
            db.close();
        }
        
        return id;
    }
    
    /**
     * 删除连接信息
     * @param id 连接信息ID
     * @return 是否删除成功
     */
    public boolean deleteConnectionInfo(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        boolean result = false;
        
        try {
            int rowsDeleted = db.delete(
                    TABLE_CONNECTION_INFO,
                    COLUMN_ID + " = ?",
                    new String[]{String.valueOf(id)}
            );
            result = rowsDeleted > 0;
        } catch (Exception e) {
            Log.e(TAG, "Error deleting connection info: " + e.getMessage(), e);
        } finally {
            db.close();
        }
        
        return result;
    }
    
    /**
     * 将Cursor转换为ConnectionInfo对象
     * @param cursor Cursor对象
     * @return ConnectionInfo对象
     */
    private ConnectionInfo cursorToConnectionInfo(Cursor cursor) {
        ConnectionInfo connectionInfo = new ConnectionInfo();
        connectionInfo.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)));
        connectionInfo.setAddress(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ADDRESS)));
        connectionInfo.setPort(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PORT)));
        connectionInfo.setUseTls(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_USE_TLS)) == 1);
        connectionInfo.setDefault(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_DEFAULT)) == 1);
        connectionInfo.setCreateTime(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATE_TIME)));
        
        return connectionInfo;
    }
    
    /**
     * 获取WebSocket URL
     * @param connectionInfo 连接信息
     * @return WebSocket URL
     */
    public static String getWebSocketUrl(ConnectionInfo connectionInfo) {
        String protocol = connectionInfo.isUseTls() ? "wss://" : "ws://";
        return protocol + connectionInfo.getAddress() + ":" + connectionInfo.getPort() + "/ws/input";
    }
}