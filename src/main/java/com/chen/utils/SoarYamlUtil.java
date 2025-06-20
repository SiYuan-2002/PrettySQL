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

import static com.chen.constant.FileConstant.*;

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
     *
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
     *
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
     *
     * @param projectBasePath 用户项目根路径
     * @param content         替换好的 yaml 内容
     * @throws IOException
     */
    public static void writeYamlToProjectIdea(String projectBasePath, String content) throws IOException {
        Path yamlPath = Paths.get(projectBasePath, SOARYMAL_PATH);
        Files.createDirectories(yamlPath.getParent());
        Files.writeString(yamlPath, content, StandardCharsets.UTF_8);
    }

    /**
     * 把 sqlContent 写入到用户项目 .idea/idea.sql 文件
     *
     * @param sqlContent SQL 脚本内容
     * @throws IOException
     */
    public static void writeSqlToIdea(Project project, String sqlContent) throws IOException {
        String projectBasePath = project.getBasePath();
        Path sqlFilePath = Paths.get(projectBasePath, SQL_SCRIPT_FILE_NAME);
        Files.createDirectories(sqlFilePath.getParent()); // 确保.idea目录存在
        Files.writeString(sqlFilePath, sqlContent, StandardCharsets.UTF_8);
    }

    /**
     * 获取当前时间戳（用于生成报告文件名）
     */
    private static String getTimestamp() {
        return new SimpleDateFormat(DATE_FORMAT).format(new Date());
    }

    /**
     * 获取 .idea 目录下某个文件的完整路径
     */
    private static Path getIdeaPath(Project project, String fileName) {
        return Paths.get(project.getBasePath(), IDEA_DIR, fileName);
    }

    /**
     * 确保结果输出目录存在，若不存在则创建
     */
    private static File ensureResultDir(String file) throws IOException {
        File resultDir = new File(RESULT_DIR+file);
        if (!resultDir.exists() && !resultDir.mkdirs()) {
            throw new IOException("无法创建结果目录 " + RESULT_DIR);
        }
        return resultDir;
    }

    /**
     * 运行分析命令并返回控制台输出（不生成文件）
     */
    public static String runSoarFixed(Project project) throws Exception {
        Path soarExePath = getIdeaPath(project, SOAR_EXE_NAME);
        Path sqlFilePath = getIdeaPath(project, SQL_FILE_NAME);

        List<String> command = List.of(
                soarExePath.toString(),
                "-query=" + sqlFilePath
        );

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);

        StringBuilder output = new StringBuilder();
        Process process = null;

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
            if (process != null) process.destroy();
        }

        return output.toString();
    }

    /**
     * 执行分析结果保存为 HTML 文件
     */
    public static void downHtml(Project project) throws Exception {
        File soarExe = getIdeaPath(project, SOAR_EXE_NAME).toFile();
        File sqlFile = getIdeaPath(project, SQL_FILE_NAME).toFile();

        if (!soarExe.exists() || !sqlFile.exists()) {
            throw new FileNotFoundException("soar.exe 或 SQL 文件不存在！");
        }

        File resultDir = ensureResultDir("\\html");
        File outputFile = new File(resultDir, REPORT_PREFIX + getTimestamp() + HTML_SUFFIX);

        ProcessBuilder pb = new ProcessBuilder(
                soarExe.getAbsolutePath(),
                "-query=" + sqlFile.getAbsolutePath()
        );
        pb.redirectErrorStream(true);

        Process process = pb.start();

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

        if (process.waitFor() != 0) {
            throw new RuntimeException("执行失败，退出码：" + process.exitValue());
        }

        Messages.showInfoMessage(project, "HTML 报告生成成功：\n" + outputFile.getAbsolutePath(), "生成成功");
    }

    /**
     * 执行分析结果保存为 Markdown 文件
     * 会修改 soar.yaml 报告类型配置，完成后再还原
     */
    public static void downMD(Project project) throws Exception {
        Path yamlPath = getIdeaPath(project, YAML_FILE_NAME);
        Path soarExePath = getIdeaPath(project, SOAR_EXE_NAME);
        Path sqlFilePath = getIdeaPath(project, SQL_FILE_NAME);

        if (!Files.exists(yamlPath) || !Files.exists(soarExePath) || !Files.exists(sqlFilePath)) {
            throw new FileNotFoundException("配置文件、soar.exe 或 SQL 文件不存在！");
        }

        String originalYaml = Files.readString(yamlPath, StandardCharsets.UTF_8);
        String modifiedYaml = originalYaml.replace("report-type: html", "report-type: markdown");

        Files.writeString(yamlPath, modifiedYaml, StandardCharsets.UTF_8);

        File outputMd = new File(ensureResultDir("\\md"), REPORT_PREFIX + getTimestamp() + MD_SUFFIX);

        ProcessBuilder pb = new ProcessBuilder(
                soarExePath.toAbsolutePath().toString(),
                "-query=" + sqlFilePath.toAbsolutePath()
        );
        pb.redirectErrorStream(true);

        Process process = pb.start();

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

        if (process.waitFor() != 0) {
            throw new RuntimeException("执行失败，退出码：" + process.exitValue());
        }

        // 恢复原 YAML
        Files.writeString(yamlPath, originalYaml, StandardCharsets.UTF_8);

        Messages.showInfoMessage(project, "Markdown 报告生成成功：\n" + outputMd.getAbsolutePath(), "生成成功");
    }


}
