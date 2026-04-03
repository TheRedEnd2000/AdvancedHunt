package de.theredend2000.advancedhunt.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

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
}