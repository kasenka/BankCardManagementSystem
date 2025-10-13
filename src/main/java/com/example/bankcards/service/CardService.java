package com.example.bankcards.service;


import com.example.bankcards.dto.CardCreateDTO;
import com.example.bankcards.dto.CardDTO;
import com.example.bankcards.dto.TransactionRequestDTO;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.CardTransaction;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.exception.ConflictErrorException;
import com.example.bankcards.exception.NotEnoughMoneyException;
import com.example.bankcards.exception.UserNotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.CardTransactionRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.CardNumber;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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
                .orElseThrow(() -> new CardNotFoundException(""));
        return new CardDTO(card, cardNumber.getMasked(card.getEncryptedNumber()));
    }

    @Transactional
    public void transaction(String username, TransactionRequestDTO dto) {
        Card from = cardRepository.findByIdAndOwnerUsername(dto.getFromCardId(), username)
                .orElseThrow(() -> new CardNotFoundException("Карта списания не найдена"));

        if (from.getStatus() != CardStatus.ACTIVE){
            throw new ConflictErrorException("Карта списания недоступна");
        }

        Card to = cardRepository.findByIdAndOwnerUsername(dto.getToCardId(), username)
                .orElseThrow(() -> new CardNotFoundException("Карта назначения не найдена"));

        if (to.getStatus() != CardStatus.ACTIVE){
            throw new ConflictErrorException("Карта назначения недоступна");
        }

        if (from.getBalance().compareTo(dto.getAmount()) < 0) {
            throw new NotEnoughMoneyException();
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

    @Transactional
    public void requestBlock(String username, Long cardId) {
        Card card = cardRepository.findByIdAndOwnerUsername(cardId, username)
                .orElseThrow(() -> new CardNotFoundException(""));
        if (card.getStatus() == CardStatus.BLOCKED){
            throw new ConflictErrorException("Карта уже заблокирована");
        }
        card.setBlockRequest(true);
        cardRepository.save(card);
    }

    public BigDecimal getBalance(String username, Long cardId) {
        return cardRepository.findByIdAndOwnerUsername(cardId, username)
                .orElseThrow(() -> new CardNotFoundException(""))
                .getBalance();
    }

    @Transactional
    public CardDTO createCard(CardCreateDTO dto) {
        User owner = userRepository.findByUsername(dto.getOwner())
                .orElseThrow(() -> new UserNotFoundException(dto.getOwner()));

        Card card = new Card();
        card.setEncryptedNumber(cardNumber.encrypt(cardNumber.generateRandomNumber()));
        card.setExpiryDate(LocalDate.now().plusYears(2));
        card.setBalance(dto.getBalance());
        card.setOwner(owner);
        card.setStatus(dto.getStatus());

        cardRepository.save(card);

        return new CardDTO(card, cardNumber.getMasked(card.getEncryptedNumber()));
    }

    @Transactional
    public void blockCard(Long id) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new CardNotFoundException(""));

        if (!card.isBlockRequest()){
            throw new ConflictErrorException("Пользователь не оставлял заявку на блокировку");
        }
        card.setStatus(CardStatus.BLOCKED);
        cardRepository.save(card);
    }

    @Transactional
    public void activateCard(Long id) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new CardNotFoundException(""));

        if (card.getStatus() != CardStatus.BLOCKED){
            throw new ConflictErrorException("Эту карту нельзя активировать");
        }
        card.setStatus(CardStatus.ACTIVE);
        cardRepository.save(card);
    }

    @Transactional
    public void deleteCard(Long id) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new CardNotFoundException(""));

        cardRepository.deleteById(id);
    }

    public Page<CardDTO> getAllCards(Pageable pageable) {
        return cardRepository.findAll(pageable)
                .map(c -> new CardDTO(c, cardNumber.getMasked(c.getEncryptedNumber())));
    }
}
