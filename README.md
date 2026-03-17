# RuneBars

WoW-style customizable action bars for combat and utility timers in RuneLite.

## Description

RuneBars is a RuneLite plugin that captures standard InfoBoxes (timers and buffs) and displays them in a highly customizable, WoW-style action bar. It's designed to give you a cleaner, more focused view of your most important combat and utility timers.

## Features

- **Customizable Orientation**: Display your bars horizontally or vertically to fit your UI layout.
- **Adjustable Appearance**: Change icon size, spacing, and font size.
- **Smart Sorting**: Sort timers alphabetically or by time remaining, in ascending or descending order.
- **Visual Cues**:
    - **Flashing**: Icons flash when they are close to expiring.
    - **Fading**: Expired timers can fade out smoothly over a customizable duration.
    - **Dominant Color Background**: Icons feature a subtle background based on their own dominant colors for better visibility.
- **Auto-Capture**: Optionally auto-captures combat-related timers (potions, vengeance, etc.) by default.
- **Selective Capture**: Discovers InfoBoxes as they appear and allows you to toggle which ones are captured into the bar via the side panel.
- **Test Mode**: Easily preview your layout with test timers.

## Usage

1. Enable the **RuneBars** plugin in your RuneLite settings.
2. Open the **RuneBars side panel** (look for the action bar icon).
3. Use the **Toggle Test Bar** button to see how the bar looks and adjust settings like orientation and icon size.
4. As different InfoBoxes appear during gameplay (e.g., after drinking a potion), they will be listed under "Captured Bars" in the side panel.
5. Use the checkboxes to decide which timers should be moved from the standard InfoBox area to your RuneBars.

## Configuration

- **Orientation**: Horizontal or Vertical.
- **Icon Size**: 16px to 64px.
- **Spacing**: Distance between icons.
- **Sort Type**: Alphabetical or Time Remaining.
- **Sort Order**: Ascending or Descending.
- **Flash Threshold**: Seconds remaining before the icon starts flashing.
- **Fade Delay**: How long (in milliseconds) the icon remains visible after expiring.
- **Auto-Capture Combat**: Whether to automatically capture common combat timers.
- **Font Size**: Size of the timer text.

---

*Developed by Saucy07*
