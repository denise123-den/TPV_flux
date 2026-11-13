package com.example.tpv_flux.controller.productos;

import com.example.tpv_flux.config.DatabaseSeeder;
import com.example.tpv_flux.config.HibernateUtil;
import com.example.tpv_flux.controller.layout.MainLayoutController;
import com.example.tpv_flux.dao.CategoriaDao;
import com.example.tpv_flux.dao.ProductoDao;
import com.example.tpv_flux.model.Categoria;
import com.example.tpv_flux.model.ProductItem;
import com.example.tpv_flux.model.Producto;
import com.example.tpv_flux.util.CurrencyUtil;
import com.example.tpv_flux.util.ImageUtil;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Controlador para la Plantilla 2: Productos (Catálogo, Tabla de Inventario y Modal Nuevo Producto).
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

    private final ProductoDao productoDao = new ProductoDao();
    private final CategoriaDao categoriaDao = new CategoriaDao();
    private final List<ProductItem> products = new ArrayList<>();
    private final List<String> availableCategories = new ArrayList<>();

    @FXML
    public void initialize() {
        List<String> categoryNames = new ArrayList<>();
        categoryNames.add("Todas las categorías");

        loadProductsAndCategoriesFromDb(categoryNames);

        cmbCategorias.setItems(FXCollections.observableArrayList(categoryNames));
        cmbCategorias.getSelectionModel().selectFirst();

        cmbEstados.setItems(FXCollections.observableArrayList(
                "Estado: Todos",
                "Disponible",
                "Stock bajo",
                "Sin stock"
        ));
        cmbEstados.getSelectionModel().selectFirst();

        btnNuevoProducto.setOnAction(e -> onOpenNuevoProductoModal());

        txtBuscarProducto.textProperty().addListener((obs, oldVal, newVal) -> renderTableRows());
        cmbCategorias.valueProperty().addListener((obs, oldVal, newVal) -> renderTableRows());
        cmbEstados.valueProperty().addListener((obs, oldVal, newVal) -> renderTableRows());

        updateSummaryLabel();
        renderTableRows();
    }

    private void loadProductsAndCategoriesFromDb(List<String> categoryNames) {
        products.clear();
        availableCategories.clear();
        try {
            if (HibernateUtil.isConnected()) {
                DatabaseSeeder.seedIfEmpty();
                List<Categoria> dbCategories = categoriaDao.findAllActive();
                for (Categoria cat : dbCategories) {
                    categoryNames.add(cat.getNombre());
                    availableCategories.add(cat.getNombre());
                }

                List<Producto> dbProducts = productoDao.findAllActive();
                for (Producto p : dbProducts) {
                    products.add(p.toProductItem());
                }
            }
        } catch (Exception e) {
            System.err.println("[ProductosController] Error consultando BD: " + e.getMessage());
        }

        if (categoryNames.size() == 1) {
            List<String> fallbackCats = List.of("Celulares", "Cómputo", "Audio", "Cables y cargadores", "Protección", "Accesorios gaming");
            categoryNames.addAll(fallbackCats);
            availableCategories.addAll(fallbackCats);
        }
        if (products.isEmpty()) {
            loadMockupProducts();
        }
        updateSummaryLabel();
    }

    private void updateSummaryLabel() {
        long lowStockCount = products.stream().filter(p -> p.getStock() <= 6).count();
        lblResumenProductos.setText(products.size() + " productos · " + lowStockCount + " con stock bajo");
    }

    private void loadMockupProducts() {
        products.add(new ProductItem("Funda iPhone 14", "FN-IP14-001", "Celulares", "FUNDA", 18, 249.00, "iphon.jpeg"));
        products.add(new ProductItem("Cable USB-C 1m", "CB-USBC-100", "Cómputo", "CABLE", 42, 99.00, "USB.jpeg"));
        products.add(new ProductItem("Audífonos Bluetooth X2", "AU-BT-X2", "Audio", "AUDIO", 6, 450.00, "audiculares.jpeg"));
        products.add(new ProductItem("Cargador 20W", "CG-20W-002", "Cómputo", "CARGA", 25, 189.00, "Cargador.jpeg"));
        products.add(new ProductItem("Mica templada iPhone 14", "MC-IP14-01", "Celulares", "MICA", 0, 79.00, "Mica.jpeg"));
        products.add(new ProductItem("Power bank 10000mAh", "PB-10K-003", "Cómputo", "POWER", 11, 399.00, "Power.jpeg"));
        products.add(new ProductItem("Memoria USB 32GB", "MU-32GB-01", "Cómputo", "USB", 30, 129.00, "Memoria.jpeg"));
        products.add(new ProductItem("Soporte celular", "SP-CEL-004", "Celulares", "SOPORTE", 14, 149.00, "Soporte.jpeg"));
    }

    private void onOpenNuevoProductoModal() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        if (tableRowsContainer.getScene() != null && tableRowsContainer.getScene().getWindow() != null) {
            dialog.initOwner(tableRowsContainer.getScene().getWindow());
        }
        dialog.setTitle("Nuevo producto — TPV Core");

        VBox root = new VBox(16.0);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: #FFFFFF;");

        // Cabecera
        VBox headerBox = new VBox(2.0);
        Label titleLbl = new Label("Nuevo producto");
        titleLbl.getStyleClass().add("page-title");
        Label subLbl = new Label("Agrega un artículo al catálogo e inventario");
        subLbl.getStyleClass().add("page-subtitle");
        headerBox.getChildren().addAll(titleLbl, subLbl);

        // Fotografía del producto
        AtomicReference<String> selectedPhotoName = new AtomicReference<>(null);
        HBox photoSection = new HBox(14.0);
        photoSection.setAlignment(Pos.CENTER_LEFT);
        StackPane photoPreview = new StackPane();
        photoPreview.getStyleClass().add("photo-upload-box");
        FontIcon cameraIcon = new FontIcon("fth-camera");
        cameraIcon.setIconSize(20);
        photoPreview.getChildren().add(cameraIcon);

        VBox photoActions = new VBox(6.0);
        Label photoTitle = new Label("Fotografía del producto");
        photoTitle.getStyleClass().add("form-label");
        Button btnUploadPhoto = new Button("Subir imagen");
        btnUploadPhoto.getStyleClass().add("secondary-action-btn");
        btnUploadPhoto.setOnAction(ev -> {
            File file = chooseImageFile(dialog, "Seleccionar imagen del producto");
            if (file != null) {
                String savedName = copyImageToResources(file);
                if (savedName != null) {
                    selectedPhotoName.set(savedName);
                    Image previewImg = ImageUtil.loadFromReference(savedName);
                    if (previewImg != null && !previewImg.isError()) {
                        ImageView iv = new ImageView(previewImg);
                        iv.setFitWidth(54);
                        iv.setFitHeight(54);
                        iv.setPreserveRatio(true);
                        photoPreview.getChildren().setAll(iv);
                    }
                }
            }
        });
        photoActions.getChildren().addAll(photoTitle, btnUploadPhoto);
        photoSection.getChildren().addAll(photoPreview, photoActions);

        // Campos del formulario (Mockup Página 5)
        VBox formBox = new VBox(12.0);

        VBox nameFieldBox = new VBox(4.0);
        Label lblNombre = new Label("Nombre del producto *");
        lblNombre.getStyleClass().add("form-label");
        TextField txtNombre = new TextField();
        txtNombre.setPromptText("Ej. Funda iPhone 14 transparente");
        txtNombre.getStyleClass().add("form-input");
        nameFieldBox.getChildren().addAll(lblNombre, txtNombre);

        HBox rowSkuBarcode = new HBox(12.0);
        VBox skuBox = new VBox(4.0);
        HBox.setHgrow(skuBox, Priority.ALWAYS);
        Label lblSku = new Label("SKU");
        lblSku.getStyleClass().add("form-label");
        TextField txtSku = new TextField("FN-IP14-00" + (products.size() + 1));
        txtSku.getStyleClass().add("form-input");
        skuBox.getChildren().addAll(lblSku, txtSku);

        VBox barcodeBox = new VBox(4.0);
        HBox.setHgrow(barcodeBox, Priority.ALWAYS);
        Label lblBarcode = new Label("Código de barras / Tag");
        lblBarcode.getStyleClass().add("form-label");
        TextField txtBarcode = new TextField();
        txtBarcode.setPromptText("Escanear o escribir...");
        txtBarcode.getStyleClass().add("form-input");
        barcodeBox.getChildren().addAll(lblBarcode, txtBarcode);
        rowSkuBarcode.getChildren().addAll(skuBox, barcodeBox);

        HBox rowCatProv = new HBox(12.0);
        VBox catBox = new VBox(4.0);
        HBox.setHgrow(catBox, Priority.ALWAYS);
        Label lblCat = new Label("Categoría *");
        lblCat.getStyleClass().add("form-label");
        ComboBox<String> cmbCatForm = new ComboBox<>(FXCollections.observableArrayList(availableCategories));
        cmbCatForm.setMaxWidth(Double.MAX_VALUE);
        cmbCatForm.getStyleClass().add("filter-combo");
        if (!availableCategories.isEmpty()) {
            cmbCatForm.getSelectionModel().selectFirst();
        }
        catBox.getChildren().addAll(lblCat, cmbCatForm);

        VBox provBox = new VBox(4.0);
        HBox.setHgrow(provBox, Priority.ALWAYS);
        Label lblProv = new Label("Proveedor");
        lblProv.getStyleClass().add("form-label");
        ComboBox<String> cmbProvForm = new ComboBox<>(FXCollections.observableArrayList("Sin proveedor", "Distribuidora Tech", "Importaciones Centro"));
        cmbProvForm.setMaxWidth(Double.MAX_VALUE);
        cmbProvForm.getStyleClass().add("filter-combo");
        cmbProvForm.getSelectionModel().selectFirst();
        provBox.getChildren().addAll(lblProv, cmbProvForm);
        rowCatProv.getChildren().addAll(catBox, provBox);

        HBox rowCostPrice = new HBox(12.0);
        VBox costBox = new VBox(4.0);
        HBox.setHgrow(costBox, Priority.ALWAYS);
        Label lblCost = new Label("Costo");
        lblCost.getStyleClass().add("form-label");
        TextField txtCosto = new TextField("0.00");
        txtCosto.getStyleClass().add("form-input");
        costBox.getChildren().addAll(lblCost, txtCosto);

        VBox priceBox = new VBox(4.0);
        HBox.setHgrow(priceBox, Priority.ALWAYS);
        Label lblPrice = new Label("Precio de venta *");
        lblPrice.getStyleClass().add("form-label");
        TextField txtPrecio = new TextField("199.00");
        txtPrecio.getStyleClass().add("form-input");
        priceBox.getChildren().addAll(lblPrice, txtPrecio);
        rowCostPrice.getChildren().addAll(costBox, priceBox);

        HBox rowStock = new HBox(12.0);
        VBox stockInitBox = new VBox(4.0);
        HBox.setHgrow(stockInitBox, Priority.ALWAYS);
        Label lblStockInit = new Label("Stock inicial *");
        lblStockInit.getStyleClass().add("form-label");
        TextField txtStockInicial = new TextField("10");
        txtStockInicial.getStyleClass().add("form-input");
        stockInitBox.getChildren().addAll(lblStockInit, txtStockInicial);

        VBox stockMinBox = new VBox(4.0);
        HBox.setHgrow(stockMinBox, Priority.ALWAYS);
        Label lblStockMin = new Label("Stock mínimo (alerta)");
        lblStockMin.getStyleClass().add("form-label");
        TextField txtStockMin = new TextField("6");
        txtStockMin.getStyleClass().add("form-input");
        stockMinBox.getChildren().addAll(lblStockMin, txtStockMin);
        rowStock.getChildren().addAll(stockInitBox, stockMinBox);

        VBox descBox = new VBox(4.0);
        Label lblDesc = new Label("Descripción");
        lblDesc.getStyleClass().add("form-label");
        TextArea txtDescripcion = new TextArea();
        txtDescripcion.setPromptText("Detalles, compatibilidad, variantes...");
        txtDescripcion.setPrefRowCount(3);
        txtDescripcion.getStyleClass().add("form-input");
        descBox.getChildren().addAll(lblDesc, txtDescripcion);

        formBox.getChildren().addAll(nameFieldBox, rowSkuBarcode, rowCatProv, rowCostPrice, rowStock, descBox);

        ScrollPane formScroll = new ScrollPane(formBox);
        formScroll.setFitToWidth(true);
        formScroll.getStyleClass().add("clean-scroll");
        VBox.setVgrow(formScroll, Priority.ALWAYS);

        // Botones inferiores
        HBox footerButtons = new HBox(10.0);
        footerButtons.setAlignment(Pos.CENTER_RIGHT);
        Button btnCancelar = new Button("Cancelar");
        btnCancelar.getStyleClass().add("secondary-action-btn");
        btnCancelar.setOnAction(ev -> dialog.close());

        Button btnGuardar = new Button("Guardar producto");
        btnGuardar.getStyleClass().add("primary-action-btn");
        btnGuardar.setOnAction(ev -> {
            String nombre = txtNombre.getText() == null ? "" : txtNombre.getText().trim();
            if (nombre.isEmpty()) {
                showError("Campo obligatorio", "Ingresa el nombre del producto.");
                return;
            }
            String sku = txtSku.getText() == null || txtSku.getText().isBlank()
                    ? "PRD-" + (products.size() + 1)
                    : txtSku.getText().trim();
            String categoriaNombre = cmbCatForm.getValue() != null ? cmbCatForm.getValue() : "Celulares";
            String tag = txtBarcode.getText() == null || txtBarcode.getText().isBlank()
                    ? nombre.split("\\s+")[0].toUpperCase(Locale.ROOT)
                    : txtBarcode.getText().trim().toUpperCase(Locale.ROOT);

            double costoVal;
            double precioVal;
            int stockVal;
            try {
                costoVal = Double.parseDouble(txtCosto.getText().replace("$", "").trim());
                precioVal = Double.parseDouble(txtPrecio.getText().replace("$", "").trim());
                stockVal = Integer.parseInt(txtStockInicial.getText().trim());
            } catch (NumberFormatException ex) {
                showError("Valores numéricos inválidos", "Verifica que el costo, precio de venta y stock inicial sean números válidos.");
                return;
            }

            String extraDesc = txtDescripcion.getText() == null ? "" : txtDescripcion.getText().trim();
            String fullDesc = "SKU: " + sku + " | TAG: " + tag + (extraDesc.isEmpty() ? "" : " | " + extraDesc);
            String photoToSave = selectedPhotoName.get() != null
                    ? selectedPhotoName.get()
                    : ImageUtil.resolveDefaultFileName(nombre, tag);

            try {
                if (HibernateUtil.isConnected()) {
                    Categoria categoriaEntity = categoriaDao.findByNombre(categoriaNombre)
                            .orElseGet(() -> categoriaDao.save(new Categoria(categoriaNombre, true)));
                    Producto nuevo = new Producto(
                            nombre,
                            fullDesc,
                            categoriaEntity,
                            BigDecimal.valueOf(costoVal),
                            BigDecimal.valueOf(precioVal),
                            photoToSave,
                            stockVal,
                            true
                    );
                    Producto guardado = productoDao.save(nuevo);
                    products.add(guardado.toProductItem());
                } else {
                    products.add(new ProductItem(
                            products.size() + 1,
                            nombre,
                            sku,
                            categoriaNombre,
                            tag,
                            stockVal,
                            precioVal,
                            photoToSave
                    ));
                }
                updateSummaryLabel();
                renderTableRows();
                MainLayoutController.refreshNotificationBadge();
                dialog.close();
            } catch (Exception ex) {
                showError("Error al guardar producto", ex.getMessage());
            }
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        footerButtons.getChildren().addAll(spacer, btnCancelar, btnGuardar);

        root.getChildren().addAll(headerBox, photoSection, formScroll, footerButtons);

        Scene scene = new Scene(root, 520, 620);
        if (HelloApplicationStylesheetHelper.getStylesheetUrl() != null) {
            scene.getStylesheets().add(HelloApplicationStylesheetHelper.getStylesheetUrl());
        }
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void showError(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("TPV Core");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
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
        Image img = ImageUtil.resolveProductImage(item);
        if (img != null && !img.isError()) {
            ImageView imageView = new ImageView(img);
            imageView.setFitWidth(28);
            imageView.setFitHeight(28);
            imageView.setPreserveRatio(true);
            imageView.setSmooth(true);
            thumb.getChildren().add(imageView);
        }
        thumb.setOnMouseClicked(e -> onSelectProductImage(item));

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

        // Columna 6: Botón de opciones ⋯ (menú para cambiar fotografía o eliminar producto)
        Button actionsBtn = new Button("⋯");
        actionsBtn.getStyleClass().add("row-action-btn");
        actionsBtn.setTooltip(new Tooltip("Opciones del producto"));

        ContextMenu menu = new ContextMenu();
        MenuItem changeImgItem = new MenuItem("Cambiar imagen");
        changeImgItem.setOnAction(e -> onSelectProductImage(item));
        MenuItem deleteItem = new MenuItem("Eliminar producto");
        deleteItem.setOnAction(e -> onDeleteProduct(item));
        menu.getItems().addAll(changeImgItem, deleteItem);

        actionsBtn.setOnAction(e -> menu.show(actionsBtn, Side.BOTTOM, 0, 0));

        row.add(productCell, 0, 0);
        row.add(skuLbl, 1, 0);
        row.add(catLbl, 2, 0);
        row.add(stockLbl, 3, 0);
        row.add(priceLbl, 4, 0);
        row.add(statusBadge, 5, 0);
        row.add(actionsBtn, 6, 0);

        return row;
    }

    private void onDeleteProduct(ProductItem item) {
        try {
            if (HibernateUtil.isConnected()) {
                if (item.getId() != null) {
                    productoDao.deleteById(item.getId());
                } else {
                    productoDao.deleteByNombre(item.getName());
                }
            }
            products.remove(item);
            updateSummaryLabel();
            renderTableRows();
        } catch (Exception ex) {
            showError("No se pudo eliminar el producto", ex.getMessage());
        }
    }

    private File chooseImageFile( javafx.stage.Window owner, String title) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Imágenes (*.png, *.jpg, *.jpeg, *.webp)", "*.png", "*.jpg", "*.jpeg", "*.webp", "*.gif")
        );
        return chooser.showOpenDialog(owner);
    }

    private String copyImageToResources(File selectedFile) {
        try {
            Path targetDir = Paths.get("src", "main", "resources", "imagenes");
            Files.createDirectories(targetDir);
            String fileName = selectedFile.getName();
            Path targetPath = targetDir.resolve(fileName);
            Files.copy(selectedFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            ImageUtil.clearCache();
            return fileName;
        } catch (Exception ex) {
            System.err.println("[ProductosController] Error al copiar imagen: " + ex.getMessage());
            return selectedFile.getAbsolutePath();
        }
    }

    private void onSelectProductImage(ProductItem item) {
        File selectedFile = chooseImageFile(tableRowsContainer.getScene().getWindow(), "Seleccionar imagen para " + item.getName());
        if (selectedFile == null) {
            return;
        }
        String fileName = copyImageToResources(selectedFile);
        if (fileName != null) {
            item.setPhoto(fileName);
            if (HibernateUtil.isConnected() && item.getId() != null) {
                productoDao.updateFoto(item.getId(), fileName);
            }
            renderTableRows();
        }
    }

    private static final class HelloApplicationStylesheetHelper {
        static String getStylesheetUrl() {
            var res = ProductosController.class.getResource("/com/example/tpv_flux/css/app.css");
            return res != null ? res.toExternalForm() : null;
        }
    }
}
