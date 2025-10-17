package com.example.bankcards.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String username) {
        super("".equals(username)? "Пользователь не найден" : "Пользователь " + username + " не найден");
    }
}
