
package com.titancommerce;

import java.math.BigDecimal;
import java.time.Instant;

import com.titancommerce.customer.Customer;
import com.titancommerce.customer.CustomerRepository;
import com.titancommerce.product.Product;
import com.titancommerce.product.ProductRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TitanBackendApplicationTests {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void cleanCustomerData() {
        customerRepository.deleteAll();
    }

    // 1. Product persistence test

    @Test
    void shouldSaveAndRetrieveProduct() {

        Product product = new Product(
                "Wireless Mouse",
                "Ergonomic wireless mouse",
                new BigDecimal("799.00")
        );

        Product savedProduct = productRepository.save(product);

        assertThat(savedProduct.getId()).isNotNull();

        Product retrievedProduct = productRepository
                .findById(savedProduct.getId())
                .orElseThrow();

        assertThat(retrievedProduct.getName())
                .isEqualTo("Wireless Mouse");

        assertThat(retrievedProduct.getPrice())
                .isEqualByComparingTo("799.00");
    }

    // 2. Successful customer registration

    @Test
    void shouldRegisterCustomerSuccessfully() throws Exception {

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "name": "John Doe",
                            "email": "john@example.com",
                            "password": "SecurePass123"
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.password").doesNotExist());

        Customer customer = customerRepository
                .findByEmail("john@example.com")
                .orElseThrow();

        assertThat(customer.getPassword())
                .isNotEqualTo("SecurePass123");

        assertThat(customer.getPassword())
                .startsWith("$2");
    }

    // 3. Duplicate email registration

    @Test
    void shouldRejectDuplicateEmail() throws Exception {

        String request = """
                {
                    "name": "John Doe",
                    "email": "john@example.com",
                    "password": "SecurePass123"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isConflict());
    }

    // 4. Invalid email registration

    @Test
    void shouldRejectInvalidEmail() throws Exception {

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "name": "John Doe",
                            "email": "invalid-email",
                            "password": "SecurePass123"
                        }
                        """))
                .andExpect(status().isBadRequest());
    }

    // 5. Missing registration fields

    @Test
    void shouldRejectMissingRegistrationFields() throws Exception {

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "name": "",
                            "email": "",
                            "password": ""
                        }
                        """))
                .andExpect(status().isBadRequest());
    }

    // 6. Successful customer login and JWT generation

    @Test
    void shouldLoginCustomerSuccessfully() throws Exception {

        Customer customer = new Customer(
                "John Doe",
                "john@example.com",
                passwordEncoder.encode("SecurePass123")
        );

        customerRepository.save(customer);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "email": "john@example.com",
                            "password": "SecurePass123"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900));
    }

    // 7. Invalid login credentials

    @Test
    void shouldRejectInvalidLoginCredentials() throws Exception {

        Customer customer = new Customer(
                "John Doe",
                "john@example.com",
                passwordEncoder.encode("SecurePass123")
        );

        customerRepository.save(customer);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "email": "john@example.com",
                            "password": "WrongPassword123"
                        }
                        """))
                .andExpect(status().isUnauthorized());
    }

    // 8. Reject request without JWT token

    @Test
    void shouldRejectRequestWithoutJwtToken() throws Exception {

        mockMvc.perform(get("/api/customers/me"))
                .andExpect(status().isUnauthorized());
    }

    // 9. Reject request with invalid JWT token

    @Test
    void shouldRejectRequestWithInvalidJwtToken() throws Exception {

        mockMvc.perform(get("/api/customers/me")
                .header(
                        "Authorization",
                        "Bearer invalid.token.value"
                ))
                .andExpect(status().isUnauthorized());
    }

    // 10. Allow request with valid JWT token

    @Test
    void shouldAllowRequestWithValidJwtToken() throws Exception {

        Customer customer = customerRepository.save(
                new Customer(
                        "Titan Test Customer",
                        "jwt-test@titan.com",
                        passwordEncoder.encode("TestPassword123")
                )
        );

        String token = generateTestJwt(customer.getId());

        mockMvc.perform(get("/api/customers/me")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(customer.getId()))
                .andExpect(jsonPath("$.email").value("jwt-test@titan.com"))
                .andExpect(jsonPath("$.name").value("Titan Test Customer"));
    }

    // Helper method to generate a signed JWT for testing

    private String generateTestJwt(Long customerId) {

        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(customerId.toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(900))
                .issuer("titan-commerce")
                .build();

        JwsHeader header = JwsHeader
                .with(MacAlgorithm.HS256)
                .build();

        return jwtEncoder
                .encode(JwtEncoderParameters.from(header, claims))
                .getTokenValue();
    }
}
