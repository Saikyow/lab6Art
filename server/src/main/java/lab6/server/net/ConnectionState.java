package lab6.server.net;

import java.nio.ByteBuffer;

/**
 * Состояние одной клиентской TCP-сессии для неблокирующего ввода-вывода.
 * <p>Кадры сообщений: 4-байтный заголовок (BigEndian {@code int}) + тело {@code length} байт.
 */
final class ConnectionState {
    /** Заголовок входящего сообщения (длина тела). */
    final ByteBuffer header = ByteBuffer.allocate(Integer.BYTES);
    /** Тело входящего сообщения (выделяется после получения заголовка). */
    ByteBuffer body;
    /** Готовый к отправке ответ (заголовок + тело уже скомпонованы). */
    ByteBuffer outgoing;
}
