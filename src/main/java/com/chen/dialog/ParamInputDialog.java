package com.chen.dialog;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.chen.constant.MessageConstants.DIALOG_TITLE;
import static com.chen.constant.MessageConstants.PARAM_OPTIONAL_LABEL;

/**
 * 参数填写对话框。支持多参数输入，所有参数均为选填
 */
public class ParamInputDialog extends DialogWrapper {
    // 常量

    private final Map<String, JTextField> fields = new LinkedHashMap<>();
    private final JPanel panel;

    /**
     * 构造方法
     * @param params 需要填写的参数名集合
     * @param project IDEA Project
     */
    public ParamInputDialog(Collection<String> params, Project project) {
        super(project, true);
        setTitle("填写SQL参数");
        panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 12, 6, 12);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.EAST;

        // 动态生成每一个参数输入框
        for (String p : params) {
            JLabel label = new JLabel(p);
            JTextField tf = new JTextField(30);
            fields.put(p, tf);
            panel.add(label, gbc);
            gbc.gridx++;
            gbc.anchor = GridBagConstraints.WEST;
            panel.add(tf, gbc);
            gbc.gridx = 0;
            gbc.gridy++;
            gbc.anchor = GridBagConstraints.EAST;
        }
        init();
        setResizable(true);
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return panel;
    }

    /**
     * 获取所有参数填写结果
     */
    public Map<String, Object> getParamValues() {
        Map<String, Object> map = new LinkedHashMap<>();
        for (Map.Entry<String, JTextField> e : fields.entrySet()) {
            map.put(e.getKey(), e.getValue().getText());
        }
        return map;
    }
}