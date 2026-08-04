package com.futuratecno.api.dto;

import java.util.List;

/** Operación masiva sobre productos identificados por sus IDs. */
public class IdsRequest {
    private List<Long> ids;

    public List<Long> getIds() { return ids; }
    public void setIds(List<Long> ids) { this.ids = ids; }
}
