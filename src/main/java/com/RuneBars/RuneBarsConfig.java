package com.RuneBars;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.ui.overlay.components.ComponentOrientation;

@ConfigGroup(RuneBarsConfig.GROUP)
public interface RuneBarsConfig extends Config
{
	String GROUP = "runebars";

	enum SortOrder { ASCENDING, DESCENDING }
	enum SortType { ALPHABETICAL, TIME_REMAINING }

	@ConfigItem(keyName = "orientation", name = "Orientation", description = "The orientation of the action bar", position = 1)
	default ComponentOrientation orientation() { return ComponentOrientation.HORIZONTAL; }

	@ConfigItem(keyName = "iconSize", name = "Icon Size", description = "The size of the icons", position = 2)
	default int iconSize() { return 32; }

	@ConfigItem(keyName = "spacing", name = "Spacing", description = "The spacing between icons", position = 3)
	default int spacing() { return 2; }

	@ConfigItem(keyName = "sortType", name = "Sort Type", description = "How to sort the active bars", position = 4)
	default SortType sortType() { return SortType.ALPHABETICAL; }

	@ConfigItem(keyName = "sortOrder", name = "Sort Order", description = "The order to sort the active bars", position = 5)
	default SortOrder sortOrder() { return SortOrder.ASCENDING; }

	@ConfigItem(keyName = "flashThreshold", name = "Flash Threshold", description = "Seconds remaining before flashing", position = 6)
	default int flashThreshold() { return 5; }

	@ConfigItem(keyName = "fadeDelay", name = "Fade Out Delay", description = "Milliseconds to fade after expiring", position = 7)
	default int fadeDelay() { return 500; }

	@ConfigItem(keyName = "combatOnlyByDefault", name = "Auto-Capture Combat", description = "Automatically capture combat-related timers", position = 8)
	default boolean combatOnlyByDefault() { return true; }

	@ConfigItem(keyName = "fontSize", name = "Font Size", description = "The font size of the timer text", position = 9)
	default int fontSize() { return 12; }

	@ConfigItem(keyName = "ignoredInfoBoxes", name = "Ignored Bars", description = "Comma-separated list of bar names to ignore (e.g., Stamina, Super Combat)", position = 10)
	default String ignoredInfoBoxes() { return ""; }
}
