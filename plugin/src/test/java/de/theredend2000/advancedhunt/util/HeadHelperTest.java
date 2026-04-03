package de.theredend2000.advancedhunt.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class HeadHelperTest {

    @Test
    public void extractsOwnerNameFromLegacyRawSkullOwnerNbt() {
        String nbt = "{SkullOwner:{Name:\"Notch\"}}";

        assertEquals("Notch", HeadHelper.getProfileNameFromNbt(nbt));
    }

    @Test
    public void extractsOwnerNameFromModernRawProfileWithoutUsingTextureEntryName() {
        String nbt = "{profile:{name:\"Dinnerbone\",properties:[{name:\"textures\",value:\"abc123\"}]}}";

        assertEquals("Dinnerbone", HeadHelper.getProfileNameFromNbt(nbt));
    }

    @Test
    public void extractsTextureFromModernRawProfileProperties() {
        String nbt = "{profile:{name:\"Dinnerbone\",properties:[{name:\"textures\",value:\"abc123\"}]}}";

        assertEquals("abc123", HeadHelper.getTextureFromNbt(nbt));
    }

    @Test
    public void resolvesModernNonPlayerWallSkullToDisplayMaterial() {
        assertEquals("SKELETON_SKULL", HeadHelper.resolveHeadDisplayMaterialName("SKELETON_WALL_SKULL", null));
    }

    @Test
    public void resolvesLegacySkullUsingModernBlockStateSubtype() {
        assertEquals("ZOMBIE_HEAD",
                HeadHelper.resolveHeadDisplayMaterialName("SKULL_ITEM", "minecraft:zombie_head[rotation=0]"));
    }

    @Test
    public void defaultsAmbiguousLegacySkullToSkeletonDisplayMaterial() {
        assertEquals("SKELETON_SKULL", HeadHelper.resolveHeadDisplayMaterialName("SKULL_ITEM", "0"));
    }

    @Test
    public void detectsPlayerHeadMaterialFromBlockState() {
        assertTrue(HeadHelper.isPlayerHeadMaterialName("SKULL_ITEM", "minecraft:player_head[rotation=3]"));
        assertFalse(HeadHelper.isPlayerHeadMaterialName("SKELETON_SKULL", null));
    }
}