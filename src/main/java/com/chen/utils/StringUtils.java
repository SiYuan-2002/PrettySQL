package com.chen.utils;

/**
 * @author czh
 * @version 1.0
 * @description:
 * @date 2025/6/12 15:59
 */
public class StringUtils {
    /**
     * 判断字符串不为 null、不为 "null"（忽略大小写）、不为 "" 或仅空白
     *
     * @param str 待判断字符串
     * @return 合法返回 true，否则 false
     */
    public static boolean notBlankAndNotNullStr(String str) {
        return str != null && !"null".equalsIgnoreCase(str.trim()) && !str.trim().isEmpty();
    }

    /**
     * 判断字符串为 null、"null"（忽略大小写）、"" 或仅空白
     *
     * @param str 待判断字符串
     * @return 无效返回 true，否则 false
     */
    public static boolean isBlankOrNullStr(String str) {
        return str == null || "null".equalsIgnoreCase(str.trim()) || str.trim().isEmpty();
    }

    /**
     * 去除字符串首尾空白并安全返回，若为 null 则返回空串
     *
     * @param str 待处理字符串
     * @return 去空白后字符串或 ""
     */
    public static String trimToEmpty(String str) {
        return str == null ? "" : str.trim();
    }

    /**
     * 判断两个字符串相等，忽略大小写和首尾空白，支持 null
     *
     * @param str1 字符串1
     * @param str2 字符串2
     * @return 相等返回 true，否则 false
     */
    public static boolean equalsIgnoreCaseAndBlank(String str1, String str2) {
        if (str1 == null && str2 == null) return true;
        if (str1 == null || str2 == null) return false;
        return str1.trim().equalsIgnoreCase(str2.trim());
    }


}
