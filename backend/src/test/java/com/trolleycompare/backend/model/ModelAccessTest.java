package com.trolleycompare.backend.model;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ModelAccessTest {

    @Test
    void appUserPersistsMutableIdentityAndStatusFields() {
        AppUser user = new AppUser("Person", "person@example.com", "hash");
        ReflectionTestUtils.setField(user, "id", 1L);
        user.setName("Updated");
        user.setEmail("updated@example.com");
        user.setPasswordHash("new-hash");
        user.setRole(AppUser.Role.SUPER_ADMIN);
        user.setEnabled(false);

        assertEquals(1L, user.getId());
        assertEquals("Updated", user.getName());
        assertEquals("updated@example.com", user.getEmail());
        assertEquals("new-hash", user.getPasswordHash());
        assertEquals(AppUser.Role.SUPER_ADMIN, user.getRole());
        assertFalse(user.isEnabled());
    }

    @Test
    void productExposesAllPersistedFields() {
        Product product = new Product();
        LocalDate date = LocalDate.now();
        product.setId(3L);
        product.setStoreName("Tesco");
        product.setName("Milk");
        product.setQuantity(1.0);
        product.setUnit("L");
        product.setRegularPrice(1.2);
        product.setClubPrice(1.0);
        product.setDate(date);

        assertEquals(3L, product.getId());
        assertEquals("Tesco", product.getStoreName());
        assertEquals("Milk", product.getName());
        assertEquals(1.0, product.getQuantity());
        assertEquals("L", product.getUnit());
        assertEquals(1.2, product.getRegularPrice());
        assertEquals(1.0, product.getClubPrice());
        assertEquals(date, product.getDate());
    }

    @Test
    void comparisonHistorySetsItsCreationTimeOnlyOnce() {
        AppUser user = new AppUser("Person", "person@example.com", "hash");
        ComparisonHistory history = new ComparisonHistory(user, "[]", "[]");

        history.setCreatedAt();
        Instant firstCreatedAt = history.getCreatedAt();
        history.setCreatedAt();

        assertSame(user, history.getUser());
        assertEquals(firstCreatedAt, history.getCreatedAt());
        assertEquals("[]", history.getBasketItems());
        assertEquals("[]", history.getStoreResults());

        ComparisonHistory withTimestamp = new ComparisonHistory(user, "[]", "[]");
        ReflectionTestUtils.setField(withTimestamp, "createdAt", firstCreatedAt);
        withTimestamp.setCreatedAt();
        assertEquals(firstCreatedAt, withTimestamp.getCreatedAt());
    }
}
