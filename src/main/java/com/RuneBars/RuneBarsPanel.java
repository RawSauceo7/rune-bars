package com.RuneBars;

import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.*;

public class RuneBarsPanel extends PluginPanel
{
    private final RuneBarsPlugin plugin;
    private final ConfigManager configManager;
    private final JPanel timerListPanel = new JPanel();
    private final Map<String, JCheckBox> toggles = new HashMap<>();

    public RuneBarsPanel(RuneBarsPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setLayout(new BorderLayout());
        timerListPanel.setLayout(new GridLayout(0, 1, 0, 5));
        add(timerListPanel, BorderLayout.CENTER);
    }

    public void update() {
        SwingUtilities.invokeLater(() -> {
            for (String name : plugin.getDiscoveredInfoBoxes()) {
                if (!toggles.containsKey(name)) {
                    JCheckBox cb = new JCheckBox(name);
                    cb.setBackground(ColorScheme.DARK_GRAY_COLOR);
                    Boolean enabled = configManager.getConfiguration("runebars", "enabled_" + name, Boolean.class);
                    cb.setSelected(enabled != null ? enabled : plugin.getCapturedInfoBoxes().stream().anyMatch(i -> i.getName().equals(name)));
                    cb.addActionListener(e -> configManager.setConfiguration("runebars", "enabled_" + name, cb.isSelected()));
                    toggles.put(name, cb);
                    timerListPanel.add(cb);
                    revalidate(); repaint();
                }
            }
        });
    }
}