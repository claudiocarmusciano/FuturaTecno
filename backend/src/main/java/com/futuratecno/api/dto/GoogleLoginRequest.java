package com.futuratecno.api.dto;

/** Payload del login con Google: el ID token (credential) que emite Google Identity Services. */
public class GoogleLoginRequest {
    private String credential;

    public String getCredential() { return credential; }
    public void setCredential(String credential) { this.credential = credential; }
}
