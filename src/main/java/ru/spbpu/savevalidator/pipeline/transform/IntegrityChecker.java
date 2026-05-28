package ru.spbpu.savevalidator.pipeline.transform;

import ru.spbpu.savevalidator.model.ValidationError;
import ru.spbpu.savevalidator.model.ValidationReport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Слой Transform (часть 2) ETL-пайплайна.
 * Проверяет целостность файла сохранения путём сравнения
 * вычисленного SHA-256 хеша с эталонным значением из .sha256 файла.
 */
public class IntegrityChecker {

    /**
     * Проверяет целостность файла, сравнивая вычисленный хеш с эталонным.
     *
     * @param checksumPath путь к файлу .sha256 с эталонным хешем
     * @param report       отчёт валидации (должен уже содержать computedHash)
     * @return true, если хеши совпадают; false при несовпадении или ошибке чтения
     */
    public boolean check(Path checksumPath, ValidationReport report) {
        String expectedHash;
        try {
            expectedHash = readExpectedHash(checksumPath);
        } catch (IOException e) {
            report.addError(new ValidationError(
                    "$checksum",
                    "Не удалось прочитать файл контрольной суммы: " + e.getMessage(),
                    "CHECKSUM_READ_ERROR",
                    ValidationError.Stage.TRANSFORM,
                    ValidationError.Severity.ERROR
            ));
            report.setIntegrityValid(false);
            return false;
        }

        report.setExpectedHash(expectedHash);
        String computedHash = report.getComputedHash();

        if (computedHash == null) {
            report.addError(new ValidationError(
                    "$checksum",
                    "Хеш файла не был вычислен на этапе Extract",
                    "MISSING_COMPUTED_HASH",
                    ValidationError.Stage.TRANSFORM,
                    ValidationError.Severity.ERROR
            ));
            report.setIntegrityValid(false);
            return false;
        }

        if (computedHash.equalsIgnoreCase(expectedHash)) {
            report.setIntegrityValid(true);
            return true;
        } else {
            report.addError(new ValidationError(
                    "$checksum",
                    String.format("Хеш не совпадает: вычислен=%s, ожидается=%s",
                            computedHash, expectedHash),
                    "INTEGRITY_FAILURE",
                    ValidationError.Stage.TRANSFORM,
                    ValidationError.Severity.ERROR
            ));
            report.setIntegrityValid(false);
            return false;
        }
    }

    /**
     * Читает эталонный хеш из .sha256 файла.
     * Формат файла: "хеш  имя_файла" (стандарт sha256sum) или просто хеш.
     *
     * @param checksumPath путь к файлу .sha256
     * @return строка хеша (64 hex-символа)
     * @throws IOException при ошибке чтения
     */
    public String readExpectedHash(Path checksumPath) throws IOException {
        String content = Files.readString(checksumPath).trim();
        // Пропускаем строки-комментарии (начинающиеся с #)
        for (String line : content.split("\n")) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            // Формат sha256sum: "хеш  имя_файла" или "хеш *имя_файла"
            String[] parts = line.split("\\s+", 2);
            return parts[0].trim();
        }
        return content;
    }
}
