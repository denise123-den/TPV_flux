package com.example.tpv_flux.config;

import com.example.tpv_flux.model.Usuario;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Mantiene en memoria la sesión activa del cajero/usuario que inició sesión y su sucursal/terminal.
 */
public final class SessionManager {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a", Locale.US);

    private static Usuario usuarioActivo = new Usuario(
            "ana.martinez",
            "123456",
            "Ana Martínez",
            "Cajera",
            "Turno AM",
            "Sucursal Centro — Caja 1",
            true
    );
    private static String terminalSucursal = "Sucursal Centro · Caja 1";
    private static String horaAperturaTurno = "09:03 am";

    private SessionManager() {
    }

    public static void iniciarSesion(Usuario usuario, String terminalSeleccionada) {
        if (usuario != null) {
            usuarioActivo = usuario;
        }
        if (terminalSeleccionada != null && !terminalSeleccionada.isBlank()) {
            terminalSucursal = terminalSeleccionada.replace("—", "·").replace("  ", " ");
        } else if (usuario != null && usuario.getSucursal() != null) {
            terminalSucursal = usuario.getSucursal().replace("—", "·").replace("  ", " ");
        }
        horaAperturaTurno = LocalTime.now().format(TIME_FMT).toLowerCase(Locale.ROOT);
    }

    public static void cerrarSesion() {
        usuarioActivo = new Usuario(
                "ana.martinez",
                "123456",
                "Ana Martínez",
                "Cajera",
                "Turno AM",
                "Sucursal Centro — Caja 1",
                true
        );
    }

    public static Usuario getUsuarioActivo() {
        return usuarioActivo;
    }

    public static String getTerminalSucursal() {
        return terminalSucursal;
    }

    public static String getNombreSucursalCorto() {
        if (terminalSucursal == null) {
            return "Sucursal Centro";
        }
        int idx = terminalSucursal.indexOf('·');
        if (idx > 0) {
            return terminalSucursal.substring(0, idx).trim();
        }
        return terminalSucursal;
    }

    public static String getHoraAperturaTurno() {
        return horaAperturaTurno;
    }
}
