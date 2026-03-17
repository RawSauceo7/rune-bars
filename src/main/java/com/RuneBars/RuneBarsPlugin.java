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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PluginDescriptor(
		name = "RuneBars"
)
public class RuneBarsPlugin extends Plugin
{
	private static final Logger log = LoggerFactory.getLogger(RuneBarsPlugin.class);

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

	public RuneBarsOverlay overlay;

	private volatile List<InfoBox> capturedInfoBoxes = Collections.emptyList();
	private volatile Set<String> discoveredInfoBoxes = Collections.emptySet();
	private volatile List<InfoBox> testInfoBoxes = Collections.emptyList();
	private boolean testMode;
	private RuneBarsPanel panel;
	private NavigationButton navButton;

	public List<InfoBox> getCapturedInfoBoxes() { return capturedInfoBoxes; }
	public Set<String> getDiscoveredInfoBoxes() { return discoveredInfoBoxes; }
	public List<InfoBox> getTestInfoBoxes() { return testInfoBoxes; }
	public boolean isTestMode() { return testMode; }

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
		capturedInfoBoxes = Collections.emptyList();
		discoveredInfoBoxes = Collections.emptySet();
		testInfoBoxes = Collections.emptyList();
	}

	@Subscribe
	public void onGameTick(GameTick event) {
		List<InfoBox> infoBoxes = infoBoxManager.getInfoBoxes();
		List<InfoBox> nextCaptured = new ArrayList<>(capturedInfoBoxes);
		Set<String> nextDiscovered = new HashSet<>(discoveredInfoBoxes);
		List<InfoBox> toCapture = new ArrayList<>();

		for (InfoBox ib : infoBoxes) {
			nextDiscovered.add(ib.getName());
			if (shouldCapture(ib)) toCapture.add(ib);
		}

		for (InfoBox ib : toCapture) {
			infoBoxManager.removeInfoBox(ib);
			nextCaptured.add(ib);
		}

		nextCaptured.removeIf(ib -> {
			if (ib.cull()) { overlay.onInfoBoxRemoved(ib); return true; }
			if (!shouldCapture(ib)) { infoBoxManager.addInfoBox(ib); return true; }
			return false;
		});

		sortCapturedInfoBoxes(nextCaptured, testMode ? new ArrayList<>(testInfoBoxes) : new ArrayList<>());

		int prevDiscoveredSize = discoveredInfoBoxes.size();
		discoveredInfoBoxes = Collections.unmodifiableSet(nextDiscovered);
		capturedInfoBoxes = Collections.unmodifiableList(nextCaptured);

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
			List<InfoBox> nextCaptured = new ArrayList<>(capturedInfoBoxes);
			List<InfoBox> nextTest = new ArrayList<>(testInfoBoxes);
			sortCapturedInfoBoxes(nextCaptured, nextTest);
			capturedInfoBoxes = Collections.unmodifiableList(nextCaptured);
			testInfoBoxes = Collections.unmodifiableList(nextTest);
			if (panel != null) {
				panel.refreshSettings();
			}
		}
	}

	public void toggleTestMode() {
		testMode = !testMode;
		List<InfoBox> nextTest = new ArrayList<>();
		if (testMode) {
			BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/icon.png");
			nextTest.add(new TestTimer(1, ChronoUnit.MINUTES, icon, this));
			nextTest.add(new TestInfoBox(icon, this));
		}
		List<InfoBox> nextCaptured = new ArrayList<>(capturedInfoBoxes);
		sortCapturedInfoBoxes(nextCaptured, nextTest);
		capturedInfoBoxes = Collections.unmodifiableList(nextCaptured);
		testInfoBoxes = Collections.unmodifiableList(nextTest);
	}

	private void sortCapturedInfoBoxes(List<InfoBox> captured, List<InfoBox> test) {
		Comparator<InfoBox> comp = config.sortType() == RuneBarsConfig.SortType.ALPHABETICAL
				? Comparator.comparing(InfoBox::getName)
				: (b1, b2) -> (b1 instanceof Timer && b2 instanceof Timer) ? ((Timer) b1).getEndTime().compareTo(((Timer) b2).getEndTime()) : b1.getName().compareTo(b2.getName());
		if (config.sortOrder() == RuneBarsConfig.SortOrder.DESCENDING) comp = comp.reversed();
		Collections.sort(captured, comp);
		Collections.sort(test, comp);
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
