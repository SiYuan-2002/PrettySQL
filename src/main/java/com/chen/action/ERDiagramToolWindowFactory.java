package com.chen.action;

import com.chen.entity.DbConfig;
import com.chen.utils.DataSourceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.jcef.JBCefBrowser;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.*;
import java.util.List;

import static com.chen.utils.DbConfigUtil.loadFromCache;

/**
 * IDEA 工具窗口：分页生成 Mermaid ER 图并展示，避免 Mermaid 大图渲染限制
 */
public class ERDiagramToolWindowFactory implements ToolWindowFactory {

    // 每页表数量，可根据实际需求调整
    private static final int PAGE_SIZE = 20;

    @Override
    public void createToolWindowContent(Project project, ToolWindow toolWindow) {
        JBCefBrowser browser = new JBCefBrowser();
        DbConfig dbConfig = loadFromCache(project);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(browser.getComponent(), BorderLayout.CENTER);

        // 分页组件
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton prevBtn = new JButton("上一页");
        JButton nextBtn = new JButton("下一页");
        JLabel pageLabel = new JLabel();
        controlPanel.add(prevBtn);
        controlPanel.add(nextBtn);
        controlPanel.add(pageLabel);
        panel.add(controlPanel, BorderLayout.SOUTH);

        // 状态
        final int[] page = {1};
        final int[] totalPages = {1};

        Runnable update = () -> {
            try (Connection conn = DataSourceManager.getDataSource(dbConfig).getConnection()) {
                int totalTables = getTableCount(conn);
                totalPages[0] = Math.max(1, (totalTables + PAGE_SIZE - 1) / PAGE_SIZE);
                String html = getMermaidHtml(conn, page[0], PAGE_SIZE, totalTables, totalPages[0]);
                System.out.println("==========="+html);
                browser.loadHTML(html);
                System.out.println("end==========="+html);
                pageLabel.setText("第 " + page[0] + " / " + totalPages[0] + " 页");
                prevBtn.setEnabled(page[0] > 1);
                nextBtn.setEnabled(page[0] < totalPages[0]);
            } catch (Exception e) {
                e.printStackTrace();
                browser.loadHTML("<html><body><pre style='color:red;'>数据库连接失败，无法生成ER图。\n" + e.getMessage() + "</pre></body></html>");
            }
        };
        update.run();
        System.out.println("111===========");
        ContentFactory contentFactory = ContentFactory.getInstance();
        System.out.println("222===========");
        Content content = contentFactory.createContent(panel, "", false);
        System.out.println("333===========");
        toolWindow.getContentManager().addContent(content);

        System.out.println("44444===========");

    }

    /**
     * 获取表总数
     */
    private int getTableCount(Connection conn) throws SQLException {
        DatabaseMetaData metaData = conn.getMetaData();
        int count = 0;
        ResultSet tables = metaData.getTables(conn.getCatalog(), null, "%", new String[]{"TABLE"});
        while (tables.next()) {
            count++;
        }
        return count;
    }

    /**
     * 生成 Mermaid ER 图的 HTML，分页
     */
    private String getMermaidHtml(Connection conn, int page, int pageSize, int totalTables, int totalPages) throws SQLException {
        String er = generateMermaidERDiagram(conn, page, pageSize);
        return """
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="utf-8"/>
          <script src="https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.min.js"></script>
          <script>
            mermaid.initialize({startOnLoad:true, theme:'dark'});
          </script>
          <style>body{background:#232323;}</style>
        </head>
        <body>
        <div style='color:white;padding:8px;'>第 %d / %d 页，每页 %d 表，共 %d 表</div>
          <div class="mermaid">%s</div>
        </body>
        </html>
       """.formatted(page, totalPages, pageSize, totalTables, er);
    }

    /**
     * 分页读取数据库结构，生成 Mermaid erDiagram 代码
     */
    private String generateMermaidERDiagram(Connection conn, int page, int pageSize) throws SQLException {
        StringBuilder er = new StringBuilder();
        er.append("erDiagram\n");

        DatabaseMetaData metaData = conn.getMetaData();

        // 收集所有表名
        ResultSet tables = metaData.getTables(conn.getCatalog(), null, "%", new String[]{"TABLE"});
        List<String> tableNames = new ArrayList<>();
        while (tables.next()) {
            String tableName = tables.getString("TABLE_NAME");
            tableNames.add(tableName);
        }

        int totalTables = tableNames.size();
        int start = Math.max(0, (page - 1) * pageSize);
        int end = Math.min(start + pageSize, totalTables);
        Map<String, List<String[]>> tableFields = new LinkedHashMap<>();

        // 只收集当前页的表和字段
        for (int i = start; i < end; i++) {
            String table = tableNames.get(i);
            List<String[]> fields = new ArrayList<>();
            ResultSet columns = metaData.getColumns(conn.getCatalog(), null, table, "%");
            while (columns.next()) {
                String colName = columns.getString("COLUMN_NAME");
                String type = columns.getString("TYPE_NAME");
                fields.add(new String[]{type, colName});
            }
            tableFields.put(table, fields);
        }

        // 输出表结构
        for (String table : tableFields.keySet()) {
            er.append("  ").append(safeName(table)).append(" {\n");
            for (String[] field : tableFields.get(table)) {
                er.append("    ").append(field[0]).append(" ").append(safeName(field[1])).append("\n");
            }
            er.append("  }\n");
        }

        // 只显示当前页表之间的外键关系
        for (String table : tableFields.keySet()) {
            ResultSet fks = metaData.getImportedKeys(conn.getCatalog(), null, table);
            while (fks.next()) {
                String pkTable = fks.getString("PKTABLE_NAME");
                if (tableFields.containsKey(pkTable)) {
                    er.append("  ").append(safeName(pkTable))
                            .append(" ||--o{ ").append(safeName(table))
                            .append(" : has\n");
                }
            }
        }

        return er.toString();
    }

    /**
     * Mermaid 合法名生成（带特殊字符的表名、字段名加引号）
     */
    private String safeName(String name) {
        if (name.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
            return name;
        } else {
            return "\"" + name.replace("\"", "\\\"") + "\"";
        }
    }
}