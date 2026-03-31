package com.RuneBars;

import com.google.inject.Provides;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Provider;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.infobox.InfoBox;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.ui.overlay.infobox.Timer;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;
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

	public RuneBarsOverlay overlay;

	private volatile List<InfoBox> capturedInfoBoxes = Collections.emptyList();
	private volatile List<InfoBox> testInfoBoxes = Collections.emptyList();

	public RuneBarsConfig getConfig() { return config; }
	public List<InfoBox> getCapturedInfoBoxes() { return capturedInfoBoxes; }
	public List<InfoBox> getTestInfoBoxes() { return testInfoBoxes; }

	@Override
	protected void startUp() throws Exception {
		overlay = overlayProvider.get();
		overlayManager.add(overlay);
		updateTestMode();
	}

	@Override
	protected void shutDown() throws Exception {
		overlayManager.remove(overlay);
		capturedInfoBoxes.forEach(infoBoxManager::addInfoBox);
		capturedInfoBoxes = Collections.emptyList();
		testInfoBoxes = Collections.emptyList();
	}

	@Subscribe
	public void onGameTick(GameTick event) {
		List<InfoBox> infoBoxes = infoBoxManager.getInfoBoxes();
		List<InfoBox> nextCaptured = new ArrayList<>(capturedInfoBoxes);
		List<InfoBox> toCapture = new ArrayList<>();

		for (InfoBox ib : infoBoxes) {
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

		sortCapturedInfoBoxes(nextCaptured, new ArrayList<>(testInfoBoxes));
		capturedInfoBoxes = Collections.unmodifiableList(nextCaptured);
	}

	public boolean shouldCapture(InfoBox ib) {
		String name = ib.getName();
		List<String> ignored = Text.fromCSV(config.ignoredInfoBoxes());
		if (ignored.stream().anyMatch(i -> i.equalsIgnoreCase(name))) return false;

		return config.combatOnlyByDefault() && (COMBAT_PATTERN.matcher(name).find() ||
				(ib.getTooltip() != null && COMBAT_PATTERN.matcher(ib.getTooltip()).find()));
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event) {
		if (event.getGroup().equals(RuneBarsConfig.GROUP)) {
			if (event.getKey().equals("testMode")) {
				updateTestMode();
			} else {
				List<InfoBox> nextCaptured = new ArrayList<>(capturedInfoBoxes);
				List<InfoBox> nextTest = new ArrayList<>(testInfoBoxes);
				sortCapturedInfoBoxes(nextCaptured, nextTest);
				capturedInfoBoxes = Collections.unmodifiableList(nextCaptured);
				testInfoBoxes = Collections.unmodifiableList(nextTest);
			}
		}
	}

	private void updateTestMode() {
		List<InfoBox> nextTest = new ArrayList<>();
		if (config.testMode()) {
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
