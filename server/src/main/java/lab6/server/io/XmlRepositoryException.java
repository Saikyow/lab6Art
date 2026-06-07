package lab6.server.io;

/**
 * Ошибки операций {@link XmlCityRepository} (чтение/запись/невалидная структура).
 */
public class XmlRepositoryException extends Exception {
    private static final long serialVersionUID = 1L;

    public XmlRepositoryException(String message) {
        super(message);
    }

    public XmlRepositoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
