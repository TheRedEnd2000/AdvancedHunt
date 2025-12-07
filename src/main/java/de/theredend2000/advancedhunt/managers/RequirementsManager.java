package de.theredend2000.advancedhunt.managers;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.util.DateTimeUtil;
import de.theredend2000.advancedhunt.util.enums.DeletionTypes;
import de.theredend2000.advancedhunt.util.enums.Requirements;
import de.theredend2000.advancedhunt.util.messages.MessageKey;
import de.theredend2000.advancedhunt.util.messages.MessageManager;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;

import java.time.LocalTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class RequirementsManager {
    private static final String REQUIREMENTS_PATH = "Requirements.";
    private static final String RESET_PATH = "Reset.";
    
    private final Main plugin;
    private final MessageManager messageManager;

    public RequirementsManager() {
        this.plugin = Main.getInstance();
        this.messageManager = plugin.getMessageManager();
    }

    /**
     * Asynchronously generates the requirements lore list
     * 
     * @param collection The egg collection name
     * @param callback Consumer to handle the generated lore list
     */
    public void getListRequirementsLoreAsync(String collection, Consumer<List<String>> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<String> lore = getListRequirementsLore(collection);
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(lore));
        });
    }

    /**
     * Generates a formatted list of requirements for display
     * 
     * @param collection The egg collection name
     * @return List of formatted lore strings
     */
    private List<String> getListRequirementsLore(String collection) {
        ArrayList<String> lore = new ArrayList<>();
        lore.add("§6§lListed:");
        FileConfiguration placedEggs = plugin.getEggDataManager().getPlacedEggs(collection);
        
        // Add each requirement type to the lore
        addRequirementTypeToLore(lore, placedEggs, "Hours", MessageKey.REQUIREMENTS_NAME_HOUR, 
                list -> list.sort(Comparator.comparingInt(Integer::parseInt)));
        
        addRequirementTypeToLore(lore, placedEggs, "Date", MessageKey.REQUIREMENTS_NAME_DATE, 
                list -> list.sort(Comparator.comparingInt(day -> DateTimeUtil.getAllDaysOfYear().indexOf(day))));
        
        addRequirementTypeToLore(lore, placedEggs, "Weekday", MessageKey.REQUIREMENTS_NAME_WEEKDAY, 
                list -> list.sort(Comparator.comparingInt(day -> DateTimeUtil.getWeekList().indexOf(day))));
        
        addRequirementTypeToLore(lore, placedEggs, "Month", MessageKey.REQUIREMENTS_NAME_MONTH, 
                list -> list.sort(Comparator.comparingInt(day -> DateTimeUtil.getMonthList().indexOf(day))));
        
        addRequirementTypeToLore(lore, placedEggs, "Year", MessageKey.REQUIREMENTS_NAME_YEAR, 
                list -> list.sort(Comparator.comparingInt(Integer::parseInt)));
        
        addRequirementTypeToLore(lore, placedEggs, "Season", MessageKey.REQUIREMENTS_NAME_SEASON, 
                list -> list.sort(Comparator.comparingInt(day -> DateTimeUtil.getSeasonList().indexOf(day))));
        
        lore.add("");
        lore.add(messageManager.getMessage(MessageKey.REQUIREMENTS_CLICK_TO_CHANGE));
        return lore;
    }
    
    /**
     * Helper method to add a specific requirement type to the lore list
     * 
     * @param lore The lore list to add to
     * @param config The configuration containing requirements
     * @param requirementType The type of requirement (Hours, Date, etc.)
     * @param titleKey The message key for the requirement title
     * @param sorter A consumer that sorts the list of requirements
     */
    private void addRequirementTypeToLore(List<String> lore, FileConfiguration config, 
                                         String requirementType, MessageKey titleKey,
                                         Consumer<List<String>> sorter) {
        String requirementPath = REQUIREMENTS_PATH + requirementType;
        lore.add("§d" + messageManager.getMessage(titleKey) + ":");
        
        if (config.contains(requirementPath)) {
            List<String> itemList = new ArrayList<>(config.getConfigurationSection(requirementPath).getKeys(false));
            
            if (!itemList.isEmpty()) {
                sorter.accept(itemList);
                int counter = 0;
                
                for (String item : itemList) {
                    if (config.getBoolean(requirementPath + "." + item)) {
                        if (counter < 3) {
                            lore.add("  §7- " + item);
                        }
                        counter++;
                    }
                }
                
                if (counter == 0) {
                    lore.add("  §cN/A");
                }
                
                if (counter > 3) {
                    lore.add("  §7§o+" + (counter - 3) + " " + 
                            messageManager.getMessage(MessageKey.REQUIREMENTS_MORE) + "...");
                }
            } else {
                lore.add("  §cN/A");
            }
        } else {
            lore.add("  §cN/A");
        }
    }

    /**
     * Converts the requirement order string to a boolean
     * 
     * @param current The order string ("AND" or "OR")
     * @return true for AND, false for OR
     */
    private boolean convertCurrentOrder(String current) {
        return "AND".equalsIgnoreCase(current);
    }

    /**
     * Checks if the collection can be accessed based on current time requirements
     * 
     * @param collection The egg collection name
     * @param current The requirement order ("AND" or "OR")
     * @return true if the collection can be accessed, false otherwise
     */
    public boolean canBeAccessed(String collection, String current) {
        boolean requireAll = convertCurrentOrder(current);
        FileConfiguration placedEggs = plugin.getEggDataManager().getPlacedEggs(collection);
        
        // Get current time values
        CurrentTimeValues timeValues = getCurrentTimeValues();
        
        // Check each requirement type
        Map<String, Boolean> matches = new HashMap<>();
        matches.put("Hours", checkMatch(placedEggs, "Hours", timeValues.hour));
        matches.put("Date", checkMatch(placedEggs, "Date", timeValues.date));
        matches.put("Weekday", checkMatch(placedEggs, "Weekday", timeValues.weekday));
        matches.put("Month", checkMatch(placedEggs, "Month", timeValues.month));
        matches.put("Year", checkMatch(placedEggs, "Year", timeValues.year));
        matches.put("Season", checkMatch(placedEggs, "Season", timeValues.season));
        
        // Apply AND / OR logic
        if (requireAll) {
            return matches.values().stream().allMatch(Boolean::booleanValue);
        } else {
            return matches.values().stream().anyMatch(Boolean::booleanValue);
        }
    }

    /**
     * Gibt eine detaillierte Analyse zurück, warum die Collection nicht zugänglich ist.
     *
     * @param collection Die Collection
     * @param current "AND" oder "OR"
     * @return Liste von Strings mit Beschreibung, warum es nicht geht (inkl. erlaubter Werte)
     */
    public List<TextComponent> getUnmetRequirementsDetailed(String collection, String current) {
        List<TextComponent> result = new ArrayList<>();
        FileConfiguration placedEggs = plugin.getEggDataManager().getPlacedEggs(collection);

        boolean requireAll = convertCurrentOrder(current);
        CurrentTimeValues timeValues = getCurrentTimeValues();

        // Key -> [CurrentValue, Matches?]
        Map<String, Boolean> matches = new LinkedHashMap<>();
        Map<String, String> currentValues = new LinkedHashMap<>();
        currentValues.put("Hours", timeValues.hour);
        currentValues.put("Date", timeValues.date);
        currentValues.put("Weekday", timeValues.weekday);
        currentValues.put("Month", timeValues.month);
        currentValues.put("Year", timeValues.year);
        currentValues.put("Season", timeValues.season);

        // Überprüfen und speichern
        for (Map.Entry<String, String> entry : currentValues.entrySet()) {
            String key = entry.getKey();
            String currentVal = entry.getValue();
            boolean ok = checkMatch(placedEggs, key, currentVal);
            matches.put(key, ok);
        }

        // Wenn bei AND einer false ist → alle false melden
        // Bei OR → nur wenn keiner true ist
        boolean allOk = requireAll ? matches.values().stream().allMatch(Boolean::booleanValue)
                : matches.values().stream().anyMatch(Boolean::booleanValue);

        if (allOk) return result; // alles in Ordnung

        // Fehlende Requirements mit erlaubten Werten
        for (Map.Entry<String, Boolean> entry : matches.entrySet()) {
            if (!entry.getValue()) {
                String key = entry.getKey();
                String currentVal = currentValues.get(key);
                String path = REQUIREMENTS_PATH + key;
                List<String> allowed = new ArrayList<>();

                if (placedEggs.contains(path)) {
                    for (String k : placedEggs.getConfigurationSection(path).getKeys(false)) {
                        if (placedEggs.getBoolean(path + "." + k)) {
                            allowed.add(k);
                        }
                    }
                }

                String name = getRequirementDisplayName(key);

                TextComponent line = new TextComponent("§8• §c" + name + " §8(hover)");
                line.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new ComponentBuilder("§7Current: \n§e" + currentVal +"\n\n§7Allowed: \n§a" + String.join(", ", allowed)).create()));
                result.add(line);
            }
        }

        return result;
    }

    private String getRequirementDisplayName(String key) {
        switch (key) {
            case "Hours": return messageManager.getMessage(MessageKey.REQUIREMENTS_NAME_HOUR);
            case "Date": return messageManager.getMessage(MessageKey.REQUIREMENTS_NAME_DATE);
            case "Weekday": return messageManager.getMessage(MessageKey.REQUIREMENTS_NAME_WEEKDAY);
            case "Month": return messageManager.getMessage(MessageKey.REQUIREMENTS_NAME_MONTH);
            case "Year": return messageManager.getMessage(MessageKey.REQUIREMENTS_NAME_YEAR);
            case "Season": return messageManager.getMessage(MessageKey.REQUIREMENTS_NAME_SEASON);
            default: return key;
        }
    }


    /**
     * Helper class to store current time values
     */
    private static class CurrentTimeValues {
        final String hour;
        final String date;
        final String weekday;
        final String month;
        final String year;
        final String season;
        
        CurrentTimeValues(String hour, String date, String weekday, String month, String year, String season) {
            this.hour = hour;
            this.date = date;
            this.weekday = weekday;
            this.month = month;
            this.year = year;
            this.season = season;
        }
    }
    
    /**
     * Gets the current time values for all requirement types
     * 
     * @return CurrentTimeValues object with all current time values
     */
    private CurrentTimeValues getCurrentTimeValues() {
        return new CurrentTimeValues(
            String.valueOf(LocalTime.now().getHour()),
            DateTimeUtil.getCurrentDateString(),
            String.valueOf(DateTimeUtil.getWeek(Calendar.getInstance())),
            String.valueOf(DateTimeUtil.getMonth(Calendar.getInstance())),
            String.valueOf(DateTimeUtil.getCurrentYear()),
            String.valueOf(DateTimeUtil.getCurrentSeason())
        );
    }

    /**
     * Checks if the current value matches any enabled requirement
     * 
     * @param placedEggs The configuration containing requirements
     * @param key The requirement type key
     * @param current The current value to check
     * @return true if there's a match or if no requirements are enabled
     */
    private boolean checkMatch(FileConfiguration placedEggs, String key, String current) {
        String requirementPath = REQUIREMENTS_PATH + key;
        
        if (!placedEggs.contains(requirementPath)) {
            return true;
        }
        
        Set<String> keys = placedEggs.getConfigurationSection(requirementPath).getKeys(false);
        
        // Check if the current value matches any enabled requirement
        boolean matched = keys.stream()
                .filter(k -> placedEggs.getBoolean(requirementPath + "." + k))
                .anyMatch(k -> k.equals(current));
                
        // If no requirements are enabled, consider it a match
        boolean noneEnabled = keys.stream()
                .noneMatch(k -> placedEggs.getBoolean(requirementPath + "." + k));
                
        return matched || noneEnabled;
    }



    /**
     * Changes the activity state of all requirements
     * 
     * @param collection The egg collection name
     * @param active The new activity state
     */
    public void changeActivity(String collection, boolean active) {
        FileConfiguration placedEggs = plugin.getEggDataManager().getPlacedEggs(collection);
        
        // Set hours (0-23)
        for (int i = 0; i < 24; i++) {
            placedEggs.set(REQUIREMENTS_PATH + "Hours." + i, active);
        }
        
        // Set dates (all days of year)
        for (String currentDate : DateTimeUtil.getAllDaysOfYear()) {
            placedEggs.set(REQUIREMENTS_PATH + "Date." + currentDate, active);
        }
        
        // Set weekdays
        for (String weekday : DateTimeUtil.getWeekList()) {
            placedEggs.set(REQUIREMENTS_PATH + "Weekday." + weekday, active);
        }
        
        // Set months
        for (String month : DateTimeUtil.getMonthList()) {
            placedEggs.set(REQUIREMENTS_PATH + "Month." + month, active);
        }
        
        // Set years (current year + 27 more)
        int currentYear = DateTimeUtil.getCurrentYear();
        for (int year = currentYear; year < (currentYear + 28); year++) {
            placedEggs.set(REQUIREMENTS_PATH + "Year." + year, active);
        }
        
        // Set seasons
        for (String season : DateTimeUtil.getSeasonList()) {
            placedEggs.set(REQUIREMENTS_PATH + "Season." + season, active);
        }
        
        plugin.getEggDataManager().savePlacedEggs(collection);
    }

    /**
     * Resets all reset time values to zero
     * 
     * @param collection The egg collection name
     */
    public void resetReset(String collection) {
        FileConfiguration placedEggs = plugin.getEggDataManager().getPlacedEggs(collection);
        
        // Reset all time values
        String[] timeUnits = {"Year", "Month", "Date", "Hour", "Minute", "Second"};
        for (String unit : timeUnits) {
            placedEggs.set(RESET_PATH + unit, 0);
        }
        
        plugin.getEggDataManager().savePlacedEggs(collection);
    }

    /**
     * Constants for time conversion
     */
    private static final int SECONDS_PER_MINUTE = 60;
    private static final int SECONDS_PER_HOUR = 60 * 60;
    private static final int SECONDS_PER_DAY = 24 * 60 * 60;
    private static final int SECONDS_PER_MONTH = 30 * 24 * 60 * 60;
    private static final int SECONDS_PER_YEAR = 365 * 24 * 60 * 60;
    
    /**
     * Calculates the total reset time in seconds
     * 
     * @param collection The egg collection name
     * @return Total time in seconds
     */
    public int getOverallTime(String collection) {
        FileConfiguration placedEggs = plugin.getEggDataManager().getPlacedEggs(collection);
        
        return placedEggs.getInt(RESET_PATH + "Year") * SECONDS_PER_YEAR +
               placedEggs.getInt(RESET_PATH + "Month") * SECONDS_PER_MONTH +
               placedEggs.getInt(RESET_PATH + "Day") * SECONDS_PER_DAY +
               placedEggs.getInt(RESET_PATH + "Hour") * SECONDS_PER_HOUR +
               placedEggs.getInt(RESET_PATH + "Minute") * SECONDS_PER_MINUTE +
               placedEggs.getInt(RESET_PATH + "Second");
    }

    /**
     * Converts total seconds to a formatted time string
     * 
     * @param collection The egg collection name
     * @return Formatted time string
     */
    public String getConvertedTime(String collection) {
        int duration = getOverallTime(collection);
        
        if (duration == 0) {
            return "§4§lNEVER";
        }
        
        TimeUnit timeUnit = new TimeUnit(duration);
        return timeUnit.toString();
    }
    
    /**
     * Helper class to handle time unit conversions
     */
    private static class TimeUnit {
        int years;
        int months;
        int days;
        int hours;
        int minutes;
        int seconds;
        
        TimeUnit(int totalSeconds) {
            // Extract years
            years = totalSeconds / SECONDS_PER_YEAR;
            totalSeconds -= years * SECONDS_PER_YEAR;
            
            // Extract months
            months = totalSeconds / SECONDS_PER_MONTH;
            totalSeconds -= months * SECONDS_PER_MONTH;
            
            // Extract days
            days = totalSeconds / SECONDS_PER_DAY;
            totalSeconds -= days * SECONDS_PER_DAY;
            
            // Extract hours
            hours = totalSeconds / SECONDS_PER_HOUR;
            totalSeconds -= hours * SECONDS_PER_HOUR;
            
            // Extract minutes
            minutes = totalSeconds / SECONDS_PER_MINUTE;
            totalSeconds -= minutes * SECONDS_PER_MINUTE;
            
            // Remaining seconds
            seconds = totalSeconds;
        }
        
        @Override
        public String toString() {
            return years + "Y " + months + "M " + days + "d " + 
                   hours + "h " + minutes + "m " + seconds + "s";
        }
    }

    /**
     * Removes all egg blocks from a collection based on deletion type
     * 
     * @param collection The egg collection name
     * @param uuid The player UUID for deletion type
     */
    public void removeAllEggBlocks(String collection, UUID uuid) {
        FileConfiguration placedEggs = plugin.getEggDataManager().getPlacedEggs(collection);
        
        if (!placedEggs.contains("PlacedEggs.")) {
            return;
        }
        
        DeletionTypes deletionType = plugin.getPlayerEggDataManager().getDeletionType(uuid);
        Set<String> eggIds = placedEggs.getConfigurationSection("PlacedEggs.").getKeys(false);
        
        for (String id : eggIds) {
            String basePath = "PlacedEggs." + id + ".";
            
            // Get egg location data
            int x = placedEggs.getInt(basePath + "X");
            int y = placedEggs.getInt(basePath + "Y");
            int z = placedEggs.getInt(basePath + "Z");
            String worldName = placedEggs.getString(basePath + "World");
            
            if (worldName == null) {
                continue;
            }
            
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                continue;
            }
            
            // Load the chunk
            int chunkX = x >> 4;
            int chunkZ = z >> 4;
            Chunk chunk = world.getChunkAt(chunkX, chunkZ);
            
            if (!chunk.load()) {
                continue;
            }
            
            // Process the block based on deletion type
            Location location = new Location(world, x, y, z);
            Block block = location.getBlock();
            
            if (shouldRemoveBlock(block, deletionType)) {
                block.setType(org.bukkit.Material.AIR);
                logBlockRemoval(x, y, z);
            }
        }
        
        // Update egg data
        plugin.getEggManager().spawnEggParticle();
        plugin.getEggDataManager().reload();
    }
    
    /**
     * Determines if a block should be removed based on deletion type
     * 
     * @param block The block to check
     * @param deletionType The deletion type
     * @return true if the block should be removed
     */
    private boolean shouldRemoveBlock(Block block, DeletionTypes deletionType) {
        switch (deletionType) {
            case Player_Heads:
                return block.getType().equals(XMaterial.PLAYER_HEAD.parseMaterial()) || 
                       block.getType().equals(XMaterial.PLAYER_WALL_HEAD.parseMaterial());
            case Everything:
                return true;
            default:
                return false;
        }
    }
    
    /**
     * Logs a block removal to console
     * 
     * @param x The x coordinate
     * @param y The y coordinate
     * @param z The z coordinate
     */
    private void logBlockRemoval(int x, int y, int z) {
        plugin.getMessageManager().sendMessage(
            Bukkit.getConsoleSender(), 
            MessageKey.SUCCESSFULLY_CHANGED_BLOCK,
            "%X%", String.valueOf(x), 
            "%Y%", String.valueOf(y), 
            "%Z%", String.valueOf(z)
        );
    }

    /**
     * Gets the count of active requirements for a specific type
     * 
     * @param requirementType The type of requirement
     * @param collection The egg collection name
     * @return Formatted string showing active/total count
     */
    public String getActives(Requirements requirementType, String collection) {
        FileConfiguration placedEggs = plugin.getEggDataManager().getPlacedEggs(collection);
        
        switch (requirementType) {
            case Hours:
                return countActiveRequirements(placedEggs, "Hours", 
                        IntStream.range(0, 24).mapToObj(String::valueOf).collect(Collectors.toList()), 24);
                
            case Date:
                return countActiveRequirements(placedEggs, "Date", 
                        DateTimeUtil.getAllDaysOfYear(), DateTimeUtil.getDaysInCurrentYear());
                
            case Weekday:
                return countActiveRequirements(placedEggs, "Weekday", 
                        DateTimeUtil.getWeekList(), 7);
                
            case Month:
                return countActiveRequirements(placedEggs, "Month", 
                        DateTimeUtil.getMonthList(), 12);
                
            case Year:
                int currentYear = DateTimeUtil.getCurrentYear();
                List<String> years = IntStream.range(currentYear, currentYear + 28)
                        .mapToObj(String::valueOf)
                        .collect(Collectors.toList());
                return countActiveRequirements(placedEggs, "Year", years, 28);
                
            case Season:
                return countActiveRequirements(placedEggs, "Season", 
                        DateTimeUtil.getSeasonList(), 4);
                
            default:
                return "§4ERROR";
        }
    }
    
    /**
     * Counts active requirements for a specific type
     * 
     * @param config The configuration containing requirements
     * @param requirementType The type of requirement
     * @param items The list of items to check
     * @param total The total number of items
     * @return Formatted string showing active/total count
     */
    private String countActiveRequirements(FileConfiguration config, String requirementType, 
                                          List<String> items, int total) {
        String basePath = REQUIREMENTS_PATH + requirementType + ".";
        
        long activeCount = items.stream()
                .filter(item -> config.getBoolean(basePath + item))
                .count();
                
        return activeCount + "/" + total;
    }

    /**
     * Gets the translated name for a requirement value
     * 
     * @param value The value to translate
     * @return The translated string
     */
    public String getRequirementsTranslation(String value) {
        // Map of values to message keys
        Map<String, MessageKey> translationMap = new HashMap<>();
        
        // Seasons
        translationMap.put("Winter", MessageKey.REQUIREMENTS_SEASON_WINTER);
        translationMap.put("Summer", MessageKey.REQUIREMENTS_SEASON_SUMMER);
        translationMap.put("Fall", MessageKey.REQUIREMENTS_SEASON_FALL);
        translationMap.put("Spring", MessageKey.REQUIREMENTS_SEASON_SPRING);
        
        // Days of week
        translationMap.put("Monday", MessageKey.DAY_MONDAY);
        translationMap.put("Tuesday", MessageKey.DAY_TUESDAY);
        translationMap.put("Wednesday", MessageKey.DAY_WEDNESDAY);
        translationMap.put("Thursday", MessageKey.DAY_THURSDAY);
        translationMap.put("Friday", MessageKey.DAY_FRIDAY);
        translationMap.put("Saturday", MessageKey.DAY_SATURDAY);
        translationMap.put("Sunday", MessageKey.DAY_SUNDAY);
        
        // Months
        translationMap.put("January", MessageKey.MONTH_JANUARY);
        translationMap.put("February", MessageKey.MONTH_FEBRUARY);
        translationMap.put("March", MessageKey.MONTH_MARCH);
        translationMap.put("April", MessageKey.MONTH_APRIL);
        translationMap.put("May", MessageKey.MONTH_MAY);
        translationMap.put("June", MessageKey.MONTH_JUNE);
        translationMap.put("July", MessageKey.MONTH_JULY);
        translationMap.put("August", MessageKey.MONTH_AUGUST);
        translationMap.put("September", MessageKey.MONTH_SEPTEMBER);
        translationMap.put("October", MessageKey.MONTH_OCTOBER);
        translationMap.put("November", MessageKey.MONTH_NOVEMBER);
        translationMap.put("December", MessageKey.MONTH_DECEMBER);
        
        // Get translation or return error message
        MessageKey key = translationMap.get(value);
        return key != null ? messageManager.getMessage(key) : "§4NO TRANSLATION FOUND";
    }
}
