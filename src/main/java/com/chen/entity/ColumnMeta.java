package com.chen.entity;

/**
 * 数据库字段元数据信息类
 * 用于描述表中的某一列，包括列名、类型、是否主键、备注等信息
 * 可用于代码生成、数据库结构读取等场景
 *
 * @author czh
 * @version 1.0
 * @date 2025/6/12 14:47
 */
public class ColumnMeta {

    // 字段名称
    private String name;

    // 字段类型（如 VARCHAR、INT、DATE 等）
    private String type;

    // 是否为主键
    private boolean primaryKey;

    // 字段备注（注释）
    private String remark;

    /**
     * 构造方法，初始化字段元数据
     *
     * @param name 字段名
     * @param type 字段类型
     * @param primaryKey 是否为主键
     * @param remark 字段备注
     */
    public ColumnMeta(String name, String type, boolean primaryKey, String remark) {
        this.name = name;
        this.type = type;
        this.primaryKey = primaryKey;
        this.remark = remark;
    }

    /**
     * 获取字段名称
     *
     * @return 字段名
     */
    public String getName() {
        return name;
    }

    /**
     * 设置字段名称
     *
     * @param name 字段名
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 获取字段类型
     *
     * @return 字段类型
     */
    public String getType() {
        return type;
    }

    /**
     * 设置字段类型
     *
     * @param type 字段类型
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * 是否为主键
     *
     * @return true 是主键，false 不是
     */
    public boolean isPrimaryKey() {
        return primaryKey;
    }

    /**
     * 设置是否为主键
     *
     * @param primaryKey 是否主键
     */
    public void setPrimaryKey(boolean primaryKey) {
        this.primaryKey = primaryKey;
    }

    /**
     * 获取字段备注
     *
     * @return 字段备注
     */
    public String getRemark() {
        return remark;
    }

    /**
     * 设置字段备注
     *
     * @param remark 字段备注
     */
    public void setRemark(String remark) {
        this.remark = remark;
    }
}
