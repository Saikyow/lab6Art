package lab6.common.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Координаты города.
 * <ul>
 *     <li>{@code x} — {@code long}, должно быть больше -690.</li>
 *     <li>{@code y} — {@code Float}, не может быть {@code null}.</li>
 * </ul>
 */
public final class Coordinates implements Serializable, Comparable<Coordinates> {
    private static final long serialVersionUID = 1L;

    private final long x;
    private final Float y;

    /**
     * Создаёт координаты.
     *
     * @param x координата X; должна быть больше -690
     * @param y координата Y; не {@code null}
     * @throws IllegalArgumentException при нарушении инвариантов
     */
    public Coordinates(long x, Float y) {
        if (x <= -690) {
            throw new IllegalArgumentException("coordinates.x должно быть больше -690");
        }
        if (y == null) {
            throw new IllegalArgumentException("coordinates.y не может быть null");
        }
        this.x = x;
        this.y = y;
    }

    /** @return координата X */
    public long getX() {
        return x;
    }

    /** @return координата Y */
    public Float getY() {
        return y;
    }

    @Override
    public int compareTo(Coordinates other) {
        int cmpX = Long.compare(this.x, other.x);
        if (cmpX != 0) return cmpX;
        return Float.compare(this.y, other.y);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Coordinates)) return false;
        Coordinates that = (Coordinates) o;
        return x == that.x && Objects.equals(y, that.y);
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return "Coordinates{x=" + x + ", y=" + y + "}";
    }
}
