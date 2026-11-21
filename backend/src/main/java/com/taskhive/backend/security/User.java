package com.taskhive.backend.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.taskhive.backend.util.Auditable;
import com.taskhive.backend.util.Uuid7;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_users_email", columnList = "email", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@ToString(exclude = {"passwordHash", "memberships"})
public class User extends Auditable {

    @Id
    @EqualsAndHashCode.Include
    private UUID id;

    @NotBlank
    @Size(max = 120)
    private String name;

    @Email
    @NotBlank
    @Size(max = 255)
    @Column(unique = true, nullable = false)
    private String email;

    @Size(max = 20)
    private String avatarInitials;

    @JsonIgnore
    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role globalRole = Role.MEMBER;

    @JsonIgnore
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkspaceMember> memberships = new ArrayList<>();

    @PrePersist
    void assignId() {
        if (this.id == null) {
            this.id = Uuid7.now();
        }
    }
}