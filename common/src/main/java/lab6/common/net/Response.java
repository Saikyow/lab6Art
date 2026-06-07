package lab6.common.net;

import java.io.Serializable;

/**
 * Сетевой ответ сервера.
 * Содержит код статуса, человекочитаемое сообщение и опциональную полезную нагрузку
 * (например, отсортированную коллекцию городов).
 */
public final class Response implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Код статуса ответа. */
    public enum Status { OK, ERROR }

    private final Status status;
    private final String message;
    private final Serializable payload;

    private Response(Status status, String message, Serializable payload) {
        this.status = status;
        this.message = message;
        this.payload = payload;
    }

    /** OK без полезной нагрузки. */
    public static Response ok(String message) {
        return new Response(Status.OK, message, null);
    }

    /** OK c полезной нагрузкой (например, списком городов). */
    public static Response ok(String message, Serializable payload) {
        return new Response(Status.OK, message, payload);
    }

    /** Ответ с признаком ошибки. */
    public static Response error(String message) {
        return new Response(Status.ERROR, message, null);
    }

    public Status getStatus() { return status; }
    public String getMessage() { return message; }
    public Serializable getPayload() { return payload; }

    public boolean isOk() { return status == Status.OK; }
}
