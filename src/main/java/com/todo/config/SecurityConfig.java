package com.todo.config;

import com.todo.entity.User;
import com.todo.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import java.util.Optional;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserRepository userRepository;

    public SecurityConfig(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringRequestMatchers("/api/**")
            )
            .sessionManagement(session -> session
                .maximumSessions(1)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/index.html", "/auth/**", "/login/**", "/oauth2/**", "/api/**", "/app", "/app/**").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/")
                .successHandler(authenticationSuccessHandler())
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            );

        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (request, response, authentication) -> {
            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();

            String googleId = oauth2User.getName();
            String email = oauth2User.getAttribute("email");
            String name = oauth2User.getAttribute("name");
            String picture = oauth2User.getAttribute("picture");

            Optional<User> userOpt = userRepository.findByGoogleId(googleId);
            User user;
            if (userOpt.isPresent()) {
                user = userOpt.get();
                user.setEmail(email);
                user.setName(name);
                user.setPictureUrl(picture);
            } else {
                user = new User();
                user.setGoogleId(googleId);
                user.setEmail(email);
                user.setName(name);
                user.setPictureUrl(picture);
            }
            user = userRepository.save(user);

            request.getSession().setAttribute("userId", user.getId());
            request.getSession().setAttribute("userName", user.getName());
            request.getSession().setAttribute("userEmail", user.getEmail());
            request.getSession().setAttribute("userPicture", user.getPictureUrl());

            response.sendRedirect("/app");
        };
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

            return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(new BCryptPasswordEncoder().encode(""))
                .authorities("ROLE_USER")
                .build();
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
