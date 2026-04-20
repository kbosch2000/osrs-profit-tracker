package com.profittracker;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import net.runelite.client.game.ItemStack;

class ProfitTrackerSession
{
	private final Map<String, ProfitTrackerTarget> targets = new LinkedHashMap<>();

	@Getter
	private Instant startedAt;

	@Getter
	private long lootValue;

	@Getter
	private long supplyCost;

	void recordKill(String npcName, Collection<ItemStack> drops, ProfitTrackerPriceLookup prices)
	{
		if (startedAt == null)
		{
			startedAt = Instant.now();
		}

		final ProfitTrackerTarget target = targets.computeIfAbsent(npcName, ProfitTrackerTarget::new);
		long killValue = 0;
		for (ItemStack drop : drops)
		{
			if (drop.getId() <= 0 || drop.getQuantity() <= 0)
			{
				continue;
			}

			final long value = (long) prices.getLivePrice(drop.getId()) * drop.getQuantity();
			killValue += value;
			target.recordDrop(drop.getId(), drop.getQuantity(), value);
		}

		lootValue += killValue;
		target.recordKill(killValue);
	}

	void recordSupplyCost(long value)
	{
		if (value <= 0)
		{
			return;
		}

		supplyCost += value;
	}

	void reset()
	{
		targets.clear();
		startedAt = null;
		lootValue = 0;
		supplyCost = 0;
	}

	boolean isStarted()
	{
		return startedAt != null;
	}

	int getKillCount()
	{
		return targets.values().stream().mapToInt(ProfitTrackerTarget::getKills).sum();
	}

	long getProfit()
	{
		return lootValue - supplyCost;
	}

	Duration getElapsed()
	{
		return startedAt == null ? Duration.ZERO : Duration.between(startedAt, Instant.now());
	}

	void setStartedAt(Instant startedAt)
	{
		this.startedAt = startedAt;
	}

	long getProfitPerHour()
	{
		final long seconds = Math.max(1, getElapsed().getSeconds());
		return getProfit() * 3600 / seconds;
	}

	Collection<ProfitTrackerTarget> getTargets()
	{
		return targets.values();
	}
}
