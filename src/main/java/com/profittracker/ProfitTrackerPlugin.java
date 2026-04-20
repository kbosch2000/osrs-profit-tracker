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
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuOptionClicked;
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
	private final Map<Integer, Integer> lastCarriedItems = new HashMap<>();
	private final ProfitTrackerSession session = new ProfitTrackerSession();
	private final ProfitTrackerInventory inventory = new ProfitTrackerInventory();
	private long lastCarriedValue;
	private boolean hasCarriedSnapshot;
	private boolean carriedItemsDirty;
	private boolean pendingSupplyAction;
	private boolean pendingIgnoredInventoryAction;

	@Override
	protected void startUp()
	{
		priceService = new ProfitTrackerPriceService(itemManager, config);
		priceService.start();

		panel = new ProfitTrackerPanel(session, itemManager, config, this::resetSession, this::togglePaused);
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
		lastCarriedItems.clear();
		hasCarriedSnapshot = false;
		carriedItemsDirty = false;
		pendingSupplyAction = false;
		pendingIgnoredInventoryAction = false;
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

		session.recordKill(npcName, event.getItems(), priceService, config.notableDropThreshold());

		if (wasEmpty && config.announceNewSession())
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Profit Tracker started for " + npcName + ".", null);
		}

		refreshPanel();
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		final String option = event.getMenuOption();
		if (isSupplyUseOption(option))
		{
			pendingSupplyAction = true;
		}
		else if (isIgnoredInventoryOption(option))
		{
			pendingIgnoredInventoryAction = true;
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() != InventoryID.INVENTORY.getId() && event.getContainerId() != InventoryID.EQUIPMENT.getId())
		{
			return;
		}

		carriedItemsDirty = true;
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (!carriedItemsDirty)
		{
			return;
		}

		carriedItemsDirty = false;
		final Map<Integer, Integer> current = carriedItems();
		final long currentValue = inventoryValue(current);

		maybeRecordSupplyLoss(currentValue);

		pendingSupplyAction = false;
		pendingIgnoredInventoryAction = false;
		lastCarriedItems.clear();
		lastCarriedItems.putAll(current);
		lastCarriedValue = currentValue;
		hasCarriedSnapshot = true;
	}

	private void maybeRecordSupplyLoss(long currentValue)
	{
		if (!config.countSupplyLosses() || !hasCarriedSnapshot || !session.isStarted() || session.isPaused())
		{
			return;
		}

		final boolean countPassiveLoss = config.countPassiveResourceLosses() && !pendingIgnoredInventoryAction;
		if (!pendingSupplyAction && !countPassiveLoss)
		{
			return;
		}

		final long valueLost = lastCarriedValue - currentValue;
		if (valueLost > 0)
		{
			session.recordSupplyCost(valueLost);
			refreshPanel();
		}
	}

	void resetSession()
	{
		session.reset();
		captureInventorySnapshot();
		refreshPanel();
	}

	void togglePaused()
	{
		session.togglePaused();
		captureInventorySnapshot();
		refreshPanel();
	}

	private void captureInventorySnapshot()
	{
		final Map<Integer, Integer> carried = carriedItems();
		lastCarriedItems.clear();
		lastCarriedItems.putAll(carried);
		lastCarriedValue = inventoryValue(lastCarriedItems);
		hasCarriedSnapshot = true;
		carriedItemsDirty = false;
		pendingSupplyAction = false;
		pendingIgnoredInventoryAction = false;
	}

	private Map<Integer, Integer> carriedItems()
	{
		final Map<Integer, Integer> quantities = new HashMap<>();
		mergeContainer(quantities, client.getItemContainer(InventoryID.INVENTORY));
		mergeContainer(quantities, client.getItemContainer(InventoryID.EQUIPMENT));
		return quantities;
	}

	private Map<Integer, Integer> toQuantityMap(ItemContainer container)
	{
		final Map<Integer, Integer> quantities = new HashMap<>();
		mergeContainer(quantities, container);
		return quantities;
	}

	private void mergeContainer(Map<Integer, Integer> quantities, ItemContainer container)
	{
		if (container == null)
		{
			return;
		}

		Arrays.stream(container.getItems())
			.filter(item -> item.getId() > 0 && item.getQuantity() > 0)
			.forEach(item ->
			{
				final int id = itemManager.canonicalize(item.getId());
				quantities.merge(id, item.getQuantity(), Integer::sum);
			});
	}

	private long inventoryValue(Map<Integer, Integer> quantities)
	{
		return inventory.value(quantities, priceService);
	}

	static boolean isSupplyUseOption(String option)
	{
		if (option == null)
		{
			return false;
		}

		final String normalized = option.toLowerCase();
		return normalized.equals("eat")
			|| normalized.equals("drink")
			|| normalized.equals("consume")
			|| normalized.equals("guzzle")
			|| normalized.equals("sip");
	}

	static boolean isIgnoredInventoryOption(String option)
	{
		if (option == null)
		{
			return false;
		}

		final String normalized = option.toLowerCase();
		return normalized.equals("drop")
			|| normalized.equals("deposit")
			|| normalized.equals("deposit-all")
			|| normalized.equals("deposit-all-but-one")
			|| normalized.equals("bank")
			|| normalized.equals("store")
			|| normalized.equals("offer")
			|| normalized.equals("sell")
			|| normalized.equals("wield")
			|| normalized.equals("wear")
			|| normalized.equals("remove")
			|| normalized.equals("take")
			|| normalized.equals("withdraw");
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
