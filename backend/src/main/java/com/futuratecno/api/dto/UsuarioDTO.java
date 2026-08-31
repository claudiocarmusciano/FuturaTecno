package com.futuratecno.api.dto;

import java.time.LocalDateTime;

public class UsuarioDTO {
    private Long id;
    private String email;
    private String nombre;
    private String apellido;
    private String celular;
    private String dni;
    private java.time.LocalDate fechaNacimiento;
    private String rol;
    private LocalDateTime fechaRegistro;
    private Boolean emailVerificado;
    private Boolean whatsappVerificado;
    private Boolean whatsappAgendado;
    private String whatsappVerificacionCodigo;
    private String instagramUsuario;
    private Boolean instagramCompletado;
    private Boolean instagramVerificado;
    private String codigoSorteo;
    private Integer chancesSorteo;
    private LocalDateTime basesAceptadasEn;

    public UsuarioDTO() {}

    public UsuarioDTO(Long id, String email, String nombre, String apellido, String celular, String rol, LocalDateTime fechaRegistro) {
        this.id = id;
        this.email = email;
        this.nombre = nombre;
        this.apellido = apellido;
        this.celular = celular;
        this.rol = rol;
        this.fechaRegistro = fechaRegistro;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }

    public String getCelular() { return celular; }
    public void setCelular(String celular) { this.celular = celular; }

    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }
    public java.time.LocalDate getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(java.time.LocalDate fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    public Boolean getEmailVerificado() { return emailVerificado; }
    public void setEmailVerificado(Boolean emailVerificado) { this.emailVerificado = emailVerificado; }
    public Boolean getWhatsappVerificado() { return whatsappVerificado; }
    public void setWhatsappVerificado(Boolean whatsappVerificado) { this.whatsappVerificado = whatsappVerificado; }
    public Boolean getWhatsappAgendado() { return whatsappAgendado; }
    public void setWhatsappAgendado(Boolean whatsappAgendado) { this.whatsappAgendado = whatsappAgendado; }
    public String getWhatsappVerificacionCodigo() { return whatsappVerificacionCodigo; }
    public void setWhatsappVerificacionCodigo(String whatsappVerificacionCodigo) { this.whatsappVerificacionCodigo = whatsappVerificacionCodigo; }
    public String getInstagramUsuario() { return instagramUsuario; }
    public void setInstagramUsuario(String instagramUsuario) { this.instagramUsuario = instagramUsuario; }
    public Boolean getInstagramCompletado() { return instagramCompletado; }
    public void setInstagramCompletado(Boolean instagramCompletado) { this.instagramCompletado = instagramCompletado; }
    public Boolean getInstagramVerificado() { return instagramVerificado; }
    public void setInstagramVerificado(Boolean instagramVerificado) { this.instagramVerificado = instagramVerificado; }
    public String getCodigoSorteo() { return codigoSorteo; }
    public void setCodigoSorteo(String codigoSorteo) { this.codigoSorteo = codigoSorteo; }
    public Integer getChancesSorteo() { return chancesSorteo; }
    public void setChancesSorteo(Integer chancesSorteo) { this.chancesSorteo = chancesSorteo; }
    public LocalDateTime getBasesAceptadasEn() { return basesAceptadasEn; }
    public void setBasesAceptadasEn(LocalDateTime basesAceptadasEn) { this.basesAceptadasEn = basesAceptadasEn; }
}
