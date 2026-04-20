package com.profittracker;

import java.util.Map;

class ProfitTrackerInventory
{
	long value(Map<Integer, Integer> quantities, ProfitTrackerPriceLookup prices)
	{
		long total = 0;
		for (Map.Entry<Integer, Integer> entry : quantities.entrySet())
		{
			if (entry.getKey() <= 0 || entry.getValue() <= 0)
			{
				continue;
			}

			total += (long) prices.getLivePrice(entry.getKey()) * entry.getValue();
		}
		return total;
	}

	long loss(Map<Integer, Integer> previous, Map<Integer, Integer> current, ProfitTrackerPriceLookup prices)
	{
		return Math.max(0, value(previous, prices) - value(current, prices));
	}
}
