package com.futuratecno.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

@Entity
@Table(name = "productos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Producto extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private Proveedor proveedor;

    @Column(nullable = false)
    private String marca;

    @Column(nullable = false)
    private String modelo;

    @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL)
    private List<Variante> variantes;

    @Column(nullable = false)
    private Boolean activo = true;
}
