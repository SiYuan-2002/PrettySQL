package com.chen.entity;

import java.util.List;

/**
 * 表结构元数据类
 * 包含表名、表注释、字段列表信息
 * 可用于数据库结构展示、SQL 解析等场景
 *
 * @author czh
 * @version 2.1
 * @date 2025/06/20
 */
public class TableMeta {

    /** 表名（英文名称） */
    private String tableName;

    /** 表备注（中文名称/说明） */
    private String tableComment;

    /** 表字段列表 */
    private List<ColumnMeta> columns;

    public TableMeta() {}

    public TableMeta(String tableName, String tableComment, List<ColumnMeta> columns) {
        this.tableName = tableName;
        this.tableComment = tableComment;
        this.columns = columns;
    }

    public String getTableName() {
        return tableName;
    }

    public TableMeta setTableName(String tableName) {
        this.tableName = tableName;
        return this;
    }

    public String getTableComment() {
        return tableComment;
    }

    public TableMeta setTableComment(String tableComment) {
        this.tableComment = tableComment;
        return this;
    }

    public List<ColumnMeta> getColumns() {
        return columns;
    }

    public TableMeta setColumns(List<ColumnMeta> columns) {
        this.columns = columns;
        return this;
    }

    @Override
    public String toString() {
        return "TableMeta{" +
                "tableName='" + tableName + '\'' +
                ", tableComment='" + tableComment + '\'' +
                ", columns=" + columns +
                '}';
    }
}
