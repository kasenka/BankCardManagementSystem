package com.example.bankcards.exception;

public class CardNotFoundException extends RuntimeException {
    public CardNotFoundException(String message) {
        super("".equals(message)? "Карта не найдена" : message);
    }
}
