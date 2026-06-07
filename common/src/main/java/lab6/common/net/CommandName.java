package lab6.common.net;

/**
 * Имена команд, поддерживаемых клиент-серверным протоколом.
 * Серверная команда {@code save} отсутствует — она запускается из консоли сервера.
 */
public enum CommandName {
    HELP,
    INFO,
    SHOW,
    ADD,
    UPDATE,
    REMOVE_BY_ID,
    CLEAR,
    ADD_IF_MAX,
    REMOVE_GREATER,
    REMOVE_LOWER,
    MIN_BY_NAME,
    FILTER_BY_TIMEZONE,
    FILTER_LESS_THAN_STANDARD_OF_LIVING
}
