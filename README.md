# LordLox's Magical Wands

A Minecraft mod for NeoForge 26.1 that adds craftable magical wands and an expanded 5x5 crafting table.

[![Minecraft](https://img.shields.io/badge/Minecraft-26.1-2b8a3e)](https://www.minecraft.net/)
[![NeoForge](https://img.shields.io/badge/NeoForge-26.1.x-e8590c)](https://neoforged.net/)
[![Java](https://img.shields.io/badge/Java-25-c92a2a)](https://adoptium.net/)
[![License](https://img.shields.io/badge/License-All_Rights_Reserved-5c7cfa)](LICENSE.txt)

---

## Features

### Wand Workbench
A custom **5x5 crafting table** required to craft all wands. It is crafted in a vanilla crafting table using Gold Ingots, Amethyst Shards, and Phantom Membranes.

### Wands

- **Gravity Wand**
  - **Right-Click:** Knocks back and launches nearby entities into the air.
  - **Shift + Right-Click:** Inverts the force, pulling nearby entities toward you.

- **Black Hole Wand**
  - **Right-Click:** Spawns a gravitational singularity at the target location that pulls in nearby entities and deals void damage.

- **Spawn Portal Wand**
  - **Shift + Right-Click:** Opens the mob selection grimoire to browse and favorite mobs.
  - **Shift + Mouse Wheel:** Cycle through favorited mobs in hand.
  - **Right-Click:** Opens a portal that spawns the selected mob (costs Diamonds).

- **Gambling Wand**
  - **Shift + Right-Click:** Opens the betting interface (wager 0–32 Diamonds to improve roll probabilities).
  - **Right-Click:** Rolls an animated 3D D20 die.
    - **1:** Critical failure (lightning strike / penalty).
    - **2–7:** Spawns hostile mobs.
    - **8–14:** Common drops or passive creatures.
    - **15–19:** High-tier ores and resources.
    - **20:** Jackpot (64 Diamonds and fireworks).

---

## Recipes

### Wand Workbench (Vanilla 3x3)

```
[Phantom Membrane] [   Gold Ingot   ] [Phantom Membrane]
[   Gold Ingot   ] [ Crafting Table ] [   Gold Ingot   ]
[ Amethyst Shard ] [   Gold Ingot   ] [ Amethyst Shard ]
```

### Wands (5x5 Wand Workbench)

All wands follow the same base pattern:

```
[Membrane] [Catalyst] [Catalyst] [Catalyst] [Membrane]
[Membrane] [  Gold  ] [  Stick ] [  Gold  ] [Membrane]
[        ] [  Gold  ] [  Stick ] [  Gold  ] [        ]
[        ] [  Gold  ] [  Stick ] [  Gold  ] [        ]
[        ] [  Gold  ] [  Stick ] [  Gold  ] [        ]
```

| Wand | Catalysts (top row) |
| :--- | :--- |
| **Gravity Wand** | 3x Amethyst Shard |
| **Black Hole Wand** | 3x Echo Shard |
| **Spawn Portal Wand** | 1x End Crystal, 2x Dragon's Breath |
| **Gambling Wand** | 3x Diamond |

---

## Installation

1. Install **NeoForge 26.1** (`26.1.0.19-beta` or higher).
2. Download the mod `.jar` from [Releases](https://github.com/LordLox/lordlox_magical_wand/releases).
3. Place the `.jar` inside your `.minecraft/mods` directory.

---

## Building from Source

Requires **JDK 25**.

```bash
git clone https://github.com/LordLox/lordlox_magical_wand.git
cd lordlox_magical_wand

# Linux / macOS
./gradlew build

# Windows
.\gradlew.bat build
```

The output file will be generated in `build/libs/`.

To test in dev:
```bash
./gradlew runClient
```

---

## Modpacks & Content Creation

Feel free to include this mod in any modpack or showcase it in videos/streams.

## License

All Rights Reserved. See [LICENSE.txt](LICENSE.txt).
