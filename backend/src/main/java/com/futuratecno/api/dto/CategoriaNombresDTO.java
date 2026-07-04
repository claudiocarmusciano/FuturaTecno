package com.futuratecno.api.dto;

/** Los 3 nombres del árbol para una subcategoría (hoja): sección > categoría > subcategoría. */
public class CategoriaNombresDTO {
    private final String seccion;
    private final String categoriaPadre;
    private final String subcategoria;

    public CategoriaNombresDTO(String seccion, String categoriaPadre, String subcategoria) {
        this.seccion = seccion;
        this.categoriaPadre = categoriaPadre;
        this.subcategoria = subcategoria;
    }

    public String getSeccion() { return seccion; }
    public String getCategoriaPadre() { return categoriaPadre; }
    public String getSubcategoria() { return subcategoria; }
}
