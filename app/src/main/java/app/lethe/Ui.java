package app.lethe;

import android.content.Context;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** Small helpers: dp conversion, dates and word counts. */
final class Ui {

    private static final Locale EN = Locale.ENGLISH;

    private Ui() {
    }

    static int dp(Context c, float value) {
        return Math.round(value * c.getResources().getDisplayMetrics().density);
    }

    /** "Today, 19:13", "Yesterday, 08:41", "November 3, 22:05", "November 3, 2025, 22:05". */
    static String formatWhen(long millis) {
        Calendar now = Calendar.getInstance();
        Calendar then = Calendar.getInstance();
        then.setTimeInMillis(millis);
        Date date = new Date(millis);
        String time = new SimpleDateFormat("HH:mm", EN).format(date);

        if (sameDay(now, then)) return "Today, " + time;
        Calendar yesterday = (Calendar) now.clone();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);
        if (sameDay(yesterday, then)) return "Yesterday, " + time;
        if (now.get(Calendar.YEAR) == then.get(Calendar.YEAR)) {
            return new SimpleDateFormat("MMMM d, HH:mm", EN).format(date);
        }
        return new SimpleDateFormat("MMMM d, yyyy, HH:mm", EN).format(date);
    }

    private static boolean sameDay(Calendar a, Calendar b) {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }

    static int countWords(CharSequence s) {
        int count = 0;
        boolean inWord = false;
        for (int i = 0; i < s.length(); i++) {
            boolean space = Character.isWhitespace(s.charAt(i));
            if (!space && !inWord) count++;
            inWord = !space;
        }
        return count;
    }

    /** "1 word", "5 words". */
    static String words(int n) {
        return n + (n == 1 ? " word" : " words");
    }

    /** "1 note", "12 notes". */
    static String notes(int n) {
        return n + (n == 1 ? " note" : " notes");
    }

    /** Removes trailing spaces and line breaks. */
    static String trimEnd(String s) {
        int end = s.length();
        while (end > 0 && Character.isWhitespace(s.charAt(end - 1))) end--;
        return s.substring(0, end);
    }
}
