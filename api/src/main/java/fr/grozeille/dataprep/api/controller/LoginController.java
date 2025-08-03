package fr.grozeille.dataprep.api.controller;

import fr.grozeille.dataprep.api.entity.User;
import fr.grozeille.dataprep.api.repository.UserRepository;
import fr.grozeille.dataprep.api.security.JwtUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/auth")
@Slf4j
public class LoginController {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        Optional<User> userOpt = userRepository.findByEmail(req.email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid credentials."));
        }
        User user = userOpt.get();
        if (!passwordEncoder.matches(req.password, user.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid credentials."));
        }
        String token = jwtUtil.generateToken(user.getEmail(), user.isSuperAdmin());
        return ResponseEntity.ok(new LoginResponse(token));
    }

    @Data
    public static class LoginRequest {
        private String email;
        private String password;
    }

    @Data
    public static class LoginResponse {
        private final String token;
    }

    @Data
    public static class ErrorResponse {
        private final String message;
    }
}
