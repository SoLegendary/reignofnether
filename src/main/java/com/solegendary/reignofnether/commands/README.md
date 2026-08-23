# Command Update for Reign of Nether (23 August 2026)

## 1. New /rtsapi commands:

:warning: **All commands beginning with /rtsapi- (with a dash) exist for backwards compability, but are no longer updated, use /rtsapi (no dash) instead**

All commands are organised into three categories: **Building**, **Unit**, and **Player**.

Custom command support has been added for normal buildings using `/rtsapi building command`. It is similar to custom building commands, however, it takes effect on each individual building, rather than on each type of building.

<details><summary>All Commands</summary>
<p>

- /rtsapi building place &lt;buildingName&gt; [ownerName] [pos] [autoBuild] [rotation]
- /rtsapi building destroy &lt;pos&gt;
- /rtsapi building destroy &lt;targets&gt; [ownerName] [preserved]
- /rtsapi building owner &lt;from&gt; &lt;to&gt; [ownerName] [newOwnerName]
- /rtsapi building owner &lt;targets&gt; [ownerName] [newOwnerName]
- /rtsapi building tag &lt;targets&gt; &lt;ownerName&gt; add &lt;name&gt;
- /rtsapi building tag &lt;targets&gt; &lt;ownerName&gt; remove &lt;name&gt;
- /rtsapi building tag &lt;targets&gt; &lt;ownerName&gt; list
- /rtsapi building hurt &lt;targets&gt; &lt;ownerName&gt; &lt;points&gt;
- /rtsapi building heal &lt;targets&gt; &lt;ownerName&gt; &lt;points&gt;
- /rtsapi building command &lt;targets&gt; &lt;ownerName&gt; add &lt;commandStr&gt; &lt;condition&gt; [tickCooldownMax] [tickCooldown]
- /rtsapi building command &lt;targets&gt; &lt;ownerName&gt; remove &lt;index&gt;
- /rtsapi building command &lt;targets&gt; &lt;ownerName&gt; remove *
- /rtsapi building command &lt;targets&gt; &lt;ownerName&gt; set &lt;index&gt; command &lt;value&gt;
- /rtsapi building command &lt;targets&gt; &lt;ownerName&gt; set &lt;index&gt; cooldown &lt;value&gt;
- /rtsapi building command &lt;targets&gt; &lt;ownerName&gt; set &lt;index&gt; trigger &lt;value&gt;
- /rtsapi building command &lt;targets&gt; &lt;ownerName&gt; list
- /rtsapi player owner entity &lt;targets&gt; [players] [newOwnerName]
- /rtsapi player owner building &lt;targets&gt; [players] [newOwnerName]
- /rtsapi player ally set &lt;player1&gt; &lt;player2&gt;
- /rtsapi player ally cancel &lt;player1&gt; &lt;player2&gt;
- /rtsapi player resources add &lt;resource&gt; &lt;points&gt; &lt;player&gt;
- /rtsapi player resources remove &lt;resource&gt; &lt;points&gt; &lt;player&gt;
- /rtsapi player resources set &lt;resource&gt; &lt;points&gt; &lt;player&gt;
- /rtsapi player resources get &lt;player&gt;
- /rtsapi player victory &lt;player&gt; [reason]
- /rtsapi player defeat &lt;player&gt; [reason]
- /rtsapi player research add &lt;researchItem&gt; &lt;player&gt;
- /rtsapi player research remove &lt;researchItem&gt; &lt;player&gt;
- /rtsapi player research get &lt;player&gt;
- /rtsapi player teammode &lt;mode&gt;
- /rtsapi player starting-teams-mode &lt;mode&gt;
- /rtsapi player camera &lt;value&gt; &lt;player&gt;
- /rtsapi player camera move &lt;pos&gt; &lt;player&gt; &lt;pos&gt; [lockTicks] [forcePanTicks] [zoom]
- /rtsapi player camera fade &lt;pos&gt; &lt;player&gt; &lt;pos&gt; [fadeTicks] [blackoutTicks]
- /rtsapi unit summon &lt;entity&gt; [ownerName] [pos] [nbt]
- /rtsapi unit owner &lt;from&gt; &lt;to&gt; [ownerName] [newOwnerName]
- /rtsapi unit owner &lt;targets&gt; [ownerName] [newOwnerName]
- /rtsapi unit anchor set &lt;from&gt; &lt;to&gt; &lt;anchor&gt;
- /rtsapi unit anchor set &lt;targets&gt; [ownerName] &lt;anchor&gt;
- /rtsapi unit anchor remove &lt;from&gt; &lt;to&gt;
- /rtsapi unit anchor remove &lt;targets&gt; [ownerName]
- /rtsapi unit action &lt;selectFrom&gt; &lt;selectTo&gt; &lt;ownerName&gt; &lt;action&gt; [targetPos]
- /rtsapi unit action &lt;selectFrom&gt; &lt;selectTo&gt; &lt;ownerName&gt; &lt;action&gt; &lt;targetFrom&gt; &lt;targetTo&gt;
- /rtsapi unit action &lt;targets&gt; &lt;ownerName&gt; &lt;action&gt; [targetPos]
- /rtsapi unit action &lt;targets&gt; &lt;ownerName&gt; &lt;action&gt; &lt;target&gt;
- /rtsapi unit enemysearch &lt;selectFrom&gt; &lt;selectTo&gt; &lt;ownerName&gt; &lt;behaviour&gt;
- /rtsapi unit enemysearch &lt;targets&gt; &lt;ownerName&gt; &lt;behaviour&gt;

