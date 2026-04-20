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
}
