package com.RuneBars;

import com.google.inject.Provides;
import java.util.*;
import java.util.regex.Pattern;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
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

	@Inject private InfoBoxManager infoBoxManager;
	@Inject private OverlayManager overlayManager;
	@Inject private ConfigManager configManager;
	@Inject private RuneBarsConfig config;
	@Inject private RuneBarsOverlay overlay;
	@Inject private ClientToolbar clientToolbar;

	@Getter private final List<InfoBox> capturedInfoBoxes = new ArrayList<>();
	@Getter private final Set<String> discoveredInfoBoxes = new HashSet<>();
	private RuneBarsPanel panel;
	private NavigationButton navButton;

	@Override
	protected void startUp() throws Exception {
		overlayManager.add(overlay);
		panel = new RuneBarsPanel(this, configManager);
		navButton = NavigationButton.builder().tooltip("RuneBars").priority(7).panel(panel)
				.icon(ImageUtil.loadImageResource(getClass(), "/com.RuneBars/icon.png")).build();
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
		if (panel != null) panel.update();
	}

	private boolean shouldCapture(InfoBox ib) {
		Boolean enabled = configManager.getConfiguration("runebars", "enabled_" + ib.getName(), Boolean.class);
		if (enabled != null) return enabled;
		return config.combatOnlyByDefault() && (COMBAT_PATTERN.matcher(ib.getName()).find() ||
				(ib.getTooltip() != null && COMBAT_PATTERN.matcher(ib.getTooltip()).find()));
	}

	private void sortCapturedInfoBoxes() {
		Comparator<InfoBox> comp = config.sortType() == RuneBarsConfig.SortType.ALPHABETICAL
				? Comparator.comparing(InfoBox::getName)
				: (b1, b2) -> (b1 instanceof Timer && b2 instanceof Timer) ? ((Timer) b1).getEndTime().compareTo(((Timer) b2).getEndTime()) : b1.getName().compareTo(b2.getName());
		if (config.sortOrder() == RuneBarsConfig.SortOrder.DESCENDING) comp = comp.reversed();
		Collections.sort(capturedInfoBoxes, comp);
	}

	@Provides RuneBarsConfig provideConfig(ConfigManager cm) { return cm.getConfig(RuneBarsConfig.class); }
}