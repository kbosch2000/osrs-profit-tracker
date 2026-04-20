package com.profittracker;

import static org.junit.Assert.assertEquals;

import com.google.gson.JsonObject;
import org.junit.Test;

public class ProfitTrackerPriceServiceTest
{
	@Test
	public void averagesHighAndLowWhenBothExist()
	{
		assertEquals(150, ProfitTrackerPriceService.choosePrice(price(200, 100)));
	}

	@Test
	public void fallsBackToOnlyAvailableSide()
	{
		assertEquals(200, ProfitTrackerPriceService.choosePrice(price(200, null)));
		assertEquals(100, ProfitTrackerPriceService.choosePrice(price(null, 100)));
	}

	@Test
	public void returnsZeroWhenNoPriceExists()
	{
		assertEquals(0, ProfitTrackerPriceService.choosePrice(price(null, null)));
		assertEquals(0, ProfitTrackerPriceService.choosePrice(price(0, 0)));
	}

	private static JsonObject price(Integer high, Integer low)
	{
		final JsonObject object = new JsonObject();
		if (high == null)
		{
			object.add("high", null);
		}
		else
		{
			object.addProperty("high", high);
		}

		if (low == null)
		{
			object.add("low", null);
		}
		else
		{
			object.addProperty("low", low);
		}

		return object;
	}
}
