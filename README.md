# Rydonut

[![Build](https://github.com/ReutMC/Rydonut/actions/workflows/build.yml/badge.svg)](https://github.com/ReutMC/Rydonut/actions/workflows/build.yml)
![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-62CD4F?logo=minecraft&logoColor=white)
![Fabric](https://img.shields.io/badge/Fabric%20Loader-0.19.5-DBB54C?logo=fabricmc&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-blue.svg)

**Rydonut** is a client-side visual awareness utility mod for Minecraft Java Edition **1.21.11** (Fabric), built for personal use on DonutSMP. It bundles a full ESP suite, a base finding & detection toolkit, an informative HUD, and a modern ClickGUI — everything rendered purely on the client, with no server-side components.

> **Environment:** `client` only — Rydonut does not need to be installed on any server, and it does not send extra packets or modify outgoing packets.

---

## Features

### ESP Suite
- **Player ESP** — box / outline / 2D modes, color-coded by health & distance
- **Name tags** — player name, health, held item and distance
- **Tracers** — lines from the player to nearby entities
- **Storage ESP** — highlights chests, barrels, shulker boxes, hoppers, etc.
- **Item ESP** — dropped item highlights with an optional value filter
- **Entity ESP** — hostile / passive / projectile filters
- **Block search ESP** — outline blocks you are searching for

### Base Finding & Detection
- **BaseFinder** — suspicion scoring over scanned chunks
- **RotatedDeepslateESP** — flags rotated deepslate (a classic stash/base indicator)
- **UnnaturalBlockESP** — flags blocks that don't generate naturally
- **StorageClusterESP** — detects clusters of storage blocks
- **SpawnerESP** — highlights mob spawners
- **Tunnel detection** — flags player-made tunnels
- **Light-level detection** — shows unusual light sources
- **Farm detection** — flags harvested/growing farm patterns
- **Portal ESP** — nether portal highlights with coordinate conversion (overworld ↔ nether)
- **Trail detection** — flags block trails left by players
- **NewChunks** — outlines newly generated chunks
- **PlayerTracker** — tracks player positions/movement
- **LogoutSpots** — marks where players last logged out
- **Chunk borders** — renders chunk boundary lines
- **Biome / slime overlay** — surface overlay for biome data and slime chunks
- **Ender-eye stronghold helper** — triangulates strongholds from thrown eyes
- **Waypoints** — named, persistent location markers
- **Hole / void ESP** — marks dangerous holes and void openings

### HUD
- **Watermark**, **FPS**, **ping**, **TPS estimate**
- **Coordinates** with nether converter
- **Durability** display for held gear
- **Animated ArrayList** of enabled modules
- **Draggable HUD editor**

### Misc
- **Fullbright**
- **Zoom**
- **Freecam**
- **ClickGUI** — modern, clean GUI (opens with **Right Shift**)

---

## Controls & Commands

| Input | Action |
|---|---|
| **Right Shift** | Opens the ClickGUI (rebindable in settings) |
| **ClickGUI** | Toggle modules, edit settings, drag HUD elements |

All in-game commands are on the client-side `/rydonut` command:

| Command | Description |
|---|---|
| `/rydonut gui` | Opens the ClickGUI |
| `/rydonut toggle <module>` | Toggles a module by name |
| `/rydonut reset` | Resets the current profile to defaults |
| `/rydonut debug` | Prints debug/diagnostic info |
| `/rydonut config save` | Saves the current profile |
| `/rydonut config load` | Loads the current profile |
| `/rydonut config export` | Exports the current profile JSON |
| `/rydonut config import` | Imports a profile JSON |
| `/rydonut waypoint add <name>` | Adds a waypoint at your current position |
| `/rydonut waypoint remove <name>` | Removes a waypoint by name |

---

## Config

- Profiles are stored as **JSON** in `.minecraft/rydonut/profiles/`
- Profiles are **autosaved** on change / game close
- Waypoints live in `waypoints.json` alongside the profiles
- Detection results ("finds") can be exported to `.minecraft/rydonut/finds/`

---

## Building Locally

### Prerequisites
- **JDK 25** to run Gradle (e.g. [Temurin 25](https://adoptium.net/)) — Loom 1.18 requires it; the mod itself still compiles to Java 21

### Steps
```bash
./gradlew build
```

- The first build **downloads Minecraft** and remaps it (via official Mojang mappings) — this can take a few minutes.
- The finished mod jar is at:

```
build/libs/rydonut-0.1.0.jar
```

> Use the plain `rydonut-0.1.0.jar` (not the `-sources` or `-dev` jars) when installing into `.minecraft/mods/`.

---

## Building with GitHub Actions

Rydonut ships with a ready-made workflow (`.github/workflows/build.yml`) so you never need to build locally:

### 1. Create the repository
Create a GitHub account (if you don't have one) and a new repository named **`Rydonut`**.

### 2. Push the project
```bash
git init
git add .
git commit -m "Initial Rydonut commit"
git branch -M main
git remote add origin https://github.com/ReutMC/Rydonut.git
git push -u origin main
```

### 3. Automatic builds
Every push (and pull request) triggers a **Build** workflow automatically.
When it finishes, download the jar from the run's **Artifacts** section (`Rydonut-<commit-sha>`).

### 4. Create a release
Push a tag starting with `v` and a GitHub Release is created automatically with the jar attached:
```bash
git tag v0.1.0
git push origin v0.1.0
```

### 5. Optional: custom release token
By default the release job uses the built-in `GITHUB_TOKEN`. If you prefer a custom token, add a repository secret named **`RELEASE_TOKEN`** (a Personal Access Token with **`contents: write`** permission). The workflow uses `RELEASE_TOKEN` when present and falls back to `GITHUB_TOKEN` otherwise.

> ⚠️ **NEVER** commit any token directly into the repository — tokens belong in repository **secrets** only.

---

## Versions

| Component | Version |
|---|---|
| Rydonut | 0.1.0 |
| Minecraft | 1.21.11 |
| Fabric Loader | 0.19.5 |
| Fabric Loom | 1.18.4 (`net.fabricmc.fabric-loom-remap`) |
| Fabric API | 0.141.6+1.21.11 |
| Mappings | Official Mojang mappings |
| Java | 21 |
| Gradle (wrapper) | 9.7.1 |

---

## Disclaimer

Rydonut is **purely client-side** and consists entirely of **read-only visual features**. It does not send extra packets and does not modify outgoing packets.

However, using utility mods **may violate the rules of some servers** (including DonutSMP). Use at **your own risk**. The authors are not responsible for any consequences resulting from the use of this software.

---

## License

[MIT](./LICENSE) — © 2025 Rydonut contributors
