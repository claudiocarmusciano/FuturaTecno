package com.futuratecno.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "productos")
public class Producto extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private Proveedor proveedor;

    // Categoría cruda tal como viene del mayorista (o cargada a mano). Ya no se muestra al
    // usuario: es el insumo del clasificador que resuelve categoriaId contra el árbol fijo.
    @Column
    private String categoria;

    // Subcategoría (hoja) del árbol fijo de categorías. Null hasta que se clasifica
    // (import automático o corrección manual del admin).
    @Column(name = "categoria_id")
    private Long categoriaId;

    @Column(nullable = false)
    private String marca;

    @Column(nullable = false)
    private String modelo;

    @Column(name = "imagen_url", length = 1000)
    private String imagenUrl;

    // Momento del último intento automático sin resultado. Permite que cada clic siga con los
    // faltantes aún no tratados, sin alterar la fecha comercial de actualización del producto.
    @Column(name = "imagen_busqueda_at")
    private LocalDateTime imagenBusquedaAt;

    // Origen externo (integraciones con distribuidores): código en el sistema del proveedor y la fuente.
    @Column(name = "codigo_externo", length = 100)
    private String codigoExterno;

    @Column(length = 50)
    private String fuente;

    // Última vez que el producto apareció en el feed del mayorista (V40). Responde "¿lo sigue
    // teniendo?", que NO es lo mismo que updatedAt ("¿cambió algo?"): un producto con precio y
    // stock estables no mueve updatedAt aunque la sync lo vea todos los días. Null en lo cargado
    // por JSON, donde no hay feed. La escriben los imports con un UPDATE masivo, a propósito
    // fuera de @PreUpdate, para no pisar updatedAt.
    @Column(name = "visto_en_sync_at")
    private LocalDateTime vistoEnSyncAt;

    // Identidad del artículo (V42), separada del nombre visible. La resuelve
    // IdentidadProductoService; acá solo se guarda. identidadClave es única por proveedor.
    @Column(name = "identidad_clave")
    private String identidadClave;

    @Column(name = "identidad_version", length = 30)
    private String identidadVersion;

    @Column(name = "identidad_familia")
    private String identidadFamilia;

    @Column(name = "identidad_atributos")
    private String identidadAtributos;

    // Override del margen y el flete de ESTE producto (V41). En null = usar el del proveedor,
    // que NO es lo mismo que 0% (eso sería vender al costo). La fórmula vive en PrecioService.
    @Column(name = "margen_porcentaje")
    private BigDecimal margenPorcentaje;

    @Column(name = "flete_porcentaje")
    private BigDecimal fletePorcentaje;

    // Peso/dimensiones reales del producto (V14), para cotizar envío. Opcionales: si están en
    // null se usa el default de la categoría (ver CategoriaService / servicio de envíos).
    @Column(name = "peso_gramos")
    private Integer pesoGramos;

    @Column(name = "alto_cm")
    private Integer altoCm;

    @Column(name = "ancho_cm")
    private Integer anchoCm;

    @Column(name = "largo_cm")
    private Integer largoCm;

    @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL)
    private List<Variante> variantes;

    @Column(nullable = false)
    private Boolean activo = true;

    public Proveedor getProveedor() {
        return proveedor;
    }

    public void setProveedor(Proveedor proveedor) {
        this.proveedor = proveedor;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public Long getCategoriaId() {
        return categoriaId;
    }

    public void setCategoriaId(Long categoriaId) {
        this.categoriaId = categoriaId;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    public String getImagenUrl() {
        return imagenUrl;
    }

    public void setImagenUrl(String imagenUrl) {
        this.imagenUrl = imagenUrl;
    }

    public LocalDateTime getImagenBusquedaAt() {
        return imagenBusquedaAt;
    }

    public void setImagenBusquedaAt(LocalDateTime imagenBusquedaAt) {
        this.imagenBusquedaAt = imagenBusquedaAt;
    }

    public String getCodigoExterno() {
        return codigoExterno;
    }

    public void setCodigoExterno(String codigoExterno) {
        this.codigoExterno = codigoExterno;
    }

    public String getFuente() {
        return fuente;
    }

    public void setFuente(String fuente) {
        this.fuente = fuente;
    }

    public LocalDateTime getVistoEnSyncAt() {
        return vistoEnSyncAt;
    }

    public void setVistoEnSyncAt(LocalDateTime vistoEnSyncAt) {
        this.vistoEnSyncAt = vistoEnSyncAt;
    }

    public BigDecimal getMargenPorcentaje() {
        return margenPorcentaje;
    }

    public void setMargenPorcentaje(BigDecimal margenPorcentaje) {
        this.margenPorcentaje = margenPorcentaje;
    }

    public BigDecimal getFletePorcentaje() {
        return fletePorcentaje;
    }

    public void setFletePorcentaje(BigDecimal fletePorcentaje) {
        this.fletePorcentaje = fletePorcentaje;
    }

    public Integer getPesoGramos() {
        return pesoGramos;
    }

    public void setPesoGramos(Integer pesoGramos) {
        this.pesoGramos = pesoGramos;
    }

    public Integer getAltoCm() {
        return altoCm;
    }

    public void setAltoCm(Integer altoCm) {
        this.altoCm = altoCm;
    }

    public Integer getAnchoCm() {
        return anchoCm;
    }

    public void setAnchoCm(Integer anchoCm) {
        this.anchoCm = anchoCm;
    }

    public Integer getLargoCm() {
        return largoCm;
    }

    public void setLargoCm(Integer largoCm) {
        this.largoCm = largoCm;
    }

    public List<Variante> getVariantes() {
        return variantes;
    }

    public void setVariantes(List<Variante> variantes) {
        this.variantes = variantes;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public String getIdentidadClave() { return identidadClave; }
    public void setIdentidadClave(String identidadClave) { this.identidadClave = identidadClave; }

    public String getIdentidadVersion() { return identidadVersion; }
    public void setIdentidadVersion(String identidadVersion) { this.identidadVersion = identidadVersion; }

    public String getIdentidadFamilia() { return identidadFamilia; }
    public void setIdentidadFamilia(String identidadFamilia) { this.identidadFamilia = identidadFamilia; }

    public String getIdentidadAtributos() { return identidadAtributos; }
    public void setIdentidadAtributos(String identidadAtributos) { this.identidadAtributos = identidadAtributos; }

    /**
     * SKU camuflado: código corto del proveedor + identificador del artículo. No delata al
     * proveedor (se lee como una referencia interna), pero permite cruzarlo con el sistema del
     * mayorista. Lo usan el catálogo y el snapshot de los pedidos, por eso vive acá y no en un
     * servicio.
     */
    public String skuCamuflado() {
        String prefijo = (proveedor != null && proveedor.getCodigo() != null && !proveedor.getCodigo().isBlank())
                ? proveedor.getCodigo() : "FT";
        String sufijo = (codigoExterno != null && !codigoExterno.isBlank())
                ? codigoExterno : "P" + getId();
        return prefijo + "-" + sufijo;
    }
}
