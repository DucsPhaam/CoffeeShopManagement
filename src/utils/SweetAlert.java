package utils;

import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.BoxBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

/**
 * Custom SweetAlert-style overlay alerts for JavaFX
 * Usage:
 *   SweetAlert.showAlert(parentPane, AlertType.SUCCESS, "Success!", "Data saved successfully");
 *   SweetAlert.showAlert(parentPane, type, title, message, () -> System.out.println("Confirmed"));
 */
public class SweetAlert {

    public enum AlertType {
        SUCCESS, ERROR, WARNING, INFO, QUESTION
    }

    /**
     * Show a beautiful overlay alert (no callback)
     */
    public static void showAlert(Pane parent, AlertType type, String title, String message) {
        showAlert(parent, type, title, message, null);
    }

    /**
     * Show alert with optional callback
     */
    public static void showAlert(Pane parent, AlertType type, String title, String message, Runnable onConfirm) {
        // Get the owner window
        Stage owner = (Stage) parent.getScene().getWindow();

        // Create dialog stage
        Stage dialog = new Stage();
        dialog.initStyle(StageStyle.TRANSPARENT);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(owner);

        // Apply blur to background
        owner.getScene().getRoot().setEffect(new BoxBlur(5, 5, 3));

        // Create root for dialog (transparent)
        StackPane dialogRoot = new StackPane();
        dialogRoot.setStyle("-fx-background-color: transparent;");

        // Create alert card
        VBox alertCard = new VBox(25);
        alertCard.setAlignment(Pos.CENTER);
        alertCard.setPadding(new Insets(40));
        alertCard.setMaxWidth(450);
        alertCard.setStyle(
                "-fx-background-color: white; " +
                        "-fx-background-radius: 20; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 30, 0, 0, 10);"
        );

        // Icon with animation
        ImageView icon = createIcon(type);
        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(400), icon);
        scaleIn.setFromX(0); scaleIn.setFromY(0);
        scaleIn.setToX(1); scaleIn.setToY(1);
        scaleIn.setInterpolator(Interpolator.EASE_OUT);

        // Title
        Label titleLabel = new Label(title);
        titleLabel.setStyle(
                "-fx-font-size: 28px; -fx-font-weight: bold; " +
                        "-fx-text-fill: #1a1a1a; -fx-wrap-text: true; -fx-text-alignment: center;"
        );
        titleLabel.setMaxWidth(380);

        // Message
        Label messageLabel = new Label(message);
        messageLabel.setStyle(
                "-fx-font-size: 16px; -fx-text-fill: #6b7280; " +
                        "-fx-wrap-text: true; -fx-text-alignment: center; -fx-line-spacing: 4px;"
        );
        messageLabel.setMaxWidth(380);
        messageLabel.setWrapText(true);

        // Button
        Button confirmButton = new Button("OK");
        confirmButton.setStyle(
                "-fx-background-color: " + getButtonColor(type) + "; " +
                        "-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; " +
                        "-fx-pref-width: 140; -fx-pref-height: 50; -fx-background-radius: 12; " +
                        "-fx-cursor: hand; -fx-effect: dropshadow(gaussian, " + getShadowColor(type) + ", 10, 0, 0, 4);"
        );

        // Hover effect
        confirmButton.setOnMouseEntered(e -> confirmButton.setScaleX(1.05));
        confirmButton.setOnMouseEntered(e -> confirmButton.setScaleY(1.05));
        confirmButton.setOnMouseExited(e -> confirmButton.setScaleX(1.0));
        confirmButton.setOnMouseExited(e -> confirmButton.setScaleY(1.0));

        confirmButton.setOnAction(e -> {
            hideAlert(dialog, owner);
            if (onConfirm != null) onConfirm.run();
        });

        // Add to card
        alertCard.getChildren().addAll(icon, titleLabel, messageLabel, confirmButton);
        dialogRoot.getChildren().add(alertCard);

        // Set scene
        Scene dialogScene = new Scene(dialogRoot);
        dialogScene.setFill(Color.TRANSPARENT);
        dialog.setScene(dialogScene);

