package com.example.tpv_flux.controller.productos;

import com.example.tpv_flux.model.ProductItem;
import com.example.tpv_flux.util.CurrencyUtil;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Controlador para la Plantilla 2: Productos (Catálogo y Tabla de Inventario).
 */
public class ProductosController {

    @FXML
    private Label lblResumenProductos;
    @FXML
    private Button btnNuevoProducto;

    @FXML
    private TextField txtBuscarProducto;
    @FXML
    private ComboBox<String> cmbCategorias;
    @FXML
    private ComboBox<String> cmbEstados;

    @FXML
    private VBox tableRowsContainer;

    private final List<ProductItem> products = new ArrayList<>();

    @FXML
    public void initialize() {
        loadMockupProducts();

        cmbCategorias.setItems(FXCollections.observableArrayList(
                "Todas las categorías",
                "Celulares",
                "Cómputo",
                "Audio"
        ));
        cmbCategorias.getSelectionModel().selectFirst();

        cmbEstados.setItems(FXCollections.observableArrayList(
                "Estado: Todos",
                "Disponible",
                "Stock bajo",
                "Sin stock"
        ));
        cmbEstados.getSelectionModel().selectFirst();

        txtBuscarProducto.textProperty().addListener((obs, oldVal, newVal) -> renderTableRows());
        cmbCategorias.valueProperty().addListener((obs, oldVal, newVal) -> renderTableRows());
        cmbEstados.valueProperty().addListener((obs, oldVal, newVal) -> renderTableRows());

        renderTableRows();
    }

    private void loadMockupProducts() {
        products.add(new ProductItem("Funda iPhone 14", "FN-IP14-001", "Celulares", "FUNDA", 18, 249.00));
        products.add(new ProductItem("Cable USB-C 1m", "CB-USBC-100", "Cómputo", "CABLE", 42, 99.00));
        products.add(new ProductItem("Audífonos Bluetooth X2", "AU-BT-X2", "Audio", "AUDIO", 6, 450.00));
        products.add(new ProductItem("Cargador 20W", "CG-20W-002", "Cómputo", "CARGA", 25, 189.00));
        products.add(new ProductItem("Mica templada iPhone 14", "MC-IP14-01", "Celulares", "MICA", 0, 79.00));
        products.add(new ProductItem("Power bank 10000mAh", "PB-10K-003", "Cómputo", "POWER", 11, 399.00));
        products.add(new ProductItem("Memoria USB 32GB", "MU-32GB-01", "Cómputo", "USB", 30, 129.00));
    }

    private void renderTableRows() {
        tableRowsContainer.getChildren().clear();

        String query = txtBuscarProducto.getText() == null ? "" : txtBuscarProducto.getText().trim().toLowerCase(Locale.ROOT);
        String catFilter = cmbCategorias.getValue();
        String statusFilter = cmbEstados.getValue();

        List<ProductItem> filtered = products.stream()
                .filter(p -> query.isEmpty()
                        || p.getName().toLowerCase(Locale.ROOT).contains(query)
                        || p.getSku().toLowerCase(Locale.ROOT).contains(query))
                .filter(p -> catFilter == null
                        || "Todas las categorías".equals(catFilter)
                        || p.getCategory().equalsIgnoreCase(catFilter))
                .filter(p -> statusFilter == null
                        || "Estado: Todos".equals(statusFilter)
                        || p.getStatus().equalsIgnoreCase(statusFilter))
                .toList();

        for (ProductItem item : filtered) {
            tableRowsContainer.getChildren().add(createTableRow(item));
        }
    }

    private GridPane createTableRow(ProductItem item) {
        GridPane row = new GridPane();
        row.getStyleClass().add("table-data-row");

        ColumnConstraints col0 = new ColumnConstraints();
        col0.setPercentWidth(33.0);
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(17.0);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(15.0);
        ColumnConstraints col3 = new ColumnConstraints();
        col3.setPercentWidth(9.0);
        col3.setHalignment(HPos.RIGHT);
        ColumnConstraints col4 = new ColumnConstraints();
        col4.setPercentWidth(12.0);
        col4.setHalignment(HPos.RIGHT);
        ColumnConstraints col5 = new ColumnConstraints();
        col5.setPercentWidth(10.0);
        ColumnConstraints col6 = new ColumnConstraints();
        col6.setPercentWidth(4.0);
        col6.setHalignment(HPos.RIGHT);

        row.getColumnConstraints().addAll(col0, col1, col2, col3, col4, col5, col6);

        // Columna 0: Miniatura + Nombre
        HBox productCell = new HBox(12.0);
        productCell.setAlignment(Pos.CENTER_LEFT);
        StackPane thumb = new StackPane();
        thumb.getStyleClass().add("product-mini-thumb");
        Label nameLbl = new Label(item.getName());
        nameLbl.getStyleClass().add("cell-product-name");
        productCell.getChildren().addAll(thumb, nameLbl);

        // Columna 1: SKU
        Label skuLbl = new Label(item.getSku());
        skuLbl.getStyleClass().add("cell-sku");

        // Columna 2: Categoría
        Label catLbl = new Label(item.getCategory());
        catLbl.getStyleClass().add("cell-category");

        // Columna 3: Stock
        Label stockLbl = new Label(String.valueOf(item.getStock()));
        stockLbl.getStyleClass().add("cell-stock");

        // Columna 4: Precio
        Label priceLbl = new Label(CurrencyUtil.format(item.getPrice()));
        priceLbl.getStyleClass().add("cell-price");
        GridPane.setMargin(priceLbl, new Insets(0, 18.0, 0, 0));

        // Columna 5: Badge de Estado
        Label statusBadge = new Label(item.getStatus());
        statusBadge.getStyleClass().add("status-badge");
        switch (item.getStatus()) {
            case "Disponible" -> statusBadge.getStyleClass().add("status-disponible");
            case "Stock bajo" -> statusBadge.getStyleClass().add("status-stock-bajo");
            default -> statusBadge.getStyleClass().add("status-sin-stock");
        }

        // Columna 6: Botón de opciones ⋯
        Button actionsBtn = new Button("⋯");
        actionsBtn.getStyleClass().add("row-action-btn");

        row.add(productCell, 0, 0);
        row.add(skuLbl, 1, 0);
        row.add(catLbl, 2, 0);
        row.add(stockLbl, 3, 0);
        row.add(priceLbl, 4, 0);
        row.add(statusBadge, 5, 0);
        row.add(actionsBtn, 6, 0);

        return row;
    }
}
