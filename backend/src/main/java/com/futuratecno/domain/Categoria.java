package com.futuratecno.domain;

import jakarta.persistence.*;

/**
 * Nodo de un árbol fijo de 3 niveles (sección > categoría > subcategoría), sembrado
 * completo por la migración V9. No se crea ni edita desde la app.
 */
@Entity
@Table(name = "categorias")
public class Categoria extends BaseEntity {
    @Column(nullable = false, length = 100)
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "padre_id")
    private Categoria padre;

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Categoria getPadre() {
        return padre;
    }

    public void setPadre(Categoria padre) {
        this.padre = padre;
    }
}
