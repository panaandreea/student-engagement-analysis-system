package com.student.engagement.system.utils;

public class ValidationResult <T extends Field> {
    public final boolean isValid;
    public final T field;
    public final String message;

    private ValidationResult(boolean isValid, T field, String message) {
        this.isValid = isValid;
        this.field = field;
        this.message = message;
    }

    public static <T extends  Field> ValidationResult<T> valid(){
        return new ValidationResult<>(true, null, null);
    }

    public static <T extends Field> ValidationResult<T> error(T field, String message){
        return new ValidationResult<>(false, field, message);
    }
}