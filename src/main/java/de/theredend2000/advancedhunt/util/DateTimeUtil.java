package de.theredend2000.advancedhunt.util;

import de.theredend2000.advancedhunt.util.enums.Seasons;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public abstract class DateTimeUtil {

    public static final String ISO_DATE_FORMAT = "yyyy-MM-dd";

    public static final String CUSTOM_DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    // Neue Funktion: Anzahl der Tage im aktuellen Jahr
    public static int getDaysInCurrentYear() {
        Calendar calendar = Calendar.getInstance();
        return calendar.getActualMaximum(Calendar.DAY_OF_YEAR);
    }

    // Neue Funktion: Liste aller Tage im aktuellen Jahr als String im "yyyy-MM-dd" Format
    public static List<String> getAllDaysOfYear() {
        List<String> days = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        int totalDays = getDaysInCurrentYear();

        for (int i = 1; i <= totalDays; i++) {
            calendar.set(Calendar.DAY_OF_YEAR, i);
            days.add(dateToString(calendar.getTime(), ISO_DATE_FORMAT));
        }
        return days;
    }

    public static ArrayList<String> getDaysOfMonth(int month) {
        ArrayList<String> daysOfMonth = new ArrayList<>();

        // Schleife durch alle Tage des Jahres
        for (String day : getAllDaysOfYear()) {
            // Split den Tag nach dem "-" Zeichen (Format: yyyy-MM-dd)
            String[] parts = day.split("-");

            // Extrahiere den Monatsteil und überprüfe, ob er mit dem angegebenen Monat übereinstimmt
            int monthOfDay = Integer.parseInt(parts[1]);
            if (monthOfDay == month) {
                daysOfMonth.add(day);  // Füge nur die Tage des ausgewählten Monats hinzu
            }
        }

        return daysOfMonth;
    }

    public static long getDayRemain(Date currentDate, Date openDate){
        long interval = openDate.getTime() - currentDate.getTime();
        long day = interval/(24*3600*1000);
        return day;
    }

    public static long getHourRemain(Date currentDate, Date openDate){
        long interval = openDate.getTime() - currentDate.getTime();
        interval = interval%(24*3600*1000);
        long hour = interval/(3600*1000);
        return hour;
    }

    public static long getMinuteRemain(Date currentDate, Date openDate){
        long interval = openDate.getTime() - currentDate.getTime();
        interval = interval%(24*3600*1000)%(3600*1000);
        long minute = interval/(60*1000);
        return minute;
    }

    public static Date getNDaysAfterDate(Integer interval) {
        Calendar now = Calendar.getInstance();
        now.add(Calendar.DATE, interval);
        return now.getTime();
    }

    public static Date getNDaysAfterDay(Integer interval, Date fromDate) {
        Calendar now = Calendar.getInstance();
        now.setTime(fromDate);
        now.add(Calendar.DATE, interval);
        return now.getTime();
    }

    public static Date get30DaysAgoDate() {
        Calendar now = Calendar.getInstance();
        now.add(Calendar.DATE, -29);
        return now.getTime();
    }

    public static String dateToString(Date date, String pattern) {
        SimpleDateFormat format = new SimpleDateFormat(pattern);
        return format.format(date);
    }

    public static String dateToString(Calendar calendar, String pattern) {
        return dateToString(calendarToDate(calendar), pattern);
    }

    public static String getDateString(Date date) {
        if (date == null) {
            date = new Date();
        }
        return dateToString(date, ISO_DATE_FORMAT);
    }

    public static String getDateString(Calendar calendar) {
        if (calendar == null) {
            calendar = Calendar.getInstance();
        }
        return dateToString(calendar, ISO_DATE_FORMAT);
    }
    public static String getDateTimeString(Date date) {
        return dateToString(date, CUSTOM_DATETIME_FORMAT);
    }

    public static String getDateTimeString(Calendar calendar) {
        return getDateTimeString(calendar.getTime());
    }

    public static String getCurrentDateString() {
        return getDateString(new Date());
    }
    public static String getCurrentDateTimeString() {
        return getDateTimeString(new Date());
    }
    public static Date stringToDate(String date, String pattern) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat(pattern);
        return format.parse(date);
    }

    public static Calendar stringToCalendar(String date, String pattern) throws ParseException {
        return dateToCalendar(stringToDate(date, pattern));
    }

    public static Timestamp getTimestamp() {
        return new Timestamp(System.currentTimeMillis());
    }

    public static String timestampToString(Timestamp timestamp, String pattern) {
        return dateToString(timestamp, pattern);
    }

    public static String getWeek(Calendar calendar) {
        final String[] dayNames = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday" };
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        return dayNames[dayOfWeek - 1];
    }

    public static String getMonth(Calendar calendar) {
        final String[] dayNames = { "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December" };
        int dayOfWeek = calendar.get(Calendar.MONTH);
        return dayNames[dayOfWeek];
    }

    public static List<String> getWeekList() {
        final String[] dayNames = { "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday" };
        return new ArrayList<>(List.of(dayNames));
    }

    public static List<String> getMonthList() {
        final String[] dayNames = { "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December" };
        return new ArrayList<>(List.of(dayNames));
    }

    public static List<String> getSeasonList() {
        final String[] dayNames = { "Winter", "Spring", "Summer", "Fall"};
        return new ArrayList<>(List.of(dayNames));
    }

    public static String getWeek(Date date) {
        return getWeek(dateToCalendar(date));
    }

    public static Calendar dateToCalendar(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar;
    }

    public static Date calendarToDate(Calendar calendar) {
        return calendar.getTime();
    }

    public static int getDayOfYear(Calendar calendar) {
        return calendar.get(Calendar.DAY_OF_YEAR);
    }

    public static int getDayOfYear(Date date) {
        Calendar calendar = dateToCalendar(date);
        return calendar.get(Calendar.DAY_OF_YEAR);
    }

    public static int getYear(Calendar calendar) {
        return calendar.get(Calendar.YEAR);
    }

    public static int getCurrentYear() {
        return getYear(Calendar.getInstance());
    }

    public static int getNextYear() {
        return getCurrentYear() + 1;
    }

    public static int getLastYear() {
        return getCurrentYear() - 1;
    }

    public static boolean isLeapYear(Calendar calendar) {
        int year = getYear(calendar);
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);
    }

    public static Seasons getCurrentSeason(){
        Month currentMonth = LocalDate.now().getMonth();
        switch (currentMonth) {
            case DECEMBER:
            case JANUARY:
            case FEBRUARY:
                return Seasons.Winter;
            case MARCH:
            case APRIL:
            case MAY:
                return Seasons.Spring;
            case JUNE:
            case JULY:
            case AUGUST:
                return Seasons.Summer;
            case SEPTEMBER:
            case OCTOBER:
            case NOVEMBER:
                return Seasons.Fall;
            default:
                return Seasons.Unknown;
        }
    }
}