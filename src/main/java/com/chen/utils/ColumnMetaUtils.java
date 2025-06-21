package com.chen.utils;

import com.chen.entity.ColumnMeta;
import com.chen.entity.DbConfig;
import com.chen.entity.TableMeta;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import static com.chen.constant.DataSourceConstants.*;

/**
 * 数据库字段元数据工具类，用于根据数据库类型动态获取表字段信息。
 * 支持 Oracle、MySQL、SQL Server 等数据库类型。
 * 使用策略模式（Map + BiFunction）来优雅地分发处理逻辑。
 *
 * @author czh
 * @version 1.0
 * @since 2025/6/17
 */
public class ColumnMetaUtils {

    /**
     * 数据库类型到字段提取方法的映射。
     * Key 为数据库类型常量，Value 为提取逻辑函数。
     */
    private static final Map<String, BiFunction<DbConfig, String, TableMeta>> DB_TYPE_TO_HANDLER = Map.of(
            DB_TYPE_MYSQL, ColumnMetaUtils::getTableColumnsFromMySQL,
            DB_TYPE_ORACLE, ColumnMetaUtils::getTableColumnsFromOracle,
            DB_TYPE_SQLSERVER, ColumnMetaUtils::getTableColumnsFromSqlServer
    );

    /**
     * 根据数据库配置和表名获取字段元数据列表。
     * 会根据数据库类型自动调用对应的处理方法。
     *
     * @param dbConfig  数据库连接配置（包含 URL、用户名、密码等）
     * @param tableName 表名
     * @return 字段元数据列表
     * @throws RuntimeException 如果不支持该数据库类型
     */
    public static TableMeta getTableColumns(DbConfig dbConfig, String tableName) {
        String dbType = DbConfigUtil.parseDbType(dbConfig.getUrl());
        BiFunction<DbConfig, String, TableMeta> handler = DB_TYPE_TO_HANDLER.get(dbType);
        if (handler == null) {
            throw new RuntimeException("不支持的数据库类型: " + dbType);
        }
        return handler.apply(dbConfig, tableName);
    }

    /**
     * 获取 Oracle 数据库表字段元数据。
     *
     * @param dbConfig  数据库连接配置
     * @param tableName 表名
     * @return 字段元数据列表
     */
    private static TableMeta getTableColumnsFromOracle(DbConfig dbConfig, String tableName) {
        return JdbcTableInfoUtil.getTableColumnsFromOracle(dbConfig, tableName);
    }

    /**
     * 获取 MySQL 数据库表字段元数据。
     *
     * @param dbConfig  数据库连接配置
     * @param tableName 表名
     * @return 字段元数据列表
     */
    private static TableMeta getTableColumnsFromMySQL(DbConfig dbConfig, String tableName) {
        return JdbcTableInfoUtil.getTableMetaFromMySQL(dbConfig, tableName);
    }

    /**
     * 获取 SQL Server 数据库表字段元数据。
     *
     * @param dbConfig  数据库连接配置
     * @param tableName 表名
     * @return 字段元数据列表
     */
    private static TableMeta getTableColumnsFromSqlServer(DbConfig dbConfig, String tableName) {
        return JdbcTableInfoUtil.getTableColumnsFromSqlServer(dbConfig, tableName);
    }
}
