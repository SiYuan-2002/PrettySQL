package com.chen.utils;
import com.chen.entity.DbConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.yaml.snakeyaml.Yaml;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.chen.constant.DbConstant.*;
import static com.chen.constant.FileConstant.CONFIG_PATH;
import static com.chen.constant.FileConstant.CONFIG_PATH_ALL;
import static com.chen.constant.MessageConstants.*;
import static com.chen.utils.DataSourceUrlFixer.appendUrlFix;

/**
 * @author czh
 * @version 1.0
 * @description:
 * @date 2025/6/14 7:25
 */
public class DbConfigUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 从项目目录中查找 YAML 配置并解析数据库连接参数，支持 spring.datasource 与 druid.master 配置
     */
    public static DbConfig tryLoadDbConfig(Project project) {
        List<File> ymlFiles = findAllYmlFiles(new File(project.getBasePath()));
        DbConfig dbConfig = null;

        for (File yml : ymlFiles) {
            try (InputStream input = new FileInputStream(yml)) {
                Yaml yaml = new Yaml();
                Map<String, Object> data = yaml.load(input);
                if (data == null) continue;

                @SuppressWarnings("unchecked")
                Map<String, Object> spring = (Map<String, Object>) data.get("spring");
                if (spring == null) continue;

                @SuppressWarnings("unchecked")
                Map<String, Object> datasource = (Map<String, Object>) spring.get("datasource");
                if (datasource == null) continue;

                // 第一优先：spring.datasource 配置
                String url = String.valueOf(datasource.get("url"));
                String username = String.valueOf(datasource.get("username"));
                String password = String.valueOf(datasource.get("username"));

                if (StringUtils.notBlankAndNotNullStr(url) && StringUtils.notBlankAndNotNullStr(username)) {
                    dbConfig = new DbConfig(url, username, password);
                    break;
                }

                // 第二优先：druid.master 配置
                @SuppressWarnings("unchecked")
                Map<String, Object> druid = (Map<String, Object>) datasource.get("druid");
                if (druid == null) continue;

                @SuppressWarnings("unchecked")
                Map<String, Object> master = (Map<String, Object>) druid.get("master");
                if (master == null) continue;

                url = String.valueOf(master.get("url"));
                username = String.valueOf(master.get("username"));
                password = String.valueOf(master.get("password"));

                if (StringUtils.notBlankAndNotNullStr(url) && StringUtils.notBlankAndNotNullStr(username)) {
                    dbConfig = new DbConfig(url, username, password);
                    saveToCacheAppend(project, dbConfig);
                    break;
                }
            } catch (Exception ignored) {
            }
        }


        if (dbConfig != null) {
            saveToCacheAppend(project, dbConfig);
            saveToCache(project, dbConfig);
        }

        return dbConfig;
    }


    /**
     * 递归查找所有 application*.yml 配置文件（仅限 src/main/resources）
     */
    public static List<File> findAllYmlFiles(File rootDir) {
        List<File> result = new ArrayList<>();
        Queue<File> queue = new LinkedList<>();
        queue.add(rootDir);

        while (!queue.isEmpty()) {
            File current = queue.poll();
            if (current == null || !current.exists()) continue;
            if (current.isDirectory()) {
                File[] children = current.listFiles();
                if (children != null) Collections.addAll(queue, children);
            } else if (isYmlFile(current)) {
                result.add(current);
            }
        }
        return result;
    }

    /**
     * 判断是否为 application(-xxx)?.yml 文件
     */
    public static boolean isYmlFile(File file) {
        String name = file.getName();
        return name.matches("application(-[\\w]+)?\\.ya?ml") &&
                file.getParentFile() != null &&
                file.getParentFile().getPath().replace("\\", "/").endsWith("src/main/resources");
    }


    /**
     * 弹出异步输入框，直到用户输入完整数据库 URL 和用户名或取消
     *
     * @param project  当前项目
     * @param callback 用户输入完成回调
     */
    public static void promptUserInput(Project project, Consumer<DbConfig> callback) {
        promptUserInputInternal(project, callback);
    }

    /**
     * 新增数据源
     *
     * @param project  当前项目
     * @param callback 用户输入完成回调
     */
    public static void promptUserAdd(Project project, Consumer<DbConfig> callback) {
        promptUserAddInternal(project, callback);
    }

    /**
     * 配置默认数据源
     *
     * @param project  当前项目
     * @param callback 用户输入完成回调
     */
    public static void promptUserInputWithDbType(Project project, Consumer<DbConfig> callback) {
        ApplicationManager.getApplication().invokeLater(() -> {
            List<DbConfig> configs = new ArrayList<>();
            Path path = Paths.get(project.getBasePath(), CONFIG_PATH_ALL);

            // 读取配置文件
            if (Files.exists(path)) {
                try {
                    String json = Files.readString(path, StandardCharsets.UTF_8);
                    if (json != null && !json.isBlank()) {
                        configs = objectMapper.readValue(json, new TypeReference<List<DbConfig>>() {
                        });
                    }
                } catch (IOException e) {
                    Messages.showErrorDialog(project, "读取数据库配置文件失败: " + e.getMessage(), "错误");
                }
            }

            // 分组
            Map<String, List<DbConfig>> typeToConfigs = configs.stream()
                    .filter(c -> c.getUrl() != null)
                    .collect(Collectors.groupingBy(c -> parseDbType(c.getUrl())));

            if (typeToConfigs.isEmpty()) {
                typeToConfigs.put("mysql", List.of(new DbConfig(DEFAULT_MYSQL, "", "")));
            }

            // UI组件
            String[] dbTypes = typeToConfigs.keySet().toArray(new String[0]);
            JComboBox<String> dbTypeComboBox = new JComboBox<>(dbTypes);
            JComboBox<String> configComboBox = new JComboBox<>();
            JTextField urlField = new JTextField();
            JTextField usernameField = new JTextField();
            JPasswordField passwordField = new JPasswordField();

            // 删除按钮（➖）
            JButton deleteButton = new JButton("➖");
            deleteButton.setToolTipText("删除当前数据源");
            deleteButton.setPreferredSize(new Dimension(45, 24));

            // 刷新配置列表下拉框
            Runnable refreshConfigList = () -> {
                configComboBox.removeAllItems();
                String selectedType = (String) dbTypeComboBox.getSelectedItem();
                if (selectedType != null && typeToConfigs.containsKey(selectedType)) {
                    List<DbConfig> list = typeToConfigs.get(selectedType);
                    for (DbConfig cfg : list) {
                        configComboBox.addItem(cfg.getUrl());
                    }
                    if (!list.isEmpty()) {
                        configComboBox.setSelectedIndex(0);
                        DbConfig first = list.get(0);
                        urlField.setText(first.getUrl());
                        usernameField.setText(first.getUsername());
                        passwordField.setText(first.getPassword());
                    }
                }
            };

            // 删除按钮逻辑
            deleteButton.addActionListener(e -> {
                String selectedType = (String) dbTypeComboBox.getSelectedItem();
                int selectedIndex = configComboBox.getSelectedIndex();

                if (selectedType != null && selectedIndex >= 0 && typeToConfigs.containsKey(selectedType)) {
                    List<DbConfig> list = typeToConfigs.get(selectedType);
                    if (selectedIndex >= list.size()) return;
                    DbConfig toRemove = list.get(selectedIndex);

                    int confirm = JOptionPane.showConfirmDialog(null,
                            "确认删除该数据源配置？\n" + toRemove.getUrl(),
                            "确认删除", JOptionPane.YES_NO_OPTION);

                    if (confirm == JOptionPane.YES_OPTION) {
                        list.remove(selectedIndex);

                        // 写回 JSON 文件
                        try {
                            List<DbConfig> allConfigs = typeToConfigs.values().stream()
                                    .flatMap(Collection::stream).collect(Collectors.toList());
                            String newJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(allConfigs);
                            Files.writeString(path, newJson, StandardCharsets.UTF_8);
                        } catch (IOException ex) {
                            Messages.showErrorDialog(project, "写入配置文件失败: " + ex.getMessage(), SQL_ERROR_TITLE);
                        }

                        refreshConfigList.run();
                    }
                }
            });

            // 监听选择变化
            dbTypeComboBox.addActionListener(e -> refreshConfigList.run());
            configComboBox.addActionListener(e -> {
                String selectedType = (String) dbTypeComboBox.getSelectedItem();
                int idx = configComboBox.getSelectedIndex();
                if (selectedType != null && idx >= 0 && typeToConfigs.containsKey(selectedType)) {
                    List<DbConfig> list = typeToConfigs.get(selectedType);
                    if (idx < list.size()) {
                        DbConfig selected = list.get(idx);
                        urlField.setText(selected.getUrl());
                        usernameField.setText(selected.getUsername());
                        passwordField.setText(selected.getPassword());
                    }
                }
            });

            // 初始化
            dbTypeComboBox.setSelectedIndex(0);
            refreshConfigList.run();

            // 构造 UI 面板
            JPanel panel = new JPanel(new GridLayout(0, 1));
            panel.setPreferredSize(new Dimension(500, 300));
            panel.add(new JLabel("数据库类型:"));
            panel.add(dbTypeComboBox);
            panel.add(new JLabel("具体配置:"));

            // 下拉框和删除按钮放一行
            JPanel configLine = new JPanel(new BorderLayout());
            configLine.add(configComboBox, BorderLayout.CENTER);
            configLine.add(deleteButton, BorderLayout.EAST);
            panel.add(configLine);

            panel.add(new JLabel("数据库 URL:"));
            panel.add(urlField);
            panel.add(new JLabel("用户名:"));
            panel.add(usernameField);
            panel.add(new JLabel("密码:"));
            panel.add(passwordField);

            // 循环弹窗
            while (true) {
                int result = JOptionPane.showConfirmDialog(
                        null,
                        panel,
                        "数据库配置",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE
                );

                if (result != JOptionPane.OK_OPTION) break;

                String dbType = ((String) dbTypeComboBox.getSelectedItem()).toLowerCase();
                String url = urlField.getText().trim();
                String username = usernameField.getText().trim();
                String password = new String(passwordField.getPassword()).trim();

                List<String> errorList = new ArrayList<>();
                if (!StringUtils.notBlankAndNotNullStr(url)) errorList.add("数据库 URL");
                if (!StringUtils.notBlankAndNotNullStr(username)) errorList.add("用户名");
                if (!StringUtils.notBlankAndNotNullStr(dbType)) errorList.add("数据库类型");

                if (errorList.isEmpty()) {
                    url += appendUrlFix(parseDbType(url), url);
                    callback.accept(new DbConfig(url, username, password));
                    break;
                } else {
                    Messages.showErrorDialog(project,
                            "以下字段不能为空：\n" + String.join("、", errorList),
                            "输入不完整");
                }
            }
        });
    }


    private static void promptUserInputInternal(Project project, Consumer<DbConfig> callback) {
        ApplicationManager.getApplication().invokeLater(() -> {
            JTextField urlField = new JTextField(DEFAULT_MYSQL);
            JTextField usernameField = new JTextField();
            JPasswordField passwordField = new JPasswordField();

            JPanel panel = new JPanel(new GridLayout(0, 1));
            panel.setPreferredSize(new Dimension(300, 200));
            panel.add(new JLabel("数据库 URL:"));
            panel.add(urlField);
            panel.add(new JLabel("用户名:"));
            panel.add(usernameField);
            panel.add(new JLabel("密码:"));
            panel.add(passwordField);


            int result = JOptionPane.showConfirmDialog(
                    null,
                    panel,
                    "请输入数据库配置",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            );


            if (result != JOptionPane.OK_OPTION) {
                // 用户点击了“取消”或关闭了窗口，直接返回，不继续执行下面的逻辑
                return;
            }
            String url = urlField.getText().trim();
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword()).trim();

            List<String> errorList = new ArrayList<>();
            if (!StringUtils.notBlankAndNotNullStr(url)) {
                errorList.add("数据库 URL");
            }
            if (!StringUtils.notBlankAndNotNullStr(username)) {
                errorList.add("用户名");
            }

            if (errorList.isEmpty()) {
                url += appendUrlFix(parseDbType(url), url);
                callback.accept(new DbConfig(url, username, password));
                saveToCache(project, new DbConfig(url, username, password));
                saveToCacheAppend(project, new DbConfig(url, username, password));
            } else {
                Messages.showErrorDialog(project,
                        "以下字段不能为空：\n" + String.join("、", errorList),
                        "输入不完整");
            }

        });
    }


    private static void promptUserAddInternal(Project project, Consumer<DbConfig> callback) {
        ApplicationManager.getApplication().invokeLater(() -> {
            JTextField urlField = new JTextField(DEFAULT_MYSQL);
            JTextField usernameField = new JTextField();
            JPasswordField passwordField = new JPasswordField();

            JPanel panel = new JPanel(new GridLayout(0, 1));
            panel.setPreferredSize(new Dimension(300, 200));
            panel.add(new JLabel("数据库 URL:"));
            panel.add(urlField);
            panel.add(new JLabel("用户名:"));
            panel.add(usernameField);
            panel.add(new JLabel("密码:"));
            panel.add(passwordField);

            int result = JOptionPane.showConfirmDialog(
                    null,
                    panel,
                    "请输入数据库配置",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            );
            if (result != JOptionPane.OK_OPTION) {
                // 用户点击了“取消”或关闭了窗口，直接返回，不继续执行下面的逻辑
                return;
            }
            String url = urlField.getText().trim();
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword()).trim();

            List<String> errorList = new ArrayList<>();
            if (!StringUtils.notBlankAndNotNullStr(url)) {
                errorList.add("数据库 URL");
            }
            if (!StringUtils.notBlankAndNotNullStr(username)) {
                errorList.add("用户名");
            }

            if (errorList.isEmpty()) {
                url += appendUrlFix(parseDbType(url), url);
                System.out.println(url);
                callback.accept(new DbConfig(url, username, password));
                saveToCache(project, new DbConfig(url, username, password));
                saveToCacheAppend(project, new DbConfig(url, username, password));
            } else {
                Messages.showErrorDialog(project,
                        "以下字段不能为空：\n" + String.join("、", errorList),
                        "输入不完整");
            }

        });
    }


    /**
     * 弹出同步阻塞输入框，直到用户输入完整数据库 URL 和用户名或取消
     *
     * @param project 当前项目
     * @return 用户输入的数据库配置，用户取消返回 null
     */
    public static DbConfig promptUserInputSync(Project project) {
        JTextField urlField = new JTextField(DEFAULT_MYSQL);
        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();

        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.setPreferredSize(new Dimension(300, 200));
        panel.add(new JLabel("数据库 URL:"));
        panel.add(urlField);
        panel.add(new JLabel("用户名:"));
        panel.add(usernameField);
        panel.add(new JLabel("密码:"));
        panel.add(passwordField);

        while (true) {
            int result = JOptionPane.showConfirmDialog(
                    null,
                    panel,
                    "请输入数据库配置",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            );

            if (result != JOptionPane.OK_OPTION) {
                return null; // 用户取消
            }

            String url = urlField.getText().trim();
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword()).trim();

            List<String> errorList = new ArrayList<>();
            if (!StringUtils.notBlankAndNotNullStr(url)) {
                errorList.add("数据库 URL");
            }
            if (!StringUtils.notBlankAndNotNullStr(username)) {
                errorList.add("用户名");
            }

            if (errorList.isEmpty()) {
                url += appendUrlFix(parseDbType(url), url);
                saveToCache(project, new DbConfig(url, username, password));
                saveToCacheAppend(project, new DbConfig(url, username, password));
                return new DbConfig(url, username, password);
            } else {
                Messages.showErrorDialog(project,
                        "以下字段不能为空：\n" + String.join("、", errorList),
                        "输入不完整");
            }
        }
    }


    /**
     * 保存数据库配置到缓存文件，写成 JSON 格式，并测试数据库连接有效性
     *
     * @param project 当前项目
     * @param config  数据库配置对象
     */
    public static boolean saveToCache(Project project, DbConfig config) {
        Path path = Paths.get(project.getBasePath(), CONFIG_PATH);

        try {
            // 优先判断：如果已有缓存且内容一致，则跳过所有操作，提升性能
            if (Files.exists(path)) {
                String oldJson = Files.readString(path, StandardCharsets.UTF_8);
                DbConfig oldConfig = objectMapper.readValue(oldJson, DbConfig.class);
                if (Objects.equals(oldConfig, config)) {
                    String yamlTemplate = SoarYamlUtil.readYamlTemplate("soar.yaml");
                    String replacedYaml  = SoarYamlUtil.replaceDbConfigInYaml(config, yamlTemplate);
                    Path ideaDir = Paths.get(project.getBasePath(), ".idea");
                    SoarYamlUtil.copySoarExeToDir(ideaDir);
                    String userProjectPath = project.getBasePath();
                    SoarYamlUtil.writeYamlToProjectIdea(userProjectPath, replacedYaml);
                    // 配置一致，无需保存
                    return true;
                }
            }

            // 配置有变化，先测试连接
            if (config != null && !JdbcTableInfoUtil.testConnection(config)) {
                return false;
            }

            // 写入新配置
            Files.createDirectories(path.getParent());
            String json = objectMapper.writeValueAsString(config);
            Files.writeString(path, json, StandardCharsets.UTF_8);
            String yamlTemplate = SoarYamlUtil.readYamlTemplate("soar.yaml");
            String replacedYaml  = SoarYamlUtil.replaceDbConfigInYaml(config, yamlTemplate);
            Path ideaDir = Paths.get(project.getBasePath(), ".idea");
            SoarYamlUtil.copySoarExeToDir(ideaDir);
            String userProjectPath = project.getBasePath();
            SoarYamlUtil.writeYamlToProjectIdea(userProjectPath, replacedYaml);
            return true;

        } catch (Exception e) {
            return false;
        }
    }
    public static void updateYamlConfig(String yamlPath, DbConfig config) throws IOException {
        String yaml = Files.readString(Paths.get(yamlPath));

        // 假设你能从 config 中拿到 url, user, password
        String url = config.getUrl();
        String user = config.getUsername();
        String password = config.getPassword();

        // 解析出 addr 和 schema（示例逻辑）
        String addr = url.substring(url.indexOf("//") + 2, url.indexOf("/", url.indexOf("//") + 2));
        String schema = url.substring(url.indexOf("/", url.indexOf("//") + 2) + 1,
                url.contains("?") ? url.indexOf("?") : url.length());

        // 替换 YAML 内容（保留缩进）
        yaml = yaml.replaceAll("(?m)^([ \\t]*)addr:\\s*.*", "$1addr: " + addr);
        yaml = yaml.replaceAll("(?m)^([ \\t]*)schema:\\s*.*", "$1schema: " + schema);
        yaml = yaml.replaceAll("(?m)^([ \\t]*)user:\\s*.*", "$1user: " + user);
        yaml = yaml.replaceAll("(?m)^([ \\t]*)password:\\s*.*", "$1password: " + password);

        // 写回文件
        Files.writeString(Paths.get(yamlPath), yaml);
    }

    public static boolean saveToCacheAppend(Project project, DbConfig newConfig) {
        Path path = Paths.get(project.getBasePath(), CONFIG_PATH_ALL);
        try {
            List<DbConfig> configList = new ArrayList<>();

            // 读取已有配置列表
            if (Files.exists(path)) {
                String json = Files.readString(path, StandardCharsets.UTF_8);
                if (json != null && !json.isBlank()) {
                    // 反序列化成列表
                    configList = objectMapper.readValue(json, new TypeReference<List<DbConfig>>() {
                    });
                }
            }

            // 判断是否已有相同配置，避免重复添加（可根据需要定义相等逻辑）
            boolean exists = configList.stream().anyMatch(c -> c.equals(newConfig));
            if (exists) {
                return true; // 已存在，不重复添加
            }

            // 测试新配置连接
            if (!JdbcTableInfoUtil.testConnection(newConfig)) {
                return false;
            }

            // 添加新配置
            configList.add(newConfig);

            // 保存回文件
            Files.createDirectories(path.getParent());
            String newJson = objectMapper.writeValueAsString(configList);
            Files.writeString(path, newJson, StandardCharsets.UTF_8);

            return true;
        } catch (Exception e) {
            return false;
        }
    }


    /**
     * 从缓存文件中读取数据库配置（JSON格式）
     *
     * @param project 当前项目
     * @return 配置对象，读取失败或文件不存在返回 null
     */
    public static DbConfig loadFromCache(Project project) {
        try {
            Path path = Paths.get(project.getBasePath(), CONFIG_PATH);
            if (!Files.exists(path)) return null;
            String json = Files.readString(path, StandardCharsets.UTF_8);
            return objectMapper.readValue(json, DbConfig.class);
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }
        return null;
    }

    public static String parseDbType(String url) {
        if (url == null || !url.startsWith("jdbc:")) {
            throw new IllegalArgumentException("无效的 JDBC URL: " + url);
        }
        return url.split(":")[1].toLowerCase();
    }
}
