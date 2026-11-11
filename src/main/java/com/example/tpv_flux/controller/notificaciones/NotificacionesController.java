package com.example.tpv_flux.controller.notificaciones;

import com.example.tpv_flux.config.DatabaseSeeder;
import com.example.tpv_flux.config.HibernateUtil;
import com.example.tpv_flux.controller.layout.MainLayoutController;
import com.example.tpv_flux.dao.NotificacionDao;
import com.example.tpv_flux.model.Notificacion;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Controlador para la vista de Notificaciones.
 */
public class NotificacionesController {

    private static final String PILL_ACTIVE_CLASS = "category-pill-active";

    @FXML
    private Label lblSubtituloSinLeer;
    @FXML
    private Button btnMarcarTodas;
    @FXML
    private Button pillTodas;
    @FXML
    private Button pillNoLeidas;
    @FXML
    private VBox notificationsListContainer;

    private final NotificacionDao notificacionDao = new NotificacionDao();
    private final List<Notificacion> notifications = new ArrayList<>();
    private boolean onlyUnreadFilter = false;

    @FXML
    public void initialize() {
        loadNotifications();
        renderNotifications();
    }

    private void loadNotifications() {
        notifications.clear();
        try {
            if (HibernateUtil.isConnected()) {
                DatabaseSeeder.seedIfEmpty();
                notifications.addAll(notificacionDao.findAll());
            }
        } catch (Exception e) {
            System.err.println("[NotificacionesController] Error consultando notificaciones en BD: " + e.getMessage());
        }

        if (notifications.isEmpty()) {
            loadMockupNotifications();
        }
    }

    private void loadMockupNotifications() {
        LocalDateTime now = LocalDateTime.now();
        boolean allReadFallback = MainLayoutController.getFallbackUnreadCount() == 0;

        Notificacion n1 = new Notificacion("Stock bajo", "Audífonos Bluetooth X2 tiene solo 6 unidades");
        n1.setLeida(allReadFallback);
        n1.setFechaCreacion(now.minusMinutes(12));

        Notificacion n2 = new Notificacion("Caja", "Recuerda cerrar la caja del turno matutino");
        n2.setLeida(allReadFallback);
        n2.setFechaCreacion(now.minusMinutes(38));

        Notificacion n3 = new Notificacion("Devolución", "Se registró una devolución en el ticket #0481");
        n3.setLeida(allReadFallback);
        n3.setFechaCreacion(now.minusHours(1));

        Notificacion n4 = new Notificacion("Sistema", "Actualización disponible: TPV Core v1.1");
        n4.setLeida(allReadFallback);
        n4.setFechaCreacion(now.minusHours(3));

        Notificacion n5 = new Notificacion("Venta", "Venta completada por $1,040.52 — Ticket #0482");
        n5.setLeida(true);
        n5.setFechaCreacion(now.minusHours(5));

        Notificacion n6 = new Notificacion("Stock", "Mica templada iPhone 14 se quedó sin stock");
        n6.setLeida(true);
        n6.setFechaCreacion(now.minusDays(1));

        Notificacion n7 = new Notificacion("Inventario", "Se registró entrada de 50 unidades: Funda iPhone 14");
        n7.setLeida(true);
        n7.setFechaCreacion(now.minusDays(1).minusHours(2));

        notifications.addAll(List.of(n1, n2, n3, n4, n5, n6, n7));
    }

    @FXML
    private void onFilterTodas() {
        onlyUnreadFilter = false;
        if (!pillTodas.getStyleClass().contains(PILL_ACTIVE_CLASS)) {
            pillTodas.getStyleClass().add(PILL_ACTIVE_CLASS);
        }
        pillNoLeidas.getStyleClass().remove(PILL_ACTIVE_CLASS);
        renderNotifications();
    }

    @FXML
    private void onFilterNoLeidas() {
        onlyUnreadFilter = true;
        if (!pillNoLeidas.getStyleClass().contains(PILL_ACTIVE_CLASS)) {
            pillNoLeidas.getStyleClass().add(PILL_ACTIVE_CLASS);
        }
        pillTodas.getStyleClass().remove(PILL_ACTIVE_CLASS);
        renderNotifications();
    }

    @FXML
    private void onMarkAllAsRead() {
        try {
            if (HibernateUtil.isConnected()) {
                notificacionDao.markAllAsRead();
            }
            for (Notificacion n : notifications) {
                n.setLeida(true);
            }
            MainLayoutController.setFallbackUnreadCount(0);
            MainLayoutController.refreshNotificationBadge();
            renderNotifications();
        } catch (Exception e) {
            System.err.println("[NotificacionesController] Error al marcar notificaciones como leídas: " + e.getMessage());
        }
    }

