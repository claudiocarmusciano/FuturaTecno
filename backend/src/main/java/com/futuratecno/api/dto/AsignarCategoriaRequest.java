package com.futuratecno.api.dto;

import java.util.List;

/** Asignación masiva de categoría: aplica el mismo categoriaId a varios productos. */
public class AsignarCategoriaRequest {
    private List<Long> ids;
    private Long categoriaId;

    public List<Long> getIds() { return ids; }
    public void setIds(List<Long> ids) { this.ids = ids; }

    public Long getCategoriaId() { return categoriaId; }
    public void setCategoriaId(Long categoriaId) { this.categoriaId = categoriaId; }
}
