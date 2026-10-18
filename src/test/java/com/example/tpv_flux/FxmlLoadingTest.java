package com.example.tpv_flux;

import atlantafx.base.theme.PrimerLight;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

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
            } catch (Throwable t) {
                error.set(t);
            } finally {
                latch.countDown();
            }
        });

        latch.await(10, TimeUnit.SECONDS);
        assertNull(error.get(), () -> "Error loading FXML: " + error.get());
    }
}
