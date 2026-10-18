package com.example.tpv_flux.controller.layout;

import com.example.tpv_flux.HelloApplication;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.util.List;

/**
 * Controlador de la base del layout (Sidebar + contenedor dinámico).
 * Permite cambiar entre las plantillas de Ventas y Productos.
 */
public class MainLayoutController {

    private static final String ACTIVE_CLASS = "nav-button-active";

    @FXML
    private StackPane contentArea;

    @FXML
    private Button btnVentas;
    @FXML
    private Button btnProductos;
    @FXML
    private Button btnCategorias;
    @FXML
    private Button btnInventario;
    @FXML
    private Button btnClientes;
    @FXML
    private Button btnCaja;
    @FXML
    private Button btnReportes;
    @FXML
    private Button btnNotificaciones;
    @FXML
    private Button btnConfiguracion;

    private List<Button> navButtons;

    @FXML
    public void initialize() {
        navButtons = List.of(
                btnVentas,
                btnProductos,
                btnCategorias,
                btnInventario,
                btnClientes,
                btnCaja,
                btnReportes,
                btnNotificaciones,
                btnConfiguracion
        );

        // Cargar la plantilla de Ventas por defecto al iniciar
        loadView("/com/example/tpv_flux/views/ventas/VentasView.fxml");
        setActiveButton(btnVentas);
    }

    @FXML
    private void onShowVentas() {
        loadView("/com/example/tpv_flux/views/ventas/VentasView.fxml");
        setActiveButton(btnVentas);
    }

    @FXML
    private void onShowProductos() {
        loadView("/com/example/tpv_flux/views/productos/ProductosView.fxml");
        setActiveButton(btnProductos);
    }

    @FXML
    private void onShowPlaceholder(ActionEvent event) {
        if (event.getSource() instanceof Button clickedButton) {
            setActiveButton(clickedButton);
            String sectionName = clickedButton.getText();
            if (sectionName == null || sectionName.isBlank()) {
                sectionName = "Notificaciones";
            }
            showPlaceholderModule(sectionName);
        }
    }

    private void loadView(String fxmlPath) {
        try {
            URL resource = HelloApplication.class.getResource(fxmlPath);
            if (resource == null) {
                throw new IllegalStateException("No se encontró el archivo FXML: " + fxmlPath);
            }
            FXMLLoader loader = new FXMLLoader(resource);
            Node view = loader.load();
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            throw new RuntimeException("Error al cargar la vista: " + fxmlPath, e);
        }
    }

    private void showPlaceholderModule(String moduleName) {
        VBox box = new VBox(8);
        box.setAlignment(Pos.CENTER);

        Label title = new Label(moduleName);
        title.getStyleClass().add("page-title");

        Label subtitle = new Label("Módulo preparado en la estructura de carpetas para la siguiente iteración.");
        subtitle.getStyleClass().add("page-subtitle");

        box.getChildren().addAll(title, subtitle);
        contentArea.getChildren().setAll(box);
    }

    private void setActiveButton(Button activeBtn) {
        for (Button btn : navButtons) {
            btn.getStyleClass().remove(ACTIVE_CLASS);
        }
        if (!activeBtn.getStyleClass().contains(ACTIVE_CLASS)) {
            activeBtn.getStyleClass().add(ACTIVE_CLASS);
        }
    }
}
