package lab6.server;

import lab6.common.model.City;
import lab6.server.commands.CommandDispatcher;
import lab6.server.core.CollectionManager;
import lab6.server.io.XmlCityRepository;
import lab6.server.io.XmlRepositoryException;
import lab6.server.net.NioServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Точка входа серверного приложения.
 * <p>Параметры окружения:
 * <ul>
 *     <li>{@code CITIES_FILE} — путь к XML-файлу с коллекцией (обязательно);</li>
 *     <li>{@code SERVER_PORT} — TCP-порт для входящих подключений (по умолчанию 5444).</li>
 * </ul>
 * <p>В консоли сервера поддерживаются недоступные клиенту команды:
 * <ul>
 *     <li>{@code save} — сохранить коллекцию в файл;</li>
 *     <li>{@code exit} — сохранить коллекцию и завершить работу.</li>
 * </ul>
 */
public final class ServerMain {

    private static final Logger LOG = LogManager.getLogger(ServerMain.class);
    private static final int DEFAULT_PORT = 5444;

    private ServerMain() {}

    public static void main(String[] args) {
        String fileEnv = System.getenv("CITIES_FILE");
        if (fileEnv == null || fileEnv.isBlank()) {
            System.err.println("Не задана переменная окружения CITIES_FILE (путь к XML-файлу).");
            System.exit(1);
            return;
        }
        Path filePath = Path.of(fileEnv).toAbsolutePath().normalize();
        int port = parsePort(System.getenv("SERVER_PORT"));

        XmlCityRepository repository = new XmlCityRepository(filePath);
        CollectionManager manager = new CollectionManager();
        try {
            List<City> loaded = repository.load();
            manager.initialize(loaded);
            LOG.info("Загружено элементов: {} из {}", loaded.size(), filePath);
        } catch (XmlRepositoryException e) {
            LOG.error("Ошибка загрузки коллекции: {}", e.getMessage());
            LOG.warn("Коллекция инициализирована пустой.");
            manager.initialize(List.of());
        }

        CommandDispatcher dispatcher = new CommandDispatcher();
        AtomicBoolean keepRunning = new AtomicBoolean(true);
        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                repository.save(manager.snapshotForPersistence());
                LOG.info("Коллекция сохранена в файл при завершении: {}", filePath);
            } catch (XmlRepositoryException e) {
                LOG.error("Не удалось сохранить коллекцию при завершении: {}", e.getMessage());
            }
        }));

        try {
            NioServer server = new NioServer(port, manager, dispatcher);
            LOG.info("Старт сервера на порту {}", port);
            System.out.println("Серверные команды: save, exit");
            server.run(keepRunning::get, s -> pollStdin(stdin, manager, repository, keepRunning));
        } catch (IOException e) {
            LOG.error("Сервер не смог стартовать: {}", e.getMessage());
            System.exit(2);
        }
    }

    private static void pollStdin(BufferedReader stdin,
                                  CollectionManager manager,
                                  XmlCityRepository repository,
                                  AtomicBoolean keepRunning) {
        try {
            if (System.in.available() <= 0) return;
        } catch (IOException ignored) {
            return;
        }
        try {
            String line = stdin.readLine();
            if (line == null) {
                keepRunning.set(false);
                return;
            }
            String cmd = line.trim();
            switch (cmd) {
                case "" -> {}
                case "save" -> {
                    try {
                        repository.save(manager.snapshotForPersistence());
                        System.out.println("Коллекция сохранена.");
                        LOG.info("Серверная команда save выполнена.");
                    } catch (XmlRepositoryException e) {
                        System.out.println("Ошибка сохранения: " + e.getMessage());
                        LOG.error("Серверная команда save завершилась ошибкой: {}", e.getMessage());
                    }
                }
                case "exit" -> {
                    LOG.info("Серверная команда exit — инициируется остановка.");
                    keepRunning.set(false);
                }
                default -> System.out.println("Неизвестная серверная команда: " + cmd);
            }
        } catch (IOException e) {
            LOG.error("Ошибка чтения серверной консоли: {}", e.getMessage());
        }
    }

    private static int parsePort(String raw) {
        if (raw == null || raw.isBlank()) return DEFAULT_PORT;
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            LOG.warn("SERVER_PORT не число ({}), используется {}", raw, DEFAULT_PORT);
            return DEFAULT_PORT;
        }
    }
}
