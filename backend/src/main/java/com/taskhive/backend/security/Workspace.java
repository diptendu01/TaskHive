package com.taskhive.backend.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.taskhive.backend.util.Auditable;
import com.taskhive.backend.util.Uuid7;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
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
@Table(name = "workspaces", indexes = {
        @Index(name = "idx_workspaces_slug", columnList = "slug", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@ToString(exclude = {"members", "boards"})
public class Workspace extends Auditable {

    @Id
    @EqualsAndHashCode.Include
    private UUID id;

    @NotBlank
    @Size(max = 120)
    private String name;

    @Size(max = 64)
    private String slug;

    @Size(max = 512)
    private String avatarColor;

    @JsonIgnore
    @OneToMany(mappedBy = "workspace", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkspaceMember> members = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "workspace", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Board> boards = new ArrayList<>();

    @PrePersist
    void assignId() {
        if (this.id == null) {
            this.id = Uuid7.now();
        }
    }
}