package com.chen.dialog;
import com.chen.constant.MessageConstants;
import com.chen.entity.DbConfig;
import com.chen.utils.SqlCheckUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.Optional;

import static com.chen.utils.DbConfigUtil.*;
import static com.chen.utils.JdbcTableInfoUtil.testConnection;
import static com.chen.constant.MessageConstants.*;
/**
 * @author czh
 * @version 1.0
 * @description: SQL 预览窗口。含“SQL执行检查”“复制”“关闭”等功能按钮
 * @date 2025/6/25 14:35
 */
public class SqlPreviewDialog extends DialogWrapper {

    private final Project project;
    private final String sql;
    private JTextArea textArea;

    /**
     * 构造方法
     */
    public SqlPreviewDialog(Project project, String sql) {
        super(project, true);
        this.project = project;
        this.sql = sql;
        setTitle("预览实际 SQL");
        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        textArea = new JTextArea(sql, Math.min(20, sql.split("\n").length + 1), 80);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setCaretPosition(0);
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(900, 400));
        return scrollPane;
    }

    @Override
    protected Action[] createActions() {
        return new Action[]{
                new AbstractAction(BTN_SQL_CHECK) {
                    @Override
                    public void actionPerformed(java.awt.event.ActionEvent e) {
                        doSqlCheck();
                    }
                },
                new AbstractAction(BTN_COPY) {
                    @Override
                    public void actionPerformed(java.awt.event.ActionEvent e) {
                        StringSelection selection = new StringSelection(textArea.getText());
                        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
                        Messages.showInfoMessage(project, COPY_SUCCESS_MSG, BTN_COPY);
                    }
                },
                getOKAction()
        };
    }

    /**
     * SQL执行检查逻辑，可根据实际需求扩展
     */
    private void doSqlCheck() {
        if (sql == null || sql.trim().isEmpty()) {
            Messages.showWarningDialog(project,
                    MessageConstants.SQL_WARNING_SQL_EMPTY,
                    MessageConstants.SQL_CHECK_TITLE);
            return;
        }

        Optional<DbConfig> dbConfigOpt = Optional.ofNullable(loadFromCache(project))
                .or(() -> Optional.ofNullable(tryLoadDbConfig(project)))
                .or(() -> Optional.ofNullable(promptUserInputSync(project)));

        if (!dbConfigOpt.isPresent()) {
            Messages.showErrorDialog(project,
                    MessageConstants.SQL_ERROR_NO_DB_CONFIG,
                    MessageConstants.SQL_ERROR_TITLE);
            return;
        }

        DbConfig dbConfig = dbConfigOpt.get();

        try {
            if (!testConnection(dbConfig)) {
                Messages.showErrorDialog(project,
                        MessageConstants.SQL_ERROR_CONNECTION_FAIL,
                        MessageConstants.SQL_ERROR_TITLE_CONNECTION_FAIL);
                return;
            }
        } catch (Exception ex) {
            Messages.showErrorDialog(project,
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
