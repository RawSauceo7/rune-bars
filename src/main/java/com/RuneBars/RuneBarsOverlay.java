package com.RuneBars;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.*;
import net.runelite.client.ui.overlay.components.ComponentOrientation;
import net.runelite.client.ui.overlay.infobox.*;
import net.runelite.client.ui.overlay.infobox.Timer;

public class RuneBarsOverlay extends Overlay
{
    private final RuneBarsPlugin plugin;
    private final RuneBarsConfig config;
    private final Map<InfoBox, Instant> fadingOut = new ConcurrentHashMap<>();
    private final Map<BufferedImage, Color> colorCache = new WeakHashMap<>();

    @Inject
    private RuneBarsOverlay(RuneBarsPlugin plugin, RuneBarsConfig config) {
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.ABOVE_CHATBOX_RIGHT);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (config == null) return null;
        if (plugin.getCapturedInfoBoxes().isEmpty() && (!plugin.isTestMode() || plugin.getTestInfoBoxes().isEmpty()) && fadingOut.isEmpty()) return null;

        Font oldFont = graphics.getFont();
        Composite oldComposite = graphics.getComposite();
        graphics.setFont(FontManager.getRunescapeFont().deriveFont((float) config.fontSize()));

        int x = 0, y = 0, maxWidth = 0, maxHeight = 0;
        for (InfoBox ib : plugin.getCapturedInfoBoxes()) {
            renderInfoBox(graphics, ib, x, y, 1.0f);
            if (config.orientation() == ComponentOrientation.HORIZONTAL) {
                x += config.iconSize() + config.spacing();
                maxWidth = x; maxHeight = Math.max(maxHeight, config.iconSize() + config.fontSize());
            } else {
                y += config.iconSize() + config.spacing() + config.fontSize();
                maxHeight = y; maxWidth = Math.max(maxWidth, config.iconSize());
            }
        }

        if (plugin.isTestMode()) {
            for (InfoBox ib : plugin.getTestInfoBoxes()) {
                renderInfoBox(graphics, ib, x, y, 1.0f);
                if (config.orientation() == ComponentOrientation.HORIZONTAL) {
                    x += config.iconSize() + config.spacing();
                    maxWidth = x; maxHeight = Math.max(maxHeight, config.iconSize() + config.fontSize());
                } else {
                    y += config.iconSize() + config.spacing() + config.fontSize();
                    maxHeight = y; maxWidth = Math.max(maxWidth, config.iconSize());
                }
            }
        }

        Iterator<Map.Entry<InfoBox, Instant>> it = fadingOut.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<InfoBox, Instant> entry = it.next();
            long elapsed = Duration.between(entry.getValue(), Instant.now()).toMillis();
            if (elapsed >= config.fadeDelay()) { it.remove(); continue; }
            renderInfoBox(graphics, entry.getKey(), x, y, 1.0f - ((float) elapsed / config.fadeDelay()));
        }

        graphics.setFont(oldFont);
        graphics.setComposite(oldComposite);
        return new Dimension(maxWidth, maxHeight);
    }

    public void onInfoBoxRemoved(InfoBox ib) { if (config.fadeDelay() > 0) fadingOut.put(ib, Instant.now()); }

    private void renderInfoBox(Graphics2D g, InfoBox ib, int x, int y, float opacity) {
        BufferedImage img = ib.getImage();
        if (img == null) return;
        float finalOpacity = opacity;
        if (ib instanceof Timer && opacity >= 1.0f) {
            long rem = Duration.between(Instant.now(), ((Timer) ib).getEndTime()).toSeconds();
            if (config.flashThreshold() > 0 && rem <= config.flashThreshold() && (System.currentTimeMillis() / 500) % 2 == 0) finalOpacity *= 0.5f;
        }
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, finalOpacity));
        Color color = getDominantColor(img);
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(100 * finalOpacity)));
        g.fillRect(x, y, config.iconSize(), config.iconSize());
        g.drawImage(img, x, y, config.iconSize(), config.iconSize(), null);
        if (ib.getText() != null) { g.setColor(ib.getTextColor()); g.drawString(ib.getText(), x, y + config.iconSize()); }
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
    }

    private Color getDominantColor(BufferedImage img) {
        return colorCache.computeIfAbsent(img, i -> {
            long r = 0, g = 0, b = 0, count = 0;
            int[] pixels = i.getRGB(0, 0, i.getWidth(), i.getHeight(), null, 0, i.getWidth());
            for (int pixel : pixels) {
                int alpha = (pixel >> 24) & 0xff;
                if (alpha > 100) {
                    r += (pixel >> 16) & 0xff;
                    g += (pixel >> 8) & 0xff;
                    b += (pixel) & 0xff;
                    count++;
                }
            }
            return count == 0 ? Color.WHITE : new Color((int) (r / count), (int) (g / count), (int) (b / count));
        });
    }
}