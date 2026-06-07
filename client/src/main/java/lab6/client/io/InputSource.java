package lab6.client.io;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Источник ввода для клиента: интерактивная консоль или скрипт.
 * <p>В режиме скрипта приглашения к вводу не показываются и ошибка ввода прерывает выполнение.
 */
public interface InputSource extends AutoCloseable {
    /** Возвращает следующую строку или {@code null}, если ввод закончился. */
    String readLine() throws IOException;

    /** @return {@code true}, если источник — интерактивная консоль */
    boolean isInteractive();

    /** @return каталог для разрешения относительных путей внутри {@code execute_script} */
    Path getBaseDirectory();

    @Override
    void close() throws IOException;
}
