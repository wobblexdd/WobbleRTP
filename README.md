# WobbleRTP

WobbleRTP is a modern, high-performance random teleport plugin with a GUI-based interface and asynchronous location search. It is designed for SMP and large-scale servers where performance, safety, and user experience are critical.

## Features

- GUI-based teleport system (Overworld / Nether / End)
- Asynchronous and hybrid location search (no TPS drops)
- Safe teleport algorithm (no lava, void, or unsafe positions)
- Intelligent Nether handling (no bedrock roof teleports)
- Countdown system with cancel-on-move
- Cooldown system with bypass permissions
- Fully configurable (radius, worlds, messages, sounds)
- Integration-ready with Wobble ecosystem

## GUI

Players use `/rtp` to open a menu and select a dimension:
- Overworld
- Nether
- The End

Each option includes lore, feedback, and visual clarity.

## Performance

WobbleRTP uses a hybrid teleport algorithm:

1. Fast Path  
   Attempts to find a safe location in already loaded chunks (instant, zero lag)

2. Guaranteed Path  
   If needed, safely loads a chunk asynchronously to find a valid location

This ensures:
- No server freezes
- No TPS/MSPT impact
- Consistent teleport success

## Safety

The plugin guarantees:
- No teleport into blocks
- No lava or void deaths
- No unsafe fall heights
- Valid ground detection

## Commands

/rtp — Open teleport GUI  
/rtp reload — Reload configuration  

## Permissions

wobble.rtp.use — Use /rtp  
wobble.rtp.admin — Reload config  
wobble.rtp.bypass.cooldown — Ignore cooldown  
wobble.rtp.bypass.move — Ignore movement cancel  

## Configuration

```yml
min-distance: 1000
max-distance: 10000

teleport-retries: 40

worlds:
  overworld: "world"
  nether: "world_nether"
  the_end: "world_the_end"

countdown:
  seconds: 3
  cancel-on-move: true

cooldown:
  seconds: 60

messages:
  teleport-countdown: "&7Teleporting in &b{seconds}s"
  teleport-success: "&7You teleported to a random location"
  failed: "&cFailed to find safe location"
````

## How it works

* Player selects a dimension in GUI
* Plugin starts async search for safe coordinates
* Countdown begins
* Movement cancels teleport (if enabled)
* Player is teleported safely

## Notes

* Designed for high-performance servers
* Does not freeze main thread
* Works without modifying other plugins
* Safe for production environments
