package com.todo.controller;

import com.todo.entity.User;
import com.todo.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final ClientRegistrationRepository clientRegistrationRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AuthController(UserRepository userRepository, ClientRegistrationRepository clientRegistrationRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        String userName = (String) session.getAttribute("userName");
        String userEmail = (String) session.getAttribute("userEmail");
        String userPicture = (String) session.getAttribute("userPicture");

        if (userId == null) {
            return ResponseEntity.ok().body(Map.of("authenticated", false));
        }

        Map<String, Object> user = new HashMap<>();
        user.put("authenticated", true);
        user.put("id", userId);
        user.put("name", userName);
        user.put("email", userEmail);
        user.put("picture", userPicture);

        return ResponseEntity.ok(user);
    }

    @GetMapping("/oauth2/callback")
    public ResponseEntity<?> oauth2Callback(@AuthenticationPrincipal OAuth2User oauth2User, HttpSession session) {
        if (oauth2User == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "OAuth2 authentication failed"));
        }

        // Extract user info from Google
        String googleId = oauth2User.getName();
        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");
        String picture = oauth2User.getAttribute("picture");

        // Find or create user
        Optional<User> existingUser = userRepository.findByGoogleId(googleId);
        User user;

        if (existingUser.isPresent()) {
            user = existingUser.get();
            // Update info
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

        // Store in session
        session.setAttribute("userId", user.getId());
        session.setAttribute("userName", user.getName());
        session.setAttribute("userEmail", user.getEmail());
        session.setAttribute("userPicture", user.getPictureUrl());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("userId", user.getId());
        response.put("name", user.getName());
        response.put("email", user.getEmail());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/google-url")
    public ResponseEntity<?> getGoogleOAuthUrl() {
        try {
            ClientRegistration registration = clientRegistrationRepository.findByRegistrationId("google");
            if (registration == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Google OAuth not configured"));
            }
            return ResponseEntity.ok(Map.of("url", "/oauth2/authorization/google"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Google OAuth not configured"));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body, HttpSession session) {
        String email = body.get("email");
        String password = body.get("password");
        String name = body.get("name");

        if (email == null || password == null || email.isBlank() || password.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email và password không được trống"));
        }

        if (userRepository.findByEmail(email).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email đã được sử dụng"));
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setName(name != null && !name.isBlank() ? name : email.split("@")[0]);
        user = userRepository.save(user);

        session.setAttribute("userId", user.getId());
        session.setAttribute("userName", user.getName());
        session.setAttribute("userEmail", user.getEmail());

        return ResponseEntity.ok(Map.of("success", true, "userId", user.getId()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body, HttpSession session) {
        String email = body.get("email");
        String password = body.get("password");

        if (email == null || password == null || email.isBlank() || password.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email và password không được trống"));
        }

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "Email hoặc password không đúng"));
        }

        User user = userOpt.get();
        if (user.getPassword() == null || !passwordEncoder.matches(password, user.getPassword())) {
            return ResponseEntity.status(401).body(Map.of("error", "Email hoặc password không đúng"));
        }

        session.setAttribute("userId", user.getId());
        session.setAttribute("userName", user.getName());
        session.setAttribute("userEmail", user.getEmail());
        session.setAttribute("userPicture", user.getPictureUrl());

        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(Map.of("success", true));
    }
}
