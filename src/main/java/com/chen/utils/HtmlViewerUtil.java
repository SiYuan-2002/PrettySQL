package com.chen.utils;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.JBPopup;
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
    public static void showHtml(Project project, String html, String title, @Nullable String markdownReportHtml, boolean isReportPage) {
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
                showHtml(project, markdownReportHtml, "SQL分析报告", null, true);
            });
            buttonPanel.add(viewReportBtn);
        }

        if (isReportPage) {
            JButton rewriteSQLBtn = new JButton("SQL重写");
            rewriteSQLBtn.addActionListener(e -> {
                try {
                    String rewriteSQL = SoarYamlUtil.rewriteSQL(project);
                    Messages.showInfoMessage(project, rewriteSQL, "重写成功");
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            });
            buttonPanel.add(rewriteSQLBtn);

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

        // 添加关闭按钮，必须点击它才能关闭
        JButton closeBtn = new JButton("关闭");
        buttonPanel.add(closeBtn);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        JBPopup popup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(panel, null)
                .setTitle(title)
                .setResizable(true)
                .setMovable(true)
                .setRequestFocus(true)
                // 禁止点击弹窗外部关闭
                .setCancelOnClickOutside(false)
                // 禁止按ESC关闭
                .setCancelOnWindowDeactivation(false)
                .createPopup();

        // 点击关闭按钮时关闭弹窗
        closeBtn.addActionListener(e -> popup.cancel());

        popup.showInFocusCenter();
    }




}
