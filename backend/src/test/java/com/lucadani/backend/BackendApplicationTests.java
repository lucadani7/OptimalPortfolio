package com.lucadani.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
// The dashboard would take port 8001, which the running application already uses
@SpringBootTest(properties = "jobrunr.dashboard.enabled=false")
class BackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
