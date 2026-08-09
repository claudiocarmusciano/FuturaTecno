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

    @Column
    private String apellido;

    @Column(length = 20)
    private String celular;

    @Column(nullable = false)
    private String rol = "USUARIO";   // "ADMIN" o "USUARIO"

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "reset_token")   // hash (SHA-256) del token de reseteo de contraseña; null si no hay uno pendiente.
    private String resetToken;

    @Column(name = "reset_token_expira")
    private java.time.LocalDateTime resetTokenExpira;

    @Column(name = "email_verificado", nullable = false)
    private Boolean emailVerificado = false;
    @Column(name = "whatsapp_verificado", nullable = false)
    private Boolean whatsappVerificado = false;
    @Column(name = "whatsapp_codigo_hash")
    private String whatsappCodigoHash;
    @Column(name = "whatsapp_codigo_expira")
    private java.time.LocalDateTime whatsappCodigoExpira;
    @Column(name = "whatsapp_verificacion_codigo")
    private String whatsappVerificacionCodigo;
    @Column(name = "paso_whatsapp_agendado", nullable = false)
    private Boolean pasoWhatsappAgendado = false;
    @Column(name = "paso_instagram_completado", nullable = false)
    private Boolean pasoInstagramCompletado = false;
    @Column(name = "email_activacion_token")
    private String emailActivacionToken;
    @Column(name = "email_activacion_expira")
    private java.time.LocalDateTime emailActivacionExpira;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getGoogleSub() { return googleSub; }
    public void setGoogleSub(String googleSub) { this.googleSub = googleSub; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }

    public String getCelular() { return celular; }
    public void setCelular(String celular) { this.celular = celular; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

    public String getResetToken() { return resetToken; }
    public void setResetToken(String resetToken) { this.resetToken = resetToken; }

    public java.time.LocalDateTime getResetTokenExpira() { return resetTokenExpira; }
    public void setResetTokenExpira(java.time.LocalDateTime resetTokenExpira) { this.resetTokenExpira = resetTokenExpira; }

    public Boolean getEmailVerificado() { return emailVerificado; }
    public void setEmailVerificado(Boolean emailVerificado) { this.emailVerificado = emailVerificado; }
    public Boolean getWhatsappVerificado() { return whatsappVerificado; }
    public void setWhatsappVerificado(Boolean whatsappVerificado) { this.whatsappVerificado = whatsappVerificado; }
    public String getWhatsappCodigoHash() { return whatsappCodigoHash; }
    public void setWhatsappCodigoHash(String whatsappCodigoHash) { this.whatsappCodigoHash = whatsappCodigoHash; }
    public java.time.LocalDateTime getWhatsappCodigoExpira() { return whatsappCodigoExpira; }
    public void setWhatsappCodigoExpira(java.time.LocalDateTime whatsappCodigoExpira) { this.whatsappCodigoExpira = whatsappCodigoExpira; }
    public String getWhatsappVerificacionCodigo() { return whatsappVerificacionCodigo; }
    public void setWhatsappVerificacionCodigo(String whatsappVerificacionCodigo) { this.whatsappVerificacionCodigo = whatsappVerificacionCodigo; }
    public Boolean getPasoWhatsappAgendado() { return pasoWhatsappAgendado; }
    public void setPasoWhatsappAgendado(Boolean pasoWhatsappAgendado) { this.pasoWhatsappAgendado = pasoWhatsappAgendado; }
    public Boolean getPasoInstagramCompletado() { return pasoInstagramCompletado; }
    public void setPasoInstagramCompletado(Boolean pasoInstagramCompletado) { this.pasoInstagramCompletado = pasoInstagramCompletado; }
    public String getEmailActivacionToken() { return emailActivacionToken; }
    public void setEmailActivacionToken(String emailActivacionToken) { this.emailActivacionToken = emailActivacionToken; }
    public java.time.LocalDateTime getEmailActivacionExpira() { return emailActivacionExpira; }
    public void setEmailActivacionExpira(java.time.LocalDateTime emailActivacionExpira) { this.emailActivacionExpira = emailActivacionExpira; }
}
