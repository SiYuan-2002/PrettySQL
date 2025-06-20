package com.chen.utils;

import com.chen.entity.DbConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import com.intellij.openapi.project.Project;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
        Path sqlFilePath = Paths.get(projectBasePath);
        System.out.println("路线"+sqlFilePath);
        String paras = "-query="+sqlFilePath+"\\.idea\\executeSqlFile.sql";
        String cmd = sqlFilePath+"\\.idea\\soar.exe " + paras;

        return openExe(cmd);
    }

    public static String openExe(String cmd) {
        StringBuilder output = new StringBuilder();
        StringBuilder error = new StringBuilder();

        BufferedReader br = null;
        BufferedReader brError = null;
        Process p = null;

        try {
            p = Runtime.getRuntime().exec(cmd);

            // 读取标准输出流
            br = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8));
            // 读取错误输出流
            brError = new BufferedReader(new InputStreamReader(p.getErrorStream(), StandardCharsets.UTF_8));

            String line;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
                output.append(line).append(System.lineSeparator());
            }
            while ((line = brError.readLine()) != null) {
                System.err.println(line);
                error.append(line).append(System.lineSeparator());
            }

            int exitCode = p.waitFor();
            if (exitCode != 0) {
                output.append("进程异常退出，退出码: ").append(exitCode).append(System.lineSeparator());
                output.append("错误信息:").append(System.lineSeparator()).append(error);
            }
        } catch (Exception e) {
            e.printStackTrace();
            output.append("执行异常: ").append(e.getMessage()).append(System.lineSeparator());
        } finally {
            try {
                if (br != null) br.close();
                if (brError != null) brError.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (p != null) p.destroy();
        }

        return output.toString();
    }




}
