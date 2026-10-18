package com.example.tpv_flux.controller.ventas;

import com.example.tpv_flux.model.ProductItem;
import com.example.tpv_flux.model.TicketLineItem;
import com.example.tpv_flux.util.CurrencyUtil;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Controlador para la Plantilla 1: Ventas (Punto de Venta).
 */
public class VentasController {

    private static final double IVA_RATE = 0.16;
    private static final String PILL_ACTIVE_CLASS = "category-pill-active";
    private static final String PAYMENT_SELECTED_CLASS = "payment-method-btn-selected";

    @FXML
    private TextField txtSearch;

    @FXML
    private Button pillTodos;
    @FXML
    private Button pillCelulares;
    @FXML
    private Button pillComputo;
    @FXML
    private Button pillAudio;

    @FXML
    private GridPane productsGrid;

    @FXML
    private Label lblTicketSubtitle;
    @FXML
    private VBox ticketItemsContainer;
    @FXML
    private Label lblSubtotal;
    @FXML
    private Label lblIva;
    @FXML
    private Label lblTotal;

    @FXML
    private Button btnEfectivo;
    @FXML
    private Button btnTarjeta;
    @FXML
    private Button btnCobrar;

    private final List<ProductItem> catalogProducts = new ArrayList<>();
    private final List<TicketLineItem> ticketItems = new ArrayList<>();
    private List<Button> categoryPills;
    private String selectedCategory = "Todos";

    @FXML
    public void initialize() {
        categoryPills = List.of(pillTodos, pillCelulares, pillComputo, pillAudio);

        loadMockupCatalog();
        loadMockupInitialTicket();

        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> renderProductGrid());

