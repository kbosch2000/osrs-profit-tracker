package com.profittracker;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ProfitTrackerSupplyActionTest
{
	@Test
	public void detectsSupplyUseMenuOptions()
	{
		assertTrue(ProfitTrackerPlugin.isSupplyUseOption("Eat"));
		assertTrue(ProfitTrackerPlugin.isSupplyUseOption("Drink"));
		assertTrue(ProfitTrackerPlugin.isSupplyUseOption("Consume"));
		assertTrue(ProfitTrackerPlugin.isSupplyUseOption("Guzzle"));
		assertTrue(ProfitTrackerPlugin.isSupplyUseOption("Sip"));
	}

	@Test
	public void ignoresNonSupplyMenuOptions()
	{
		assertFalse(ProfitTrackerPlugin.isSupplyUseOption(null));
		assertFalse(ProfitTrackerPlugin.isSupplyUseOption("Use"));
		assertFalse(ProfitTrackerPlugin.isSupplyUseOption("Drop"));
		assertFalse(ProfitTrackerPlugin.isSupplyUseOption("Take"));
		assertFalse(ProfitTrackerPlugin.isSupplyUseOption("Wield"));
	}
}
