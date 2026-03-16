package com.RuneBars;

import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.*;
import net.runelite.client.ui.overlay.components.ComponentOrientation;

public class RuneBarsPanel extends PluginPanel
{
    private final RuneBarsPlugin plugin;
    private final ConfigManager configManager;
    private final JPanel timerListPanel = new JPanel();
    private final Map<String, JCheckBox> toggles = new HashMap<>();

    private JComboBox<ComponentOrientation> orientation;
    private JSlider iconSize;
    private JSlider spacing;
    private JComboBox<RuneBarsConfig.SortType> sortType;
    private JComboBox<RuneBarsConfig.SortOrder> sortOrder;
    private JSlider flash;
    private JSlider fade;
    private JCheckBox combatOnly;
    private JSlider fontSize;

    public RuneBarsPanel(RuneBarsPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setLayout(new BorderLayout());

        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));

        JButton toggleTestBtn = new JButton("Toggle Test Bar");
        toggleTestBtn.addActionListener(e -> plugin.toggleTestMode());
        toggleTestBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        container.add(toggleTestBtn);
        container.add(Box.createVerticalStrut(10));

        addSettings(container);

        container.add(Box.createVerticalStrut(10));
        JLabel capturedLabel = new JLabel("Captured Bars");
        capturedLabel.setForeground(Color.WHITE);
        capturedLabel.setFont(FontManager.getRunescapeSmallFont());
        container.add(capturedLabel);
        container.add(Box.createVerticalStrut(5));

        timerListPanel.setLayout(new GridLayout(0, 1, 0, 5));
        container.add(timerListPanel);

        JScrollPane scrollPane = new JScrollPane(container);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(null);

        add(scrollPane, BorderLayout.CENTER);
    }

    private void addSettings(JPanel panel) {
        JLabel settingsLabel = new JLabel("Settings");
        settingsLabel.setForeground(Color.WHITE);
        settingsLabel.setFont(FontManager.getRunescapeSmallFont());
        panel.add(settingsLabel);
        panel.add(Box.createVerticalStrut(5));

        panel.add(createLabel("Orientation"));
        orientation = new JComboBox<>(ComponentOrientation.values());
        orientation.setSelectedItem(plugin.getConfig().orientation());
        orientation.addActionListener(e -> configManager.setConfiguration(RuneBarsConfig.GROUP, "orientation", orientation.getSelectedItem()));
        panel.add(orientation);
        panel.add(Box.createVerticalStrut(5));

        panel.add(createLabel("Icon Size"));
        iconSize = new JSlider(16, 64, plugin.getConfig().iconSize());
        iconSize.setMajorTickSpacing(16);
        iconSize.setPaintTicks(true);
        iconSize.addChangeListener(e -> {
            if (!iconSize.getValueIsAdjusting()) {
                configManager.setConfiguration(RuneBarsConfig.GROUP, "iconSize", iconSize.getValue());
            }
        });
        panel.add(iconSize);
        panel.add(Box.createVerticalStrut(5));

        panel.add(createLabel("Spacing"));
        spacing = new JSlider(0, 10, plugin.getConfig().spacing());
        spacing.setMajorTickSpacing(2);
        spacing.setPaintTicks(true);
        spacing.addChangeListener(e -> {
            if (!spacing.getValueIsAdjusting()) {
                configManager.setConfiguration(RuneBarsConfig.GROUP, "spacing", spacing.getValue());
            }
        });
        panel.add(spacing);
        panel.add(Box.createVerticalStrut(5));

        panel.add(createLabel("Sort Type"));
        sortType = new JComboBox<>(RuneBarsConfig.SortType.values());
        sortType.setSelectedItem(plugin.getConfig().sortType());
        sortType.addActionListener(e -> configManager.setConfiguration(RuneBarsConfig.GROUP, "sortType", sortType.getSelectedItem()));
        panel.add(sortType);
        panel.add(Box.createVerticalStrut(5));

        panel.add(createLabel("Sort Order"));
        sortOrder = new JComboBox<>(RuneBarsConfig.SortOrder.values());
        sortOrder.setSelectedItem(plugin.getConfig().sortOrder());
        sortOrder.addActionListener(e -> configManager.setConfiguration(RuneBarsConfig.GROUP, "sortOrder", sortOrder.getSelectedItem()));
        panel.add(sortOrder);
        panel.add(Box.createVerticalStrut(5));

        panel.add(createLabel("Flash Threshold (sec)"));
        flash = new JSlider(0, 30, plugin.getConfig().flashThreshold());
        flash.setMajorTickSpacing(10);
        flash.setPaintTicks(true);
        flash.addChangeListener(e -> {
            if (!flash.getValueIsAdjusting()) {
                configManager.setConfiguration(RuneBarsConfig.GROUP, "flashThreshold", flash.getValue());
            }
        });
        panel.add(flash);
        panel.add(Box.createVerticalStrut(5));

        panel.add(createLabel("Fade Delay (ms)"));
        fade = new JSlider(0, 5000, plugin.getConfig().fadeDelay());
        fade.setMajorTickSpacing(1000);
        fade.setPaintTicks(true);
        fade.addChangeListener(e -> {
            if (!fade.getValueIsAdjusting()) {
                configManager.setConfiguration(RuneBarsConfig.GROUP, "fadeDelay", fade.getValue());
            }
        });
        panel.add(fade);
        panel.add(Box.createVerticalStrut(5));

        combatOnly = new JCheckBox("Auto-Capture Combat", plugin.getConfig().combatOnlyByDefault());
        combatOnly.addActionListener(e -> configManager.setConfiguration(RuneBarsConfig.GROUP, "combatOnlyByDefault", combatOnly.isSelected()));
        panel.add(combatOnly);
        panel.add(Box.createVerticalStrut(5));

        panel.add(createLabel("Font Size"));
        fontSize = new JSlider(5, 30, plugin.getConfig().fontSize());
        fontSize.setMajorTickSpacing(5);
        fontSize.setPaintTicks(true);
        fontSize.addChangeListener(e -> {
            if (!fontSize.getValueIsAdjusting()) {
                configManager.setConfiguration(RuneBarsConfig.GROUP, "fontSize", fontSize.getValue());
            }
        });
        panel.add(fontSize);
    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FontManager.getRunescapeSmallFont());
        return label;
    }

    public void refreshSettings() {
        SwingUtilities.invokeLater(() -> {
            RuneBarsConfig config = plugin.getConfig();
            if (config == null) return;

            orientation.setSelectedItem(config.orientation());
            iconSize.setValue(config.iconSize());
            spacing.setValue(config.spacing());
            sortType.setSelectedItem(config.sortType());
            sortOrder.setSelectedItem(config.sortOrder());
            flash.setValue(config.flashThreshold());
            fade.setValue(config.fadeDelay());
            combatOnly.setSelected(config.combatOnlyByDefault());
            fontSize.setValue(config.fontSize());

            for (Map.Entry<String, JCheckBox> entry : toggles.entrySet()) {
                Boolean enabled = configManager.getConfiguration(RuneBarsConfig.GROUP, "enabled_" + entry.getKey(), Boolean.class);
                entry.getValue().setSelected(enabled != null ? enabled : plugin.getCapturedInfoBoxes().stream().anyMatch(i -> i.getName().equals(entry.getKey())));
            }
        });
    }

    public void update() {
        SwingUtilities.invokeLater(() -> {
            for (String name : plugin.getDiscoveredInfoBoxes()) {
                if (!toggles.containsKey(name)) {
                    JCheckBox cb = new JCheckBox(name);
                    cb.setBackground(ColorScheme.DARK_GRAY_COLOR);
                    Boolean enabled = configManager.getConfiguration(RuneBarsConfig.GROUP, "enabled_" + name, Boolean.class);
                    cb.setSelected(enabled != null ? enabled : plugin.getCapturedInfoBoxes().stream().anyMatch(i -> i.getName().equals(name)));
                    cb.addActionListener(e -> configManager.setConfiguration(RuneBarsConfig.GROUP, "enabled_" + name, cb.isSelected()));
                    toggles.put(name, cb);
                    timerListPanel.add(cb);
                    revalidate(); repaint();
                }
            }
        });
    }
}