package ru.spbpu.savevalidator.pipeline;

import com.fasterxml.jackson.databind.JsonNode;
import ru.spbpu.savevalidator.model.ValidationReport;
import ru.spbpu.savevalidator.pipeline.extract.SaveFileExtractor;
import ru.spbpu.savevalidator.pipeline.load.ReportWriter;
import ru.spbpu.savevalidator.pipeline.transform.IntegrityChecker;
import ru.spbpu.savevalidator.pipeline.transform.SchemaValidator;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Оркестратор ETL-пайплайна валидации файлов сохранений.
 * Последовательно запускает этапы: Extract -> Transform -> Load.
 */
public class PipelineOrchestrator {

    private final SaveFileExtractor extractor;
    private final SchemaValidator schemaValidator;
    private final IntegrityChecker integrityChecker;
    private final ReportWriter reportWriter;

    /** Создаёт оркестратор со стандартными компонентами. */
    public PipelineOrchestrator() {
        this.extractor = new SaveFileExtractor();
        this.schemaValidator = new SchemaValidator();
        this.integrityChecker = new IntegrityChecker();
        this.reportWriter = new ReportWriter();
    }

    /**
     * Создаёт оркестратор с пользовательскими компонентами (для тестирования).
     *
     * @param extractor        слой Extract
     * @param schemaValidator  валидатор схемы
     * @param integrityChecker проверка целостности
     * @param reportWriter     слой Load
     */
    public PipelineOrchestrator(SaveFileExtractor extractor,
                                SchemaValidator schemaValidator,
                                IntegrityChecker integrityChecker,
                                ReportWriter reportWriter) {
        this.extractor = extractor;
        this.schemaValidator = schemaValidator;
        this.integrityChecker = integrityChecker;
        this.reportWriter = reportWriter;
    }

    /**
     * Запускает полный ETL-пайплайн валидации.
     *
     * @param savePath     путь к файлу сохранения
     * @param schemaPath   путь к JSON Schema
     * @param checksumPath путь к файлу .sha256 с эталонным хешем
     * @param outputPath   путь для записи отчёта (null — вывод на консоль)
     * @return отчёт валидации
     */
    public ValidationReport run(Path savePath, Path schemaPath,
                                Path checksumPath, Path outputPath) {
        ValidationReport report = new ValidationReport();

        // === EXTRACT ===
        System.out.println("[Extract] Чтение файла: " + savePath);
        JsonNode jsonData = extractor.extract(savePath, report);

        if (jsonData != null) {
            System.out.println("[Extract] SHA-256: " + report.getComputedHash());

            // === TRANSFORM: Schema Validation ===
            System.out.println("[Transform] Валидация схемы...");
            schemaValidator.validate(jsonData, schemaPath, report);
            System.out.println("[Transform] Схема: " + (report.isSchemaValid() ? "OK" : "FAIL"));

            // === TRANSFORM: Integrity Check ===
            if (checksumPath != null) {
                System.out.println("[Transform] Проверка целостности...");
                integrityChecker.check(checksumPath, report);
                System.out.println("[Transform] Целостность: " +
                        (report.isIntegrityValid() ? "OK" : "FAIL"));
            }
        }

        // === LOAD ===
        System.out.println("[Load] Итоговый статус: " + report.getStatus());
        if (outputPath != null) {
            try {
                String fileName = outputPath.getFileName().toString();
                if (fileName.endsWith(".csv")) {
                    reportWriter.writeCsv(report, outputPath);
                } else {
                    reportWriter.writeJson(report, outputPath);
                }
                System.out.println("[Load] Отчёт записан: " + outputPath);
            } catch (IOException e) {
                System.err.println("[Load] Ошибка записи отчёта: " + e.getMessage());
            }
        } else {
            try {
                System.out.println(reportWriter.toJsonString(report));
            } catch (IOException e) {
                System.err.println("[Load] Ошибка сериализации: " + e.getMessage());
            }
        }

        return report;
    }
}