    private void onMarkSingleAsRead(Notificacion notif) {
        if (Boolean.TRUE.equals(notif.getLeida())) {
            return;
        }
        try {
            if (HibernateUtil.isConnected() && notif.getId() != null) {
                notificacionDao.markAsRead(notif.getId());
            }
            notif.setLeida(true);
            long unreadCount = notifications.stream().filter(n -> !Boolean.TRUE.equals(n.getLeida())).count();
            MainLayoutController.setFallbackUnreadCount((int) unreadCount);
            MainLayoutController.refreshNotificationBadge();
            renderNotifications();
        } catch (Exception e) {
            System.err.println("[NotificacionesController] Error al marcar notificación individual: " + e.getMessage());
        }
    }

    private void renderNotifications() {
        notificationsListContainer.getChildren().clear();

        long unreadCount = notifications.stream().filter(n -> !Boolean.TRUE.equals(n.getLeida())).count();
        lblSubtituloSinLeer.setText(unreadCount + " sin leer");
        pillTodas.setText("Todas · " + notifications.size());
        pillNoLeidas.setText("No leídas · " + unreadCount);

        List<Notificacion> filtered = notifications.stream()
                .filter(n -> !onlyUnreadFilter || !Boolean.TRUE.equals(n.getLeida()))
                .toList();

        for (Notificacion notif : filtered) {
            notificationsListContainer.getChildren().add(createNotificationCard(notif));
        }
    }

    private HBox createNotificationCard(Notificacion notif) {
        boolean unread = !Boolean.TRUE.equals(notif.getLeida());

        HBox card = new HBox(14.0);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add(unread ? "notification-card-unread" : "notification-card");

        StackPane iconBox = new StackPane();
        iconBox.getStyleClass().add("notif-icon-box");

        String titleLower = notif.getTitulo() != null ? notif.getTitulo().toLowerCase(Locale.ROOT) : "";
        String iconLiteral;
        if (titleLower.contains("stock")) {
            iconBox.getStyleClass().add("notif-icon-warning");
            iconLiteral = "fth-alert-triangle";
        } else if (titleLower.contains("devoluci")) {
            iconBox.getStyleClass().add("notif-icon-danger");
            iconLiteral = "fth-rotate-ccw";
        } else if (titleLower.contains("caja")) {
            iconBox.getStyleClass().add("notif-icon-teal");
            iconLiteral = "fth-credit-card";
        } else if (titleLower.contains("venta")) {
            iconBox.getStyleClass().add("notif-icon-teal");
            iconLiteral = "fth-shopping-cart";
        } else if (titleLower.contains("inventario")) {
            iconBox.getStyleClass().add("notif-icon-info");
            iconLiteral = "fth-archive";
        } else {
            iconBox.getStyleClass().add("notif-icon-neutral");
            iconLiteral = "fth-info";
        }

        FontIcon icon = new FontIcon(iconLiteral);
        iconBox.getChildren().add(icon);

        VBox textContainer = new VBox(2.0);
        HBox.setHgrow(textContainer, Priority.ALWAYS);

        Label titleLbl = new Label(notif.getTitulo());
        titleLbl.getStyleClass().add("cell-product-name");

        Label msgLbl = new Label(notif.getMensaje());
        msgLbl.getStyleClass().add("cell-muted");

        textContainer.getChildren().addAll(titleLbl, msgLbl);

        HBox rightMeta = new HBox(8.0);
        rightMeta.setAlignment(Pos.TOP_RIGHT);

        Label timeLbl = new Label(formatRelativeTime(notif.getFechaCreacion()));
        timeLbl.getStyleClass().add("ticket-subtitle");
        rightMeta.getChildren().add(timeLbl);

        if (unread) {
            Region dot = new Region();
            dot.getStyleClass().add("unread-dot");
            rightMeta.getChildren().add(dot);
        }

        card.getChildren().addAll(iconBox, textContainer, rightMeta);
        card.setOnMouseClicked(e -> onMarkSingleAsRead(notif));
        return card;
    }

    private String formatRelativeTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "hace un momento";
        }
        Duration diff = Duration.between(dateTime, LocalDateTime.now());
        long minutes = Math.max(1, diff.toMinutes());
        if (minutes < 60) {
            return "hace " + minutes + " min";
        }
        long hours = diff.toHours();
        if (hours < 24) {
            return "hace " + hours + " h";
        }
        long days = diff.toDays();
        if (days == 1) {
            return "ayer";
        }
        return "hace " + days + " días";
    }
}
