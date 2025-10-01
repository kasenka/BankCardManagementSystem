package com.example.bankcards.controller;


import com.example.bankcards.dto.CardCreateDTO;
import com.example.bankcards.dto.CardDTO;
import com.example.bankcards.dto.TransactionRequestDTO;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.service.CardService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Комплексные тесты для CardController")
class CardControllerMockMvcTest {

    @Resource
    private MockMvc mockMvc;

    @MockBean
    private CardService cardService;

    @Nested
    @DisplayName("Пользовательские эндпоинты")
    class UserEndpointsTests {

        @Test
        @WithMockUser(username = "user1", roles = "USER")
        void getMyCards_ShouldReturn200() throws Exception {
            Card card = new Card();
            card.setId(1L);
            card.setOwner(new User(1l,"user1", "encodedPass", Role.USER));
            card.setExpiryDate(LocalDate.now().plusYears(3));
            card.setStatus(CardStatus.ACTIVE);
            card.setBalance(BigDecimal.valueOf(1000));
            card.setBlockRequest(false);
            card.setCreatedAt(LocalDateTime.now());

            CardDTO dto = new CardDTO(card, "**** **** **** 1234");

            when(cardService.getMyCards(eq("user1"), any(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(dto)));

            mockMvc.perform(get("/api/cards")
                            .param("search", "")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].owner").value("user1"))
                    .andExpect(jsonPath("$.content[0].maskNumber").value("**** **** **** 1234"))
                    .andDo(print());
        }

        @Test
        @WithMockUser(username = "user1", roles = "USER")
        void getMyCard_Valid_ShouldReturn200() throws Exception {
            Card card = new Card();
            card.setId(1L);
            card.setOwner(new User(1l,"user1", "encodedPass", Role.USER));
            card.setExpiryDate(LocalDate.now().plusYears(3));
            card.setStatus(CardStatus.ACTIVE);
            card.setBalance(BigDecimal.valueOf(500));
            card.setBlockRequest(false);
            card.setCreatedAt(LocalDateTime.now());

            CardDTO dto = new CardDTO(card, "**** **** **** 5678");

            when(cardService.getMyCard("user1", 1L)).thenReturn(dto);

            mockMvc.perform(get("/api/cards/1")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.owner").value("user1"))
                    .andExpect(jsonPath("$.maskNumber").value("**** **** **** 5678"))
                    .andDo(print());
        }

        @Test
        @WithMockUser(username = "user1", roles = "USER")
        void transfer_Valid_ShouldReturn200() throws Exception {
            TransactionRequestDTO dto = new TransactionRequestDTO();
            dto.setFromCardId(1L);
            dto.setToCardId(2L);
            dto.setAmount(BigDecimal.valueOf(100));

            String json = """
                    {"fromCardId":1,"toCardId":2,"amount":100,"description":"test"}
                    """;

            doNothing().when(cardService).transaction(eq("user1"), any(TransactionRequestDTO.class));

            mockMvc.perform(post("/api/cards/transaction")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Перевод выполнен"))
                    .andDo(print());
        }

        @Test
        @WithMockUser(username = "user1", roles = "USER")
        void getBalance_Valid_ShouldReturn200() throws Exception {
            when(cardService.getBalance("user1", 1L)).thenReturn(BigDecimal.valueOf(999));

            mockMvc.perform(get("/api/cards/1/balance")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.balance").value(999))
                    .andDo(print());
        }
    }


    @Nested
    @DisplayName("Админские эндпоинты")
    class AdminEndpointsTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        void createCard_Valid_ShouldReturn201() throws Exception {
            CardCreateDTO dto = new CardCreateDTO();
            dto.setOwner("user1");
            dto.setBalance(BigDecimal.valueOf(100));

            Card card = new Card();
            card.setId(1L);
            card.setOwner(new User(1l,"user1", "pass", Role.USER));
            card.setExpiryDate(LocalDate.now().plusYears(3));
            card.setStatus(CardStatus.ACTIVE);
            card.setBalance(BigDecimal.valueOf(100));
            card.setBlockRequest(false);
            card.setCreatedAt(LocalDateTime.now());

            CardDTO response = new CardDTO(card, "**** **** **** 4321");

            when(cardService.createCard(any(CardCreateDTO.class))).thenReturn(response);

            String json = """
                    {"owner":"user1","balance":100}
                    """;

            mockMvc.perform(post("/api/cards")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.owner").value("user1"))
                    .andExpect(jsonPath("$.maskNumber").value("**** **** **** 4321"))
                    .andDo(print());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void blockCard_Valid_ShouldReturn200() throws Exception {
            doNothing().when(cardService).blockCard(1L);

            mockMvc.perform(patch("/api/cards/1/block"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Карта заблокирована"))
                    .andDo(print());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void activateCard_Valid_ShouldReturn200() throws Exception {
            doNothing().when(cardService).activateCard(1L);

            mockMvc.perform(patch("/api/cards/1/activate"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Карта активирована"))
                    .andDo(print());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void deleteCard_Valid_ShouldReturn204() throws Exception {
            doNothing().when(cardService).deleteCard(1L);

            mockMvc.perform(delete("/api/cards/1"))
                    .andExpect(status().isNoContent())
                    .andDo(print());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void getAllCards_ShouldReturn200() throws Exception {
            Card card = new Card();
            card.setId(1L);
            card.setOwner(new User(1l,"user1", "encodedPass", Role.USER));
            card.setExpiryDate(LocalDate.now().plusYears(3));
            card.setStatus(CardStatus.ACTIVE);
            card.setBalance(BigDecimal.valueOf(1000));
            card.setBlockRequest(false);
            card.setCreatedAt(LocalDateTime.now());

            CardDTO dto = new CardDTO(card, "**** **** **** 1111");

            when(cardService.getAllCards(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(dto)));

            mockMvc.perform(get("/api/cards/all"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].owner").value("user1"))
                    .andDo(print());
        }
    }
}
