package com.taskhive.backend.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.taskhive.backend.util.Auditable;
import com.taskhive.backend.util.Uuid7;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "boards", indexes = {
        @Index(name = "idx_boards_workspace", columnList = "workspace_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@ToString(exclude = {"workspace", "lists"})
public class Board extends Auditable {

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
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Column(name = "workspace_id", insertable = false, updatable = false)
    private UUID workspaceId;

    @JsonIgnore
    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TaskList> lists = new ArrayList<>();

    @PrePersist
    void assignId() {
        if (this.id == null) {
            this.id = Uuid7.now();
        }
    }
}