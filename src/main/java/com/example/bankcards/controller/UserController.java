package com.example.bankcards.controller;

import com.example.bankcards.dto.UserDTO;
import com.example.bankcards.service.UserService;
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
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/me")
    public ResponseEntity<?> getMe(Principal principal) {
        try{
            return ResponseEntity.status(HttpStatus.OK)
                    .body(Map.of("me",userService.getMe(principal.getName())));
        }catch (NoSuchElementException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllUsers(Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(Map.of("users",userService.getAllUsers(pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUser(@PathVariable("id") long id) {
        try {
            UserDTO userDTO = userService.getUser(id);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(userDTO);
        }catch (NoSuchElementException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable("id") long id){
        try{
            if (userService.deleteUser(id)){
                return ResponseEntity.status(HttpStatus.NO_CONTENT)
                        .body(Map.of("message","Этот пользователь удален"));
            }return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error","Не удалось удалить пользователя"));

        }catch (NoSuchElementException | IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
