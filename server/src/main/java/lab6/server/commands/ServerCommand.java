package lab6.server.commands;

import lab6.common.net.Request;
import lab6.common.net.Response;
import lab6.server.core.CollectionManager;

/**
 * Команда, выполняемая на стороне сервера.
 */
@FunctionalInterface
public interface ServerCommand {
    /**
     * Выполнить команду.
     *
     * @param request запрос клиента
     * @param manager менеджер коллекции
     * @return ответ для отправки клиенту
     */
    Response execute(Request request, CollectionManager manager);
}
