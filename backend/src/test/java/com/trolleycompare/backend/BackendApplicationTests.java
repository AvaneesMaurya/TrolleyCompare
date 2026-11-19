package com.trolleycompare.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BackendApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void contextLoads() {
	}

	@Test
	void userListRequiresSuperAdminAuthentication() throws Exception {
		mockMvc.perform(get("/api/admin/users"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void comparisonHistoryRequiresAuthentication() throws Exception {
		mockMvc.perform(get("/api/history"))
				.andExpect(status().isUnauthorized());
	}

}
