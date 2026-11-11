package com.example.tpv_flux.config;

import com.example.tpv_flux.model.Categoria;
import com.example.tpv_flux.model.DetalleVenta;
import com.example.tpv_flux.model.MetodoPago;
import com.example.tpv_flux.model.MovimientoInventario;
import com.example.tpv_flux.model.Notificacion;
import com.example.tpv_flux.model.Producto;
import com.example.tpv_flux.model.Usuario;
import com.example.tpv_flux.model.Venta;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Inicializador automático de datos, si la base de datos PostgreSQL está conectada pero vacía.
 */
public final class DatabaseSeeder {

    private static boolean seededChecked = false;

    private DatabaseSeeder() {
    }

    public static synchronized void seedIfEmpty() {
        if (seededChecked) {
            return;
        }
        seededChecked = true;

        SessionFactory sf = HibernateUtil.getSessionFactory();
        if (sf == null) {
            return;
        }

        try (Session session = sf.openSession()) {
            Transaction tx = null;
            try {
                Long userCount = session.createQuery("SELECT COUNT(u) FROM Usuario u", Long.class).uniqueResult();
                Long productCount = session.createQuery("SELECT COUNT(p) FROM Producto p", Long.class).uniqueResult();
                Long ventaCount = session.createQuery("SELECT COUNT(v) FROM Venta v", Long.class).uniqueResult();

                if (userCount != null && userCount > 0 && productCount != null && productCount > 0 && ventaCount != null && ventaCount > 0) {
                    return;
                }

                tx = session.beginTransaction();

            // 0. Usuarios del sistema (Mockup Página 1 y Sidebar)
            ensureUsuario(
                    session,
                    "ana.martinez",
                    "123456",
                    "Ana Martínez",
                    "Cajera",
                    "Turno AM",
                    "Sucursal Centro — Caja 1"
            );
            ensureUsuario(
                    session,
                    "carlos.ruiz",
                    "123456",
                    "Carlos Ruiz",
                    "Supervisor",
                    "Turno PM",
                    "Sucursal Centro — Caja 2"
            );

            // 1. Métodos de pago
            MetodoPago mpEfectivo = ensureMetodoPago(session, MetodoPago.EFECTIVO);
            MetodoPago mpTarjeta = ensureMetodoPago(session, MetodoPago.TARJETA);

            // 2. Categorías
            Categoria catCelulares = getOrCreateCategoria(session, "Celulares");
            Categoria catComputo = getOrCreateCategoria(session, "Cómputo");
            Categoria catAudio = getOrCreateCategoria(session, "Audio");
            getOrCreateCategoria(session, "Cables y cargadores");
            getOrCreateCategoria(session, "Protección");
            getOrCreateCategoria(session, "Accesorios gaming");

            Producto p1;
            Producto p2;
            Producto p3;
            Producto p4;
            Producto p5;
            Producto p6;
            Producto p7;
            Producto p8;

            if (productCount == null || productCount == 0) {
                // 3. Productos iniciales
                p1 = new Producto(
                        "Funda iPhone 14",
                        "SKU: FN-IP14-001 | TAG: FUNDA | Funda protectora para iPhone 14",
                        catCelulares,
                        new BigDecimal("120.00"),
                        new BigDecimal("249.00"),
                        "iphon.jpeg",
                        18,
                        true
                );
                p2 = new Producto(
                        "Cable USB-C 1m",
                        "SKU: CB-USBC-100 | TAG: CABLE | Cable de carga rápida USB-C de 1 metro",
                        catComputo,
                        new BigDecimal("45.00"),
                        new BigDecimal("99.00"),
                        "d1.jpeg",
                        42,
                        true
                );
                p3 = new Producto(
                        "Audífonos Bluetooth X2",
                        "SKU: AU-BT-X2 | TAG: AUDIO | Audífonos inalámbricos Bluetooth con estuche de carga",
                        catAudio,
                        new BigDecimal("230.00"),
                        new BigDecimal("450.00"),
                        "biblio.jpeg",
                        6,
                        true
                );
                p4 = new Producto(
                        "Cargador 20W",
                        "SKU: CG-20W-002 | TAG: CARGA | Adaptador de corriente USB-C de 20W carga rápida",
                        catComputo,
                        new BigDecimal("95.00"),
                        new BigDecimal("189.00"),
                        "upp.jpeg",
                        25,
                        true
                );
                p5 = new Producto(
                        "Mica templada iPhone 14",
                        "SKU: MC-IP14-01 | TAG: MICA | Cristal templado 9H para pantalla de iPhone 14",
                        catCelulares,
                        new BigDecimal("25.00"),
                        new BigDecimal("79.00"),
                        "logo.png",
                        0,
                        true
                );
                p6 = new Producto(
                        "Power bank 10000mAh",
                        "SKU: PB-10K-003 | TAG: POWER | Batería portátil de 10000mAh doble salida USB",
                        catComputo,
                        new BigDecimal("210.00"),
                        new BigDecimal("399.00"),
                        "logotarea.png",
                        11,
                        true
                );
                p7 = new Producto(
                        "Memoria USB 32GB",
                        "SKU: MU-32GB-01 | TAG: USB | Memoria flash USB 3.0 de 32GB metálica",
                        catComputo,
                        new BigDecimal("65.00"),
                        new BigDecimal("129.00"),
                        "d1.jpeg",
                        30,
                        true
                );
                p8 = new Producto(
                        "Soporte celular",
                        "SKU: SP-CEL-004 | TAG: SOPORTE | Soporte ajustable de escritorio para celular",
                        catCelulares,
                        new BigDecimal("70.00"),
                        new BigDecimal("149.00"),
                        "iphon.jpeg",
                        14,
                        true
                );

                session.persist(p1);
                session.persist(p2);
                session.persist(p3);
                session.persist(p4);
                session.persist(p5);
                session.persist(p6);
                session.persist(p7);
                session.persist(p8);

                // 4. Movimientos de inventario iniciales
                LocalDateTime now = LocalDateTime.now();
                MovimientoInventario m1 = new MovimientoInventario(p1, MovimientoInventario.TIPO_ENTRADA, 50, 18);
                m1.setFecha(now.minusHours(4));
                MovimientoInventario m2 = new MovimientoInventario(p2, MovimientoInventario.TIPO_SALIDA, 2, 42);
                m2.setFecha(now.minusHours(3));
                MovimientoInventario m3 = new MovimientoInventario(p3, MovimientoInventario.TIPO_ENTRADA, 20, 6);
                m3.setFecha(now.minusDays(1));
                MovimientoInventario m4 = new MovimientoInventario(p6, MovimientoInventario.TIPO_SALIDA, 1, 11);
                m4.setFecha(now.minusDays(1).minusHours(4));
                MovimientoInventario m5 = new MovimientoInventario(p7, MovimientoInventario.TIPO_ENTRADA, 30, 30);
                m5.setFecha(now.minusDays(2));

                session.persist(m1);
                session.persist(m2);
                session.persist(m3);
                session.persist(m4);
                session.persist(m5);

                // 5. Notificaciones iniciales
                Notificacion n1 = new Notificacion("Stock bajo", "Audífonos Bluetooth X2 tiene solo 6 unidades");
                n1.setFechaCreacion(now.minusMinutes(12));
                Notificacion n2 = new Notificacion("Caja", "Recuerda cerrar la caja del turno matutino");
                n2.setFechaCreacion(now.minusMinutes(38));
                Notificacion n3 = new Notificacion("Devolución", "Se registró una devolución en el ticket #0481");
                n3.setFechaCreacion(now.minusHours(1));
                Notificacion n4 = new Notificacion("Sistema", "Actualización disponible: TPV Core v1.1");
                n4.setFechaCreacion(now.minusHours(3));
                Notificacion n5 = new Notificacion("Venta", "Venta completada por $450.00 — Ticket #0478");
                n5.setLeida(true);
                n5.setFechaCreacion(now.minusHours(5));
                Notificacion n6 = new Notificacion("Stock", "Mica templada iPhone 14 se quedó sin stock");
                n6.setLeida(true);
                n6.setFechaCreacion(now.minusDays(1));
                Notificacion n7 = new Notificacion("Inventario", "Se registró entrada de 50 unidades: Funda iPhone 14");
                n7.setLeida(true);
                n7.setFechaCreacion(now.minusDays(1).minusHours(2));

                session.persist(n1);
                session.persist(n2);
                session.persist(n3);
                session.persist(n4);
                session.persist(n5);
                session.persist(n6);
                session.persist(n7);
            } else {
                p1 = findProductoByNombre(session, "Funda iPhone 14");
                p2 = findProductoByNombre(session, "Cable USB-C 1m");
                p3 = findProductoByNombre(session, "Audífonos Bluetooth X2");
                p4 = findProductoByNombre(session, "Cargador 20W");
            }

            // 6. Ventas iniciales (para poblar Reportes e historial desde el primer inicio)
            if ((ventaCount == null || ventaCount == 0) && p1 != null && p2 != null && p3 != null && p4 != null) {
                LocalDateTime now = LocalDateTime.now();

                Venta v1 = new Venta("#0478", new BigDecimal("387.93"), new BigDecimal("62.07"), new BigDecimal("450.00"), mpEfectivo, Venta.ESTADO_COMPLETADA);
                v1.setFechaVenta(now.minusHours(3));
                v1.agregarDetalle(new DetalleVenta(p3, 1, new BigDecimal("450.00"), new BigDecimal("450.00")));
                session.persist(v1);

                Venta v2 = new Venta("#0479", new BigDecimal("162.93"), new BigDecimal("26.07"), new BigDecimal("189.00"), mpTarjeta, Venta.ESTADO_COMPLETADA);
                v2.setFechaVenta(now.minusHours(2));
                v2.agregarDetalle(new DetalleVenta(p4, 1, new BigDecimal("189.00"), new BigDecimal("189.00")));
                session.persist(v2);

                Venta v3 = new Venta("#0480", new BigDecimal("85.34"), new BigDecimal("13.66"), new BigDecimal("99.00"), mpEfectivo, Venta.ESTADO_COMPLETADA);
                v3.setFechaVenta(now.minusMinutes(90));
                v3.agregarDetalle(new DetalleVenta(p2, 1, new BigDecimal("99.00"), new BigDecimal("99.00")));
                session.persist(v3);

                Venta v4 = new Venta("#0481", new BigDecimal("429.31"), new BigDecimal("68.69"), new BigDecimal("498.00"), mpTarjeta, Venta.ESTADO_COMPLETADA);
                v4.setFechaVenta(now.minusDays(1));
                v4.agregarDetalle(new DetalleVenta(p1, 2, new BigDecimal("249.00"), new BigDecimal("498.00")));
                session.persist(v4);
            }

                tx.commit();
            } catch (Exception e) {
                if (tx != null && tx.isActive()) {
                    try {
                        tx.rollback();
                    } catch (Exception ignored) {
                    }
                }
                System.err.println("[TPV_flux] Error al inicializar datos semilla: " + e.getMessage());
            }
        }
    }

