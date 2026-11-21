package app.lethe;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Insets;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/** Главный экран: список всех заметок с датой и временем. */
public class MainActivity extends Activity {

    private NoteStore store;
    private LinearLayout list;
    private TextView subtitle;
    private TextView empty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setDecorFitsSystemWindows(false);
        setContentView(R.layout.activity_main);
        // Не показывать содержимое в превью «Недавних приложений».
        setRecentsScreenshotEnabled(false);

        store = new NoteStore(this);
        list = findViewById(R.id.list);
        subtitle = findViewById(R.id.subtitle);
        empty = findViewById(R.id.empty);

        View fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> startActivity(new Intent(this, WriteActivity.class)));

        View root = findViewById(R.id.root);
        View scroll = findViewById(R.id.scroll);
        View scrim = findViewById(R.id.scrim);
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            scroll.setPadding(bars.left, bars.top, bars.right, bars.bottom + Ui.dp(this, 104));

            ViewGroup.LayoutParams scrimLp = scrim.getLayoutParams();
            scrimLp.height = bars.top + Ui.dp(this, 12);
            scrim.setLayoutParams(scrimLp);

            ViewGroup.MarginLayoutParams fabLp = (ViewGroup.MarginLayoutParams) fab.getLayoutParams();
            fabLp.bottomMargin = bars.bottom + Ui.dp(this, 20);
            fab.setLayoutParams(fabLp);
            return WindowInsets.CONSUMED;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        List<NoteStore.Note> notes = store.list();
        list.removeAllViews();
        subtitle.setText(Ui.notes(notes.size()));
        empty.setVisibility(notes.isEmpty() ? View.VISIBLE : View.GONE);

        LayoutInflater inflater = getLayoutInflater();
        for (NoteStore.Note note : notes) {
            View item = inflater.inflate(R.layout.item_note, list, false);
            TextView preview = item.findViewById(R.id.preview);
            TextView meta = item.findViewById(R.id.meta);

            String text = note.text.trim().replaceAll("\\s+", " ");
            if (text.length() > 240) text = text.substring(0, 240);
            preview.setText(text);
            meta.setText(Ui.formatWhen(note.id) + "  ·  " + Ui.words(Ui.countWords(note.text)));

            item.setOnClickListener(v -> startActivity(
                    new Intent(this, NoteActivity.class).putExtra(NoteActivity.EXTRA_ID, note.id)));
            item.setOnLongClickListener(v -> {
                confirmDelete(note.id);
                return true;
            });
            list.addView(item);
        }
    }

    private void confirmDelete(long id) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_title)
                .setMessage(R.string.delete_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    store.delete(id);
                    refresh();
                })
                .show();
    }
}
