package com.example.tpv_flux.controller.inventario;

import com.example.tpv_flux.config.DatabaseSeeder;
import com.example.tpv_flux.config.HibernateUtil;
import com.example.tpv_flux.config.SessionManager;
import com.example.tpv_flux.controller.layout.MainLayoutController;
import com.example.tpv_flux.dao.InventarioDao;
import com.example.tpv_flux.dao.ProductoDao;
import com.example.tpv_flux.model.MovimientoInventario;
import com.example.tpv_flux.model.ProductItem;
import com.example.tpv_flux.model.Producto;
import com.example.tpv_flux.util.CurrencyUtil;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Controlador para el módulo de Inventario.
 */
public class InventarioController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM · HH:mm", new Locale("es", "MX"));

    @FXML
    private Button btnRegistrarMovimiento;
    @FXML
    private Label lblSucursalSubtitulo;
    @FXML
    private Label lblTotalProductos;
    @FXML
    private Label lblValorInventario;
    @FXML
    private Label lblStockBajo;
    @FXML
    private Label lblSinStock;
    @FXML
    private VBox movementsRowsContainer;

    private final ProductoDao productoDao = new ProductoDao();
    private final InventarioDao inventarioDao = new InventarioDao();

    private final List<ProductItem> catalogProducts = new ArrayList<>();
    private final List<MovementRowData> movementRows = new ArrayList<>();

    public record MovementRowData(
            String fecha,
            String producto,
            String tipo,
            int cantidadConSigno,
            String usuario,
            String motivo
    ) {
    }

    @FXML
    public void initialize() {
        if (lblSucursalSubtitulo != null) {
            lblSucursalSubtitulo.setText(SessionManager.getNombreSucursalCorto());
        }
        btnRegistrarMovimiento.setOnAction(e -> onOpenRegistrarMovimientoModal());
        loadInventoryData();
    }

    private void loadInventoryData() {
        catalogProducts.clear();
        movementRows.clear();

        try {
            if (HibernateUtil.isConnected()) {
                DatabaseSeeder.seedIfEmpty();
                List<Producto> dbProducts = productoDao.findAllActive();
                for (Producto p : dbProducts) {
                    catalogProducts.add(p.toProductItem());
                }

                String activeUserName = SessionManager.getUsuarioActivo() != null
                        ? SessionManager.getUsuarioActivo().getNombreCompleto()
                        : "Ana Martínez";
                List<MovimientoInventario> dbMovements = inventarioDao.findRecent(50);
                for (MovimientoInventario m : dbMovements) {
                    boolean isEntrada = MovimientoInventario.TIPO_ENTRADA.equalsIgnoreCase(m.getTipo());
                    String tipoLabel = isEntrada ? "Entrada" : "Salida";
                    int signedQty = isEntrada ? m.getCantidad() : -m.getCantidad();
                    String usuario = isEntrada ? activeUserName : "Sistema";
                    String motivo = isEntrada ? "Reabastecimiento" : "Venta / Salida (Stock: " + m.getStockResultante() + ")";
                    String fechaStr = m.getFecha() != null ? m.getFecha().format(DATE_FMT) : LocalDateTime.now().format(DATE_FMT);
                    movementRows.add(new MovementRowData(
                            fechaStr,
                            m.getProducto().getNombre(),
                            tipoLabel,
                            signedQty,
                            usuario,
                            motivo
                    ));
                }
            }
        } catch (Exception e) {
            System.err.println("[InventarioController] Error consultando BD: " + e.getMessage());
        }

        if (catalogProducts.isEmpty()) {
            loadMockupProductsAndMovements();
        }

        updateKpiCards();
        renderMovementRows();
    }

    private void loadMockupProductsAndMovements() {
        catalogProducts.add(new ProductItem(1, "Funda iPhone 14", "FN-IP14-001", "Celulares", "FUNDA", 18, 249.00, "iphon.jpeg"));
        catalogProducts.add(new ProductItem(2, "Cable USB-C 1m", "CB-USBC-100", "Cómputo", "CABLE", 42, 99.00, "USB.jpeg"));
        catalogProducts.add(new ProductItem(3, "Audífonos Bluetooth X2", "AU-BT-X2", "Audio", "AUDIO", 6, 450.00, "audiculares.jpeg"));
        catalogProducts.add(new ProductItem(4, "Cargador 20W", "CG-20W-002", "Cómputo", "CARGA", 25, 189.00, "Cargador.jpeg"));
        catalogProducts.add(new ProductItem(5, "Mica templada iPhone 14", "MC-IP14-01", "Celulares", "MICA", 0, 79.00, "Mica.jpeg"));
        catalogProducts.add(new ProductItem(6, "Power bank 10000mAh", "PB-10K-003", "Cómputo", "POWER", 11, 399.00, "Power.jpeg"));
        catalogProducts.add(new ProductItem(7, "Memoria USB 32GB", "MU-32GB-01", "Cómputo", "USB", 30, 129.00, "Memoria.jpeg"));
        catalogProducts.add(new ProductItem(8, "Soporte celular", "SP-CEL-004", "Celulares", "SOPORTE", 14, 149.00, "Soporte.jpeg"));

        movementRows.add(new MovementRowData("30 sep · 09:10", "Funda iPhone 14", "Entrada", 50, "Ana Martínez", "Reabastecimiento"));
        movementRows.add(new MovementRowData("30 sep · 09:45", "Cable USB-C 1m", "Salida", -2, "Sistema", "Venta #0479"));
        movementRows.add(new MovementRowData("29 sep · 18:20", "Mica templada iPhone 14", "Ajuste", -3, "Carlos Ruiz", "Merma / rotura"));
        movementRows.add(new MovementRowData("29 sep · 16:05", "Audífonos Bluetooth X2", "Entrada", 20, "Ana Martínez", "Reabastecimiento"));
        movementRows.add(new MovementRowData("29 sep · 11:30", "Power bank 10000mAh", "Salida", -1, "Sistema", "Venta #0455"));
        movementRows.add(new MovementRowData("28 sep · 10:00", "Memoria USB 32GB", "Ajuste", 5, "Carlos Ruiz", "Corrección de conteo"));
    }

    private void updateKpiCards() {
        int totalUnits = catalogProducts.stream().mapToInt(ProductItem::getStock).sum();
        double inventoryValue = catalogProducts.stream()
                .mapToDouble(p -> p.getStock() * p.getPrice())
                .sum();
        long lowStock = catalogProducts.stream().filter(p -> p.getStock() > 0 && p.getStock() <= 6).count();
        long noStock = catalogProducts.stream().filter(p -> p.getStock() <= 0).count();

        lblTotalProductos.setText(String.valueOf(totalUnits));
        lblValorInventario.setText(CurrencyUtil.format(inventoryValue));
        lblStockBajo.setText(lowStock + (lowStock == 1 ? " producto" : " productos"));
        lblSinStock.setText(noStock + (noStock == 1 ? " producto" : " productos"));
    }

    private void renderMovementRows() {
        movementsRowsContainer.getChildren().clear();
        for (MovementRowData item : movementRows) {
            movementsRowsContainer.getChildren().add(createMovementRow(item));
        }
    }

    private GridPane createMovementRow(MovementRowData item) {
        GridPane row = new GridPane();
        row.getStyleClass().add("table-data-row");

        ColumnConstraints col0 = new ColumnConstraints();
        col0.setPercentWidth(16.0);
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(28.0);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(14.0);
        ColumnConstraints col3 = new ColumnConstraints();
        col3.setPercentWidth(12.0);
        col3.setHalignment(HPos.RIGHT);
        ColumnConstraints col4 = new ColumnConstraints();
        col4.setPercentWidth(15.0);
        ColumnConstraints col5 = new ColumnConstraints();
        col5.setPercentWidth(15.0);

        row.getColumnConstraints().addAll(col0, col1, col2, col3, col4, col5);

        Label dateLbl = new Label(item.fecha());
        dateLbl.getStyleClass().add("cell-muted");

        Label prodLbl = new Label(item.producto());
        prodLbl.getStyleClass().add("cell-product-name");

        Label typeBadge = new Label(item.tipo());
        if ("Entrada".equalsIgnoreCase(item.tipo())) {
            typeBadge.getStyleClass().add("mov-badge-entrada");
        } else if ("Salida".equalsIgnoreCase(item.tipo())) {
            typeBadge.getStyleClass().add("mov-badge-salida");
        } else {
            typeBadge.getStyleClass().add("mov-badge-ajuste");
        }

        String qtyText = item.cantidadConSigno() > 0 ? "+" + item.cantidadConSigno() : String.valueOf(item.cantidadConSigno());
        Label qtyLbl = new Label(qtyText);
        if ("Entrada".equalsIgnoreCase(item.tipo()) || item.cantidadConSigno() > 0) {
            qtyLbl.getStyleClass().add("qty-positive");
        } else if ("Ajuste".equalsIgnoreCase(item.tipo())) {
            qtyLbl.getStyleClass().add("qty-warning");
        } else {
            qtyLbl.getStyleClass().add("qty-negative");
        }
        GridPane.setMargin(qtyLbl, new Insets(0, 24.0, 0, 0));

        Label userLbl = new Label(item.usuario());
        userLbl.getStyleClass().add("cell-muted");

        Label reasonLbl = new Label(item.motivo());
        reasonLbl.getStyleClass().add("cell-muted");

        row.add(dateLbl, 0, 0);
        row.add(prodLbl, 1, 0);
        row.add(typeBadge, 2, 0);
        row.add(qtyLbl, 3, 0);
        row.add(userLbl, 4, 0);
        row.add(reasonLbl, 5, 0);

        return row;
    }

    private void onOpenRegistrarMovimientoModal() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        if (movementsRowsContainer.getScene() != null && movementsRowsContainer.getScene().getWindow() != null) {
            dialog.initOwner(movementsRowsContainer.getScene().getWindow());
        }
        dialog.setTitle("Registrar movimiento de inventario — TPV Core");

        VBox root = new VBox(16.0);
        root.setPadding(new Insets(24, 24, 30, 24));
        root.setStyle("-fx-background-color: #FFFFFF;");

        VBox headerBox = new VBox(2.0);
        Label titleLbl = new Label("Registrar movimiento");
        titleLbl.getStyleClass().add("page-title");
        Label subLbl = new Label("Registra una entrada o salida de mercancía en inventario");
        subLbl.getStyleClass().add("page-subtitle");
        headerBox.getChildren().addAll(titleLbl, subLbl);

        VBox prodBox = new VBox(4.0);
        Label lblProd = new Label("Producto *");
        lblProd.getStyleClass().add("form-label");
        List<String> productNames = catalogProducts.stream().map(ProductItem::getName).toList();
        ComboBox<String> cmbProducto = new ComboBox<>(FXCollections.observableArrayList(productNames));
        cmbProducto.setMaxWidth(Double.MAX_VALUE);
        cmbProducto.getStyleClass().add("filter-combo");
        if (!productNames.isEmpty()) {
            cmbProducto.getSelectionModel().selectFirst();
        }
        prodBox.getChildren().addAll(lblProd, cmbProducto);

        HBox rowTypeQty = new HBox(12.0);
        VBox typeBox = new VBox(4.0);
        HBox.setHgrow(typeBox, Priority.ALWAYS);
        Label lblType = new Label("Tipo de movimiento *");
        lblType.getStyleClass().add("form-label");
        ComboBox<String> cmbTipo = new ComboBox<>(FXCollections.observableArrayList("ENTRADA", "SALIDA"));
        cmbTipo.setMaxWidth(Double.MAX_VALUE);
        cmbTipo.getStyleClass().add("filter-combo");
        cmbTipo.getSelectionModel().selectFirst();
        typeBox.getChildren().addAll(lblType, cmbTipo);

        VBox qtyBox = new VBox(4.0);
        HBox.setHgrow(qtyBox, Priority.ALWAYS);
        Label lblQty = new Label("Cantidad *");
        lblQty.getStyleClass().add("form-label");
        TextField txtCantidad = new TextField("10");
        txtCantidad.getStyleClass().add("form-input");
        qtyBox.getChildren().addAll(lblQty, txtCantidad);
        rowTypeQty.getChildren().addAll(typeBox, qtyBox);

        VBox reasonBox = new VBox(4.0);
        Label lblReason = new Label("Motivo");
        lblReason.getStyleClass().add("form-label");
        TextField txtMotivo = new TextField("Reabastecimiento");
        txtMotivo.getStyleClass().add("form-input");
        reasonBox.getChildren().addAll(lblReason, txtMotivo);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        HBox footerButtons = new HBox(10.0);
        footerButtons.setAlignment(Pos.CENTER_RIGHT);
        Button btnCancelar = new Button("Cancelar");
        btnCancelar.getStyleClass().add("secondary-action-btn");
        btnCancelar.setOnAction(ev -> dialog.close());

        Button btnGuardar = new Button("Guardar movimiento");
        btnGuardar.getStyleClass().add("primary-action-btn");
        btnGuardar.setOnAction(ev -> {
            String prodName = cmbProducto.getValue();
            String tipo = cmbTipo.getValue();
            int cantidad;
            try {
                cantidad = Integer.parseInt(txtCantidad.getText().trim());
                if (cantidad <= 0) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Cantidad inválida");
                alert.setHeaderText("Ingresa una cantidad entera mayor a 0");
                alert.showAndWait();
                return;
            }

            ProductItem selectedProduct = catalogProducts.stream()
                    .filter(p -> p.getName().equalsIgnoreCase(prodName))
                    .findFirst()
                    .orElse(null);

            if (selectedProduct == null) {
                return;
            }

            try {
                if (HibernateUtil.isConnected() && selectedProduct.getId() != null) {
                    inventarioDao.registrarMovimiento(selectedProduct.getId(), tipo, cantidad);
                    loadInventoryData();
                } else {
                    boolean isEntrada = "ENTRADA".equalsIgnoreCase(tipo);
                    if (!isEntrada && selectedProduct.getStock() < cantidad) {
                        throw new IllegalStateException("Stock insuficiente para realizar la salida.");
                    }
                    int nuevoStock = isEntrada ? selectedProduct.getStock() + cantidad : selectedProduct.getStock() - cantidad;
                    selectedProduct.setStock(nuevoStock);

                    String motivo = txtMotivo.getText() == null || txtMotivo.getText().isBlank()
                            ? (isEntrada ? "Reabastecimiento" : "Salida manual")
                            : txtMotivo.getText().trim();

                    movementRows.add(0, new MovementRowData(
                            LocalDateTime.now().format(DATE_FMT),
                            selectedProduct.getName(),
                            isEntrada ? "Entrada" : "Salida",
                            isEntrada ? cantidad : -cantidad,
                            "Ana Martínez",
                            motivo
                    ));
                    updateKpiCards();
                    renderMovementRows();
                }
                MainLayoutController.refreshNotificationBadge();
                dialog.close();
            } catch (Exception ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error en inventario");
                alert.setHeaderText("No se pudo registrar el movimiento");
                alert.setContentText(ex.getMessage());
                alert.showAndWait();
            }
        });

        footerButtons.getChildren().addAll(btnCancelar, btnGuardar);
        root.getChildren().addAll(headerBox, prodBox, rowTypeQty, reasonBox, spacer, footerButtons);

        Scene scene = new Scene(root, 440, 420);
        var cssRes = InventarioController.class.getResource("/com/example/tpv_flux/css/app.css");
        if (cssRes != null) {
            scene.getStylesheets().add(cssRes.toExternalForm());
        }
        dialog.setScene(scene);
        dialog.showAndWait();
    }
}
