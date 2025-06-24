package com.chen.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;

/**
 * @author czh
 * @version 1.0
 * @description:
 * @date 2025/6/24 7:11
 */
public class ShowERDiagramAction extends AnAction {

        @Override
        public void actionPerformed(AnActionEvent e) {
            ToolWindow toolWindow = ToolWindowManager.getInstance(e.getProject()).getToolWindow("ER Diagram");
            if (toolWindow != null) {
                toolWindow.show();
            }
        }
    }


