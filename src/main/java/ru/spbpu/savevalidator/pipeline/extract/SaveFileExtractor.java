package ru.spbpu.savevalidator.pipeline.extract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.spbpu.savevalidator.model.ValidationError;
import ru.spbpu.savevalidator.model.ValidationReport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Слой Extract ETL-пайплайна.
 * Читает файл сохранения, парсит JSON и вычисляет SHA-256 хеш содержимого.
 */
public class SaveFileExtractor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Извлекает данные из файла сохранения.
     * Читает содержимое файла, парсит как JSON и вычисляет SHA-256 хеш.
     *
     * @param savePath путь к файлу сохранения (.json)
     * @param report   отчёт валидации, в который записываются ошибки и хеш
     * @return распарсенный JSON как JsonNode, или null при ошибке чтения/парсинга
     */
    public JsonNode extract(Path savePath, ValidationReport report) {
        report.setFileName(savePath.getFileName().toString());

        // Чтение файла
        byte[] fileBytes;
        try {
            fileBytes = Files.readAllBytes(savePath);
        } catch (IOException e) {
            report.addError(new ValidationError(
                    "$",
                    "Не удалось прочитать файл: " + e.getMessage(),
                    "FILE_READ_ERROR",
                    ValidationError.Stage.EXTRACT,
                    ValidationError.Severity.ERROR
            ));
            return null;
        }

        // Вычисление SHA-256 хеша
        String hash = computeSha256(fileBytes);
        report.setComputedHash(hash);

        // Парсинг JSON
        try {
            return objectMapper.readTree(fileBytes);
        } catch (IOException e) {
            report.addError(new ValidationError(
                    "$",
                    "Некорректный JSON: " + e.getMessage(),
                    "JSON_PARSE_ERROR",
                    ValidationError.Stage.EXTRACT,
                    ValidationError.Severity.ERROR
            ));
            return null;
        }
    }

    /**
     * Вычисляет SHA-256 хеш массива байтов.
     *
     * @param data входные данные
     * @return шестнадцатеричное представление хеша (64 символа, lowercase)
     */
    public String computeSha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(data);
            StringBuilder sb = new StringBuilder(64);
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 не поддерживается", e);
        }
    }
}
