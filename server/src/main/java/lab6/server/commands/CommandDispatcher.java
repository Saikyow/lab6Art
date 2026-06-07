package lab6.server.commands;

import lab6.common.model.City;
import lab6.common.net.CommandName;
import lab6.common.net.Request;
import lab6.common.net.Response;
import lab6.server.core.CollectionManager;

import java.io.Serializable;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Маршрутизатор команд: связывает {@link CommandName} с реализацией {@link ServerCommand}
 * и применяет нужную к запросу.
 * <p>Реализации сделаны как лямбды/method references — это часть требования "Stream API + лямбды".
 */
public final class CommandDispatcher {

    private final Map<CommandName, ServerCommand> commands = new EnumMap<>(CommandName.class);

    public CommandDispatcher() {
        commands.put(CommandName.HELP, (req, mgr) -> Response.ok(helpText()));

        commands.put(CommandName.INFO, (req, mgr) -> Response.ok(
                "Тип коллекции: " + mgr.collectionType()
                        + "\nДата инициализации: " + mgr.getInitializationTime().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                        + "\nКоличество элементов: " + mgr.size()));

        commands.put(CommandName.SHOW, (req, mgr) -> {
            List<City> snapshot = mgr.snapshotByLocation();
            if (snapshot.isEmpty()) return Response.ok("Коллекция пуста.");
            return Response.ok("Элементы коллекции (отсортированы по местоположению):",
                    new ArrayList<>(snapshot));
        });

        commands.put(CommandName.ADD, (req, mgr) -> {
            if (req.getCityData() == null) return Response.error("Команда add требует объект City.");
            City added = mgr.add(req.getCityData());
            return Response.ok("Добавлен элемент с id=" + added.getId());
        });

        commands.put(CommandName.UPDATE, (req, mgr) -> {
            if (req.getId() == null || req.getCityData() == null) {
                return Response.error("Команда update требует id и объект City.");
            }
            return mgr.update(req.getId(), req.getCityData())
                    ? Response.ok("Элемент с id=" + req.getId() + " обновлён.")
                    : Response.error("Элемент с id=" + req.getId() + " не найден.");
        });

        commands.put(CommandName.REMOVE_BY_ID, (req, mgr) -> {
            if (req.getId() == null) return Response.error("Команда remove_by_id требует id.");
            return mgr.removeById(req.getId())
                    ? Response.ok("Элемент с id=" + req.getId() + " удалён.")
                    : Response.error("Элемент с id=" + req.getId() + " не найден.");
        });

        commands.put(CommandName.CLEAR, (req, mgr) -> {
            mgr.clear();
            return Response.ok("Коллекция очищена.");
        });

        commands.put(CommandName.ADD_IF_MAX, (req, mgr) -> {
            if (req.getCityData() == null) return Response.error("Команда add_if_max требует объект City.");
            return mgr.addIfMax(req.getCityData())
                    ? Response.ok("Элемент добавлен (больше всех существующих).")
                    : Response.ok("Элемент не добавлен: значение не превышает максимальное.");
        });

        commands.put(CommandName.REMOVE_GREATER, (req, mgr) -> {
            if (req.getCityData() == null) return Response.error("Команда remove_greater требует объект City.");
            int removed = mgr.removeGreater(req.getCityData());
            return Response.ok("Удалено элементов: " + removed);
        });

        commands.put(CommandName.REMOVE_LOWER, (req, mgr) -> {
            if (req.getCityData() == null) return Response.error("Команда remove_lower требует объект City.");
            int removed = mgr.removeLower(req.getCityData());
            return Response.ok("Удалено элементов: " + removed);
        });

        commands.put(CommandName.MIN_BY_NAME, (req, mgr) -> mgr.minByName()
                .<Response>map(city -> Response.ok("Минимальный по name:", (Serializable) city))
                .orElseGet(() -> Response.ok("Коллекция пуста.")));

        commands.put(CommandName.FILTER_BY_TIMEZONE, (req, mgr) -> {
            if (req.getTimezone() == null) return Response.error("Команда filter_by_timezone требует значение timezone.");
            List<City> matches = mgr.filterByTimezone(req.getTimezone());
            return matches.isEmpty()
                    ? Response.ok("Подходящих элементов нет.")
                    : Response.ok("Найдено элементов: " + matches.size(), new ArrayList<>(matches));
        });

        commands.put(CommandName.FILTER_LESS_THAN_STANDARD_OF_LIVING, (req, mgr) -> {
            if (req.getStandardOfLiving() == null) {
                return Response.error("Команда filter_less_than_standard_of_living требует значение enum.");
            }
            List<City> matches = mgr.filterLessThanStandardOfLiving(req.getStandardOfLiving());
            return matches.isEmpty()
                    ? Response.ok("Подходящих элементов нет.")
                    : Response.ok("Найдено элементов: " + matches.size(), new ArrayList<>(matches));
        });
    }

    /**
     * Выполняет команду из запроса. Если команды нет — возвращает ошибку.
     */
    public Response dispatch(Request request, CollectionManager manager) {
        ServerCommand command = commands.get(request.getCommand());
        if (command == null) return Response.error("Неизвестная команда: " + request.getCommand());
        try {
            return command.execute(request, manager);
        } catch (IllegalArgumentException e) {
            return Response.error("Ошибка валидации: " + e.getMessage());
        } catch (RuntimeException e) {
            return Response.error("Внутренняя ошибка сервера: " + e.getMessage());
        }
    }

    private static String helpText() {
        return String.join("\n",
                "help — справка по командам",
                "info — информация о коллекции",
                "show — вывести все элементы",
                "add {element} — добавить элемент",
                "update id {element} — обновить элемент по id",
                "remove_by_id id — удалить элемент по id",
                "clear — очистить коллекцию",
                "execute_script file_name — выполнить скрипт",
                "exit — выйти из клиента",
                "add_if_max {element} — добавить, если больше максимального",
                "remove_greater {element} — удалить элементы больше заданного",
                "remove_lower {element} — удалить элементы меньше заданного",
                "min_by_name — элемент с минимальным name",
                "filter_by_timezone timezone — элементы с указанным timezone",
                "filter_less_than_standard_of_living standardOfLiving — элементы с меньшим уровнем жизни");
    }
}
