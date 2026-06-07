package lab6.client.net;

import lab6.common.net.Request;
import lab6.common.net.Response;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * TCP-клиент. Использует потоки ввода-вывода поверх {@link Socket}
 * (по требованию ТЗ — на клиенте именно потоки, а не каналы).
 * <p>Соединение ленивое: открывается при первой команде. При обрыве — попытка
 * переподключения; внешний код может выполнять {@code send} в цикле с задержкой
 * для устойчивости к временной недоступности сервера.
 */
public final class TcpClient implements AutoCloseable {

    private final String host;
    private final int port;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    public TcpClient(String host, int port, int connectTimeoutMs, int readTimeoutMs) {
        this.host = host;
        this.port = port;
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
    }

    /**
     * Отправляет запрос и блокирующе ждёт ответ.
     *
     * @throws IOException если соединение не удалось установить или прервалось
     */
    public Response send(Request request) throws IOException {
        ensureConnected();
        try {
            writeFrame(serialize(request));
            byte[] respBytes = readFrame();
            return deserialize(respBytes);
        } catch (IOException e) {
            closeQuietly();
            throw e;
        }
    }

    /** Принудительно переоткрывает соединение при следующем {@code send}. */
    public void resetConnection() {
        closeQuietly();
    }

    private void ensureConnected() throws IOException {
        if (socket != null && socket.isConnected() && !socket.isClosed()) return;
        Socket s = new Socket();
        s.connect(new InetSocketAddress(host, port), connectTimeoutMs);
        s.setSoTimeout(readTimeoutMs);
        this.socket = s;
        this.in = new DataInputStream(s.getInputStream());
        this.out = new DataOutputStream(s.getOutputStream());
    }

    private void writeFrame(byte[] payload) throws IOException {
        out.writeInt(payload.length);
        out.write(payload);
        out.flush();
    }

    private byte[] readFrame() throws IOException {
        int length;
        try {
            length = in.readInt();
        } catch (SocketTimeoutException timeout) {
            throw new IOException("Сервер не ответил вовремя.", timeout);
        }
        if (length <= 0 || length > 16 * 1024 * 1024) {
            throw new IOException("Невалидная длина ответа: " + length);
        }
        byte[] body = new byte[length];
        in.readFully(body);
        return body;
    }

    private byte[] serialize(Request request) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(request);
        }
        return baos.toByteArray();
    }

    private Response deserialize(byte[] bytes) throws IOException {
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            Object obj = ois.readObject();
            if (!(obj instanceof Response response)) {
                throw new IOException("Ожидался Response, получен " + obj.getClass());
            }
            return response;
        } catch (ClassNotFoundException e) {
            throw new IOException("Неизвестный класс в сериализованных данных: " + e.getMessage(), e);
        }
    }

    private void closeQuietly() {
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
        socket = null;
        in = null;
        out = null;
    }

    @Override
    public void close() {
        closeQuietly();
    }
}
