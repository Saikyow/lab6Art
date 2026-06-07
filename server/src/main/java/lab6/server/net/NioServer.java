package lab6.server.net;

import lab6.common.net.Request;
import lab6.common.net.Response;
import lab6.server.commands.CommandDispatcher;
import lab6.server.core.CollectionManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Однопоточный неблокирующий TCP-сервер на {@link Selector}/{@link ServerSocketChannel}.
 * <p>В одном цикле обслуживает:
 * <ul>
 *     <li>сетевые события (accept/read/write) на сокете;</li>
 *     <li>пользовательский ввод сервера (через callback {@code stdinPoller}),
 *         что нужно для серверной команды {@code save}.</li>
 * </ul>
 */
public final class NioServer {
    private static final Logger LOG = LogManager.getLogger(NioServer.class);
    private static final long SELECT_TIMEOUT_MS = 200L;

    private final int port;
    private final CollectionManager manager;
    private final CommandDispatcher dispatcher;
    private final ConnectionAcceptor acceptor;
    private final RequestReader reader = new RequestReader();
    private final ResponseSender sender = new ResponseSender();

    private Selector selector;
    private ServerSocketChannel serverChannel;

    public NioServer(int port, CollectionManager manager, CommandDispatcher dispatcher) throws IOException {
        this.port = port;
        this.manager = manager;
        this.dispatcher = dispatcher;
        this.selector = Selector.open();
        this.acceptor = new ConnectionAcceptor(selector);
    }

    /**
     * Запускает сервер. Цикл выполняется до тех пор, пока {@code keepRunning.getAsBoolean()}
     * возвращает {@code true}. После каждого {@code select} вызывается {@code stdinPoller}
     * (для обработки команд серверной консоли).
     */
    public void run(BooleanSupplier keepRunning, Consumer<NioServer> stdinPoller) throws IOException {
        serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        serverChannel.bind(new InetSocketAddress(port));
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        LOG.info("Сервер слушает порт {}", port);

        while (keepRunning.getAsBoolean()) {
            selector.select(SELECT_TIMEOUT_MS);
            Iterator<SelectionKey> it = selector.selectedKeys().iterator();
            while (it.hasNext()) {
                SelectionKey key = it.next();
                it.remove();
                if (!key.isValid()) continue;
                try {
                    if (key.isAcceptable()) {
                        acceptor.accept(serverChannel);
                    } else if (key.isReadable()) {
                        handleRead(key);
                    } else if (key.isWritable()) {
                        sender.tryFlush(key);
                    }
                } catch (ConnectionClosedException closed) {
                    closeQuietly(key, "клиент закрыл соединение");
                } catch (IOException io) {
                    LOG.warn("Сетевая ошибка: {}", io.getMessage());
                    closeQuietly(key, "сетевая ошибка");
                }
            }
            stdinPoller.accept(this);
        }
        shutdown();
    }

    private void handleRead(SelectionKey key) throws IOException, ConnectionClosedException {
        Request request = reader.tryRead(key);
        if (request == null) return;
        Response response = dispatcher.dispatch(request, manager);
        sender.enqueue(key, response);
    }

    private void closeQuietly(SelectionKey key, String reason) {
        try {
            SocketChannel channel = (SocketChannel) key.channel();
            LOG.info("Закрытие соединения {} ({})", channel.getRemoteAddress(), reason);
            channel.close();
        } catch (IOException ignored) {
        }
        key.cancel();
    }

    private void shutdown() {
        try { if (serverChannel != null) serverChannel.close(); } catch (IOException ignored) {}
        try { if (selector != null) selector.close(); } catch (IOException ignored) {}
        LOG.info("Сервер остановлен");
    }
}
