package com.ssafy.eatBusan;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:mysql://localhost:3306/eatbusan_test?useSSL=false&serverTimezone=Asia/Seoul&characterEncoding=UTF-8&createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true",
		"spring.jpa.hibernate.ddl-auto=create-drop"
})
class EatBusanApplicationTests {

	@Test
	void contextLoads() {
	}

}
