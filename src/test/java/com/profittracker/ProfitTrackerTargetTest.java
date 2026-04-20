package com.profittracker;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ProfitTrackerTargetTest
{
	@Test
	public void estimatesProfitShareFromLootShare()
	{
		final ProfitTrackerTarget target = new ProfitTrackerTarget("Guard");
		target.recordKill(250);
		target.recordKill(750);

		assertEquals(400, target.getProfitShare(2_000, 5_000));
	}

	@Test
	public void profitShareIsZeroWithoutLoot()
	{
		final ProfitTrackerTarget target = new ProfitTrackerTarget("Guard");

		assertEquals(0, target.getProfitShare(2_000, 0));
	}
}
