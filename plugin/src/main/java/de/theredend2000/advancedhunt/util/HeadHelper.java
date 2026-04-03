package de.theredend2000.advancedhunt.util;

import com.cryptomorin.xseries.XMaterial;
import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.nbtapi.iface.ReadWriteNBTCompoundList;
import org.bukkit.Bukkit;
import org.bukkit.block.BlockState;
import org.bukkit.block.Skull;
import org.bukkit.inventory.ItemStack;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HeadHelper {

    private static final Pattern SKIN_URL_PATTERN = Pattern.compile("\\\"url\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");

    public static final class SkullProfileData {
        private final String texture;
        private final String ownerName;

        private SkullProfileData(String texture, String ownerName) {
            this.texture = texture;
            this.ownerName = ownerName;
        }

        public String texture() {
            return texture;
        }

        public String ownerName() {
            return ownerName;
        }

        public boolean hasRenderableData() {
            return (texture != null && !texture.isEmpty()) || (ownerName != null && !ownerName.isEmpty());
        }
    }

    /**
     * Fast heuristic for determining whether a stored material name represents a head/skull.
     * This is important for legacy versions (1.8-1.12) where skulls are represented by
     * {@code SKULL}/{@code SKULL_ITEM} + durability, and XMaterial matching can otherwise
     * resolve to a non-player skull type (e.g. creeper).
     */
    public static boolean isHeadMaterialName(String materialName) {
        if (materialName == null) return false;
        String name = materialName.trim();
        if (name.isEmpty()) return false;
        name = name.toUpperCase(java.util.Locale.ROOT);

        if ("SKULL".equals(name) || "SKULL_ITEM".equals(name) || "LEGACY_SKULL".equals(name) || "LEGACY_SKULL_ITEM".equals(name)) {
            return true;
        }
        return name.endsWith("_HEAD")
                || name.endsWith("_WALL_HEAD")
                || name.endsWith("_SKULL")
                || name.endsWith("_WALL_SKULL");
    }

    public static boolean isPlayerHead(ItemStack item) {
        XMaterial material = XMaterial.matchXMaterial(item);
        return material == XMaterial.PLAYER_HEAD || material == XMaterial.PLAYER_WALL_HEAD;
    }

    public static SkullProfileData getSkullProfileData(String nbtData) {
        if (nbtData == null || nbtData.isEmpty()) return null;

        try {
            ReadWriteNBT nbt = NBT.parseNBT(nbtData);
            ReadWriteNBT profile = getProfileCompound(nbt);

            if (profile != null) {
                return buildProfileData(extractTextureFromProfile(profile), extractProfileName(profile));
            }
        } catch (Throwable ignored) {
        }

        return extractProfileDataFromRawNbt(nbtData);
    }

    public static String getTextureFromNbt(String nbtData) {
        SkullProfileData profileData = getSkullProfileData(nbtData);
        return profileData == null ? null : profileData.texture();
    }

    public static String getProfileNameFromNbt(String nbtData) {
        SkullProfileData profileData = getSkullProfileData(nbtData);
        return profileData == null ? null : profileData.ownerName();
    }

    public static boolean applySkullProfile(BlockState state, String nbtData) {
        if (!(state instanceof Skull)) return false;
        if (nbtData == null || nbtData.isEmpty()) return false;

        Skull skull = (Skull) state;
        boolean copiedStoredProfile = copyStoredProfile(state, nbtData);

        SkullProfileData profileData = getSkullProfileData(nbtData);
        if (profileData == null) {
            return copiedStoredProfile;
        }

        if (profileData.texture() != null && !profileData.texture().isEmpty() && applyTexture(skull, profileData.texture())) {
            return true;
        }

        if (profileData.ownerName() != null && !profileData.ownerName().isEmpty()) {
            if (applyOwnerName(skull, profileData.ownerName())) {
                return true;
            }
        }

        return copiedStoredProfile;
    }

    private static boolean copyStoredProfile(BlockState state, String nbtData) {
        try {
            ReadWriteNBT source = NBT.parseNBT(nbtData);
            if (!hasStoredProfileCompound(source)) {
                return false;
            }

            final boolean[] copied = new boolean[] { false };
            NBT.modify(state, (java.util.function.Consumer<ReadWriteNBT>) nbt -> {
                copied[0] = mergeStoredProfileCompounds(nbt, source);
            });
            return copied[0];
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static ReadWriteNBT getProfileCompound(ReadWriteNBT nbt) {
        if (nbt.hasTag("Owner")) return nbt.getCompound("Owner");
        if (nbt.hasTag("SkullOwner")) return nbt.getCompound("SkullOwner");
        if (nbt.hasTag("profile")) return nbt.getCompound("profile");
        if (nbt.hasTag("minecraft:profile")) return nbt.getCompound("minecraft:profile");
        return null;
    }

    private static boolean hasStoredProfileCompound(ReadWriteNBT nbt) {
        return mergeStoredProfileCompounds(null, nbt);
    }

    private static boolean mergeStoredProfileCompounds(ReadWriteNBT target, ReadWriteNBT source) {
        boolean copied = false;
        copied |= mergeStoredProfileCompound(target, source, "Owner");
        copied |= mergeStoredProfileCompound(target, source, "SkullOwner");
        copied |= mergeStoredProfileCompound(target, source, "profile");
        copied |= mergeStoredProfileCompound(target, source, "minecraft:profile");
        return copied;
    }

    private static boolean mergeStoredProfileCompound(ReadWriteNBT target, ReadWriteNBT source, String key) {
        if (source == null) {
            return false;
        }

        try {
            if (!source.hasTag(key)) {
                return false;
            }

            ReadWriteNBT sourceCompound = source.getCompound(key);
            if (sourceCompound == null) {
                return false;
            }

            if (target != null) {
                target.getOrCreateCompound(key).mergeCompound(sourceCompound);
            }
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static SkullProfileData buildProfileData(String texture, String ownerName) {
        String normalizedTexture = normalizeValue(texture);
        String normalizedOwnerName = normalizeValue(ownerName);
        if (normalizedTexture == null && normalizedOwnerName == null) {
            return null;
        }
        return new SkullProfileData(normalizedTexture, normalizedOwnerName);
    }

    private static boolean applyTexture(Skull skull, String texture) {
        String skinUrl = getSkinUrl(texture);
        if (skinUrl == null || skinUrl.isEmpty()) return false;

        try {
            Class<?> profileClass = Class.forName("org.bukkit.profile.PlayerProfile");
            Object profile = createPlayerProfile();
            if (profile == null) return false;

            Object textures = profileClass.getMethod("getTextures").invoke(profile);
            if (textures == null) return false;

            Class<?> texturesClass = textures.getClass();
            texturesClass.getMethod("setSkin", URL.class).invoke(textures, new URL(skinUrl));
            try {
                profileClass.getMethod("setTextures", texturesClass).invoke(profile, textures);
            } catch (Throwable ignored) {
            }

            skull.getClass().getMethod("setOwnerProfile", profileClass).invoke(skull, profile);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Object createPlayerProfile() {
        try {
            return Bukkit.class.getMethod("createPlayerProfile", UUID.class, String.class)
                    .invoke(null, UUID.randomUUID(), null);
        } catch (Throwable ignored) {
        }

        try {
            return Bukkit.class.getMethod("createPlayerProfile", UUID.class)
                    .invoke(null, UUID.randomUUID());
        } catch (Throwable ignored) {
        }

        return null;
    }

    private static boolean applyOwnerName(Skull skull, String ownerName) {
        try {
            Object profile = createNamedPlayerProfile(ownerName);
            if (profile != null) {
                Class<?> profileClass = Class.forName("org.bukkit.profile.PlayerProfile");
                skull.getClass().getMethod("setOwnerProfile", profileClass).invoke(skull, profile);
                return true;
            }
        } catch (Throwable ignored) {
        }

        try {
            skull.getClass().getMethod("setOwner", String.class).invoke(skull, ownerName);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Object createNamedPlayerProfile(String ownerName) {
        if (ownerName == null || ownerName.trim().isEmpty()) {
            return null;
        }

        try {
            return Bukkit.class.getMethod("createPlayerProfile", String.class)
                    .invoke(null, ownerName);
        } catch (Throwable ignored) {
        }

        try {
            return Bukkit.class.getMethod("createPlayerProfile", UUID.class, String.class)
                    .invoke(null, null, ownerName);
        } catch (Throwable ignored) {
        }

        return null;
    }

    private static String getSkinUrl(String texture) {
        if (texture == null || texture.isEmpty()) return null;

        try {
            String decoded = new String(Base64.getDecoder().decode(texture), StandardCharsets.UTF_8);
            Matcher matcher = SKIN_URL_PATTERN.matcher(decoded);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (IllegalArgumentException ignored) {
        }

        return null;
    }

    private static String extractTextureFromProfile(ReadWriteNBT profile) {
        // Try "Properties" (Capitalized)
        if (profile.hasTag("Properties")) {
            ReadWriteNBT props = profile.getCompound("Properties");
            if (props.hasTag("textures")) {
                return getTextureFromList(props.getCompoundList("textures"));
            }
        }
        
        // Try "properties" (Lowercase)
        if (profile.hasTag("properties")) {
            // Try as a list directly (1.20.5+ component format sometimes)
             try {
                 ReadWriteNBTCompoundList list = profile.getCompoundList("properties");
                 for (ReadWriteNBT entry : list) {
                     if (entry.hasTag("name") && "textures".equals(entry.getString("name"))) {
                         return entry.getString("value");
                     }
                 }
             } catch (Exception ignored) {}
             
             // Try as a compound
             try {
                 ReadWriteNBT props = profile.getCompound("properties");
                 if (props.hasTag("textures")) {
                     return getTextureFromList(props.getCompoundList("textures"));
                 }
             } catch (Exception ignored) {}
        }
        return null;
    }

    private static String extractProfileName(ReadWriteNBT profile) {
        if (profile.hasTag("name")) {
            return normalizeValue(profile.getString("name"));
        }
        if (profile.hasTag("Name")) {
            return normalizeValue(profile.getString("Name"));
        }
        return null;
    }

    private static SkullProfileData extractProfileDataFromRawNbt(String nbtData) {
        String profileBody = findProfileCompoundBody(nbtData);
        if (profileBody == null) {
            return null;
        }

        String texture = extractTextureFromRawProfile(profileBody);
        String ownerName = extractTopLevelStringProperty(profileBody, "name", "Name");
        return buildProfileData(texture, ownerName);
    }

    private static String findProfileCompoundBody(String nbtData) {
        String[] keys = new String[] { "SkullOwner", "Owner", "profile", "minecraft:profile" };
        for (String key : keys) {
            int braceIndex = findCompoundStart(nbtData, key);
            if (braceIndex != -1) {
                return extractBalancedSection(nbtData, braceIndex, '{', '}');
            }
        }
        return null;
    }

    private static int findCompoundStart(String nbtData, String key) {
        Pattern pattern = Pattern.compile("(?:^|[\\{,])\\s*\\\"?" + Pattern.quote(key) + "\\\"?\\s*:\\s*\\{");
        Matcher matcher = pattern.matcher(nbtData);
        if (matcher.find()) {
            return matcher.end() - 1;
        }
        return -1;
    }

    private static String extractBalancedSection(String value, int openIndex, char openChar, char closeChar) {
        if (value == null || openIndex < 0 || openIndex >= value.length() || value.charAt(openIndex) != openChar) {
            return null;
        }

        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int index = openIndex; index < value.length(); index++) {
            char current = value.charAt(index);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (current == '\\') {
                escaped = true;
                continue;
            }
            if (current == '"') {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }
            if (current == openChar) {
                depth++;
            } else if (current == closeChar) {
                depth--;
                if (depth == 0) {
                    return value.substring(openIndex + 1, index);
                }
            }
        }
        return null;
    }

    private static String extractTextureFromRawProfile(String profileBody) {
        if (profileBody == null || profileBody.isEmpty()) {
            return null;
        }

        Matcher matcher = Pattern.compile("\\b(?:Value|value)\\b\\s*:\\s*\"([^\"]+)\"").matcher(profileBody);
        if (matcher.find()) {
            return normalizeValue(matcher.group(1));
        }
        return null;
    }

    private static String extractTopLevelStringProperty(String compoundBody, String... keys) {
        if (compoundBody == null || compoundBody.isEmpty()) {
            return null;
        }

        java.util.Map<String, String> properties = new java.util.LinkedHashMap<String, String>();
        for (String entry : splitTopLevel(compoundBody)) {
            int separator = findTopLevelSeparator(entry, ':');
            if (separator == -1) {
                continue;
            }

            String rawKey = trimQuotes(entry.substring(0, separator).trim());
            String rawValue = entry.substring(separator + 1).trim();
            if (rawKey.isEmpty() || rawValue.length() < 2 || rawValue.charAt(0) != '"' || rawValue.charAt(rawValue.length() - 1) != '"') {
                continue;
            }
            properties.put(rawKey, trimQuotes(rawValue));
        }

        for (String key : keys) {
            String value = normalizeValue(properties.get(key));
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static java.util.List<String> splitTopLevel(String compoundBody) {
        java.util.List<String> parts = new java.util.ArrayList<String>();
        int start = 0;
        int compoundDepth = 0;
        int listDepth = 0;
        boolean inString = false;
        boolean escaped = false;

        for (int index = 0; index < compoundBody.length(); index++) {
            char current = compoundBody.charAt(index);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (current == '\\') {
                escaped = true;
                continue;
            }
            if (current == '"') {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }

            if (current == '{') {
                compoundDepth++;
            } else if (current == '}') {
                compoundDepth--;
            } else if (current == '[') {
                listDepth++;
            } else if (current == ']') {
                listDepth--;
            } else if (current == ',' && compoundDepth == 0 && listDepth == 0) {
                parts.add(compoundBody.substring(start, index));
                start = index + 1;
            }
        }

        if (start < compoundBody.length()) {
            parts.add(compoundBody.substring(start));
        }
        return parts;
    }

    private static int findTopLevelSeparator(String value, char separator) {
        int compoundDepth = 0;
        int listDepth = 0;
        boolean inString = false;
        boolean escaped = false;

        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (current == '\\') {
                escaped = true;
                continue;
            }
            if (current == '"') {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }

            if (current == '{') {
                compoundDepth++;
            } else if (current == '}') {
                compoundDepth--;
            } else if (current == '[') {
                listDepth++;
            } else if (current == ']') {
                listDepth--;
            } else if (current == separator && compoundDepth == 0 && listDepth == 0) {
                return index;
            }
        }

        return -1;
    }

    private static String trimQuotes(String value) {
        if (value == null || value.length() < 2) {
            return value;
        }
        if (value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"') {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static String normalizeValue(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String getTextureFromList(ReadWriteNBTCompoundList list) {
        if (!list.isEmpty()) {
            ReadWriteNBT entry = list.get(0);
            if (entry.hasTag("Value")) return entry.getString("Value");
            if (entry.hasTag("value")) return entry.getString("value");
        }
        return null;
    }
}
