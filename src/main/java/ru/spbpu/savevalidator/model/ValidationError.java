package ru.spbpu.savevalidator.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * POJO, представляющий одну ошибку валидации.
 * Содержит путь к полю, сообщение об ошибке, тип ошибки и уровень серьёзности.
 */
public class ValidationError {

    /** Этап ETL, на котором обнаружена ошибка */
    public enum Stage {
        EXTRACT, TRANSFORM, LOAD
    }

    /** Уровень серьёзности ошибки */
    public enum Severity {
        ERROR, WARNING
    }

    @JsonProperty("field_path")
    private String fieldPath;

    @JsonProperty("message")
    private String message;

    @JsonProperty("error_type")
    private String errorType;

    @JsonProperty("stage")
    private Stage stage;

    @JsonProperty("severity")
    private Severity severity;

    /** Конструктор по умолчанию для Jackson. */
    public ValidationError() {
    }

    /**
     * Создаёт ошибку валидации.
     *
     * @param fieldPath путь к полю, вызвавшему ошибку (например, "$.player.level")
     * @param message   описание ошибки
     * @param errorType тип ошибки (например, "SCHEMA_VIOLATION", "INTEGRITY_FAILURE")
     * @param stage     этап ETL
     * @param severity  уровень серьёзности
     */
    public ValidationError(String fieldPath, String message, String errorType,
                           Stage stage, Severity severity) {
        this.fieldPath = fieldPath;
        this.message = message;
        this.errorType = errorType;
        this.stage = stage;
        this.severity = severity;
    }

    public String getFieldPath() { return fieldPath; }
    public String getMessage() { return message; }
    public String getErrorType() { return errorType; }
    public Stage getStage() { return stage; }
    public Severity getSeverity() { return severity; }

    @Override
    public String toString() {
        return String.format("[%s/%s] %s: %s (%s)", stage, severity, errorType, message, fieldPath);
    }
}
