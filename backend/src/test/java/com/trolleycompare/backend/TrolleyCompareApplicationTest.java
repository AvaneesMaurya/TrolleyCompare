package com.trolleycompare.backend;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import static org.mockito.Mockito.mockStatic;

class TrolleyCompareApplicationTest {

    @Test
    void launchesTheSpringApplication() {
        try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
            TrolleyCompareApplication.main(new String[0]);

            springApplication.verify(() -> SpringApplication.run(TrolleyCompareApplication.class, new String[0]));
        }
    }
}
