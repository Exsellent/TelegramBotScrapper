package backend.academy.scrapper.filter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import backend.academy.scrapper.configuration.RateLimitingConfig;
import backend.academy.scrapper.controller.TestController;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@WebMvcTest(controllers = TestController.class)
@Import({RateLimitingFilter.class, RateLimitingIntegrationTest.TestConfig.class})
@AutoConfigureMockMvc
@TestPropertySource(properties = {"rate-limiting.request-limit=3", "rate-limiting.window-seconds=60"})
class RateLimitingIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private RateLimitingFilter rateLimitingFilter;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // Очищаем счетчики перед каждым тестом
        rateLimitingFilter.clearRequestCountsForTest();

        // Явно настраиваем MockMvc
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilter(rateLimitingFilter)
                .build();
    }

    @Test
    void shouldAllowRequestsUnderLimit() throws Exception {
        // Один запрос в пределах лимита
        mockMvc.perform(get("/api/test").header("X-Client-Id", "test-client-1"))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void shouldBlockRequestsOverLimit() throws Exception {
        // Используем уникальный клиентский ID для этого теста
        String clientId = "test-client-2";

        // Выполняем первые 3 запроса (в пределах лимита)
        for (int i = 0; i < 3; i++) {
            MvcResult result = mockMvc.perform(get("/api/test").header("X-Client-Id", clientId))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andReturn();

            // Печатаем ответ для отладки
            System.out.println("Response " + i + ": " + result.getResponse().getContentAsString());
        }

        System.out.println("=== Запрос #4 (должен быть заблокирован) ===");
        // Четвертый запрос (превышает лимит)
        mockMvc.perform(get("/api/test").header("X-Client-Id", clientId))
                .andDo(print())
                .andExpect(status().isTooManyRequests());

        System.out.println("=== Запрос #5 (должен быть заблокирован) ===");
        // Пятый запрос (также превышает лимит)
        mockMvc.perform(get("/api/test").header("X-Client-Id", clientId))
                .andDo(print())
                .andExpect(status().isTooManyRequests());
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        public RateLimitingConfig rateLimitingConfig() {
            RateLimitingConfig config = new RateLimitingConfig();
            config.setRequestLimit(3); // Максимум 3 запроса
            config.setWindowSeconds(60); // За 60-секундное окно
            return config;
        }

        @Bean
        public ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        // Регистрируем фильтр явно как FilterRegistrationBean
        @Bean
        public FilterRegistrationBean<Filter> rateLimitingFilterRegistration(RateLimitingFilter filter) {
            FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
            registration.setFilter(filter);
            registration.addUrlPatterns("/*");
            registration.setName("rateLimitingFilter");
            registration.setOrder(1);
            return registration;
        }
    }
}
