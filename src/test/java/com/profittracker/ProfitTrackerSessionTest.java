package com.profittracker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.runelite.client.game.ItemStack;
import org.junit.Test;

public class ProfitTrackerSessionTest
{
	private static final int COINS = 995;
	private static final int BONES = 526;
	private static final int RUNE_SCIMITAR = 1333;

	private final ProfitTrackerPriceLookup prices = itemId ->
	{
		switch (itemId)
		{
			case COINS:
				return 1;
			case BONES:
				return 120;
			case RUNE_SCIMITAR:
				return 15_000;
			default:
				return 0;
		}
	};

	@Test
	public void recordsNpcKillsLootAndProfit()
	{
		final ProfitTrackerSession session = new ProfitTrackerSession();

		session.recordKill("Guard", Arrays.asList(new ItemStack(COINS, 250), new ItemStack(BONES, 1)), prices);
		session.recordKill("Guard", Collections.singletonList(new ItemStack(COINS, 100)), prices);
		session.recordSupplyCost(70);
		session.recordSupplyCost(0);
		session.recordSupplyCost(-20);

		assertTrue(session.isStarted());
		assertEquals(2, session.getKillCount());
		assertEquals(470, session.getLootValue());
		assertEquals(70, session.getSupplyCost());
		assertEquals(400, session.getProfit());

		final ProfitTrackerTarget guard = onlyTarget(session);
		assertEquals("Guard", guard.getName());
		assertEquals(2, guard.getKills());
		assertEquals(470, guard.getLootValue());
		assertEquals(235, guard.getAverageKillValue());
		assertEquals(350, guard.getDrops().get(COINS).getQuantity());
		assertEquals(1, guard.getDrops().get(BONES).getQuantity());
	}

	@Test
	public void tracksMultipleNpcNamesSeparately()
	{
		final ProfitTrackerSession session = new ProfitTrackerSession();

		session.recordKill("Guard", Collections.singletonList(new ItemStack(COINS, 50)), prices);
		session.recordKill("Greater demon", Collections.singletonList(new ItemStack(RUNE_SCIMITAR, 1)), prices);

		final Map<String, ProfitTrackerTarget> targets = session.getTargets().stream()
			.collect(Collectors.toMap(ProfitTrackerTarget::getName, Function.identity()));

		assertEquals(2, session.getKillCount());
		assertEquals(15_050, session.getLootValue());
		assertEquals(1, targets.get("Guard").getKills());
		assertEquals(50, targets.get("Guard").getLootValue());
		assertEquals(1, targets.get("Greater demon").getKills());
		assertEquals(15_000, targets.get("Greater demon").getLootValue());
	}

	@Test
	public void ignoresInvalidDrops()
	{
		final ProfitTrackerSession session = new ProfitTrackerSession();

		session.recordKill("Rat", Arrays.asList(
			new ItemStack(-1, 1),
			new ItemStack(COINS, 0),
			new ItemStack(COINS, -4),
			new ItemStack(COINS, 12)
		), prices);

		assertEquals(1, session.getKillCount());
		assertEquals(12, session.getLootValue());
		assertEquals(12, onlyTarget(session).getLootValue());
	}

	@Test
	public void calculatesProfitPerHourFromElapsedSession()
	{
		final ProfitTrackerSession session = new ProfitTrackerSession();

		session.recordKill("Guard", Collections.singletonList(new ItemStack(COINS, 1_200)), prices);
		session.recordSupplyCost(200);
		session.setStartedAt(Instant.now().minusSeconds(1_800));

		assertEquals(1_000, session.getProfit());
		assertEquals(2_000, session.getProfitPerHour());
	}

	@Test
	public void resetClearsSession()
	{
		final ProfitTrackerSession session = new ProfitTrackerSession();

		session.recordKill("Guard", Collections.singletonList(new ItemStack(COINS, 250)), prices);
		session.recordSupplyCost(50);
		session.reset();

		assertFalse(session.isStarted());
		assertEquals(0, session.getKillCount());
		assertEquals(0, session.getLootValue());
		assertEquals(0, session.getSupplyCost());
		assertEquals(0, session.getProfit());
		assertTrue(session.getTargets().isEmpty());
	}

	@Test
	public void pausedSessionIgnoresKillsAndSupplyUse()
	{
		final ProfitTrackerSession session = new ProfitTrackerSession();

		session.recordKill("Guard", Collections.singletonList(new ItemStack(COINS, 250)), prices);
		session.togglePaused();
		session.recordKill("Guard", Collections.singletonList(new ItemStack(COINS, 999)), prices);
		session.recordSupplyCost(100);

		assertTrue(session.isPaused());
		assertEquals("Paused", session.getStatus());
		assertEquals(1, session.getKillCount());
		assertEquals(250, session.getLootValue());
		assertEquals(0, session.getSupplyCost());

		session.togglePaused();
		session.recordSupplyCost(25);

		assertFalse(session.isPaused());
		assertEquals("Tracking", session.getStatus());
		assertEquals(25, session.getSupplyCost());
	}

	@Test
	public void unstartedSessionCannotPause()
	{
		final ProfitTrackerSession session = new ProfitTrackerSession();

		session.togglePaused();

		assertFalse(session.isPaused());
		assertEquals("Waiting for loot", session.getStatus());
	}

	private static ProfitTrackerTarget onlyTarget(ProfitTrackerSession session)
	{
		assertEquals(1, session.getTargets().size());
		return session.getTargets().iterator().next();
	}
}
