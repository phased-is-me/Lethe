package app.lethe;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Insets;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

/**
 * Режим исчезающих слов.
 *
 * Весь текст копится в невидимом поле ввода. На экране видно только
 * слово, которое печатается сейчас. Как только после слова появляется
 * пробел (или перенос строки), слово плавно растворяется и больше не
 * редактируется. Кнопка «Готово» (или жест «назад») сохраняет запись
 * и открывает её целиком как обычную заметку.
 */
public class WriteActivity extends Activity {

    static final String EXTRA_ID = "id";
    static final String EXTRA_FROM_NOTE = "from_note";

    private static final long SAVE_DELAY_MS = 700;
    private static final long DISSOLVE_MS = 750;

    private NoteStore store;
    private long noteId;
    private boolean fromNote;

    private TrapEditText input;
    private FrameLayout stage;
    private TextView current;
    private TextView counter;

    /** Граница «запечатанного» текста: всё до неё уже исчезло и защищено от правок. */
    private int sealed;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable saveTask = () -> save();
    private final OnBackInvokedCallback backCallback = () -> finishWriting();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setDecorFitsSystemWindows(false);
        setContentView(R.layout.activity_write);
        setRecentsScreenshotEnabled(false);

        store = new NoteStore(this);
        fromNote = getIntent().getBooleanExtra(EXTRA_FROM_NOTE, false);
        long id = savedInstanceState != null
                ? savedInstanceState.getLong(EXTRA_ID, -1)
                : getIntent().getLongExtra(EXTRA_ID, -1);
        noteId = id > 0 ? id : System.currentTimeMillis();

        input = findViewById(R.id.input);
        stage = findViewById(R.id.stage);
        current = findViewById(R.id.current);
        counter = findViewById(R.id.counter);

        // Если продолжаем существующую заметку — дописываем с новой строки.
        String existing = store.exists(noteId) ? store.read(noteId) : "";
        String prefix = existing;
        if (!prefix.isEmpty() && !Character.isWhitespace(prefix.charAt(prefix.length() - 1))) {
            prefix = prefix + "\n";
        }
        input.setText(prefix);
        sealed = prefix.length();
        input.setSelection(sealed);
        if (!existing.isEmpty()) current.setHint(null);

