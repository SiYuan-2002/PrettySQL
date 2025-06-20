package com.chen.utils;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import groovyjarjarantlr4.v4.runtime.misc.Nullable;

import javax.swing.*;
import java.awt.*;

public class HtmlViewerUtil {

    /**
     * 弹出一个简单的 HTML 查看窗口
     * @param html HTML 字符串内容
     * @param title 窗口标题
     */
    public static void showHtml(Project project,String html, String title, @Nullable String markdownReportHtml, boolean isReportPage) {
        JEditorPane editorPane = new JEditorPane("text/html", html);
        editorPane.setEditable(false);
        editorPane.setCaretPosition(0);

        JScrollPane scrollPane = new JScrollPane(editorPane);
        scrollPane.setPreferredSize(new Dimension(1500, 600));
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        // 如果是展示原始HTML的弹窗，才显示“查看分析报告和建议”按钮
        if (!isReportPage && markdownReportHtml != null) {
            JButton viewReportBtn = new JButton("查看分析报告和建议");
            viewReportBtn.addActionListener(e -> {
                // 这里打开的是报告页面（isReportPage = true）
                showHtml(project,markdownReportHtml, "SQL分析报告", null, true);
            });
            buttonPanel.add(viewReportBtn);
        }

        // 如果是报告页面，显示生成HTML和生成MD按钮
        if (isReportPage) {
            JButton generateHtmlBtn = new JButton("生成 HTML");
            generateHtmlBtn.addActionListener(e -> {
                try {
                    SoarYamlUtil.downHtml(project);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            });
            buttonPanel.add(generateHtmlBtn);

            JButton generateMdBtn = new JButton("生成 MD");
            generateMdBtn.addActionListener(e -> {
                try {
                    SoarYamlUtil.downMD(project);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            });
            buttonPanel.add(generateMdBtn);
        }

        if (buttonPanel.getComponentCount() > 0) {
            panel.add(buttonPanel, BorderLayout.SOUTH);
        }

        JBPopupFactory.getInstance()
                .createComponentPopupBuilder(panel, null)
                .setTitle(title)
                .setResizable(true)
                .setMovable(true)
                .setRequestFocus(true)
                .createPopup()
                .showInFocusCenter();
    }



}
