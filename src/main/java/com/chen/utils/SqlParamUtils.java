package com.chen.utils;

import java.util.*;
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

    /**
     * 提取 SQL 中的参数名
     *
     * @param sql SQL字符串
     * @return 参数名集合，按出现顺序
     */
    public static Set<String> extractParams(String sql) {
        Set<String> params = new LinkedHashSet<>();
        Matcher matcher = Pattern.compile(PARAM_REGEX).matcher(sql);
        while (matcher.find()) params.add(matcher.group(1));
        return params;
    }

    /**
     * 根据参数值渲染最终 SQL，支持 if/foreach/set 模板语法
     *
     * @param sql         SQL模板
     * @param paramValues 参数值映射
     * @return 渲染后的 SQL
     */
    public static String buildFinalSql(String sql, Map<String, Object> paramValues) {
        sql = processIfBlocks(sql, paramValues);
        sql = processForeachBlocks(sql, paramValues);
        sql = processSetBlocks(sql);
        sql= processWhereTag(sql);

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

    /**
     * 判断字符串是否为数字
     *
     * @param s 输入字符串
     * @return 是否数字
     */
    public static boolean isNumber(String s) {
        return s != null && s.matches("^-?\\d+(\\.\\d+)?$");
    }


    /**
     * 处理 SQL 里的 <if test=""> 块
     *
     * @param sql         SQL模板
     * @param paramValues 参数值映射
     * @return 处理后的 SQL
     */
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

    /**
     * 模拟 MyBatis 的 <where> 标签逻辑：
     * - 去除无用的 <where> 标签
     * - 自动添加 WHERE
     * - 移除第一个 AND 或 OR
     */
    public static String processWhereTag(String sql) {
        Pattern pattern = Pattern.compile("<where>(.*?)</where>", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(sql);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String body = matcher.group(1).trim();

            // 去除最前面的 and / or
            body = body.replaceFirst("(?i)^\\s*(and|or)\\s+", "");

            if (!body.isEmpty()) {
                matcher.appendReplacement(sb, "WHERE " + Matcher.quoteReplacement(body));
            } else {
                matcher.appendReplacement(sb, "");
            }
        }

        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 判断 if test 内的条件是否成立
     * 只支持 xx != null, xx != '', xx != 0 的多 and 条件
     *
     * @param condition   条件表达式
     * @param paramValues 参数值映射
     * @return 是否成立
     */
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

    /**
     * 处理带<foreach>的SQL模板，支持弹窗输入userId为单个或逗号分隔的字符串
     * @param sql SQL模板
     * @param paramValues 参数map(支持userIds传"1,2,3"或List)
     * @return 替换后的SQL
     */
    public static String processForeachBlocks(String sql, Map<String, Object> paramValues) {
        Pattern foreachPattern = Pattern.compile(
                "<foreach\\s+([^>]*)>([\\s\\S]*?)</foreach>",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = foreachPattern.matcher(sql);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String attrStr = matcher.group(1);
            String content = matcher.group(2);

            // 只关心item，不判定collection
            String itemAlias = getAttr(attrStr, "item");
            String open = getAttr(attrStr, "open", "(");
            String separator = getAttr(attrStr, "separator", ",");
            String close = getAttr(attrStr, "close", ")");

            // 只从paramValues取itemAlias
            Object valueObj = paramValues.get(itemAlias);
            List<String> list = parseList(valueObj);

            StringBuilder fragment = new StringBuilder();
            fragment.append(open);
            for (int i = 0; i < list.size(); i++) {
                String val = list.get(i);
                String itemSql = content;
                itemSql = itemSql.replace(
                        "#{" + itemAlias + "}",
                        isNumber(val) ? val : ("'" + val + "'")
                );
                itemSql = itemSql.replace(
                        "${" + itemAlias + "}",
                        val
                );
                fragment.append(itemSql.trim());
                if (i < list.size() - 1) fragment.append(separator);
            }
            fragment.append(close);

            matcher.appendReplacement(sb, Matcher.quoteReplacement(fragment.toString()));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static List<String> parseList(Object obj) {
        if (obj == null) return Collections.emptyList();
        if (obj instanceof List) {
            List<?> origin = (List<?>) obj;
            List<String> ret = new ArrayList<>();
            for (Object o : origin) ret.add(o == null ? "" : o.toString().trim());
            return ret;
        }
        if (obj instanceof String) {
            String s = ((String)obj).trim();
            if (s.isEmpty()) return Collections.emptyList();
            if (s.contains(",")) {
                List<String> ret = new ArrayList<>();
                for (String part : s.split(",")) ret.add(part.trim());
                return ret;
            } else {
                return Collections.singletonList(s);
            }
        }
        return Collections.singletonList(obj.toString().trim());
    }

    private static String getAttr(String attrStr, String attrName) {
        return getAttr(attrStr, attrName, "");
    }
    private static String getAttr(String attrStr, String attrName, String defaultVal) {
        Pattern p = Pattern.compile(attrName + "=\"([^\"]*)\"");
        Matcher m = p.matcher(attrStr);
        return m.find() ? m.group(1) : defaultVal;
    }



    /**
     * 处理 <set> 块，自动补全逗号，去除末尾多余逗号
     *
     * @param sql SQL模板
     * @return 处理后的 SQL
     */
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
