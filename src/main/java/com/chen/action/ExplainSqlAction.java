package com.chen.action;

import com.chen.entity.DbConfig;
import com.chen.utils.HtmlViewerUtil;
import com.chen.utils.JdbcTableInfoUtil;
import com.chen.utils.SoarYamlUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.chen.constant.FileConstant.*;
import static com.chen.constant.MessageConstants.*;
import static com.chen.utils.DbConfigUtil.*;
import static com.chen.utils.JdbcTableInfoUtil.testConnection;
import static com.chen.utils.SoarYamlUtil.runSoarFixed;
import static com.chen.utils.SoarYamlUtil.writeSqlToIdea;

/**
 * 执行计划分析操作类
 * 模仿 CheckSqlAction，实现 EXPLAIN 执行计划展示
 */
public class ExplainSqlAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR);

        if (editor == null) {
            Messages.showWarningDialog(project,
                    ERROR_NO_EDITOR,
                    DIALOG_TITLE);
            return;
        }

        SelectionModel selectionModel = editor.getSelectionModel();
        String sql = selectionModel.getSelectedText();
        if (sql == null || sql.trim().isEmpty()) {
            sql = editor.getDocument().getText();
        }

        if (sql == null || sql.trim().isEmpty()) {
            Messages.showWarningDialog(project,
                    ERROR_NO_EDITOR,
                    DIALOG_TITLE);
            return;
        }

        Optional<DbConfig> dbConfigOpt = Optional.ofNullable(loadFromCache(project))
                .or(() -> Optional.ofNullable(tryLoadDbConfig(project)))
                .or(() -> Optional.ofNullable(promptUserInputSync(project)));

        if (!dbConfigOpt.isPresent()) {
            Messages.showErrorDialog(project,
                    ERROR_NO_DB_CONFIG,
                    DIALOG_TITLE);
            return;
        }

        DbConfig dbConfig = dbConfigOpt.get();

        try {
            if (!testConnection(dbConfig)) {
                Messages.showErrorDialog(project,
                        ERROR_CONNECTION_FAIL,
                        DIALOG_TITLE);
                return;
            }
        } catch (Exception ex) {
            Messages.showErrorDialog(project,
                    ERROR_CONNECTION_EXCEPTION_PREFIX + ex.getMessage(),
                    DIALOG_TITLE);
            return;
        }





        try {
            // 仅支持 SELECT 语句
            if (!sql.trim().toLowerCase().startsWith("select")) {
                Messages.showWarningDialog(project,
                        WARN_NOT_SELECT,
                        DIALOG_TITLE);
                return;
            }

            List<Map<String, Object>> explainRows = JdbcTableInfoUtil.explainSql(dbConfig, sql);

            if (explainRows == null || explainRows.isEmpty()) {
                Messages.showInfoMessage(project,
                        INFO_NO_RESULT,
                        DIALOG_TITLE);
                return;
            }
            saveToCache(project, dbConfig);
            //缓存sql文件
            writeSqlToIdea(project, sql);

            String html = buildExplainHtmlWithChinese(sql, explainRows);

            HtmlViewerUtil.showHtml(html, DIALOG_TITLE,runSoarFixed(project));

        } catch (Exception ex) {
            Messages.showErrorDialog(project,
                    ERROR_ANALYZE_FAIL_PREFIX + ex.getMessage(),
                    DIALOG_TITLE);
        }
    }


    /**
     * 生成执行计划 HTML
     */
    private String buildExplainHtmlWithChinese(String sql, List<Map<String, Object>> rows) {
        StringBuilder html = new StringBuilder();
        html.append("<style>")
                .append(".db-table { width:100%; border-collapse:collapse; font-family:'Segoe UI',Arial,sans-serif; background:#23272f; border: 1px solid #444b58; }")
                .append(".db-table th, .db-table td { border:1px solid #444b58; padding:10px 8px; color:#d8dee9; text-align:center; vertical-align:middle; }")
                .append(".db-table th { background:linear-gradient(90deg,#34495e 0%,#23272f 100%); font-weight:600; font-size:15px; }")
                .append(".db-table td { background:#23272f; font-size:14px; }")
                .append(".db-table tr:hover td { background:#2d333b; }")
                .append(".db-table .warn { color: #ffa726; font-weight: bold; }")
                .append("</style>");

        html.append("<b style='font-size:1.1em;color:#e1eaff;'>SQL 执行计划分析：</b><br>");
        html.append("<div style='color:#999999;padding:4px 0 8px;'>").append(sql).append("</div>");

        html.append("<table class='db-table'><thead><tr>");
        // 中文列头映射
        Map<String, String> columnTitleMap = Map.ofEntries(
                Map.entry("id", "序号"),
                Map.entry("select_type", "查询类型"),
                Map.entry("table", "表名"),
                Map.entry("type", "连接类型"),
                Map.entry("possible_keys", "可能使用的索引"),
                Map.entry("key", "实际使用的索引"),
                Map.entry("key_len", "索引长度"),
                Map.entry("ref", "关联字段"),
                Map.entry("rows", "扫描行数"),
                Map.entry("filtered", "过滤比例"),
                Map.entry("Extra", "额外信息")
        );

        var keys = rows.get(0).keySet();
        for (String col : keys) {
            html.append("<th>").append(columnTitleMap.getOrDefault(col, col)).append("</th>");
        }
        html.append("</tr></thead><tbody>");

        for (Map<String, Object> row : rows) {
            html.append("<tr>");
            for (String key : keys) {
                Object value = row.get(key);
                String valStr = value != null ? value.toString() : "";
                if ("type".equalsIgnoreCase(key) && "ALL".equalsIgnoreCase(valStr)) {
                    valStr = "<span class='warn'>ALL（全表扫描）</span>";
                }
                html.append("<td>").append(valStr).append("</td>");
            }
            html.append("</tr>");
        }

        html.append("</tbody></table>");

        html.append("<div style='padding-top:10px;padding-left:10px;font-size:13px;color:#8fa1b3;'>")
                .append("<b>字段解释：</b><br>")
                .append("➤ <b>序号（id）</b>：执行顺序，数字越大优先执行。<br>")
                .append("➤ <b>查询类型（select_type）</b>：表示当前 SELECT 的类型，如 SIMPLE（简单查询）、PRIMARY（主查询）、UNION 等。<br>")
                .append("➤ <b>表名（table）</b>：当前正在访问的表名或临时表名。<br>")
                .append("➤ <b>连接类型（type）</b>：MySQL 选择的数据读取方式，对性能影响很大，按效率从高到低排列如下：<br>")
                .append("&nbsp;&nbsp;&nbsp;&nbsp;- <b>system</b>：系统表（只有一行），极少见，效率最高。<br>")
                .append("&nbsp;&nbsp;&nbsp;&nbsp;- <b>const</b>：常量查找，例如主键查询，最多返回一行。<br>")
                .append("&nbsp;&nbsp;&nbsp;&nbsp;- <b>eq_ref</b>：主键或唯一索引等值连接，性能很高。<br>")
                .append("&nbsp;&nbsp;&nbsp;&nbsp;- <b>ref</b>：普通索引等值连接，效率略低于 eq_ref。<br>")
                .append("&nbsp;&nbsp;&nbsp;&nbsp;- <b>range</b>：范围查询，例如使用了 BETWEEN、<、>、IN 等。<br>")
                .append("&nbsp;&nbsp;&nbsp;&nbsp;- <b>index</b>：全索引扫描，不读取数据表，仅扫描索引。<br>")
                .append("&nbsp;&nbsp;&nbsp;&nbsp;- <b>ALL</b>：全表扫描，性能最差，应尽量避免。<br>")
                .append("➤ <b>可能使用的索引（possible_keys）</b>：查询可能会用到的索引。<br>")
                .append("➤ <b>使用索引（key）</b>：优化器实际使用的索引。<br>")
                .append("➤ <b>索引长度（key_len）</b>：使用索引的长度，越短越优。<br>")
                .append("➤ <b>关联字段（ref）</b>：显示索引的哪一列被用于查找数据。<br>")
                .append("➤ <b>扫描行数（rows）</b>：MySQL 预估要扫描的行数，值越小越优。<br>")
                .append("➤ <b>过滤比例（filtered）</b>：剩余记录占比，表示该表经过 WHERE 条件过滤后剩余的记录比例。<br>")
                .append("➤ <b>额外信息（Extra）</b>：包含执行过程中的额外信息，如是否使用临时表、文件排序、索引下推等。<br>")
                .append("</div>");


        return html.toString();
    }
}
