package com.example.tpv_flux.dao;

import com.example.tpv_flux.config.HibernateUtil;
import com.example.tpv_flux.model.MovimientoInventario;
import com.example.tpv_flux.model.Notificacion;
import com.example.tpv_flux.model.Producto;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.util.Collections;
import java.util.List;

public class InventarioDao {

    public List<MovimientoInventario> findRecent(int limit) {
        SessionFactory sf = HibernateUtil.getSessionFactory();
        if (sf == null) {
            return Collections.emptyList();
        }
        try (Session session = sf.openSession()) {
            return session.createQuery(
                            "SELECT m FROM MovimientoInventario m JOIN FETCH m.producto p JOIN FETCH p.categoria ORDER BY m.fecha DESC",
                            MovimientoInventario.class
                    )
                    .setMaxResults(limit)
                    .getResultList();
        }
    }

    /**
     * Registra una ENTRADA o SALIDA de inventario, actualizando el stock_actual del producto en la misma transacción.
     */
    public MovimientoInventario registrarMovimiento(Integer productoId, String tipo, int cantidad) {
        SessionFactory sf = HibernateUtil.getSessionFactory();
        if (sf == null) {
            throw new IllegalStateException("No hay conexión activa a la base de datos PostgreSQL.");
        }
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a cero.");
        }

        try (Session session = sf.openSession()) {
            Transaction tx = null;
            try {
                tx = session.beginTransaction();

                Producto producto = session.get(Producto.class, productoId);
                if (producto == null) {
                    throw new IllegalArgumentException("No se encontró el producto con ID: " + productoId);
                }

                int stockActual = producto.getStockActual() != null ? producto.getStockActual() : 0;
                int nuevoStock;
                if (MovimientoInventario.TIPO_ENTRADA.equalsIgnoreCase(tipo)) {
                    nuevoStock = stockActual + cantidad;
                } else if (MovimientoInventario.TIPO_SALIDA.equalsIgnoreCase(tipo)) {
                    if (stockActual < cantidad) {
                        throw new IllegalStateException("Stock insuficiente para el producto: " + producto.getNombre());
                    }
                    nuevoStock = stockActual - cantidad;
                } else {
                    throw new IllegalArgumentException("Tipo de movimiento inválido (use ENTRADA o SALIDA): " + tipo);
                }

                producto.setStockActual(nuevoStock);

                MovimientoInventario movimiento = new MovimientoInventario(
                        producto,
                        tipo.toUpperCase(),
                        cantidad,
                        nuevoStock
                );
                session.persist(movimiento);

                if (nuevoStock == 0) {
                    session.persist(new Notificacion("Sin stock", producto.getNombre() + " se quedó sin stock"));
                } else if (nuevoStock <= 6 && stockActual > 6) {
                    session.persist(new Notificacion("Stock bajo", producto.getNombre() + " tiene solo " + nuevoStock + " unidades"));
                } else if (MovimientoInventario.TIPO_ENTRADA.equalsIgnoreCase(tipo)) {
                    session.persist(new Notificacion("Inventario", "Se registró entrada de " + cantidad + " unidades: " + producto.getNombre()));
                }

                tx.commit();
                return movimiento;
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
