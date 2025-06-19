package com.chen.utils;

import com.intellij.openapi.ui.popup.JBPopupFactory;

import javax.swing.*;
import java.awt.*;

public class HtmlViewerUtil {

    /**
     * 弹出一个简单的 HTML 查看窗口
     * @param html HTML 字符串内容
     * @param title 窗口标题
     */
    public static void showHtml(String html, String title) {

        JEditorPane editorPane = new JEditorPane("text/html", html);
        editorPane.setEditable(false);
        editorPane.setCaretPosition(0);

        // 放入滚动面板，防止内容太长
        JScrollPane scrollPane = new JScrollPane(editorPane);
        scrollPane.setPreferredSize(new Dimension(1500, 600));
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        // 使用 IntelliJ JBPopupFactory 创建弹窗
        JBPopupFactory.getInstance()
                .createComponentPopupBuilder(scrollPane, null)
                .setTitle(title)
                .setResizable(true)
                .setMovable(true)
                .setRequestFocus(true)
                .createPopup()
                .showInFocusCenter();
    }
}
