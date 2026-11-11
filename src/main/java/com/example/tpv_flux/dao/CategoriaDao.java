package com.example.tpv_flux.dao;

import com.example.tpv_flux.config.HibernateUtil;
import com.example.tpv_flux.model.Categoria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class CategoriaDao {

    public List<Categoria> findAllActive() {
        SessionFactory sf = HibernateUtil.getSessionFactory();
        if (sf == null) {
            return Collections.emptyList();
        }
        try (Session session = sf.openSession()) {
            return session.createQuery(
                    "FROM Categoria c WHERE c.activo = true ORDER BY c.id ASC",
                    Categoria.class
            ).getResultList();
        }
    }

    public Optional<Categoria> findByNombre(String nombre) {
        SessionFactory sf = HibernateUtil.getSessionFactory();
        if (sf == null) {
            return Optional.empty();
        }
        try (Session session = sf.openSession()) {
            return session.createQuery(
                            "FROM Categoria c WHERE LOWER(c.nombre) = LOWER(:nombre)",
                            Categoria.class
                    )
                    .setParameter("nombre", nombre)
                    .setMaxResults(1)
                    .uniqueResultOptional();
        }
    }

    public Categoria save(Categoria categoria) {
        SessionFactory sf = HibernateUtil.getSessionFactory();
        if (sf == null) {
            throw new IllegalStateException("No hay conexión activa a la base de datos PostgreSQL.");
        }
        try (Session session = sf.openSession()) {
            Transaction tx = null;
            try {
                tx = session.beginTransaction();
                if (categoria.getId() == null) {
                    session.persist(categoria);
                } else {
                    categoria = session.merge(categoria);
                }
                tx.commit();
                return categoria;
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
