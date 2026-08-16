package com.futuratecno.api.dto;

public class RegisterRequest {
    private String email;
    private String password;
    private String nombre;
    private String apellido;
    private String celular;
    private String dni;
    private String fechaNacimiento;
    private String instagramUsuario;
    private Boolean aceptaBases;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }

    public String getCelular() { return celular; }
    public void setCelular(String celular) { this.celular = celular; }

    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }

    public String getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(String fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }

    public String getInstagramUsuario() { return instagramUsuario; }
    public void setInstagramUsuario(String instagramUsuario) { this.instagramUsuario = instagramUsuario; }

    public Boolean getAceptaBases() { return aceptaBases; }
    public void setAceptaBases(Boolean aceptaBases) { this.aceptaBases = aceptaBases; }
}
