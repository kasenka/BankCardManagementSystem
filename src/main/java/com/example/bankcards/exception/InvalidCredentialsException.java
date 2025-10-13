package com.example.bankcards.exception;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("Неверный юзернейм или пароль");
    }
}

