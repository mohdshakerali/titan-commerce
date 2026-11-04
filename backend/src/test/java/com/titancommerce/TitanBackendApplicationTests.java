
package com.titancommerce;

import com.titancommerce.product.Product;
import com.titancommerce.product.ProductRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class TitanBackendApplicationTests {

    @Container
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:17")
                    .withDatabaseName("titan_test")
                    .withUsername("titan_test_user")
                    .withPassword("titan_test_password");

    @DynamicPropertySource
    static void configureDatabaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private ProductRepository productRepository;

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
}
