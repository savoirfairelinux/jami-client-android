package cx.ring.history;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class HistoryTimeModel {

    static ArrayList<String> timeCategories;

    public interface HistoryTimeCategoryModel {
        String TODAY = "Today"; // 0
        String YESTERDAY = "Yesterday"; // 1
        String TWO_DAYS = getDate(2, "MM/dd");// 2
        String THREE_DAYS = getDate(3, "MM/dd");// 3
        String FOUR_DAYS = getDate(4, "MM/dd");// 4
        String FIVE_DAYS = getDate(5, "MM/dd");// 5
        String SIX_DAYS = getDate(6, "MM/dd");// 6
        String LAST_WEEK = "Last week"; // 7
        String TWO_WEEKS = "Two weeks ago"; // 8
        String THREE_WEEKS = "Three weeks ago"; // 9
        String LAST_MONTH = "Last month"; // 10
        String TWO_MONTH = "Two months ago"; // 11
        String THREE_MONTH = "Three months ago"; // 12
        String FOUR_MONTH = "Four months ago"; // 13
        String FIVE_MONTH = "Five months ago"; // 14
        String SIX_MONTH = "Six months ago"; // 15
        String SEVEN_MONTH = "Seven months ago"; // 16
        String EIGHT_MONTH = "Eight months ago"; // 17
        String NINE_MONTH = "Nine months ago"; // 18
        String TEN_MONTH = "Ten months ago"; // 19
        String ELEVEN_MONTH = "Eleven months ago"; // 20
        String TWELVE_MONTH = "Twelve months ago"; // 21
        String LAST_YEAR = "Last year"; // 22
        String LONG_TIME_AGO = "Very long time ago"; // 23
        String NEVER = "Never"; // 24
    }

    private static final String TAG = HistoryManager.class.getSimpleName();

    static Calendar removeDays(int ago) {
        Calendar cal = Calendar.getInstance(Locale.getDefault());
        int currentDay = cal.get(Calendar.DAY_OF_MONTH);
        // Set the date to 2 days ago
        cal.set(Calendar.DAY_OF_MONTH, currentDay - ago);
        return cal;
    }

    static String getDate(int ago, String format) {
        Calendar cal = removeDays(ago);
        SimpleDateFormat objFormatter = new SimpleDateFormat(format, Locale.CANADA);
        objFormatter.setTimeZone(cal.getTimeZone());

        String result = objFormatter.format(cal.getTime());
        cal.clear();
        return result;
    }

    public static String timeToHistoryConst(long time) {

        if(timeCategories == null){
            initializeCategories();
        }

        long time2 = time;
        long currentTime = Calendar.getInstance(Locale.getDefault()).getTime().getTime() / 1000; // in seconds

        if (time < 0)
            return HistoryTimeCategoryModel.NEVER;

        // Check if part if the current Nychthemeron
        if (currentTime - time <= 3600 * 24) // The future case would be a bug, but it have to be handled anyway or it will appear in
            // "very long time ago"
            return HistoryTimeCategoryModel.TODAY;

        time2 -= time % (3600 * 24); // Reset to midnight
        currentTime -= currentTime % (3600 * 24); // Reset to midnight
        // Check for last week
        if (currentTime - (6) * 3600 * 24 < time2) {
            for (int i = 1; i < 7; i++) {
                if (currentTime - ((i) * 3600 * 24) == time2)
                    return timeCategories.get(i); // Yesterday to Six_days_ago
            }
        }
        // Check for last month
        else if (currentTime - ((4) * 7 * 24 * 3600) < time2) {
            for (int i = 1; i < 4; i++) {
                if (currentTime - ((i + 1) * 7 * 24 * 3600) < time2)
                    return timeCategories.get(i + timeCategories.indexOf(HistoryTimeCategoryModel.LAST_WEEK) - 1); // Last_week to Three_weeks_ago
            }
        }
        // Check for last year
        else if (currentTime - (12) * 30.4f * 24 * 3600 < time2) {
            for (int i = 1; i < 12; i++) {
                if (currentTime - (i + 1) * 30.4f * 24 * 3600 < time2) // Not exact, but faster
                    return timeCategories.get(i + timeCategories.indexOf(HistoryTimeCategoryModel.LAST_MONTH) - 1);
                // Last_month to Twelve_months ago
            }
        }
        // if (QDate::currentDate().addYears(-1) >= date && QDate::currentDate().addYears(-2) < date)
        else if (currentTime - 365 * 24 * 3600 < time2)
            return HistoryTimeCategoryModel.LAST_YEAR;

        // Every other senario
        return HistoryTimeCategoryModel.LONG_TIME_AGO;
    }

    private static void initializeCategories() {
        timeCategories = new ArrayList<String>();
        timeCategories.add(HistoryTimeCategoryModel.TODAY);
        timeCategories.add(HistoryTimeCategoryModel.YESTERDAY);
        timeCategories.add(HistoryTimeCategoryModel.TWO_DAYS);
        timeCategories.add(HistoryTimeCategoryModel.THREE_DAYS);
        timeCategories.add(HistoryTimeCategoryModel.FOUR_DAYS);
        timeCategories.add(HistoryTimeCategoryModel.FIVE_DAYS);
        timeCategories.add(HistoryTimeCategoryModel.SIX_DAYS);
        timeCategories.add(HistoryTimeCategoryModel.LAST_WEEK);
        timeCategories.add(HistoryTimeCategoryModel.TWO_WEEKS);
        timeCategories.add(HistoryTimeCategoryModel.THREE_WEEKS);
        timeCategories.add(HistoryTimeCategoryModel.LAST_MONTH);
        timeCategories.add(HistoryTimeCategoryModel.TWO_MONTH);
        timeCategories.add(HistoryTimeCategoryModel.THREE_MONTH);
        timeCategories.add(HistoryTimeCategoryModel.FOUR_MONTH);
        timeCategories.add(HistoryTimeCategoryModel.FIVE_MONTH);
        timeCategories.add(HistoryTimeCategoryModel.SIX_MONTH);
        timeCategories.add(HistoryTimeCategoryModel.SEVEN_MONTH);
        timeCategories.add(HistoryTimeCategoryModel.EIGHT_MONTH);
        timeCategories.add(HistoryTimeCategoryModel.NINE_MONTH);
        timeCategories.add(HistoryTimeCategoryModel.TEN_MONTH);
        timeCategories.add(HistoryTimeCategoryModel.ELEVEN_MONTH);
        timeCategories.add(HistoryTimeCategoryModel.TWELVE_MONTH);
        timeCategories.add(HistoryTimeCategoryModel.LAST_YEAR);
        timeCategories.add(HistoryTimeCategoryModel.LONG_TIME_AGO);
        timeCategories.add(HistoryTimeCategoryModel.NEVER);
    }
}
