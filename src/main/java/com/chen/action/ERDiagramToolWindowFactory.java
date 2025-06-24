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
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import java.util.List;

import static com.chen.constant.FileConstant.D3_JS_PATH;
import static com.chen.constant.FileConstant.DAGRE_JS_PATH;
import static com.chen.constant.MessageConstants.ERROR_NO_DB_CONFIG;
import static com.chen.constant.MessageConstants.NO_DB_CONFIG_HTML;
import static com.chen.utils.DbConfigUtil.loadFromCache;

/**
 * IDEA 工具窗口：展示数据库ER图（GoJS已换为D3+dagre-d3）。
 * 支持一键全部刷新、批量获取表/字段/外键信息，展示全部表及外键关系。
 * 支持表名及备注、字段类型及备注的显示。
 */
public class ERDiagramToolWindowFactory implements ToolWindowFactory {


    /** 刷新按钮文本 */
    private static final String REFRESH_BUTTON_TEXT = "刷新ER图";
    /** 刷新按钮宽度 */
    private static final int REFRESH_BUTTON_WIDTH = 90;
    /** 刷新按钮高度 */
    private static final int REFRESH_BUTTON_HEIGHT = 28;
    /** 刷新按钮左侧边距 */
    private static final int REFRESH_BUTTON_LEFT_MARGIN = 10;
    /** 刷新按钮上侧边距 */
    private static final int REFRESH_BUTTON_TOP_MARGIN = 10;
    /** ER图页签标题 */
    private static final String TAB_TITLE = "ER Diagram";

