package lab6.common.model;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.Objects;

/**
 * Город, хранимый в коллекции.
 * <p>Поля и инварианты — согласно ТЗ ЛР5:
 * <ul>
 *     <li>{@code id} — {@code int} &gt; 0, уникальный, автогенерируется;</li>
 *     <li>{@code name} — не {@code null} и не пустая;</li>
 *     <li>{@code coordinates} — не {@code null};</li>
 *     <li>{@code creationDate} — {@link ZonedDateTime}, автогенерируется;</li>
 *     <li>{@code area} — {@link Double} &gt; 0, не {@code null};</li>
 *     <li>{@code population} — {@link Long} &gt; 0, не {@code null};</li>
 *     <li>{@code metersAboveSeaLevel} — {@code int};</li>
 *     <li>{@code timezone} — {@code double} в диапазоне (-13; 15];</li>
 *     <li>{@code carCode} — {@code int} в диапазоне (0; 1000];</li>
 *     <li>{@code standardOfLiving} — может быть {@code null};</li>
 *     <li>{@code governor} — не {@code null}.</li>
 * </ul>
 * Естественный порядок — по имени, затем по площади (см. {@link #compareTo}).
 */
public final class City implements Serializable, Comparable<City> {
    private static final long serialVersionUID = 1L;

    private static final Comparator<City> NATURAL = Comparator
            .comparing((City c) -> c.name, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(c -> c.area)
            .thenComparing(c -> c.population)
            .thenComparing(c -> c.id);

    private final int id;
    private final String name;
    private final Coordinates coordinates;
    private final ZonedDateTime creationDate;
    private final Double area;
    private final Long population;
    private final int metersAboveSeaLevel;
    private final double timezone;
    private final int carCode;
    private final StandardOfLiving standardOfLiving;
    private final Human governor;

    /**
     * Полный конструктор. Применяется при десериализации из XML и при создании
     * объекта по пользовательской форме (через {@link #of(int, ZonedDateTime, CityData)}).
     *
     * @throws IllegalArgumentException при нарушении любого инварианта
     */
    public City(int id,
                String name,
                Coordinates coordinates,
                ZonedDateTime creationDate,
                Double area,
                Long population,
                int metersAboveSeaLevel,
                double timezone,
                int carCode,
                StandardOfLiving standardOfLiving,
                Human governor) {
        if (id <= 0) throw new IllegalArgumentException("id должен быть больше 0");
        if (name == null || name.trim().isEmpty()) throw new IllegalArgumentException("name не может быть пустым");
        if (coordinates == null) throw new IllegalArgumentException("coordinates не может быть null");
        if (creationDate == null) throw new IllegalArgumentException("creationDate не может быть null");
        if (area == null || area <= 0) throw new IllegalArgumentException("area должно быть больше 0");
        if (population == null || population <= 0) throw new IllegalArgumentException("population должно быть больше 0");
        if (timezone <= -13 || timezone > 15) throw new IllegalArgumentException("timezone должен быть в (-13; 15]");
        if (carCode <= 0 || carCode > 1000) throw new IllegalArgumentException("carCode должен быть в (0; 1000]");
        if (governor == null) throw new IllegalArgumentException("governor не может быть null");

        this.id = id;
        this.name = name.trim();
        this.coordinates = coordinates;
        this.creationDate = creationDate;
        this.area = area;
        this.population = population;
        this.metersAboveSeaLevel = metersAboveSeaLevel;
        this.timezone = timezone;
        this.carCode = carCode;
        this.standardOfLiving = standardOfLiving;
        this.governor = governor;
    }

    /**
     * Фабрика: создаёт {@code City} из пользовательской формы и серверных автогенерируемых полей.
     */
    public static City of(int id, ZonedDateTime creationDate, CityData data) {
        return new City(
                id,
                data.getName(),
                data.getCoordinates(),
                creationDate,
                data.getArea(),
                data.getPopulation(),
                data.getMetersAboveSeaLevel(),
                data.getTimezone(),
                data.getCarCode(),
                data.getStandardOfLiving(),
                data.getGovernor()
        );
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public Coordinates getCoordinates() { return coordinates; }
    public ZonedDateTime getCreationDate() { return creationDate; }
    public Double getArea() { return area; }
    public Long getPopulation() { return population; }
    public int getMetersAboveSeaLevel() { return metersAboveSeaLevel; }
    public double getTimezone() { return timezone; }
    public int getCarCode() { return carCode; }
    public StandardOfLiving getStandardOfLiving() { return standardOfLiving; }
    public Human getGovernor() { return governor; }

    @Override
    public int compareTo(City other) {
        return NATURAL.compare(this, other);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof City)) return false;
        return id == ((City) o).id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "City{id=" + id
                + ", name='" + name + '\''
                + ", coordinates=" + coordinates
                + ", creationDate=" + creationDate
                + ", area=" + area
                + ", population=" + population
                + ", metersAboveSeaLevel=" + metersAboveSeaLevel
                + ", timezone=" + timezone
                + ", carCode=" + carCode
                + ", standardOfLiving=" + standardOfLiving
                + ", governor=" + governor
                + '}';
    }
}
