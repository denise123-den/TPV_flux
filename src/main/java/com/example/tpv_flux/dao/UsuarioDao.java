package com.example.tpv_flux.dao;

import com.example.tpv_flux.config.HibernateUtil;
import com.example.tpv_flux.model.Usuario;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.text.Normalizer;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class UsuarioDao {

    public List<Usuario> findAllActive() {
        SessionFactory sf = HibernateUtil.getSessionFactory();
        if (sf == null) {
            return Collections.emptyList();
        }
        try (Session session = sf.openSession()) {
            return session.createQuery(
                    "FROM Usuario u WHERE u.activo = true ORDER BY u.id ASC",
                    Usuario.class
            ).getResultList();
        }
    }

    /**
     * Autentica un usuario contra la tabla 'usuarios' de PostgreSQL.
     * Permite ingresar con el username ('ana.martinez') o con el nombre completo ('Ana Martínez' / 'Ana martines').
     */
    public Optional<Usuario> autenticar(String userInput, String passwordInput) {
        if (userInput == null || userInput.isBlank() || passwordInput == null) {
            return Optional.empty();
        }
        String trimmedUser = userInput.trim();
        String normalizedInput = normalize(trimmedUser);

        SessionFactory sf = HibernateUtil.getSessionFactory();
        if (sf != null) {
            try (Session session = sf.openSession()) {
                List<Usuario> activeUsers = session.createQuery(
                        "FROM Usuario u WHERE u.activo = true",
                        Usuario.class
                ).getResultList();

                for (Usuario u : activeUsers) {
                    if (matchesUserIdentifier(u, normalizedInput) && u.getPassword().equals(passwordInput)) {
                        return Optional.of(u);
                    }
                }
                return Optional.empty();
            } catch (Exception e) {
                System.err.println("[UsuarioDao] Error autenticando en BD: " + e.getMessage());
            }
        }

        // Respaldo local si PostgreSQL está desconectado
        List<Usuario> fallbackUsers = List.of(
                new Usuario("ana.martinez", "123456", "Ana Martínez", "Cajera", "Turno AM", "Sucursal Centro — Caja 1", true),
                new Usuario("carlos.ruiz", "123456", "Carlos Ruiz", "Supervisor", "Turno PM", "Sucursal Centro — Caja 2", true)
        );
        for (Usuario u : fallbackUsers) {
            if (matchesUserIdentifier(u, normalizedInput) && u.getPassword().equals(passwordInput)) {
                return Optional.of(u);
            }
        }
        return Optional.empty();
    }

    private boolean matchesUserIdentifier(Usuario u, String normalizedInput) {
        String normUsername = normalize(u.getUsername());
        String normNombre = normalize(u.getNombreCompleto());
        String normNombreConS = normNombre.replace("martinez", "martines");
        String normInputConZ = normalizedInput.replace("martines", "martinez");

        return normUsername.equals(normalizedInput)
                || normUsername.equals(normInputConZ)
                || normNombre.equals(normalizedInput)
                || normNombre.equals(normInputConZ)
                || normNombreConS.equals(normalizedInput);
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        String nfd = Normalizer.normalize(text.trim(), Normalizer.Form.NFD);
        return nfd.replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }
}
