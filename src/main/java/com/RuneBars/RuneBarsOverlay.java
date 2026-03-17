package com.RuneBars;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.time.Instant;
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
    private final Map<InfoBox, Instant> fadingOut = new ConcurrentHashMap<>();
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

            int x = 0, y = 0;
            ComponentOrientation orientation = config.orientation();
            int iconSize = config.iconSize();
            int spacing = config.spacing();
            int fontSize = config.fontSize();

            for (InfoBox ib : captured) {
                renderInfoBox(g, ib, x, y, 1.0f);
                if (orientation == ComponentOrientation.HORIZONTAL) {
                    x += iconSize + spacing;
                } else {
                    y += iconSize + spacing + fontSize;
                }
            }

            if (testMode) {
                for (InfoBox ib : test) {
                    renderInfoBox(g, ib, x, y, 1.0f);
                    if (orientation == ComponentOrientation.HORIZONTAL) {
                        x += iconSize + spacing;
                    } else {
                        y += iconSize + spacing + fontSize;
                    }
                }
            }

            Iterator<Map.Entry<InfoBox, Instant>> it = fadingOut.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<InfoBox, Instant> entry = it.next();
                long elapsed = Duration.between(entry.getValue(), Instant.now()).toMillis();
                if (elapsed >= config.fadeDelay()) {
                    it.remove();
                    continue;
                }
                renderInfoBox(g, entry.getKey(), x, y, 1.0f - ((float) elapsed / config.fadeDelay()));
                if (orientation == ComponentOrientation.HORIZONTAL) {
                    x += iconSize + spacing;
                } else {
                    y += iconSize + spacing + fontSize;
                }
            }

            int maxWidth, maxHeight;
            if (orientation == ComponentOrientation.HORIZONTAL) {
                maxWidth = Math.max(0, x - spacing);
                maxHeight = iconSize + fontSize;
            } else {
                maxWidth = iconSize;
                maxHeight = Math.max(0, y - spacing);
            }

            return new Dimension(maxWidth, maxHeight);
        } finally {
            g.dispose();
        }
    }

    public void onInfoBoxRemoved(InfoBox ib) {
        if (config.fadeDelay() > 0) {
            fadingOut.put(ib, Instant.now());
        }
    }

    private void renderInfoBox(Graphics2D graphics, InfoBox ib, int x, int y, float opacity) {
        BufferedImage img = ib.getImage();
        if (img == null) return;

        Graphics2D g = (Graphics2D) graphics.create();
        try {
            float finalOpacity = opacity;
            if (ib instanceof Timer && opacity >= 1.0f) {
                long rem = Duration.between(Instant.now(), ((Timer) ib).getEndTime()).toSeconds();
                if (config.flashThreshold() > 0 && rem <= config.flashThreshold() && (System.currentTimeMillis() / 500) % 2 == 0) {
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
            g.fillRect(x, y, config.iconSize(), config.iconSize());
            g.drawImage(img, x, y, config.iconSize(), config.iconSize(), null);

            String text = ib.getText();
            if (text != null) {
                g.setColor(ib.getTextColor());
                // Center text horizontally under the icon
                int textWidth = g.getFontMetrics().stringWidth(text);
                int textX = x + (config.iconSize() - textWidth) / 2;
                g.drawString(text, textX, y + config.iconSize() + config.fontSize());
            }
        } finally {
            g.dispose();
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
