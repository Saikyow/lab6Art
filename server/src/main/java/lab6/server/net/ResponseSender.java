package lab6.server.net;

import lab6.common.net.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * Модуль отправки ответов клиенту в неблокирующем режиме.
 */
final class ResponseSender {
    private static final Logger LOG = LogManager.getLogger(ResponseSender.class);

    /**
     * Сериализует ответ и подготавливает его к отправке: ставит в {@code state.outgoing}
     * и переключает интерес ключа на {@link SelectionKey#OP_WRITE}.
     */
    void enqueue(SelectionKey key, Response response) throws IOException {
        ConnectionState state = (ConnectionState) key.attachment();
        byte[] payload = serialize(response);

        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES + payload.length);
        buffer.putInt(payload.length).put(payload).flip();
        state.outgoing = buffer;
        key.interestOps(SelectionKey.OP_WRITE);
        LOG.debug("Подготовлен ответ статуса {} ({} байт)", response.getStatus(), payload.length);
    }

    /**
     * Пытается отправить ожидающий ответ. По завершении сбрасывает {@code state.outgoing}
     * и возвращает ключ к чтению следующего запроса.
     */
    void tryFlush(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        ConnectionState state = (ConnectionState) key.attachment();
        if (state.outgoing == null) {
            key.interestOps(SelectionKey.OP_READ);
            return;
        }
        channel.write(state.outgoing);
        if (state.outgoing.hasRemaining()) return;
        LOG.info("Отправлен ответ клиенту {}", channel.getRemoteAddress());
        state.outgoing = null;
        key.interestOps(SelectionKey.OP_READ);
    }

    private byte[] serialize(Response response) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(response);
        }
        return baos.toByteArray();
    }
}
