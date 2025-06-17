package com.chen.utils;

import com.chen.constant.DataSourceConstants;
import com.chen.constant.DbConstant;

import java.util.Map;

/**
 * 数据库 URL 补全工具类。
 * <p>
 * 不同数据库类型连接 URL 可能需要追加特定的参数（如字符集、时区等）。
 * 本类通过数据库类型自动匹配并返回对应的 URL 补全片段。
 * 例如：
 * - MySQL: ?useSSL=false&characterEncoding=utf8
 * - SQLServer: ;encrypt=true
 * - Oracle: （如不需要补全，可为空字符串）
 * </p>
 * 用法示例：
 * <pre>
 *     String dbType = parseDbType(url);
 *     String fullUrl = url + DataSourceUrlFixer.appendUrlFix(dbType, url);
 * </pre>
 *
 * @author czh
 * @version 1.0
 * @date 2025/6/17 15:00
 */
public class DataSourceUrlFixer {

    /**
     * 数据库类型与对应的 URL 补全片段映射。
     * 用于连接 JDBC 参数时自动拼接标准参数。
     */
    private static final Map<String, String> DB_TYPE_TO_URL_FIX = Map.of(
            DataSourceConstants.DB_TYPE_MYSQL, DbConstant.MYSQL_URL_FIX,
            DataSourceConstants.DB_TYPE_SQLSERVER, DbConstant.SQLSERVER_URL_FIX,
            DataSourceConstants.DB_TYPE_ORACLE, DbConstant.ORACLE_URL_FIX
    );

    /**
     * 根据数据库类型返回对应的 URL 补全字符串。
     * <p>
     * 若该数据库类型无补全片段，返回空字符串。
     * </p>
     *
     * @param dbType 数据库类型（如 "mysql"、"oracle"）
     * @param url    原始数据库连接 URL（未使用，仅预留）
     * @return URL 补全片段（以 `?` 或 `;` 开头）
     */
    public static String appendUrlFix(String dbType, String url) {
        return DB_TYPE_TO_URL_FIX.getOrDefault(dbType, "");
    }
}
