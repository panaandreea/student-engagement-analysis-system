package com.student.engagement.system.utils;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class FormUtils {

    public static void clearErrorOnTyping(TextInputEditText textInputEditText, TextInputLayout textInputLayout) {

        if (textInputEditText == null || textInputLayout == null) {
            return;
        }

        textInputEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {

            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (textInputLayout.getError() != null) {
                    textInputLayout.setError(null);
                }
            }
        });
    }

    public static TextInputLayout findParentLayout(View view) {

        while (view.getParent() instanceof View) {
            view = (View) view.getParent();

            if (view instanceof TextInputLayout) {
                return (TextInputLayout) view;
            }
        }

        return null;
    }

    public static void clearErrors(TextInputLayout... textInputLayouts) {

        for (TextInputLayout textInputLayout : textInputLayouts) {

            if (textInputLayout != null) {
                textInputLayout.setError(null);
            }
        }
    }

    public static void setError(TextInputLayout textInputLayout, TextInputEditText textInputEditText, String message) {

        if (textInputLayout != null) {
            textInputLayout.setError(message);
        }

        if (textInputEditText != null) {
            textInputEditText.requestFocus();
        }
    }

    public static String getText(TextInputEditText textInputEditText) {

        if (textInputEditText == null || textInputEditText.getText() == null) {
            return "";
        }

        return textInputEditText.getText().toString().trim();
    }
}