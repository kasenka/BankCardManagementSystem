package com.example.bankcards.exception;

public class InvalidRefreshTokenException extends RuntimeException {
    public InvalidRefreshTokenException() {
        super("Невалидный refresh токен");
    }
}

