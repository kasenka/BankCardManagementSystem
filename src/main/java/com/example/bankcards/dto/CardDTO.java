package com.example.bankcards.dto;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CardDTO {
    private String maskNumber;
    private String owner;
    private LocalDate expiryDate;
    private CardStatus status;
    private BigDecimal balance;
    private Boolean blockRequest;
    private LocalDateTime createdAt;

    public CardDTO(Card card, String maskNumber){
        this.maskNumber = maskNumber;
        this.owner = String.valueOf(card.getOwner().getUsername());
        this.expiryDate = card.getExpiryDate();
        this.status = card.getStatus();
        this.balance = card.getBalance();
        this.blockRequest = card.isBlockRequest();
        this.createdAt = card.getCreatedAt();
    }
}
