package com.profittracker;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;

class ProfitTrackerTarget
{
	@Getter
	private final String name;

	@Getter
	private int kills;

	@Getter
	private long lootValue;

	private final Map<Integer, ProfitTrackerDrop> drops = new LinkedHashMap<>();

	ProfitTrackerTarget(String name)
	{
		this.name = name;
	}

	void recordKill(long value)
	{
		kills++;
		lootValue += value;
	}

	void recordDrop(int itemId, int quantity, long value)
	{
		drops.computeIfAbsent(itemId, ProfitTrackerDrop::new).add(quantity, value);
	}

	long getAverageKillValue()
	{
		return kills == 0 ? 0 : lootValue / kills;
	}

	Map<Integer, ProfitTrackerDrop> getDrops()
	{
		return drops;
	}

	long getProfitShare(long totalProfit, long totalLoot)
	{
		if (totalLoot <= 0)
		{
			return 0;
		}
		return totalProfit * lootValue / totalLoot;
	}
}
