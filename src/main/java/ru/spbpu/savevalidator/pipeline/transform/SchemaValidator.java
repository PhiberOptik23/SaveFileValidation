package ru.spbpu.savevalidator.pipeline.transform;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import ru.spbpu.savevalidator.model.ValidationError;
import ru.spbpu.savevalidator.model.ValidationReport;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/**
 * Слой Transform (часть 1) ETL-пайплайна.
 * Валидирует JSON-данные файла сохранения против JSON Schema Draft-07.
 */
public class SchemaValidator {

    /**
     * Валидирует JSON-данные против указанной JSON-схемы.
     * Каждая ошибка схемы добавляется в отчёт как отдельная ValidationError.
     *
     * @param jsonData   распарсенные данные файла сохранения
     * @param schemaPath путь к файлу JSON Schema
     * @param report     отчёт валидации
     * @return true, если данные прошли валидацию; false при наличии ошибок
     */
    public boolean validate(JsonNode jsonData, Path schemaPath, ValidationReport report) {
        JsonSchema schema;
        try (InputStream schemaStream = Files.newInputStream(schemaPath)) {
            JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
            schema = factory.getSchema(schemaStream);
        } catch (IOException e) {
            report.addError(new ValidationError(
                    "$schema",
                    "Не удалось загрузить схему: " + e.getMessage(),
                    "SCHEMA_LOAD_ERROR",
                    ValidationError.Stage.TRANSFORM,
                    ValidationError.Severity.ERROR
            ));
            report.setSchemaValid(false);
            return false;
        }

        Set<ValidationMessage> errors = schema.validate(jsonData);

        if (errors.isEmpty()) {
            report.setSchemaValid(true);
            return true;
        }

        for (ValidationMessage msg : errors) {
            report.addError(new ValidationError(
                    msg.getPath(),
                    msg.getMessage(),
                    "SCHEMA_VIOLATION",
                    ValidationError.Stage.TRANSFORM,
                    ValidationError.Severity.ERROR
            ));
        }
        report.setSchemaValid(false);
        return false;
    }
}
