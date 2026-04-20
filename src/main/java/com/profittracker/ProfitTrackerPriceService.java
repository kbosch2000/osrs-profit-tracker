package com.profittracker;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.game.ItemManager;

@Slf4j
class ProfitTrackerPriceService
{
	private static final String LATEST_PRICES_URL = "https://prices.runescape.wiki/api/v1/osrs/latest";
	private static final String USER_AGENT = "RuneLite Profit Tracker plugin - local development";

	private final ItemManager itemManager;
	private final ProfitTrackerConfig config;
	private final Map<Integer, Integer> wikiPrices = new ConcurrentHashMap<>();
	private ScheduledExecutorService executor;

	@Getter
	private Instant lastRefresh;

	@Getter
	private boolean usingWikiPrices;

	ProfitTrackerPriceService(ItemManager itemManager, ProfitTrackerConfig config)
	{
		this.itemManager = itemManager;
		this.config = config;
	}

	void start()
	{
		executor = Executors.newSingleThreadScheduledExecutor(runnable ->
		{
			final Thread thread = new Thread(runnable, "profit-tracker-prices");
			thread.setDaemon(true);
			return thread;
		});
		executor.execute(this::refresh);
		executor.scheduleWithFixedDelay(this::refresh, config.priceRefreshSeconds(), config.priceRefreshSeconds(), TimeUnit.SECONDS);
	}

	void stop()
	{
		if (executor != null)
		{
			executor.shutdownNow();
		}
	}

	int getLivePrice(int itemId)
	{
		final int canonicalId = itemManager.canonicalize(itemId);
		final Integer wikiPrice = wikiPrices.get(canonicalId);
		if (wikiPrice != null && wikiPrice > 0)
		{
			return wikiPrice;
		}

		final int runelitePrice = itemManager.getItemPrice(canonicalId);
		if (runelitePrice > 0)
		{
			return runelitePrice;
		}

		return 0;
	}

	private void refresh()
	{
		try
		{
			final HttpURLConnection connection = (HttpURLConnection) new URL(LATEST_PRICES_URL).openConnection();
			connection.setRequestProperty("User-Agent", USER_AGENT);
			connection.setConnectTimeout(5000);
			connection.setReadTimeout(10000);

			try (InputStreamReader reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))
			{
				final JsonObject root = new com.google.gson.JsonParser().parse(reader).getAsJsonObject();
				final JsonObject data = root.getAsJsonObject("data");
				final Map<Integer, Integer> refreshed = new ConcurrentHashMap<>();

				for (Map.Entry<String, JsonElement> entry : data.entrySet())
				{
					final int itemId = Integer.parseInt(entry.getKey());
					final JsonObject price = entry.getValue().getAsJsonObject();
					final int value = choosePrice(price);
					if (value > 0)
					{
						refreshed.put(itemId, value);
					}
				}

				wikiPrices.clear();
				wikiPrices.putAll(refreshed);
				lastRefresh = Instant.now();
				usingWikiPrices = true;
			}
		}
		catch (Exception ex)
		{
			usingWikiPrices = false;
			log.debug("Unable to refresh OSRS Wiki live prices", ex);
		}
	}

	private static int choosePrice(JsonObject price)
	{
		final int high = getInt(price, "high");
		final int low = getInt(price, "low");
		if (high > 0 && low > 0)
		{
			return (high + low) / 2;
		}
		return Math.max(high, low);
	}

	private static int getInt(JsonObject object, String key)
	{
		final JsonElement element = object.get(key);
		return element == null || element.isJsonNull() ? 0 : element.getAsInt();
	}
}
