package com.profittracker;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

class ProfitTrackerPanel extends PluginPanel
{
	private static final DateTimeFormatter REFRESH_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

	private final ProfitTrackerSession session;
	private final ItemManager itemManager;
	private final ProfitTrackerConfig config;
	private final Runnable resetCallback;
	private final Runnable pauseCallback;
	private final JPanel content = new JPanel();
	private final JButton pause = new JButton("Pause");

	ProfitTrackerPanel(
		ProfitTrackerSession session,
		ItemManager itemManager,
		ProfitTrackerConfig config,
		Runnable resetCallback,
		Runnable pauseCallback
	)
	{
		super(false);
		this.session = session;
		this.itemManager = itemManager;
		this.config = config;
		this.resetCallback = resetCallback;
		this.pauseCallback = pauseCallback;

		setLayout(new BorderLayout());
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setBackground(ColorScheme.DARK_GRAY_COLOR);

		final JPanel controls = new JPanel(new GridLayout(1, 2, 6, 0));
		controls.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
		controls.setBackground(ColorScheme.DARK_GRAY_COLOR);

		final JButton reset = new JButton("Reset");
		reset.addActionListener(event -> resetCallback.run());
		pause.addActionListener(event -> pauseCallback.run());
		controls.add(reset);
		controls.add(pause);

		add(controls, BorderLayout.NORTH);
		add(new JScrollPane(content), BorderLayout.CENTER);
	}

	void rebuild(Instant lastRefresh, boolean usingWikiPrices)
	{
		content.removeAll();
		pause.setText(session.isPaused() ? "Resume" : "Pause");
		pause.setEnabled(session.isStarted());

		content.add(section("Session",
			row("Status", session.getStatus()),
			row("Elapsed", formatDuration(session.getElapsed())),
			row("Kills", Integer.toString(session.getKillCount())),
			row("Loot", formatGp(session.getLootValue())),
			row("Supplies", formatGp(session.getSupplyCost())),
			row("Profit", formatGp(session.getProfit())),
			row("Profit/hr", formatGp(session.getProfitPerHour())),
			row("Prices", priceStatus(lastRefresh, usingWikiPrices))
		));

		if (session.getTargets().isEmpty())
		{
			content.add(message("Kill an NPC to start tracking this trip."));
		}

		for (ProfitTrackerTarget target : session.getTargets())
		{
			content.add(targetSection(target));
		}

		content.revalidate();
		content.repaint();
	}

	private JPanel targetSection(ProfitTrackerTarget target)
	{
		final JPanel targetPanel = section(target.getName(),
			row("Kills", Integer.toString(target.getKills())),
			row("Loot", formatGp(target.getLootValue())),
			row("Avg kill", formatGp(target.getAverageKillValue())),
			row("Profit share", formatGp(target.getProfitShare(session.getProfit(), session.getLootValue())))
		);

		if (config.showDropBreakdown() && !target.getDrops().isEmpty())
		{
			targetPanel.add(subtitle("Top drops"));
			target.getDrops().values().stream()
				.sorted(Comparator.comparingLong(ProfitTrackerDrop::getValue).reversed())
				.limit(config.maxDropsShown())
				.forEach(drop -> targetPanel.add(row(itemName(drop.getItemId()) + " x" + drop.getQuantity(), formatGp(drop.getValue()))));
		}

		return targetPanel;
	}

	private String itemName(int itemId)
	{
		try
		{
			return itemManager.getItemComposition(itemId).getName();
		}
		catch (Exception ex)
		{
			return "Item " + itemId;
		}
	}

	private static JPanel section(String title, JPanel... rows)
	{
		final JPanel section = new JPanel();
		section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
		section.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		section.setBorder(BorderFactory.createTitledBorder(title));
		section.setMaximumSize(new Dimension(Integer.MAX_VALUE, section.getPreferredSize().height));
		for (JPanel row : rows)
		{
			section.add(row);
		}
		return section;
	}

	private static JPanel message(String text)
	{
		final JPanel panel = new JPanel(new BorderLayout());
		panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		panel.setBorder(BorderFactory.createEmptyBorder(8, 6, 8, 6));
		panel.add(new JLabel(text), BorderLayout.CENTER);
		return panel;
	}

	private static JPanel subtitle(String text)
	{
		final JPanel panel = new JPanel(new BorderLayout());
		panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		panel.setBorder(BorderFactory.createEmptyBorder(8, 6, 2, 6));
		panel.add(new JLabel(text), BorderLayout.WEST);
		return panel;
	}

	static JPanel row(String label, String value)
	{
		final JPanel row = new JPanel(new BorderLayout());
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		row.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));

		final JLabel name = new JLabel(label);
		final JLabel amount = new JLabel(value, SwingConstants.RIGHT);
		row.add(name, BorderLayout.WEST);
		row.add(amount, BorderLayout.EAST);
		return row;
	}

	static String priceStatus(Instant lastRefresh, boolean usingWikiPrices)
	{
		if (lastRefresh == null)
		{
			return "loading";
		}
		return (usingWikiPrices ? "live " : "fallback ") + REFRESH_FORMAT.format(lastRefresh);
	}

	static String formatDuration(Duration duration)
	{
		final long seconds = duration.getSeconds();
		final long hours = seconds / 3600;
		final long minutes = seconds % 3600 / 60;
		final long rest = seconds % 60;
		return String.format("%02d:%02d:%02d", hours, minutes, rest);
	}

	static String formatGp(long value)
	{
		final String sign = value < 0 ? "-" : "";
		final long absolute = Math.abs(value);
		if (absolute >= 1_000_000)
		{
			return sign + String.format("%.2fm", absolute / 1_000_000.0);
		}
		if (absolute >= 1_000)
		{
			return sign + String.format("%.1fk", absolute / 1_000.0);
		}
		return sign + absolute + " gp";
	}
}
