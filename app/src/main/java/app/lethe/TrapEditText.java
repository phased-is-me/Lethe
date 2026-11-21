package app.lethe;

import android.content.Context;
import android.text.Editable;
import android.util.AttributeSet;
import android.widget.EditText;

/**
 * Невидимое поле ввода для режима исчезающих слов.
 * Курсор всегда прижат к концу текста: даже если клавиатура
 * (например, свайп по пробелу на Samsung Keyboard) попытается
 * его сдвинуть, вернуться к уже исчезнувшим словам нельзя.
 */
public class TrapEditText extends EditText {

    private boolean ready;

    public TrapEditText(Context context) {
        super(context);
        ready = true;
    }

    public TrapEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        ready = true;
    }

    public TrapEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        ready = true;
    }

    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
        super.onSelectionChanged(selStart, selEnd);
        if (!ready) return;
        Editable text = getText();
        if (text == null) return;
        int len = text.length();
        if (selStart != len || selEnd != len) {
            setSelection(len);
        }
    }
}
