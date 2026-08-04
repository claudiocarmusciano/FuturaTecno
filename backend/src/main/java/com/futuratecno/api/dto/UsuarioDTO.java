package com.futuratecno.api.dto;

import java.time.LocalDateTime;

public class UsuarioDTO {
    private Long id;
    private String email;
    private String nombre;
    private String apellido;
    private String celular;
    private String rol;
    private LocalDateTime fechaRegistro;

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

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
}
