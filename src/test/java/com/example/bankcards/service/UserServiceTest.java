package com.example.bankcards.service;

import com.example.bankcards.dto.UserDTO;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.service.UserService;
import com.example.bankcards.util.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Комплексные тесты для UserService")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    private User user;
    private UserDTO userDTO;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        user = new User(1L, "user1", "pass", Role.USER);
        userDTO = new UserDTO("user1", Role.USER);
    }

    @Nested
    @DisplayName("Тесты метода getMe()")
    class GetMeTests {

        @Test
        void getMe_Valid_ShouldReturnUserDTO() {
            when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user));
            when(userMapper.map(user)).thenReturn(userDTO);

            UserDTO result = userService.getMe("user1");

            assertThat(result).isNotNull();
            assertThat(result.getUsername()).isEqualTo("user1");
            verify(userRepository).findByUsername("user1");
        }

        @Test
        void getMe_UserNotFound_ShouldThrow() {
            when(userRepository.findByUsername("user2")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getMe("user2"))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessage("Пользователь не найден");

            verify(userRepository).findByUsername("user2");
        }
    }

    @Nested
    @DisplayName("Тесты метода getAllUsers()")
    class GetAllUsersTests {

        @Test
        void getAllUsers_ShouldReturnPageOfDTOs() {
            Page<User> page = new PageImpl<>(List.of(user));
            when(userRepository.findAll(any(Pageable.class))).thenReturn(page);
            when(userMapper.map(user)).thenReturn(userDTO);

            Page<UserDTO> result = userService.getAllUsers(Pageable.unpaged());

            assertThat(result).isNotEmpty();
            assertThat(result.getContent().get(0).getUsername()).isEqualTo("user1");
            verify(userRepository).findAll(any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("Тесты метода getUser()")
    class GetUserTests {

        @Test
        void getUser_Valid_ShouldReturnUserDTO() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userMapper.map(user)).thenReturn(userDTO);

            UserDTO result = userService.getUser(1L);

            assertThat(result.getUsername()).isEqualTo("user1");
            verify(userRepository).findById(1L);
        }

        @Test
        void getUser_NotFound_ShouldThrow() {
            when(userRepository.findById(2L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUser(2L))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessage("Пользователь не найден");

            verify(userRepository).findById(2L);
        }
    }

    @Nested
    @DisplayName("Тесты метода deleteUser()")
    class DeleteUserTests {

        @Test
        void deleteUser_Valid_ShouldReturnTrue() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            boolean result = userService.deleteUser(1L);

            assertThat(result).isTrue();
            verify(userRepository).delete(user);
        }

        @Test
        void deleteUser_NotFound_ShouldThrow() {
            when(userRepository.findById(2L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.deleteUser(2L))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessage("Пользователь не найден");
        }

        @Test
        void deleteUser_Admin_ShouldThrow() {
            User admin = new User(2L, "admin", "pass", Role.ADMIN);
            when(userRepository.findById(2L)).thenReturn(Optional.of(admin));

            assertThatThrownBy(() -> userService.deleteUser(2L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Админ не может управлять другими админами");

            verify(userRepository, never()).delete(any());
        }
    }
}