        renderProductGrid();
        renderTicket();
    }

    private void loadMockupCatalog() {
        catalogProducts.add(new ProductItem("Funda iPhone 14", "FN-IP14-001", "Celulares", "FUNDA", 18, 249.00));
        catalogProducts.add(new ProductItem("Cable USB-C 1m", "CB-USBC-100", "Cómputo", "CABLE", 42, 99.00));
        catalogProducts.add(new ProductItem("Audífonos Bluetooth", "AU-BT-X2", "Audio", "AUDIO", 6, 450.00));
        catalogProducts.add(new ProductItem("Cargador 20W", "CG-20W-002", "Cómputo", "CARGA", 25, 189.00));
        catalogProducts.add(new ProductItem("Mica templada", "MC-IP14-01", "Celulares", "MICA", 3, 79.00));
        catalogProducts.add(new ProductItem("Power bank 10000mAh", "PB-10K-003", "Cómputo", "POWER", 11, 399.00));
        catalogProducts.add(new ProductItem("Memoria USB 32GB", "MU-32GB-01", "Cómputo", "USB", 30, 129.00));
        catalogProducts.add(new ProductItem("Soporte celular", "SP-CEL-004", "Celulares", "SOPORTE", 14, 149.00));
    }

    private void loadMockupInitialTicket() {
        // Estado inicial idéntico al mockup (Ticket #0482 · 3 artículos)
        ticketItems.add(new TicketLineItem(catalogProducts.get(0), 1)); // Funda iPhone 14 x1 ($249.00)
        ticketItems.add(new TicketLineItem(catalogProducts.get(1), 2)); // Cable USB-C 1m x2 ($198.00)
        ticketItems.add(new TicketLineItem(catalogProducts.get(2), 1)); // Audífonos Bluetooth x1 ($450.00)
    }

    @FXML
    private void onFilterCategory(ActionEvent event) {
        if (event.getSource() instanceof Button clickedPill) {
            for (Button pill : categoryPills) {
                pill.getStyleClass().remove(PILL_ACTIVE_CLASS);
            }
            clickedPill.getStyleClass().add(PILL_ACTIVE_CLASS);
            selectedCategory = clickedPill.getText();
            renderProductGrid();
        }
    }

    @FXML
    private void onSelectEfectivo() {
        btnEfectivo.getStyleClass().add(PAYMENT_SELECTED_CLASS);
        btnTarjeta.getStyleClass().remove(PAYMENT_SELECTED_CLASS);
    }

    @FXML
    private void onSelectTarjeta() {
        btnTarjeta.getStyleClass().add(PAYMENT_SELECTED_CLASS);
        btnEfectivo.getStyleClass().remove(PAYMENT_SELECTED_CLASS);
    }

    private void renderProductGrid() {
        productsGrid.getChildren().clear();
        String query = txtSearch.getText() == null ? "" : txtSearch.getText().trim().toLowerCase(Locale.ROOT);

        List<ProductItem> filtered = catalogProducts.stream()
                .filter(p -> "Todos".equalsIgnoreCase(selectedCategory) || p.getCategory().equalsIgnoreCase(selectedCategory))
                .filter(p -> query.isEmpty()
                        || p.getName().toLowerCase(Locale.ROOT).contains(query)
                        || p.getSku().toLowerCase(Locale.ROOT).contains(query))
                .toList();

        int col = 0;
        int row = 0;
        for (ProductItem product : filtered) {
            VBox card = createProductCard(product);
            productsGrid.add(card, col, row);
            col++;
            if (col == 4) {
                col = 0;
                row++;
            }
        }
    }

    private VBox createProductCard(ProductItem product) {
        VBox card = new VBox(10.0);
        card.getStyleClass().add("product-card");
        card.setMaxWidth(Double.MAX_VALUE);

        StackPane thumb = new StackPane();
        thumb.getStyleClass().add("product-card-thumb");
        Label tagLabel = new Label(product.getShortTag());
        tagLabel.getStyleClass().add("product-card-tag");
        thumb.getChildren().add(tagLabel);

        Label nameLabel = new Label(product.getName());
        nameLabel.getStyleClass().add("product-card-name");

        HBox bottomRow = new HBox();
        bottomRow.setAlignment(Pos.CENTER_LEFT);

        Label priceLabel = new Label(CurrencyUtil.format(product.getPrice()));
        priceLabel.getStyleClass().add("product-card-price");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label stockLabel = new Label(product.getStock() + " pzs");
        if (product.getStock() <= 6) {
            stockLabel.getStyleClass().add("product-card-stock-low");
        } else {
            stockLabel.getStyleClass().add("product-card-stock");
        }

        bottomRow.getChildren().addAll(priceLabel, spacer, stockLabel);
        card.getChildren().addAll(thumb, nameLabel, bottomRow);

        card.setOnMouseClicked(e -> addProductToTicket(product));
        return card;
    }

    private void addProductToTicket(ProductItem product) {
        for (TicketLineItem line : ticketItems) {
            if (line.getProduct().getSku().equals(product.getSku())) {
                line.setQuantity(line.getQuantity() + 1);
                renderTicket();
                return;
            }
        }
        ticketItems.add(new TicketLineItem(product, 1));
        renderTicket();
    }

    private void renderTicket() {
        ticketItemsContainer.getChildren().clear();

        for (TicketLineItem item : new ArrayList<>(ticketItems)) {
            HBox row = new HBox(10.0);
            row.getStyleClass().add("ticket-line-row");
            row.setAlignment(Pos.CENTER_LEFT);

            VBox infoBox = new VBox(2.0);
            HBox.setHgrow(infoBox, Priority.ALWAYS);
            Label nameLbl = new Label(item.getProduct().getName());
            nameLbl.getStyleClass().add("ticket-item-name");
            Label unitLbl = new Label(CurrencyUtil.format(item.getProduct().getPrice()) + " c/u");
            unitLbl.getStyleClass().add("ticket-item-unit");
            infoBox.getChildren().addAll(nameLbl, unitLbl);

            HBox stepper = new HBox(4.0);
            stepper.getStyleClass().add("qty-stepper");
            stepper.setAlignment(Pos.CENTER);

            Button btnMinus = new Button("−");
            btnMinus.getStyleClass().add("qty-btn");
            btnMinus.setOnAction(e -> {
                if (item.getQuantity() > 1) {
                    item.setQuantity(item.getQuantity() - 1);
                } else {
                    ticketItems.remove(item);
                }
                renderTicket();
            });

            Label qtyLbl = new Label(String.valueOf(item.getQuantity()));
            qtyLbl.getStyleClass().add("qty-value");

            Button btnPlus = new Button("+");
            btnPlus.getStyleClass().add("qty-btn");
            btnPlus.setOnAction(e -> {
                item.setQuantity(item.getQuantity() + 1);
                renderTicket();
            });

            stepper.getChildren().addAll(btnMinus, qtyLbl, btnPlus);

            Label lineTotalLbl = new Label(CurrencyUtil.format(item.getSubtotal()));
            lineTotalLbl.getStyleClass().add("ticket-line-total");

            row.getChildren().addAll(infoBox, stepper, lineTotalLbl);
            ticketItemsContainer.getChildren().add(row);
        }

        double subtotal = ticketItems.stream().mapToDouble(TicketLineItem::getSubtotal).sum();
        double iva = subtotal * IVA_RATE;
        double total = subtotal + iva;

        int distinctArticles = ticketItems.size();
        lblTicketSubtitle.setText("Ticket #0482 · " + distinctArticles + (distinctArticles == 1 ? " artículo" : " artículos"));
        lblSubtotal.setText(CurrencyUtil.format(subtotal));
        lblIva.setText(CurrencyUtil.format(iva));
        lblTotal.setText(CurrencyUtil.format(total));
        btnCobrar.setText("Cobrar " + CurrencyUtil.format(total));
    }
}