    @Override
    public void createToolWindowContent(Project project, ToolWindow toolWindow) {
        // 创建浏览器组件
        JBCefBrowser browser = new JBCefBrowser();

        // 主面板，包含浏览器和按钮
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(browser.getComponent(), BorderLayout.CENTER);

        // —— 刷新ER图按钮 ——
        JButton refreshButton = new JButton(REFRESH_BUTTON_TEXT);
        refreshButton.setFocusable(false);
        refreshButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        refreshButton.setPreferredSize(new Dimension(REFRESH_BUTTON_WIDTH, REFRESH_BUTTON_HEIGHT));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, REFRESH_BUTTON_LEFT_MARGIN, REFRESH_BUTTON_TOP_MARGIN));
        buttonPanel.setOpaque(false);
        buttonPanel.add(refreshButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        // 刷新按钮点击事件
        refreshButton.addActionListener(e -> {
            DbConfig dbConfigRefresh = loadFromCache(project);
            if (dbConfigRefresh == null) {
                showWarnDialog(panel, ERROR_NO_DB_CONFIG);
                return;
            }
            try (Connection conn = DataSourceManager.getDataSource(dbConfigRefresh).getConnection()) {
                String html = buildERHtml(conn);
                browser.loadHTML(html);
                showInfoDialog(panel, "ER图刷新成功！");
            } catch (Exception ex) {
                ex.printStackTrace();
                browser.loadHTML(buildErrorHtml("数据库连接失败，无法生成ER图。\n" + ex.getMessage()));
            }
        });

        // 初始加载（自动加载上次数据库配置）
        DbConfig dbConfig = loadFromCache(project);
        if (dbConfig == null) {
            browser.loadHTML(NO_DB_CONFIG_HTML);
        } else {
            try (Connection conn = DataSourceManager.getDataSource(dbConfig).getConnection()) {
                String html = buildERHtml(conn);
                browser.loadHTML(html);
            } catch (Exception e) {
                e.printStackTrace();
                browser.loadHTML(buildErrorHtml("数据库连接失败，无法生成ER图。\n" + e.getMessage()));
            }
        }

        // 添加到IntelliJ ToolWindow
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(panel, TAB_TITLE, false);
        toolWindow.getContentManager().addContent(content);
    }

    /**
     * 构建ER图HTML内容（批量查询数据库表、字段、外键信息，生成D3+dagre-d3代码）。
     * @param conn 有效的MySQL数据库连接
     * @return HTML字符串
     * @throws SQLException SQL异常
     * @throws IOException  IO异常（js文件读取）
     */
    public String buildERHtml(Connection conn) throws SQLException, IOException {
        StringBuilder nodesBuilder = new StringBuilder();
        StringBuilder edgesBuilder = new StringBuilder();

        String catalog = conn.getCatalog();

        // 1. 批量查询所有表备注
        Map<String, String> tableRemarks = queryTableRemarks(conn, catalog);

        // 2. 批量查询所有表字段及字段备注
        Map<String, Map<String, String>> tableFields = queryTableFields(conn, catalog);

        // 3. 批量查询所有外键信息
        List<Map<String, String>> links = queryForeignKeys(conn, catalog);

        // 4. 生成节点（表+备注+字段）
        for (Map.Entry<String, Map<String, String>> entry : tableFields.entrySet()) {
            String table = entry.getKey();
            Map<String, String> fields = entry.getValue();
            StringBuilder label = new StringBuilder();
            label.append(table);
            // 拼接表备注（如 user(用户表)）
            if (tableRemarks.containsKey(table) && !tableRemarks.get(table).isEmpty()) {
                label.append("(").append(tableRemarks.get(table)).append(")");
            }
            label.append("\\n———\\n");
            // 拼接字段及备注
            for (Map.Entry<String, String> field : fields.entrySet()) {
                label.append(field.getKey()).append(": ").append(field.getValue()).append("\\n");
            }
            nodesBuilder.append("g.setNode(\"").append(table).append("\", {\n")
                    .append("label: \"").append(label.toString().replace("\"", "\\\"")).append("\",\n")
                    .append("shape: \"rect\"\n")
                    .append("});\n");
        }

        // 5. 生成边（外键关系）
        for (Map<String, String> link : links) {
            edgesBuilder.append("g.setEdge(\"").append(link.get("from")).append("\", \"")
                    .append(link.get("to")).append("\", {\n")
                    .append("label: \"").append(link.get("label")).append("\",\n")
                    .append("lineInterpolate: \"basis\",\n")
                    .append("arrowhead: \"vee\"\n")
                    .append("});\n");
        }

        // 6. 读取d3.js和dagre-d3.js资源
        String d3Js = readJsResource(D3_JS_PATH);
        String dagreJs = readJsResource(DAGRE_JS_PATH);
        return String.format(HTML_TEMPLATE, d3Js, dagreJs, nodesBuilder, edgesBuilder);
    }

    /**
     * 批量查询所有表备注
     * @param conn 数据库连接
     * @param catalog 数据库名
     * @return 表名 -> 表备注
     * @throws SQLException SQL异常
     */
    private Map<String, String> queryTableRemarks(Connection conn, String catalog) throws SQLException {
        Map<String, String> tableRemarks = new HashMap<>();
        final String tableRemarkSql = "SELECT TABLE_NAME, TABLE_COMMENT FROM information_schema.TABLES WHERE TABLE_SCHEMA = ?";
        try (PreparedStatement ps = conn.prepareStatement(tableRemarkSql)) {
            ps.setString(1, catalog);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String tableName = rs.getString("TABLE_NAME");
                    String comment = rs.getString("TABLE_COMMENT");
                    tableRemarks.put(tableName, (comment != null && !comment.isBlank()) ? comment : "");
                }
            }
        }
        return tableRemarks;
    }

    /**
     * 批量查询所有表字段及字段备注
     * @param conn 数据库连接
     * @param catalog 数据库名
     * @return 表名 -> (字段名 -> 类型+备注)
     * @throws SQLException SQL异常
     */
    private Map<String, Map<String, String>> queryTableFields(Connection conn, String catalog) throws SQLException {
        Map<String, Map<String, String>> tableFields = new LinkedHashMap<>();
        final String columnSql = "SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, COLUMN_COMMENT FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = ?";
        try (PreparedStatement ps = conn.prepareStatement(columnSql)) {
            ps.setString(1, catalog);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String tableName = rs.getString("TABLE_NAME");
                    String colName = rs.getString("COLUMN_NAME");
                    String type = rs.getString("COLUMN_TYPE");
                    String remark = rs.getString("COLUMN_COMMENT");
                    tableFields
                            .computeIfAbsent(tableName, k -> new LinkedHashMap<>())
                            .put(colName, type + (remark != null && !remark.isEmpty() ? " (" + remark + ")" : ""));
                }
            }
        }
        return tableFields;
    }

    /**
     * 批量查询所有外键信息
     * @param conn 数据库连接
     * @param catalog 数据库名
     * @return 外键关系列表
     * @throws SQLException SQL异常
     */
    private List<Map<String, String>> queryForeignKeys(Connection conn, String catalog) throws SQLException {
        List<Map<String, String>> links = new ArrayList<>();
        final String fkSql = "SELECT TABLE_NAME, COLUMN_NAME, REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME " +
                "FROM information_schema.KEY_COLUMN_USAGE " +
                "WHERE TABLE_SCHEMA = ? AND REFERENCED_TABLE_NAME IS NOT NULL";
        try (PreparedStatement ps = conn.prepareStatement(fkSql)) {
            ps.setString(1, catalog);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String pkTable = rs.getString("REFERENCED_TABLE_NAME");
                    String pkColumn = rs.getString("REFERENCED_COLUMN_NAME");
                    String fkTable = rs.getString("TABLE_NAME");
                    String fkColumn = rs.getString("COLUMN_NAME");
                    links.add(Map.of(
                            "from", pkTable,
                            "to", fkTable,
                            "label", pkTable + "." + pkColumn + " -> " + fkTable + "." + fkColumn
                    ));
                }
            }
        }
        return links;
    }

    /**
     * 读取js资源文件内容
     * @param resourcePath 资源路径
     * @return 文件内容字符串
     * @throws IOException IO异常
     */
    private String readJsResource(String resourcePath) throws IOException {
        // 兼容IDEA插件环境的资源读取
        InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath);
        if (is == null) throw new FileNotFoundException("资源未找到: " + resourcePath);
        try (is) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * 构建错误信息的HTML片段
     * @param message 错误信息
     * @return HTML字符串
     */
    private String buildErrorHtml(String message) {
        return "<html><body><pre style='color:red;'>" + message + "</pre></body></html>";
    }

    /**
     * 弹出警告对话框
     * @param parent 父组件
     * @param message 提示信息
     */
    private void showWarnDialog(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "提示", JOptionPane.WARNING_MESSAGE);
    }

    /**
     * 弹出信息对话框
     * @param parent 父组件
     * @param message 信息
     */
    private void showInfoDialog(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "提示", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * ER图HTML模板，内嵌D3与Dagre-D3绘图脚本和样式
     */
    private static final String HTML_TEMPLATE = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8" />
            <title>ER 图</title>
            <script>
            %s
            </script>
            <script>
            %s
            </script>
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
        <svg id="svg-canvas">
            <g id="graph-container"></g>
        </svg>
        <script>
            const g = new dagreD3.graphlib.Graph().setGraph({
                rankdir: "LR", nodesep: 40, ranksep: 100, marginx: 20, marginy: 20
            });
            %s
            %s
            const svg = d3.select("#svg-canvas");
            const inner = svg.select("#graph-container");
            const render = new dagreD3.render();
            render(inner, g);

            inner.selectAll("g.node")
                .attr("transform", d => {
                    const node = g.node(d);
                    node.x = node.x || 0;
                    node.y = node.y || 0;
                    return `translate(${node.x},${node.y})`;
                });

            const zoom = d3.zoom()
                .scaleExtent([0.1, 2])
                .on("zoom", event => {
                    inner.attr("transform", event.transform);
                });
            svg.call(zoom);
            svg.call(zoom.transform, d3.zoomIdentity.translate(20, 20).scale(1));

            const drag = d3.drag()
                .on("drag", (event, nodeId) => {
                    const node = g.node(nodeId);
                    node.x += event.dx;
                    node.y += event.dy;
                    inner.select(`g.node[id='${nodeId}']`).attr("transform", `translate(${node.x},${node.y})`);
                    inner.selectAll("g.edgePath").each(function(d) {
                        const edge = g.edge(d);
                        const sourceNode = g.node(d.v);
                        const targetNode = g.node(d.w);
                        if (!sourceNode || !targetNode) return;
                        const path = `M${sourceNode.x},${sourceNode.y}L${targetNode.x},${targetNode.y}`;
                        d3.select(this).select("path").attr("d", path);
                    });
                });

            inner.selectAll("g.node")
                .attr("id", d => d)
                .call(drag);
        </script>
        </body>
        </html>
        """;
}