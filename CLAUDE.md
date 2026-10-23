# Reign of Nether

Minecraft Forge mod that adds RTS gameplay (units, buildings, abilities, research, fog of war) to Minecraft.

## Tech stack
- Minecraft 1.20.1, Forge 47.4.0, Parchment mappings 2023.09.03-1.20.1
- Java 17, ForgeGradle 5.1
- The current mod version is the `version` in `build.gradle`. Don't hardcode it anywhere.
- Branches: the latest dev branch is usually `1.20.1-<version>`. PRs target `1.20.1-dev`.

## Commands
- Compile: `./gradlew compileJava`
- Run client: `./gradlew runClient`
- Run server: `./gradlew runServer`
- Build jar: `./gradlew build`
- Do not edit `build/`, `run/` or `.gradle/`. They are generated or local.
- `build.gradle` is listed in `.gitignore`, so check before assuming changes to it are tracked.

## Naming patterns
- `*ClientEvents` / `*ServerEvents`: event handlers and logic for a feature on each side.
- `*ClientboundPacket` / `*ServerboundPacket`: network packets
- `*SaveData`: persistent world data.
- `*Action`: enum of the action types a packet or feature carries.
- `*Unit`, `*Prod`, `Research*`: unit, unit production entry, and research upgrade classes.

## Client/server rules
- Never reference client-only classes from server-side code or vice versa
- The server is the authority on state changes. The client sends requests and mirrors the result.
- For packets, synced state, event handlers and side-aware code, use the `client-server` skill (`.claude/skills/client-server/SKILL.md`).

## Conventions
- Prefer to reuse existing code as much as possible and use `util/` package functions where appropriate. Consider adding a new util function if there are 3 or more use cases.
- Prefer builder classes where available (for example `ButtonBuilder` for HUD buttons, `UnitItemBuilder` for unit items) over constructing the objects by hand.
- Match the style of the surrounding code (naming, comment density, formatting).
- Strings shown to players go in `lang/en_us.json`
- When adding features/fixing bugs, in order of preference:
1. Isolated non-event changes to individual classes such as in a particular building, ability or unit
2. Forge @SubscribeEvent event in an existing relevant *ClientEvents or *ServerEvents class
3. A new mixin function (mixins can interfere with behaviour of vanilla and other mods)
4. A new mixin class
- Mixins can interfere with behaviour of vanilla and other mods and has the widest scope of impact, so if you use a mixin, say why the earlier tiers weren't enough.

## Logging
- Use `ReignOfNether.LOGGER`, not `System.out`.

## Workflow
- Never commit or push.
- Do not read, reference or change `TODO.txt`, `TODO_backlog.txt` or `TODO_items.txt`. They are for human reference only.
- After changes, compile with `./gradlew compileJava` and report the result. Say clearly if something could not be tested in-game.
- After changes, make suggestions for in-game tests to be run that are not obvious