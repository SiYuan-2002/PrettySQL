package com.chen.utils;

import com.chen.constant.DataSourceConstants;
import com.chen.entity.DbConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.chen.utils.DbConfigUtil.parseDbType;

/**
 * 数据源管理类，用于统一管理数据库连接池实例。
 * 基于 HikariCP 实现数据源的创建、缓存与复用。
 * 支持 MySQL、Oracle、SQL Server 等主流数据库。
 */
public class DataSourceManager {

    /**
     * 数据库类型与对应 JDBC 驱动类名的映射。
     */
    private static final Map<String, String> DRIVER_MAP = Map.of(
            DataSourceConstants.DB_TYPE_MYSQL, DataSourceConstants.MYSQL_DRIVER,
            DataSourceConstants.DB_TYPE_ORACLE, DataSourceConstants.ORACLE_DRIVER,
            DataSourceConstants.DB_TYPE_SQLSERVER, DataSourceConstants.SQLSERVER_DRIVER,
            DataSourceConstants.DB_TYPE_POSTGRESQL, DataSourceConstants.POSTGRESQL_DRIVER
    );

    /**
     * 数据源缓存池，key 为 DbConfig，value 为 HikariDataSource。
     * 使用 ConcurrentHashMap 保证线程安全。
     */
    private static final Map<DbConfig, HikariDataSource> cache = new ConcurrentHashMap<>();

    /**
     * 获取指定配置的 DataSource，如果缓存中不存在则创建新的。
     *
     * @param config 数据库连接配置
     * @return 对应的数据源对象（HikariDataSource 实现）
     */
    public static DataSource getDataSource(DbConfig config) {
        return cache.computeIfAbsent(config, cfg -> {
            loadJdbcDriver(cfg.getUrl());
            return createHikariDataSource(cfg);
        });
    }

    /**
     * 根据数据库 URL 加载对应的 JDBC 驱动类。
     *
     * @param url 数据库连接 URL
     * @throws RuntimeException 如果不支持该数据库或驱动加载失败
     */
    private static void loadJdbcDriver(String url) {
        String dbType = parseDbType(url);
        String driverClass = DRIVER_MAP.get(dbType);
        if (driverClass == null) {
            throw new RuntimeException("不支持的数据库类型: " + dbType);
        }
        try {
            Class.forName(driverClass);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("JDBC驱动加载失败: " + e.getMessage(), e);
        }
    }

    /**
     * 创建一个新的 HikariCP 数据源实例。
     *
     * @param cfg 数据库连接配置
     * @return 初始化后的 HikariDataSource 实例
     */
    private static HikariDataSource createHikariDataSource(DbConfig cfg) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(cfg.getUrl());
        hikariConfig.setUsername(cfg.getUsername());
        hikariConfig.setPassword(cfg.getPassword());
        hikariConfig.setMaximumPoolSize(DataSourceConstants.MAX_POOL_SIZE);      // 最大连接数
        hikariConfig.setMinimumIdle(DataSourceConstants.MIN_IDLE);              // 最小空闲连接数
        hikariConfig.setConnectionTimeout(DataSourceConstants.CONNECTION_TIMEOUT); // 获取连接超时时间
        hikariConfig.setIdleTimeout(DataSourceConstants.IDLE_TIMEOUT);          // 空闲连接最大存活时间
        hikariConfig.setMaxLifetime(DataSourceConstants.MAX_LIFETIME);          // 连接最大生命周期
        return new HikariDataSource(hikariConfig);
    }
}