        // Animations
        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), dialogRoot);
        fadeIn.setFromValue(0); fadeIn.setToValue(1);

        ScaleTransition cardScale = new ScaleTransition(Duration.millis(300), alertCard);
        cardScale.setFromX(0.7); cardScale.setFromY(0.7);
        cardScale.setToX(1); cardScale.setToY(1);
        cardScale.setInterpolator(Interpolator.EASE_OUT);

        // Show and play animations
        dialog.show();
        centerStage(dialog);
        fadeIn.play();
        cardScale.play();
        scaleIn.play();

        // Auto close if SUCCESS or INFO
        if (type == AlertType.SUCCESS || type == AlertType.INFO) {
            PauseTransition autoClose = new PauseTransition(Duration.seconds(3));
            autoClose.setOnFinished(e -> hideAlert(dialog, owner));
            autoClose.play();
        }
    }

    /**
     * Show confirmation dialog with Yes/No
     */
    public static void showConfirmation(Pane parent, String title, String message, Runnable onConfirm, Runnable onCancel) {
        // Get the owner window
        Stage owner = (Stage) parent.getScene().getWindow();

        // Create dialog stage
        Stage dialog = new Stage();
        dialog.initStyle(StageStyle.TRANSPARENT);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(owner);

        // Apply blur to background
        owner.getScene().getRoot().setEffect(new BoxBlur(5, 5, 3));

        // Create root for dialog
        StackPane dialogRoot = new StackPane();
        dialogRoot.setStyle("-fx-background-color: transparent;");

        // Create alert card
        VBox alertCard = new VBox(25);
        alertCard.setAlignment(Pos.CENTER);
        alertCard.setPadding(new Insets(40));
        alertCard.setMaxWidth(450);
        alertCard.setStyle(
                "-fx-background-color: white; -fx-background-radius: 20; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 30, 0, 0, 10);"
        );

        // Icon
        ImageView icon = createIcon(AlertType.QUESTION);
        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(400), icon);
        scaleIn.setFromX(0); scaleIn.setFromY(0);
        scaleIn.setToX(1); scaleIn.setToY(1);
        scaleIn.setInterpolator(Interpolator.EASE_OUT);

        // Title
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #1a1a1a; -fx-wrap-text: true; -fx-text-alignment: center;");
        titleLabel.setMaxWidth(380);

        // Message
        Label messageLabel = new Label(message);
        messageLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #6b7280; -fx-wrap-text: true; -fx-text-alignment: center; -fx-line-spacing: 4px;");
        messageLabel.setMaxWidth(380);
        messageLabel.setWrapText(true);

        // Buttons
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);

        Button confirmButton = new Button("Yes, Confirm");
        confirmButton.setStyle(
                "-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold; " +
                        "-fx-pref-width: 160; -fx-pref-height: 50; -fx-background-radius: 12; -fx-cursor: hand; " +
                        "-fx-effect: dropshadow(gaussian, rgba(16, 185, 129, 0.3), 10, 0, 0, 4);"
        );

        Button cancelButton = new Button("Cancel");
        cancelButton.setStyle(
                "-fx-background-color: #e5e7eb; -fx-text-fill: #4a5568; -fx-font-size: 15px; -fx-font-weight: bold; " +
                        "-fx-pref-width: 120; -fx-pref-height: 50; -fx-background-radius: 12; -fx-cursor: hand;"
        );

        confirmButton.setOnAction(e -> {
            hideAlert(dialog, owner);
            if (onConfirm != null) onConfirm.run();
        });

        cancelButton.setOnAction(e -> {
            hideAlert(dialog, owner);
            if (onCancel != null) onCancel.run();
        });

        buttonBox.getChildren().addAll(confirmButton, cancelButton);
        alertCard.getChildren().addAll(icon, titleLabel, messageLabel, buttonBox);
        dialogRoot.getChildren().add(alertCard);

        // Set scene
        Scene dialogScene = new Scene(dialogRoot);
        dialogScene.setFill(Color.TRANSPARENT);
        dialog.setScene(dialogScene);

        // Animations
        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), dialogRoot);
        fadeIn.setFromValue(0); fadeIn.setToValue(1);

        ScaleTransition cardScale = new ScaleTransition(Duration.millis(300), alertCard);
        cardScale.setFromX(0.7); cardScale.setFromY(0.7);
        cardScale.setToX(1); cardScale.setToY(1);
        cardScale.setInterpolator(Interpolator.EASE_OUT);

        // Show and play animations
        dialog.show();
        centerStage(dialog);
        fadeIn.play();
        cardScale.play();
        scaleIn.play();
    }

    /**
     * Hide the alert and remove blur
     */
    private static void hideAlert(Stage dialog, Stage owner) {
        if (dialog != null) {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(200), dialog.getScene().getRoot());
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(e -> {
                dialog.close();
                owner.getScene().getRoot().setEffect(null);
            });
            fadeOut.play();
        }
    }

    /**
     * Center the stage on screen
     */
    private static void centerStage(Stage stage) {
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        stage.setX((screenBounds.getWidth() - stage.getWidth()) / 2);
        stage.setY((screenBounds.getHeight() - stage.getHeight()) / 2);
    }

    // === HÀM HỖ TRỢ ===
    private static ImageView createIcon(AlertType type) {
        ImageView icon = new ImageView();
        String iconPath = switch (type) {
            case SUCCESS -> "/resources/img/circle-check-solid-full.png";
            case ERROR -> "/resources/img/circle-xmark-solid-full.png";
            case WARNING -> "/resources/img/triangle-exclamation-solid-full.png";
            case INFO -> "/resources/img/circle-info-solid-full.png";
            case QUESTION -> "/resources/img/circle-question-solid-full.png";
        };

        try {
            Image image = new Image(SweetAlert.class.getResourceAsStream(iconPath));
            icon.setImage(image);
            icon.setFitWidth(80);
            icon.setFitHeight(80);
            icon.setPreserveRatio(true);
        } catch (Exception e) {
            System.err.println("Failed to load icon: " + iconPath);
        }
        return icon;
    }

    private static String getButtonColor(AlertType type) {
        return switch (type) {
            case SUCCESS -> "#10b981";
            case ERROR -> "#ef4444";
            case WARNING -> "#f59e0b";
            case INFO -> "#3b82f6";
            case QUESTION -> "#8b5cf6";
            default -> "#6b7280";
        };
    }

    private static String getShadowColor(AlertType type) {
        return switch (type) {
            case SUCCESS -> "rgba(16, 185, 129, 0.3)";
            case ERROR -> "rgba(239, 68, 68, 0.3)";
            case WARNING -> "rgba(245, 158, 11, 0.3)";
            case INFO -> "rgba(59, 130, 246, 0.3)";
            case QUESTION -> "rgba(139, 92, 246, 0.3)";
            default -> "rgba(107, 114, 128, 0.3)";
        };
    }
}
