package com.example.keralatemple.event;

import com.example.keralatemple.KeralaTempleMod;
import com.example.keralatemple.config.Config;
import com.example.keralatemple.item.TempleWandItem;
import com.example.keralatemple.ritual.KeralaClock;
import com.example.keralatemple.ritual.RitualSchedule;
import com.example.keralatemple.temple.Temple;
import com.example.keralatemple.temple.TempleData;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * /keralatemple ...
 *   status
 *   temple   create | delete | list | info | select | center | radius
 *   schedule list | set | remove | reset
 *   balikkal list | remove | clearstones | save | place
 *   wand     mode
 */
@Mod.EventBusSubscriber(modid = KeralaTempleMod.MOD_ID)
public class ModCommands {
    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter HMS = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final Pattern NAME = Pattern.compile("[a-z0-9_-]{1,32}");

    private static final DynamicCommandExceptionType ERROR =
            new DynamicCommandExceptionType(o -> Component.literal(String.valueOf(o)));

    private static final SuggestionProvider<CommandSourceStack> TEMPLE_NAMES = (ctx, builder) ->
            SharedSuggestionProvider.suggest(TempleData.get(ctx.getSource().getLevel()).names(), builder);
    private static final SuggestionProvider<CommandSourceStack> RITUAL_IDS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(RitualSchedule.ids(), builder);
    private static final SuggestionProvider<CommandSourceStack> WAND_MODES = (ctx, builder) ->
            SharedSuggestionProvider.suggest(Arrays.stream(TempleWandItem.Mode.values()).map(m -> m.id).toList(), builder);

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("keralatemple");
        root.then(Commands.literal("status").executes(ModCommands::status));
        root.then(templeNode());
        root.then(scheduleNode());
        root.then(balikkalNode());
        root.then(wandNode());
        event.getDispatcher().register(root);
    }

    // ------------------------------------------------------------------ small helpers

    private static boolean canEdit(CommandSourceStack source) {
        return source.hasPermission(Config.COMMAND_PERMISSION_LEVEL.get());
    }

    private static void say(CommandContext<CommandSourceStack> ctx, String message) {
        ctx.getSource().sendSuccess(() -> Component.literal(message), false);
    }

    private static String nameArg(CommandContext<CommandSourceStack> ctx, String arg) throws CommandSyntaxException {
        String name = StringArgumentType.getString(ctx, arg).toLowerCase(Locale.ROOT);
        if (!NAME.matcher(name).matches()) {
            throw ERROR.create("Names may use a-z, 0-9, _ and - (up to 32 characters).");
        }
        return name;
    }

    private static Temple temple(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String name = StringArgumentType.getString(ctx, "temple").toLowerCase(Locale.ROOT);
        Temple t = TempleData.get(ctx.getSource().getLevel()).get(name);
        if (t == null) {
            throw ERROR.create("No temple named '" + name + "'. Try /keralatemple temple list");
        }
        return t;
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> templeArg() {
        return Commands.argument("temple", StringArgumentType.word()).suggests(TEMPLE_NAMES);
    }

    // ------------------------------------------------------------------ status

    private static int status(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        BlockPos here = BlockPos.containing(ctx.getSource().getPosition());
        Temple temple = TempleData.get(level).findFor(here);
        List<RitualSchedule.Ritual> schedule = RitualSchedule.forTemple(temple);

        ZonedDateTime now = KeralaClock.now();
        RitualSchedule.Ritual current = RitualSchedule.current(schedule, now.toLocalTime());
        RitualSchedule.Ritual next = RitualSchedule.next(schedule, now.toLocalTime());

        say(ctx, "Kerala time: " + now.format(HMS) + " (" + KeralaClock.zone() + ")");
        say(ctx, "Temple: " + (temple == null ? "none here (using the default schedule)" : temple.name));
        say(ctx, "Now: " + (current == null ? "no ritual in progress" : current.displayName()));
        say(ctx, "Next: " + (next == null ? "none scheduled" : next.displayName() + " at " + next.start().format(HM)));
        return 1;
    }

    // ------------------------------------------------------------------ temple

    private static LiteralArgumentBuilder<CommandSourceStack> templeNode() {
        return Commands.literal("temple")
                .then(Commands.literal("create").requires(ModCommands::canEdit)
                        .then(Commands.argument("name", StringArgumentType.word()).executes(ModCommands::templeCreate)))
                .then(Commands.literal("delete").requires(ModCommands::canEdit)
                        .then(templeArg().executes(ModCommands::templeDelete)))
                .then(Commands.literal("list").executes(ModCommands::templeList))
                .then(Commands.literal("info").then(templeArg().executes(ModCommands::templeInfo)))
                .then(Commands.literal("select").requires(ModCommands::canEdit)
                        .then(templeArg().executes(ModCommands::templeSelect)))
                .then(Commands.literal("center").requires(ModCommands::canEdit)
                        .then(templeArg().executes(ModCommands::templeCenter)))
                .then(Commands.literal("radius").requires(ModCommands::canEdit)
                        .then(templeArg().then(Commands.argument("blocks", IntegerArgumentType.integer(4, 128))
                                .executes(ModCommands::templeRadius))));
    }

    private static int templeCreate(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String name = nameArg(ctx, "name");
        ServerLevel level = ctx.getSource().getLevel();
        TempleData data = TempleData.get(level);
        if (data.get(name) != null) {
            throw ERROR.create("A temple named '" + name + "' already exists.");
        }
        BlockPos ground = BlockPos.containing(ctx.getSource().getPosition()).below();
        data.create(name, ground);
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player != null) {
            TempleWandItem.selectOnHeldWand(player, name);
        }
        say(ctx, "Created temple '" + name + "' at " + ground.toShortString() + ". Next: place a Pooja Station, "
                + "bali stones, and spawn a Melshanti and Keezhshanthi nearby.");
        return 1;
    }

    private static int templeDelete(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Temple t = temple(ctx);
        TempleData.get(ctx.getSource().getLevel()).remove(t.name);
        say(ctx, "Deleted temple '" + t.name + "'. (The blocks you built are not touched.)");
        return 1;
    }

    private static int templeList(CommandContext<CommandSourceStack> ctx) {
        TempleData data = TempleData.get(ctx.getSource().getLevel());
        if (data.all().isEmpty()) {
            say(ctx, "No temples yet. Use a Temple Wand or /keralatemple temple create <name>.");
            return 0;
        }
        for (Temple t : data.all()) {
            say(ctx, t.name + " at " + t.center.toShortString() + ", radius " + t.radius + ", "
                    + t.baliStones.size() + " bali stone(s), " + t.valiya.size() + " valiya balikkal");
        }
        return data.all().size();
    }

    private static int templeInfo(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Temple t = temple(ctx);
        say(ctx, "Temple '" + t.name + "'");
        say(ctx, "  Centre: " + t.center.toShortString() + "   Radius: " + t.radius);
        say(ctx, "  Schedule: " + (t.schedule == null ? "default (config)" : "custom (" + t.schedule.size() + " entries)"));
        say(ctx, "  Bali stones: " + t.baliStones.size());
        for (Temple.Valiya v : t.valiya) {
            say(ctx, "  Valiya balikkal '" + v.name() + "': " + v.min().toShortString() + " to " + v.max().toShortString());
        }
        return 1;
    }

    private static int templeSelect(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Temple t = temple(ctx);
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        if (TempleWandItem.findWand(player) == null) {
            throw ERROR.create("Hold a Temple Wand first.");
        }
        TempleWandItem.selectOnHeldWand(player, t.name);
        say(ctx, "Temple Wand now works on '" + t.name + "'.");
        return 1;
    }

    private static int templeCenter(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Temple t = temple(ctx);
        t.center = BlockPos.containing(ctx.getSource().getPosition()).below();
        TempleData.get(ctx.getSource().getLevel()).touch();
        say(ctx, "Centre of '" + t.name + "' moved to " + t.center.toShortString() + ".");
        return 1;
    }

    private static int templeRadius(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Temple t = temple(ctx);
        t.radius = IntegerArgumentType.getInteger(ctx, "blocks");
        TempleData.get(ctx.getSource().getLevel()).touch();
        say(ctx, "Radius of '" + t.name + "' is now " + t.radius + " blocks.");
        return 1;
    }

    // ------------------------------------------------------------------ schedule

    private static LiteralArgumentBuilder<CommandSourceStack> scheduleNode() {
        return Commands.literal("schedule")
                .then(Commands.literal("list")
                        .executes(ctx -> scheduleList(ctx, null))
                        .then(templeArg().executes(ctx -> scheduleList(ctx, temple(ctx)))))
                .then(Commands.literal("set").requires(ModCommands::canEdit)
                        .then(templeArg().then(Commands.argument("ritual", StringArgumentType.word()).suggests(RITUAL_IDS)
                                .then(Commands.argument("hour", IntegerArgumentType.integer(0, 23))
                                        .then(Commands.argument("minute", IntegerArgumentType.integer(0, 59))
                                                .then(Commands.argument("duration", IntegerArgumentType.integer(1, 1440))
                                                        .executes(ModCommands::scheduleSet)))))))
                .then(Commands.literal("remove").requires(ModCommands::canEdit)
                        .then(templeArg().then(Commands.argument("ritual", StringArgumentType.word()).suggests(RITUAL_IDS)
                                .executes(ModCommands::scheduleRemove))))
                .then(Commands.literal("reset").requires(ModCommands::canEdit)
                        .then(templeArg().executes(ModCommands::scheduleReset)));
    }

    private static int scheduleList(CommandContext<CommandSourceStack> ctx, Temple temple) {
        say(ctx, (temple == null ? "Default schedule" : "Schedule of '" + temple.name + "'")
                + " (" + KeralaClock.zone() + "):");
        List<RitualSchedule.Ritual> list = RitualSchedule.forTemple(temple);
        for (RitualSchedule.Ritual r : list) {
            say(ctx, r.start().format(HM) + "  " + r.displayName() + " (" + r.minutes() + " min)  [" + r.id() + "]");
        }
        return list.size();
    }

    private static String ritualArg(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String id = StringArgumentType.getString(ctx, "ritual").toUpperCase(Locale.ROOT);
        if (!RitualSchedule.isKnown(id)) {
            throw ERROR.create("Unknown ritual '" + id + "'. Press Tab after the temple name to see the choices.");
        }
        return id;
    }

    private static int scheduleSet(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Temple t = temple(ctx);
        String id = ritualArg(ctx);
        int hour = IntegerArgumentType.getInteger(ctx, "hour");
        int minute = IntegerArgumentType.getInteger(ctx, "minute");
        int duration = IntegerArgumentType.getInteger(ctx, "duration");

        List<String> entries = RitualSchedule.stringsFor(t);
        entries.removeIf(e -> e.toUpperCase(Locale.ROOT).startsWith(id + "="));
        entries.add(RitualSchedule.format(id, hour, minute, duration));
        t.schedule = entries;
        TempleData.get(ctx.getSource().getLevel()).touch();
        say(ctx, "'" + t.name + "': " + id + " now starts at " + String.format("%02d:%02d", hour, minute)
                + " for " + duration + " minutes.");
        return 1;
    }

    private static int scheduleRemove(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Temple t = temple(ctx);
        String id = ritualArg(ctx);
        List<String> entries = RitualSchedule.stringsFor(t);
        boolean removed = entries.removeIf(e -> e.toUpperCase(Locale.ROOT).startsWith(id + "="));
        if (!removed) {
            throw ERROR.create(id + " is not in the schedule of '" + t.name + "'.");
        }
        t.schedule = entries;
        TempleData.get(ctx.getSource().getLevel()).touch();
        say(ctx, "Removed " + id + " from '" + t.name + "'.");
        return 1;
    }

    private static int scheduleReset(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Temple t = temple(ctx);
        t.schedule = null;
        TempleData.get(ctx.getSource().getLevel()).touch();
        say(ctx, "'" + t.name + "' now uses the default schedule again.");
        return 1;
    }

    // ------------------------------------------------------------------ balikkal

    private static LiteralArgumentBuilder<CommandSourceStack> balikkalNode() {
        return Commands.literal("balikkal")
                .then(Commands.literal("list").then(templeArg().executes(ModCommands::balikkalList)))
                .then(Commands.literal("remove").requires(ModCommands::canEdit)
                        .then(templeArg().then(Commands.argument("name", StringArgumentType.word())
                                .executes(ModCommands::balikkalRemove))))
                .then(Commands.literal("clearstones").requires(ModCommands::canEdit)
                        .then(templeArg().executes(ModCommands::balikkalClearStones)))
                .then(Commands.literal("save").requires(ModCommands::canEdit)
                        .then(Commands.argument("name", StringArgumentType.word()).executes(ModCommands::balikkalSave)))
                .then(Commands.literal("place").requires(ModCommands::canEdit)
                        .then(Commands.argument("name", StringArgumentType.word()).executes(ModCommands::balikkalPlace)));
    }

    private static int balikkalList(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Temple t = temple(ctx);
        say(ctx, "'" + t.name + "': " + t.baliStones.size() + " bali stone(s), " + t.valiya.size() + " valiya balikkal");
        for (Temple.Valiya v : t.valiya) {
            say(ctx, "  " + v.name() + ": " + v.min().toShortString() + " to " + v.max().toShortString());
        }
        return t.valiya.size();
    }

    private static int balikkalRemove(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Temple t = temple(ctx);
        String name = nameArg(ctx, "name");
        boolean removed = t.valiya.removeIf(v -> v.name().equals(name));
        if (!removed) {
            throw ERROR.create("'" + t.name + "' has no valiya balikkal named '" + name + "'.");
        }
        TempleData.get(ctx.getSource().getLevel()).touch();
        say(ctx, "Removed '" + name + "' from '" + t.name + "'. (The blocks are not touched.)");
        return 1;
    }

    private static int balikkalClearStones(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Temple t = temple(ctx);
        int n = t.baliStones.size();
        t.baliStones.clear();
        TempleData.get(ctx.getSource().getLevel()).touch();
        say(ctx, "Cleared " + n + " bali point(s) from '" + t.name + "'.");
        return n;
    }

    /** Stores the two corners last selected with the wand (Valiya balikkal mode) as a reusable template. */
    private static int balikkalSave(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String name = nameArg(ctx, "name");
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack wand = TempleWandItem.findWand(player);
        if (wand == null) {
            throw ERROR.create("Hold a Temple Wand that has a region selected (Valiya balikkal mode, two clicks).");
        }
        CompoundTag tag = wand.getOrCreateTag();
        if (!tag.contains("SelA") || !tag.contains("SelB")) {
            throw ERROR.create("No region selected. Use the wand in Valiya balikkal mode: click two opposite corners.");
        }
        BlockPos a = BlockPos.of(tag.getLong("SelA"));
        BlockPos b = BlockPos.of(tag.getLong("SelB"));
        BlockPos min = new BlockPos(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()));
        Vec3i size = new Vec3i(Math.abs(a.getX() - b.getX()) + 1, Math.abs(a.getY() - b.getY()) + 1, Math.abs(a.getZ() - b.getZ()) + 1);
        if (size.getX() > 48 || size.getY() > 48 || size.getZ() > 48) {
            throw ERROR.create("That region is too big (max 48 blocks in each direction).");
        }

        ServerLevel level = ctx.getSource().getLevel();
        StructureTemplateManager manager = level.getStructureManager();
        ResourceLocation id = new ResourceLocation(KeralaTempleMod.MOD_ID, "balikkal/" + name);
        StructureTemplate template = manager.getOrCreate(id);
        template.fillFromWorld(level, min, size, false, Blocks.STRUCTURE_VOID);
        manager.save(id);
        say(ctx, "Saved '" + name + "' (" + size.getX() + "x" + size.getY() + "x" + size.getZ()
                + "). Use /keralatemple balikkal place " + name + " to build another copy where you stand.");
        return 1;
    }

    private static int balikkalPlace(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String name = nameArg(ctx, "name");
        ServerLevel level = ctx.getSource().getLevel();
        ResourceLocation id = new ResourceLocation(KeralaTempleMod.MOD_ID, "balikkal/" + name);
        Optional<StructureTemplate> template = level.getStructureManager().get(id);
        if (template.isEmpty()) {
            throw ERROR.create("No saved balikkal named '" + name + "'.");
        }
        BlockPos pos = BlockPos.containing(ctx.getSource().getPosition());
        template.get().placeInWorld(level, pos, pos, new StructurePlaceSettings(), level.getRandom(), 2);
        say(ctx, "Placed '" + name + "' with its corner at " + pos.toShortString()
                + ". Register it with the Temple Wand (Valiya balikkal mode) so the priest visits it.");
        return 1;
    }

    // ------------------------------------------------------------------ wand

    private static LiteralArgumentBuilder<CommandSourceStack> wandNode() {
        return Commands.literal("wand").requires(ModCommands::canEdit)
                .then(Commands.literal("mode")
                        .then(Commands.argument("mode", StringArgumentType.word()).suggests(WAND_MODES)
                                .executes(ModCommands::wandMode)));
    }

    private static int wandMode(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack wand = TempleWandItem.findWand(player);
        if (wand == null) {
            throw ERROR.create("Hold a Temple Wand first.");
        }
        String id = StringArgumentType.getString(ctx, "mode").toLowerCase(Locale.ROOT);
        TempleWandItem.Mode mode = Arrays.stream(TempleWandItem.Mode.values()).filter(m -> m.id.equals(id)).findFirst().orElse(null);
        if (mode == null) {
            throw ERROR.create("Modes: temple_center, bali_stone, valiya_balikkal");
        }
        TempleWandItem.setMode(wand, mode);
        say(ctx, "Temple Wand mode: " + mode.label);
        return 1;
    }
}
