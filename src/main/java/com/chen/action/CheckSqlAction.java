package com.chen.action;

import com.chen.constant.MessageConstants;
import com.chen.entity.DbConfig;
import com.chen.utils.SqlCheckUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

import static com.chen.utils.DbConfigUtil.*;
import static com.chen.utils.JdbcTableInfoUtil.testConnection;

/**
 * @author czh
 * @version 1.0
 * @description: SQL语法检查操作类
 * @date 2025/6/13 14:29
 */

public class CheckSqlAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR);

        if (editor == null) {
            Messages.showWarningDialog(project,
                    MessageConstants.SQL_WARNING_EDITOR_EMPTY,
                    MessageConstants.SQL_CHECK_TITLE);
            return;
        }

        SelectionModel selectionModel = editor.getSelectionModel();
        String sql = selectionModel.getSelectedText();
        if (sql == null || sql.trim().isEmpty()) {
            sql = editor.getDocument().getText();
        }

        if (sql == null || sql.trim().isEmpty()) {
            Messages.showWarningDialog(project,
                    MessageConstants.SQL_WARNING_SQL_EMPTY,
                    MessageConstants.SQL_CHECK_TITLE);
            return;
        }

        Optional<DbConfig> dbConfigOpt = Optional.ofNullable(loadFromCache(e.getProject()))
                .or(() -> Optional.ofNullable(tryLoadDbConfig(e.getProject())))
                .or(() -> Optional.ofNullable(promptUserInputSync(e.getProject())));

        if (!dbConfigOpt.isPresent()) {
            Messages.showErrorDialog(e.getProject(),
                    MessageConstants.SQL_ERROR_NO_DB_CONFIG,
                    MessageConstants.SQL_ERROR_TITLE);
            return;
        }

        DbConfig dbConfig = dbConfigOpt.get();

        try {
            if (!testConnection(dbConfig)) {
                Messages.showErrorDialog(e.getProject(),
                        MessageConstants.SQL_ERROR_CONNECTION_FAIL,
                        MessageConstants.SQL_ERROR_TITLE_CONNECTION_FAIL);
                return;
            }
        } catch (Exception ex) {
            Messages.showErrorDialog(e.getProject(),
                    "数据库连接异常：" + ex.getMessage(),
                    MessageConstants.SQL_ERROR_TITLE_CONNECTION_FAIL);
            return;
        }


        // 语法检查
        String syntaxResult = SqlCheckUtil.checkSQLWithRollback(dbConfig, sql);
        if (syntaxResult != null) {
            Messages.showErrorDialog(project,
                    syntaxResult,
                    MessageConstants.SQL_ERROR_SYNTAX);
            return;
        }

        // 危险SQL检查
        String dangerResult = SqlCheckUtil.checkDangerous(sql);
        if (dangerResult != null) {
            Messages.showWarningDialog(project,
                    dangerResult,
                    MessageConstants.SQL_WARNING_DANGER);
            return;
        }

        Messages.showInfoMessage(project,
                MessageConstants.SQL_SUCCESS,
                MessageConstants.SQL_SUCCESS_TITLE);
    }
}
