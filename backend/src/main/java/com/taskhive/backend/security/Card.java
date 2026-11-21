package com.taskhive.backend.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.taskhive.backend.util.Auditable;
import com.taskhive.backend.util.Uuid7;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

import java.util.UUID;

@Entity
@Table(name = "cards", indexes = {
        @Index(name = "idx_cards_list", columnList = "list_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@ToString(exclude = {"list"})
public class Card extends Auditable {

    @Id
    @EqualsAndHashCode.Include
    private UUID id;

    @NotBlank
    @Size(max = 512)
    private String title;

    @Size(max = 4096)
    private String description;

    @Column(name = "position", nullable = false)
    private double position;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "list_id", nullable = false)
    private TaskList list;

    @Column(name = "list_id", insertable = false, updatable = false)
    private UUID listId;

    @PrePersist
    void assignId() {
        if (this.id == null) {
            this.id = Uuid7.now();
        }
    }
}