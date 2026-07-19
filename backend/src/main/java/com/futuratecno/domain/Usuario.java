package com.futuratecno.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "usuarios")
public class Usuario extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String email;

    @Column   // opcional: los usuarios que entran con Google no tienen contraseña local.
    private String password;

    @Column(name = "google_sub")   // claim "sub" del ID token de Google; null si la cuenta no usa Google.
    private String googleSub;

    @Column
    private String nombre;

    @Column(nullable = false)
    private String rol = "USUARIO";   // "ADMIN" o "USUARIO"

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "reset_token")   // hash (SHA-256) del token de reseteo de contraseña; null si no hay uno pendiente.
    private String resetToken;

    @Column(name = "reset_token_expira")
    private java.time.LocalDateTime resetTokenExpira;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getGoogleSub() { return googleSub; }
    public void setGoogleSub(String googleSub) { this.googleSub = googleSub; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

    public String getResetToken() { return resetToken; }
    public void setResetToken(String resetToken) { this.resetToken = resetToken; }

    public java.time.LocalDateTime getResetTokenExpira() { return resetTokenExpira; }
    public void setResetTokenExpira(java.time.LocalDateTime resetTokenExpira) { this.resetTokenExpira = resetTokenExpira; }
}
