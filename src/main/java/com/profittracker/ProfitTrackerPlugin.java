package com.profittracker;

import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.NPC;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;

@Slf4j
@PluginDescriptor(
	name = "NPC Profit Tracker",
	description = "Tracks NPC kill loot, supply cost, and profit per hour with live OSRS Wiki prices.",
	tags = {"boss", "loot", "npc", "profit", "supplies"}
)
public class ProfitTrackerPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ItemManager itemManager;

	@Inject
	private ProfitTrackerConfig config;

	private ProfitTrackerPriceService priceService;
	private ProfitTrackerPanel panel;
	private NavigationButton navButton;
	private final Map<Integer, Integer> lastInventory = new HashMap<>();
	private final ProfitTrackerSession session = new ProfitTrackerSession();
	private long lastInventoryValue;
	private boolean hasInventorySnapshot;

	@Override
	protected void startUp()
	{
		priceService = new ProfitTrackerPriceService(itemManager, config);
		priceService.start();

		panel = new ProfitTrackerPanel(session, this::resetSession);
		navButton = NavigationButton.builder()
			.tooltip("Profit Tracker")
			.icon(createIcon())
			.priority(6)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);

		captureInventorySnapshot();
		refreshPanel();
		log.debug("Profit Tracker started");
	}

	@Override
	protected void shutDown()
	{
		clientToolbar.removeNavigation(navButton);
		priceService.stop();
		lastInventory.clear();
		hasInventorySnapshot = false;
		session.reset();
		log.debug("Profit Tracker stopped");
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			captureInventorySnapshot();
		}
	}

	@Subscribe
	public void onNpcLootReceived(NpcLootReceived event)
	{
		final NPC npc = event.getNpc();
		final String npcName = npc == null || npc.getName() == null ? "Unknown NPC" : npc.getName();
		final boolean wasEmpty = session.getKillCount() == 0;

		session.recordKill(npcName, event.getItems(), priceService);

		if (wasEmpty && config.announceNewSession())
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Profit Tracker started for " + npcName + ".", null);
		}

		refreshPanel();
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() != InventoryID.INVENTORY.getId())
		{
			return;
		}

		final Map<Integer, Integer> current = toQuantityMap(event.getItemContainer());
		final long currentValue = inventoryValue(current);

		if (config.countSupplyLosses() && hasInventorySnapshot && session.isStarted())
		{
			final long valueLost = lastInventoryValue - currentValue;
			if (valueLost > 0)
			{
				session.recordSupplyCost(valueLost);
				refreshPanel();
			}
		}

		lastInventory.clear();
		lastInventory.putAll(current);
		lastInventoryValue = currentValue;
		hasInventorySnapshot = true;
	}

	void resetSession()
	{
		session.reset();
		captureInventorySnapshot();
		refreshPanel();
	}

	private void captureInventorySnapshot()
	{
		final ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
		lastInventory.clear();
		lastInventory.putAll(toQuantityMap(inventory));
		lastInventoryValue = inventoryValue(lastInventory);
		hasInventorySnapshot = true;
	}

	private Map<Integer, Integer> toQuantityMap(ItemContainer container)
	{
		final Map<Integer, Integer> quantities = new HashMap<>();
		if (container == null)
		{
			return quantities;
		}

		Arrays.stream(container.getItems())
			.filter(item -> item.getId() > 0 && item.getQuantity() > 0)
			.forEach(item ->
			{
				final int id = itemManager.canonicalize(item.getId());
				quantities.merge(id, item.getQuantity(), Integer::sum);
			});
		return quantities;
	}

	private long inventoryValue(Map<Integer, Integer> quantities)
	{
		long total = 0;
		for (Map.Entry<Integer, Integer> entry : quantities.entrySet())
		{
			total += (long) priceService.getLivePrice(entry.getKey()) * entry.getValue();
		}
		return total;
	}

	private void refreshPanel()
	{
		if (panel == null)
		{
			return;
		}

		SwingUtilities.invokeLater(() -> panel.rebuild(priceService.getLastRefresh(), priceService.isUsingWikiPrices()));
	}

	private static BufferedImage createIcon()
	{
		final BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D graphics = image.createGraphics();
		graphics.setColor(new Color(42, 136, 86));
		graphics.fillRoundRect(1, 1, 14, 14, 4, 4);
		graphics.setColor(new Color(245, 211, 93));
		graphics.fillOval(4, 3, 8, 8);
		graphics.setColor(new Color(32, 32, 32));
		graphics.drawString("g", 5, 13);
		graphics.dispose();
		return image;
	}

	@Provides
	ProfitTrackerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ProfitTrackerConfig.class);
	}
}
