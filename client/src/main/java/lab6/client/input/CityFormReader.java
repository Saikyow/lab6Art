package lab6.client.input;

import lab6.client.io.InputSource;
import lab6.client.io.InputValidationException;
import lab6.common.model.CityData;
import lab6.common.model.Coordinates;
import lab6.common.model.Human;
import lab6.common.model.StandardOfLiving;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.function.Function;

/**
 * Построчное чтение пользовательской формы {@link CityData}.
 * <p>В интерактивном режиме при ошибке поля ввод того же поля повторяется.
 * В режиме скрипта первая же ошибка прерывает чтение формы ({@link InputValidationException}).
 */
public final class CityFormReader {

    private final PrintStream out;

    public CityFormReader(PrintStream out) {
        this.out = out;
    }

    public CityData readCityData(InputSource source) throws IOException, InputValidationException {
        String name = readField(source, "Введите name:", false, raw -> {
            if (raw.isEmpty()) throw new IllegalArgumentException("name не может быть пустым");
            return raw;
        });

        Coordinates coordinates = readCoordinates(source);

        Double area = readField(source, "Введите area (Double > 0):", false, raw -> {
            double v = Double.parseDouble(raw);
            if (v <= 0) throw new IllegalArgumentException("area должно быть больше 0");
            return v;
        });

        Long population = readField(source, "Введите population (Long > 0):", false, raw -> {
            long v = Long.parseLong(raw);
            if (v <= 0) throw new IllegalArgumentException("population должно быть больше 0");
            return v;
        });

        Integer metersAboveSeaLevel = readField(source, "Введите metersAboveSeaLevel (int):", false,
                Integer::parseInt);

        Double timezone = readField(source, "Введите timezone (double, -13 < x <= 15):", false, raw -> {
            double v = Double.parseDouble(raw);
            if (v <= -13 || v > 15) throw new IllegalArgumentException("timezone должен быть в (-13; 15]");
            return v;
        });

        Integer carCode = readField(source, "Введите carCode (int, 0 < x <= 1000):", false, raw -> {
            int v = Integer.parseInt(raw);
            if (v <= 0 || v > 1000) throw new IllegalArgumentException("carCode должен быть в (0; 1000]");
            return v;
        });

        out.println("Допустимые значения standardOfLiving: " + Arrays.toString(StandardOfLiving.values()));
        StandardOfLiving sol = readField(source, "Введите standardOfLiving (пустая строка = null):", true,
                raw -> raw.isEmpty() ? null : StandardOfLiving.valueOf(raw));

        Human governor = readGovernor(source);

        return new CityData(name, coordinates, area, population, metersAboveSeaLevel,
                timezone, carCode, sol, governor);
    }

    private Coordinates readCoordinates(InputSource source) throws IOException, InputValidationException {
        Long x = readField(source, "Введите coordinates.x (long > -690):", false, raw -> {
            long v = Long.parseLong(raw);
            if (v <= -690) throw new IllegalArgumentException("coordinates.x должно быть больше -690");
            return v;
        });
        Float y = readField(source, "Введите coordinates.y (Float, не null):", false, raw -> {
            if (raw.isEmpty()) throw new IllegalArgumentException("coordinates.y не может быть пустым");
            return Float.parseFloat(raw);
        });
        return new Coordinates(x, y);
    }

    private Human readGovernor(InputSource source) throws IOException, InputValidationException {
        String name = readField(source, "Введите governor.name:", false, raw -> {
            if (raw.isEmpty()) throw new IllegalArgumentException("governor.name не может быть пустым");
            return raw;
        });
        return new Human(name);
    }

    private <T> T readField(InputSource source,
                            String prompt,
                            boolean allowEmpty,
                            Function<String, T> parser) throws IOException, InputValidationException {
        while (true) {
            if (source.isInteractive()) out.println(prompt);
            String line = source.readLine();
            if (line == null) throw new InputValidationException("ввод закончился прежде, чем были введены все поля");
            String trimmed = line.trim();
            try {
                if (trimmed.isEmpty() && !allowEmpty && !source.isInteractive()) {
                    throw new IllegalArgumentException("значение поля не может быть пустым");
                }
                return parser.apply(trimmed);
            } catch (IllegalArgumentException ex) {
                String msg = "Некорректный ввод: " + ex.getMessage();
                if (source.isInteractive()) {
                    out.println(msg + ". Повторите.");
                } else {
                    throw new InputValidationException(msg);
                }
            }
        }
    }
}