</p>
</details> 

---
## 2. FPV-friendly Ally commands
Sending an ally request to an FPV player now shows clickable buttons in their chat window:\
`[Click to Confirm]` | `[Click to Cancel]`

---
## 3. New command arguments
To support the new `/rtsapi` commands, several new argument types have been added:

<details><summary>BuildingArgument</summary>
<p>

Similar to the vanilla entity argument, four type of building arguments have been added:
- `@b -- All buildings`
- `@p -- Nearest building`
- `@r -- Random building`
- `@s -- The building at your position.`
They also support options such as `"type" | "rotation" | "limit"`
</p>
</details> 

<details><summary>UnitArgument</summary>
<p>
The main difference from the EntityArgument is that it can select units from the entities. they are identical to the previous `/rts-*` commands.
</p>
</details> 

<details><summary>PlayerNameArgument</summary>
<p>

This combines the EntityArgument and the StringTypeArgument. You can use `@a` as an EntitySelector, or `"a"` as a fake player name. Four use cases:
- `*` (optional) — all players (including fake players like "Enemy")
- `@a` / `@p` / `@r` / ... — EntitySelector (does not apply to offline or virtual players like scenario NPCs)
- `a` / `b` / `c` / ... — an online player's name
- `"a"` / `"b"` / `"c"` / ... — player name as a string (`""` is valid as neutral)

The result is a string representing the player's name. It is usually appended to the BuildingArgument and UnitArgument as the `ownerName` selector.

</p>
</details> 

---
## 4. /data for buildings
You can now use `/data get/merge/modify/remove building` on building data:

<details><summary>Valid NBTs</summary>
<p>

- `isBuilt`: boolean
- `baseMsPerBuild`: int
- `minBlocksPercent`: float
- `ownerName`: string
- `scenarioRoleIndex`: int
- `ticksToExtinguishMax`: long
- `ticksToSpawnAnimalsMax`: long
- `maxAnimals`: int
- `animalSpawnBlockRange`: int
- `animalSpawnRangeMin`: int
- `health`: int
- `maxHealth`: int
- `Tags`: list of strings (may be absent)
- `Commands[].tickCooldown`: int
- `Commands[].tickCooldownMax`: int
- `Commands[].commandStr`: string
- `Commands[].condition`: string
- `Commands[].index`: int
- `cooldowns.<skill_name>`: float
- `charges.<skill_name>`: int
</p>
</details> 

---
## 5.New Scoreboard Criteria
You can now use RTS resources (food, wood, ore, population) in scoreboards, eg:
- `/scoreboard objectives add myfood resources.food "food"`

---
## 6.New Execute Commands
You can now select buildings using `/execute buildings ...` to apply commands that normally only accept a single building (like `/data`). This will provide their centre positions. Similarly, you can select units using `/execute units`.

A new child command category called `rts-related` has also been added. As the name suggests, it allows you to get players or entities that have a specific relationship with the former entity, similar to `/execute on`.

<details><summary>Rts-related Child Commands</summary>
<p>

- /execute rts-related owner building -- (needs former building declaration)
- /execute rts-related owner entity -- (needs former building declaration)
- /execute rts-related allies -- (needs former player declaration)
- /execute rts-related enemies
- /execute rts-related attacker entity
- /execute rts-related attacker building
- /execute rts-related target building
- /execute rts-related target entity
- /execute building <targets> <ownerName>
- /execute unit <targets> <ownerName>

</p>
</details> 

---
## 7.Unit Death Commands
All RoN units now have an NBT tag called `onDeathCommand`. You can set it by running:
`/data modify <EntitySelector> onDeathCommand set value "<String:command>"`

For example, `/data modify @e[type=reignofnether:zombie_unit,limit=1] onDeathCommand set value "say I died!"` will cause that zombie to say "I died!" when it dies.
