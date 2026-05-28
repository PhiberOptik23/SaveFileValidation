package ru.spbpu.savevalidator.pipeline.load;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import ru.spbpu.savevalidator.model.ValidationReport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Слой Load ETL-пайплайна.
 * Записывает отчёт валидации в файл формата JSON или выводит на консоль.
 */
public class ReportWriter {

    private final ObjectMapper objectMapper;

    /** Создаёт ReportWriter с форматированным JSON-выводом. */
    public ReportWriter() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * Записывает отчёт валидации в JSON-файл.
     *
     * @param report     отчёт валидации
     * @param outputPath путь для сохранения файла отчёта
     * @throws IOException при ошибке записи
     */
    public void writeJson(ValidationReport report, Path outputPath) throws IOException {
        // Создаём родительские директории при необходимости
        Path parent = outputPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        objectMapper.writeValue(outputPath.toFile(), report);
    }

    /**
     * Сериализует отчёт валидации в строку JSON.
     *
     * @param report отчёт валидации
     * @return строковое представление JSON
     * @throws IOException при ошибке сериализации
     */
    public String toJsonString(ValidationReport report) throws IOException {
        return objectMapper.writeValueAsString(report);
    }

    /**
     * Записывает отчёт валидации в формате CSV.
     *
     * @param report     отчёт валидации
     * @param outputPath путь для сохранения CSV-файла
     * @throws IOException при ошибке записи
     */
    public void writeCsv(ValidationReport report, Path outputPath) throws IOException {
        Path parent = outputPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("file_name,status,schema_valid,integrity_valid,computed_hash,expected_hash,timestamp\n");
        sb.append(String.format("%s,%s,%s,%s,%s,%s,%s\n",
                report.getFileName(),
                report.getStatus(),
                report.isSchemaValid(),
                report.isIntegrityValid(),
                report.getComputedHash(),
                report.getExpectedHash(),
                report.getTimestamp()));

        if (!report.getErrors().isEmpty()) {
            sb.append("\nerror_field,error_type,stage,severity,message\n");
            report.getErrors().forEach(e ->
                    sb.append(String.format("%s,%s,%s,%s,%s\n",
                            e.getFieldPath(), e.getErrorType(),
                            e.getStage(), e.getSeverity(),
                            e.getMessage().replace(",", ";"))));
        }

        Files.writeString(outputPath, sb.toString());
    }
}
