package com.chen.constant;

/**
 * @author czh
 * @version 1.0
 * @description: 数据源常量类
 * @date 2025/6/14 10:17
 */
public class DataSourceConstants {

    // ================== 驱动类名 ==================
    public static final String MYSQL_DRIVER = "com.mysql.cj.jdbc.Driver";
    public static final String ORACLE_DRIVER = "oracle.jdbc.OracleDriver";
    public static final String SQLSERVER_DRIVER = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
    public static final String POSTGRESQL_DRIVER = "org.postgresql.Driver";
    public static final String SQLITE_DRIVER = "org.sqlite.JDBC";
    public static final String H2_DRIVER = "org.h2.Driver";
    public static final String DM_DRIVER = "dm.jdbc.driver.DmDriver"; // 达梦
    public static final String KINGBASE_DRIVER = "com.kingbase8.Driver"; // 人大金仓
    public static final String DB2_DRIVER = "com.ibm.db2.jcc.DB2Driver";

    // ================== 连接池配置 ==================
    public static final int MAX_POOL_SIZE = 5;              // 最大连接数
    public static final int MIN_IDLE = 1;                   // 最小空闲连接数
    public static final long CONNECTION_TIMEOUT = 5000L;    // 获取连接超时时间（ms）
    public static final long IDLE_TIMEOUT = 30000L;         // 空闲连接超时时间（ms）
    public static final long MAX_LIFETIME = 60000L;         // 最大连接生存时间（ms）

    // ================== 支持的数据库类型 ==================
    public static final String DB_TYPE_MYSQL = "mysql";
    public static final String DB_TYPE_ORACLE = "oracle";
    public static final String DB_TYPE_SQLSERVER = "sqlserver";
    public static final String DB_TYPE_POSTGRESQL = "postgresql";
    public static final String DB_TYPE_SQLITE = "sqlite";
    public static final String DB_TYPE_H2 = "h2";
    public static final String DB_TYPE_DM = "dm";
    public static final String DB_TYPE_KINGBASE = "kingbase";
    public static final String DB_TYPE_DB2 = "db2";
}
