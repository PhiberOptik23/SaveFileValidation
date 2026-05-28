package ru.spbpu.savevalidator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.spbpu.savevalidator.model.ValidationReport;
import ru.spbpu.savevalidator.pipeline.extract.SaveFileExtractor;
import ru.spbpu.savevalidator.pipeline.transform.IntegrityChecker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit-тесты для IntegrityChecker.
 * Проверяют корректность сравнения SHA-256 хешей.
 */
class IntegrityCheckerTest {

    private IntegrityChecker checker;
    private SaveFileExtractor extractor;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        checker = new IntegrityChecker();
        extractor = new SaveFileExtractor();
    }

    @Test
    @DisplayName("Корректный хеш проходит проверку целостности")
    void testValidChecksum() throws IOException {
        // Создаём тестовый файл
        String content = "{\"test\": \"data\"}";
        Path testFile = tempDir.resolve("test.json");
        Files.writeString(testFile, content);

        // Вычисляем правильный хеш
        String hash = extractor.computeSha256(content.getBytes());

        // Создаём файл контрольной суммы
        Path checksumFile = tempDir.resolve("test.json.sha256");
        Files.writeString(checksumFile, hash + "  test.json\n");

        // Подготавливаем отчёт
        ValidationReport report = new ValidationReport();
        report.setComputedHash(hash);

        boolean result = checker.check(checksumFile, report);

        assertTrue(result, "Корректный хеш должен проходить проверку");
        assertTrue(report.isIntegrityValid());
        assertEquals(hash, report.getExpectedHash());
    }

    @Test
    @DisplayName("Некорректный хеш не проходит проверку целостности")
    void testInvalidChecksum() throws IOException {
        String content = "{\"test\": \"data\"}";
        String computedHash = extractor.computeSha256(content.getBytes());
        String wrongHash = "0000000000000000000000000000000000000000000000000000000000000000";

        Path checksumFile = tempDir.resolve("test.json.sha256");
        Files.writeString(checksumFile, wrongHash + "  test.json\n");

        ValidationReport report = new ValidationReport();
        report.setComputedHash(computedHash);

        boolean result = checker.check(checksumFile, report);

        assertFalse(result, "Неверный хеш должен отклоняться");
        assertFalse(report.isIntegrityValid());
        assertEquals(wrongHash, report.getExpectedHash());
        assertFalse(report.getErrors().isEmpty());
    }

    @Test
    @DisplayName("Отсутствующий файл контрольной суммы вызывает ошибку")
    void testMissingChecksumFile() {
        Path missingFile = tempDir.resolve("nonexistent.sha256");
        ValidationReport report = new ValidationReport();
        report.setComputedHash("abc123");

        boolean result = checker.check(missingFile, report);

        assertFalse(result, "Отсутствующий файл должен вызывать ошибку");
        assertFalse(report.isIntegrityValid());
    }

    @Test
    @DisplayName("Файл с комментариями корректно парсится")
    void testChecksumFileWithComments() throws IOException {
        String hash = "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2";

        Path checksumFile = tempDir.resolve("test.json.sha256");
        Files.writeString(checksumFile, "# Checksum for test.json\n" + hash + "  test.json\n");

        ValidationReport report = new ValidationReport();
        report.setComputedHash(hash);

        boolean result = checker.check(checksumFile, report);

        assertTrue(result, "Хеш из файла с комментариями должен читаться верно");
        assertTrue(report.isIntegrityValid());
    }

    @Test
    @DisplayName("SHA-256 хеш вычисляется детерминированно")
    void testHashDeterminism() {
        byte[] data = "Hello, World!".getBytes();
        String hash1 = extractor.computeSha256(data);
        String hash2 = extractor.computeSha256(data);

        assertEquals(hash1, hash2, "Хеш должен быть детерминированным");
        assertEquals(64, hash1.length(), "SHA-256 хеш должен иметь длину 64 символа");
    }

    @Test
    @DisplayName("Разные данные дают разные хеши")
    void testDifferentDataDifferentHash() {
        String hash1 = extractor.computeSha256("data1".getBytes());
        String hash2 = extractor.computeSha256("data2".getBytes());

        assertNotEquals(hash1, hash2, "Разные данные должны давать разные хеши");
    }
}
