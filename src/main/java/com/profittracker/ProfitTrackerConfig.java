package com.profittracker;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup("profittracker")
public interface ProfitTrackerConfig extends Config
{
	@ConfigItem(
		keyName = "priceRefreshSeconds",
		name = "Live price refresh",
		description = "How often to refresh the OSRS Wiki real-time price cache."
	)
	@Range(min = 60, max = 600)
	@Units(Units.SECONDS)
	default int priceRefreshSeconds()
	{
		return 120;
	}

	@ConfigItem(
		keyName = "countSupplyLosses",
		name = "Track supplies",
		description = "Estimate supply cost from inventory value decreases during the session."
	)
	default boolean countSupplyLosses()
	{
		return true;
	}

	@ConfigItem(
		keyName = "countPassiveResourceLosses",
		name = "Track ammo/runes/charges",
		description = "Count value lost from inventory or equipment during combat, including runes, ammo, thrown items, and visible charge/item changes."
	)
	default boolean countPassiveResourceLosses()
	{
		return true;
	}

	@ConfigItem(
		keyName = "announceNewSession",
		name = "Chat session start",
		description = "Send a quiet chat message when the tracker sees the first NPC loot."
	)
	default boolean announceNewSession()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showDropBreakdown",
		name = "Show drop breakdown",
		description = "Show the highest-value drops for each NPC in the side panel."
	)
	default boolean showDropBreakdown()
	{
		return true;
	}

	@ConfigItem(
		keyName = "maxDropsShown",
		name = "Drops shown",
		description = "Maximum number of drops to show per NPC."
	)
	@Range(min = 3, max = 20)
	default int maxDropsShown()
	{
		return 8;
	}

	@ConfigItem(
		keyName = "showNotableDrops",
		name = "Show notable drops",
		description = "Show a mini log of high-value drops with item icons."
	)
	default boolean showNotableDrops()
	{
		return true;
	}

	@ConfigItem(
		keyName = "notableDropThreshold",
		name = "Notable threshold",
		description = "Drops at or above this value are added to the notable drop log."
	)
	@Range(min = 1_000, max = 2_000_000_000)
	default int notableDropThreshold()
	{
		return 10_000_000;
	}

	@ConfigItem(
		keyName = "maxNotableDrops",
		name = "Notables shown",
		description = "Maximum number of notable drops to show."
	)
	@Range(min = 3, max = 50)
	default int maxNotableDrops()
	{
		return 12;
	}
}
