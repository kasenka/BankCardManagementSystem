package com.example.bankcards.dto;

import com.example.bankcards.entity.CardStatus;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;


@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class CardCreateDTO {
    @NotNull
    private String owner;
    private CardStatus status = CardStatus.ACTIVE;
    private BigDecimal balance = BigDecimal.ZERO;
}
