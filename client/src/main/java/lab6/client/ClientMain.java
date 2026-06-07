package lab6.client;

import lab6.client.input.CityFormReader;
import lab6.client.input.ClientCommand;
import lab6.client.io.ConsoleInput;
import lab6.client.io.InputSource;
import lab6.client.io.InputValidationException;
import lab6.client.io.ScriptInput;
import lab6.client.net.TcpClient;
import lab6.common.model.City;
import lab6.common.model.CityData;
import lab6.common.model.StandardOfLiving;
import lab6.common.net.CommandName;
import lab6.common.net.Request;
import lab6.common.net.Response;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.List;

/**
 * Точка входа клиентского приложения.
 * <p>Параметры окружения:
 * <ul>
 *     <li>{@code SERVER_HOST} (по умолчанию {@code localhost});</li>
 *     <li>{@code SERVER_PORT} (по умолчанию {@code 5444}).</li>
 * </ul>
 * <p>Цикл:
 * <ol>
 *     <li>читает строку из {@link InputSource},</li>
 *     <li>парсит имя команды и её аргументы,</li>
 *     <li>при необходимости запрашивает у пользователя объект {@code City},</li>
 *     <li>формирует {@link Request}, отправляет на сервер с поддержкой повторных попыток,</li>
 *     <li>печатает результат.</li>
 * </ol>
 */
public final class ClientMain {

    private static final int RETRY_DELAY_MS = 2_000;
    private static final int MAX_RETRIES = 5;

    private final PrintStream out = System.out;
    private final TcpClient client;
    private final CityFormReader formReader;
    private final ArrayDeque<Path> scriptStack = new ArrayDeque<>();
    private boolean running = true;

    private ClientMain(TcpClient client) {
        this.client = client;
        this.formReader = new CityFormReader(out);
    }

    public static void main(String[] args) {
        String host = envOr("SERVER_HOST", "localhost");
        int port = parseInt(envOr("SERVER_PORT", "5444"), 5444);
        try (TcpClient client = new TcpClient(host, port, 2_000, 10_000)) {
            new ClientMain(client).run();
        }
    }

    private void run() {
        out.println("Чтобы увидеть список команд, наберите help.");
        try (InputSource source = new ConsoleInput()) {
            loop(source);
        } catch (IOException e) {
            out.println("Ошибка ввода: " + e.getMessage());
        }
    }

    private void loop(InputSource source) throws IOException {
        while (running) {
            if (source.isInteractive()) out.print("> ");
            String line = source.readLine();
            if (line == null) {
                if (source.isInteractive()) running = false;
                return;
            }
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            try {
                executeLine(trimmed, source);
            } catch (InputValidationException ex) {
                out.println("Ошибка ввода: " + ex.getMessage());
                if (!source.isInteractive()) {
                    out.println("Выполнение скрипта прервано.");
                    return;
                }
            }
        }
    }

    private void executeLine(String line, InputSource source) throws IOException, InputValidationException {
        String[] parts = line.split("\\s+", 2);
        ClientCommand cmd = ClientCommand.fromToken(parts[0]);
        if (cmd == null) {
            out.println("Неизвестная команда: " + parts[0] + ". Используйте help.");
            return;
        }
        String args = parts.length > 1 ? parts[1].trim() : "";
        switch (cmd) {
            case EXIT -> { running = false; out.println("Выход."); }
            case EXECUTE_SCRIPT -> runScript(args);
            case HELP -> sendAndPrint(Request.simple(CommandName.HELP));
            case INFO -> sendAndPrint(Request.simple(CommandName.INFO));
            case SHOW -> sendAndPrint(Request.simple(CommandName.SHOW));
            case CLEAR -> sendAndPrint(Request.simple(CommandName.CLEAR));
            case MIN_BY_NAME -> sendAndPrint(Request.simple(CommandName.MIN_BY_NAME));
            case ADD -> sendAndPrint(Request.withCity(CommandName.ADD, formReader.readCityData(source)));
            case ADD_IF_MAX -> sendAndPrint(Request.withCity(CommandName.ADD_IF_MAX, formReader.readCityData(source)));
            case REMOVE_GREATER -> sendAndPrint(Request.withCity(CommandName.REMOVE_GREATER, formReader.readCityData(source)));
            case REMOVE_LOWER -> sendAndPrint(Request.withCity(CommandName.REMOVE_LOWER, formReader.readCityData(source)));
            case REMOVE_BY_ID -> {
                int id = parseRequiredInt(args, "remove_by_id");
                sendAndPrint(Request.withId(CommandName.REMOVE_BY_ID, id));
            }
            case UPDATE -> {
                int id = parseRequiredInt(args, "update");
                CityData data = formReader.readCityData(source);
                sendAndPrint(Request.withIdAndCity(CommandName.UPDATE, id, data));
            }
            case FILTER_BY_TIMEZONE -> {
                double tz = parseRequiredDouble(args, "filter_by_timezone");
                sendAndPrint(Request.withTimezone(CommandName.FILTER_BY_TIMEZONE, tz));
            }
            case FILTER_LESS_THAN_STANDARD_OF_LIVING -> {
                StandardOfLiving sol = parseRequiredEnum(args);
                sendAndPrint(Request.withStandardOfLiving(CommandName.FILTER_LESS_THAN_STANDARD_OF_LIVING, sol));
            }
        }
    }

