<a id="readme-top"></a>

<p align="center">
  <img src="https://i.imgur.com/zlGbFm5.png" alt="Stweaks banner" />
</p>

<p align="center">
  <a href="https://github.com/StoryTime-Productions/Stweaks/actions/workflows/main.yml"><img src="https://github.com/StoryTime-Productions/Stweaks/actions/workflows/main.yml/badge.svg" alt="CI status" /></a>
  <img src="https://img.shields.io/badge/Paper-26.1.2-blue" alt="Paper 26.1.2" />
  <img src="https://img.shields.io/badge/Java-26-orange" alt="Java 26" />
</p>

Stweaks is a PaperMC plugin built for the StoryTime SMP - a collection of in-house vanilla tweaks (daily playtime tracking, teleport/lobby utilities, biome tracking, quests, pets, cosmetics, and a casino GUI) meant to make playing vanilla Minecraft together a bit more fun.

### Why Stweaks?

- We're looking to balance game time and promote more social gameplay through time-based mechanic implementations.
- We're looking to make our users laugh with inside-joke custom recipes that still add practical value to the game.
- We're looking to express ourselves both creatively through custom texturing, programming through datapacks, and scripting through plugin implementation.

Contributions, ideas, or feature requests are always welcome!

<p align="right">(<a href="#readme-top">back to top</a>)</p>

## What It Does

- **Stracker**: tracks daily active playtime and applies weekend/social participation multipliers toward a required-minutes goal.
- **Teleport utilities**: lobby and spawn teleport commands, with a configurable spawn location and allowed worlds.
- **Biome tracker**: a GUI for tracking discovered biomes.
- **Quests, Pets, Cosmetics, Casino**: dedicated GUIs for each system.
- **Boost**: a community-participation multiplier booster (work in progress).
- Ships a bundled resource pack (`src/main/resources/st-respack`) and datapack (`src/main/resources/st-datapack`), and integrates with SkinsRestorer, ProtocolLib, LibsDisguises, packetevents, and the StoryTime EDEN plugin.

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/ststatus` (alias `status`) | Shows how much time you've actively played today | `stweaks.status` (default: true) |
| `/stboost` (alias `boost`) | Boosts multiplier for community participation (WIP) | `stweaks.boost` (default: op) |
| `/stlobby` (alias `lobby`) | Teleport to the lobby | `stweaks.lobby` (default: true) |
| `/stspawn` (alias `spawn`) | Teleports you to the spawn location in the world | `stweaks.spawn` (default: true) |
| `/biometracker` | Opens the biome tracker GUI | `stweaks.biometracker` (default: true) |
| `/stquests` (alias `quests`) | Opens the quests GUI | `stweaks.quests` (default: true) |
| `/stpets` (alias `pets`) | Opens the pets GUI | `stweaks.stpets` (default: true) |
| `/stcosmetics` (alias `cosmetics`) | Opens the cosmetics GUI | `stweaks.cosmetics` (default: true) |
| `/stcasino` (alias `casino`) | Opens the casino GUI | none - open to all players. The `casino reload` subcommand is gated by op status (`isOp()`), not a permission node. |

`stweaks.*` grants all of the above permissions (default: false).

## Requirements

- Paper `26.1` (see `api-version` in `plugin.yml`; built against `paperDevBundle('26.1.2.build.+')`)
- Java 26 (toolchain), source/target compatibility 25
- Optional soft-dependencies: SkinsRestorer, ProtocolLib, Slimefun
- Requires the [EDEN](https://github.com/StoryTime-Productions/EDEN) plugin jar available at build time (see Build below)

## Getting Started (Developers)

### Prerequisites

- Gradle (or the included `gradlew` / `gradlew.bat` wrapper)
- Java 26: set `JAVA_HOME` accordingly
- A local build of [EDEN](https://github.com/StoryTime-Productions/EDEN) (defaults to `../../EDEN`, or set the `EDEN_PATH` environment variable to point elsewhere)
- PowerShell 5.1+ (for Windows users): used for deployment zip compression
- A `.env` file in the project root defining:
  ```env
  SERVER_PATH=path\to\your\server
  RESOURCE_PATH=path\to\your\resourcepacks
  ```

### Build

```
./gradlew build
```

Run `./gradlew spotlessApply` first to auto-format code (CI runs `spotlessApply` on PRs and expects clean formatting; it's skipped automatically when `CI` env var is set).

### Install

Drop the built shadow jar (from `build/libs/`) into your server's `plugins/` folder and restart.

Additional local dev steps:

1. For resource pack implementation, refer to `src/main/resources/st-respack`.
2. For datapack implementation, refer to `src/main/resources/st-datapack`.
3. The `deploy.bat` script in the repo root can update the plugin, resource pack, and datapack, assuming correct paths are set in `.env`.
4. Run `./set-hooks-path.sh` once to enable project-specific Git hooks.

## Configuration

Configuration lives in `src/main/resources/config.yml`:

- `spawn`: world/coordinates/allowed-worlds for `/stspawn`
- `lobby`: world/coordinates/yaw/pitch for `/stlobby`
- `required_minutes`: daily playtime goal for Stracker
- `afk_threshold_seconds`: AFK cutoff for playtime tracking
- `weekend_multiplier` / `multipliers`: base, weekend, social, social-distance, and social-cap multipliers for playtime
- `resource-pack`: enable/require a server resource pack, with a prompt message and pack URL/hash list

## CI/CD

GitHub Actions workflows under `.github/workflows/`:

- `pr.yml`: on pull requests to `main`: checks out EDEN, builds it, runs `spotlessApply`, then builds this plugin with Gradle and uploads build/test artifacts.
- `main.yml`: on push to `main` (and a weekly Saturday cron, to catch PaperMC upstream breakage): builds and tests, then runs a `release` Gradle task and uploads the release build.
- `commitlint.yml`: lints commit messages.
- `static.yml`: on push to `main` or manual dispatch: checks out EDEN, builds with Gradle (JDK 26 + 21), runs `./gradlew javadoc`, and deploys the generated Javadoc to GitHub Pages.
- `release.yml`: on a published GitHub release: posts a Discord notification (pre-release vs. full release, different webhooks) via `appleboy/discord-action`.
- `tag.yml`: on pushing a `v*` tag: runs `./gradlew release`, uploads build artifacts, and creates a draft GitHub Release (marked prerelease if the tag contains `-rc-`); a second job notifies Discord of the outcome.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md). In short: fork the repo, create a feature branch, commit your changes, push, and open a pull request. Issues and feature requests are also welcome via the [issue templates](.github/ISSUE_TEMPLATE).

### Top contributors

<a href="https://github.com/StoryTime-Productions/Stweaks/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=StoryTime-Productions/Stweaks" alt="contrib.rocks image" />
</a>

## Roadmap

- [x] Add Illegal Water
- [x] Add Lebron James armor set
- [x] Add FBI Disc
- [x] Add Cow Skinner
- [x] Add 1-hour Timer
- [ ] Add Nature Compass

See the [open issues](https://github.com/StoryTime-Productions/Stweaks/issues) for a full list of proposed features (and known issues).

## Contact

Nirav Patel - [@Niravanaa](https://github.com/Niravanaa) - niravp0703@gmail.com

StoryTime Productions: [Portfolio Link](https://storytime-productions.github.io/)

<p align="right">(<a href="#readme-top">back to top</a>)</p>

## License

GNU General Public License v3.0 - see [LICENSE](LICENSE).
