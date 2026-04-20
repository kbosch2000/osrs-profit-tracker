# Profit Tracker

RuneLite external plugin for tracking NPC kill loot, supply costs, and profit per hour.

## Current behavior

- Starts a session when NPC loot is received.
- Tracks kills and loot value per NPC name.
- Estimates supply cost from inventory value decreases during the active session.
- Refreshes live prices from the OSRS Wiki real-time price API and falls back to RuneLite prices when unavailable.
- Adds a RuneLite sidebar panel with session totals and per-NPC breakdowns.

This plugin observes game/client events only. It does not automate gameplay or send inputs.
