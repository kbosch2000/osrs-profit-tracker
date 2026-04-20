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
}
