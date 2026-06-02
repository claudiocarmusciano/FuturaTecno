package com.futuratecno.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "variantes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Variante extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(name = "almacenamiento_gb")
    private Integer almacenamientoGb;

    @Column(name = "ram_gb")
    private Integer ramGb;

    @Column
    private String color;

    @Column(name = "costo_usd", nullable = false)
    private BigDecimal costoUsd;

    @Column(nullable = false)
    private Integer stock = 0;

    @Column(nullable = false)
    private Boolean activo = true;

    @OneToMany(mappedBy = "variante", cascade = CascadeType.ALL)
    private List<Imagen> imagenes;
}
