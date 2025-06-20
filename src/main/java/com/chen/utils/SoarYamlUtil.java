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
import java.util.Arrays;
import java.util.Date;
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

    public static void openExeNoMsg(String cmd) {
        BufferedReader br = null;
        BufferedReader brError = null;

        try {
            //执行exe  cmd可以为字符串(exe存放路径)也可为数组，调用exe时需要传入参数时，可以传数组调用(参数有顺序要求)
            Process p = Runtime.getRuntime().exec(cmd);
            String line = null;
            //获得子进程的输入流。
            br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            //获得子进程的错误流。
            brError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            while ((line = br.readLine()) != null  || (line = brError.readLine()) != null) {
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }



}
