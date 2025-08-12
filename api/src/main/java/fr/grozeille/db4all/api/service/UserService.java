package fr.grozeille.db4all.api.service;

import fr.grozeille.db4all.api.exceptions.PasswordTooWeakException;
import fr.grozeille.db4all.api.exceptions.UserAlreadyExistsException;
import fr.grozeille.db4all.api.model.User;
import fr.grozeille.db4all.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.passay.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public boolean isInitialized() {
        return userRepository.count() > 0;
    }

    @Transactional(readOnly = true)
    public Optional<User> authenticate(String username, String password) {
        Optional<User> userOpt = userRepository.findByEmail(username);
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }
        User user = userOpt.get();
        if (password == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            return Optional.empty();
        }
        return Optional.of(user);
    }

    private void validateEmail(String email) {
        // Rule: The email must be a valid email address.
        final String EMAIL_REGEX = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9\\.-]+\\.[a-zA-Z]{2,6}$";
        if (email == null || !email.matches(EMAIL_REGEX)) {
            throw new IllegalArgumentException("Invalid email format. Please use a valid email address.");
        }
    }

    private void validatePassword(String password) {
        // Rule: Password must be strong.
        if (!isPasswordStrong(password)) {
            throw new PasswordTooWeakException("Password is not strong enough.");
        }
    }

    @Transactional
    public void createInitialAdmin(String email, String password) {
        if (isInitialized()) {
            throw new IllegalStateException("Initialization already done.");
        }
        validateEmail(email);
        validatePassword(password);

        if (userRepository.existsById(email)) {
            throw new UserAlreadyExistsException("User with this email already exists.");
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setSuperAdmin(true);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Transactional
    public User createUser(String email, String password, boolean isSuperAdmin) {
        validateEmail(email);
        validatePassword(password);

        if (userRepository.existsById(email)) {
            throw new UserAlreadyExistsException("User with this email already exists.");
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setSuperAdmin(isSuperAdmin);
        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(String email) {
        if (!userRepository.existsById(email)) {
            throw new IllegalArgumentException("User not found with email: " + email);
        }
        userRepository.deleteById(email);
    }

    @Transactional
    public void changeCurrentUserPassword(String email, String oldPassword, String newPassword) {
        User user = userRepository.findById(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));

        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Incorrect old password.");
        }

        validatePassword(newPassword);

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public void updateUserPasswordByAdmin(String email, String newPassword) {
        User user = userRepository.findById(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));

        validatePassword(newPassword);

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public User updateSuperAdminStatus(String email, boolean superAdmin, Authentication authentication) {
        // Rule: A user cannot change their own superAdmin status.
        if (authentication.getName().equals(email)) {
            throw new IllegalArgumentException("A user cannot change their own superAdmin status to prevent accidental lock-out.");
        }

        User user = userRepository.findById(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));

        user.setSuperAdmin(superAdmin);
        return userRepository.save(user);
    }

    private boolean isPasswordStrong(String password) {
        if (password == null) return false;
        PasswordValidator validator = new PasswordValidator(
            new LengthRule(8, 64),
            new CharacterRule(EnglishCharacterData.UpperCase, 1),
            new CharacterRule(EnglishCharacterData.LowerCase, 1),
            new CharacterRule(
                new CharacterData() {
                    public String getErrorCode() { return "INSUFFICIENT_DIGIT_OR_SPECIAL"; }
                    public String getCharacters() {
                        return EnglishCharacterData.Digit.getCharacters() + EnglishCharacterData.Special.getCharacters();
                    }
                }, 1
            )
        );
        RuleResult result = validator.validate(new PasswordData(password));
        return result.isValid();
    }

    public User findByEmail(String email) {
        return this.userRepository.findByEmail(email).orElse(null);
    }
}
