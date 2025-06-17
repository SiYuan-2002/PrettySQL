package com.chen.entity;

import java.util.Objects;

/**
 * 数据库配置信息类
 * 包含数据库的连接地址、用户名和密码
 * 用于数据库连接的初始化配置
 *
 * @author czh
 * @version 1.0
 * @date 2025/6/12 14:45
 */
public class DbConfig {

    // 数据库连接地址
    private String url;

    // 数据库用户名
    private String username;

    // 数据库密码
    private String password;

    public DbConfig() {
    }

    /**
     * 构造方法，初始化数据库配置
     *
     * @param url      数据库连接地址
     * @param username 数据库用户名
     * @param password 数据库密码
     */
    public DbConfig(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }

    /**
     * 获取数据库连接地址
     *
     * @return url
     */
    public String getUrl() {
        return url;
    }

    /**
     * 设置数据库连接地址
     *
     * @param url 数据库地址
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * 获取数据库用户名
     *
     * @return 用户名
     */
    public String getUsername() {
        return username;
    }

    /**
     * 设置数据库用户名
     *
     * @param username 用户名
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * 获取数据库密码
     *
     * @return 密码
     */
    public String getPassword() {
        return password;
    }

    /**
     * 设置数据库密码
     *
     * @param password 密码
     */
    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DbConfig dbConfig = (DbConfig) o;
        return Objects.equals(url, dbConfig.url) && Objects.equals(username, dbConfig.username) && Objects.equals(password, dbConfig.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, username, password);
    }
}
