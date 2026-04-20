package com.profittracker;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import lombok.Getter;
import net.runelite.client.game.ItemStack;

class ProfitTrackerSession
{
	private final Map<String, ProfitTrackerTarget> targets = new LinkedHashMap<>();
	private final Deque<ProfitTrackerNotableDrop> notableDrops = new LinkedList<>();

	@Getter
	private Instant startedAt;

	@Getter
	private long lootValue;

	@Getter
	private long supplyCost;

	private Instant pausedAt;
	private Duration pausedDuration = Duration.ZERO;

	void recordKill(String npcName, Collection<ItemStack> drops, ProfitTrackerPriceLookup prices, long notableThreshold)
	{
		if (isPaused())
		{
			return;
		}

		if (startedAt == null)
		{
			startedAt = Instant.now();
		}

		final ProfitTrackerTarget target = targets.computeIfAbsent(npcName, ProfitTrackerTarget::new);
		final int killCount = target.getKills() + 1;
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
			if (notableThreshold > 0 && value >= notableThreshold)
			{
				notableDrops.addFirst(new ProfitTrackerNotableDrop(npcName, drop.getId(), drop.getQuantity(), value, killCount));
			}
		}

		lootValue += killValue;
		target.recordKill(killValue);
	}

	void recordKill(String npcName, Collection<ItemStack> drops, ProfitTrackerPriceLookup prices)
	{
		recordKill(npcName, drops, prices, Long.MAX_VALUE);
	}

	void recordSupplyCost(long value)
	{
		if (value <= 0 || isPaused())
		{
			return;
		}

		supplyCost += value;
	}

	void reset()
	{
		targets.clear();
		notableDrops.clear();
		startedAt = null;
		lootValue = 0;
		supplyCost = 0;
		pausedAt = null;
		pausedDuration = Duration.ZERO;
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
		if (startedAt == null)
		{
			return Duration.ZERO;
		}

		final Instant end = pausedAt == null ? Instant.now() : pausedAt;
		final Duration elapsed = Duration.between(startedAt, end).minus(pausedDuration);
		return elapsed.isNegative() ? Duration.ZERO : elapsed;
	}

	void setStartedAt(Instant startedAt)
	{
		this.startedAt = startedAt;
	}

	boolean isPaused()
	{
		return pausedAt != null;
	}

	void togglePaused()
	{
		if (!isStarted())
		{
			return;
		}

		if (pausedAt == null)
		{
			pausedAt = Instant.now();
			return;
		}

		pausedDuration = pausedDuration.plus(Duration.between(pausedAt, Instant.now()));
		pausedAt = null;
	}

	String getStatus()
	{
		if (!isStarted())
		{
			return "Waiting for loot";
		}
		return isPaused() ? "Paused" : "Tracking";
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

	Collection<ProfitTrackerNotableDrop> getNotableDrops()
	{
		return Collections.unmodifiableCollection(notableDrops);
	}
}
