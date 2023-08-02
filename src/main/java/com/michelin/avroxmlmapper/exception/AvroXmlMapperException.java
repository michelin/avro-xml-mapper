package com.michelin.avroxmlmapper.exception;

/**
 * Exception thrown when an error occurs during the mapping process.
 */
public class AvroXmlMapperException extends RuntimeException {
    /**
     * Default constructor
     *
     * @param message The message
     * @param cause   The cause
     */
    public AvroXmlMapperException(String message, Throwable cause) {
        super(message, cause);
    }
}
