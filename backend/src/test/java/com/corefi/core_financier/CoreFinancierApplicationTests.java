package com.corefi.core_financier;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@org.springframework.test.context.ActiveProfiles("test")
@org.springframework.test.context.TestPropertySource(properties = {
    "DB_URL=jdbc:h2:mem:testdb", "DB_USERNAME=sa", "DB_PASSWORD=", 
    "MAIL_USERNAME=test", "MAIL_PASSWORD=test", "MINDEE_API_KEY=test", 
    "JWT_SECRET=this_is_a_very_long_secret_key_for_testing_purposes_only_32_chars", "JWT_EXPIRATION=3600"
})
class CoreFinancierApplicationTests {

	@Test
	void contextLoads() {
	}

}
