# NPC Profit Tracker

NPC Profit Tracker is a RuneLite external plugin for tracking how much money you make from NPC kills, bosses, and slayer trips.

It watches NPC loot, live item prices, and visible supply/resource use, then turns that into a clear session view with loot, supplies, profit, profit per hour, per-NPC totals, and notable drops.

The plugin is a tracker only. It does not automate gameplay, click anything, choose actions, or send game inputs.

## What It Tracks

- NPC kills and boss kills
- Loot value per kill
- Total loot value
- Supplies used
- Net profit
- Profit per hour
- Per-NPC kill count and average loot
- Top drops per NPC
- High-value notable drops with item icons
- Live/fallback price status

## Live Prices

NPC Profit Tracker uses the OSRS Wiki real-time price API for live prices.

If live prices are temporarily unavailable, the plugin falls back to RuneLite item prices where possible. The side panel shows whether prices are currently using live data or fallback data.

## Supplies And Resource Costs

The plugin subtracts supply costs from profit and profit per hour.

It tracks visible value loss from both inventory and equipment, including:

- Food
- Potions
- Runes
- Ammo
- Thrown weapons
- Stackable resources
- Visible charged-item value or item changes

To avoid false supply costs, obvious inventory movement actions are ignored, such as banking, depositing, withdrawing, dropping, taking, wearing, wielding, selling, or offering items.

Some OSRS charge systems do not expose every internal charge decrement as an inventory/equipment item change. If RuneLite cannot see the charge loss, the plugin cannot price that hidden loss perfectly. Anything visible through inventory or equipment changes is included.

## Notable Drops

The plugin includes a mini notable-drop log for valuable drops.

Each notable entry shows:

- Item icon
- Item name
- Quantity
- Value
- NPC name
- Kill count for that NPC when the drop happened

By default, drops worth at least `10,000,000 gp` are logged as notable drops. You can change the threshold in the plugin settings.

## Side Panel

The sidebar includes:

- Reset button
- Pause/Resume button
- Session status
- Elapsed time
- Kill count
- Loot
- Supplies
- Profit
- Profit/hr
- Price status
- Notable drops
- Per-NPC breakdowns
- Top drops per NPC

Pause is useful when you take a break, bank, trade, or otherwise do something that should not affect the active trip.

## Settings

Available settings include:

- Live price refresh interval
- Track supplies
- Track ammo/runes/charges
- Show a chat message when tracking starts
- Show drop breakdowns
- Number of drops shown per NPC
- Show notable drops
- Notable drop value threshold
- Number of notable drops shown

## Example Use Cases

- Track profit during a boss trip
- Compare GP/hr between different monsters
- See how much supplies reduce your real profit
- Keep a mini log of big drops
- Track slayer task profitability
- Review average loot per kill

## Testing

The project includes simulation tests for the plugin's core logic. These tests do not require logging into the game.

Covered behavior includes:

- Kill tracking
- Loot value calculation
- Per-NPC totals
- Supply cost calculation
- Ammo/rune/resource value loss
- Profit and profit/hr
- Notable drop threshold and kill count
- Price parsing
- Panel formatting
- Pause/reset behavior

Run tests with:

```powershell
.\gradlew.bat test
```

Build the plugin jar with:

```powershell
.\gradlew.bat jar
```

## Development

This repository is based on RuneLite's external plugin template.

To launch a development RuneLite client from this project:

```powershell
.\gradlew.bat run
```

Jagex accounts require launching RuneLite through the Jagex Launcher for live login. Local development clients may not be able to log in with a Jagex account unless your environment supports RuneLite's Jagex-account developer credential flow.

## Safety

NPC Profit Tracker only observes RuneLite/client events and calculates values from them.

It does not:

- Automate gameplay
- Click menus
- Move the mouse
- Press keys
- Choose attacks
- Loot items
- Control the client
- Bypass Jagex or RuneLite restrictions
