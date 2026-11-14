module com.example.tpv_flux {
    requires javafx.controls;
    requires javafx.fxml;

    requires atlantafx.base;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.feather;

    requires org.hibernate.orm.core;
    requires jakarta.persistence;
    requires java.naming;
    requires java.sql;
    requires java.desktop;
    requires flyway.core;

    opens com.example.tpv_flux to javafx.fxml;
    opens com.example.tpv_flux.controller.auth to javafx.fxml;
    opens com.example.tpv_flux.controller.layout to javafx.fxml;
    opens com.example.tpv_flux.controller.ventas to javafx.fxml;
    opens com.example.tpv_flux.controller.productos to javafx.fxml;
    opens com.example.tpv_flux.controller.inventario to javafx.fxml;
    opens com.example.tpv_flux.controller.reportes to javafx.fxml;
    opens com.example.tpv_flux.controller.notificaciones to javafx.fxml;
    opens com.example.tpv_flux.model to org.hibernate.orm.core, javafx.base, javafx.fxml;
    opens db.migration;

    exports com.example.tpv_flux;
    exports com.example.tpv_flux.config;
    exports com.example.tpv_flux.dao;
    exports com.example.tpv_flux.controller.auth;
    exports com.example.tpv_flux.controller.layout;
    exports com.example.tpv_flux.controller.ventas;
    exports com.example.tpv_flux.controller.productos;
    exports com.example.tpv_flux.controller.inventario;
    exports com.example.tpv_flux.controller.reportes;
    exports com.example.tpv_flux.controller.notificaciones;
    exports com.example.tpv_flux.model;
    exports com.example.tpv_flux.util;
}