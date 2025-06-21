package com.chen.utils;

import com.chen.constant.DataSourceConstants;
import com.chen.entity.ColumnMeta;
import com.chen.entity.DbConfig;
import com.chen.entity.TableMeta;

import java.sql.*;
import java.util.*;

import static com.chen.utils.DbConfigUtil.parseDbType;
import static java.sql.DriverManager.getConnection;

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
    public static TableMeta getTableMetaFromMySQL(DbConfig dbConfig, String tableName) {
        try (Connection conn = DataSourceManager.getDataSource(dbConfig).getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();

            String catalog = null;
            String url = dbConfig.getUrl();
            if (url != null) {
                int idx1 = url.indexOf("/", "jdbc:mysql://".length());
                int idx2 = url.indexOf("?", idx1);
                if (idx1 != -1) {
                    catalog = (idx2 != -1) ? url.substring(idx1 + 1, idx2) : url.substring(idx1 + 1);
                }
            }

            // ✅ 获取表注释（表备注）
            String tableComment = tableName;
            String commentSql = "SELECT TABLE_COMMENT FROM information_schema.TABLES WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?";
            try (PreparedStatement ps = conn.prepareStatement(commentSql)) {
                ps.setString(1, catalog);
                ps.setString(2, tableName);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String comment = rs.getString("TABLE_COMMENT");
                        if (comment != null && !comment.isBlank()) {
                            tableComment = comment;
                        }
                    }
                }
            }

            // ✅ 获取主键字段
            Set<String> pkSet = new HashSet<>();
            try (ResultSet pkRs = meta.getPrimaryKeys(catalog, null, tableName)) {
                while (pkRs.next()) {
                    pkSet.add(pkRs.getString("COLUMN_NAME"));
                }
            }

            // ✅ 获取索引字段
            Set<String> indexSet = new HashSet<>();
            try (ResultSet idxRs = meta.getIndexInfo(catalog, null, tableName, false, false)) {
                while (idxRs.next()) {
                    String colName = idxRs.getString("COLUMN_NAME");
                    if (colName != null) {
                        indexSet.add(colName);
                    }
                }
            }

            // ✅ 获取字段列表
            List<ColumnMeta> columns = new ArrayList<>();
            try (ResultSet rs = meta.getColumns(catalog, null, tableName, null)) {
                while (rs.next()) {
                    String name = rs.getString("COLUMN_NAME");
                    String type = rs.getString("TYPE_NAME");
                    String remark = rs.getString("REMARKS");
                    boolean pk = pkSet.contains(name);
                    boolean idx = indexSet.contains(name);
                    columns.add(new ColumnMeta(name, type, pk, idx, remark));
                }
            }

            return new TableMeta(tableName, tableComment, columns);

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
    public static TableMeta getTableColumnsFromSqlServer(DbConfig dbConfig, String tableName) {
        try (Connection conn = DataSourceManager.getDataSource(dbConfig).getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();

            // 获取当前数据库名（Catalog）
            String catalog = conn.getCatalog();

            // 表注释
            String tableComment = tableName;
            String commentSql = "SELECT TABLE_NAME, TABLE_SCHEMA, TABLE_CATALOG, TABLE_TYPE, REMARKS " +
                    "FROM INFORMATION_SCHEMA.TABLES " +
                    "WHERE TABLE_CATALOG = ? AND TABLE_NAME = ?";
            try (PreparedStatement ps = conn.prepareStatement(commentSql)) {
                ps.setString(1, catalog);
                ps.setString(2, tableName);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String comment = rs.getString("REMARKS");
                        if (comment != null && !comment.isBlank()) {
                            tableComment = comment;
                        }
                    }
                }
            }

            // 主键集合
            Set<String> pkSet = new HashSet<>();
            try (ResultSet pkRs = meta.getPrimaryKeys(catalog, null, tableName)) {
                while (pkRs.next()) {
                    pkSet.add(pkRs.getString("COLUMN_NAME"));
                }
            }

            // 索引字段集合
            Set<String> indexSet = new HashSet<>();
            try (ResultSet idxRs = meta.getIndexInfo(catalog, null, tableName, false, false)) {
                while (idxRs.next()) {
                    String colName = idxRs.getString("COLUMN_NAME");
                    if (colName != null) {
                        indexSet.add(colName);
                    }
                }
            }

            // 字段信息
            List<ColumnMeta> columns = new ArrayList<>();
            try (ResultSet rs = meta.getColumns(catalog, null, tableName, null)) {
                while (rs.next()) {
                    String name = rs.getString("COLUMN_NAME");
                    String type = rs.getString("TYPE_NAME");
                    String remark = rs.getString("REMARKS");
                    boolean pk = pkSet.contains(name);
                    boolean idx = indexSet.contains(name);
                    columns.add(new ColumnMeta(name, type, pk, idx, remark));
                }
            }

            return new TableMeta(tableName, tableComment, columns);
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
    public static TableMeta getTableColumnsFromOracle(DbConfig dbConfig, String tableName) {
        List<ColumnMeta> columns = new ArrayList<>();
        Set<String> pkSet = new HashSet<>();
        Set<String> indexSet = new HashSet<>();
        String schema = dbConfig.getUsername().toUpperCase();
        String upperTableName = tableName.toUpperCase();
        String tableComment = tableName;

        try (Connection conn = DataSourceManager.getDataSource(dbConfig).getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();

            // 表注释
            String commentSql = "SELECT COMMENTS FROM ALL_TAB_COMMENTS WHERE OWNER = ? AND TABLE_NAME = ?";
            try (PreparedStatement ps = conn.prepareStatement(commentSql)) {
                ps.setString(1, schema);
                ps.setString(2, upperTableName);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String comment = rs.getString("COMMENTS");
                        if (comment != null && !comment.isBlank()) {
                            tableComment = comment;
                        }
                    }
                }
            }

            // 主键字段
            try (ResultSet pkRs = meta.getPrimaryKeys(null, schema, upperTableName)) {
                while (pkRs.next()) {
                    pkSet.add(pkRs.getString("COLUMN_NAME"));
                }
            }

            // 索引字段
            try (ResultSet idxRs = meta.getIndexInfo(null, schema, upperTableName, false, false)) {
                while (idxRs.next()) {
                    String colName = idxRs.getString("COLUMN_NAME");
                    if (colName != null) {
                        indexSet.add(colName);
                    }
                }
            }

            // 字段信息（带注释）
            String columnSql = "SELECT col.COLUMN_NAME, col.DATA_TYPE, com.COMMENTS " +
                    "FROM ALL_TAB_COLUMNS col " +
                    "LEFT JOIN ALL_COL_COMMENTS com " +
                    "ON col.OWNER = com.OWNER AND col.TABLE_NAME = com.TABLE_NAME AND col.COLUMN_NAME = com.COLUMN_NAME " +
                    "WHERE col.OWNER = ? AND col.TABLE_NAME = ?";
            try (PreparedStatement ps = conn.prepareStatement(columnSql)) {
                ps.setString(1, schema);
                ps.setString(2, upperTableName);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String name = rs.getString("COLUMN_NAME");
                        String type = rs.getString("DATA_TYPE");
                        String remark = rs.getString("COMMENTS");
                        boolean pk = pkSet.contains(name);
                        boolean idx = indexSet.contains(name);
                        columns.add(new ColumnMeta(name, type, pk, idx, remark));
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("获取 Oracle 字段失败: " + e.getMessage(), e);
        }

        return new TableMeta(tableName, tableComment, columns);
    }



    /**
     * 获取指定表的字段元数据
     *
     * @param dbConfig  数据库连接配置
     * @param tableName 表名
     * @return 字段元数据列表
     * @throws Exception 如果获取失败
     */
    public static TableMeta getTableColumns(DbConfig dbConfig, String tableName) throws Exception {
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

        try (Connection conn = getConnection(
                dbConfig.getUrl(),
                dbConfig.getUsername(),
                dbConfig.getPassword())) {
            return conn != null && !conn.isClosed();
        }
    }

    /**
     * 执行 EXPLAIN 查询，并返回每行结果映射列表
     * 只支持 MySQL 的 EXPLAIN
     *
     * @param config 数据库连接配置
     * @param sql    要执行 EXPLAIN 的 SQL 查询（必须是 SELECT）
     * @return 结果集每行对应的 Map 集合列表
     * @throws Exception 执行异常
     */
    public static List<Map<String, Object>> explainSql(DbConfig config, String sql) throws Exception {
        if (sql == null || !sql.trim().toLowerCase().startsWith("select")) {
            throw new IllegalArgumentException("只支持 SELECT 语句的执行计划");
        }

        String explainSql = "EXPLAIN " + sql;

        try (Connection conn = DataSourceManager.getDataSource(config).getConnection()) {
             PreparedStatement ps = conn.prepareStatement(explainSql);
             ResultSet rs = ps.executeQuery();

            ResultSetMetaData metaData = rs.getMetaData();
            int colCount = metaData.getColumnCount();
            List<Map<String, Object>> list = new ArrayList<>();

            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= colCount; i++) {
                    String colName = metaData.getColumnLabel(i);
                    Object value = rs.getObject(i);
                    row.put(colName, value);
                }
                list.add(row);
            }

            return list;
        } catch (SQLException e) {
            throw new Exception("执行 EXPLAIN 失败：" + e.getMessage(), e);
        }
    }



}
