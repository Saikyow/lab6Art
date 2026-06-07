package lab6.client.io;

/**
 * Ошибка валидации пользовательского ввода (некорректный формат, выход за границы и т.п.).
 */
public class InputValidationException extends Exception {
    private static final long serialVersionUID = 1L;

    public InputValidationException(String message) {
        super(message);
    }
}
