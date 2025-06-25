package com.chen.utils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author czh
 * @version 1.6
 * @description: SQL 参数工具类。包含参数提取、参数替换、if判断、foreach展开、set块修正、输入为空则跳过渲染（即便只判断 != null）
 * @date 2025/6/25
 */
public class SqlParamUtils {

    public static final String PARAM_REGEX = "[#\\$]\\{(\\w+(?:\\.\\w+)*)}";
    public static final String IF_BLOCK_REGEX = "<if\\s+test=\"([^\"]+)\">([\\s\\S]*?)</if>";
    public static final String FOREACH_REGEX = "<foreach[^>]*collection=\"(\\w+)\"[^>]*>([\\s\\S]*?)</foreach>";
    public static final String SQL_WHITESPACE_REGEX = "[\\t ]+";
    public static final String SQL_MULTILINE_REGEX = "(\\r?\\n){2,}";
    public static final String SET_BLOCK_REGEX = "<set>([\\s\\S]*?)</set>";

    public static Set<String> extractParams(String sql) {
        Set<String> params = new LinkedHashSet<>();
        Matcher matcher = Pattern.compile(PARAM_REGEX).matcher(sql);
        while (matcher.find()) params.add(matcher.group(1));
        return params;
    }

    public static String buildFinalSql(String sql, Map<String, Object> paramValues) {
        sql = processIfBlocks(sql, paramValues);
        sql = processForeachBlocks(sql, paramValues);
        sql = processSetBlocks(sql);

        for (Map.Entry<String, Object> entry : paramValues.entrySet()) {
            String key = entry.getKey();
            Object valObj = entry.getValue();
            if (valObj == null || valObj.toString().trim().isEmpty()) {
                sql = sql.replaceAll("[#\\$]\\{" + Pattern.quote(key) + "}", "");
                continue;
            }
            String value = String.valueOf(valObj);
            sql = sql.replaceAll("#\\{" + Pattern.quote(key) + "}", isNumber(value) ? value : ("'" + value + "'"));
            sql = sql.replaceAll("\\$\\{" + Pattern.quote(key) + "}", value);
        }
        return sql.replaceAll("</?if.*?>", "")
                .replaceAll(SQL_WHITESPACE_REGEX, " ")
                .replaceAll(SQL_MULTILINE_REGEX, "\n")
                .trim();
    }

    public static boolean isNumber(String s) {
        return s != null && s.matches("^-?\\d+(\\.\\d+)?$");
    }

    public static String processIfBlocks(String sql, Map<String, ?> paramValues) {
        Pattern ifBlock = Pattern.compile(IF_BLOCK_REGEX, Pattern.DOTALL);
        Matcher matcher = ifBlock.matcher(sql);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String condition = matcher.group(1);
            String blockSql = matcher.group(2);
            boolean show = evalCondition(condition, paramValues);
            matcher.appendReplacement(sb, show ? Matcher.quoteReplacement(blockSql) : "");
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    public static boolean evalCondition(String condition, Map<String, ?> paramValues) {
        String[] ands = condition.split("and");
        for (String cond : ands) {
            cond = cond.trim();
            Matcher keyMatch = Pattern.compile("(\\w+)").matcher(cond);
            if (keyMatch.find()) {
                String key = keyMatch.group(1);
                Object val = paramValues.get(key);
                if (cond.contains("!= null")) {
                    if (val == null || val.toString().trim().isEmpty()) return false;
                } else if (cond.contains("!= ''")) {
                    if (val == null || val.toString().trim().isEmpty()) return false;
                } else if (cond.contains("!= 0")) {
                    if (val == null || "0".equals(val.toString().trim())) return false;
                }
            }
        }
        return true;
    }

    public static String processForeachBlocks(String sql, Map<String, Object> paramValues) {
        // 这个正则匹配任意顺序的collection和item属性，open,separator,close属性可选，且支持换行和空白
        Pattern foreachPattern = Pattern.compile(
                "<foreach\\s+([^>]*collection=\"(\\w+)\")[^>]*item=\"(\\w+)\"[^>]*open=\"([^\"]*)\"[^>]*separator=\"([^\"]*)\"[^>]*close=\"([^\"]*)\"[^>]*>([\\s\\S]*?)</foreach>",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = foreachPattern.matcher(sql);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String collectionName = matcher.group(2);
            String itemAlias = matcher.group(3);
            String open = matcher.group(4);
            String separator = matcher.group(5);
            String close = matcher.group(6);
            String content = matcher.group(7);

            Object collectionObj = paramValues.get(collectionName);
            if (!(collectionObj instanceof List)) {
                // 如果不是 List，原样替换不变
                continue;
            }
            List<?> list = (List<?>) collectionObj;
            StringBuilder fragment = new StringBuilder();
            fragment.append(open);
            for (int i = 0; i < list.size(); i++) {
                Object item = list.get(i);
                String itemSql = content;

                if (item instanceof Map) {
                    for (Map.Entry<?, ?> en : ((Map<?, ?>) item).entrySet()) {
                        String key = en.getKey().toString();
                        String val = String.valueOf(en.getValue());
                        String pattern = "#\\{" + Pattern.quote(itemAlias) + "\\." + Pattern.quote(key) + "}";
                        itemSql = itemSql.replaceAll(pattern, isNumber(val) ? val : ("'" + val + "'"));
                    }
                } else {
                    String val = item.toString();
                    String pattern = "#\\{" + Pattern.quote(itemAlias) + "}";
                    itemSql = itemSql.replaceAll(pattern, isNumber(val) ? val : ("'" + val + "'"));
                }

                fragment.append(itemSql);
                if (i < list.size() - 1) fragment.append(separator);
            }
            fragment.append(close);

            matcher.appendReplacement(sb, Matcher.quoteReplacement(fragment.toString()));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }


    public static String processSetBlocks(String sql) {
        Pattern setBlock = Pattern.compile(SET_BLOCK_REGEX, Pattern.DOTALL);
        Matcher matcher = setBlock.matcher(sql);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String content = matcher.group(1).trim();
            String[] lines = content.split("\r?\n");
            StringBuilder cleaned = new StringBuilder();
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;
                cleaned.append(trimmed);
                if (!trimmed.endsWith(",")) cleaned.append(",");
                cleaned.append("\n");
            }
            if (cleaned.length() > 0 && cleaned.charAt(cleaned.length() - 2) == ',') {
                cleaned.deleteCharAt(cleaned.length() - 2);
            }
            matcher.appendReplacement(sb, "SET\n" + Matcher.quoteReplacement(cleaned.toString().trim()));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
