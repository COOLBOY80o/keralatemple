package com.example.keralatemple.event;

import com.example.keralatemple.KeralaTempleMod;
import com.example.keralatemple.config.Config;
import com.example.keralatemple.item.TempleWandItem;
import com.example.keralatemple.ritual.Flower;
import com.example.keralatemple.ritual.KeralaClock;
import com.example.keralatemple.ritual.Nivedyam;
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
 *   schedule list | set | sunset | remove | reset
 *   flowers  list | add | remove
 *   nivedyam list | set | clear
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
    private static final SuggestionProvider<CommandSourceStack> FLOWER_IDS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(Arrays.stream(Flower.values()).map(f -> f.id).toList(), builder);
    private static final SuggestionProvider<CommandSourceStack> FOOD_IDS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(Arrays.stream(Nivedyam.values()).map(n -> n.id).toList(), builder);
    private static final SuggestionProvider<CommandSourceStack> WAND_MODES = (ctx, builder) ->
            SharedSuggestionProvider.suggest(Arrays.stream(TempleWandItem.Mode.values()).map(m -> m.id).toList(), builder);

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("keralatemple");
        root.then(Commands.literal("status").executes(ModCommands::status));
        root.then(templeNode());
        root.then(scheduleNode());
        root.then(flowersNode());
        root.then(nivedyamNode());
        root.then(balikkalNode());
        root.then(routeNode());
        root.then(vastramNode());
        root.then(brahminNode());
        root.then(carryNode());
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
        if (temple != null && temple.kudamKey.length() > 0) {
            say(ctx, "Ayirakudam: " + temple.kudams + " / 1000 pots");
        }
        if (temple != null) {
            for (var e : temple.nivedyam.entrySet()) {
                Nivedyam n = Nivedyam.byId(e.getValue());
                say(ctx, "Nivedyam: " + e.getKey() + " -> " + (n == null ? e.getValue() : n.label));
            }
            if (!temple.cookMissing.isEmpty()) {
                say(ctx, "The cook is waiting: " + temple.cookMissing);
            }
        }
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
                .then(Commands.literal("sunset").requires(ModCommands::canEdit)
                        .then(templeArg().then(Commands.argument("ritual", StringArgumentType.word()).suggests(RITUAL_IDS)
                                .then(Commands.argument("offset", IntegerArgumentType.integer(-180, 180))
                                        .then(Commands.argument("duration", IntegerArgumentType.integer(1, 1440))
                                                .executes(ModCommands::scheduleSunset))))))
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

    private static int scheduleSunset(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Temple t = temple(ctx);
        String id = ritualArg(ctx);
        int offset = IntegerArgumentType.getInteger(ctx, "offset");
        int duration = IntegerArgumentType.getInteger(ctx, "duration");
        List<String> entries = RitualSchedule.stringsFor(t);
        entries.removeIf(e -> e.toUpperCase(Locale.ROOT).startsWith(id + "="));
        entries.add(RitualSchedule.formatSunset(id, offset, duration));
        t.schedule = entries;
        TempleData.get(ctx.getSource().getLevel()).touch();
        say(ctx, "'" + t.name + "': " + id + " now follows sunset" + (offset == 0 ? "" : (offset > 0 ? " +" : " ") + offset + " min")
                + " for " + duration + " minutes. Sunset today is about "
                + RitualSchedule.sunset(KeralaClock.now().toLocalDate()).format(HM) + ".");
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

    // ------------------------------------------------------------------ flowers and nivedyam

    private static LiteralArgumentBuilder<CommandSourceStack> flowersNode() {
        return Commands.literal("flowers")
                .then(Commands.literal("list").then(templeArg().executes(ModCommands::flowersList)))
                .then(Commands.literal("add").requires(ModCommands::canEdit)
                        .then(templeArg().then(Commands.argument("flower", StringArgumentType.word()).suggests(FLOWER_IDS)
                                .executes(ctx -> flowersChange(ctx, true)))))
                .then(Commands.literal("remove").requires(ModCommands::canEdit)
                        .then(templeArg().then(Commands.argument("flower", StringArgumentType.word()).suggests(FLOWER_IDS)
                                .executes(ctx -> flowersChange(ctx, false)))));
    }

    private static int flowersList(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Temple t = temple(ctx);
        say(ctx, "Flowers used by '" + t.name + "': " + (t.flowers.isEmpty() ? "none" : String.join(", ", t.flowers)));
        say(ctx, "All flowers: " + String.join(", ", Arrays.stream(Flower.values()).map(f -> f.id).toList()));
        return t.flowers.size();
    }

    private static int flowersChange(CommandContext<CommandSourceStack> ctx, boolean add) throws CommandSyntaxException {
        Temple t = temple(ctx);
        String id = StringArgumentType.getString(ctx, "flower").toLowerCase(Locale.ROOT);
        if (Flower.byId(id) == null) {
            throw ERROR.create("Unknown flower '" + id + "'. Press Tab to see the choices.");
        }
        if (add) {
            t.flowers.add(id);
        } else {
            t.flowers.remove(id);
        }
        TempleData.get(ctx.getSource().getLevel()).touch();
        say(ctx, "'" + t.name + "' now uses: " + (t.flowers.isEmpty() ? "no flowers" : String.join(", ", t.flowers)));
        return 1;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> nivedyamNode() {
        return Commands.literal("nivedyam")
                .then(Commands.literal("list").then(templeArg().executes(ModCommands::nivedyamList)))
                .then(Commands.literal("set").requires(ModCommands::canEdit)
                        .then(templeArg().then(Commands.argument("ritual", StringArgumentType.word()).suggests(RITUAL_IDS)
                                .then(Commands.argument("food", StringArgumentType.word()).suggests(FOOD_IDS)
                                        .executes(ModCommands::nivedyamSet)))))
                .then(Commands.literal("clear").requires(ModCommands::canEdit)
                        .then(templeArg().then(Commands.argument("ritual", StringArgumentType.word()).suggests(RITUAL_IDS)
                                .executes(ModCommands::nivedyamClear))));
    }

    private static int nivedyamList(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Temple t = temple(ctx);
        if (t.nivedyam.isEmpty()) {
            say(ctx, "'" + t.name + "' has no nivedyam set. Use /keralatemple nivedyam set <temple> <ritual> <food>.");
        }
        for (var e : t.nivedyam.entrySet()) {
            Nivedyam n = Nivedyam.byId(e.getValue());
            say(ctx, e.getKey() + " -> " + (n == null ? e.getValue() : n.label));
        }
        for (Nivedyam n : Nivedyam.values()) {
            say(ctx, n.id + " needs " + com.example.keralatemple.idol.AchariCosts.describe(n.ingredients()));
        }
        return t.nivedyam.size();
    }

    private static int nivedyamSet(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Temple t = temple(ctx);
        String ritual = ritualArg(ctx);
        String food = StringArgumentType.getString(ctx, "food").toLowerCase(Locale.ROOT);
        Nivedyam n = Nivedyam.byId(food);
        if (n == null) {
            throw ERROR.create("Unknown nivedyam '" + food + "'. Press Tab to see the choices.");
        }
        t.nivedyam.put(ritual, n.id);
        TempleData.get(ctx.getSource().getLevel()).touch();
        say(ctx, "'" + t.name + "': the cook will make " + n.label + " for " + ritual + " about "
                + Config.COOK_MINUTES.get() + " minutes before it starts. Put " + com.example.keralatemple.idol.AchariCosts
                .describe(n.ingredients()) + " in a chest near the uruli.");
        return 1;
    }

    private static int nivedyamClear(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Temple t = temple(ctx);
        String ritual = ritualArg(ctx);
        if (t.nivedyam.remove(ritual) == null) {
            throw ERROR.create(ritual + " has no nivedyam in '" + t.name + "'.");
        }
        TempleData.get(ctx.getSource().getLevel()).touch();
        say(ctx, "Removed the nivedyam of " + ritual + ".");
        return 1;
    }

    // ------------------------------------------------------------------ angavastram and names

    private static final SuggestionProvider<CommandSourceStack> ROLE_IDS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(com.example.keralatemple.ritual.Vastram.ROLES, builder);
    private static final SuggestionProvider<CommandSourceStack> COLOUR_IDS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(com.example.keralatemple.ritual.Vastram.COLOURS, builder);
    private static final SuggestionProvider<CommandSourceStack> DESIGN_IDS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(com.example.keralatemple.ritual.Vastram.DESIGNS, builder);

    /** /keralatemple vastram <temple> <role> <colour> <accent> <design>  |  list <temple>  |  reset <temple> <role> */
    private static LiteralArgumentBuilder<CommandSourceStack> vastramNode() {
        return Commands.literal("vastram")
                .then(Commands.literal("list").then(templeArg().executes(ctx -> {
                    Temple t = temple(ctx);
                    for (String role : com.example.keralatemple.ritual.Vastram.ROLES) {
                        String v = t.vastram.get(role);
                        say(ctx, "  " + role + ": " + (v == null ? "default" : v.replace(",", " / ")));
                    }
                    return 1;
                })))
                .then(Commands.literal("reset").requires(ModCommands::canEdit)
                        .then(templeArg().then(Commands.argument("role", StringArgumentType.word()).suggests(ROLE_IDS)
                                .executes(ctx -> {
                                    Temple t = temple(ctx);
                                    t.vastram.remove(nameArg(ctx, "role"));
                                    TempleData.get(ctx.getSource().getLevel()).touch();
                                    say(ctx, "Back to the default angavastram.");
                                    return 1;
                                }))))
                .then(Commands.literal("set").requires(ModCommands::canEdit)
                        .then(templeArg().then(Commands.argument("role", StringArgumentType.word()).suggests(ROLE_IDS)
                                .then(Commands.argument("colour", StringArgumentType.word()).suggests(COLOUR_IDS)
                                        .then(Commands.argument("accent", StringArgumentType.word()).suggests(COLOUR_IDS)
                                                .then(Commands.argument("design", StringArgumentType.word()).suggests(DESIGN_IDS)
                                                        .executes(ModCommands::vastramSet)))))));
    }

    private static int vastramSet(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Temple t = temple(ctx);
        String role = nameArg(ctx, "role");
        String spec = com.example.keralatemple.ritual.Vastram.spec(nameArg(ctx, "colour"), nameArg(ctx, "accent"),
                nameArg(ctx, "design"));
        if (!com.example.keralatemple.ritual.Vastram.ROLES.contains(role)
                || com.example.keralatemple.ritual.Vastram.parse(spec) == 0) {
            throw ERROR.create("Roles: " + com.example.keralatemple.ritual.Vastram.ROLES + ". Colours: "
                    + com.example.keralatemple.ritual.Vastram.COLOURS + ". Designs: "
                    + com.example.keralatemple.ritual.Vastram.DESIGNS);
        }
        t.vastram.put(role, spec);
        TempleData.get(ctx.getSource().getLevel()).touch();
        say(ctx, "The " + role + " of '" + t.name + "' now wears " + spec.replace(",", " / ") + ". It shows within a few seconds.");
        return 1;
    }

    /** /keralatemple brahmin name <text>: names the nearest priest, helper or cook within 6 blocks of you. */
    private static LiteralArgumentBuilder<CommandSourceStack> brahminNode() {
        return Commands.literal("brahmin").requires(ModCommands::canEdit)
                .then(Commands.literal("name").then(Commands.argument("text", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            com.example.keralatemple.entity.PriestEntity nearest = null;
                            double best = 36.0D;
                            for (com.example.keralatemple.entity.PriestEntity p : player.level().getEntitiesOfClass(
                                    com.example.keralatemple.entity.PriestEntity.class, player.getBoundingBox().inflate(6.0D))) {
                                double d = p.distanceToSqr(player);
                                if (d < best) {
                                    best = d;
                                    nearest = p;
                                }
                            }
                            if (nearest == null) {
                                throw ERROR.create("Stand within 6 blocks of the brahmin you want to name.");
                            }
                            String text = StringArgumentType.getString(ctx, "text");
                            nearest.setCustomName(net.minecraft.network.chat.Component.literal(text));
                            nearest.setCustomNameVisible(true);
                            say(ctx, "He is now called " + text + ".");
                            return 1;
                        })));
    }

    /** /keralatemple carry <temple> arms|head|none */
    private static LiteralArgumentBuilder<CommandSourceStack> carryNode() {
        SuggestionProvider<CommandSourceStack> modes = (c, b) -> SharedSuggestionProvider.suggest(List.of("arms", "head", "none"), b);
        return Commands.literal("carry").requires(ModCommands::canEdit)
                .then(templeArg().then(Commands.argument("mode", StringArgumentType.word()).suggests(modes)
                        .executes(ctx -> {
                            Temple t = temple(ctx);
                            String mode = nameArg(ctx, "mode");
                            if (!List.of("arms", "head", "none").contains(mode)) {
                                throw ERROR.create("Choose arms, head or none.");
                            }
                            t.carry = mode;
                            TempleData.get(ctx.getSource().getLevel()).touch();
                            say(ctx, "In the seeveli of '" + t.name + "' the keezhshanthi will carry the idol: " + mode + ".");
                            return 1;
                        })));
    }

    // ------------------------------------------------------------------ seeveli route

    private static LiteralArgumentBuilder<CommandSourceStack> routeNode() {
        return Commands.literal("route")
                .then(Commands.literal("list").then(templeArg().executes(ctx -> {
                    Temple t = temple(ctx);
                    say(ctx, "'" + t.name + "' seeveli route: " + (t.route.isEmpty() ? "automatic (clockwise)" : t.route.size() + " stop(s)"));
                    int i = 1;
                    for (BlockPos p : t.route) {
                        say(ctx, "  " + (i++) + ". " + p.toShortString());
                    }
                    return t.route.size();
                })))
                .then(Commands.literal("clear").requires(ModCommands::canEdit).then(templeArg().executes(ctx -> {
                    Temple t = temple(ctx);
                    int n = t.route.size();
                    t.route.clear();
                    TempleData.get(ctx.getSource().getLevel()).touch();
                    say(ctx, "Cleared the seeveli route of '" + t.name + "'; the priest walks the automatic route again.");
                    return n;
                })));
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
