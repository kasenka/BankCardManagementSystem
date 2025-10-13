package com.example.bankcards.controller;

import com.example.bankcards.dto.CardCreateDTO;
import com.example.bankcards.dto.TransactionRequestDTO;
import com.example.bankcards.service.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Pageable;
import java.security.Principal;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/cards")
@Tag(name = "Cards", description = "Операции с банковскими картами")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }


    @GetMapping
    @PreAuthorize("hasRole('USER')")
    @Operation(
            summary = "Список своих карт",
            description = "Возвращает список карт текущего пользователя с возможностью поиска и пагинации",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Карты найдены")
            }
    )
    public ResponseEntity<?> getMyCards(@RequestParam(name = "search", required = false) String search,
                                        Pageable pageable,
                                        Principal principal) {
        return ResponseEntity.ok(cardService.getMyCards(principal.getName(), search, pageable));
    }

    @GetMapping("/{cardId}")
    @PreAuthorize("hasRole('USER')")
    @Operation(
            summary = "Информация о своей карте",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Карта найдена"),
                    @ApiResponse(responseCode = "404", description = "Карта не найдена")
            }
    )
    public ResponseEntity<?> getMyCard(@PathVariable("cardId") Long id, Principal principal) {
        return ResponseEntity.ok(cardService.getMyCard(principal.getName(), id));
    }

    @PostMapping("/transaction")
    @PreAuthorize("hasRole('USER')")
    @Operation(
            summary = "Перевод между картами",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Перевод выполнен"),
                    @ApiResponse(responseCode = "400", description = "Недостаточно средств или ошибка перевода"),
                    @ApiResponse(responseCode = "404", description = "Карта не найдена")
            }
    )
    public ResponseEntity<?> transfer(@RequestBody TransactionRequestDTO dto, Principal principal) {
        cardService.transaction(principal.getName(), dto);
        return ResponseEntity.ok(Map.of("message", "Перевод выполнен"));
    }

    @PostMapping("/{cardId}/block-request")
    @PreAuthorize("hasRole('USER')")
    @Operation(
            summary = "Запрос на блокировку карты",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Запрос принят"),
                    @ApiResponse(responseCode = "400", description = "Карта уже заблокирована"),
                    @ApiResponse(responseCode = "404", description = "Карта не найдена")
            }
    )
    public ResponseEntity<?> requestBlock(@PathVariable("cardId") Long id, Principal principal) {
        cardService.requestBlock(principal.getName(), id);
        return ResponseEntity.ok(Map.of("message", "Запрос на блокировку отправлен"));
    }

    @GetMapping("/{cardId}/balance")
    @PreAuthorize("hasRole('USER')")
    @Operation(
            summary = "Баланс карты",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Баланс возвращен"),
                    @ApiResponse(responseCode = "404", description = "Карта не найдена")
            }
    )
    public ResponseEntity<?> getBalance(@PathVariable("cardId") Long id, Principal principal) {
        return ResponseEntity.ok(Map.of("balance", cardService.getBalance(principal.getName(), id)));
    }


    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Создание карты",
            description = "Создает карту для пользователя (только админ)",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Карта создана"),
                    @ApiResponse(responseCode = "404", description = "Пользователь не найден")
            }
    )
    public ResponseEntity<?> createCard(@RequestBody CardCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cardService.createCard(dto));
    }

    @PatchMapping("/{cardId}/block")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Блокировка карты",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Карта заблокирована"),
                    @ApiResponse(responseCode = "400", description = "Карта уже заблокирована"),
                    @ApiResponse(responseCode = "404", description = "Карта не найдена")
            }
    )
    public ResponseEntity<?> blockCard(@PathVariable("cardId") Long id) {
        cardService.blockCard(id);
        return ResponseEntity.ok(Map.of("message", "Карта заблокирована"));
    }

    @PatchMapping("/{cardId}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Активация карты",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Карта активирована"),
                    @ApiResponse(responseCode = "400", description = "Карта уже активна"),
                    @ApiResponse(responseCode = "404", description = "Карта не найдена")
            }
    )
    public ResponseEntity<?> activateCard(@PathVariable("cardId") Long id) {
        cardService.activateCard(id);
        return ResponseEntity.ok(Map.of("message", "Карта активирована"));
    }

    @DeleteMapping("/{cardId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Удаление карты",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Карта удалена"),
                    @ApiResponse(responseCode = "404", description = "Карта не найдена")
            }
    )
    public ResponseEntity<?> deleteCard(@PathVariable("cardId") Long id) {
        cardService.deleteCard(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Список всех карт",
            description = "Возвращает все карты с пагинацией (для администратора)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Карты получены")
            }
    )
    public ResponseEntity<?> getAllCards(Pageable pageable) {
        return ResponseEntity.ok(cardService.getAllCards(pageable));
    }
}
