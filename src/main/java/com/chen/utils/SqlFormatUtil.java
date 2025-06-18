package com.chen.utils;

import com.github.vertical_blank.sqlformatter.SqlFormatter;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * SQL 格式化工具类。
 * <p>
 * 基于第三方库 {@code vertical-blank/sql-formatter}，可根据指定方言对 SQL 字符串进行美化格式化，
 * 使 SQL 更易阅读，适用于开发工具、日志展示、SQL 编辑器等场景。
 * 如果传入方言无效或格式化失败，将原样返回 SQL 字符串。
 * @author czh
 * @version 1.0
 * @since 2025/6/12
 */
public class SqlFormatUtil {

    /**
     * 根据指定 SQL 方言对 SQL 字符串进行格式化美化处理。
     *
     * @param sql     原始 SQL 字符串（非空）
     * @param dialect SQL 方言名称，如 "mysql"、"postgresql"、"sql" 等
     * @return 格式化后的 SQL 字符串；如果格式化失败，返回原始 SQL
     */
    public static String formatSql(String sql, String dialect) {
        try {
            // 1. 提取所有MyBatis标签（成对和自闭合）作为占位符，保存原始内容
            Map<String, String> placeholderMap = new LinkedHashMap<>();
            String regex = "<(\\w+)(\\s[^>]*)?>[\\s\\S]*?</\\1>|<\\w+[^>]*/?>";
            Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
            Matcher matcher = pattern.matcher(sql);

            int idx = 0;
            StringBuffer buffer = new StringBuffer();
            while (matcher.find()) {
                String tag = matcher.group();
                String key = "###PLACEHOLDER_" + idx++ + "###";
                placeholderMap.put(key, tag);
                matcher.appendReplacement(buffer, key);
            }
            matcher.appendTail(buffer);

            // 2. 格式化没有标签的SQL部分
            String formatted = SqlFormatter.of(dialect).format(buffer.toString());

            // 3. 逐个替换回原始标签
            for (Map.Entry<String, String> entry : placeholderMap.entrySet()) {
                String key = entry.getKey();
                String originalTag = entry.getValue();
                formatted = formatted.replaceFirst(Pattern.quote(key), Matcher.quoteReplacement(originalTag));
            }

            // 4. 标签缩进后处理
            String[] lines = formatted.split("\n");
            StringBuilder result = new StringBuilder();
            for (String line : lines) {
                String trim = line.trim();
                if (trim.startsWith("<") && trim.endsWith(">")) {
                    result.append("    ").append(trim).append("\n");
                } else if(trim.equals("")) {
                    result.append("\n");
                } else {
                    result.append(line).append("\n");
                }
            }
            return result.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return sql;
        }
    }






}
