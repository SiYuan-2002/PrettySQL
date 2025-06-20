package com.chen.utils;

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
    public static void showHtml(String html, String title, @Nullable String markdownReportHtml) {
        JEditorPane editorPane = new JEditorPane("text/html", html);
        editorPane.setEditable(false);
        editorPane.setCaretPosition(0);

        JScrollPane scrollPane = new JScrollPane(editorPane);
        scrollPane.setPreferredSize(new Dimension(1500, 600));
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(scrollPane, BorderLayout.CENTER);

        if (markdownReportHtml != null) {
            JButton button = new JButton("查看分析报告和建议");
            button.addActionListener(e -> {
                // 弹出 markdown 分析报告
                showHtml(markdownReportHtml, "SQL分析报告", null);
            });
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            buttonPanel.add(button);
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
