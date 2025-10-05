package com.example.bankcards.service;


import com.example.bankcards.dto.CardCreateDTO;
import com.example.bankcards.dto.CardDTO;
import com.example.bankcards.dto.TransactionRequestDTO;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.CardTransaction;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.CardTransactionRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.CardNumber;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;

@Service
public class CardService {

    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final CardTransactionRepository transactionRepository;
    private CardNumber cardNumber;

    public CardService(CardRepository cardRepository,
                       UserRepository userRepository,
                       CardTransactionRepository transactionRepository,
                       CardNumber cardNumber) {
        this.cardRepository = cardRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.cardNumber = cardNumber;
    }


    public Page<CardDTO> getMyCards(String username, String search, Pageable pageable) {
        String searchTerm = (search == null) ? "" : search;

        return cardRepository.findByOwnerUsername(username, searchTerm, pageable)
                .map(c -> new CardDTO(c, cardNumber.getMasked(c.getEncryptedNumber())));
    }

    public CardDTO getMyCard(String username, Long cardId) {
        Card card = cardRepository.findByIdAndOwnerUsername(cardId, username)
                .orElseThrow(() -> new NoSuchElementException("Карта не найдена"));
        return new CardDTO(card, cardNumber.getMasked(card.getEncryptedNumber()));
    }

    public void transaction(String username, TransactionRequestDTO dto) {
        Card from = cardRepository.findByIdAndOwnerUsername(dto.getFromCardId(), username)
                .orElseThrow(() -> new IllegalStateException("Карта списания не найдена"));

        if (from.getStatus() != CardStatus.ACTIVE){
            throw new IllegalStateException("Карта списания недоступна");
        }

        Card to = cardRepository.findByIdAndOwnerUsername(dto.getToCardId(), username)
                .orElseThrow(() -> new NoSuchElementException("Карта назначения не найдена"));

        if (to.getStatus() != CardStatus.ACTIVE){
            throw new IllegalStateException("Карта назначения недоступна");
        }

        if (from.getBalance().compareTo(dto.getAmount()) < 0) {
            throw new IllegalStateException("Недостаточно средств");
        }

        from.setBalance(from.getBalance().subtract(dto.getAmount()));
        to.setBalance(to.getBalance().add(dto.getAmount()));

        cardRepository.save(from);
        cardRepository.save(to);

        CardTransaction transaction = new CardTransaction();
        transaction.setFromCard(from);
        transaction.setToCard(to);
        transaction.setAmount(dto.getAmount());
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setDescription(dto.getDescription());

        transactionRepository.save(transaction);
    }

    public void requestBlock(String username, Long cardId) {
        Card card = cardRepository.findByIdAndOwnerUsername(cardId, username)
                .orElseThrow(() -> new NoSuchElementException("Карта не найдена"));
        if (card.getStatus() == CardStatus.BLOCKED){
            throw new IllegalStateException("Карта уже заблокирована");
        }
        card.setBlockRequest(true);
        cardRepository.save(card);
    }

    public BigDecimal getBalance(String username, Long cardId) {
        return cardRepository.findByIdAndOwnerUsername(cardId, username)
                .orElseThrow(() -> new NoSuchElementException("Карта не найдена"))
                .getBalance();
    }


    public CardDTO createCard(CardCreateDTO dto) {
        User owner = userRepository.findByUsername(dto.getOwner())
                .orElseThrow(() -> new NoSuchElementException("Пользователь не найден"));

        Card card = new Card();
        card.setEncryptedNumber(cardNumber.encrypt(cardNumber.generateRandomNumber()));
        card.setExpiryDate(LocalDate.now().plusYears(2));
        card.setBalance(dto.getBalance());
        card.setOwner(owner);
        card.setStatus(dto.getStatus());

        cardRepository.save(card);

        return new CardDTO(card, cardNumber.getMasked(card.getEncryptedNumber()));
    }

    public void blockCard(Long id) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Карта не найдена"));

        if (!card.isBlockRequest()){
            throw new IllegalStateException("Пользователь не оставлял заявку на блокировку");
        }
        card.setStatus(CardStatus.BLOCKED);
        cardRepository.save(card);
    }

    public void activateCard(Long id) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Карта не найдена"));

        if (card.getStatus() != CardStatus.BLOCKED){
            throw new IllegalStateException("Эту карту нельзя активировать");
        }
        card.setStatus(CardStatus.ACTIVE);
        cardRepository.save(card);
    }

    public void deleteCard(Long id) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Карта не найдена"));

        cardRepository.deleteById(id);
    }

    public Page<CardDTO> getAllCards(Pageable pageable) {
        return cardRepository.findAll(pageable).map(c -> new CardDTO(c, cardNumber.getMasked(c.getEncryptedNumber())));
    }
}
