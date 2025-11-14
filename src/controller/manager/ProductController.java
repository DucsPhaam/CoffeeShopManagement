package controller.manager;

import dao.InventoryDAO;
import dao.ProductDAO;
import dao.ProductIngredientDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import model.Inventory;
import model.Product;
import model.ProductIngredient;
import utils.SweetAlert;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javafx.fxml.FXML;

public class ProductController {

    @FXML private TableView<Product> tableProducts;
    @FXML private TableColumn<Product, String> colId;
    //    @FXML private TableColumn<Product, String> colImage;
    @FXML private TableColumn<Product, String> colName;
    @FXML private TableColumn<Product, Double> colPrice;
    @FXML private TableColumn<Product, String> colDrinkTypes;
    @FXML private TableColumn<Product, Void> colActions;

    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cmbFilterType;
    @FXML private Label lblTotalProducts;

    @FXML private VBox dialogPane;
    @FXML private Pane overlay;
    @FXML private Label dialogTitle;
    @FXML private ImageView imgPreview;
    @FXML private TextField txtProductName;
    @FXML private TextField txtPrice;
    @FXML private CheckBox chkHot;
    @FXML private CheckBox chkIced;
    @FXML private CheckBox chkFrappe;
    @FXML private Button btnUploadImage;
    @FXML private Button btnSave;

    // Ingredients management
    @FXML private TableView<ProductIngredientRow> tableIngredients;
    @FXML private TableColumn<ProductIngredientRow, String> colIngredientName;
    @FXML private TableColumn<ProductIngredientRow, Double> colIngredientQuantity;
    @FXML private TableColumn<ProductIngredientRow, String> colIngredientUnit;
    @FXML private TableColumn<ProductIngredientRow, Void> colIngredientActions;
    @FXML private ComboBox<Inventory> cmbInventory;
    @FXML private TextField txtIngredientQuantity;
    @FXML private Button btnAddIngredient;
    @FXML private VBox ingredientsPane;

    private ProductDAO productDAO;
    private ProductIngredientDAO productIngredientDAO;
    private InventoryDAO inventoryDAO;
    private ObservableList<Product> productList;
    private ObservableList<Product> filteredList;
    private ObservableList<ProductIngredientRow> ingredientsList;
    private ObservableList<Inventory> inventoryOptions;

    private Product currentProduct;
    private boolean isEditMode = false;
    private String selectedImagePath = "";
    @FXML private Button btnViewMore;

    private int currentPage = 0;
    private static final int PAGE_SIZE = 10;
    private boolean hasMoreData = true;
    @FXML
    public void initialize() {
        productDAO = new ProductDAO();
        productIngredientDAO = new ProductIngredientDAO();
        inventoryDAO = new InventoryDAO();

        productList = FXCollections.observableArrayList();
        filteredList = FXCollections.observableArrayList();
        ingredientsList = FXCollections.observableArrayList();
        inventoryOptions = FXCollections.observableArrayList();

        setupTableColumns();
        setupIngredientsTable();
        loadProductData();
        loadInventoryOptions();
        setupSearchAndFilter();

        // Set overlay click handler
        overlay.setOnMouseClicked(this::handleOverlayClick);

        cmbInventory.setCellFactory(listView -> new ListCell<Inventory>() {
            @Override
            protected void updateItem(Inventory item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName());
                }
            }
        });

        cmbInventory.setButtonCell(new ListCell<Inventory>() {
            @Override
            protected void updateItem(Inventory item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName());
                }
            }
        });
    }

    private void handleOverlayClick(MouseEvent event) {
        if (dialogPane.isVisible()) {
            hideDialog();
        } else if (ingredientsPane.isVisible()) {
            hideIngredientsDialog();
        }
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(cellData -> {
            ObservableList<Product> items = tableProducts.getItems();
            int index = items.indexOf(cellData.getValue());
            return new javafx.beans.property.SimpleStringProperty(String.valueOf(index + 1));
        });
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colDrinkTypes.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
                String.join(", ", cellData.getValue().getDrinkTypes())));

        // Format price column
        colPrice.setCellFactory(col -> new TableCell<Product, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("$%.2f", item));
                    setFont(javafx.scene.text.Font.font("Segoe UI", 13));
                    setStyle("-fx-text-fill: #1e293b;");
                }
            }
        });

        // Image column
