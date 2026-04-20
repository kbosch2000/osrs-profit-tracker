package com.profittracker;

import lombok.Getter;

class ProfitTrackerNotableDrop
{
	@Getter
	private final String npcName;

	@Getter
	private final int itemId;

	@Getter
	private final int quantity;

	@Getter
	private final long value;

	@Getter
	private final int killCount;

	ProfitTrackerNotableDrop(String npcName, int itemId, int quantity, long value, int killCount)
	{
		this.npcName = npcName;
		this.itemId = itemId;
		this.quantity = quantity;
		this.value = value;
		this.killCount = killCount;
	}
}
