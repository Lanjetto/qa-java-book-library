package library.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security (t7/Б16): basic auth для API. API без сессий, поэтому CSRF отключён.
 * {@code /actuator/health} — permitAll (придёт в t8), {@code /api/**} — authenticated,
 * httpBasic из {@code spring.security.user.name/password} (application.yml: admin/secret).
 *
 * <p>Профиль {@code test} (H2-контексты {@code @SpringBootTest}/{@code @DataJpaTest}) — open:
 * они проверяют CRUD/слайсы и не должны знать пароль; сама авторизация покрывается web-слайсами
 * ({@code @WithMockUser}) и docker-e2e с реальным basic-заголовком.
 */
@Configuration
public class SecurityConfig {

    /** Прод/стенд/docker-тесты: API защищён basic auth. */
    @Bean
    @Profile("!test")
    SecurityFilterChain secured(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    /** Локальный профиль test: всё открыто (брокер/пароли не нужны для CRUD-слайсов на H2). */
    @Bean
    @Profile("test")
    SecurityFilterChain open(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
