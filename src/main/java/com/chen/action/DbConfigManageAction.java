package com.chen.action;

import com.chen.constant.MessageConstants;
import com.chen.utils.SoarYamlUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;
import static com.chen.constant.MessageConstants.*;
import static com.chen.utils.DbConfigUtil.promptUserInputWithDbType;
import static com.chen.utils.DbConfigUtil.saveToCache;
import static com.chen.utils.JdbcTableInfoUtil.testConnection;

/**
 * @author czh
 * @version 1.0
 * @description: 数据库配置管理操作类
 * @date 2025/6/14 10:58
 */
public class DbConfigManageAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent element) {
        Project project = element.getProject();
        if (project == null) {
            return;
        }
        promptUserInputWithDbType(element.getProject(), config -> {
            try {
                if (!testConnection(config)) {
                    Messages.showErrorDialog(element.getProject(),
                            MessageConstants.SQL_ERROR_CONNECTION_FAIL,
                            MessageConstants.SQL_ERROR_TITLE_CONNECTION_FAIL);
                    return;
                }
            } catch (Exception ex) {
                Messages.showErrorDialog(element.getProject(),
                        "数据库连接异常：" + ex.getMessage(),
                        MessageConstants.SQL_ERROR_TITLE_CONNECTION_FAIL);
                return;
            }

            if (saveToCache(element.getProject(), config)) {
                Messages.showInfoMessage(CONFIG_SWITCH_SUCCESS_MESSAGE,
                        CONFIG_SAVE_SUCCESS_TITLE
                );
            }
        });
    }
}

