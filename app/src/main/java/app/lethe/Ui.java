package app.lethe;

import android.content.Context;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** Мелкие помощники: dp, даты по-русски, склонения. */
final class Ui {

    private static final Locale RU = Locale.forLanguageTag("ru");

    private Ui() {
    }

    static int dp(Context c, float value) {
        return Math.round(value * c.getResources().getDisplayMetrics().density);
    }

    /** «Сегодня, 19:13», «Вчера, 08:41», «3 ноября, 22:05», «3 ноября 2025, 22:05». */
    static String formatWhen(long millis) {
        Calendar now = Calendar.getInstance();
        Calendar then = Calendar.getInstance();
        then.setTimeInMillis(millis);
        Date date = new Date(millis);
        String time = new SimpleDateFormat("HH:mm", RU).format(date);

        if (sameDay(now, then)) return "Сегодня, " + time;
        Calendar yesterday = (Calendar) now.clone();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);
        if (sameDay(yesterday, then)) return "Вчера, " + time;
        if (now.get(Calendar.YEAR) == then.get(Calendar.YEAR)) {
            return new SimpleDateFormat("d MMMM, HH:mm", RU).format(date);
        }
        return new SimpleDateFormat("d MMMM yyyy, HH:mm", RU).format(date);
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

    /** Русское склонение: plural(5, "слово", "слова", "слов") → «5 слов». */
    static String plural(int n, String one, String few, String many) {
        int mod100 = n % 100;
        int mod10 = n % 10;
        String word;
        if (mod100 >= 11 && mod100 <= 14) word = many;
        else if (mod10 == 1) word = one;
        else if (mod10 >= 2 && mod10 <= 4) word = few;
        else word = many;
        return n + " " + word;
    }

    static String words(int n) {
        return plural(n, "слово", "слова", "слов");
    }

    static String notes(int n) {
        return plural(n, "запись", "записи", "записей");
    }

    /** Убирает пробелы и переносы в конце текста. */
    static String trimEnd(String s) {
        int end = s.length();
        while (end > 0 && Character.isWhitespace(s.charAt(end - 1))) end--;
        return s.substring(0, end);
    }
}
