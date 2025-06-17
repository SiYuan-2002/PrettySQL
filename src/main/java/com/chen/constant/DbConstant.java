package com.chen.constant;

/**
 * @author czh
 * @version 1.0
 * @description: 默认数据库连接兼容常量
 * @date 2025/6/13 7:12
 */
public class DbConstant {

    public static final String MYSQL_URL_FIX = "?useSSL=false&useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useInformationSchema=false";
    public static final String SQLSERVER_URL_FIX = ";encrypt=true;trustServerCertificate=true";
    public static final String ORACLE_URL_FIX = "";
    public static final String DEFAULT_MYSQL = "jdbc:mysql://127.0.0.1:3306/db";


}