        input.setFilters(new InputFilter[]{this::guard});
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                onInputChanged(s);
            }
        });
        updateCounter(prefix);

        findViewById(R.id.done).setOnClickListener(v -> finishWriting());
        stage.setOnClickListener(v -> showKeyboard());

        // Экран делится на полотно и клавиатуру: снизу отступ ровно под клавиатуру.
        findViewById(R.id.root).setOnApplyWindowInsetsListener((v, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            Insets ime = insets.getInsets(WindowInsets.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, Math.max(bars.bottom, ime.bottom));
            return WindowInsets.CONSUMED;
        });

        getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT, backCallback);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) showKeyboard();
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(saveTask);
        save();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(EXTRA_ID, noteId);
    }

    @Override
    protected void onDestroy() {
        getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(backCallback);
        handler.removeCallbacks(saveTask);
        super.onDestroy();
    }

    // ---------------------------------------------------------------- ввод

    /**
     * Защита исчезнувших слов: клавиатура не может их удалить или изменить.
     * Разрешены только правки после границы {@link #sealed}.
     */
    private CharSequence guard(CharSequence source, int start, int end,
                               Spanned dest, int dstart, int dend) {
        if (dstart >= sealed) return null;                 // правка в текущем слове — можно
        if (dstart == dend) return "";                     // вставка внутрь прошлого текста — нельзя
        int protEnd = Math.min(dend, sealed);
        String prot = dest.subSequence(dstart, protEnd).toString();
        String src = source.subSequence(start, end).toString();
        if (src.startsWith(prot)) return null;             // клавиатура переподтверждает то же слово
        if (dend <= sealed) return prot;                   // удаление/замена прошлого — отклоняем
        return prot + src;                                 // прошлое оставляем, новое дописываем
    }

    private void onInputChanged(Editable s) {
        String full = s.toString();
        int n = full.length();
        if (sealed > n) sealed = n;

        String lastSealedWord = null;
        String partial;
        int i = sealed;
        while (true) {
            int ws = i;
            while (ws < n && Character.isWhitespace(full.charAt(ws))) ws++;
            int we = ws;
            while (we < n && !Character.isWhitespace(full.charAt(we))) we++;
            if (we > ws && we < n) {
                // Слово закончилось пробелом/переносом → запечатываем.
                lastSealedWord = full.substring(ws, we);
                sealed = we;
                i = we;
            } else {
                partial = full.substring(ws, we);
                break;
            }
        }

        // Если вставили сразу много слов — анимируем только последнее.
        if (lastSealedWord != null) dissolve(lastSealedWord);
        current.setText(partial);
        if (!partial.isEmpty()) current.setHint(null);

        updateCounter(full);
        handler.removeCallbacks(saveTask);
        handler.postDelayed(saveTask, SAVE_DELAY_MS);
    }

    /** Плавное исчезновение слова: прозрачность, лёгкий подъём и размытие. */
    private void dissolve(String word) {
        TextView ghost = new TextView(this);
        ghost.setLayoutParams(new FrameLayout.LayoutParams(
                (FrameLayout.LayoutParams) current.getLayoutParams()));
        ghost.setTypeface(current.getTypeface());
        ghost.setTextSize(TypedValue.COMPLEX_UNIT_PX, current.getTextSize());
        ghost.setTextColor(current.getCurrentTextColor());
        ghost.setGravity(current.getGravity());
        ghost.setMaxLines(current.getMaxLines());
        ghost.setPadding(current.getPaddingLeft(), current.getPaddingTop(),
                current.getPaddingRight(), current.getPaddingBottom());
        ghost.setText(word);
        stage.addView(ghost, stage.indexOfChild(current));

        float lift = Ui.dp(this, 18);
        ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(DISSOLVE_MS);
        anim.setInterpolator(new DecelerateInterpolator(1.3f));
        anim.addUpdateListener(a -> {
            float f = (float) a.getAnimatedValue();
            ghost.setAlpha(1f - f);
            ghost.setTranslationY(-lift * f);
            float r = 0.5f + 18f * f;
            ghost.setRenderEffect(RenderEffect.createBlurEffect(r, r, Shader.TileMode.DECAL));
        });
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                stage.removeView(ghost);
            }
        });
        anim.start();

        // Новое слово появляется мягко.
        current.animate().cancel();
        current.setAlpha(0f);
        current.animate().alpha(1f).setStartDelay(80).setDuration(220).start();
    }

    private void updateCounter(CharSequence text) {
        counter.setText(Ui.words(Ui.countWords(text)));
    }

    private void showKeyboard() {
        input.requestFocus();
        WindowInsetsController controller = getWindow().getInsetsController();
        if (controller != null) controller.show(WindowInsets.Type.ime());
        InputMethodManager imm = getSystemService(InputMethodManager.class);
        if (imm != null) imm.showSoftInput(input, 0);
    }

    // ------------------------------------------------------- сохранение/выход

    /** Сохраняет запись. Возвращает false, если она пустая (пустые не храним). */
    private boolean save() {
        String text = Ui.trimEnd(input.getText().toString());
        if (text.trim().isEmpty()) {
            if (store.exists(noteId)) store.delete(noteId);
            return false;
        }
        store.write(noteId, text);
        return true;
    }

    private void finishWriting() {
        handler.removeCallbacks(saveTask);
        boolean hasText = save();
        if (hasText && !fromNote) {
            startActivity(new Intent(this, NoteActivity.class)
                    .putExtra(NoteActivity.EXTRA_ID, noteId));
        }
        finish();
    }
}
