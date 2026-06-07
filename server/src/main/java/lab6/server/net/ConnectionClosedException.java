package lab6.server.net;

/**
 * Сигнальное исключение: клиент закрыл соединение со своей стороны (read вернул -1).
 */
final class ConnectionClosedException extends Exception {
    private static final long serialVersionUID = 1L;
}
