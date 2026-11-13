package com.example.tpv_flux.controller.ventas;

import com.example.tpv_flux.config.DatabaseSeeder;
import com.example.tpv_flux.config.HibernateUtil;
import com.example.tpv_flux.config.SessionManager;
import com.example.tpv_flux.controller.layout.MainLayoutController;
import com.example.tpv_flux.dao.ProductoDao;
import com.example.tpv_flux.dao.VentaDao;
import com.example.tpv_flux.model.MetodoPago;
import com.example.tpv_flux.model.ProductItem;
import com.example.tpv_flux.model.Producto;
import com.example.tpv_flux.model.TicketLineItem;
import com.example.tpv_flux.util.CurrencyUtil;
import com.example.tpv_flux.util.ImageUtil;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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
    private Label lblBranchTitle;
    @FXML
    private Label lblBranchSubtitle;

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

    private final ProductoDao productoDao = new ProductoDao();
    private final VentaDao ventaDao = new VentaDao();

    private final List<ProductItem> catalogProducts = new ArrayList<>();
    private final List<TicketLineItem> ticketItems = new ArrayList<>();
    private List<Button> categoryPills;
    private String selectedCategory = "Todos";
    private String selectedPaymentMethod = MetodoPago.EFECTIVO;
    private String currentFolio = "#0482";

    @FXML
    public void initialize() {
        categoryPills = List.of(pillTodos, pillCelulares, pillComputo, pillAudio);

        if (lblBranchTitle != null) {
            lblBranchTitle.setText(SessionManager.getTerminalSucursal());
        }
        if (lblBranchSubtitle != null) {
            lblBranchSubtitle.setText("Turno abierto · " + SessionManager.getHoraAperturaTurno());
        }

        loadCatalogFromDatabaseOrFallback();
        loadMockupInitialTicket();

        onSelectEfectivo();
        btnCobrar.setOnAction(e -> onCobrarVenta());
        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> renderProductGrid());

        renderProductGrid();
        renderTicket();
    }

    private void loadCatalogFromDatabaseOrFallback() {
        catalogProducts.clear();
        try {
            if (HibernateUtil.isConnected()) {
                DatabaseSeeder.seedIfEmpty();
                List<Producto> dbProducts = productoDao.findAllActive();
                for (Producto p : dbProducts) {
                    catalogProducts.add(p.toProductItem());
                }
                currentFolio = ventaDao.generarSiguienteFolio();
            }
        } catch (Exception e) {
            System.err.println("[VentasController] Error consultando productos en BD: " + e.getMessage());
        }

        if (catalogProducts.isEmpty()) {
            loadMockupCatalog();
        }
    }

    private void loadMockupCatalog() {
        catalogProducts.add(new ProductItem("Funda iPhone 14", "FN-IP14-001", "Celulares", "FUNDA", 18, 249.00, "iphon.jpeg"));
        catalogProducts.add(new ProductItem("Cable USB-C 1m", "CB-USBC-100", "Cómputo", "CABLE", 42, 99.00, "USB.jpeg"));
        catalogProducts.add(new ProductItem("Audífonos Bluetooth", "AU-BT-X2", "Audio", "AUDIO", 6, 450.00, "audiculares.jpeg"));
        catalogProducts.add(new ProductItem("Cargador 20W", "CG-20W-002", "Cómputo", "CARGA", 25, 189.00, "Cargador.jpeg"));
        catalogProducts.add(new ProductItem("Mica templada", "MC-IP14-01", "Celulares", "MICA", 3, 79.00, "Mica.jpeg"));
        catalogProducts.add(new ProductItem("Power bank 10000mAh", "PB-10K-003", "Cómputo", "POWER", 11, 399.00, "Power.jpeg"));
        catalogProducts.add(new ProductItem("Memoria USB 32GB", "MU-32GB-01", "Cómputo", "USB", 30, 129.00, "Memoria.jpeg"));
        catalogProducts.add(new ProductItem("Soporte celular", "SP-CEL-004", "Celulares", "SOPORTE", 14, 149.00, "Soporte.jpeg"));
    }

    private void loadMockupInitialTicket() {
        if (catalogProducts.size() >= 3) {
            if (catalogProducts.get(0).getStock() >= 1) {
                ticketItems.add(new TicketLineItem(catalogProducts.get(0), 1));
            }
            if (catalogProducts.get(1).getStock() >= 2) {
                ticketItems.add(new TicketLineItem(catalogProducts.get(1), 2));
            }
            if (catalogProducts.get(2).getStock() >= 1) {
                ticketItems.add(new TicketLineItem(catalogProducts.get(2), 1));
            }
        }
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
        selectedPaymentMethod = MetodoPago.EFECTIVO;
        if (!btnEfectivo.getStyleClass().contains(PAYMENT_SELECTED_CLASS)) {
            btnEfectivo.getStyleClass().add(PAYMENT_SELECTED_CLASS);
        }
        btnTarjeta.getStyleClass().remove(PAYMENT_SELECTED_CLASS);
    }

    @FXML
    private void onSelectTarjeta() {
        selectedPaymentMethod = MetodoPago.TARJETA;
        if (!btnTarjeta.getStyleClass().contains(PAYMENT_SELECTED_CLASS)) {
            btnTarjeta.getStyleClass().add(PAYMENT_SELECTED_CLASS);
        }
        btnEfectivo.getStyleClass().remove(PAYMENT_SELECTED_CLASS);
    }

    private void onCobrarVenta() {
        if (ticketItems.isEmpty()) {
            return;
        }
        try {
            if (HibernateUtil.isConnected()) {
                ventaDao.registrarVenta(currentFolio, ticketItems, selectedPaymentMethod);
                loadCatalogFromDatabaseOrFallback();
            } else {
                // Descuento local si no hay conexión a PostgreSQL
                for (TicketLineItem line : ticketItems) {
                    line.getProduct().setStock(Math.max(0, line.getProduct().getStock() - line.getQuantity()));
                }
                MainLayoutController.setFallbackUnreadCount(MainLayoutController.getFallbackUnreadCount() + 1);
            }
            ticketItems.clear();
            renderProductGrid();
            renderTicket();
            MainLayoutController.refreshNotificationBadge();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error al registrar venta");
            alert.setHeaderText("No se pudo procesar el cobro");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
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

        Image productImage = ImageUtil.resolveProductImage(product);
        if (productImage != null && !productImage.isError()) {
            ImageView imageView = new ImageView(productImage);
            imageView.setFitWidth(130);
            imageView.setFitHeight(68);
            imageView.setPreserveRatio(true);
            imageView.setSmooth(true);
            thumb.getChildren().add(imageView);
        } else {
            Label tagLabel = new Label(product.getShortTag());
            tagLabel.getStyleClass().add("product-card-tag");
            thumb.getChildren().add(tagLabel);
        }

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
        if (product.getStock() <= 0) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Sin stock disponible");
            alert.setHeaderText("Producto agotado");
            alert.setContentText("El producto '" + product.getName() + "' no tiene unidades disponibles en inventario.");
            alert.showAndWait();
            return;
        }

        for (TicketLineItem line : ticketItems) {
            if (isSameProduct(line.getProduct(), product)) {
                if (line.getQuantity() + 1 > product.getStock()) {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Stock máximo alcanzado");
                    alert.setHeaderText("Cantidad límite en inventario");
                    alert.setContentText("Solo hay " + product.getStock() + " unidades disponibles de '" + product.getName() + "'.");
                    alert.showAndWait();
                    return;
                }
                line.setQuantity(line.getQuantity() + 1);
                renderTicket();
                return;
            }
        }
        ticketItems.add(new TicketLineItem(product, 1));
        renderTicket();
    }

    private boolean isSameProduct(ProductItem a, ProductItem b) {
        if (a == b) {
            return true;
        }
        if (a.getId() != null && b.getId() != null) {
            return a.getId().equals(b.getId());
        }
        return a.getName().equalsIgnoreCase(b.getName()) && a.getSku().equalsIgnoreCase(b.getSku());
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
                if (item.getQuantity() + 1 > item.getProduct().getStock()) {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Stock máximo alcanzado");
                    alert.setHeaderText("Cantidad límite en inventario");
                    alert.setContentText("Solo hay " + item.getProduct().getStock() + " unidades disponibles de '" + item.getProduct().getName() + "'.");
                    alert.showAndWait();
                    return;
                }
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
        lblTicketSubtitle.setText("Ticket " + currentFolio + " · " + distinctArticles + (distinctArticles == 1 ? " artículo" : " artículos"));
        lblSubtotal.setText(CurrencyUtil.format(subtotal));
        lblIva.setText(CurrencyUtil.format(iva));
        lblTotal.setText(CurrencyUtil.format(total));
        btnCobrar.setText("Cobrar " + CurrencyUtil.format(total));
    }
}
