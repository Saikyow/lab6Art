package lab6.client.input;

/**
 * Имена команд, которые принимает клиент в интерактивном режиме.
 * Команда {@code save} отсутствует — это требование ТЗ ЛР6.
 */
public enum ClientCommand {
    HELP("help"),
    INFO("info"),
    SHOW("show"),
    ADD("add"),
    UPDATE("update"),
    REMOVE_BY_ID("remove_by_id"),
    CLEAR("clear"),
    EXECUTE_SCRIPT("execute_script"),
    EXIT("exit"),
    ADD_IF_MAX("add_if_max"),
    REMOVE_GREATER("remove_greater"),
    REMOVE_LOWER("remove_lower"),
    MIN_BY_NAME("min_by_name"),
    FILTER_BY_TIMEZONE("filter_by_timezone"),
    FILTER_LESS_THAN_STANDARD_OF_LIVING("filter_less_than_standard_of_living");

    public final String token;

    ClientCommand(String token) {
        this.token = token;
    }

    public static ClientCommand fromToken(String token) {
        for (ClientCommand cmd : values()) {
            if (cmd.token.equalsIgnoreCase(token)) return cmd;
        }
        return null;
    }
}
