package com.futuratecno.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "promociones")
public class Promocion extends BaseEntity {
    @Column(length = 160) private String titulo;
    @Column(length = 500) private String texto;
    @Column(length = 500) private String enlace;
    @Column(nullable = false) private Integer orden = 0;
    @Column(name = "fecha_inicio") private LocalDateTime fechaInicio;
    @Column(name = "fecha_fin") private LocalDateTime fechaFin;
    @Column(nullable = false) private Boolean activo = false;
    @Column(name = "imagen_escritorio", nullable = false) private byte[] imagenEscritorio;
    @Column(name = "mime_escritorio", nullable = false, length = 30) private String mimeEscritorio;
    @Column(name = "imagen_movil") private byte[] imagenMovil;
    @Column(name = "mime_movil", length = 30) private String mimeMovil;

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }
    public String getEnlace() { return enlace; }
    public void setEnlace(String enlace) { this.enlace = enlace; }
    public Integer getOrden() { return orden; }
    public void setOrden(Integer orden) { this.orden = orden; }
    public LocalDateTime getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDateTime fechaInicio) { this.fechaInicio = fechaInicio; }
    public LocalDateTime getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDateTime fechaFin) { this.fechaFin = fechaFin; }
    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
    public byte[] getImagenEscritorio() { return imagenEscritorio; }
    public void setImagenEscritorio(byte[] imagenEscritorio) { this.imagenEscritorio = imagenEscritorio; }
    public String getMimeEscritorio() { return mimeEscritorio; }
    public void setMimeEscritorio(String mimeEscritorio) { this.mimeEscritorio = mimeEscritorio; }
    public byte[] getImagenMovil() { return imagenMovil; }
    public void setImagenMovil(byte[] imagenMovil) { this.imagenMovil = imagenMovil; }
    public String getMimeMovil() { return mimeMovil; }
    public void setMimeMovil(String mimeMovil) { this.mimeMovil = mimeMovil; }
}
