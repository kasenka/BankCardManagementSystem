package com.example.bankcards.service;


import com.example.bankcards.entity.RefreshToken;
import com.example.bankcards.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class RefreshTokenService {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    public void saveRefreshToken(String refreshToken, String username, LocalDateTime expiryDate) {
        RefreshToken token = new RefreshToken(refreshToken, username, expiryDate);
        refreshTokenRepository.save(token);

    }

    public boolean isValid(String token){
        Optional<RefreshToken> refreshToken = refreshTokenRepository.findByToken(token);

        if (refreshToken.isEmpty()) {
            System.out.println("------------------------------");
            return false;
        }

        if (refreshToken.get().getExpiryDate().isBefore(LocalDateTime.now())){
            System.out.println("111111121213312312312313123123");
            refreshTokenRepository.deleteByToken(token);
            return false;
        }
        return true;
    }

    public String getUsernameByToken(String token){
        return refreshTokenRepository.findByToken(token).get().getUsername();
    }

    public void deleteRefreshToken(String token){
        refreshTokenRepository.deleteByToken(token);
    }
}


