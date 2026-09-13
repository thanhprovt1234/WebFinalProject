package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import java.util.Locale;

import javax.sql.DataSource;

import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.servlet.view.InternalResourceView;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

@SpringBootTest
@ActiveProfiles("test")
class DemoApplicationTests {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private DataSource dataSource;

	@Autowired
	@Qualifier("defaultViewResolver")
	private InternalResourceViewResolver viewResolver;

	@Test
	void contextLoads() {
		assertThat(entityManagerFactory.isOpen()).isTrue();
	}

	@Test
	void testDatabaseIsIsolatedAndReachable() throws SQLException {
		try (var connection = dataSource.getConnection();
				var statement = connection.createStatement();
				var result = statement.executeQuery("SELECT 1")) {
			assertThat(connection.getMetaData().getURL()).startsWith("jdbc:h2:mem:");
			assertThat(result.next()).isTrue();
			assertThat(result.getInt(1)).isEqualTo(1);
		}
	}

	@Test
	void jspViewsResolveUnderWebInf() throws Exception {
		var view = viewResolver.resolveViewName("smoke-test", Locale.ROOT);
		assertThat(view).isInstanceOf(InternalResourceView.class);
		assertThat(((InternalResourceView) view).getUrl())
				.isEqualTo("/WEB-INF/views/smoke-test.jsp");
	}

}
