package com.example.keralatemple.item;

import com.example.keralatemple.config.Config;
import com.example.keralatemple.temple.Temple;
import com.example.keralatemple.temple.TempleData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The Temple Wand. Sneak + right-click changes the mode; right-click a block to use the mode.
 *  - Temple centre: marks (or moves) the centre of the selected temple. With no temple selected it creates one.
 *  - Bali stone: adds or removes a bali point for the selected temple.
 *  - Valiya balikkal: click two opposite corners of a structure you built; it becomes a valiya balikkal
 *    for the temple, and the region is remembered so /keralatemple balikkal save can store it as a template.
 */
public class TempleWandItem extends Item {

    public enum Mode {
        TEMPLE_CENTER("temple_center", "Temple centre"),
        BALI_STONE("bali_stone", "Bali stone"),
        VALIYA_BALIKKAL("valiya_balikkal", "Valiya balikkal"),
        SEEVELI_ROUTE("seeveli_route", "Seeveli route");

        public final String id;
        public final String label;

        Mode(String id, String label) {
            this.id = id;
            this.label = label;
        }

        public Mode next() {
            return values()[(ordinal() + 1) % values().length];
        }

        public static Mode byId(String id) {
            for (Mode m : values()) {
                if (m.id.equals(id)) {
                    return m;
                }
            }
            return TEMPLE_CENTER;
        }
    }

    public TempleWandItem(Properties properties) {
        super(properties);
    }

    // ------------------------------------------------------------------ helpers shared with the commands

    public static Mode getMode(ItemStack stack) {
        return Mode.byId(stack.getOrCreateTag().getString("Mode"));
    }

    public static void setMode(ItemStack stack, Mode mode) {
        stack.getOrCreateTag().putString("Mode", mode.id);
    }

    public static String getTempleName(ItemStack stack) {
        return stack.getOrCreateTag().getString("Temple");
    }

    /** The wand in the player's main hand, else off hand, else null. */
    @Nullable
    public static ItemStack findWand(Player player) {
        if (player.getMainHandItem().getItem() instanceof TempleWandItem) {
            return player.getMainHandItem();
        }
        if (player.getOffhandItem().getItem() instanceof TempleWandItem) {
            return player.getOffhandItem();
        }
        return null;
    }

    public static void selectOnHeldWand(Player player, String templeName) {
        ItemStack wand = findWand(player);
        if (wand != null) {
            wand.getOrCreateTag().putString("Temple", templeName);
        }
    }

