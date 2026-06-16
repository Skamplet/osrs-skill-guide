# 1-99 Skill Guide (RuneLite plugin)

A RuneLite plugin that works like a "WoW quest helper" for skilling in Old School RuneScape:

- **1-99 guide per skill** with recommended training methods grouped into level ranges.
- **Quantity calculations based on YOUR own XP** — the plugin reads your actual XP live from the client and works out *how many* ores/logs/fish/actions you need to reach the next level and your target level.
- **Quest status read from the client** — shows whether recommended quests (Waterfall Quest, The Tourist Trap, Fight Arena …) are completed, in progress, or not started.
- **Self-updating** — the training data lives in a JSON file online; the plugin fetches it on start-up, so the guide can be updated without a new plugin version.
- **Works alongside Quest Helper** — the step-by-step walkthrough (arrows, what to click) is left to the existing [Quest Helper](https://github.com/Zoinkwiz/quest-helper) plugin. This plugin tells you *which* quests are worth doing and *why*, and points you to Quest Helper for the actual completion.

## Why reuse Quest Helper?
Quest Helper already supports nearly every OSRS quest with a full step-by-step overlay. Reimplementing that would be months of work. Install it from the RuneLite Plugin Hub alongside this plugin — then you have both the "what should I do" guide and the "how" walkthrough.

## Project structure
```
osrs-skill-guide/
├─ build.gradle / settings.gradle      # Standard RuneLite Plugin Hub build
├─ runelite-plugin.properties          # Plugin Hub metadata
└─ src/main/java/com/skillguide/
   ├─ SkillGuidePlugin.java            # Plugin entry point, nav button, events
   ├─ SkillGuideConfig.java            # Settings (URL, auto-update, target level)
   ├─ GuideDataManager.java            # Fetches/caches/falls back the guide JSON  ← self-update
   ├─ GuideCalculator.java             # XP → quantity calculations (unit-tested)
   ├─ model/                           # GuideData, SkillGuide, TrainingMethod, QuestRecommendation
   └─ ui/SkillGuidePanel.java          # The side panel
   resources/com/skillguide/
   ├─ guide-data.json                  # Bundled fallback guide + schema example
   └─ icon.png
src/test/java/com/skillguide/
   ├─ SkillGuideDevLauncher.java       # Launches an isolated dev client (gradlew run)
   └─ GuideCalculatorTest.java         # Unit tests
```

## How self-update works
1. On start-up local data is loaded immediately (disk cache, otherwise the bundled `guide-data.json`), so the panel is never empty.
2. Then `guideUrl` is fetched in the background. If it succeeds, the panel updates and the file is cached to disk.
3. If the network fails, the most recently cached/bundled version is used.

### Hosting your own data file
1. Create a public repo, e.g. `osrs-skill-guide-data`, with a `guide-data.json` that follows the schema in [`src/main/resources/com/skillguide/guide-data.json`](src/main/resources/com/skillguide/guide-data.json).
2. Use the **raw** URL (`https://raw.githubusercontent.com/<user>/<repo>/main/guide-data.json`).
3. Set it as `guideUrl` in the plugin settings (the default already points at a placeholder with your username).
4. Bump `version` each time you update it — that way the user sees the new version number in the panel.

The data can be filled in from the OSRS wiki (XP tables, method guides) and other sites. Keep `xpPerAction` and the level ranges up to date there.

## Build
Requires **JDK 11** and Gradle. The project has `gradle/wrapper/gradle-wrapper.properties`, but the `gradle-wrapper.jar` + `gradlew` scripts are binary and aren't included here. Get them in one of two ways:

```powershell
# A) If you have Gradle installed (generates the wrapper files):
gradle wrapper --gradle-version 7.6.4

# B) Or copy gradlew, gradlew.bat and gradle/wrapper/gradle-wrapper.jar
#    from https://github.com/runelite/example-plugin
```

Then:
```powershell
./gradlew build      # compiles + runs tests
./gradlew test       # tests only
```

## Running a dev client
```powershell
./gradlew run                            # normal
./gradlew run --args="--developer-mode"  # with RuneLite dev tools
```
This launches a RuneLite client with the plugin already loaded. It uses its own config folder (`.dev-home/.runelite`), so it never touches your real `~/.runelite` or your normal plugin settings. Use a test account when logging in.

## Schema for guide-data.json
- `version` (int), `updated` (string)
- `skills[]`: `{ skill, members?, methods[] }` where `skill` matches `net.runelite.api.Skill` (e.g. `MINING`)
  - `methods[]`: `{ name, minLevel, maxLevel, xpPerAction, actionName, members?, notes?, wikiUrl? }`
- `questGuides[]`: `{ name, quest, members?, reason, recommendedAtLevel, skillRewards{}, questHelperName, wikiUrl? }`
- `members` (boolean, default false): mark members-only content. It is hidden when the **Game mode** setting is Free-to-play (or Auto on a F2P world).
  - `quest` matches `net.runelite.api.Quest` (e.g. `WATERFALL_QUEST`, `THE_TOURIST_TRAP`)

## Status / next steps
This is a working skeleton. Obvious extensions:
- Show material consumption (e.g. number of bars/planks), not just action counts.
- A "next recommended action" tip at the top across skills.
- Clickable wiki links and a button that opens Quest Helper directly on a quest.
- More skills and finer-grained level ranges in the data file.
