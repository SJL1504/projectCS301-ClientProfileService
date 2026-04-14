package com.example.client;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import software.amazon.awssdk.services.sns.SnsClient;

@SpringBootTest
class DemoApplicationTests {

	@MockBean
    private SnsClient snsClient;

	@Test
	void contextLoads() {
	}

}
