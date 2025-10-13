package com.example.bankcards.exception;

public class UsernameNotUniqueException extends RuntimeException {
    public UsernameNotUniqueException(String username) {
        super("Пользователь с именем '" + username + "' уже существует");
    }
}