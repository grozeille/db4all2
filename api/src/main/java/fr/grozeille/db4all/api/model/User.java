package fr.grozeille.db4all.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class User {
    @Id
    @Column(nullable = false, unique = true)
    private String email;

    @JsonIgnore
    @Column(nullable = false, name = "password_hash")
    private String passwordHash;

    @Column(nullable = false, name = "super_admin")
    private boolean superAdmin;
}
