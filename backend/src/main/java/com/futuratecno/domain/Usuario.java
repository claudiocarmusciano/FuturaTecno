package com.futuratecno.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "usuarios")
public class Usuario extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column
    private String nombre;

    @Column(nullable = false)
    private String rol = "USUARIO";   // "ADMIN" o "USUARIO"

    @Column(nullable = false)
    private Boolean activo = true;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
}
