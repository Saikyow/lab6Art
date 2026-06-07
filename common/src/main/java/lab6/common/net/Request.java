package lab6.common.net;

import lab6.common.model.CityData;
import lab6.common.model.StandardOfLiving;

import java.io.Serializable;

/**
 * Объект сетевого запроса от клиента к серверу.
 * Хранит тип команды и типизированный набор аргументов.
 * <p>Используются фабричные методы {@code of*} — они подбирают только те поля,
 * которые осмыслены для конкретной команды.
 */
public final class Request implements Serializable {
    private static final long serialVersionUID = 1L;

    private final CommandName command;
    private final Integer id;
    private final CityData cityData;
    private final Double timezone;
    private final StandardOfLiving standardOfLiving;

    private Request(CommandName command,
                    Integer id,
                    CityData cityData,
                    Double timezone,
                    StandardOfLiving standardOfLiving) {
        this.command = command;
        this.id = id;
        this.cityData = cityData;
        this.timezone = timezone;
        this.standardOfLiving = standardOfLiving;
    }

    public static Request simple(CommandName command) {
        return new Request(command, null, null, null, null);
    }

    public static Request withCity(CommandName command, CityData data) {
        return new Request(command, null, data, null, null);
    }

    public static Request withId(CommandName command, int id) {
        return new Request(command, id, null, null, null);
    }

    public static Request withIdAndCity(CommandName command, int id, CityData data) {
        return new Request(command, id, data, null, null);
    }

    public static Request withTimezone(CommandName command, double timezone) {
        return new Request(command, null, null, timezone, null);
    }

    public static Request withStandardOfLiving(CommandName command, StandardOfLiving standardOfLiving) {
        return new Request(command, null, null, null, standardOfLiving);
    }

    public CommandName getCommand() { return command; }
    public Integer getId() { return id; }
    public CityData getCityData() { return cityData; }
    public Double getTimezone() { return timezone; }
    public StandardOfLiving getStandardOfLiving() { return standardOfLiving; }
}
