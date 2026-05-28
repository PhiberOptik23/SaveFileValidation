package ru.spbpu.savevalidator;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.spbpu.savevalidator.model.ValidationReport;
import ru.spbpu.savevalidator.pipeline.PipelineOrchestrator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Интеграционные тесты для полного ETL-пайплайна.
 * Запускают PipelineOrchestrator на реальных файлах из каталога samples/.
 */
class PipelineIntegrationTest {

    private PipelineOrchestrator orchestrator;
    private Path schemaPath;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        orchestrator = new PipelineOrchestrator();
        schemaPath = Paths.get("schemas", "save_schema_v1.json");
    }

    @Test
    @DisplayName("Полный пайплайн: валидный файл -> PASS")
    void testValidFilePipeline() {
        Path savePath = Paths.get("samples", "test_save_valid.json");
        Path checksumPath = Paths.get("samples", "test_save_valid.json.sha256");
        Path outputPath = tempDir.resolve("report_valid.json");

        ValidationReport report = orchestrator.run(savePath, schemaPath, checksumPath, outputPath);

        assertEquals(ValidationReport.Status.PASS, report.getStatus());
        assertTrue(report.isSchemaValid());
        assertTrue(report.isIntegrityValid());
        assertTrue(report.getErrors().isEmpty());
        assertTrue(Files.exists(outputPath), "Отчёт должен быть записан в файл");
    }

    @Test
    @DisplayName("Полный пайплайн: файл с нарушением схемы -> FAIL")
    void testCorruptSchemaPipeline() {
        Path savePath = Paths.get("samples", "test_save_corrupt_schema.json");
        Path outputPath = tempDir.resolve("report_corrupt_schema.json");

        // Без checksum (проверяем только схему)
        ValidationReport report = orchestrator.run(savePath, schemaPath, null, outputPath);

        assertEquals(ValidationReport.Status.FAIL, report.getStatus());
        assertFalse(report.isSchemaValid());
        assertFalse(report.getErrors().isEmpty(), "Должны быть ошибки схемы");
    }

    @Test
    @DisplayName("Полный пайплайн: файл с неверным хешем -> FAIL")
    void testCorruptHashPipeline() {
        Path savePath = Paths.get("samples", "test_save_corrupt_hash.json");
        Path checksumPath = Paths.get("samples", "test_save_corrupt_hash.json.sha256");
        Path outputPath = tempDir.resolve("report_corrupt_hash.json");

        ValidationReport report = orchestrator.run(savePath, schemaPath, checksumPath, outputPath);

        assertEquals(ValidationReport.Status.FAIL, report.getStatus());
        assertTrue(report.isSchemaValid(), "Схема валидного файла должна проходить");
        assertFalse(report.isIntegrityValid(), "Целостность должна не пройти");
    }

    @Test
    @DisplayName("Полный пайплайн: несуществующий файл -> FAIL с ошибкой Extract")
    void testMissingFilePipeline() {
        Path savePath = Paths.get("samples", "nonexistent.json");
        Path outputPath = tempDir.resolve("report_missing.json");

        ValidationReport report = orchestrator.run(savePath, schemaPath, null, outputPath);

        assertEquals(ValidationReport.Status.FAIL, report.getStatus());
        assertFalse(report.getErrors().isEmpty());
        assertEquals("FILE_READ_ERROR", report.getErrors().get(0).getErrorType());
    }

    @Test
    @DisplayName("Отчёт корректно сериализуется в JSON")
    void testReportSerialization() throws Exception {
        Path savePath = Paths.get("samples", "test_save_valid.json");
        Path checksumPath = Paths.get("samples", "test_save_valid.json.sha256");
        Path outputPath = tempDir.resolve("report_serialization.json");

        orchestrator.run(savePath, schemaPath, checksumPath, outputPath);

        // Проверяем, что отчёт десериализуется обратно
        ObjectMapper mapper = new ObjectMapper();
        ValidationReport loaded = mapper.readValue(outputPath.toFile(), ValidationReport.class);

        assertNotNull(loaded.getFileName());
        assertNotNull(loaded.getTimestamp());
        assertNotNull(loaded.getComputedHash());
        assertEquals(ValidationReport.Status.PASS, loaded.getStatus());
    }

    @Test
    @DisplayName("CSV-отчёт создаётся корректно")
    void testCsvReport() {
        Path savePath = Paths.get("samples", "test_save_valid.json");
        Path checksumPath = Paths.get("samples", "test_save_valid.json.sha256");
        Path outputPath = tempDir.resolve("report.csv");

        orchestrator.run(savePath, schemaPath, checksumPath, outputPath);

        assertTrue(Files.exists(outputPath), "CSV-отчёт должен быть создан");
    }
}
