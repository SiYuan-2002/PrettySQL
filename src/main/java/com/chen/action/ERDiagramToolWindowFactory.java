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

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(browser.getComponent(), BorderLayout.CENTER);

        JButton refreshButton = new JButton("刷新ER图");
        refreshButton.setFocusable(false);
        refreshButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        refreshButton.setPreferredSize(new Dimension(90, 28));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        buttonPanel.setOpaque(false);
        buttonPanel.add(refreshButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        refreshButton.addActionListener(e -> {
            DbConfig dbConfigRefresh = loadFromCache(project);
            if (dbConfigRefresh == null) {
                JOptionPane.showMessageDialog(panel, "请先配置数据库连接！", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try (Connection conn = DataSourceManager.getDataSource(dbConfigRefresh).getConnection()) {
                String html = getD3Html(conn);
                browser.loadHTML(html);
                JOptionPane.showMessageDialog(panel, "ER图刷新成功！", "提示", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                ex.printStackTrace();
                browser.loadHTML("<html><body><pre style='color:red;'>数据库连接失败，无法生成ER图。\n" + ex.getMessage() + "</pre></body></html>");
            }
        });

        // 一开始加载时也调用刷新逻辑
        DbConfig dbConfig = loadFromCache(project);
        if (dbConfig == null) {
            browser.loadHTML("<html><body><h2 style='color:red;'>请先配置数据库连接！</h2></body></html>");
        } else {
            try (Connection conn = DataSourceManager.getDataSource(dbConfig).getConnection()) {
                String html = getD3Html(conn);
                browser.loadHTML(html);
            } catch (Exception e) {
                e.printStackTrace();
                browser.loadHTML("<html><body><pre style='color:red;'>数据库连接失败，无法生成ER图。\n" + e.getMessage() + "</pre></body></html>");
            }
        }

        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(panel, "ER Diagram", false);
        toolWindow.getContentManager().addContent(content);
    }


    public String getD3Html(Connection conn) throws SQLException {
        StringBuilder nodesBuilder = new StringBuilder();
        StringBuilder edgesBuilder = new StringBuilder();

        DatabaseMetaData metaData = conn.getMetaData();
        String catalog = conn.getCatalog();
        String schema = conn.getSchema();

        Map<String, Map<String, String>> tableFields = new LinkedHashMap<>();
        Map<String, String> tableRemarks = new HashMap<>();
        List<Map<String, String>> links = new ArrayList<>();

        try (ResultSet tables = metaData.getTables(catalog, schema, "%", new String[]{"TABLE"})) {
            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                String tableRemark = tables.getString("REMARKS");
                tableRemarks.put(tableName, tableRemark != null ? tableRemark : "");

                Map<String, String> fields = new LinkedHashMap<>();
                try (ResultSet columns = metaData.getColumns(catalog, schema, tableName, "%")) {
                    while (columns.next()) {
                        String colName = columns.getString("COLUMN_NAME");
                        String type = columns.getString("TYPE_NAME");
                        String remark = columns.getString("REMARKS");
                        fields.put(colName, type + (remark != null && !remark.isEmpty() ? " (" + remark + ")" : ""));
                    }
                }
                tableFields.put(tableName, fields);
            }
        }

        for (String table : tableFields.keySet()) {
            try (ResultSet fks = metaData.getImportedKeys(catalog, schema, table)) {
                while (fks.next()) {
                    String pkTable = fks.getString("PKTABLE_NAME");
                    String fkTable = fks.getString("FKTABLE_NAME");
                    String fkColumn = fks.getString("FKCOLUMN_NAME");
                    String pkColumn = fks.getString("PKCOLUMN_NAME");
                    links.add(Map.of("from", pkTable, "to", fkTable, "label", pkTable + "." + pkColumn + " -> " + fkTable + "." + fkColumn));
                }
            }
        }

        for (Map.Entry<String, Map<String, String>> entry : tableFields.entrySet()) {
            String table = entry.getKey();
            Map<String, String> fields = entry.getValue();
            StringBuilder label = new StringBuilder();
            label.append(table);
            if (tableRemarks.containsKey(table) && !tableRemarks.get(table).isEmpty()) {
                label.append(" (" + tableRemarks.get(table) + ")");
            }
            label.append("\\n———\\n");
            for (Map.Entry<String, String> field : fields.entrySet()) {
                label.append(field.getKey()).append(": ").append(field.getValue()).append("\\n");
            }
            nodesBuilder.append("g.setNode(\"").append(table).append("\", {\n")
                    .append("label: \"").append(label.toString().replace("\"", "\\\"")).append("\",\n")
                    .append("shape: \"rect\"\n")
                    .append("});\n");
        }

        for (Map<String, String> link : links) {
            edgesBuilder.append("g.setEdge(\"").append(link.get("from")).append("\", \"")
                    .append(link.get("to")).append("\", {\n")
                    .append("label: \"").append(link.get("label")).append("\",\n")
                    .append("lineInterpolate: \"basis\",\n")
                    .append("arrowhead: \"vee\"\n")
                    .append("});\n");
        }

        return String.format(HTML_TEMPLATE, nodesBuilder, edgesBuilder);
    }

    private static final String HTML_TEMPLATE = """
<!DOCTYPE html>
<html>
<head>
    <meta charset=\"UTF-8\" />
    <title>ER 图</title>
    <script src=\"https://d3js.org/d3.v7.min.js\"></script>
    <script src=\"https://unpkg.com/dagre-d3@0.6.4/dist/dagre-d3.min.js\"></script>
    <style>
        html, body { margin:0; padding:0; width:100vw; height:100vh; background:#1e1e1e; overflow:hidden; }
        svg { width: 100%%; height: 100%%; cursor: grab; user-select: none; }
        .node rect { fill:#4e88af; stroke:#333; stroke-width:1.5px; rx:8; ry:8; cursor:move; }
        .node text { fill:white; font-family:monospace; font-size:12px; white-space:pre; pointer-events: none; }
        .edgePath path { stroke:#ccc; stroke-width:1.5px; fill:none; }
        .edgeLabel text { fill:#ccc; font-size:11px; }
    </style>
</head>
<body>
<svg id=\"svg-canvas\">
    <g id=\"graph-container\"></g>
</svg>
<script>
    const g = new dagreD3.graphlib.Graph().setGraph({
        rankdir: \"LR\", nodesep: 40, ranksep: 100, marginx: 20, marginy: 20
    });
    %s
    %s
    const svg = d3.select(\"#svg-canvas\");
    const inner = svg.select(\"#graph-container\");
    const render = new dagreD3.render();
    render(inner, g);

    inner.selectAll(\"g.node\")
        .attr(\"transform\", d => {
            const node = g.node(d);
            node.x = node.x || 0;
            node.y = node.y || 0;
            return `translate(${node.x},${node.y})`;
        });

    const zoom = d3.zoom()
        .scaleExtent([0.1, 2])
        .on(\"zoom\", event => {
            inner.attr(\"transform\", event.transform);
        });
    svg.call(zoom);
    svg.call(zoom.transform, d3.zoomIdentity.translate(20, 20).scale(1));

    const drag = d3.drag()
        .on(\"drag\", (event, nodeId) => {
            const node = g.node(nodeId);
            node.x += event.dx;
            node.y += event.dy;
            inner.select(`g.node[id='${nodeId}']`).attr(\"transform\", `translate(${node.x},${node.y})`);
            inner.selectAll(\"g.edgePath\").each(function(d) {
                const edge = g.edge(d);
                const sourceNode = g.node(d.v);
                const targetNode = g.node(d.w);
                if (!sourceNode || !targetNode) return;
                const path = `M${sourceNode.x},${sourceNode.y}L${targetNode.x},${targetNode.y}`;
                d3.select(this).select(\"path\").attr(\"d\", path);
            });
        });

    inner.selectAll(\"g.node\")
        .attr(\"id\", d => d)
        .call(drag);
</script>
</body>
</html>
""";
}
