package com.example.tpv_flux;

import atlantafx.base.theme.PrimerLight;
import com.example.tpv_flux.model.Categoria;
import com.example.tpv_flux.model.ProductItem;
import com.example.tpv_flux.model.Producto;
import com.example.tpv_flux.util.ImageUtil;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class FxmlLoadingTest {

    @BeforeAll
    static void initJfx() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException e) {
            // Toolkit already initialized
            latch.countDown();
        }
        latch.await(5, TimeUnit.SECONDS);
    }

    @Test
    void shouldLoadMainLayoutVentasAndProductosViews() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                javafx.application.Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

                Parent loginRoot = FXMLLoader.load(
                        HelloApplication.class.getResource("/com/example/tpv_flux/views/auth/LoginView.fxml")
                );
                assertNotNull(loginRoot);

                Parent mainRoot = FXMLLoader.load(
                        HelloApplication.class.getResource("/com/example/tpv_flux/views/layout/MainLayout.fxml")
                );
                assertNotNull(mainRoot);

                Parent ventasRoot = FXMLLoader.load(
                        HelloApplication.class.getResource("/com/example/tpv_flux/views/ventas/VentasView.fxml")
                );
                assertNotNull(ventasRoot);

                Parent productosRoot = FXMLLoader.load(
                        HelloApplication.class.getResource("/com/example/tpv_flux/views/productos/ProductosView.fxml")
                );
                assertNotNull(productosRoot);

                Parent inventarioRoot = FXMLLoader.load(
                        HelloApplication.class.getResource("/com/example/tpv_flux/views/inventario/InventarioView.fxml")
                );
                assertNotNull(inventarioRoot);

                Parent reportesRoot = FXMLLoader.load(
                        HelloApplication.class.getResource("/com/example/tpv_flux/views/reportes/ReportesView.fxml")
                );
                assertNotNull(reportesRoot);

                Parent notificacionesRoot = FXMLLoader.load(
                        HelloApplication.class.getResource("/com/example/tpv_flux/views/notificaciones/NotificacionesView.fxml")
                );
                assertNotNull(notificacionesRoot);
            } catch (Throwable t) {
                error.set(t);
            } finally {
                latch.countDown();
            }
        });

        latch.await(10, TimeUnit.SECONDS);
        assertNull(error.get(), () -> "Error loading FXML: " + error.get());
    }

    @Test
    void shouldMapProductoEntityToProductItem() {
        Categoria cat = new Categoria("Celulares", true);
        Producto prod = new Producto(
                "Funda iPhone 14",
                "SKU: FN-IP14-001 | TAG: FUNDA | Funda protectora",
                cat,
                new BigDecimal("120.00"),
                new BigDecimal("249.00"),
                "funda.png",
                18,
                true
        );
        prod.setId(1);

        ProductItem item = prod.toProductItem();
        assertEquals(1, item.getId());
        assertEquals("Funda iPhone 14", item.getName());
        assertEquals("FN-IP14-001", item.getSku());
        assertEquals("Celulares", item.getCategory());
        assertEquals("FUNDA", item.getShortTag());
        assertEquals(18, item.getStock());
        assertEquals(249.00, item.getPrice());
        assertEquals("Disponible", item.getStatus());
        assertNotNull(ImageUtil.loadFromReference("iphon.jpeg"));
    }

    @Test
    void shouldConnectAndQueryDatabaseWhenAvailable() {
        var usuarioDao = new com.example.tpv_flux.dao.UsuarioDao();

        var authAna = usuarioDao.autenticar("ana.martinez", "123456");
        org.junit.jupiter.api.Assertions.assertTrue(authAna.isPresent(), "Debe autenticar a ana.martinez con 123456");
        assertEquals("Ana Martínez", authAna.get().getNombreCompleto());
        assertEquals("AM", authAna.get().getIniciales());
        assertEquals("Cajera · Turno AM", authAna.get().getPuestoYTurno());

        var authPorNombre = usuarioDao.autenticar("Ana martines", "123456");
        org.junit.jupiter.api.Assertions.assertTrue(authPorNombre.isPresent(), "Debe autenticar también escribiendo Ana martines");

        if (com.example.tpv_flux.config.HibernateUtil.isConnected()) {
            com.example.tpv_flux.config.DatabaseSeeder.seedIfEmpty();
            var productoDao = new com.example.tpv_flux.dao.ProductoDao();
            int borradosMilk = productoDao.deleteByNombre("milk");
            System.out.println("[TPV_flux] Productos 'milk' eliminados de BD: " + borradosMilk);

            var productos = productoDao.findAllActive();
            var movimientos = new com.example.tpv_flux.dao.InventarioDao().findRecent(10);
            var ventas = new com.example.tpv_flux.dao.VentaDao().findRecent(10);
            var notificaciones = new com.example.tpv_flux.dao.NotificacionDao().findAll();
            var usuarios = usuarioDao.findAllActive();

            org.junit.jupiter.api.Assertions.assertFalse(productos.isEmpty(), "Productos debe tener datos semilla");
            org.junit.jupiter.api.Assertions.assertFalse(movimientos.isEmpty(), "Inventario debe tener movimientos semilla");
            org.junit.jupiter.api.Assertions.assertFalse(ventas.isEmpty(), "Ventas debe tener tickets semilla");
            org.junit.jupiter.api.Assertions.assertFalse(notificaciones.isEmpty(), "Notificaciones debe tener alertas semilla");
            org.junit.jupiter.api.Assertions.assertFalse(usuarios.isEmpty(), "Usuarios debe tener usuarios semilla");

            // Probar cobro de una venta con múltiples productos contra PostgreSQL real
            var disponibles = productos.stream().filter(p -> p.getStockActual() != null && p.getStockActual() >= 2).toList();
            if (disponibles.size() >= 2) {
                var pA = disponibles.get(0);
                var pB = disponibles.get(1);
                int stockOrigA = pA.getStockActual();
                int stockOrigB = pB.getStockActual();

                var ventaDao = new com.example.tpv_flux.dao.VentaDao();
                String folio = ventaDao.generarSiguienteFolio();
                var ticketItems = java.util.List.of(
                        new com.example.tpv_flux.model.TicketLineItem(pA.toProductItem(), 1),
                        new com.example.tpv_flux.model.TicketLineItem(pB.toProductItem(), 2)
                );

                var ventaCreada = ventaDao.registrarVenta(folio, ticketItems, com.example.tpv_flux.model.MetodoPago.EFECTIVO);
                assertNotNull(ventaCreada.getId(), "La venta con varios artículos debe guardarse con éxito en PostgreSQL");

                // Restaurar el stock original y limpiar la venta de prueba para no ensuciar los datos del usuario
                try (var session = com.example.tpv_flux.config.HibernateUtil.getSessionFactory().openSession()) {
                    var tx = session.beginTransaction();
                    var dbPA = session.get(Producto.class, pA.getId());
                    var dbPB = session.get(Producto.class, pB.getId());
                    dbPA.setStockActual(stockOrigA);
                    dbPB.setStockActual(stockOrigB);
                    session.createMutationQuery("DELETE FROM MovimientoInventario m WHERE m.producto.id IN (:idA, :idB) AND m.stockResultante IN (:sA, :sB)")
                            .setParameter("idA", pA.getId())
                            .setParameter("idB", pB.getId())
                            .setParameter("sA", stockOrigA - 1)
                            .setParameter("sB", stockOrigB - 2)
                            .executeUpdate();
                    session.createMutationQuery("DELETE FROM Notificacion n WHERE n.mensaje LIKE :folio")
                            .setParameter("folio", "%" + ventaCreada.getFolioTicket() + "%")
                            .executeUpdate();
                    var dbVenta = session.get(com.example.tpv_flux.model.Venta.class, ventaCreada.getId());
                    if (dbVenta != null) {
                        session.remove(dbVenta);
                    }
                    tx.commit();
                }
            }
        }
    }
}
