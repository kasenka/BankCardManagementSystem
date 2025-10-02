package com.example.bankcards.controller;

import com.example.bankcards.dto.TokenRefreshDTO;
import com.example.bankcards.dto.UserAuthDTO;
import com.example.bankcards.dto.UserDTO;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.RefreshTokenRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.service.JwtService;
import com.example.bankcards.service.RefreshTokenService;
import com.example.bankcards.util.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.apache.coyote.BadRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.tags.Tag;


@RestController
@RequestMapping("api/auth")
@Tag(name = "Auth", description = "Регистрация, вход, refresh и logout")
public class AuthController {

    private UserRepository userRepository;
    private AuthenticationManager authenticationManager;
    private JwtService jwtService;
    private UserMapper userMapper;
    private PasswordEncoder passwordEncoder;
    private RefreshTokenService refreshTokenService;
    private RefreshTokenRepository refreshTokenRepository;


    public AuthController(UserRepository userRepository,
                          AuthenticationManager authenticationManager,
                          JwtService jwtService,
                          UserMapper userMapper,
                          PasswordEncoder passwordEncoder,
                          RefreshTokenService refreshTokenService,
                          RefreshTokenRepository refreshTokenRepository) {
        this.userRepository = userRepository;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @PostMapping(value = "/register")
    @Operation(
            description = "Регистрирует нового пользователя с ролью USER",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Пользователь успешно создан"),
                    @ApiResponse(responseCode = "400", description = "Ошибка валидации или юзернейм занят")
            }
    )
    public ResponseEntity<?> register(@RequestBody @Valid UserAuthDTO userAuthDTO,
                                      BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            List<String> errors = bindingResult.getAllErrors().stream()
                    .map(error -> error.getDefaultMessage())
                    .collect(Collectors.toList());
            return ResponseEntity.badRequest()
                    .body(Map.of("errors", errors));
        }

        if(userRepository.findByUsername(userAuthDTO.getUsername()).isPresent()){
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Этот юзернейм уже занят"));
        }

        User user = userMapper.map(userAuthDTO);

        user.setEncryptedPassword(passwordEncoder.encode(userAuthDTO.getPassword()));
        user.setRole(Role.USER);

        userRepository.save(user);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userMapper.map(user));
    }

    @PostMapping(value = "/login")
    @Operation(
            description = "Аутентификация пользователя, возвращает access и refresh JWT токены",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Успешный вход"),
                    @ApiResponse(responseCode = "401", description = "Неверный логин или пароль")
            }
    )
    public ResponseEntity<?> login(@RequestBody UserAuthDTO userAuthDTO){
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            userAuthDTO.getUsername(),
                            userAuthDTO.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            User user = userRepository.findByUsername(userAuthDTO.getUsername()).get();

            String jwtAccess = jwtService.generateAccessToken(user);
            String jwtRefresh = jwtService.generateRefreshToken(user);

            refreshTokenService.saveRefreshToken(jwtRefresh, user.getUsername(),
                    LocalDateTime.now().plusSeconds(jwtService.jwtExpirationRefresh));

            return ResponseEntity.status(HttpStatus.OK)
                    .body(Map.of("jwtAccess", jwtAccess,
                            "jwtRefresh", jwtRefresh));
        } catch (BadCredentialsException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Неверный юзернейм или пароль"));
        }
    }

    @PostMapping(value = "/refresh")
    @Operation(
            description = "Обновление access токена, принимает refreshToken и возвращает новый accessToken",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Токен обновлен"),
                    @ApiResponse(responseCode = "404", description = "Невалидный refreshToken")
            }
    )
    public ResponseEntity<?> refresh(@RequestBody TokenRefreshDTO tokenRefreshDTO){
        if (refreshTokenService.isValid(tokenRefreshDTO.getRefreshToken())){
            String username = refreshTokenService.getUsernameByToken(tokenRefreshDTO.getRefreshToken());
            User user = userRepository.findByUsername(username).get();

            String accessToken = jwtService.generateAccessToken(user);
            return ResponseEntity.ok()
                    .body(Map.of("jwtAccess", accessToken));
        }return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Невалидный refreshToken"));
    }

    @DeleteMapping("/logout")
    @Operation(
            description = "Выход пользователя, удаляет refresh токен из БД, делая его недействительным",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Успешный выход"),
                    @ApiResponse(responseCode = "400", description = "Токен невалидный")
            }
    )
    public ResponseEntity<?> logout(@RequestBody TokenRefreshDTO tokenRefreshDTO) {
        if (refreshTokenRepository.findByToken(tokenRefreshDTO.getRefreshToken()).isEmpty()){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Refresh токен не валидный"));
        }
        refreshTokenService.deleteRefreshToken(tokenRefreshDTO.getRefreshToken());
        return ResponseEntity.status(HttpStatus.OK)
                .body(Map.of("message", "Logged out"));
    }

}
