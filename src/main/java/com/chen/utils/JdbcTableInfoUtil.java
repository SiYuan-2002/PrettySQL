package com.chen.utils;

import com.chen.constant.DataSourceConstants;
import com.chen.entity.ColumnMeta;
import com.chen.entity.DbConfig;

import java.sql.*;
import java.util.*;

import static com.chen.utils.DbConfigUtil.parseDbType;

/**
 * JDBC 工具类，用于获取指定表的字段元数据信息
 * 包括字段名称、类型、是否主键和备注信息
 * <p>
 * 依赖 MySQL 数据库驱动：com.mysql.cj.jdbc.Driver
 *
 * @author czh
 * @version 1.0
 * @date 2025/6/12 14:44
 */
public class JdbcTableInfoUtil {

    private static final Map<String, String> DRIVER_MAP = Map.of(
            DataSourceConstants.DB_TYPE_MYSQL, DataSourceConstants.MYSQL_DRIVER,
            DataSourceConstants.DB_TYPE_ORACLE, DataSourceConstants.ORACLE_DRIVER,
            DataSourceConstants.DB_TYPE_SQLSERVER, DataSourceConstants.SQLSERVER_DRIVER,
            DataSourceConstants.DB_TYPE_POSTGRESQL, DataSourceConstants.POSTGRESQL_DRIVER
            // 可继续扩展
    );

    /**
     * 获取指定表的字段元数据列表
     *
     * @param dbConfig  数据库连接配置
     * @param tableName 表名
     * @return 字段元数据列表（ColumnMeta）
     * @throws ClassNotFoundException 如果 JDBC 驱动未加载
     */
    public static List<ColumnMeta> getTableColumnsFromMySQL(DbConfig dbConfig, String tableName) {
        try (Connection conn = DataSourceManager.getDataSource(dbConfig).getConnection()) {

            DatabaseMetaData meta = conn.getMetaData();

            String catalog = null;
            String url = dbConfig.getUrl();
            if (url != null) {
                int idx1 = url.indexOf("/", "jdbc:mysql://".length());
                int idx2 = url.indexOf("?", idx1);
                if (idx1 != -1) {
                    if (idx2 != -1) {
                        catalog = url.substring(idx1 + 1, idx2);
                    } else {
                        catalog = url.substring(idx1 + 1);
                    }
                }
            }

            // 获取表的主键字段集合
            Set<String> pkSet = new HashSet<>();
            try (ResultSet pkRs = meta.getPrimaryKeys(catalog, null, tableName)) {
                while (pkRs.next()) {
                    pkSet.add(pkRs.getString("COLUMN_NAME"));
                }
            }

            // 获取表的字段信息
            List<ColumnMeta> columns = new ArrayList<>();
            try (ResultSet rs = meta.getColumns(catalog, null, tableName, null)) {
                while (rs.next()) {
                    String name = rs.getString("COLUMN_NAME");    // 字段名称
                    String type = rs.getString("TYPE_NAME");      // 字段类型
                    String remark = rs.getString("REMARKS");      // 字段注释
                    boolean pk = pkSet.contains(name);            // 是否为主键
                    columns.add(new ColumnMeta(name, type, pk, remark));
                }
            }

            return columns;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取 SQL Server 表字段元数据
     *
     * @param dbConfig  数据库配置
     * @param tableName 表名
     * @return 字段元数据列表
     */
    public static List<ColumnMeta> getTableColumnsFromSqlServer(DbConfig dbConfig, String tableName) {
        try (Connection conn = DataSourceManager.getDataSource(dbConfig).getConnection()) {

            DatabaseMetaData meta = conn.getMetaData();

            // 获取表主键字段集合
            Set<String> pkSet = new HashSet<>();
            try (ResultSet pkRs = meta.getPrimaryKeys(null, null, tableName)) {
                while (pkRs.next()) {
                    pkSet.add(pkRs.getString("COLUMN_NAME"));
                }
            }

            // 获取字段信息
            List<ColumnMeta> columns = new ArrayList<>();
            try (ResultSet rs = meta.getColumns(null, null, tableName, null)) {
                while (rs.next()) {
                    String name = rs.getString("COLUMN_NAME");      // 字段名称
                    String type = rs.getString("TYPE_NAME");        // 字段类型
                    String remark = rs.getString("REMARKS");        // 字段注释（SQL Server 有时需要额外查 sys.extended_properties）
                    boolean pk = pkSet.contains(name);              // 是否主键
                    columns.add(new ColumnMeta(name, type, pk, remark));
                }
            }

            return columns;

        } catch (SQLException e) {
            throw new RuntimeException("获取 SQL Server 字段失败: " + e.getMessage(), e);
        }
    }


    /**
     * 获取 Oracle 表字段元数据
     *
     * @param dbConfig  数据库配置
     * @param tableName 表名
     * @return 字段元数据列表
     */
    public static List<ColumnMeta> getTableColumnsFromOracle(DbConfig dbConfig, String tableName) {
        List<ColumnMeta> columns = new ArrayList<>();
        Set<String> pkSet = new HashSet<>();

        try (Connection conn = DataSourceManager.getDataSource(dbConfig).getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            String schema = dbConfig.getUsername().toUpperCase();

            // 获取主键
            try (ResultSet pkRs = meta.getPrimaryKeys(null, schema, tableName.toUpperCase())) {
                while (pkRs.next()) {
                    pkSet.add(pkRs.getString("COLUMN_NAME"));
                }
            }

            // 获取列及类型
            String columnSql = "SELECT col.COLUMN_NAME, col.DATA_TYPE, com.COMMENTS " +
                    "FROM ALL_TAB_COLUMNS col " +
                    "LEFT JOIN ALL_COL_COMMENTS com " +
                    "ON col.OWNER = com.OWNER AND col.TABLE_NAME = com.TABLE_NAME AND col.COLUMN_NAME = com.COLUMN_NAME " +
                    "WHERE col.OWNER = ? AND col.TABLE_NAME = ?";

            try (PreparedStatement ps = conn.prepareStatement(columnSql)) {
                ps.setString(1, schema);
                ps.setString(2, tableName.toUpperCase());

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String name = rs.getString("COLUMN_NAME");
                        String type = rs.getString("DATA_TYPE");
                        String remark = rs.getString("COMMENTS");
                        boolean pk = pkSet.contains(name);
                        columns.add(new ColumnMeta(name, type, pk, remark));
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return columns;
    }


    /**
     * 获取指定表的字段元数据
     *
     * @param dbConfig  数据库连接配置
     * @param tableName 表名
     * @return 字段元数据列表
     * @throws Exception 如果获取失败
     */
    public static List<ColumnMeta> getTableColumns(DbConfig dbConfig, String tableName) throws Exception {
        return ColumnMetaUtils.getTableColumns(dbConfig, tableName);
    }



    /**
     * 检查数据库连接是否有效
     *
     * @param dbConfig 数据库连接配置
     * @return true 表示连接成功，false 表示连接失败
     */
    public static boolean testConnection(DbConfig dbConfig) throws Exception {
        String dbType = parseDbType(dbConfig.getUrl());
        String driverClass = Optional.ofNullable(DRIVER_MAP.get(dbType))
                .orElseThrow(() -> new RuntimeException("不支持的数据库类型: " + dbType));
        Class.forName(driverClass);

        try (Connection conn = DriverManager.getConnection(
                dbConfig.getUrl(),
                dbConfig.getUsername(),
                dbConfig.getPassword())) {
            return conn != null && !conn.isClosed();
        }
    }


}
