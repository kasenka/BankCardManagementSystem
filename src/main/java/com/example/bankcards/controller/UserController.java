package com.example.bankcards.controller;

import com.example.bankcards.dto.UserDTO;
import com.example.bankcards.entity.ApiError;
import com.example.bankcards.exception.ConflictErrorException;
import com.example.bankcards.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Pageable;
import java.security.Principal;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("api/users")
@Tag(name = "Users", description = "Управление пользователями")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/me")
    @Operation(
            summary = "Информация о текущем пользователе",
            description = "Возвращает данные текущего пользователя на основе Principal",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Пользователь найден"),
                    @ApiResponse(responseCode = "404", description = "Пользователь не найден")
            }
    )
    public ResponseEntity<?> getMe(Principal principal) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(Map.of("me",userService.getMe(principal.getName())));
    }

    @GetMapping("")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Список всех пользователей",
            description = "Возвращает постраничный список пользователей (только для администратора)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Успешный запрос")
            }
    )
    public ResponseEntity<?> getAllUsers(Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(Map.of("users",userService.getAllUsers(pageable)));
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Информация о пользователе по ID",
            description = "Возвращает данные пользователя (только для администратора)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Пользователь найден"),
                    @ApiResponse(responseCode = "404", description = "Пользователь не найден")
            }
    )
    public ResponseEntity<?> getUser(@PathVariable("userId") long id) {
        UserDTO userDTO = userService.getUser(id);
        return ResponseEntity.status(HttpStatus.OK)
                .body(userDTO);
    }

    @DeleteMapping("/{userId}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Удаление пользователя",
            description = "Удаляет пользователя по ID. Администраторов удалить нельзя.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Пользователь успешно удален"),
                    @ApiResponse(responseCode = "404", description = "Пользователь не найден"),
                    @ApiResponse(responseCode = "409", description = "Конфликт — невозможно удалить пользователя")
            }
    )
    public ResponseEntity<?> deleteUser(@PathVariable("userId") long id){
        if (userService.deleteUser(id)){
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body(Map.of("message","Этот пользователь удален"));
        }throw new ConflictErrorException("Не удалось удалить пользователя");
    }
}