    private void runScript(String args) throws IOException, InputValidationException {
        if (args.isEmpty()) throw new InputValidationException("execute_script требует имя файла");
        Path path = Path.of(args).toAbsolutePath().normalize();
        if (!Files.exists(path) || !Files.isReadable(path)) {
            throw new InputValidationException("файл скрипта недоступен: " + path);
        }
        if (scriptStack.contains(path)) {
            out.println("Обнаружена рекурсия execute_script. Скрипт пропущен: " + path);
            return;
        }
        scriptStack.push(path);
        try (ScriptInput script = new ScriptInput(path)) {
            out.println("Запуск скрипта: " + path);
            loop(script);
        } finally {
            scriptStack.pop();
        }
    }

    private void sendAndPrint(Request request) {
        Response response = sendWithRetry(request);
        if (response == null) {
            out.println("Сервер недоступен после нескольких попыток. Команда не выполнена.");
            return;
        }
        out.println("[" + response.getStatus() + "] " + response.getMessage());
        if (response.getPayload() instanceof List<?> list) {
            list.forEach(item -> out.println("  " + item));
        } else if (response.getPayload() instanceof City city) {
            out.println("  " + city);
        }
    }

    private Response sendWithRetry(Request request) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                return client.send(request);
            } catch (IOException e) {
                out.println("Попытка " + attempt + "/" + MAX_RETRIES + ": сервер недоступен (" + e.getMessage() + ")");
                client.resetConnection();
                if (attempt < MAX_RETRIES) {
                    try { Thread.sleep(RETRY_DELAY_MS); }
                    catch (InterruptedException ie) { Thread.currentThread().interrupt(); return null; }
                }
            }
        }
        return null;
    }

    private static int parseRequiredInt(String args, String cmd) throws InputValidationException {
        if (args.isEmpty()) throw new InputValidationException(cmd + " требует целое число");
        try { return Integer.parseInt(args); }
        catch (NumberFormatException e) { throw new InputValidationException(cmd + ": ожидалось целое число"); }
    }

    private static double parseRequiredDouble(String args, String cmd) throws InputValidationException {
        if (args.isEmpty()) throw new InputValidationException(cmd + " требует число");
        try { return Double.parseDouble(args); }
        catch (NumberFormatException e) { throw new InputValidationException(cmd + ": ожидалось число"); }
    }

    private static StandardOfLiving parseRequiredEnum(String args) throws InputValidationException {
        if (args.isEmpty()) {
            throw new InputValidationException("filter_less_than_standard_of_living требует значение enum (одно из "
                    + java.util.Arrays.toString(StandardOfLiving.values()) + ")");
        }
        try { return StandardOfLiving.valueOf(args); }
        catch (IllegalArgumentException e) {
            throw new InputValidationException("Невалидное значение enum: " + args);
        }
    }

    private static String envOr(String key, String defaultValue) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static int parseInt(String raw, int defaultValue) {
        try { return Integer.parseInt(raw); }
        catch (NumberFormatException e) { return defaultValue; }
    }
}
