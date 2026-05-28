package ru.spbpu.savevalidator;

import ru.spbpu.savevalidator.model.ValidationReport;
import ru.spbpu.savevalidator.pipeline.PipelineOrchestrator;

import java.nio.file.Paths;

/**
 * CLI-точка входа в приложение Save File Validator.
 * Принимает аргументы командной строки и запускает ETL-пайплайн.
 *
 * <p>Использование:
 * <pre>
 * java -jar save-validator.jar --save &lt;path&gt; --schema &lt;path&gt; --checksum &lt;path&gt; [--output &lt;path&gt;]
 * </pre>
 */
public class Main {

    /**
     * Точка входа. Парсит аргументы CLI и запускает пайплайн.
     *
     * @param args аргументы командной строки:
     *             --save     путь к файлу сохранения (обязательный)
     *             --schema   путь к JSON Schema (обязательный)
     *             --checksum путь к .sha256 файлу (опциональный)
     *             --output   путь для записи отчёта (опциональный, по умолчанию — stdout)
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            System.exit(1);
        }

        String savePath = null;
        String schemaPath = null;
        String checksumPath = null;
        String outputPath = null;

        // Парсинг аргументов
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--save", "-s" -> {
                    if (i + 1 < args.length) savePath = args[++i];
                }
                case "--schema", "-S" -> {
                    if (i + 1 < args.length) schemaPath = args[++i];
                }
                case "--checksum", "-c" -> {
                    if (i + 1 < args.length) checksumPath = args[++i];
                }
                case "--output", "-o" -> {
                    if (i + 1 < args.length) outputPath = args[++i];
                }
                case "--help", "-h" -> {
                    printUsage();
                    System.exit(0);
                }
                default -> {
                    System.err.println("Неизвестный аргумент: " + args[i]);
                    printUsage();
                    System.exit(1);
                }
            }
        }

        // Проверка обязательных аргументов
        if (savePath == null || schemaPath == null) {
            System.err.println("Ошибка: аргументы --save и --schema обязательны.");
            printUsage();
            System.exit(1);
        }

        // Запуск пайплайна
        PipelineOrchestrator orchestrator = new PipelineOrchestrator();
        ValidationReport report = orchestrator.run(
                Paths.get(savePath),
                Paths.get(schemaPath),
                checksumPath != null ? Paths.get(checksumPath) : null,
                outputPath != null ? Paths.get(outputPath) : null
        );

        // Код возврата: 0 = PASS, 1 = FAIL
        System.exit(report.getStatus() == ValidationReport.Status.PASS ? 0 : 1);
    }

    /** Выводит справку по использованию программы. */
    private static void printUsage() {
        System.out.println("Save File Validator — валидация файлов сохранений игр");
        System.out.println();
        System.out.println("Использование:");
        System.out.println("  java -jar save-validator.jar [опции]");
        System.out.println();
        System.out.println("Опции:");
        System.out.println("  --save, -s <path>      Путь к файлу сохранения (обязательный)");
        System.out.println("  --schema, -S <path>    Путь к JSON Schema (обязательный)");
        System.out.println("  --checksum, -c <path>  Путь к .sha256 файлу (опциональный)");
        System.out.println("  --output, -o <path>    Путь для отчёта .json/.csv (по умолчанию: stdout)");
        System.out.println("  --help, -h             Показать справку");
    }
}
