package lab6.common.model;

import java.io.Serializable;

/**
 * Пользовательские данные города без автоматически генерируемых полей ({@code id}, {@code creationDate}).
 * Используется при передаче формы {@code add} / {@code update} / {@code add_if_max} /
 * {@code remove_greater} / {@code remove_lower} от клиента к серверу.
 */
public final class CityData implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String name;
    private final Coordinates coordinates;
    private final Double area;
    private final Long population;
    private final int metersAboveSeaLevel;
    private final double timezone;
    private final int carCode;
    private final StandardOfLiving standardOfLiving;
    private final Human governor;

    /**
     * Создаёт пользовательские данные города.
     *
     * @param name название; не {@code null} и не пустое
     * @param coordinates координаты; не {@code null}
     * @param area площадь; не {@code null} и больше 0
     * @param population население; не {@code null} и больше 0
     * @param metersAboveSeaLevel высота над уровнем моря
     * @param timezone часовой пояс; больше -13 и не больше 15
     * @param carCode автомобильный код; больше 0 и не больше 1000
     * @param standardOfLiving уровень жизни; может быть {@code null}
     * @param governor губернатор; не {@code null}
     * @throws IllegalArgumentException при нарушении инвариантов
     */
    public CityData(String name,
                    Coordinates coordinates,
                    Double area,
                    Long population,
                    int metersAboveSeaLevel,
                    double timezone,
                    int carCode,
                    StandardOfLiving standardOfLiving,
                    Human governor) {
        validate(name, coordinates, area, population, timezone, carCode, governor);
        this.name = name.trim();
        this.coordinates = coordinates;
        this.area = area;
        this.population = population;
        this.metersAboveSeaLevel = metersAboveSeaLevel;
        this.timezone = timezone;
        this.carCode = carCode;
        this.standardOfLiving = standardOfLiving;
        this.governor = governor;
    }

    private static void validate(String name,
                                 Coordinates coordinates,
                                 Double area,
                                 Long population,
                                 double timezone,
                                 int carCode,
                                 Human governor) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("name не может быть пустым");
        }
        if (coordinates == null) {
            throw new IllegalArgumentException("coordinates не может быть null");
        }
        if (area == null || area <= 0) {
            throw new IllegalArgumentException("area должно быть больше 0");
        }
        if (population == null || population <= 0) {
            throw new IllegalArgumentException("population должно быть больше 0");
        }
        if (timezone <= -13 || timezone > 15) {
            throw new IllegalArgumentException("timezone должен быть больше -13 и не больше 15");
        }
        if (carCode <= 0 || carCode > 1000) {
            throw new IllegalArgumentException("carCode должен быть больше 0 и не больше 1000");
        }
        if (governor == null) {
            throw new IllegalArgumentException("governor не может быть null");
        }
    }

    public String getName() { return name; }
    public Coordinates getCoordinates() { return coordinates; }
    public Double getArea() { return area; }
    public Long getPopulation() { return population; }
    public int getMetersAboveSeaLevel() { return metersAboveSeaLevel; }
    public double getTimezone() { return timezone; }
    public int getCarCode() { return carCode; }
    public StandardOfLiving getStandardOfLiving() { return standardOfLiving; }
    public Human getGovernor() { return governor; }
}
