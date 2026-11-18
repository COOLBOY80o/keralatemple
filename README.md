# Kerala Temple Architecture (Forge 1.20.1)

A mod for building Kerala-style temples and running their daily rituals.

**Status:** Phases 1 and 2a are written. **Not built yet:** abhishekam dravyas, kalasam and kalasha pooja, and the priest dressing the idols.

> This source has not been compiled or run by its author (no Forge download was possible while writing it).
> Expect to fix a few compile errors on your first `./gradlew build`.

## Build and run

You need **JDK 17** (Temurin from adoptium.net) and internet on the first build. The Gradle wrapper is included.

- **Windows:** double-click `BUILD-WINDOWS.bat`
- **Mac / Linux:** run `./build-mac-linux.sh`

The jar appears in `build/libs/keralatemple-1.0.0.jar`. Put it in the `mods` folder of a **Minecraft 1.20.1 Forge** install (Forge 47.3.0 or newer).
To test straight from the project instead, run `./gradlew runClient`.

**Permissions:** the Temple Wand and all editing commands need permission level 2 (a world with cheats on, or an op).
To let everyone use them, set `commandPermissionLevel=0` in `config/keralatemple-common.toml`.

## Blocks and items

| | |
|---|---|
| Laterite Bricks, Temple Granite, Copper Roof Tiles (+ stairs, slabs), Carved Wood Pillar and Panel, Nilavilakku | Building blocks |
| Pooja Station (Sreekovil Mat) | Where the priest performs poojas |
| Seeveli Waypoint | Optional extra stop on the seeveli route |
| **Bali Stone** | A small balikkal |
| **Idol Pedestal** | Holds a cast idol |
| **Temple Wand** | Marks temples, bali stones and valiya balikkals |
| Melshanti and Keezhshanthi spawn eggs | The two priests |

## Time sync

The Overworld clock follows real Kerala time (`Asia/Kolkata`): 06:00 sunrise, 12:00 noon, 18:00 sunset, 00:00 midnight.
Time is locked to the real clock, so sleeping will not skip the night. Turn it off with `syncTimeToKerala=false` in the config and `/gamerule doDaylightCycle true`.

## Setting up a temple (the Temple Wand)

Sneak + right-click changes the wand's mode. Right-click a block to use the mode.

1. **Temple centre mode.** Click a block at the middle of your temple. This creates a temple (`temple_1`, ...) and selects it on the wand. Click again to move the centre. (Or `/keralatemple temple create <name>`.)
2. **Bali stone mode.** Place Bali Stones around the temple and click each one. Click again to remove it. Any block can be a bali point.
3. **Valiya balikkal mode.** Build your own valiya balikkal out of any blocks, then click two opposite corners of it. It is registered as `valiya_1`, `valiya_2`, ...
4. Place a **Pooja Station** where the priest should stand (without one, he uses the temple centre) and spawn the **Melshanti** and **Keezhshanthi** within the temple's radius (default 24 blocks; `/keralatemple temple radius <name> <blocks>`).

### Designing a custom valiya balikkal

Build it however you like. Then, in Valiya balikkal mode, click two opposite corners. To keep the design:

    /keralatemple balikkal save <name>      saves the region you last selected (up to 48x48x48)
    /keralatemple balikkal place <name>     builds another copy at your feet

Saved designs live in the world's `generated/keralatemple/structures/balikkal/` folder.
Register each copy with the wand so the priests visit it.

## The two priests

- **Melshanti:** performs each pooja at the station, with bells, incense smoke and gestures, and walks the seeveli.
- **Keezhshanthi:** waits beside the sanctum during poojas, then follows the seeveli a few seconds behind and scatters rice at each bali stone and valiya balikkal (the offerings are stylised).

During **seeveli** they make three clockwise circuits through every bali stone, valiya balikkal and Seeveli Waypoint of the temple.

## Customising the schedule in game

Each temple has its own schedule. Until you change it, a temple uses the default from the config file.

    /keralatemple schedule list [<temple>]
    /keralatemple schedule set <temple> <RITUAL> <hour> <minute> <duration-minutes>
    /keralatemple schedule remove <temple> <RITUAL>
    /keralatemple schedule reset <temple>          back to the default

`<RITUAL>` tab-completes: NADA_THURAKKAL, NIRMALYA_DARSHANAM, ABHISHEKAM, USHAH_POOJA, ETHIRTHA_POOJA, SEEVELI_MORNING,
PANTHEERADI_POOJA, UCHA_POOJA, NADA_ADAKKAL, EVENING_NADA_THURAKKAL, DEEPARADHANA, ATHAZHA_POOJA, SEEVELI_NIGHT,
NADA_ADAKKAL_NIGHT, plus spare slots EXTRA_POOJA_1..3 and EXTRA_SEEVELI. Each ritual can appear once per day.
Example: `/keralatemple schedule set temple_1 USHAH_POOJA 6 45 40`

Other commands: `/keralatemple status`, `temple list|info|select|delete|center|radius`, `balikkal list|remove|clearstones`, `wand mode <mode>`.

**The default timings are typical, not any one temple's real schedule.** Kerala temples keep their own timings and customs, and the in-game rites are a simple stylised version. Please check them with a thantri or melshanti before treating them as accurate.

## Idols: Chisel, shilas and the Achari

