package fr.grozeille.db4all.api.repository;

import fr.grozeille.db4all.api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);
    long count();
}
