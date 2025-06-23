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
 * IDEA 工具窗口：使用 GoJS 展示数据库 ER 图，去除分页，展示全部表和外键关系
 */
public class ERDiagramToolWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(Project project, ToolWindow toolWindow) {
        JBCefBrowser browser = new JBCefBrowser();
        DbConfig dbConfig = loadFromCache(project);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(browser.getComponent(), BorderLayout.CENTER);

        // 无分页，直接加载所有数据
        try (Connection conn = DataSourceManager.getDataSource(dbConfig).getConnection()) {
            String html = getGoJsHtml(conn);
            browser.loadHTML(html);
        } catch (Exception e) {
            e.printStackTrace();
            browser.loadHTML("<html><body><pre style='color:red;'>数据库连接失败，无法生成ER图。\n" + e.getMessage() + "</pre></body></html>");
        }

        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(panel, "ER Diagram", false);
        toolWindow.getContentManager().addContent(content);
    }

    /**
     * 生成包含全部表和关系的 GoJS ER 图 HTML
     */
    public String getGoJsHtml(Connection conn) throws SQLException {
        String json = generateFullGoJsERDiagramJson(conn);
        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="UTF-8">
              <title>GoJS ER 图</title>
              <script src="https://unpkg.com/gojs/release/go.js"></script>
              <style>
                html, body { height: 100%%; margin: 0; background: #1e1e1e; }
                #myDiagramDiv { width: 100%%; height: 100%%; }
              </style>
            </head>
            <body>
              <div id="myDiagramDiv"></div>
              <script>
                const $ = go.GraphObject.make;
                const diagram = $(go.Diagram, "myDiagramDiv", {
                  "undoManager.isEnabled": true,
                  layout: $(go.ForceDirectedLayout),
                  "toolManager.mouseWheelBehavior": go.ToolManager.WheelZoom,
                });

                diagram.nodeTemplate =
                  $(go.Node, "Auto",
                    $(go.Shape, "RoundedRectangle", { fill: "#4e88af", stroke: null }),
                    $(go.Panel, "Table", { margin: 6 },
                      $(go.TextBlock, {
                        row: 0, column: 0, columnSpan: 2,
                        font: "bold 13px sans-serif", margin: 4, stroke: "white"
                      }, new go.Binding("text", "name")),
                      $(go.Panel, "Table", { row: 1, column: 0, columnSpan: 2 },
                        new go.Binding("itemArray", "fields"),
                        {
                          itemTemplate:
                            $(go.Panel, "TableRow",
                              $(go.TextBlock, { column: 0, margin: 2, font: "11px monospace", stroke: "#ddd" },
                                new go.Binding("text", "name")),
                              $(go.TextBlock, { column: 1, margin: 2, font: "11px monospace", stroke: "#aaa" },
                                new go.Binding("text", "type"))
                            )
                        }
                      )
                    )
                  );

                diagram.linkTemplate =
                  $(go.Link, { routing: go.Link.AvoidsNodes, corner: 5 },
                    $(go.Shape, { stroke: "#ccc" }),
                    $(go.Shape, { toArrow: "Standard", stroke: "#ccc", fill: "#ccc" })
                  );

                const data = %s;
                diagram.model = new go.GraphLinksModel(data.nodes, data.links);
              </script>
            </body>
            </html>
            """.formatted(json);
    }

    /**
     * 读取数据库所有表和字段，获取外键关系，生成 GoJS 用 JSON
     */
    public String generateFullGoJsERDiagramJson(Connection conn) throws SQLException {
        DatabaseMetaData metaData = conn.getMetaData();
        String catalog = conn.getCatalog();
        String schema = conn.getSchema();

        Map<String, Map<String, String>> tableFields = new LinkedHashMap<>();
        List<Map<String, String>> links = new ArrayList<>();

        // 读取所有表
        try (ResultSet tables = metaData.getTables(catalog, schema, "%", new String[]{"TABLE"})) {
            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                Map<String, String> fields = new LinkedHashMap<>();

                try (ResultSet columns = metaData.getColumns(catalog, schema, tableName, "%")) {
                    while (columns.next()) {
                        String colName = columns.getString("COLUMN_NAME");
                        String type = columns.getString("TYPE_NAME");
                        fields.put(colName, type);
                    }
                }

                tableFields.put(tableName, fields);
            }
        }

        // 读取所有外键关系
        for (String table : tableFields.keySet()) {
            try (ResultSet fks = metaData.getImportedKeys(catalog, schema, table)) {
                while (fks.next()) {
                    String pkTable = fks.getString("PKTABLE_NAME");
                    String fkTable = fks.getString("FKTABLE_NAME");
                    if (tableFields.containsKey(pkTable) && tableFields.containsKey(fkTable)) {
                        links.add(Map.of("from", pkTable, "to", fkTable));
                    }
                }
            }
        }

        // 拼接 JSON 字符串
        StringBuilder sb = new StringBuilder();
        sb.append("{\"nodes\":[");

        boolean firstNode = true;
        for (Map.Entry<String, Map<String, String>> entry : tableFields.entrySet()) {
            if (!firstNode) sb.append(",");
            firstNode = false;

            sb.append("{");
            sb.append("\"key\":\"").append(entry.getKey()).append("\",");
            sb.append("\"name\":\"").append(entry.getKey()).append("\",");
            sb.append("\"fields\":[");
            boolean firstField = true;
            for (Map.Entry<String, String> field : entry.getValue().entrySet()) {
                if (!firstField) sb.append(",");
                firstField = false;
                sb.append("{\"name\":\"").append(field.getKey()).append("\",\"type\":\"").append(field.getValue()).append("\"}");
            }
            sb.append("]}");
        }
        sb.append("],\"links\":[");

        boolean firstLink = true;
        for (Map<String, String> link : links) {
            if (!firstLink) sb.append(",");
            firstLink = false;
            sb.append("{\"from\":\"").append(link.get("from")).append("\",\"to\":\"").append(link.get("to")).append("\"}");
        }
        sb.append("]}");

        return sb.toString();
    }
}
