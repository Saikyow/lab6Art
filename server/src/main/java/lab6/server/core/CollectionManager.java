package lab6.server.core;

import lab6.common.model.City;
import lab6.common.model.CityData;
import lab6.common.model.StandardOfLiving;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Управляет коллекцией городов на стороне сервера.
 * <p>Использует {@link LinkedHashSet} (по требованию ТЗ). Все операции над коллекцией
 * реализованы через Stream API + лямбды. Назначает автоматически генерируемые
 * поля {@code id} и {@code creationDate}.
 */
public final class CollectionManager {
    private final LinkedHashSet<City> cities = new LinkedHashSet<>();
    private final ZonedDateTime initializationTime = ZonedDateTime.now();
    private int nextId = 1;

    /**
     * Инициализирует менеджер списком городов из файла.
     * Назначает {@code nextId} как максимальный + 1.
     */
    public void initialize(Collection<City> initial) {
        cities.clear();
        cities.addAll(initial);
        nextId = cities.stream().mapToInt(City::getId).max().orElse(0) + 1;
    }

    /** @return снимок коллекции, отсортированный по координатам (местоположению) */
    public List<City> snapshotByLocation() {
        return cities.stream()
                .sorted(Comparator.comparing(City::getCoordinates))
                .toList();
    }

    /** Добавляет город из пользовательской формы и возвращает его. */
    public City add(CityData data) {
        City city = City.of(nextId++, ZonedDateTime.now(), data);
        cities.add(city);
        return city;
    }

    /**
     * Обновляет город по id — сохраняет исходный {@code creationDate}, заменяет остальное.
     *
     * @return {@code true}, если объект найден и обновлён
     */
    public boolean update(int id, CityData data) {
        Optional<City> existing = findById(id);
        if (existing.isEmpty()) return false;
        City old = existing.get();
        cities.remove(old);
        cities.add(City.of(id, old.getCreationDate(), data));
        return true;
    }

    /** @return {@code true}, если объект с указанным id был удалён */
    public boolean removeById(int id) {
        return cities.removeIf(c -> c.getId() == id);
    }

    public void clear() {
        cities.clear();
    }

    /**
     * Добавляет город, если он больше максимального по естественному порядку.
     *
     * @return {@code true}, если объект был добавлен
     */
    public boolean addIfMax(CityData data) {
        City candidate = City.of(nextId, ZonedDateTime.now(), data);
        boolean isMaxOrEmpty = cities.stream()
                .max(Comparator.naturalOrder())
                .map(max -> candidate.compareTo(max) > 0)
                .orElse(true);
        if (!isMaxOrEmpty) return false;
        cities.add(candidate);
        nextId++;
        return true;
    }

    /** @return число удалённых элементов, больших чем {@code threshold} */
    public int removeGreater(CityData threshold) {
        City probe = City.of(Integer.MAX_VALUE, ZonedDateTime.now(), threshold);
        return removeMatching(c -> c.compareTo(probe) > 0);
    }

    /** @return число удалённых элементов, меньших чем {@code threshold} */
    public int removeLower(CityData threshold) {
        City probe = City.of(Integer.MAX_VALUE, ZonedDateTime.now(), threshold);
        return removeMatching(c -> c.compareTo(probe) < 0);
    }

    /** @return любой город с минимальным значением {@code name} (без учёта регистра) */
    public Optional<City> minByName() {
        return cities.stream().min(Comparator.comparing(City::getName, String.CASE_INSENSITIVE_ORDER));
    }

    /** @return города с {@code timezone}, равным указанному, отсортированные по местоположению */
    public List<City> filterByTimezone(double timezone) {
        return cities.stream()
                .filter(c -> Double.compare(c.getTimezone(), timezone) == 0)
                .sorted(Comparator.comparing(City::getCoordinates))
                .toList();
    }

    /**
     * @return города с {@code standardOfLiving}, строго меньшим указанного,
     *         отсортированные по местоположению. {@code null} значения не попадают в результат.
     */
    public List<City> filterLessThanStandardOfLiving(StandardOfLiving threshold) {
        Predicate<City> below = c -> c.getStandardOfLiving() != null
                && c.getStandardOfLiving().ordinal() < threshold.ordinal();
        return cities.stream()
                .filter(below)
                .sorted(Comparator.comparing(City::getCoordinates))
                .toList();
    }

    public int size() {
        return cities.size();
    }

    public boolean isEmpty() {
        return cities.isEmpty();
    }

    public String collectionType() {
        return cities.getClass().getName();
    }

    public ZonedDateTime getInitializationTime() {
        return initializationTime;
    }

    /** @return оборонительная копия для сохранения в файл */
    public List<City> snapshotForPersistence() {
        return new ArrayList<>(cities);
    }

    private Optional<City> findById(int id) {
        return cities.stream().filter(c -> c.getId() == id).findFirst();
    }

    private int removeMatching(Predicate<City> predicate) {
        int sizeBefore = cities.size();
        cities.removeIf(predicate);
        return sizeBefore - cities.size();
    }
}
