package com.example.bankcards.controller;

import com.example.bankcards.dto.CardCreateDTO;
import com.example.bankcards.dto.CardDTO;
import com.example.bankcards.dto.TransactionRequestDTO;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.service.CardService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.annotation.Resource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Комплексные тесты для CardController")
class CardControllerTest {

    @Resource
    private MockMvc mockMvc;

    @MockBean
    private CardService cardService;

    private Card buildCard(String username, long id) {
        Card card = new Card();
        card.setId(id);
        card.setOwner(new User(1L, username, "pass", Role.USER));
        card.setExpiryDate(LocalDate.now().plusYears(3));
        card.setStatus(CardStatus.ACTIVE);
        card.setBalance(BigDecimal.valueOf(1000));
        card.setBlockRequest(false);
        card.setCreatedAt(LocalDateTime.now());
        return card;
    }

    // ---------------------- USER ----------------------

    @Nested
    @DisplayName("Пользовательские эндпоинты")
    class UserEndpoints {

        @Test
        @WithMockUser(username = "user1", roles = "USER")
        void getMyCards_Success() throws Exception {
            CardDTO dto = new CardDTO(buildCard("user1", 1L), "**** 1111");
            when(cardService.getMyCards(eq("user1"), any(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(dto)));

            mockMvc.perform(get("/api/cards"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].owner").value("user1"))
                    .andExpect(jsonPath("$.content[0].maskNumber").value("**** 1111"))
                    .andDo(print());
        }

        @Test
        @WithMockUser(username = "user1", roles = "USER")
        void getMyCard_Success() throws Exception {
            CardDTO dto = new CardDTO(buildCard("user1", 1L), "**** 2222");
            when(cardService.getMyCard("user1", 1L)).thenReturn(dto);

            mockMvc.perform(get("/api/cards/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.maskNumber").value("**** 2222"))
                    .andDo(print());
        }

        @Test
        @WithMockUser(username = "user1", roles = "USER")
        void getMyCard_NotFound() throws Exception {
            when(cardService.getMyCard("user1", 99L))
                    .thenThrow(new NoSuchElementException("Карта не найдена"));

            mockMvc.perform(get("/api/cards/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("Карта не найдена"))
                    .andDo(print());
        }

        @Test
        @WithMockUser(username = "user1", roles = "USER")
        void transfer_Success() throws Exception {
            doNothing().when(cardService).transaction(eq("user1"), any(TransactionRequestDTO.class));

            String json = """
                    {"fromCardId":1,"toCardId":2,"amount":100,"description":"ok"}
                    """;

            mockMvc.perform(post("/api/cards/transaction")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Перевод выполнен"))
                    .andDo(print());
        }

        @Test
        @WithMockUser(username = "user1", roles = "USER")
        void transfer_Fail_NotEnoughMoney() throws Exception {
            doThrow(new IllegalStateException("Недостаточно средств"))
                    .when(cardService).transaction(eq("user1"), any(TransactionRequestDTO.class));

            String json = """
                    {"fromCardId":1,"toCardId":2,"amount":99999}
                    """;

            mockMvc.perform(post("/api/cards/transaction")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Недостаточно средств"))
                    .andDo(print());
        }

        @Test
        @WithMockUser(username = "user1", roles = "USER")
        void requestBlock_Success() throws Exception {
            doNothing().when(cardService).requestBlock("user1", 1L);

            mockMvc.perform(post("/api/cards/1/block-request"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Запрос на блокировку отправлен"))
                    .andDo(print());
        }

        @Test
        @WithMockUser(username = "user1", roles = "USER")
        void requestBlock_NotFound() throws Exception {
            doThrow(new NoSuchElementException("Карта не найдена"))
                    .when(cardService).requestBlock("user1", 1L);

            mockMvc.perform(post("/api/cards/1/block-request"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("Карта не найдена"))
                    .andDo(print());
        }

        @Test
        @WithMockUser(username = "user1", roles = "USER")
        void requestBlock_BadRequest() throws Exception {
            doThrow(new IllegalStateException("Карта уже в блокировке"))
                    .when(cardService).requestBlock("user1", 1L);

            mockMvc.perform(post("/api/cards/1/block-request"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Карта уже в блокировке"))
                    .andDo(print());
        }

        @Test
        @WithMockUser(username = "user1", roles = "USER")
        void getBalance_Success() throws Exception {
            when(cardService.getBalance("user1", 1L)).thenReturn(BigDecimal.valueOf(500));

            mockMvc.perform(get("/api/cards/1/balance"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.balance").value(500))
                    .andDo(print());
        }

        @Test
        @WithMockUser(username = "user1", roles = "USER")
        void getBalance_NotFound() throws Exception {
            when(cardService.getBalance("user1", 1L))
                    .thenThrow(new NoSuchElementException("Карта не найдена"));

            mockMvc.perform(get("/api/cards/1/balance"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("Карта не найдена"))
                    .andDo(print());
        }
    }

    // ---------------------- ADMIN ----------------------

    @Nested
    @DisplayName("Админские эндпоинты")
    class AdminEndpoints {

        @Test
        @WithMockUser(roles = "ADMIN")
        void createCard_Success() throws Exception {
            CardDTO dto = new CardDTO(buildCard("user1", 1L), "**** 3333");
            when(cardService.createCard(any(CardCreateDTO.class))).thenReturn(dto);

            String json = """
                    {"owner":"user1","balance":100}
                    """;

            mockMvc.perform(post("/api/cards")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.maskNumber").value("**** 3333"))
                    .andDo(print());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void createCard_NotFoundOwner() throws Exception {
            when(cardService.createCard(any(CardCreateDTO.class)))
                    .thenThrow(new NoSuchElementException("Владелец не найден"));

            String json = """
                    {"owner":"ghost","balance":100}
                    """;

            mockMvc.perform(post("/api/cards")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("Владелец не найден"))
                    .andDo(print());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void blockCard_Success() throws Exception {
            doNothing().when(cardService).blockCard(1L);

            mockMvc.perform(patch("/api/cards/1/block"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Карта заблокирована"))
                    .andDo(print());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void blockCard_NotFound() throws Exception {
            doThrow(new NoSuchElementException("Карта не найдена"))
                    .when(cardService).blockCard(99L);

            mockMvc.perform(patch("/api/cards/99/block"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("Карта не найдена"))
                    .andDo(print());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void blockCard_BadRequest() throws Exception {
            doThrow(new IllegalStateException("Уже заблокирована"))
                    .when(cardService).blockCard(1L);

            mockMvc.perform(patch("/api/cards/1/block"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Уже заблокирована"))
                    .andDo(print());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void activateCard_Success() throws Exception {
            doNothing().when(cardService).activateCard(1L);

            mockMvc.perform(patch("/api/cards/1/activate"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Карта активирована"))
                    .andDo(print());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void activateCard_NotFound() throws Exception {
            doThrow(new NoSuchElementException("Карта не найдена"))
                    .when(cardService).activateCard(1L);

            mockMvc.perform(patch("/api/cards/1/activate"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("Карта не найдена"))
                    .andDo(print());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void activateCard_BadRequest() throws Exception {
            doThrow(new IllegalStateException("Нельзя активировать"))
                    .when(cardService).activateCard(1L);

            mockMvc.perform(patch("/api/cards/1/activate"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Нельзя активировать"))
                    .andDo(print());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void deleteCard_Success() throws Exception {
            doNothing().when(cardService).deleteCard(1L);

            mockMvc.perform(delete("/api/cards/1"))
                    .andExpect(status().isNoContent())
                    .andDo(print());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void deleteCard_NotFound() throws Exception {
            doThrow(new NoSuchElementException("Карта не найдена"))
                    .when(cardService).deleteCard(1L);

            mockMvc.perform(delete("/api/cards/1"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("Карта не найдена"))
                    .andDo(print());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void getAllCards_Success() throws Exception {
            CardDTO dto = new CardDTO(buildCard("user1", 1L), "**** 4444");
            when(cardService.getAllCards(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(dto)));

            mockMvc.perform(get("/api/cards/all"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].owner").value("user1"))
                    .andDo(print());
        }
    }
}
