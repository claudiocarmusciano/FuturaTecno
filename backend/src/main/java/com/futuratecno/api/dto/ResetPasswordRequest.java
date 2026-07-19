package com.futuratecno.api.dto;

/** Reseteo de contraseña: el token que llegó por email + la nueva contraseña. */
public class ResetPasswordRequest {
    private String token;
    private String password;

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
