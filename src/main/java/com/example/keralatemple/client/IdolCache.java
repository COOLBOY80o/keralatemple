package com.example.keralatemple.client;

import com.example.keralatemple.KeralaTempleMod;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Loads idol photos from the "keralatemple_idols" folder in the game directory (client side) and keeps the
 * generated 3D models. Replacing a photo on disk updates the idol within a couple of seconds.
 */
@OnlyIn(Dist.CLIENT)
public final class IdolCache {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Pattern NAME = Pattern.compile("[a-z0-9_-]{1,32}");
    private static final String[] EXTENSIONS = {".png", ".jpg", ".jpeg"};

    public record IdolMesh(float[] data, int vertexCount, ResourceLocation texture) {}

    private static final class Entry {
        IdolMesh mesh;
        long modified = Long.MIN_VALUE;
        long checkedAt;
        boolean warned;
    }

    private static final Map<String, Entry> CACHE = new HashMap<>();
    private static int counter = 0;

    private IdolCache() {}

    public static Path folder() {
        return FMLPaths.GAMEDIR.get().resolve("keralatemple_idols");
    }

    /** The model for this photo name, or null if the photo is missing or unreadable. */
    public static IdolMesh get(String name) {
        if (!NAME.matcher(name).matches()) {
            return null;
        }
        Entry e = CACHE.computeIfAbsent(name, k -> new Entry());
        long now = System.currentTimeMillis();
        if (now - e.checkedAt >= 2000) {
            e.checkedAt = now;
            refresh(name, e);
        }
        return e.mesh;
    }

    private static Path find(String name) {
        for (String ext : EXTENSIONS) {
            Path p = folder().resolve(name + ext);
            if (Files.isRegularFile(p)) {
                return p;
            }
        }
        return null;
    }

    private static void refresh(String name, Entry e) {
        Path file = find(name);
        if (file == null) {
            release(e);
            e.modified = Long.MIN_VALUE;
            if (!e.warned) {
                e.warned = true;
                warn("Idol photo not found: put " + name + ".png (or .jpg) in " + folder());
            }
            return;
        }
        long modified;
        try {
            modified = Files.getLastModifiedTime(file).toMillis();
        } catch (Exception ex) {
            return;
        }
        if (modified == e.modified) {
            return;
        }
        e.modified = modified;
        e.warned = false;
        release(e);
        try {
            BufferedImage image = ImageIO.read(file.toFile());
            if (image == null) {
                throw new IllegalStateException("not a readable image");
            }
            IdolMeshBuilder.MeshData data = IdolMeshBuilder.build(image);
            e.mesh = upload(name, data);
        } catch (Exception ex) {
            LOGGER.warn("Could not build idol '{}' from {}", name, file, ex);
            e.warned = true;
            warn("Could not read the idol photo " + file.getFileName() + " (use a PNG or JPG).");
        }
    }

    private static IdolMesh upload(String name, IdolMeshBuilder.MeshData data) {
        NativeImage image = new NativeImage(data.texW(), data.texH(), false);
        for (int j = 0; j < data.texH(); j++) {
            for (int i = 0; i < data.texW(); i++) {
                int p = data.argb()[j * data.texW() + i];
                int a = (p >>> 24) & 255;
                int r = (p >> 16) & 255;
                int g = (p >> 8) & 255;
                int b = p & 255;
                image.setPixelRGBA(i, j, (a << 24) | (b << 16) | (g << 8) | r);
            }
        }
        ResourceLocation id = new ResourceLocation(KeralaTempleMod.MOD_ID, "idol/" + name + "_" + (counter++));
        Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(image));
        return new IdolMesh(data.verts(), data.vertexCount(), id);
    }

    private static void release(Entry e) {
        if (e.mesh != null) {
            Minecraft.getInstance().getTextureManager().release(e.mesh.texture());
            e.mesh = null;
        }
    }

    private static void warn(String message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal("[Kerala Temple] " + message), false);
        }
    }
}
