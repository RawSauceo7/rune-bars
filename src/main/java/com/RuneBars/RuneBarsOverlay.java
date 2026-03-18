package com.RuneBars;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.ComponentOrientation;
import net.runelite.client.ui.overlay.infobox.InfoBox;
import net.runelite.client.ui.overlay.infobox.Timer;

public class RuneBarsOverlay extends Overlay
{
    private final RuneBarsPlugin plugin;
    private final RuneBarsConfig config;
    private final Map<InfoBox, Long> fadingOut = new ConcurrentHashMap<>();
    private final Map<BufferedImage, Color[]> colorCache = new WeakHashMap<>();

    @Inject
    public RuneBarsOverlay(RuneBarsPlugin plugin, RuneBarsConfig config) {
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.ABOVE_CHATBOX_RIGHT);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        List<InfoBox> captured = plugin.getCapturedInfoBoxes();
        List<InfoBox> test = plugin.getTestInfoBoxes();
        boolean testMode = plugin.isTestMode();

        if (captured.isEmpty() && (!testMode || test.isEmpty()) && fadingOut.isEmpty()) return null;

        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setFont(FontManager.getRunescapeFont().deriveFont((float) config.fontSize()));
            FontMetrics fm = g.getFontMetrics();

            int x = 0, y = 0;
            ComponentOrientation orientation = config.orientation();
            int iconSize = config.iconSize();
            int spacing = config.spacing();
            int fontSize = config.fontSize();
            int totalMaxWidth = 0;
            int totalMaxHeight = 0;

            long now = System.currentTimeMillis();

            // Render active info boxes
            for (InfoBox ib : captured) {
                if (fadingOut.containsKey(ib)) continue;
                renderItem(g, ib, x, y, 1.0f, fm, now);
                int slotWidth = Math.max(iconSize, ib.getText() != null ? fm.stringWidth(ib.getText()) : 0);
                if (orientation == ComponentOrientation.HORIZONTAL) {
                    x += slotWidth + spacing;
                    totalMaxWidth = x;
                    totalMaxHeight = Math.max(totalMaxHeight, iconSize + fontSize);
                } else {
                    y += iconSize + spacing + fontSize;
                    totalMaxHeight = y;
                    totalMaxWidth = Math.max(totalMaxWidth, slotWidth);
                }
            }

            // Render test info boxes
            if (testMode) {
                for (InfoBox ib : test) {
                    if (fadingOut.containsKey(ib)) continue;
                    renderItem(g, ib, x, y, 1.0f, fm, now);
                    int slotWidth = Math.max(iconSize, ib.getText() != null ? fm.stringWidth(ib.getText()) : 0);
                    if (orientation == ComponentOrientation.HORIZONTAL) {
                        x += slotWidth + spacing;
                        totalMaxWidth = x;
                        totalMaxHeight = Math.max(totalMaxHeight, iconSize + fontSize);
                    } else {
                        y += iconSize + spacing + fontSize;
                        totalMaxHeight = y;
                        totalMaxWidth = Math.max(totalMaxWidth, slotWidth);
                    }
                }
            }

            // Render fading info boxes
            Iterator<Map.Entry<InfoBox, Long>> it = fadingOut.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<InfoBox, Long> entry = it.next();
                long elapsed = now - entry.getValue();
                if (elapsed >= config.fadeDelay()) {
                    it.remove();
                    continue;
                }
                float opacity = 1.0f - ((float) elapsed / config.fadeDelay());
                renderItem(g, entry.getKey(), x, y, opacity, fm, now);
                int slotWidth = Math.max(iconSize, entry.getKey().getText() != null ? fm.stringWidth(entry.getKey().getText()) : 0);
                if (orientation == ComponentOrientation.HORIZONTAL) {
                    x += slotWidth + spacing;
                    totalMaxWidth = x;
                    totalMaxHeight = Math.max(totalMaxHeight, iconSize + fontSize);
                } else {
                    y += iconSize + spacing + fontSize;
                    totalMaxHeight = y;
                    totalMaxWidth = Math.max(totalMaxWidth, slotWidth);
                }
            }

            // Adjust dimensions to remove trailing spacing and account for text descenders
            if (orientation == ComponentOrientation.HORIZONTAL) {
                totalMaxWidth = Math.max(0, totalMaxWidth - spacing);
                totalMaxHeight += 2; // Padding for descenders
            } else {
                totalMaxHeight = Math.max(0, totalMaxHeight - spacing);
                totalMaxHeight += 2; // Padding for last item's text descenders
            }

            return new Dimension(totalMaxWidth, totalMaxHeight);
        } finally {
            g.dispose();
        }
    }

    public void onInfoBoxRemoved(InfoBox ib) {
        if (config.fadeDelay() > 0) {
            fadingOut.put(ib, System.currentTimeMillis());
        }
    }

    private void renderItem(Graphics2D g, InfoBox ib, int x, int y, float opacity, FontMetrics fm, long now) {
        BufferedImage img = ib.getImage();
        if (img == null) return;

        int iconSize = config.iconSize();
        String text = ib.getText();
        int textWidth = text != null ? fm.stringWidth(text) : 0;
        int slotWidth = Math.max(iconSize, textWidth);

        // Center icon and text within the slot
        int iconX = x + (slotWidth - iconSize) / 2;
        int textX = x + (slotWidth - textWidth) / 2;

        float finalOpacity = opacity;
        if (ib instanceof Timer && opacity >= 1.0f) {
            long rem = ((((Timer) ib).getEndTime().toEpochMilli()) - now) / 1000;
            if (config.flashThreshold() > 0 && rem <= config.flashThreshold() && (now / 500) % 2 == 0) {
                finalOpacity *= 0.5f;
            }
        }

        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, finalOpacity));

        Color[] colors = getCachedColors(img);
        Color bg = colors[1];
        if (finalOpacity < 1.0f) {
            bg = new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), (int)(bg.getAlpha() * finalOpacity));
        }
        g.setColor(bg);
        g.fillRect(iconX, y, iconSize, iconSize);
        g.drawImage(img, iconX, y, iconSize, iconSize, null);

        if (text != null) {
            g.setColor(ib.getTextColor());
            g.drawString(text, textX, y + iconSize + config.fontSize());
        }
    }

    private Color[] getCachedColors(BufferedImage img) {
        return colorCache.computeIfAbsent(img, i -> {
            long r = 0, g = 0, b = 0, count = 0;
            int w = i.getWidth();
            int h = i.getHeight();
            int[] pixels = i.getRGB(0, 0, w, h, null, 0, w);
            for (int pixel : pixels) {
                int alpha = (pixel >> 24) & 0xff;
                if (alpha > 100) {
                    r += (pixel >> 16) & 0xff;
                    g += (pixel >> 8) & 0xff;
                    b += (pixel) & 0xff;
                    count++;
                }
            }
            Color base = count == 0 ? Color.WHITE : new Color((int) (r / count), (int) (g / count), (int) (b / count));
            Color translucent = new Color(base.getRed(), base.getGreen(), base.getBlue(), 100);
            return new Color[] { base, translucent };
        });
    }
}
