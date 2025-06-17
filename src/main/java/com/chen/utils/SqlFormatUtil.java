package com.chen.utils;

import com.github.vertical_blank.sqlformatter.SqlFormatter;

/**
 * SQL 格式化工具类。
 * <p>
 * 基于第三方库 {@code vertical-blank/sql-formatter}，可根据指定方言对 SQL 字符串进行美化格式化，
 * 使 SQL 更易阅读，适用于开发工具、日志展示、SQL 编辑器等场景。
 * 如果传入方言无效或格式化失败，将原样返回 SQL 字符串。
 * @author czh
 * @version 1.0
 * @since 2025/6/12
 */
public class SqlFormatUtil {

    /**
     * 根据指定 SQL 方言对 SQL 字符串进行格式化美化处理。
     *
     * @param sql     原始 SQL 字符串（非空）
     * @param dialect SQL 方言名称，如 "mysql"、"postgresql"、"sql" 等
     * @return 格式化后的 SQL 字符串；如果格式化失败，返回原始 SQL
     */
    public static String formatSql(String sql, String dialect) {
        try {
            return SqlFormatter.of(dialect).format(sql);
        } catch (Exception e) {
            // 若格式化出错，则返回原始 SQL 字符串
            return sql;
        }
    }
}
