package com.futuratecno.api.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Una página del catálogo público + lo que hace falta del catálogo completo para armar los
 * filtros (marcas, categorías con productos, rango de precios). Así el navegador ya no baja
 * los miles de productos para mostrar 24.
 */
public class CatalogoPaginaDTO {
    private List<ProductoCatalogoDTO> items;
    private int total;            // productos que cumplen los filtros
    private int pagina;           // 1-based, ya ajustada al rango válido
    private int porPagina;
    private int totalPaginas;
    private int totalCatalogo;    // productos publicados, sin filtros
    private List<String> marcas;
    private List<Long> categoriaIds;
    private BigDecimal precioMinUsd;
    private BigDecimal precioMaxUsd;

    public List<ProductoCatalogoDTO> getItems() { return items; }
    public void setItems(List<ProductoCatalogoDTO> items) { this.items = items; }
    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
    public int getPagina() { return pagina; }
    public void setPagina(int pagina) { this.pagina = pagina; }
    public int getPorPagina() { return porPagina; }
    public void setPorPagina(int porPagina) { this.porPagina = porPagina; }
    public int getTotalPaginas() { return totalPaginas; }
    public void setTotalPaginas(int totalPaginas) { this.totalPaginas = totalPaginas; }
    public int getTotalCatalogo() { return totalCatalogo; }
    public void setTotalCatalogo(int totalCatalogo) { this.totalCatalogo = totalCatalogo; }
    public List<String> getMarcas() { return marcas; }
    public void setMarcas(List<String> marcas) { this.marcas = marcas; }
    public List<Long> getCategoriaIds() { return categoriaIds; }
    public void setCategoriaIds(List<Long> categoriaIds) { this.categoriaIds = categoriaIds; }
    public BigDecimal getPrecioMinUsd() { return precioMinUsd; }
    public void setPrecioMinUsd(BigDecimal precioMinUsd) { this.precioMinUsd = precioMinUsd; }
    public BigDecimal getPrecioMaxUsd() { return precioMaxUsd; }
    public void setPrecioMaxUsd(BigDecimal precioMaxUsd) { this.precioMaxUsd = precioMaxUsd; }
}
