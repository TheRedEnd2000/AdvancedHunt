package de.theredend2000.advancedegghunt.util.saveinventory;

import de.theredend2000.advancedegghunt.Main;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;

public class DatetimeUtils {

    public final String[] TIME_UNITS = new String[]{"YEAR", "MONTH", "WEEK", "DAY", "HOUR", "MINUTE", "SECOND"};
    public final int[] SECONDS_IN_UNIT = new int[]{31536000, 2592000, 604800, 86400, 3600, 60};
    public DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(getZoneId());
    public DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(getZoneId());
    public DateTimeFormatter YEAR_FORMATTER = DateTimeFormatter.ofPattern("yyyy").withZone(getZoneId());


    public byte getCurrentDayOfMonth() {
        return (byte) Calendar.getInstance().get(5);
    }

    public String getRelativeDate(long secondsToAdd) {
        try {
            return ZonedDateTime.now().plusSeconds(secondsToAdd).format(DATE_FORMATTER);
        } catch (Exception e) {
            return "§cNOT FOUND";
        }
    }

    public String getNowDate() {
        try {
            return ZonedDateTime.now().format(DATE_FORMATTER);
        } catch (Exception e) {
            e.printStackTrace();
            return "§cNOT FOUND";
        }
    }

    public String getNowTime() {
        try {
            return ZonedDateTime.now().format(TIME_FORMATTER);
        } catch (Exception e) {
            e.printStackTrace();
            return "§cNOT FOUND";
        }
    }

    public String getNowYear() {
        try {
            return ZonedDateTime.now().format(YEAR_FORMATTER);
        } catch (Exception e) {
            e.printStackTrace();
            return "§cNOT FOUND";
        }
    }


    public long getSeconds(String date) {
        try {
            return ZonedDateTime.parse(date, DATE_FORMATTER).toEpochSecond();
        } catch (Exception e) {
            return -1L;
        }
    }

    public int[] getTimeValues(double seconds) {
        int[] values = new int[]{0, 0, 0, 0, 0, 0, 0};

        for(int unitIndex = 0; unitIndex <= 5; ++unitIndex) {
            int amountForUnit = (int)seconds / SECONDS_IN_UNIT[unitIndex];
            values[unitIndex] += amountForUnit;
            seconds -= (double)(amountForUnit * SECONDS_IN_UNIT[unitIndex]);
        }

        values[6] += (int)Math.round(seconds);
        return values;
    }

    public String convertToSentence(double seconds) {
        int[] values = getTimeValues(seconds);
        StringBuilder sentenceBuilder = new StringBuilder();

        int valueIndex;
        for(valueIndex = 0; valueIndex <= 6; ++valueIndex) {
            int value = values[valueIndex];
            if (value > 0) {
                String valueMessage = TIME_UNITS[valueIndex];
                if (value > 1) {
                    valueMessage = valueMessage + "S";
                }

                sentenceBuilder.append(value).append(" ").append("§cNOT FOUND").append(" ");
            }
        }

        valueIndex = sentenceBuilder.length();
        return valueIndex > 1 ? sentenceBuilder.deleteCharAt(valueIndex - 1).toString() : "0 §cNOT FOUND";
    }

    public String getTimeAgo(String date) {
        return convertToSentence((double)(-getSecondsBetweenNowAndDate(date)));
    }

    public long getSecondsBetweenNowAndDate(String date) {
        try {
            return Duration.between(ZonedDateTime.now(getZoneId()), ZonedDateTime.parse(date, DATE_FORMATTER)).getSeconds();
        } catch (Exception e) {
            return -1L;
        }
    }
    public ZoneId getZoneId() {
        String time = "default";
        if (!time.equals("default")) {
            try {
                return ZoneId.of(time);
            } catch (Exception e) {
            }
        }

        return ZoneId.systemDefault();
    }
}