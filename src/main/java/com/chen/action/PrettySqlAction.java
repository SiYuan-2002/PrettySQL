package com.chen.action;

import com.chen.entity.DbConfig;
import com.chen.utils.SqlFormatUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.stream.Collectors;

import static com.chen.utils.DbConfigUtil.loadFromCache;


/**
 * SQL 格式化操作类
 * 当用户选中 SQL 文本时，自动进行美化格式化，并替换选中内容
 * 适用于 IntelliJ Platform 插件开发
 *
 * @author czh
 * @version 1.0
 * @date 2025/6/12 14:47
 */
public class PrettySqlAction extends AnAction {

    /**
     * 执行格式化操作的入口方法
     *
     * @param e 当前触发动作的事件对象
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // 获取当前编辑器
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (editor == null) return;

        // 获取当前选中内容
        SelectionModel selectionModel = editor.getSelectionModel();
        String selectedText = selectionModel.getSelectedText();
        if (selectedText == null || selectedText.isEmpty()) return;
        DbConfig dbConfig = loadFromCache(e.getProject());
        // 使用工具类格式化 SQL 文本
        String formattedSql = SqlFormatUtil.formatSql(selectedText, dbConfig.getUrl().replaceFirst("^jdbc:(\\w+):.*", "$1"));

        // 设置缩进（这里是 11 个空格）
        String indent = "           ";

        // 为格式化后的每一行添加统一缩进
        String indentedSql = Arrays.stream(formattedSql.split("\n"))
                .map(line -> indent + line)
                .collect(Collectors.joining("\n"));

        // 获取选中文本的起止位置
        int start = selectionModel.getSelectionStart();
        int end = selectionModel.getSelectionEnd();

        Project project = e.getProject();

        // 执行替换操作，并支持撤销（IDE 操作必须在 WriteCommandAction 中执行）
        WriteCommandAction.runWriteCommandAction(project, () -> {
            editor.getDocument().replaceString(start, end, indentedSql);

            // 可选：将光标跳转到 SELECT 后的位置，便于继续编辑
            int newOffset = start + indentedSql.indexOf("SELECT");
            if (newOffset >= start) {
                CaretModel caretModel = editor.getCaretModel();
                caretModel.moveToOffset(newOffset + "SELECT".length() + 1);
            }
        });
    }
}
