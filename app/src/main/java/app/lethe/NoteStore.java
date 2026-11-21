package app.lethe;

import android.content.Context;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Хранилище заметок: каждая заметка — обычный текстовый файл UTF-8
 * во внутренней памяти приложения (files/notes/<время создания>.txt).
 * Имя файла — момент создания в миллисекундах, он же показывается в списке.
 */
final class NoteStore {

    static final class Note {
        final long id;
        final long modified;
        final String text;

        Note(long id, long modified, String text) {
            this.id = id;
            this.modified = modified;
            this.text = text;
        }
    }

    private final File dir;

    NoteStore(Context context) {
        dir = new File(context.getFilesDir(), "notes");
        if (!dir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }
    }

    private File file(long id) {
        return new File(dir, id + ".txt");
    }

    boolean exists(long id) {
        return file(id).isFile();
    }

    long modified(long id) {
        return file(id).lastModified();
    }

    String read(long id) {
        File f = file(id);
        if (!f.isFile()) return "";
        try {
            return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    /** Атомарная запись: сначала во временный файл, потом переименование. */
    void write(long id, String text) {
        File target = file(id);
        File tmp = new File(dir, id + ".tmp");
        try {
            Files.write(tmp.toPath(), text.getBytes(StandardCharsets.UTF_8));
            if (!tmp.renameTo(target)) {
                Files.write(target.toPath(), text.getBytes(StandardCharsets.UTF_8));
                //noinspection ResultOfMethodCallIgnored
                tmp.delete();
            }
        } catch (IOException ignored) {
            // Нечего сделать разумного: место кончилось или память недоступна.
        }
    }

    void delete(long id) {
        //noinspection ResultOfMethodCallIgnored
        file(id).delete();
    }

    /** Все заметки, новые сверху. */
    List<Note> list() {
        List<Note> notes = new ArrayList<>();
        File[] files = dir.listFiles();
        if (files == null) return notes;
        for (File f : files) {
            String name = f.getName();
            if (!name.endsWith(".txt")) continue;
            long id;
            try {
                id = Long.parseLong(name.substring(0, name.length() - 4));
            } catch (NumberFormatException e) {
                continue;
            }
            notes.add(new Note(id, f.lastModified(), read(id)));
        }
        Collections.sort(notes, (a, b) -> Long.compare(b.id, a.id));
        return notes;
    }
}
