package com.chen.utils;

import com.chen.constant.SqlConstants;
import com.chen.entity.DbConfig;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;

import java.sql.Connection;
import java.sql.Statement;

/**
 * SQL 语句检查工具类。
 * <p>
 * 提供对 SQL 执行安全性与语法合法性的静态校验功能，适用于开发工具、在线 SQL 编辑器等场景。
 * 包括：
 * - 通过回滚事务方式检测 SQL 是否能成功执行；
 * - 语法层面的静态检查；
 * - 检测是否存在无 WHERE 条件的危险 DELETE/UPDATE 操作。
 * </p>
 */
public class SqlCheckUtil {

    /**
     * 尝试执行 SQL，并立即回滚，用于检测 SQL 是否能被数据库正常解析和执行。
     * <p>
     * 注意：此方法不会真正修改数据库内容，执行后会立刻回滚事务。
     * 适用于检测 INSERT、UPDATE、DELETE 等 DML 语句是否可执行。
     * </p>
     *
     * @param config 数据库连接配置
     * @param sql    要检测的 SQL 语句
     * @return 若无异常返回 null，若出错返回错误前缀加具体错误信息
     */
    public static String checkSQLWithRollback(DbConfig config, String sql) {
        try (Connection conn = DataSourceManager.getDataSource(config).getConnection();
             Statement stmt = conn.createStatement()) {

            conn.setAutoCommit(false); // 开启事务
            stmt.execute(sql);         // 尝试执行 SQL
            conn.rollback();           // 执行后立即回滚
            return null;
        } catch (Exception e) {
            return SqlConstants.ERROR_SQL_EXECUTE_PREFIX + e.getMessage();
        }
    }

    /**
     * 使用 JSqlParser 对 SQL 进行语法解析检查，仅验证语法是否合法。
     * <p>
     * 已被标记为废弃，推荐使用真实数据库执行+回滚的方式校验。
     * </p>
     *
     * @param sql 要检查的 SQL 字符串
     * @return 若语法正确返回 null，错误则返回错误信息
     */
    @Deprecated
    private static String checkSyntaxOnly(String sql) {
        try {
            CCJSqlParserUtil.parse(sql);
            return null;
        } catch (JSQLParserException e) {
            return "SQL语法错误: " + e.getMessage();
        }
    }

    /**
     * 检查是否存在危险 SQL，例如没有 WHERE 子句的 DELETE 或 UPDATE。
     * <p>
     * 示例：
     * DELETE FROM user → ⚠️ 警告
     * UPDATE user SET name = 'x' → ⚠️ 警告
     * </p>
     *
     * @param sql SQL 语句
     * @return 若是危险语句返回提示信息，否则返回 null
     */
    public static String checkDangerous(String sql) {
        String lower = sql.trim().toLowerCase();
        if ((lower.startsWith(SqlConstants.SQL_DELETE) || lower.startsWith(SqlConstants.SQL_UPDATE))
                && !lower.contains(SqlConstants.SQL_WHERE)) {
            return SqlConstants.WARN_UNSAFE_DELETE_UPDATE;
        }
        return null;
    }
}