    private static Producto findProductoByNombre(Session session, String nombre) {
        return session.createQuery(
                        "FROM Producto p WHERE LOWER(p.nombre) = LOWER(:nombre)",
                        Producto.class
                )
                .setParameter("nombre", nombre)
                .setMaxResults(1)
                .uniqueResult();
    }

    private static MetodoPago ensureMetodoPago(Session session, String nombre) {
        MetodoPago existing = session.createQuery(
                        "FROM MetodoPago m WHERE UPPER(m.nombre) = UPPER(:nombre)",
                        MetodoPago.class
                )
                .setParameter("nombre", nombre)
                .uniqueResult();
        if (existing != null) {
            return existing;
        }
        MetodoPago nuevo = new MetodoPago(nombre, true);
        session.persist(nuevo);
        return nuevo;
    }

    private static Categoria getOrCreateCategoria(Session session, String nombre) {
        Categoria existing = session.createQuery(
                        "FROM Categoria c WHERE LOWER(c.nombre) = LOWER(:nombre)",
                        Categoria.class
                )
                .setParameter("nombre", nombre)
                .uniqueResult();
        if (existing != null) {
            return existing;
        }
        Categoria nueva = new Categoria(nombre, true);
        session.persist(nueva);
        return nueva;
    }

    private static void ensureUsuario(
            Session session,
            String username,
            String password,
            String nombreCompleto,
            String puesto,
            String turno,
            String sucursal
    ) {
        Usuario existing = session.createQuery(
                        "FROM Usuario u WHERE LOWER(u.username) = LOWER(:username)",
                        Usuario.class
                )
                .setParameter("username", username)
                .uniqueResult();
        if (existing == null) {
            session.persist(new Usuario(username, password, nombreCompleto, puesto, turno, sucursal, true));
        }
    }
}
