package com.profittracker;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class ProfitTrackerInventoryTest
{
	private static final int PRAYER_POTION = 2434;
	private static final int SHARK = 385;
	private static final int COINS = 995;

	private final ProfitTrackerPriceLookup prices = itemId ->
	{
		switch (itemId)
		{
			case PRAYER_POTION:
				return 9_000;
			case SHARK:
				return 700;
			case COINS:
				return 1;
			default:
				return 0;
		}
	};

	@Test
	public void valuesInventoryFromQuantities()
	{
		final ProfitTrackerInventory inventory = new ProfitTrackerInventory();

		assertEquals(20_400, inventory.value(mapOf(
			PRAYER_POTION, 2,
			SHARK, 2,
			COINS, 1_000
		), prices));
	}

	@Test
	public void calculatesOnlyPositiveSupplyLoss()
	{
		final ProfitTrackerInventory inventory = new ProfitTrackerInventory();
		final Map<Integer, Integer> previous = mapOf(
			PRAYER_POTION, 2,
			SHARK, 4,
			COINS, 1_000
		);
		final Map<Integer, Integer> current = mapOf(
			PRAYER_POTION, 1,
			SHARK, 1,
			COINS, 1_000
		);

		assertEquals(11_100, inventory.loss(previous, current, prices));
		assertEquals(0, inventory.loss(current, previous, prices));
	}

	@Test
	public void ignoresInvalidItemsAndUnknownPrices()
	{
		final ProfitTrackerInventory inventory = new ProfitTrackerInventory();

		assertEquals(700, inventory.value(mapOf(
			SHARK, 1,
			-1, 5,
			123456, 100,
			COINS, 0
		), prices));
	}

	private static Map<Integer, Integer> mapOf(int... values)
	{
		final Map<Integer, Integer> map = new HashMap<>();
		for (int i = 0; i < values.length; i += 2)
		{
			map.put(values[i], values[i + 1]);
		}
		return map;
	}
}
