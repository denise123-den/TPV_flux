package com.example.tpv_flux.dao;

import com.example.tpv_flux.config.HibernateUtil;
import com.example.tpv_flux.model.MovimientoInventario;
import com.example.tpv_flux.model.Producto;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ProductoDao {

    public List<Producto> findAllActive() {
        SessionFactory sf = HibernateUtil.getSessionFactory();
        if (sf == null) {
            return Collections.emptyList();
        }
        try (Session session = sf.openSession()) {
            return session.createQuery(
                    "SELECT p FROM Producto p JOIN FETCH p.categoria WHERE p.activo = true ORDER BY p.id ASC",
                    Producto.class
            ).getResultList();
        }
    }

    public Optional<Producto> findById(Integer id) {
        SessionFactory sf = HibernateUtil.getSessionFactory();
        if (sf == null || id == null) {
            return Optional.empty();
        }
        try (Session session = sf.openSession()) {
            return session.createQuery(
                            "SELECT p FROM Producto p JOIN FETCH p.categoria WHERE p.id = :id",
                            Producto.class
                    )
                    .setParameter("id", id)
                    .uniqueResultOptional();
        }
    }

    public Producto save(Producto producto) {
        SessionFactory sf = HibernateUtil.getSessionFactory();
        if (sf == null) {
            throw new IllegalStateException("No hay conexión activa a la base de datos PostgreSQL.");
        }
        try (Session session = sf.openSession()) {
            Transaction tx = null;
            try {
                tx = session.beginTransaction();
                boolean isNew = producto.getId() == null;
                if (isNew) {
                    session.persist(producto);
                    if (producto.getStockActual() != null && producto.getStockActual() > 0) {
                        MovimientoInventario entradaInicial = new MovimientoInventario(
                                producto,
                                MovimientoInventario.TIPO_ENTRADA,
                                producto.getStockActual(),
                                producto.getStockActual()
                        );
                        session.persist(entradaInicial);
                    }
                } else {
                    producto = session.merge(producto);
                }
                tx.commit();
                return producto;
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

    public void updateFoto(Integer productoId, String nombreORutaFoto) {
        SessionFactory sf = HibernateUtil.getSessionFactory();
        if (sf == null || productoId == null) {
            return;
        }
        try (Session session = sf.openSession()) {
            Transaction tx = null;
            try {
                tx = session.beginTransaction();
                Producto p = session.get(Producto.class, productoId);
                if (p != null) {
                    p.setFoto(nombreORutaFoto);
                }
                tx.commit();
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

    /**
     * Elimina un producto de PostgreSQL junto con sus movimientos de inventario y detalles asociados.
     */
    public void deleteById(Integer productoId) {
        SessionFactory sf = HibernateUtil.getSessionFactory();
        if (sf == null || productoId == null) {
            return;
        }
        try (Session session = sf.openSession()) {
            Transaction tx = null;
            try {
                tx = session.beginTransaction();
                session.createMutationQuery("DELETE FROM MovimientoInventario m WHERE m.producto.id = :id")
                        .setParameter("id", productoId)
                        .executeUpdate();
                session.createMutationQuery("DELETE FROM DetalleVenta d WHERE d.producto.id = :id")
                        .setParameter("id", productoId)
                        .executeUpdate();
                Producto p = session.get(Producto.class, productoId);
                if (p != null) {
                    session.remove(p);
                }
                tx.commit();
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

    /**
     * Elimina por nombre (sin importar mayúsculas/minúsculas) un producto de PostgreSQL.
     */
    public int deleteByNombre(String nombre) {
        SessionFactory sf = HibernateUtil.getSessionFactory();
        if (sf == null || nombre == null || nombre.isBlank()) {
            return 0;
        }
        try (Session session = sf.openSession()) {
            Transaction tx = null;
            try {
                tx = session.beginTransaction();
                List<Producto> encontrados = session.createQuery(
                                "FROM Producto p WHERE LOWER(TRIM(p.nombre)) = LOWER(:nombre)",
                                Producto.class
                        )
                        .setParameter("nombre", nombre.trim())
                        .getResultList();

                for (Producto p : encontrados) {
                    session.createMutationQuery("DELETE FROM MovimientoInventario m WHERE m.producto.id = :id")
                            .setParameter("id", p.getId())
                            .executeUpdate();
                    session.createMutationQuery("DELETE FROM DetalleVenta d WHERE d.producto.id = :id")
                            .setParameter("id", p.getId())
                            .executeUpdate();
                    session.remove(p);
                }
                tx.commit();
                return encontrados.size();
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
