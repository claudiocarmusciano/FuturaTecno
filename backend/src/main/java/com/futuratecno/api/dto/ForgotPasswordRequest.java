package com.futuratecno.api.dto;

/** "Olvidé mi contraseña": el usuario ingresa su email para recibir el enlace de reseteo. */
public class ForgotPasswordRequest {
    private String email;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