**Stone idols.** Place an **Anjana Shila** or **Krishna Shila** (craft from 4 polished blackstone / 4 polished deepslate), then **left-click it with the Chisel** (iron ingot over a stick). A designer window opens:
- **Design** tab: draw the idol's front view (pen, erase, fill, mirror, 24 colours).
- **Import** tab: drag a PNG or JPG from your computer onto the window; the game removes a plain background and shrinks it.
- Set the **height** (0.5 to 6 blocks) and **thickness**, pick the **peetham** (none, simple, lotus, tiered) and whether to keep the picture's own colours or carve it in the stone.
- Press **Carve idol**. The idol appears standing on the stone.

**Metal idols.** Spawn an **Achari** (spawn egg) and **left-click him**. Same window, plus a metal (panchaloha, bronze, silver, gold). He casts it, which takes a while (more for taller idols; `achariCastSeconds` in the config), then hands you a Cast Idol. Right-click an **Idol Pedestal** with it to set it up; sneak with an empty hand to take it back.

**Jewellery and angi.** Talk to the Achari and choose:
- **Make jewellery:** a *kireedam* (crown), *mala* (necklace), *vala* (armlets) or *odyanam* (waist belt) in gold or silver, with an emerald, ruby or diamond in each of its five settings. Right-click an idol with the finished piece to dress it (a new piece replaces the old one of the same kind). Sneak with an empty hand to take things off, one at a time.
- **Cast an angi:** right-click an idol with your Chisel to measure it, hold that chisel and order an angi in the metal you want. Right-click the idol with the angi to fit it; it covers the idol like a metal thiruvabharanam. Sneak with an empty hand to remove it.
- He asks for materials (turn this off with `achariNeedsMaterials` in the config; creative players pay nothing). Gold = gold ingots, silver = iron ingots, bronze and panchaloha = copper ingots; emerald, diamond, and redstone for rubies (Minecraft has no silver or ruby).

**What this really is.** A picture cannot hold the true shape of a statue. The mod makes a rounded relief from the outline (like inflating a sculpture), colours the front, and gives it a darker back and solid sides, up to 64 cells on the long side. It is not a full 3D sculpture, and there is no 3D preview inside the window.
- Best results: a front-facing picture on a plain background, or a PNG with a transparent background.
- Breaking a shila loses the idol carved on it. Breaking a pedestal gives the idol back.
- The Achari does not charge materials yet.

Only use pictures you have the right to use, and treat real murtis with respect: the in-game idol is a decorative model, not a consecrated one.

## Programming the rituals (round 1)

Rituals are programmed per temple, on the real Kerala 24-hour clock. **Nothing is announced in chat or on screen** (`announceRitualsOnScreen` in the config is off by default).

- `/keralatemple schedule list <temple>` shows the day. `schedule set <temple> <ritual> <hour> <minute> <minutes>` sets a ritual to start at a clock time and last that long (press Tab after the temple name to see the rituals).
- **Sunset:** `schedule sunset <temple> DEEPARADHANA 0 20` makes the ritual follow the (approximate) Kerala sunset every day; the offset is in minutes (for example -10 starts ten minutes before sunset). Deeparadhana is at sunset in the default schedule.
- Rituals: nada thurakkal, **nirmalya darshanam**, **vakacharthu**, **shankha abhishekam**, **jaladhara**, **ksheeradhara**, **ayirakudam**, abhishekam, ushah pooja, ethirtha pooja, pantheeradi pooja, ucha pooja, athazha pooja, deeparadhana, seeveli, nada adakkal, plus spare slots. The melshanthi shows each one differently (sandal paste, shankh, falling water, milk, lamps).
- **Ayirakudam:** place a **Kudam Station** (blue mat) beside water, and one or more Keezhshanthis. They fetch pots and carry them to the melshanthi again and again. `/keralatemple status` shows the count out of 1000. Set a long duration for it; more keezhshanthis finish faster. Without a station they fill pots slowly where they stand.
- **Temple doors** (wood, gold-plated, gold): you can open them by hand, and the melshanthi opens every door of the temple at nada thurakkal and closes them at nada adakkal.
- The default schedule changed, so existing worlds use a new config entry (`ritualScheduleV2`). Temples with their own schedule keep it.

## Layout

    src/main/java/com/example/keralatemple/
        KeralaTempleMod.java
        config/Config.java
        temple/Temple.java, TempleData.java              temples saved with the world
        ritual/KeralaClock.java, RitualSchedule.java
        entity/PriestEntity.java, KeezhshanthiEntity.java, RitualGoal.java
        item/TempleWandItem.java
        block/NilavilakkuBlock.java, MarkerBlock.java, BaliStoneBlock.java, IdolBlock.java
        blockentity/IdolBlockEntity.java
        event/TimeSyncHandler.java, ModCommands.java, ModCommonEvents.java
        client/PriestRenderer.java, IdolRenderer.java, IdolCache.java, IdolMeshBuilder.java, ClientEvents.java
        registry/ModBlocks.java, ModItems.java, ModEntities.java, ModBlockEntities.java, ModCreativeTabs.java

## Still to come

- Abhishekam dravyas (milk, ghee, honey, curd, rose water, tender coconut water, sandal paste, panchagavyam, ...) and the abhishekam ritual
- Kalasam and kalasha pooja
- The priest dressing the idols (vastram, garlands, chandanam, ornaments)
