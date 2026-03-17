package com.RuneBars;

import com.google.inject.Provides;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Provider;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.infobox.InfoBox;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.ui.overlay.infobox.Timer;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(
		name = "RuneBars"
)
public class RuneBarsPlugin extends Plugin
{
	private static final Pattern COMBAT_PATTERN = Pattern.compile(
			"(poison|venom|vengeance|antifire|stamina|prayer|overload|divine|boost|super|recharge)",
			Pattern.CASE_INSENSITIVE
	);

	@Inject InfoBoxManager infoBoxManager;
	@Inject OverlayManager overlayManager;
	@Inject ConfigManager configManager;
	@Inject RuneBarsConfig config;
	@Inject Provider<RuneBarsOverlay> overlayProvider;
	@Inject ClientToolbar clientToolbar;

	private RuneBarsOverlay overlay;

	@Getter private final List<InfoBox> capturedInfoBoxes = new ArrayList<>();
	@Getter private final Set<String> discoveredInfoBoxes = new HashSet<>();
	@Getter private final List<InfoBox> testInfoBoxes = new ArrayList<>();
	@Getter private boolean testMode;
	private RuneBarsPanel panel;
	private NavigationButton navButton;

	public RuneBarsConfig getConfig()
	{
		return config == null ? configManager.getConfig(RuneBarsConfig.class) : config;
	}

	@Override
	protected void startUp() throws Exception {
		overlay = overlayProvider.get();
		overlayManager.add(overlay);
		panel = new RuneBarsPanel(this, configManager);
		navButton = NavigationButton.builder().tooltip("RuneBars").priority(7).panel(panel)
				.icon(ImageUtil.loadImageResource(getClass(), "/icon.png")).build();
		clientToolbar.addNavigation(navButton);
	}

	@Override
	protected void shutDown() throws Exception {
		overlayManager.remove(overlay);
		clientToolbar.removeNavigation(navButton);
		capturedInfoBoxes.forEach(infoBoxManager::addInfoBox);
		capturedInfoBoxes.clear();
	}

	@Subscribe
	public void onGameTick(GameTick event) {
		List<InfoBox> infoBoxes = infoBoxManager.getInfoBoxes();
		List<InfoBox> toCapture = new ArrayList<>();

		int prevDiscoveredSize = discoveredInfoBoxes.size();
		for (InfoBox ib : infoBoxes) {
			discoveredInfoBoxes.add(ib.getName());
			if (shouldCapture(ib)) toCapture.add(ib);
		}

		for (InfoBox ib : toCapture) {
			infoBoxManager.removeInfoBox(ib);
			capturedInfoBoxes.add(ib);
		}

		capturedInfoBoxes.removeIf(ib -> {
			if (ib.cull()) { overlay.onInfoBoxRemoved(ib); return true; }
			if (!shouldCapture(ib)) { infoBoxManager.addInfoBox(ib); return true; }
			return false;
		});

		sortCapturedInfoBoxes();
		if (panel != null && discoveredInfoBoxes.size() != prevDiscoveredSize) {
			panel.update();
		}
	}

	private boolean shouldCapture(InfoBox ib) {
		Boolean enabled = configManager.getConfiguration(RuneBarsConfig.GROUP, "enabled_" + ib.getName(), Boolean.class);
		if (enabled != null) return enabled;
		return config.combatOnlyByDefault() && (COMBAT_PATTERN.matcher(ib.getName()).find() ||
				(ib.getTooltip() != null && COMBAT_PATTERN.matcher(ib.getTooltip()).find()));
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event) {
		if (event.getGroup().equals(RuneBarsConfig.GROUP)) {
			sortCapturedInfoBoxes();
			if (panel != null) {
				panel.refreshSettings();
			}
		}
	}

	public void toggleTestMode() {
		testMode = !testMode;
		if (testMode) {
			testInfoBoxes.clear();
			BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/icon.png");
			testInfoBoxes.add(new TestTimer(1, ChronoUnit.MINUTES, icon, this));
			testInfoBoxes.add(new TestInfoBox(icon, this));
		} else {
			testInfoBoxes.clear();
		}
		sortCapturedInfoBoxes();
	}

	private void sortCapturedInfoBoxes() {
		Comparator<InfoBox> comp = config.sortType() == RuneBarsConfig.SortType.ALPHABETICAL
				? Comparator.comparing(InfoBox::getName)
				: (b1, b2) -> (b1 instanceof Timer && b2 instanceof Timer) ? ((Timer) b1).getEndTime().compareTo(((Timer) b2).getEndTime()) : b1.getName().compareTo(b2.getName());
		if (config.sortOrder() == RuneBarsConfig.SortOrder.DESCENDING) comp = comp.reversed();
		Collections.sort(capturedInfoBoxes, comp);
		Collections.sort(testInfoBoxes, comp);
	}

	@Provides
	public static RuneBarsConfig provideConfig(ConfigManager cm)
	{
		return cm.getConfig(RuneBarsConfig.class);
	}

	public static class TestTimer extends Timer {
		public TestTimer(long duration, ChronoUnit unit, BufferedImage image, Plugin plugin) {
			super(duration, unit, image, plugin);
		}
		@Override public String getName() { return "Test Timer"; }
	}

	public static class TestInfoBox extends InfoBox {
		public TestInfoBox(BufferedImage image, Plugin plugin) {
			super(image, plugin);
		}
		@Override public String getName() { return "Test Box"; }
		@Override public String getText() { return "Test"; }
		@Override public Color getTextColor() { return Color.WHITE; }
	}
}
