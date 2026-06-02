package com.student.engagement.system.utils;

import android.view.inputmethod.EditorInfo;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class ViewUtils {

    public static void resetButton(MaterialButton materialButton, String text) {

        if (materialButton != null) {
            materialButton.setEnabled(true);
            materialButton.setText(text);
        }
    }

    public static void setLoading(MaterialButton materialButton, String text) {

        if (materialButton != null) {
            materialButton.setEnabled(false);
            materialButton.setText(text);
        }
    }

    public static void onDone(TextInputEditText textInputEditText, Runnable action){

        if (textInputEditText == null || action == null) {
            return;
        }

        textInputEditText.setOnEditorActionListener((textView, actionId, keyEvent) -> {

            if (actionId == EditorInfo.IME_ACTION_DONE) {
                textView.clearFocus();
                action.run();
                return true;
            }

            return false;
        });
    }
}
