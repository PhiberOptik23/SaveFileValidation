package ru.spbpu.savevalidator.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * POJO результата валидации файла сохранения.
 * Содержит итоговый статус, список ошибок, хеш-суммы и метаданные.
 */
public class ValidationReport {

    /** Итоговый статус валидации */
    public enum Status {
        PASS, FAIL
    }

    @JsonProperty("file_name")
    private String fileName;

    @JsonProperty("status")
    private Status status;

    @JsonProperty("schema_valid")
    private boolean schemaValid;

    @JsonProperty("integrity_valid")
    private boolean integrityValid;

    @JsonProperty("computed_hash")
    private String computedHash;

    @JsonProperty("expected_hash")
    private String expectedHash;

    @JsonProperty("errors")
    private List<ValidationError> errors = new ArrayList<>();

    @JsonProperty("timestamp")
    private String timestamp;

    /** Конструктор по умолчанию. Устанавливает timestamp на текущий момент. */
    public ValidationReport() {
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        this.status = Status.PASS;
        this.schemaValid = true;
        this.integrityValid = true;
    }

    /**
     * Добавляет ошибку в отчёт и обновляет итоговый статус.
     *
     * @param error ошибка валидации
     */
    public void addError(ValidationError error) {
        this.errors.add(error);
        if (error.getSeverity() == ValidationError.Severity.ERROR) {
            this.status = Status.FAIL;
        }
    }

    /** Устанавливает результат проверки схемы. */
    public void setSchemaValid(boolean schemaValid) {
        this.schemaValid = schemaValid;
        if (!schemaValid) {
            this.status = Status.FAIL;
        }
    }

    /** Устанавливает результат проверки целостности. */
    public void setIntegrityValid(boolean integrityValid) {
        this.integrityValid = integrityValid;
        if (!integrityValid) {
            this.status = Status.FAIL;
        }
    }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public boolean isSchemaValid() { return schemaValid; }
    public boolean isIntegrityValid() { return integrityValid; }
    public String getComputedHash() { return computedHash; }
    public void setComputedHash(String computedHash) { this.computedHash = computedHash; }
    public String getExpectedHash() { return expectedHash; }
    public void setExpectedHash(String expectedHash) { this.expectedHash = expectedHash; }
    public List<ValidationError> getErrors() { return errors; }
    public void setErrors(List<ValidationError> errors) { this.errors = errors; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}
