package com.example.bankcards.controller;

// --- Контроллер ---

import com.example.bankcards.dto.CardCreateDTO;
import com.example.bankcards.dto.TransactionRequestDTO;
import com.example.bankcards.service.CardService;
import org.hibernate.query.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Pageable;
import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/cards")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    // --- Пользователь ---

    // Список своих карт с поиском и пагинацией
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getMyCards(@RequestParam(required = false) String search,
                                        Pageable pageable,
                                        Principal principal) {
        return ResponseEntity.ok(cardService.getMyCards(principal.getName(), search, pageable));
    }

    // Инфо о своей карте
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getMyCard(@PathVariable("id") Long id, Principal principal) {
        return ResponseEntity.ok(cardService.getMyCard(principal.getName(), id));
    }

    // Перевод между картами
    @PostMapping("/transaction")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> transfer(@RequestBody TransactionRequestDTO dto, Principal principal) {
        cardService.transaction(principal.getName(), dto);
        return ResponseEntity.ok(Map.of("message", "Перевод выполнен"));
    }

    // Запрос на блокировку карты
    @PostMapping("/{id}/block-request")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> requestBlock(@PathVariable("id") Long id, Principal principal) {
        cardService.requestBlock(principal.getName(), id);
        return ResponseEntity.ok(Map.of("message", "Запрос на блокировку отправлен"));
    }

    // Баланс карты
    @GetMapping("/{id}/balance")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getBalance(@PathVariable("id") Long id, Principal principal) {
        return ResponseEntity.ok(Map.of("balance", cardService.getBalance(principal.getName(), id)));
    }

    // --- Админ ---

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createCard(@RequestBody CardCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cardService.createCard(dto));
    }

    @PatchMapping("/{id}/block")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> blockCard(@PathVariable("id") Long id) {
        cardService.blockCard(id);
        return ResponseEntity.ok(Map.of("message", "Карта заблокирована"));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> activateCard(@PathVariable("id") Long id) {
        cardService.activateCard(id);
        return ResponseEntity.ok(Map.of("message", "Карта активирована"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteCard(@PathVariable("id") Long id) {
        cardService.deleteCard(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllCards(Pageable pageable) {
        return ResponseEntity.ok(cardService.getAllCards(pageable));
    }
}
