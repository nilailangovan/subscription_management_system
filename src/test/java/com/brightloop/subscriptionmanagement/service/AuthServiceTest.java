package com.brightloop.subscriptionmanagement.service;

import com.brightloop.subscriptionmanagement.config.JwtUtil;
import com.brightloop.subscriptionmanagement.dto.request.CreateStaffRequestDto;
import com.brightloop.subscriptionmanagement.dto.request.LoginRequestDto;
import com.brightloop.subscriptionmanagement.dto.request.SignupRequestDto;
import com.brightloop.subscriptionmanagement.dto.response.AuthResponseDto;
import com.brightloop.subscriptionmanagement.dto.response.UserResponseDto;
import com.brightloop.subscriptionmanagement.entity.Role;
import com.brightloop.subscriptionmanagement.entity.User;
import com.brightloop.subscriptionmanagement.exception.EmailAlreadyExistsException;
import com.brightloop.subscriptionmanagement.repository.PasswordResetTokenRepository;
import com.brightloop.subscriptionmanagement.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthService.
 * Tests signup, login, staff creation, and validation handling with mocks.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    private User sampleCustomer;

    @BeforeEach
    void setUp() {
        sampleCustomer = User.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .password("encoded_password")
                .role(Role.CUSTOMER)
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Signup: Success creates a new user with CUSTOMER role")
    void testSignupSuccess() {
        SignupRequestDto request = new SignupRequestDto();
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setEmail("john.doe@example.com");
        request.setPassword("StrongP@ss1");

        when(userRepository.existsByEmail("john.doe@example.com")).thenReturn(false);
        when(passwordEncoder.encode("StrongP@ss1")).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(sampleCustomer);

        UserResponseDto response = authService.signup(request);

        assertNotNull(response);
        assertEquals("john.doe@example.com", response.getEmail());
        assertEquals("CUSTOMER", response.getRole());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Signup: Fails when email already exists")
    void testSignupDuplicateEmail() {
        SignupRequestDto request = new SignupRequestDto();
        request.setEmail("john.doe@example.com");

        when(userRepository.existsByEmail("john.doe@example.com")).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> authService.signup(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Login: Success returns JWT token and user info")
    void testLoginSuccess() {
        LoginRequestDto request = new LoginRequestDto();
        request.setEmail("john.doe@example.com");
        request.setPassword("StrongP@ss1");

        when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(sampleCustomer));
        when(passwordEncoder.matches("StrongP@ss1", "encoded_password")).thenReturn(true);
        when(jwtUtil.generateToken(any(), eq(1L), eq("CUSTOMER"))).thenReturn("mocked.jwt.token");

        AuthResponseDto response = authService.login(request);

        assertNotNull(response);
        assertEquals("mocked.jwt.token", response.getToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals("CUSTOMER", response.getRole());
        assertEquals("john.doe@example.com", response.getEmail());
    }

    @Test
    @DisplayName("Login: Fails with wrong password")
    void testLoginWrongPassword() {
        LoginRequestDto request = new LoginRequestDto();
        request.setEmail("john.doe@example.com");
        request.setPassword("WrongPassword");

        when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(sampleCustomer));
        when(passwordEncoder.matches("WrongPassword", "encoded_password")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("Admin Create Staff: Success assigns STAFF role")
    void testCreateStaffSuccess() {
        CreateStaffRequestDto request = new CreateStaffRequestDto();
        request.setFirstName("Staff");
        request.setLastName("Member");
        request.setEmail("staff@brightloop.com");
        request.setPassword("StaffP@ss1");

        User savedStaff = User.builder()
                .id(2L)
                .firstName("Staff")
                .lastName("Member")
                .email("staff@brightloop.com")
                .role(Role.STAFF)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.existsByEmail("staff@brightloop.com")).thenReturn(false);
        when(passwordEncoder.encode("StaffP@ss1")).thenReturn("encoded_staff_password");
        when(userRepository.save(any(User.class))).thenReturn(savedStaff);

        UserResponseDto response = authService.createStaffUser(request);

        assertNotNull(response);
        assertEquals("STAFF", response.getRole());
        assertEquals("staff@brightloop.com", response.getEmail());
    }
}

