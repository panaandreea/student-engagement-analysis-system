package com.student.engagement.system.validation;

public class FormValidation<T extends FormField> {

    public final boolean isValid;

    public final T field;

    public final String message;

    private FormValidation(boolean isValid, T field, String message) {
        this.isValid = isValid;
        this.field = field;
        this.message = message;
    }

    public static <T extends FormField> FormValidation<T> valid(){
        return new FormValidation<>(true, null, null);
    }

    public static <T extends FormField> FormValidation<T> error(T field, String message){
        return new FormValidation<>(false, field, message);
    }
}