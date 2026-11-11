package com.example.tpv_flux.dao;

import com.example.tpv_flux.config.HibernateUtil;
import com.example.tpv_flux.model.MetodoPago;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class MetodoPagoDao {

    public List<MetodoPago> findAllActive() {
        SessionFactory sf = HibernateUtil.getSessionFactory();
        if (sf == null) {
            return Collections.emptyList();
        }
        try (Session session = sf.openSession()) {
            return session.createQuery(
                    "FROM MetodoPago m WHERE m.activo = true ORDER BY m.id ASC",
                    MetodoPago.class
            ).getResultList();
        }
    }

    public Optional<MetodoPago> findByNombre(String nombre) {
        SessionFactory sf = HibernateUtil.getSessionFactory();
        if (sf == null) {
            return Optional.empty();
        }
        try (Session session = sf.openSession()) {
            return session.createQuery(
                            "FROM MetodoPago m WHERE UPPER(m.nombre) = UPPER(:nombre)",
                            MetodoPago.class
                    )
                    .setParameter("nombre", nombre)
                    .setMaxResults(1)
                    .uniqueResultOptional();
        }
    }

    public MetodoPago getOrCreate(String nombre) {
        Optional<MetodoPago> existing = findByNombre(nombre);
        if (existing.isPresent()) {
            return existing.get();
        }
        SessionFactory sf = HibernateUtil.getSessionFactory();
        if (sf == null) {
            throw new IllegalStateException("No hay conexión activa a la base de datos PostgreSQL.");
        }
        try (Session session = sf.openSession()) {
            Transaction tx = null;
            try {
                tx = session.beginTransaction();
                MetodoPago nuevo = new MetodoPago(nombre.toUpperCase(), true);
                session.persist(nuevo);
                tx.commit();
                return nuevo;
            } catch (Exception e) {
                if (tx != null && tx.isActive()) {
                    try {
                        tx.rollback();
                    } catch (Exception ignored) {
                    }
                }
                throw e;
            }
        }
    }
}
