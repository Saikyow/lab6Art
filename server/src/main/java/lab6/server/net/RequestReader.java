package lab6.server.net;

import lab6.common.net.Request;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * Модуль чтения запросов в неблокирующем режиме.
 * <p>Читает 4-байтный заголовок длины, затем тело такого же размера, после чего
 * десериализует {@link Request}. Возвращает {@code null}, если кадр ещё не дочитан.
 */
final class RequestReader {
    private static final Logger LOG = LogManager.getLogger(RequestReader.class);

    /**
     * Пытается прочитать готовый {@link Request} из канала.
     *
     * @return объект запроса, если кадр прочитан полностью; {@code null} — иначе
     * @throws ConnectionClosedException если клиент закрыл соединение
     * @throws IOException при сетевой ошибке
     */
    Request tryRead(SelectionKey key) throws IOException, ConnectionClosedException {
        SocketChannel channel = (SocketChannel) key.channel();
        ConnectionState state = (ConnectionState) key.attachment();

        if (state.body == null) {
            readInto(channel, state.header);
            if (state.header.hasRemaining()) return null;
            state.header.flip();
            int length = state.header.getInt();
            if (length <= 0 || length > 16 * 1024 * 1024) {
                throw new IOException("Невалидная длина сообщения: " + length);
            }
            state.body = ByteBuffer.allocate(length);
        }

        readInto(channel, state.body);
        if (state.body.hasRemaining()) return null;

        state.body.flip();
        byte[] bytes = new byte[state.body.remaining()];
        state.body.get(bytes);
        state.header.clear();
        state.body = null;

        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            Object obj = ois.readObject();
            if (!(obj instanceof Request request)) {
                throw new IOException("Ожидался Request, получен " + obj.getClass());
            }
            LOG.info("Получен запрос {} от {}", request.getCommand(), channel.getRemoteAddress());
            return request;
        } catch (ClassNotFoundException e) {
            throw new IOException("Неизвестный класс в сериализованных данных: " + e.getMessage(), e);
        }
    }

    private void readInto(SocketChannel channel, ByteBuffer buffer) throws IOException, ConnectionClosedException {
        int read = channel.read(buffer);
        if (read == -1) throw new ConnectionClosedException();
    }
}
