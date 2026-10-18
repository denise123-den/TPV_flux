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

    opens com.example.tpv_flux to javafx.fxml;
    opens com.example.tpv_flux.controller.layout to javafx.fxml;
    opens com.example.tpv_flux.controller.ventas to javafx.fxml;
    opens com.example.tpv_flux.controller.productos to javafx.fxml;
    opens com.example.tpv_flux.model to javafx.base, javafx.fxml;

    exports com.example.tpv_flux;
    exports com.example.tpv_flux.controller.layout;
    exports com.example.tpv_flux.controller.ventas;
    exports com.example.tpv_flux.controller.productos;
    exports com.example.tpv_flux.model;
    exports com.example.tpv_flux.util;
}