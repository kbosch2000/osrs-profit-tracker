package com.profittracker;

import static org.junit.Assert.assertEquals;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.junit.Test;

public class ProfitTrackerPanelTest
{
	@Test
	public void formatsGpValues()
	{
		assertEquals("999 gp", ProfitTrackerPanel.formatGp(999));
		assertEquals("1.0k", ProfitTrackerPanel.formatGp(1_000));
		assertEquals("15.4k", ProfitTrackerPanel.formatGp(15_420));
		assertEquals("2.50m", ProfitTrackerPanel.formatGp(2_500_000));
		assertEquals("-2.50m", ProfitTrackerPanel.formatGp(-2_500_000));
	}

	@Test
	public void formatsDuration()
	{
		assertEquals("00:00:00", ProfitTrackerPanel.formatDuration(Duration.ZERO));
		assertEquals("00:01:05", ProfitTrackerPanel.formatDuration(Duration.ofSeconds(65)));
		assertEquals("02:03:04", ProfitTrackerPanel.formatDuration(Duration.ofSeconds(7_384)));
	}

	@Test
	public void formatsPriceStatus()
	{
		final Instant refresh = Instant.parse("2026-04-20T01:23:45Z");
		final String time = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault()).format(refresh);

		assertEquals("loading", ProfitTrackerPanel.priceStatus(null, false));
		assertEquals("live " + time, ProfitTrackerPanel.priceStatus(refresh, true));
		assertEquals("fallback " + time, ProfitTrackerPanel.priceStatus(refresh, false));
	}

	@Test
	public void buildsRows()
	{
		assertEquals(2, ProfitTrackerPanel.row("Loot", "1.0k").getComponentCount());
	}
}
