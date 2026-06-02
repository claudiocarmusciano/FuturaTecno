package com.futuratecno.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "imagenes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Imagen extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variante_id", nullable = false)
    private Variante variante;

    @Column(nullable = false)
    private String url;

    @Column(name = "orden", nullable = false)
    private Integer orden = 0;

    @Column(nullable = false)
    private Boolean activo = true;
}
