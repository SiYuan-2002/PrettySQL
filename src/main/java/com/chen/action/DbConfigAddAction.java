package com.chen.action;

import com.chen.constant.MessageConstants;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.nio.file.Paths;

import static com.chen.constant.FileConstant.CONFIG_PATH;
import static com.chen.constant.MessageConstants.*;
import static com.chen.utils.DbConfigUtil.*;
import static com.chen.utils.JdbcTableInfoUtil.testConnection;

/**
 * @author czh
 * @version 1.0
 * @description: 数据库配置添加操作类
 * @date 2025/6/14 10:23
 */
public class DbConfigAddAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent element) {
        Project project = element.getProject();
        if (project == null) {
            return;
        }
        promptUserAdd(element.getProject(), config -> {
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
                Path path = Paths.get(element.getProject().getBasePath(), CONFIG_PATH);
                Messages.showInfoMessage(
                        CONFIG_SAVE_SUCCESS_MESSAGE_PREFIX + path.toString() + CONFIG_SAVE_SUCCESS_MESSAGE_SUFFIX,
                        CONFIG_SAVE_SUCCESS_TITLE
                );
            }
        });
    }
}

