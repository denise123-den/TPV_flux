package com.example.tpv_flux.controller.layout;

import com.example.tpv_flux.HelloApplication;
import com.example.tpv_flux.config.DatabaseSeeder;
import com.example.tpv_flux.config.HibernateUtil;
import com.example.tpv_flux.config.SessionManager;
import com.example.tpv_flux.dao.NotificacionDao;
import com.example.tpv_flux.model.Usuario;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.util.List;

/**
 * Controlador de la base del layout (Sidebar + contenedor dinámico).
 * Permite cambiar entre Ventas, Productos, Inventario, Reportes y Notificaciones.
 */
public class MainLayoutController {

    private static final String ACTIVE_CLASS = "nav-button-active";
    private static MainLayoutController instance;
    private static int fallbackUnreadCount = 4;

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
    @FXML
    private Label lblNotificationBadge;
    @FXML
    private Label lblUserAvatar;
    @FXML
    private Label lblUserName;
    @FXML
    private Label lblUserRole;

    private final NotificacionDao notificacionDao = new NotificacionDao();
    private List<Button> navButtons;

    @FXML
    public void initialize() {
        instance = this;
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

        updateActiveUserSidebar();
        loadView("/com/example/tpv_flux/views/ventas/VentasView.fxml");
        setActiveButton(btnVentas);
        updateNotificationBadge();
    }

    public void updateActiveUserSidebar() {
        Usuario u = SessionManager.getUsuarioActivo();
        if (u != null) {
            if (lblUserAvatar != null) {
                lblUserAvatar.setText(u.getIniciales());
            }
            if (lblUserName != null) {
                lblUserName.setText(u.getNombreCompleto());
            }
            if (lblUserRole != null) {
                lblUserRole.setText(u.getPuestoYTurno());
            }
        }
    }

    @FXML
    private void onCerrarSesion() {
        try {
            SessionManager.cerrarSesion();
            FXMLLoader loader = new FXMLLoader(
                    HelloApplication.class.getResource("/com/example/tpv_flux/views/auth/LoginView.fxml")
            );
            Parent loginRoot = loader.load();
            Scene scene = contentArea.getScene();
            if (scene != null) {
                scene.setRoot(loginRoot);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error al regresar a la pantalla de inicio de sesión", e);
        }
    }

    public static void refreshNotificationBadge() {
        if (instance != null) {
            instance.updateNotificationBadge();
        }
    }

    public static void setFallbackUnreadCount(int count) {
        fallbackUnreadCount = Math.max(0, count);
        refreshNotificationBadge();
    }

    public static int getFallbackUnreadCount() {
        return fallbackUnreadCount;
    }

    public void updateNotificationBadge() {
        if (lblNotificationBadge == null) {
            return;
        }
        long unread = fallbackUnreadCount;
        try {
            if (HibernateUtil.isConnected()) {
                DatabaseSeeder.seedIfEmpty();
                unread = notificacionDao.countUnread();
            }
        } catch (Exception ignored) {
        }
        lblNotificationBadge.setText(String.valueOf(unread));
        lblNotificationBadge.setVisible(unread > 0);
        lblNotificationBadge.setManaged(unread > 0);
    }

    @FXML
    private void onShowVentas() {
        loadView("/com/example/tpv_flux/views/ventas/VentasView.fxml");
        setActiveButton(btnVentas);
        updateNotificationBadge();
    }

    @FXML
    private void onShowProductos() {
        loadView("/com/example/tpv_flux/views/productos/ProductosView.fxml");
        setActiveButton(btnProductos);
        updateNotificationBadge();
    }

    @FXML
    private void onShowInventario() {
        loadView("/com/example/tpv_flux/views/inventario/InventarioView.fxml");
        setActiveButton(btnInventario);
        updateNotificationBadge();
    }

    @FXML
    private void onShowReportes() {
        loadView("/com/example/tpv_flux/views/reportes/ReportesView.fxml");
        setActiveButton(btnReportes);
        updateNotificationBadge();
    }

    @FXML
    private void onShowNotificaciones() {
        loadView("/com/example/tpv_flux/views/notificaciones/NotificacionesView.fxml");
        setActiveButton(btnNotificaciones);
        updateNotificationBadge();
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

        box.getChildren().add(title);
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
