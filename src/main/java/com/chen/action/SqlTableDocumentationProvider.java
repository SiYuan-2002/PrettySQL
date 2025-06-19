package com.chen.action;

import com.chen.constant.MessageConstants;
import com.chen.entity.ColumnMeta;
import com.chen.entity.DbConfig;
import com.chen.utils.JdbcTableInfoUtil;
import com.intellij.lang.documentation.AbstractDocumentationProvider;
import com.intellij.lang.documentation.DocumentationProvider;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlText;
import com.intellij.psi.xml.XmlToken;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.List;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.chen.constant.FileConstant.CONFIG_PATH;
import static com.chen.constant.FileConstant.XML;
import static com.chen.constant.MessageConstants.*;
import static com.chen.utils.DbConfigUtil.*;
import static com.chen.utils.JdbcTableInfoUtil.testConnection;

/**
 * SQL 表结构文档展示提供器：支持在 IntelliJ 中对 SQL 中的表名悬停显示结构
 * 自动从 application.yml / application-dev.yml 中提取数据库连接配置并通过 JDBC 获取表字段元数据
 */
public class SqlTableDocumentationProvider extends AbstractDocumentationProvider {


    /**
     * 悬停展示文档：从 PSI 中识别表名，读取数据库连接并生成 HTML 表格
     */
    @Override
    public @Nullable String generateDoc(PsiElement element, @Nullable PsiElement originalElement) {
        String extension = Optional.ofNullable(element.getContainingFile())
                .map(PsiFile::getVirtualFile)
                .map(VirtualFile::getExtension)
                .orElse("");

        if (!XML.equalsIgnoreCase(extension)) {
            return NOT_IN_SCOPE;
        }


        String tableName = extractTableName(element);
        if (tableName == null) {
            return null;
        }

        DbConfig dbConfig = loadFromCache(element.getProject());

        if (dbConfig == null) {
            dbConfig = tryLoadDbConfig(element.getProject());
        }

        if (dbConfig == null) {
            promptUserInput(element.getProject(), config -> {
                try {
                    if (!testConnection(config)) {
                        Messages.showErrorDialog(element.getProject(),
                                MessageConstants.SQL_ERROR_CONNECTION_FAIL,
                                MessageConstants.SQL_ERROR_TITLE_CONNECTION_FAIL);
                        return;
                    }
                } catch (Exception ex) {
                    Messages.showErrorDialog(element.getProject(),
                            "数据库连接异常：" + ex.getMessage(),
                            MessageConstants.SQL_ERROR_TITLE_CONNECTION_FAIL);
                    return;
                }

                if (saveToCache(element.getProject(), config)) {
                    Path path = Paths.get(element.getProject().getBasePath(), CONFIG_PATH);
                    Messages.showInfoMessage(
                            CONFIG_SAVE_SUCCESS_MESSAGE_PREFIX + path.toString() + CONFIG_SAVE_SUCCESS_MESSAGE_SUFFIX,
                            CONFIG_SAVE_SUCCESS_TITLE
                    );
                }
            });
        }

        List<ColumnMeta> columns;
        try {
            columns = JdbcTableInfoUtil.getTableColumns(dbConfig, tableName);
        } catch (Exception e) {
            return ERROR_PREFIX + tableName + DATASOURCE + dbConfig.getUrl() + e.getMessage();
        }

        if (columns == null || columns.isEmpty()) {
            return ERROR_PREFIX + tableName + DATASOURCE + dbConfig.getUrl() + ERROR_NO_COLUMNS;
        }

        return buildHtmlTable(tableName, columns);
    }


    /**
     * 构建表结构的 HTML 表格展示
     */
    private String buildHtmlTable(String tableName, List<ColumnMeta> columns) {
        StringBuilder html = new StringBuilder();
        html.append("<style>")
                .append(".db-table { width:100%; border-collapse:collapse; font-family:'Segoe UI',Arial,sans-serif; background:#23272f; border: 1px solid #444b58; }")
                .append(".db-table th, .db-table td { border:1px solid #444b58; padding:10px 8px; color:#d8dee9; text-align:center; vertical-align:middle; }")
                .append(".db-table th { background:linear-gradient(90deg,#34495e 0%,#23272f 100%); font-weight:600; font-size:15px; }")
                .append(".db-table td { background:#23272f; font-size:14px; }")
                .append(".db-table tr:hover td { background:#2d333b; }")
                .append(".db-table .pk { color:#ffb300; font-weight:bold; }")
                .append(".db-table .idx { color:#4CAF50; font-weight:bold; }")
                .append("</style>");

        html.append("<b style='font-size:1.1em;color:#e1eaff;'>表名：</b> ").append(tableName).append("<br>");
        html.append("<table class='db-table'><thead><tr>")
                .append("<th style='width: 25%;'>字段名称</th>")
                .append("<th style='width: 20%;'>类型</th>")
                .append("<th style='width: 40%;'>备注</th>")
                .append("<th style='width: 7.5%;'>主键</th>")
                .append("<th style='width: 7.5%;'>索引</th>")
                .append("</tr></thead><tbody>");

        for (ColumnMeta col : columns) {
            html.append("<tr><td>").append(col.getName()).append("</td>")
                    .append("<td>").append(col.getType()).append("</td>")
                    .append("<td>").append(col.getRemark() == null ? "" : col.getRemark()).append("</td>")
                    .append("<td>").append(col.isPrimaryKey() ? "<span class='pk'>YES</span>" : "").append("</td>")
                    .append("<td>").append(col.isIndex() ? "<span class='idx'>YES</span>" : "").append("</td></tr>");
        }

        html.append("</tbody></table>");
        return html.toString();
    }

    /**
     * 提取 SQL 中的表名，适配 MyBatis XML 或 SQL 字符串
     */
    private @Nullable String extractTableName(PsiElement element) {
        if (element == null) return null;
        String text = element.getText();
        if (text == null || !text.matches("[a-zA-Z_][a-zA-Z0-9_]*")) return null;

        PsiElement parent = element.getParent();

        // 支持 from/join 关键字场景
        if (parent != null && parent.getText().matches("(?i).*\\b(from|join)\\b.*" + text + ".*")) {
            return text;
        }

        // XML/SQL 中的 token 内容匹配
        if (element instanceof XmlToken || element instanceof XmlText) {
            Pattern pattern = Pattern.compile("\\b(from|join)\\s+([a-zA-Z_][a-zA-Z0-9_]*)", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return matcher.group(2);
            }
        }

        return text;
    }


}
