package com.titancommerce.auth;

import com.titancommerce.customer.CustomerService;
import com.titancommerce.customer.dto.CustomerResponse;
import com.titancommerce.customer.dto.RegisterRequest;
import com.titancommerce.customer.dto.LoginRequest;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final CustomerService customerService;
    private final AuthService authService;

    public AuthController(
            CustomerService customerService,
            AuthService authService) {

        this.customerService = customerService;
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<CustomerResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        CustomerResponse response = customerService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {

        LoginResponse response = authService.login(request);

        return ResponseEntity.ok(response);
    }
}