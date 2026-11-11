package com.example.tpv_flux.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Locale;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "username", nullable = false, unique = true, length = 60)
    private String username;

    @Column(name = "password", nullable = false, length = 120)
    private String password;

    @Column(name = "nombre_completo", nullable = false, length = 120)
    private String nombreCompleto;

    @Column(name = "puesto", nullable = false, length = 60)
    private String puesto;

    @Column(name = "turno", nullable = false, length = 60)
    private String turno;

    @Column(name = "sucursal", nullable = false, length = 100)
    private String sucursal;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    public Usuario() {
    }

    public Usuario(String username, String password, String nombreCompleto, String puesto, String turno, String sucursal, Boolean activo) {
        this.username = username;
        this.password = password;
        this.nombreCompleto = nombreCompleto;
        this.puesto = puesto;
        this.turno = turno;
        this.sucursal = sucursal;
        this.activo = activo;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public String getPuesto() {
        return puesto;
    }

    public void setPuesto(String puesto) {
        this.puesto = puesto;
    }

    public String getTurno() {
        return turno;
    }

    public void setTurno(String turno) {
        this.turno = turno;
    }

    public String getSucursal() {
        return sucursal;
    }

    public void setSucursal(String sucursal) {
        this.sucursal = sucursal;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    /**
     * Calcula las iniciales a partir del nombre completo (ej. "Ana Martínez" -> "AM").
     */
    public String getIniciales() {
        if (nombreCompleto == null || nombreCompleto.isBlank()) {
            return "US";
        }
        String[] parts = nombreCompleto.trim().split("\\s+");
        if (parts.length == 1) {
            String p = parts[0].toUpperCase(Locale.ROOT);
            return p.length() >= 2 ? p.substring(0, 2) : p;
        }
        return (String.valueOf(parts[0].charAt(0)) + parts[1].charAt(0)).toUpperCase(Locale.ROOT);
    }

    public String getPuestoYTurno() {
        return (puesto != null ? puesto : "Cajero") + " · " + (turno != null ? turno : "Turno AM");
    }
}
