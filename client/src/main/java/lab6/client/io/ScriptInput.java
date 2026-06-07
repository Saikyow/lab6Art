package lab6.client.io;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * Источник ввода из файла-скрипта.
 */
public final class ScriptInput implements InputSource {
    private final BufferedReader reader;
    private final Path baseDirectory;

    public ScriptInput(Path scriptPath) throws IOException {
        this.reader = new BufferedReader(new FileReader(scriptPath.toFile(), StandardCharsets.UTF_8));
        Path parent = scriptPath.toAbsolutePath().getParent();
        this.baseDirectory = parent != null ? parent : Path.of(".").toAbsolutePath().normalize();
    }

    @Override
    public String readLine() throws IOException {
        return reader.readLine();
    }

    @Override
    public boolean isInteractive() {
        return false;
    }

    @Override
    public Path getBaseDirectory() {
        return baseDirectory;
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }
}
