package com.futuratecno.domain;

import jakarta.persistence.*;

/**
 * Nodo del árbol de categorías: dos niveles (categoría > subcategoría), sembrado por la
 * migración V10 y editable desde el admin (ver CategoriaService y CategoriaAdminController).
 *
 * <p>Ojo: `CategoriaService` cachea el árbol entero en memoria, así que toda escritura tiene
 * que recargarlo o el cambio no se ve hasta reiniciar.
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



