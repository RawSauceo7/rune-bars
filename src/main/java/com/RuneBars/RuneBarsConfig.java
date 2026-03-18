package com.RuneBars;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;
import net.runelite.client.ui.overlay.components.ComponentOrientation;

@ConfigGroup(RuneBarsConfig.GROUP)
public interface RuneBarsConfig extends Config
{
	String GROUP = "runebars";

	@ConfigSection(
		name = "General",
		description = "Core plugin settings",
		position = 0
	)
	String generalSection = "generalSection";

	@ConfigSection(
		name = "Layout",
		description = "Appearance and positioning",
		position = 10
	)
	String layoutSection = "layoutSection";

	@ConfigSection(
		name = "Visuals",
		description = "Effects and feedback",
		position = 20
	)
	String visualsSection = "visualsSection";

	enum SortOrder { ASCENDING, DESCENDING }
	enum SortType { ALPHABETICAL, TIME_REMAINING }

	// --- General Section ---

	@ConfigItem(
		keyName = "testMode",
		name = "Test Mode",
		description = "Display test bars to help with configuration and positioning",
		position = 1,
		section = generalSection
	)
	default boolean testMode() { return false; }

	@ConfigItem(
		keyName = "combatOnlyByDefault",
		name = "Auto-Capture Combat",
		description = "Automatically capture common combat-related timers (e.g. Potions, Vengeance)",
		position = 2,
		section = generalSection
	)
	default boolean combatOnlyByDefault() { return true; }

	@ConfigItem(
		keyName = "ignoredInfoBoxes",
		name = "Ignored Bars",
		description = "Comma-separated list of bar names to ignore (e.g. Stamina, Super Combat)",
		position = 3,
		section = generalSection
	)
	default String ignoredInfoBoxes() { return ""; }

	// --- Layout Section ---

	@ConfigItem(
		keyName = "orientation",
		name = "Orientation",
		description = "The direction the action bars will stack",
		position = 11,
		section = layoutSection
	)
	default ComponentOrientation orientation() { return ComponentOrientation.HORIZONTAL; }

	@Range(min = 16, max = 128)
	@Units(Units.PIXELS)
	@ConfigItem(
		keyName = "iconSize",
		name = "Icon Size",
		description = "The width and height of each bar icon",
		position = 12,
		section = layoutSection
	)
	default int iconSize() { return 32; }

	@Range(min = 0, max = 50)
	@Units(Units.PIXELS)
	@ConfigItem(
		keyName = "spacing",
		name = "Spacing",
		description = "The gap between consecutive bar icons",
		position = 13,
		section = layoutSection
	)
	default int spacing() { return 2; }

	@Range(min = 6, max = 32)
	@Units(Units.PIXELS)
	@ConfigItem(
		keyName = "fontSize",
		name = "Font Size",
		description = "The size of the timer text rendered under icons",
		position = 14,
		section = layoutSection
	)
	default int fontSize() { return 12; }

	@ConfigItem(
		keyName = "sortType",
		name = "Sort Type",
		description = "The criteria used to order the active bars",
		position = 15,
		section = layoutSection
	)
	default SortType sortType() { return SortType.ALPHABETICAL; }

	@ConfigItem(
		keyName = "sortOrder",
		name = "Sort Order",
		description = "The direction of the sorting criteria",
		position = 16,
		section = layoutSection
	)
	default SortOrder sortOrder() { return SortOrder.ASCENDING; }

	// --- Visuals Section ---

	@Range(min = 0, max = 60)
	@Units(Units.SECONDS)
	@ConfigItem(
		keyName = "flashThreshold",
		name = "Flash Threshold",
		description = "Remaining time at which a timer starts to flash for visibility",
		position = 21,
		section = visualsSection
	)
	default int flashThreshold() { return 5; }

	@Range(min = 0, max = 5000)
	@Units(Units.MILLISECONDS)
	@ConfigItem(
		keyName = "fadeDelay",
		name = "Fade Out Delay",
		description = "How long an expired bar takes to completely disappear",
		position = 22,
		section = visualsSection
	)
	default int fadeDelay() { return 500; }
}
