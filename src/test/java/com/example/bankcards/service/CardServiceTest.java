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
import com.example.bankcards.service.CardService;
import com.example.bankcards.util.CardNumber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("CardService — юнит-тесты")
class CardServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CardTransactionRepository transactionRepository;

    @Mock
    private CardNumber cardNumber;

    @InjectMocks
    private CardService cardService;

    private User user;
    private Card card;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        user = new User(1L, "user1", "pass", null);

        card = new Card();
        card.setId(1L);
        card.setOwner(user);
        card.setEncryptedNumber("encrypted");
        card.setExpiryDate(LocalDate.now().plusYears(2));
        card.setBalance(BigDecimal.valueOf(1000));
        card.setStatus(CardStatus.ACTIVE);
        card.setBlockRequest(false);
        card.setCreatedAt(LocalDateTime.now());
    }

    @Nested
    @DisplayName("Тесты метода getMyCards()")
    class GetMyCardsTests {
        @Test
        void getMyCards_ShouldReturnPage() {
            when(cardRepository.findByOwnerUsername("user1", "", Pageable.unpaged()))
                    .thenReturn(new PageImpl<>(List.of(card)));
            when(cardNumber.getMasked(card.getEncryptedNumber())).thenReturn("**** **** **** 1234");

            Page<CardDTO> result = cardService.getMyCards("user1", null, Pageable.unpaged());

            assertThat(result).isNotEmpty();
            assertThat(result.getContent().get(0).getMaskNumber()).isEqualTo("**** **** **** 1234");
        }
    }

    @Nested
    @DisplayName("Тесты метода getMyCard()")
    class GetMyCardTests {
        @Test
        void getMyCard_Valid_ShouldReturnCardDTO() {
            when(cardRepository.findByIdAndOwnerUsername(1L, "user1")).thenReturn(Optional.of(card));
            when(cardNumber.getMasked(card.getEncryptedNumber())).thenReturn("**** **** **** 1234");

            CardDTO dto = cardService.getMyCard("user1", 1L);

            assertThat(dto.getMaskNumber()).isEqualTo("**** **** **** 1234");
        }

        @Test
        void getMyCard_NotFound_ShouldThrow() {
            when(cardRepository.findByIdAndOwnerUsername(1L, "user1")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cardService.getMyCard("user1", 1L))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessage("Карта не найдена");
        }
    }

    @Nested
    @DisplayName("Тесты метода transaction()")
    class TransactionTests {
        @Test
        void transaction_Valid_ShouldUpdateBalancesAndSave() {
            Card to = new Card();
            to.setId(2L);
            to.setOwner(user);
            to.setStatus(CardStatus.ACTIVE);
            to.setBalance(BigDecimal.valueOf(500));

            TransactionRequestDTO dto = new TransactionRequestDTO();
            dto.setFromCardId(1L);
            dto.setToCardId(2L);
            dto.setAmount(BigDecimal.valueOf(200));

            when(cardRepository.findByIdAndOwnerUsername(1L, "user1")).thenReturn(Optional.of(card));
            when(cardRepository.findByIdAndOwnerUsername(2L, "user1")).thenReturn(Optional.of(to));

            cardService.transaction("user1", dto);

            assertThat(card.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(800));
            assertThat(to.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(700));

            verify(transactionRepository).save(any(CardTransaction.class));
            verify(cardRepository, times(2)).save(any(Card.class));
        }

        @Test
        void transaction_FromCardInactive_ShouldThrow() {
            card.setStatus(CardStatus.BLOCKED);
            TransactionRequestDTO dto = new TransactionRequestDTO();
            dto.setFromCardId(1L);
            dto.setToCardId(2L);
            dto.setAmount(BigDecimal.valueOf(100));

            when(cardRepository.findByIdAndOwnerUsername(1L, "user1")).thenReturn(Optional.of(card));

            assertThatThrownBy(() -> cardService.transaction("user1", dto))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Карта списания недоступна");
        }

        @Test
        void transaction_InsufficientFunds_ShouldThrow() {
            Card to = new Card();
            to.setId(2L);
            to.setOwner(user);
            to.setStatus(CardStatus.ACTIVE);
            to.setBalance(BigDecimal.valueOf(500));

            TransactionRequestDTO dto = new TransactionRequestDTO();
            dto.setFromCardId(1L);
            dto.setToCardId(2L);
            dto.setAmount(BigDecimal.valueOf(2000));

            when(cardRepository.findByIdAndOwnerUsername(1L, "user1")).thenReturn(Optional.of(card));
            when(cardRepository.findByIdAndOwnerUsername(2L, "user1")).thenReturn(Optional.of(to));

            assertThatThrownBy(() -> cardService.transaction("user1", dto))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Недостаточно средств");
        }
    }

    @Nested
    @DisplayName("Тесты метода requestBlock()")
    class RequestBlockTests {
        @Test
        void requestBlock_Valid_ShouldSetBlockRequest() {
            when(cardRepository.findByIdAndOwnerUsername(1L, "user1")).thenReturn(Optional.of(card));

            cardService.requestBlock("user1", 1L);

            assertThat(card.isBlockRequest()).isTrue();
            verify(cardRepository).save(card);
        }

        @Test
        void requestBlock_AlreadyBlocked_ShouldThrow() {
            card.setStatus(CardStatus.BLOCKED);
            when(cardRepository.findByIdAndOwnerUsername(1L, "user1")).thenReturn(Optional.of(card));

            assertThatThrownBy(() -> cardService.requestBlock("user1", 1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Карта уже заблокирована");
        }
    }

    @Nested
    @DisplayName("Тесты метода getBalance()")
    class GetBalanceTests {
        @Test
        void getBalance_Valid_ShouldReturnBalance() {
            when(cardRepository.findByIdAndOwnerUsername(1L, "user1")).thenReturn(Optional.of(card));

            BigDecimal balance = cardService.getBalance("user1", 1L);

            assertThat(balance).isEqualByComparingTo(BigDecimal.valueOf(1000));
        }

        @Test
        void getBalance_NotFound_ShouldThrow() {
            when(cardRepository.findByIdAndOwnerUsername(1L, "user1")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cardService.getBalance("user1", 1L))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessage("Карта не найдена");
        }
    }

    @Nested
    @DisplayName("Тесты метода createCard()")
    class CreateCardTests {
        @Test
        void createCard_Valid_ShouldReturnCardDTO() {
            CardCreateDTO dto = new CardCreateDTO();
            dto.setOwner("user1");
            dto.setBalance(BigDecimal.valueOf(500));
            dto.setStatus(CardStatus.ACTIVE);

            when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user));
            when(cardNumber.generateRandomNumber()).thenReturn("1234123412341234");
            when(cardNumber.encrypt(anyString())).thenReturn("encrypted");
            when(cardNumber.getMasked("encrypted")).thenReturn("**** **** **** 1234");

            CardDTO result = cardService.createCard(dto);

            assertThat(result.getMaskNumber()).isEqualTo("**** **** **** 1234");
        }

        @Test
        void createCard_UserNotFound_ShouldThrow() {
            CardCreateDTO dto = new CardCreateDTO();
            dto.setOwner("user2");

            when(userRepository.findByUsername("user2")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cardService.createCard(dto))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessage("Пользователь не найден");
        }
    }

    @Nested
    @DisplayName("Тесты метода blockCard()")
    class BlockCardTests {

        @Test
        void blockCard_Valid_ShouldSetStatusBlocked() {
            card.setBlockRequest(true);
            when(cardRepository.findById(1L)).thenReturn(Optional.of(card));

            cardService.blockCard(1L);

            assertThat(card.getStatus()).isEqualTo(CardStatus.BLOCKED);
        }

        @Test
        void blockCard_NoRequest_ShouldThrow() {
            card.setBlockRequest(false);
            when(cardRepository.findById(1L)).thenReturn(Optional.of(card));

            assertThatThrownBy(() -> cardService.blockCard(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Пользователь не оставлял заявку на блокировку");
        }
    }

    @Nested
    @DisplayName("Тесты метода activateCard()")
    class ActivateCardTests {

        @Test
        void activateCard_Valid_ShouldSetStatusActive() {
            card.setStatus(CardStatus.BLOCKED);
            when(cardRepository.findById(1L)).thenReturn(Optional.of(card));

            cardService.activateCard(1L);

            assertThat(card.getStatus()).isEqualTo(CardStatus.ACTIVE);
        }

        @Test
        void activateCard_InvalidStatus_ShouldThrow() {
            card.setStatus(CardStatus.ACTIVE);
            when(cardRepository.findById(1L)).thenReturn(Optional.of(card));

            assertThatThrownBy(() -> cardService.activateCard(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Эту карту нельзя активировать");
        }
    }

    @Nested
    @DisplayName("Тесты метода deleteCard()")
    class DeleteCardTests {

        @Test
        void deleteCard_Valid_ShouldCallDelete() {
            when(cardRepository.findById(1L)).thenReturn(Optional.of(card));

            cardService.deleteCard(1L);

            verify(cardRepository).deleteById(1L);
        }

        @Test
        void deleteCard_NotFound_ShouldThrow() {
            when(cardRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cardService.deleteCard(1L))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessage("Карта не найдена");
        }
    }

    @Nested
    @DisplayName("Тесты метода  getAllCards()")
    class GetAllCardsTests {

        @Test
        void getAllCards_ShouldReturnPageOfDTOs() {
            when(cardRepository.findAll(Pageable.unpaged())).thenReturn(new PageImpl<>(List.of(card)));
            when(cardNumber.getMasked("encrypted")).thenReturn("**** **** **** 1234");

            Page<CardDTO> result = cardService.getAllCards(Pageable.unpaged());

            assertThat(result.getContent().get(0).getMaskNumber()).isEqualTo("**** **** **** 1234");
        }
    }
}