    // ------------------------------------------------------------------ interaction

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                cycleMode(player, stack);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return InteractionResultHolder.pass(stack);
    }

    private static void cycleMode(Player player, ItemStack stack) {
        Mode next = getMode(stack).next();
        setMode(stack, next);
        player.displayClientMessage(Component.literal("Temple Wand mode: " + next.label).withStyle(ChatFormatting.GOLD), true);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Player player = ctx.getPlayer();
        Level level = ctx.getLevel();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        ItemStack stack = ctx.getItemInHand();
        if (player.isShiftKeyDown()) {
            cycleMode(player, stack);
            return InteractionResult.CONSUME;
        }
        if (!(player instanceof ServerPlayer sp) || !(level instanceof ServerLevel sl)) {
            return InteractionResult.PASS;
        }
        if (!sp.hasPermissions(Config.COMMAND_PERMISSION_LEVEL.get())) {
            sp.displayClientMessage(Component.literal("You are not allowed to use the Temple Wand here.")
                    .withStyle(ChatFormatting.RED), true);
            return InteractionResult.CONSUME;
        }

        BlockPos pos = ctx.getClickedPos();
        TempleData data = TempleData.get(sl);
        CompoundTag tag = stack.getOrCreateTag();

        switch (getMode(stack)) {
            case TEMPLE_CENTER -> useCenter(sp, data, tag, pos);
            case BALI_STONE -> useBaliStone(sp, data, tag, pos);
            case VALIYA_BALIKKAL -> useValiya(sp, data, tag, pos);
            case SEEVELI_ROUTE -> useRoute(sp, data, tag, pos);
        }
        return InteractionResult.CONSUME;
    }

    private static Temple selected(TempleData data, CompoundTag tag, BlockPos near) {
        String name = tag.getString("Temple");
        Temple t = name.isEmpty() ? null : data.get(name);
        if (t == null) {
            t = data.findFor(near);
            if (t != null) {
                tag.putString("Temple", t.name);
            }
        }
        return t;
    }

    private static void tell(ServerPlayer sp, String message) {
        sp.sendSystemMessage(Component.literal("[Temple Wand] " + message).withStyle(ChatFormatting.GOLD));
    }

    private static void useCenter(ServerPlayer sp, TempleData data, CompoundTag tag, BlockPos pos) {
        String name = tag.getString("Temple");
        Temple t = name.isEmpty() ? null : data.get(name);
        if (t == null) {
            String newName = data.nextName();
            data.create(newName, pos);
            tag.putString("Temple", newName);
            tell(sp, "Created temple '" + newName + "' centred here. Rename is not needed; use /keralatemple temple info "
                    + newName + ". Next: switch to Bali stone mode (sneak + right-click).");
        } else {
            t.center = pos;
            data.touch();
            tell(sp, "Moved the centre of '" + t.name + "' to " + pos.toShortString() + ".");
        }
    }

    private static void useRoute(ServerPlayer sp, TempleData data, CompoundTag tag, BlockPos pos) {
        Temple t = selected(data, tag, pos);
        if (t == null) {
            tell(sp, "No temple selected. Mark a temple first (Temple centre mode).");
            return;
        }
        if (t.route.remove(pos)) {
            data.touch();
            tell(sp, "Removed that stop from the seeveli route of '" + t.name + "' (" + t.route.size() + " left).");
        } else {
            t.route.add(pos);
            data.touch();
            tell(sp, "Seeveli route stop " + t.route.size() + " added. Click the stops in the order the priest should walk; "
                    + "click a stop again to remove it. The route repeats for three circuits.");
        }
    }

    private static void useBaliStone(ServerPlayer sp, TempleData data, CompoundTag tag, BlockPos pos) {
        Temple t = selected(data, tag, pos);
        if (t == null) {
            tell(sp, "No temple selected. Mark a temple first (Temple centre mode) or use /keralatemple temple select <name>.");
            return;
        }
        if (t.baliStones.remove(pos)) {
            data.touch();
            tell(sp, "Removed that bali point from '" + t.name + "' (" + t.baliStones.size() + " left).");
        } else {
            t.baliStones.add(pos);
            data.touch();
            tell(sp, "Added a bali point to '" + t.name + "' (" + t.baliStones.size() + " total). Click it again to remove it.");
        }
    }

    private static void useValiya(ServerPlayer sp, TempleData data, CompoundTag tag, BlockPos pos) {
        if (!tag.contains("CornerA")) {
            tag.putLong("CornerA", pos.asLong());
            tell(sp, "Corner A set at " + pos.toShortString() + ". Now click the opposite corner of your valiya balikkal.");
            return;
        }
        BlockPos a = BlockPos.of(tag.getLong("CornerA"));
        tag.remove("CornerA");
        tag.putLong("SelA", a.asLong());
        tag.putLong("SelB", pos.asLong());

        BlockPos min = new BlockPos(Math.min(a.getX(), pos.getX()), Math.min(a.getY(), pos.getY()), Math.min(a.getZ(), pos.getZ()));
        BlockPos max = new BlockPos(Math.max(a.getX(), pos.getX()), Math.max(a.getY(), pos.getY()), Math.max(a.getZ(), pos.getZ()));
        String size = (max.getX() - min.getX() + 1) + "x" + (max.getY() - min.getY() + 1) + "x" + (max.getZ() - min.getZ() + 1);

        Temple t = selected(data, tag, pos);
        if (t == null) {
            tell(sp, "Region " + size + " selected, but no temple is selected so it was not registered. "
                    + "You can still store it with /keralatemple balikkal save <name>.");
            return;
        }
        int n = t.valiya.size() + 1;
        String name = "valiya_" + n;
        while (hasValiya(t, name)) {
            n++;
            name = "valiya_" + n;
        }
        t.valiya.add(new Temple.Valiya(name, min, max));
        data.touch();
        tell(sp, "Registered '" + name + "' (" + size + ") as a valiya balikkal of '" + t.name
                + "'. Save its design with /keralatemple balikkal save <name>.");
    }

    private static boolean hasValiya(Temple t, String name) {
        for (Temple.Valiya v : t.valiya) {
            if (v.name().equals(name)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.literal("Mode: " + getMode(stack).label).withStyle(ChatFormatting.GOLD));
        String temple = getTempleName(stack);
        lines.add(Component.literal("Temple: " + (temple.isEmpty() ? "none selected" : temple)).withStyle(ChatFormatting.GRAY));
        lines.add(Component.literal("Sneak + right-click: change mode").withStyle(ChatFormatting.DARK_GRAY));
        lines.add(Component.literal("Right-click a block: use the mode").withStyle(ChatFormatting.DARK_GRAY));
    }
}
