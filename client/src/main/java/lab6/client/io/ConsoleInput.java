package lab6.client.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * Интерактивный консольный источник ввода (stdin).
 */
public final class ConsoleInput implements InputSource {
    private final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

    @Override
    public String readLine() throws IOException {
        return reader.readLine();
    }

    @Override
    public boolean isInteractive() {
        return true;
    }

    @Override
    public Path getBaseDirectory() {
        return Path.of(".").toAbsolutePath().normalize();
    }

    @Override
    public void close() {
        // Не закрываем System.in
    }
}
