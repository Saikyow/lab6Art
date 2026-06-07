package lab6.server.net;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * Модуль приёма входящих TCP-подключений.
 * Принимает соединение в неблокирующем режиме и регистрирует канал в {@link Selector}.
 */
final class ConnectionAcceptor {
    private static final Logger LOG = LogManager.getLogger(ConnectionAcceptor.class);

    private final Selector selector;

    ConnectionAcceptor(Selector selector) {
        this.selector = selector;
    }

    void accept(ServerSocketChannel serverChannel) throws IOException {
        SocketChannel client = serverChannel.accept();
        if (client == null) return;
        client.configureBlocking(false);
        SelectionKey key = client.register(selector, SelectionKey.OP_READ, new ConnectionState());
        LOG.info("Принято подключение: {}", client.getRemoteAddress());
        if (LOG.isDebugEnabled()) {
            LOG.debug("Зарегистрирован OP_READ для {}", key.channel());
        }
    }
}
