package com.profittracker;

import lombok.Getter;

class ProfitTrackerDrop
{
	@Getter
	private final int itemId;

	@Getter
	private int quantity;

	@Getter
	private long value;

	ProfitTrackerDrop(int itemId)
	{
		this.itemId = itemId;
	}

	void add(int quantity, long value)
	{
		this.quantity += quantity;
		this.value += value;
	}
}
