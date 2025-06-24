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
            String html = getD3Html(conn);
            System.out.println(html);
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
    public String getD3Html(Connection conn) throws SQLException {
        StringBuilder nodesBuilder = new StringBuilder();
        StringBuilder edgesBuilder = new StringBuilder();

        DatabaseMetaData metaData = conn.getMetaData();
        String catalog = conn.getCatalog();
        String schema = conn.getSchema();

        Map<String, Map<String, String>> tableFields = new LinkedHashMap<>();
        List<Map<String, String>> links = new ArrayList<>();

        // 读取表及字段
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

        // 读取外键
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

        // 组装 JS：节点
        for (Map.Entry<String, Map<String, String>> entry : tableFields.entrySet()) {
            String table = entry.getKey();
            Map<String, String> fields = entry.getValue();
            StringBuilder label = new StringBuilder();
            label.append(table).append("\\n———\\n");
            for (Map.Entry<String, String> field : fields.entrySet()) {
                label.append(field.getKey()).append(": ").append(field.getValue()).append("\\n");
            }
            nodesBuilder.append("g.setNode(\"").append(table).append("\", {\n")
                    .append("label: \"").append(label.toString().replace("\"", "\\\"")).append("\",\n")
                    .append("shape: \"rect\"\n")
                    .append("});\n");
        }

        // 组装 JS：关系
        for (Map<String, String> link : links) {
            edgesBuilder.append("g.setEdge(\"").append(link.get("from")).append("\", \"")
                    .append(link.get("to")).append("\", {\n")
                    .append("label: \"").append(link.get("label")).append("\",\n")
                    .append("lineInterpolate: \"basis\",\n")
                    .append("arrowhead: \"vee\"\n")
                    .append("});\n");
        }

        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8" />
                    <title>可拖动节点的 dagre-d3 示例</title>
                    <script src="https://d3js.org/d3.v7.min.js"></script>
                    <script src="https://unpkg.com/dagre-d3@0.6.4/dist/dagre-d3.min.js"></script>
                    <style>
                        html, body {
                            margin:0; padding:0; width:100vw; height:100vh; background:#1e1e1e; overflow:hidden;
                        }
                        svg {
                            width: 100%%; height: 100%%; cursor: grab;
                            user-select: none;
                        }
                        .node rect {
                            fill: #4e88af;
                            stroke: #333;
                            stroke-width: 1.5px;
                            rx: 8; ry: 8;
                            cursor: move;
                        }
                        .node text {
                            fill: white;
                            font-family: monospace;
                            font-size: 12px;
                            white-space: pre;
                            pointer-events: none;
                        }
                        .edgePath path {
                            stroke: #ccc;
                            stroke-width: 1.5px;
                            fill: none;
                        }
                        .edgeLabel text {
                            fill: #ccc;
                            font-size: 11px;
                        }
                    </style>
                </head>
                <body>
                <svg id="svg-canvas">
                    <g id="graph-container"></g>
                </svg>

                <script>
                    const g = new dagreD3.graphlib.Graph().setGraph({
                        rankdir: "LR",
                        nodesep: 40,
                        ranksep: 100,
                        marginx: 20,
                        marginy: 20
                    });

                            %s
                        
                            // 动态插入边定义
                            %s

                    const svg = d3.select("#svg-canvas");
                    const inner = svg.select("#graph-container");
                    const render = new dagreD3.render();

                    // 初次渲染布局
                    render(inner, g);

                    // 把节点移动到dagre计算好的初始位置
                    inner.selectAll("g.node")
                        .attr("transform", d => {
                            const node = g.node(d);
                            node.x = node.x || 0;
                            node.y = node.y || 0;
                            return `translate(${node.x},${node.y})`;
                        });

                    // 缩放拖动整个画布
                    const zoom = d3.zoom()
                        .scaleExtent([0.1, 5])
                        .on("zoom", event => {
                            inner.attr("transform", event.transform);
                        });
                    svg.call(zoom);
                    svg.call(zoom.transform, d3.zoomIdentity.translate(20, 20).scale(5));

                    // 节点拖拽事件，不重新渲染layout，只更新节点和边位置
                    const drag = d3.drag()
                        .on("drag", (event, nodeId) => {
                            const node = g.node(nodeId);
                            node.x += event.dx;
                            node.y += event.dy;

                            // 更新节点位置
                            inner.select(`g.node[id='${nodeId}']`).attr("transform", `translate(${node.x},${node.y})`);

                            // 更新边路径
                            inner.selectAll("g.edgePath").each(function(d) {
                                const edge = g.edge(d);
                                const sourceNode = g.node(d.v);
                                const targetNode = g.node(d.w);
                                if (!sourceNode || !targetNode) return;

                                // 计算简单直线边路径（你可以换成更复杂的路径）
                                const path = `M${sourceNode.x},${sourceNode.y}L${targetNode.x},${targetNode.y}`;
                                d3.select(this).select("path").attr("d", path);
                            });
                        });

                    // 给节点绑定id和拖拽
                    inner.selectAll("g.node")
                        .attr("id", d => d)
                        .call(drag);
                </script>
                </body>
                </html>

                                        
  """.formatted(nodesBuilder, edgesBuilder);
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
