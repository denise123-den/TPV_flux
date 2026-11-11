package com.example.tpv_flux.dao;

import com.example.tpv_flux.config.HibernateUtil;
import com.example.tpv_flux.model.DetalleVenta;
import com.example.tpv_flux.model.MetodoPago;
import com.example.tpv_flux.model.MovimientoInventario;
import com.example.tpv_flux.model.Notificacion;
import com.example.tpv_flux.model.Producto;
import com.example.tpv_flux.model.TicketLineItem;
import com.example.tpv_flux.model.Venta;
import com.example.tpv_flux.util.CurrencyUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

public class VentaDao {

    private static final BigDecimal IVA_RATE = new BigDecimal("0.16");

    public List<Venta> findRecent(int limit) {
        SessionFactory sf = HibernateUtil.getSessionFactory();
        if (sf == null) {
            return Collections.emptyList();
        }
        try (Session session = sf.openSession()) {
            return session.createQuery(
                            "SELECT v FROM Venta v JOIN FETCH v.metodoPago ORDER BY v.fechaVenta DESC",
                            Venta.class
                    )
                    .setMaxResults(limit)
                    .getResultList();
        }
    }

    public List<Venta> findAllWithDetailsSince(LocalDateTime fromDate) {
        SessionFactory sf = HibernateUtil.getSessionFactory();
        if (sf == null) {
            return Collections.emptyList();
        }
        try (Session session = sf.openSession()) {
            return session.createQuery(
                            "SELECT DISTINCT v FROM Venta v " +
                                    "JOIN FETCH v.metodoPago " +
                                    "LEFT JOIN FETCH v.detalles d " +
                                    "LEFT JOIN FETCH d.producto p " +
                                    "LEFT JOIN FETCH p.categoria " +
                                    "WHERE v.fechaVenta >= :fromDate AND v.estado = :estado " +
                                    "ORDER BY v.fechaVenta DESC",
                            Venta.class
                    )
                    .setParameter("fromDate", fromDate)
                    .setParameter("estado", Venta.ESTADO_COMPLETADA)
                    .getResultList();
        }
    }

    public String generarSiguienteFolio() {
        SessionFactory sf = HibernateUtil.getSessionFactory();
        if (sf == null) {
            return "#0482";
        }
        try (Session session = sf.openSession()) {
            return resolverSiguienteFolioUnico(session, null);
        }
    }

    private String resolverSiguienteFolioUnico(Session session, String folioPreferido) {
        if (folioPreferido != null && !folioPreferido.isBlank()) {
            Long existePref = session.createQuery(
                            "SELECT COUNT(v) FROM Venta v WHERE v.folioTicket = :folio",
                            Long.class
                    )
                    .setParameter("folio", folioPreferido)
                    .uniqueResult();
            if (existePref == null || existePref == 0) {
                return folioPreferido;
            }
        }

        Long totalVentas = session.createQuery("SELECT COUNT(v) FROM Venta v", Long.class).uniqueResult();
        long siguienteNumero = 478 + (totalVentas != null ? totalVentas : 0);
        String candidato = String.format("#%04d", siguienteNumero);

        Long existe = session.createQuery(
                        "SELECT COUNT(v) FROM Venta v WHERE v.folioTicket = :folio",
                        Long.class
                )
                .setParameter("folio", candidato)
                .uniqueResult();

        while (existe != null && existe > 0) {
            siguienteNumero++;
            candidato = String.format("#%04d", siguienteNumero);
            existe = session.createQuery(
                            "SELECT COUNT(v) FROM Venta v WHERE v.folioTicket = :folio",
                            Long.class
                    )
                    .setParameter("folio", candidato)
                    .uniqueResult();
        }
        return candidato;
    }

