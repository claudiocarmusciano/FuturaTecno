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

    // Peso/dimensiones por defecto para cotizar envío de los productos de esta categoría, cuando
    // el producto no tiene su propio valor cargado (V14). Se resuelve hoja primero, padre después
    // — ver la lógica de resolución en el servicio de envíos.
    @Column(name = "peso_gramos_default")
    private Integer pesoGramosDefault;

    @Column(name = "alto_cm_default")
    private Integer altoCmDefault;

    @Column(name = "ancho_cm_default")
    private Integer anchoCmDefault;

    @Column(name = "largo_cm_default")
    private Integer largoCmDefault;

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

    public Integer getPesoGramosDefault() {
        return pesoGramosDefault;
    }

    public void setPesoGramosDefault(Integer pesoGramosDefault) {
        this.pesoGramosDefault = pesoGramosDefault;
    }

    public Integer getAltoCmDefault() {
        return altoCmDefault;
    }

    public void setAltoCmDefault(Integer altoCmDefault) {
        this.altoCmDefault = altoCmDefault;
    }

    public Integer getAnchoCmDefault() {
        return anchoCmDefault;
    }

    public void setAnchoCmDefault(Integer anchoCmDefault) {
        this.anchoCmDefault = anchoCmDefault;
    }

    public Integer getLargoCmDefault() {
        return largoCmDefault;
    }

    public void setLargoCmDefault(Integer largoCmDefault) {
        this.largoCmDefault = largoCmDefault;
    }
}



