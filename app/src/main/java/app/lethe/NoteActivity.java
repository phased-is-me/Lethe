package app.lethe;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Insets;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.WindowInsets;
import android.widget.EditText;
import android.widget.TextView;

/** Обычная заметка: весь текст целиком, можно править, делиться, удалять. */
public class NoteActivity extends Activity {

    static final String EXTRA_ID = "id";

    private NoteStore store;
    private long noteId;
    private EditText editor;
    private TextView meta;

    private boolean loading;
    private boolean dirty;
    private boolean deleted;
    private long loadedModified = -1;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable saveTask = () -> save();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setDecorFitsSystemWindows(false);
        setContentView(R.layout.activity_note);
        setRecentsScreenshotEnabled(false);

        store = new NoteStore(this);
        noteId = getIntent().getLongExtra(EXTRA_ID, -1);
        if (noteId <= 0) {
            finish();
            return;
        }

        editor = findViewById(R.id.editor);
        meta = findViewById(R.id.meta);

        editor.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (loading) return;
                dirty = true;
                updateMeta();
                handler.removeCallbacks(saveTask);
                handler.postDelayed(saveTask, 800);
            }
        });

        findViewById(R.id.back).setOnClickListener(v -> finish());
        findViewById(R.id.hide).setOnClickListener(v -> {
            save();
            startActivity(new Intent(this, WriteActivity.class)
                    .putExtra(WriteActivity.EXTRA_ID, noteId)
                    .putExtra(WriteActivity.EXTRA_FROM_NOTE, true));
        });
        findViewById(R.id.share).setOnClickListener(v -> share());
        findViewById(R.id.delete).setOnClickListener(v -> confirmDelete());

        findViewById(R.id.root).setOnApplyWindowInsetsListener((v, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            Insets ime = insets.getInsets(WindowInsets.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, Math.max(bars.bottom, ime.bottom));
            return WindowInsets.CONSUMED;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!store.exists(noteId)) {
            finish();
            return;
        }
        // Перечитываем, только если файл изменился (например, после «Продолжить скрытно»).
        long modified = store.modified(noteId);
        if (modified != loadedModified) {
            loading = true;
            editor.setText(store.read(noteId));
            loading = false;
            dirty = false;
            loadedModified = modified;
        }
        updateMeta();
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(saveTask);
        save();
    }

    private void save() {
        if (!dirty || deleted) return;
        String text = editor.getText().toString();
        if (text.trim().isEmpty()) {
            store.delete(noteId);
        } else {
            store.write(noteId, text);
            loadedModified = store.modified(noteId);
        }
        dirty = false;
    }

    private void updateMeta() {
        meta.setText(Ui.formatWhen(noteId) + "  ·  " + Ui.words(Ui.countWords(editor.getText())));
    }

    private void share() {
        save();
        Intent send = new Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, editor.getText().toString());
        startActivity(Intent.createChooser(send, getString(R.string.share)));
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_title)
                .setMessage(R.string.delete_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    deleted = true;
                    handler.removeCallbacks(saveTask);
                    store.delete(noteId);
                    finish();
                })
                .show();
    }
}
