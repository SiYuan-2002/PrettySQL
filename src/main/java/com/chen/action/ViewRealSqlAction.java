package com.chen.action;

import com.chen.dialog.ParamInputDialog;
import com.chen.dialog.SqlPreviewDialog;
import com.chen.utils.SqlParamUtils;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static com.chen.constant.MessageConstants.DIALOG_TITLE;
import static com.chen.constant.MessageConstants.MESSAGE_SELECT_SQL;

/**
 * 主 Action 类：用于处理 SQL 选中、参数提取、弹窗调用
 */
public class ViewRealSqlAction extends AnAction {


    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR);
        if (project == null || editor == null) return;

        // 获取 SQL 文本
        SelectionModel selectionModel = editor.getSelectionModel();
        String sql = selectionModel.getSelectedText();
        if (sql == null || sql.trim().isEmpty()) {
            Messages.showWarningDialog(project, MESSAGE_SELECT_SQL, DIALOG_TITLE);
            return;
        }

        // 解析 SQL 中的参数
        Set<String> params = SqlParamUtils.extractParams(sql);

        Map<String, Object> paramValues = Collections.emptyMap();
        if (!params.isEmpty()) {
            // 存在参数，弹参数输入框
            ParamInputDialog paramDialog = new ParamInputDialog(params, project);
            if (!paramDialog.showAndGet()) return;
            paramValues = paramDialog.getParamValues();
        }

        // 生成预览 SQL
        String resultSql = SqlParamUtils.buildFinalSql(sql, paramValues);

        // 弹出预览窗口
        SqlPreviewDialog previewDialog = new SqlPreviewDialog(project, resultSql);
        previewDialog.showAndGet();
    }
}