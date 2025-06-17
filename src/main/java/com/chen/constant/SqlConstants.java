package com.chen.constant;

/**
 * @author czh
 * @version 1.0
 * @description: SQL相关常量维护
 * @date 2025/6/14 8:42
 */
public class SqlConstants {

    // SQL关键词（统一小写）
    public static final String SQL_DELETE = "delete";
    public static final String SQL_UPDATE = "update";
    public static final String SQL_WHERE = "where";

    // 警告和错误提示（可根据需要迁移到国际化资源文件）
    public static final String WARN_UNSAFE_DELETE_UPDATE =
            "SQL检查通过，警告：检测到未加 WHERE 的 DELETE/UPDATE 语句，可能会影响全表数据！";

    public static final String ERROR_SQL_EXECUTE_PREFIX = "SQL执行错误: ";
}

