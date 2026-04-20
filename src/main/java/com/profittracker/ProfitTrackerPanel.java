package com.profittracker;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

class ProfitTrackerPanel extends PluginPanel
{
	private static final DateTimeFormatter REFRESH_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

	private final ProfitTrackerSession session;
	private final Runnable resetCallback;
	private final JPanel content = new JPanel(new GridLayout(0, 1, 0, 6));

	ProfitTrackerPanel(ProfitTrackerSession session, Runnable resetCallback)
	{
		super(false);
		this.session = session;
		this.resetCallback = resetCallback;

		setLayout(new BorderLayout());
		content.setBackground(ColorScheme.DARK_GRAY_COLOR);

		final JButton reset = new JButton("Reset session");
		reset.addActionListener(event -> resetCallback.run());

		add(reset, BorderLayout.NORTH);
		add(new JScrollPane(content), BorderLayout.CENTER);
	}

	void rebuild(Instant lastRefresh, boolean usingWikiPrices)
	{
		content.removeAll();

		content.add(row("Elapsed", formatDuration(session.getElapsed())));
		content.add(row("Kills", Integer.toString(session.getKillCount())));
		content.add(row("Loot", formatGp(session.getLootValue())));
		content.add(row("Supplies", formatGp(session.getSupplyCost())));
		content.add(row("Profit", formatGp(session.getProfit())));
		content.add(row("Profit/hr", formatGp(session.getProfitPerHour())));
		content.add(row("Prices", priceStatus(lastRefresh, usingWikiPrices)));

		for (ProfitTrackerTarget target : session.getTargets())
		{
			final JPanel targetPanel = new JPanel(new GridLayout(0, 1, 0, 2));
			targetPanel.setBorder(BorderFactory.createTitledBorder(target.getName()));
			targetPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			targetPanel.add(row("Kills", Integer.toString(target.getKills())));
			targetPanel.add(row("Loot", formatGp(target.getLootValue())));
			targetPanel.add(row("Avg kill", formatGp(target.getAverageKillValue())));
			content.add(targetPanel);
		}

		content.revalidate();
		content.repaint();
	}

	private static JPanel row(String label, String value)
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

	private static String priceStatus(Instant lastRefresh, boolean usingWikiPrices)
	{
		if (lastRefresh == null)
		{
			return "loading";
		}
		return (usingWikiPrices ? "live " : "fallback ") + REFRESH_FORMAT.format(lastRefresh);
	}

	private static String formatDuration(Duration duration)
	{
		final long seconds = duration.getSeconds();
		final long hours = seconds / 3600;
		final long minutes = seconds % 3600 / 60;
		final long rest = seconds % 60;
		return String.format("%02d:%02d:%02d", hours, minutes, rest);
	}

	private static String formatGp(long value)
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
