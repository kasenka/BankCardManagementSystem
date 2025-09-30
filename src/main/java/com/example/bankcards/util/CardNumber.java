package com.example.bankcards.util;

import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.Random;
import java.util.UUID;

@Component
public class CardNumber {

    private final Random random = new Random();

    // Генерация случайного 16-значного номера
    public String generateRandomNumber() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            sb.append(random.nextInt(10)); // 0..9
        }
        return sb.toString();
    }

    // Простое "шифрование" Base64
    public String encrypt(String rawNumber) {
        return Base64.getEncoder().encodeToString(rawNumber.getBytes());
    }

    // Получить номер в маскированном виде **** **** **** 1234
    public String getMasked(String encrypted) {
        String raw = new String(Base64.getDecoder().decode(encrypted));
        return "**** **** **** " + raw.substring(raw.length() - 4);
    }

    // Вернуть расшифрованный номер (при необходимости)
    public String getRaw(String encrypted) {
        return new String(Base64.getDecoder().decode(encrypted));
    }
}

