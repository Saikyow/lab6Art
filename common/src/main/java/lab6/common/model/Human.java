package lab6.common.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Человек (губернатор города).
 * По ТЗ содержит только имя.
 */
public final class Human implements Serializable, Comparable<Human> {
    private static final long serialVersionUID = 1L;

    private final String name;

    /**
     * Создаёт объект человека.
     *
     * @param name имя; не {@code null} и не пустая строка
     * @throws IllegalArgumentException если имя пустое или {@code null}
     */
    public Human(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("governor.name не может быть пустым");
        }
        this.name = name.trim();
    }

    /** @return имя */
    public String getName() {
        return name;
    }

    @Override
    public int compareTo(Human other) {
        return String.CASE_INSENSITIVE_ORDER.compare(this.name, other.name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Human)) return false;
        return name.equals(((Human) o).name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "Human{name='" + name + "'}";
    }
}
