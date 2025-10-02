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
import java.util.NoSuchElementException;

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
    public ResponseEntity<?> getMyCards(@RequestParam(name = "search", required = false) String search,
                                        Pageable pageable,
                                        Principal principal) {
        return ResponseEntity.ok(cardService.getMyCards(principal.getName(), search, pageable));
    }

    // Инфо о своей карте
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getMyCard(@PathVariable("id") Long id, Principal principal) {
        try {
            return ResponseEntity.ok(cardService.getMyCard(principal.getName(), id));
        }catch (NoSuchElementException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // Перевод между картами
    @PostMapping("/transaction")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> transfer(@RequestBody TransactionRequestDTO dto, Principal principal) {
        try{
            cardService.transaction(principal.getName(), dto);
            return ResponseEntity.ok(Map.of("message", "Перевод выполнен"));

        }catch (NoSuchElementException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }catch (IllegalStateException e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }

    }

    // Запрос на блокировку карты
    @PostMapping("/{id}/block-request")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> requestBlock(@PathVariable("id") Long id, Principal principal) {
        try {
            cardService.requestBlock(principal.getName(), id);
            return ResponseEntity.ok(Map.of("message", "Запрос на блокировку отправлен"));

        }catch (NoSuchElementException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }catch (IllegalStateException e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // Баланс карты
    @GetMapping("/{id}/balance")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getBalance(@PathVariable("id") Long id, Principal principal) {
        try{
            return ResponseEntity.ok(Map.of("balance", cardService.getBalance(principal.getName(), id)));

        }catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // --- Админ ---

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createCard(@RequestBody CardCreateDTO dto) {
        try{
            return ResponseEntity.status(HttpStatus.CREATED).body(cardService.createCard(dto));

        }catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/block")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> blockCard(@PathVariable("id") Long id) {
        try{
            cardService.blockCard(id);
            return ResponseEntity.ok(Map.of("message", "Карта заблокирована"));

        }catch (NoSuchElementException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }catch (IllegalStateException e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> activateCard(@PathVariable("id") Long id) {
        try{
            cardService.activateCard(id);
            return ResponseEntity.ok(Map.of("message", "Карта активирована"));

        }catch (NoSuchElementException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }catch (IllegalStateException e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteCard(@PathVariable("id") Long id) {
        try{
            cardService.deleteCard(id);
            return ResponseEntity.noContent().build();

        }catch (NoSuchElementException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllCards(Pageable pageable) {
        return ResponseEntity.ok(cardService.getAllCards(pageable));
    }
}