//        colImage.setCellFactory(col -> new TableCell<Product, String>() {
//            private final ImageView imageView = new ImageView();
//
//            {
//                imageView.setFitWidth(50);
//                imageView.setFitHeight(50);
//                imageView.setPreserveRatio(true);
//            }
//
//            @Override
//            protected void updateItem(String imagePath, boolean empty) {
//                super.updateItem(imagePath, empty);
//                if (empty || imagePath == null || imagePath.trim().isEmpty()) {
//                    setGraphic(null);
//                } else {
//                    try {
//                        // Chuẩn hóa đường dẫn
//                        Path path = Paths.get(imagePath);
//                        String fullPath = path.toAbsolutePath().toString();
//                        if (Files.exists(path)) {
//                            Image image = new Image("file:" + fullPath);
//                            if (!image.isError()) {
//                                imageView.setImage(image);
//                                setGraphic(imageView);
//                            } else {
//                                System.out.println("Image error for path: " + fullPath);
//                                loadDefaultImage();
//                            }
//                        } else {
//                            System.out.println("Image file not found: " + fullPath);
//                            loadDefaultImage();
//                        }
//                    } catch (Exception e) {
//                        System.out.println("Error loading image: " + imagePath + ", Error: " + e.getMessage());
//                        loadDefaultImage();
//                    }
//                }
//            }
//
//            private void loadDefaultImage() {
//                String defaultImagePath = "img/temp_icon.png";
//                try {
//                    Path defaultPath = Paths.get(defaultImagePath);
//                    if (Files.exists(defaultPath)) {
//                        Image defaultImage = new Image("file:" + defaultPath.toAbsolutePath().toString());
//                        imageView.setImage(defaultImage);
//                        setGraphic(imageView);
//                    } else {
//                        System.out.println("Default image not found: " + defaultImagePath);
//                        setGraphic(null);
//                    }
//                } catch (Exception e) {
//                    System.out.println("Error loading default image: " + e.getMessage());
//                    setGraphic(null);
//                }
//            }
//        });

        // Actions column with clear, readable buttons
        colActions.setCellFactory(col -> new TableCell<Product, Void>() {
            private final Button btnEdit = new Button("Edit");
            private final Button btnDelete = new Button("Delete");
            private final Button btnIngredients = new Button("Ingredients");
            private final HBox actionBox = new HBox(8);

            {
                // EDIT BUTTON - Blue
                btnEdit.getStyleClass().add("action-button-edit");
                btnEdit.setFont(javafx.scene.text.Font.font("Segoe UI Semibold", 12));
                btnEdit.setStyle(
                        "-fx-background-color: #3b82f6; " +
                                "-fx-text-fill: white; " +
                                "-fx-font-size: 12px; " +
                                "-fx-font-weight: 600; " +
                                "-fx-padding: 6 14; " +
                                "-fx-background-radius: 5; " +
                                "-fx-border-color: transparent; " +
                                "-fx-cursor: hand;"
                );
                btnEdit.setOnMouseEntered(e -> btnEdit.setStyle(
                        btnEdit.getStyle() + "-fx-background-color: #2563eb;"
                ));
                btnEdit.setOnMouseExited(e -> btnEdit.setStyle(
                        btnEdit.getStyle() + "-fx-background-color: #3b82f6;"
                ));

                // INGREDIENTS BUTTON - Purple
                btnIngredients.getStyleClass().add("action-button-view");
                btnIngredients.setFont(javafx.scene.text.Font.font("Segoe UI Semibold", 12));
                btnIngredients.setStyle(
                        "-fx-background-color: #8b5cf6; " +
                                "-fx-text-fill: white; " +
                                "-fx-font-size: 12px; " +
                                "-fx-font-weight: 600; " +
                                "-fx-padding: 6 14; " +
                                "-fx-background-radius: 5; " +
                                "-fx-border-color: transparent; " +
                                "-fx-cursor: hand;"
                );
                btnIngredients.setOnMouseEntered(e -> btnIngredients.setStyle(
                        btnIngredients.getStyle() + "-fx-background-color: #7c3aed;"
                ));
                btnIngredients.setOnMouseExited(e -> btnIngredients.setStyle(
                        btnIngredients.getStyle() + "-fx-background-color: #8b5cf6;"
                ));

                // DELETE BUTTON - Red
                btnDelete.getStyleClass().add("action-button-delete");
                btnDelete.setFont(javafx.scene.text.Font.font("Segoe UI Semibold", 12));
                btnDelete.setStyle(
                        "-fx-background-color: #ef4444; " +
                                "-fx-text-fill: white; " +
                                "-fx-font-size: 12px; " +
                                "-fx-font-weight: 600; " +
                                "-fx-padding: 6 14; " +
                                "-fx-background-radius: 5; " +
                                "-fx-border-color: transparent; " +
                                "-fx-cursor: hand;"
                );
                btnDelete.setOnMouseEntered(e -> btnDelete.setStyle(
                        btnDelete.getStyle() + "-fx-background-color: #dc2626;"
                ));
                btnDelete.setOnMouseExited(e -> btnDelete.setStyle(
                        btnDelete.getStyle() + "-fx-background-color: #ef4444;"
                ));

                actionBox.setAlignment(Pos.CENTER);
                actionBox.getChildren().addAll(btnEdit, btnIngredients, btnDelete);

                btnEdit.setOnAction(event -> {
                    Product product = getTableView().getItems().get(getIndex());
                    handleEditProduct(product);
                });

                btnDelete.setOnAction(event -> {
                    Product product = getTableView().getItems().get(getIndex());
                    handleDeleteProduct(product);
                });

                btnIngredients.setOnAction(event -> {
                    Product product = getTableView().getItems().get(getIndex());
                    handleManageIngredients(product);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(actionBox);
                }
            }
        });
    }

    private void setupIngredientsTable() {
        colIngredientName.setCellValueFactory(new PropertyValueFactory<>("ingredientName"));
        colIngredientQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colIngredientUnit.setCellValueFactory(new PropertyValueFactory<>("unit"));

        // Format quantity
        colIngredientQuantity.setCellFactory(col -> new TableCell<ProductIngredientRow, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f", item));
                }
            }
        });

        // Actions for ingredients
        colIngredientActions.setCellFactory(col -> new TableCell<ProductIngredientRow, Void>() {
            private final Button btnRemove = new Button("Remove");

            {
                btnRemove.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-cursor: hand; -fx-font-size: 11px; -fx-background-radius: 5;");
                btnRemove.setOnAction(event -> {
                    ProductIngredientRow row = getTableView().getItems().get(getIndex());
                    ingredientsList.remove(row);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btnRemove);
                }
            }
        });
    }

    private void loadProductData() {
        currentPage = 0;
        hasMoreData = true;
        productList.clear();
        filteredList.clear();

        List<Product> firstPage = productDAO.getProductsPaginated(currentPage * PAGE_SIZE, PAGE_SIZE);
        productList.addAll(firstPage);
        filteredList.setAll(firstPage);

        tableProducts.setItems(filteredList);
        updateStatistics();
        updateViewMoreButton();
    }

    @FXML
    private void handleLoadMore() {
        if (!hasMoreData) return;

        currentPage++;
        List<Product> nextPage = productDAO.getProductsPaginated(currentPage * PAGE_SIZE, PAGE_SIZE);

        if (nextPage.isEmpty()) {
            hasMoreData = false;
        } else {
            productList.addAll(nextPage);
            applyFilters(); // Áp dụng lại filter
        }

        updateViewMoreButton();
    }

    private void updateViewMoreButton() {
        if (btnViewMore != null) {
            btnViewMore.setVisible(hasMoreData);
            btnViewMore.setManaged(hasMoreData);
        }
    }

    private void loadInventoryOptions() {
        inventoryOptions.clear();
        inventoryOptions.addAll(inventoryDAO.getAllInventory());
        cmbInventory.setItems(inventoryOptions);
    }

    private void setupSearchAndFilter() {
        cmbFilterType.setValue("All");
    }

    @FXML
    private void handleSearch() {
        applyFilters();
    }

    @FXML
    private void handleFilter() {
        applyFilters();
    }

    private void applyFilters() {
        String searchText = txtSearch.getText().toLowerCase().trim();
        String typeFilter = cmbFilterType.getValue() != null ? cmbFilterType.getValue().toLowerCase() : "all";

        filteredList.clear();

        for (Product product : productList) {
            boolean matchSearch = searchText.isEmpty() || product.getName().toLowerCase().contains(searchText);
            boolean matchType = typeFilter.equals("all") ||
                    (product.getDrinkTypes() != null && product.getDrinkTypes().stream()
                            .anyMatch(type -> type.toLowerCase().contains(typeFilter)));

            if (matchSearch && matchType) {
                filteredList.add(product);
            }
        }

        tableProducts.setItems(filteredList);
        updateStatistics();
        updateViewMoreButton();
    }

    private void updateStatistics() {
        lblTotalProducts.setText("Total: " + filteredList.size() + " products");
    }

    @FXML
    private void handleRefresh() {
        loadProductData();
        txtSearch.clear();
        cmbFilterType.setValue("All");
    }

    @FXML
    private void handleAddProduct() {
        isEditMode = false;
        currentProduct = null;
        selectedImagePath = "img/temp_icon.png";
        dialogTitle.setText("Add New Product");

        txtProductName.clear();
        txtPrice.setText("0.00");
        chkHot.setSelected(false);
        chkIced.setSelected(false);
        chkFrappe.setSelected(false);
        imgPreview.setImage(new Image("file:img/temp_icon.png"));

        showDialog();
    }

    private void handleEditProduct(Product product) {
        isEditMode = true;
        currentProduct = product;
        selectedImagePath = product.getImage();
        dialogTitle.setText("Edit Product");

        txtProductName.setText(product.getName());
        txtPrice.setText(String.format("%.2f", product.getPrice()));

        Set<String> types = product.getDrinkTypes();
        chkHot.setSelected(types.contains("hot"));
        chkIced.setSelected(types.contains("iced"));
        chkFrappe.setSelected(types.contains("frappe"));

        try {
            Image image = new Image("file:" + product.getImage());
            imgPreview.setImage(image);
        } catch (Exception e) {
            imgPreview.setImage(new Image("file:img/temp_icon.png"));
        }

        showDialog();
    }

    @FXML
    private void handleUploadImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Product Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        File selectedFile = fileChooser.showOpenDialog(btnUploadImage.getScene().getWindow());
        if (selectedFile != null) {
            try {
                // Define the target directory: src/resources/img/product/
                Path projectDir = Paths.get(System.getProperty("user.dir"));
                Path imgDir = projectDir.resolve("src/resources/img/product");

                // Create the directory if it doesn't exist
                if (!Files.exists(imgDir)) {
                    Files.createDirectories(imgDir);
                    System.out.println("Created directory: " + imgDir.toAbsolutePath());
                }

                // Generate a unique file name to avoid conflicts
                String originalFileName = selectedFile.getName();
                String fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
                String uniqueFileName = UUID.randomUUID().toString() + fileExtension;
                Path destPath = imgDir.resolve(uniqueFileName);

                // Copy the file to the target directory
                Files.copy(selectedFile.toPath(), destPath, StandardCopyOption.REPLACE_EXISTING);

                System.out.println("Image saved to: " + destPath.toAbsolutePath());

                // Store the relative path for use in the application
                selectedImagePath = "img/product/" + uniqueFileName;

                // Update the image preview
                imgPreview.setImage(new Image("file:" + destPath.toAbsolutePath().toString()));

                showSweetAlert(SweetAlert.AlertType.SUCCESS, "Success", "Image uploaded successfully!");

            } catch (IOException e) {
                e.printStackTrace();
                showSweetAlert(SweetAlert.AlertType.ERROR, "Error", "Failed to upload image: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleSaveProduct() {
        if (!validateInput()) {
            return;
        }

        String name = txtProductName.getText().trim();
        double price = Double.parseDouble(txtPrice.getText().trim());

        Set<String> drinkTypes = new HashSet<>();
        if (chkHot.isSelected()) drinkTypes.add("hot");
        if (chkIced.isSelected()) drinkTypes.add("iced");
        if (chkFrappe.isSelected()) drinkTypes.add("frappe");

        if (isEditMode && currentProduct != null) {
            currentProduct.setName(name);
            currentProduct.setPrice(price);
            currentProduct.setDrinkTypes(drinkTypes);
            currentProduct.setImage(selectedImagePath);

            if (productDAO.updateProduct(currentProduct)) {
                showSweetAlert(SweetAlert.AlertType.SUCCESS, "Success", "Product updated successfully.");
                loadProductData();
                handleCloseDialog();
            } else {
                showSweetAlert(SweetAlert.AlertType.ERROR, "Error", "Failed to update product.");
            }
        } else {
            Product newProduct = new Product();
            newProduct.setName(name);
            newProduct.setPrice(price);
            newProduct.setDrinkTypes(drinkTypes);
            newProduct.setImage(selectedImagePath);

            if (productDAO.addProduct(newProduct)) {
                showSweetAlert(SweetAlert.AlertType.SUCCESS, "Success", "Product added successfully.");
                loadProductData();
                handleCloseDialog();
            } else {
                showSweetAlert(SweetAlert.AlertType.ERROR, "Error", "Failed to add product.");
            }
        }
    }

    private void handleDeleteProduct(Product product) {
        showConfirmation("Confirm Delete", "Are you sure you want to delete this product?\nProduct: " + product.getName(), () -> {
            if (productDAO.deleteProduct(product.getId())) {
                showSweetAlert(SweetAlert.AlertType.SUCCESS, "Success", "Product deleted successfully.");
                loadProductData();
            } else {
                showSweetAlert(SweetAlert.AlertType.ERROR, "Error", "Failed to delete product. It may be in use.");
            }
        }, null);
    }

    private void handleManageIngredients(Product product) {
        currentProduct = product;
        ingredientsList.clear();

        // Load existing ingredients
        List<ProductIngredient> existing = productIngredientDAO.getIngredientsByProductId(product.getId());
        for (ProductIngredient pi : existing) {
            Inventory inv = inventoryDAO.getInventoryById(pi.getInventoryId());
            if (inv != null) {
                ingredientsList.add(new ProductIngredientRow(
                        pi.getInventoryId(),
                        inv.getName(),
                        pi.getQuantity(),
                        inv.getUnit()
                ));
            }
        }

        tableIngredients.setItems(ingredientsList);
        showIngredientsDialog();
    }

    @FXML
    private void handleAddIngredient() {
        Inventory selected = cmbInventory.getValue();
        String quantityStr = txtIngredientQuantity.getText().trim();

        if (selected == null) {
            showSweetAlert(SweetAlert.AlertType.WARNING, "Warning", "Please select an ingredient.");
            return;
        }

        if (quantityStr.isEmpty()) {
            showSweetAlert(SweetAlert.AlertType.WARNING, "Warning", "Please enter quantity.");
            return;
        }

        try {
            double quantity = Double.parseDouble(quantityStr);
            if (quantity <= 0) {
                showSweetAlert(SweetAlert.AlertType.WARNING, "Warning", "Quantity must be greater than 0.");
                return;
            }

            // Check if already exists
            for (ProductIngredientRow row : ingredientsList) {
                if (row.getInventoryId() == selected.getId()) {
                    showSweetAlert(SweetAlert.AlertType.WARNING, "Warning", "This ingredient is already added.");
                    return;
                }
            }

            ingredientsList.add(new ProductIngredientRow(
                    selected.getId(),
                    selected.getName(),
                    quantity,
                    selected.getUnit()
            ));

            cmbInventory.setValue(null);
            txtIngredientQuantity.clear();

        } catch (NumberFormatException e) {
            showSweetAlert(SweetAlert.AlertType.WARNING, "Warning", "Invalid quantity format.");
        }
    }

    @FXML
    private void handleSaveIngredients() {
        if (currentProduct == null) return;

        // Delete existing ingredients
        productIngredientDAO.deleteByProductId(currentProduct.getId());

        // Add new ingredients
        for (ProductIngredientRow row : ingredientsList) {
            ProductIngredient pi = new ProductIngredient();
            pi.setProductId(currentProduct.getId());
            pi.setInventoryId(row.getInventoryId());
            pi.setQuantity(row.getQuantity());
            productIngredientDAO.addProductIngredient(pi);
        }

        showSweetAlert(SweetAlert.AlertType.SUCCESS, "Success", "Ingredients saved successfully.");
        hideIngredientsDialog();
    }

    @FXML
    private void handleCloseDialog() {
        hideDialog();
    }

    @FXML
    private void handleCloseIngredientsDialog() {
        hideIngredientsDialog();
    }

    private void showDialog() {
        overlay.setVisible(true);
        overlay.setManaged(true);
        dialogPane.setVisible(true);
        dialogPane.setManaged(true);
    }

    private void hideDialog() {
        overlay.setVisible(false);
        overlay.setManaged(false);
        dialogPane.setVisible(false);
        dialogPane.setManaged(false);
    }

    private void showIngredientsDialog() {
        overlay.setVisible(true);
        overlay.setManaged(true);
        ingredientsPane.setVisible(true);
        ingredientsPane.setManaged(true);
    }

    private void hideIngredientsDialog() {
        overlay.setVisible(false);
        overlay.setManaged(false);
        ingredientsPane.setVisible(false);
        ingredientsPane.setManaged(false);
    }

    private boolean validateInput() {
        if (txtProductName.getText().trim().isEmpty()) {
            showSweetAlert(SweetAlert.AlertType.WARNING, "Warning", "Please enter product name.");
            txtProductName.requestFocus();
            return false;
        }

        try {
            double price = Double.parseDouble(txtPrice.getText().trim());
            if (price <= 0) {
                showSweetAlert(SweetAlert.AlertType.WARNING, "Warning", "Price must be greater than 0.");
                txtPrice.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            showSweetAlert(SweetAlert.AlertType.WARNING, "Warning", "Invalid price format.");
            txtPrice.requestFocus();
            return false;
        }

        if (!chkHot.isSelected() && !chkIced.isSelected() && !chkFrappe.isSelected()) {
            showSweetAlert(SweetAlert.AlertType.WARNING, "Warning", "Please select at least one drink type.");
            return false;
        }

        return true;
    }

    private void showSweetAlert(SweetAlert.AlertType type, String title, String content) {
        try {
            if (tableProducts.getScene() == null) {
                throw new NullPointerException("Scene is null - falling back to standard alert");
            }
            Pane rootPane = (Pane) tableProducts.getScene().getRoot();
            SweetAlert.showAlert(rootPane, type, title, content, null);
        } catch (Exception e) {
            System.err.println("Failed to show SweetAlert: " + e.getMessage());
            // Fallback to regular alert
            Alert alert = new Alert(convertToAlertType(type));
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        }
    }

    private void showConfirmation(String title, String content, Runnable onConfirm, Runnable onCancel) {
        try {
            if (tableProducts.getScene() == null) {
                throw new NullPointerException("Scene is null - falling back to standard confirmation");
            }
            Pane rootPane = (Pane) tableProducts.getScene().getRoot();
            SweetAlert.showConfirmation(rootPane, title, content, onConfirm, onCancel);
        } catch (Exception e) {
            System.err.println("Failed to show SweetConfirmation: " + e.getMessage());
            // Fallback to regular confirmation
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            if (alert.showAndWait().get() == ButtonType.OK) {
                if (onConfirm != null) onConfirm.run();
            } else {
                if (onCancel != null) onCancel.run();
            }
        }
    }

    private Alert.AlertType convertToAlertType(SweetAlert.AlertType sweetType) {
        return switch (sweetType) {
            case SUCCESS -> Alert.AlertType.INFORMATION;
            case ERROR -> Alert.AlertType.ERROR;
            case WARNING -> Alert.AlertType.WARNING;
            case QUESTION -> Alert.AlertType.CONFIRMATION;
            default -> Alert.AlertType.INFORMATION;
        };
    }

    // Inner class for ingredient rows
    public static class ProductIngredientRow {
        private int inventoryId;
        private String ingredientName;
        private double quantity;
        private String unit;

        public ProductIngredientRow(int inventoryId, String ingredientName, double quantity, String unit) {
            this.inventoryId = inventoryId;
            this.ingredientName = ingredientName;
            this.quantity = quantity;
            this.unit = unit;
        }

        public int getInventoryId() { return inventoryId; }
        public String getIngredientName() { return ingredientName; }
        public double getQuantity() { return quantity; }
        public void setQuantity(double quantity) { this.quantity = quantity; }
        public String getUnit() { return unit; }
    }


}