    /**
     * Registra una venta completa en PostgreSQL:
     * 1. Inserta el ticket en 'ventas' y sus renglones en 'detalle_ventas'.
     * 2. Descuenta el 'stock_actual' en 'productos'.
     * 3. Registra cada movimiento 'SALIDA' en 'inventario'.
     * 4. Registra notificaciones automáticas de venta y alertas de stock.
     */
    public Venta registrarVenta(String folioTicket, List<TicketLineItem> items, String nombreMetodoPago) {
        SessionFactory sf = HibernateUtil.getSessionFactory();
        if (sf == null) {
            throw new IllegalStateException("No hay conexión activa a PostgreSQL.");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("El ticket no contiene artículos para cobrar.");
        }

        try (Session session = sf.openSession()) {
            Transaction tx = null;
            try {
                tx = session.beginTransaction();

                String folioUnico = resolverSiguienteFolioUnico(session, folioTicket);

                MetodoPago metodoPago = session.createQuery(
                                "FROM MetodoPago m WHERE UPPER(m.nombre) = UPPER(:nombre)",
                                MetodoPago.class
                        )
                        .setParameter("nombre", nombreMetodoPago)
                        .setMaxResults(1)
                        .uniqueResultOptional()
                        .orElseGet(() -> {
                            MetodoPago nuevo = new MetodoPago(nombreMetodoPago.toUpperCase(), true);
                            session.persist(nuevo);
                            return nuevo;
                        });

                BigDecimal subtotal = BigDecimal.ZERO;
                for (TicketLineItem line : items) {
                    BigDecimal lineSub = BigDecimal.valueOf(line.getSubtotal()).setScale(2, RoundingMode.HALF_UP);
                    subtotal = subtotal.add(lineSub);
                }
                BigDecimal iva = subtotal.multiply(IVA_RATE).setScale(2, RoundingMode.HALF_UP);
                BigDecimal total = subtotal.add(iva).setScale(2, RoundingMode.HALF_UP);

                Venta venta = new Venta(
                        folioUnico,
                        subtotal,
                        iva,
                        total,
                        metodoPago,
                        Venta.ESTADO_COMPLETADA
                );

                for (TicketLineItem line : items) {
                    Producto producto = null;
                    if (line.getProduct().getId() != null) {
                        producto = session.get(Producto.class, line.getProduct().getId());
                    }
                    if (producto == null) {
                        producto = session.createQuery(
                                        "FROM Producto p WHERE LOWER(p.nombre) = LOWER(:nombre)",
                                        Producto.class
                                )
                                .setParameter("nombre", line.getProduct().getName())
                                .setMaxResults(1)
                                .uniqueResult();
                    }
                    if (producto == null) {
                        throw new IllegalStateException("El producto '" + line.getProduct().getName() + "' no existe en la base de datos.");
                    }

                    int stockAnterior = producto.getStockActual() != null ? producto.getStockActual() : 0;
                    if (stockAnterior < line.getQuantity()) {
                        throw new IllegalStateException(
                                "Stock insuficiente para '" + producto.getNombre()
                                        + "' (disponible: " + stockAnterior + ", solicitado: " + line.getQuantity() + ")."
                        );
                    }

                    int nuevoStock = stockAnterior - line.getQuantity();
                    producto.setStockActual(nuevoStock);

                    BigDecimal precioUnitario = BigDecimal.valueOf(line.getProduct().getPrice()).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal importeLinea = precioUnitario.multiply(BigDecimal.valueOf(line.getQuantity())).setScale(2, RoundingMode.HALF_UP);

                    DetalleVenta detalle = new DetalleVenta(producto, line.getQuantity(), precioUnitario, importeLinea);
                    venta.agregarDetalle(detalle);

                    MovimientoInventario salida = new MovimientoInventario(
                            producto,
                            MovimientoInventario.TIPO_SALIDA,
                            line.getQuantity(),
                            nuevoStock
                    );
                    session.persist(salida);

                    if (nuevoStock == 0) {
                        session.persist(new Notificacion("Stock", producto.getNombre() + " se quedó sin stock"));
                    } else if (nuevoStock <= 6 && stockAnterior > 6) {
                        session.persist(new Notificacion("Stock bajo", producto.getNombre() + " tiene solo " + nuevoStock + " unidades"));
                    }
                }

                session.persist(venta);
                session.persist(new Notificacion(
                        "Venta",
                        "Venta completada por " + CurrencyUtil.format(total.doubleValue()) + " — Ticket " + folioUnico
                ));

                tx.commit();
                return venta;
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
