package ru.spbpu.savevalidator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.spbpu.savevalidator.model.ValidationReport;
import ru.spbpu.savevalidator.pipeline.transform.SchemaValidator;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit-тесты для SchemaValidator.
 * Проверяют корректность валидации JSON-данных против JSON Schema Draft-07.
 */
class SchemaValidatorTest {

    private SchemaValidator validator;
    private ObjectMapper mapper;
    private Path schemaPath;

    @BeforeEach
    void setUp() {
        validator = new SchemaValidator();
        mapper = new ObjectMapper();
        schemaPath = Paths.get("schemas", "save_schema_v1.json");
    }

    @Test
    @DisplayName("Валидный файл сохранения проходит проверку схемы")
    void testValidSaveFile() throws Exception {
        String json = """
                {
                  "save_version": "1.2",
                  "player": { "name": "Hero", "level": 15, "hp": 100, "inventory": ["sword"] },
                  "location": "dungeon_level_3",
                  "playtime_seconds": 3600
                }
                """;
        JsonNode node = mapper.readTree(json);
        ValidationReport report = new ValidationReport();

        boolean result = validator.validate(node, schemaPath, report);

        assertTrue(result, "Валидный файл должен проходить проверку");
        assertTrue(report.isSchemaValid());
        assertTrue(report.getErrors().isEmpty());
    }

    @Test
    @DisplayName("Некорректный save_version не проходит проверку pattern")
    void testInvalidSaveVersion() throws Exception {
        String json = """
                {
                  "save_version": "abc",
                  "player": { "name": "Hero", "level": 15, "hp": 100 },
                  "location": "forest"
                }
                """;
        JsonNode node = mapper.readTree(json);
        ValidationReport report = new ValidationReport();

        boolean result = validator.validate(node, schemaPath, report);

        assertFalse(result, "Некорректный save_version должен отклоняться");
        assertFalse(report.isSchemaValid());
        assertFalse(report.getErrors().isEmpty());
    }

    @Test
    @DisplayName("Отсутствие обязательного поля location вызывает ошибку")
    void testMissingRequiredField() throws Exception {
        String json = """
                {
                  "save_version": "1.0",
                  "player": { "name": "Hero", "level": 10, "hp": 50 }
                }
                """;
        JsonNode node = mapper.readTree(json);
        ValidationReport report = new ValidationReport();

        boolean result = validator.validate(node, schemaPath, report);

        assertFalse(result, "Отсутствие обязательного поля должно вызывать ошибку");
        assertFalse(report.isSchemaValid());
    }

    @Test
    @DisplayName("Уровень больше 100 не проходит проверку maximum")
    void testLevelExceedsMaximum() throws Exception {
        String json = """
                {
                  "save_version": "1.0",
                  "player": { "name": "Hero", "level": 150, "hp": 100 },
                  "location": "castle"
                }
                """;
        JsonNode node = mapper.readTree(json);
        ValidationReport report = new ValidationReport();

        boolean result = validator.validate(node, schemaPath, report);

        assertFalse(result, "level > 100 должен отклоняться");
        assertFalse(report.isSchemaValid());
    }

    @Test
    @DisplayName("Отрицательное hp не проходит проверку minimum")
    void testNegativeHp() throws Exception {
        String json = """
                {
                  "save_version": "1.0",
                  "player": { "name": "Hero", "level": 10, "hp": -5 },
                  "location": "town"
                }
                """;
        JsonNode node = mapper.readTree(json);
        ValidationReport report = new ValidationReport();

        boolean result = validator.validate(node, schemaPath, report);

        assertFalse(result, "Отрицательное hp должно отклоняться");
        assertFalse(report.isSchemaValid());
    }

    @Test
    @DisplayName("Минимально валидный файл (только обязательные поля)")
    void testMinimalValidFile() throws Exception {
        String json = """
                {
                  "save_version": "2.0",
                  "player": { "name": "Min", "level": 1, "hp": 0 },
                  "location": "start"
                }
                """;
        JsonNode node = mapper.readTree(json);
        ValidationReport report = new ValidationReport();

        boolean result = validator.validate(node, schemaPath, report);

        assertTrue(result, "Минимально валидный файл должен проходить");
        assertTrue(report.isSchemaValid());
    }
}
