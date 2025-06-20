package com.chen.utils;

import com.chen.entity.DbConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.chen.constant.FileConstant.SOARYMAL_PATH;
import static com.chen.constant.FileConstant.SQL_SCRIPT_FILE_NAME;

/**
 * @author czh
 * @version 1.0
 * @description:
 * @date 2025/6/20 8:23
 */
public class SoarYamlUtil {

    public static String readYamlTemplate(String resourcePath) throws IOException {
        try (InputStream is = SoarYamlUtil.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) throw new IOException("资源文件未找到: " + resourcePath);

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                return sb.toString();
            }
        }
    }
    /**
     * 复制resources目录下的soar.exe到目标目录
     * @param targetDir 目标目录路径，例如 .idea目录的绝对路径
     * @throws IOException
     */
    public static void copySoarExeToDir(Path targetDir) throws IOException {
        if (!Files.exists(targetDir)) {
            Files.createDirectories(targetDir);
        }
        Path targetFile = targetDir.resolve("soar.exe");
        if (Files.exists(targetFile)) {
            return;
        }
        // 资源路径（resources目录下的soar.exe）
        try (InputStream in = SoarYamlUtil.class.getClassLoader().getResourceAsStream("soar.exe")) {
            if (in == null) {
                throw new FileNotFoundException("资源目录下找不到 soar.exe");
            }

            Files.copy(in, targetFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }
    /**
     * 根据 json 配置替换 yaml 内容中的数据库连接信息
     * @param yamlTemplate yaml 模板内容字符串
     */
    public static String replaceDbConfigInYaml(DbConfig config, String yamlTemplate) {
        String url = config.getUrl();
        String username = config.getUsername(); // 或者 getUser()
        String password = config.getPassword();

        String addr = url.substring(url.indexOf("//") + 2, url.indexOf("/", url.indexOf("//") + 2));
        String schema = url.substring(url.indexOf("/", url.indexOf("//") + 2) + 1,
                url.contains("?") ? url.indexOf("?") : url.length());

        String replaced = yamlTemplate;
        replaced = replaced.replaceAll("(?m)^([ \\t]*)addr:\\s*.*", "$1addr: " + addr);
        replaced = replaced.replaceAll("(?m)^([ \\t]*)schema:\\s*.*", "$1schema: " + schema);
        replaced = replaced.replaceAll("(?m)^([ \\t]*)user:\\s*.*", "$1user: " + username);
        replaced = replaced.replaceAll("(?m)^([ \\t]*)password:\\s*.*", "$1password: " + password);

        return replaced;
    }


    /**
     * 写内容到用户项目的.idea目录下的soar.yaml
     * @param projectBasePath 用户项目根路径
     * @param content 替换好的 yaml 内容
     * @throws IOException
     */
    public static void writeYamlToProjectIdea(String projectBasePath, String content) throws IOException {
        Path yamlPath = Paths.get(projectBasePath, SOARYMAL_PATH);
        Files.createDirectories(yamlPath.getParent());
        Files.writeString(yamlPath, content, StandardCharsets.UTF_8);
    }

    /**
     * 把 sqlContent 写入到用户项目 .idea/idea.sql 文件
     * @param sqlContent SQL 脚本内容
     * @throws IOException
     */
    public static void writeSqlToIdea(Project project, String sqlContent) throws IOException {
        String projectBasePath = project.getBasePath();
        Path sqlFilePath = Paths.get(projectBasePath, SQL_SCRIPT_FILE_NAME);
        Files.createDirectories(sqlFilePath.getParent()); // 确保.idea目录存在
        Files.writeString(sqlFilePath, sqlContent, StandardCharsets.UTF_8);
    }

    public static String runSoarFixed(Project project) throws Exception {
        String projectBasePath = project.getBasePath();
        Path basePath = Paths.get(projectBasePath);

        // 构建 soar.exe 路径和 SQL 文件路径
        Path soarExePath = basePath.resolve(".idea").resolve("soar.exe");
        Path sqlFilePath = basePath.resolve(".idea").resolve("executeSqlFile.sql");

        System.out.println("执行路径: " + soarExePath);
        System.out.println("SQL文件路径: " + sqlFilePath);

        // 构建命令参数列表
        List<String> command = new ArrayList<>();
        command.add(soarExePath.toString());
        command.add("-query=" + sqlFilePath.toString());

        // 创建 ProcessBuilder
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true); // 合并标准错误输出

        Process process = null;
        StringBuilder output = new StringBuilder();

        try {
            process = builder.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                    output.append(line).append(System.lineSeparator());
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                output.append("进程异常退出，退出码: ").append(exitCode).append(System.lineSeparator());
            }

        } catch (Exception e) {
            e.printStackTrace();
            output.append("执行异常: ").append(e.getMessage()).append(System.lineSeparator());
        } finally {
            if (process != null) {
                process.destroy();
            }
        }

        return output.toString();
    }

    public static void downHtml(Project project) throws Exception {
        String projectBasePath = project.getBasePath();
        if (projectBasePath == null) {
            throw new IllegalArgumentException("项目路径不能为空");
        }

        Path ideaDir = Paths.get(projectBasePath, ".idea");
        File soarExe = ideaDir.resolve("soar.exe").toFile();
        File sqlFile = ideaDir.resolve("executeSqlFile.sql").toFile();

        if (!soarExe.exists() || !sqlFile.exists()) {
            throw new FileNotFoundException("soar.exe 或 SQL 文件不存在！");
        }

        // 1. 结果目录 D:\result
        File resultDir = new File("D:\\result");
        if (!resultDir.exists()) {
            if (!resultDir.mkdirs()) {
                throw new IOException("无法创建目录 D:\\result");
            }
        }

        // 2. 构建输出文件名，包含当前时间
        String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new java.util.Date());
        File outputFile = new File(resultDir, "SQL分析报告_" + timestamp + ".html");

        // 3. 创建空文件（如果不存在）
        if (!outputFile.exists()) {
            if (!outputFile.createNewFile()) {
                throw new IOException("无法创建报告文件：" + outputFile.getAbsolutePath());
            }
        }

        // 4. 执行 soar 分析命令
        ProcessBuilder pb = new ProcessBuilder(
                soarExe.getAbsolutePath(),
                "-query=" + sqlFile.getAbsolutePath()
        );
        pb.redirectErrorStream(true); // 合并错误输出

        Process process = pb.start();

        // 5. 将分析结果写入 HTML 文件
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(
                     new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                writer.write(line);
                writer.newLine();
            }
        }

        // 6. 校验退出码
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("执行失败，退出码：" + exitCode);
        }

        // 7. 成功提示
        Messages.showInfoMessage(project, "HTML 报告生成成功：\n" + outputFile.getAbsolutePath(), "生成成功");
    }

    public static void downMD(Project project) throws Exception {
        String projectBasePath = project.getBasePath();
        if (projectBasePath == null) {
            throw new IllegalArgumentException("项目路径不能为空");
        }

        Path ideaPath = Paths.get(projectBasePath, ".idea");
        Path yamlPath = ideaPath.resolve("soar.yaml");
        Path soarExePath = ideaPath.resolve("soar.exe");
        Path sqlFilePath = ideaPath.resolve("executeSqlFile.sql");

        if (!Files.exists(yamlPath) || !Files.exists(soarExePath) || !Files.exists(sqlFilePath)) {
            throw new FileNotFoundException("配置文件、soar.exe 或 SQL 文件不存在！");
        }

        // 读取 soar.yaml 并备份
        String yamlContent = Files.readString(yamlPath, StandardCharsets.UTF_8);
        String backupContent = yamlContent;

        // 生成结果目录和带时间的文件名
        File resultDir = new File("D:\\result");
        if (!resultDir.exists() && !resultDir.mkdirs()) {
            throw new IOException("无法创建结果目录 D:\\result");
        }

        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        File outputMd = new File(resultDir, "SQL分析报告_" + timestamp + ".md");

        try {
            // 修改配置文件：report-type 改为 markdown
            String modified = yamlContent.replace("report-type: html", "report-type: markdown");
            Files.writeString(yamlPath, modified, StandardCharsets.UTF_8);

            // 构造命令
            ProcessBuilder pb = new ProcessBuilder(
                    soarExePath.toAbsolutePath().toString(),
                    "-query=" + sqlFilePath.toAbsolutePath()
            );
            pb.redirectErrorStream(true); // 合并错误输出

            Process process = pb.start();

            // 将结果写入 markdown 文件
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
                 BufferedWriter writer = new BufferedWriter(
                         new OutputStreamWriter(new FileOutputStream(outputMd), StandardCharsets.UTF_8))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    writer.write(line);
                    writer.newLine();
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("执行失败，退出码：" + exitCode);
            }

            Messages.showInfoMessage(project, "Markdown 报告生成成功：\n" + outputMd.getAbsolutePath(), "生成成功");

        } finally {
            // 恢复原始 soar.yaml 配置
            Files.writeString(yamlPath, backupContent, StandardCharsets.UTF_8);
        }
    }



}
