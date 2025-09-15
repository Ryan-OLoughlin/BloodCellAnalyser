package oloughlin.ryan;

import java.io.File;
import java.util.Optional;

import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextInputDialog;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
public class PrimaryController {

    @FXML
    private AnchorPane imagePane; // Pane to hold the imageView
    @FXML
    private ImageView imageView = new ImageView();  // ImageView to hold the image
    @FXML
    private Button resetButton;  // Button to reset the sliders
    @FXML
    private Slider hueSlider, saturationSlider, brightnessSlider;  // Sliders to adjust the hue, saturation, and brightness of the image
    @FXML
    private Label fileNameLabel, fileSizeLabel, fileHeightLabel, fileWidthLabel; // Labels to display the file name, size, height, and width

    private ImageHandler imageHandler = new ImageHandler(); // Handles all image processing
    private BloodCellDetector bloodCellDetector = new BloodCellDetector(); // Handles blood cell detection
    private File selectedFile;
    private Image originalImage; // For resetting the image
    private ColorAdjust colorAdjust; 



    @FXML
    private void initialize() {
        // Initialize the ImageView and bind its size to the AnchorPane
        imageView = new ImageView();
        imageView.setPreserveRatio(true); // Maintain aspect ratio
        // Add the ImageView to the AnchorPane
        imagePane.getChildren().add(imageView);
        // Initialize the ColorAdjust effect
        colorAdjust = new ColorAdjust();
        imageView.setEffect(colorAdjust);
        // Bind sliders to auto adjust the hue, saturation, and brightness of the image
        colorAdjust.hueProperty().bind(hueSlider.valueProperty());
        colorAdjust.saturationProperty().bind(saturationSlider.valueProperty());
        colorAdjust.brightnessProperty().bind(brightnessSlider.valueProperty());
    }

    @FXML
    private void handleOpenImage() {
        // Open a FileChooser to select an image
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.bmp", "*.gif")
        );

        Stage stage = (Stage) imagePane.getScene().getWindow();
        selectedFile = fileChooser.showOpenDialog(stage);
        if (selectedFile != null) {
            // Load and set the image in the ImageView
            Image image = new Image(selectedFile.toURI().toString());
            imageView.setImage(image);
            originalImage = image;
            displayMetadata();
        }
    }

    @FXML
    private void searchURL() {
        // Open a dialog box to enter a URL
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Image URL Input");
        dialog.setHeaderText("Enter the URL of the image to process:");
        dialog.setContentText("URL:");

        // Show the dialog and wait for the user's response
        Optional<String> result = dialog.showAndWait();

        // Process the user's input
        if (result.isPresent() && !result.get().isEmpty()) {
            selectedFile = null;                // Clear the selected file
            String imageUrl = result.get();     // Get the URL entered by the user
            Image image = new Image(imageUrl);  // Load the image from the URL
            imageView.setImage(image);          // Display the image in the ImageView
            originalImage = image;              // Save the original image
            fileNameLabel.setText("File Name: " +imageUrl); // Display the URL as the file name
            displayMetadata();                  // Display the metadata
        } 
        else {
            System.out.println("No URL entered.");
        }

    }

    // Display info about the selected file
    private void displayMetadata() {
        fileNameLabel.setText("File Name: " +selectedFile.getName());
        fileSizeLabel.setText("File size: " +selectedFile.length()+ " bytes");
        fileHeightLabel.setText("Height: " +(int) originalImage.getHeight()+ " px");
        fileWidthLabel.setText("Width: " +(int) originalImage.getWidth()+ " px");
    }

    @FXML
    private void revertToOriginal() {
        imageView.setImage(originalImage);  // Revert to unaltered image
    }

    @FXML
    private void convertToTricolor() {
        Image image2Convert = imageView.snapshot(null, null);  // Get the current image (including any adjustments)
        Image convertedImage = imageHandler.extractTricolor(image2Convert);  // Convert the image to a tricolor image and display result
        if(showImagePreview(convertedImage)) {              // popup window with a accept or reject message if the user wants to save the image
            imageView.setImage(convertedImage);             // Set the converted image as the current image
            resetSliders();
        }
    }

    @FXML
    private void detectBloodCells() {
        Image image2Detect = imageView.snapshot(null, null);  // Get the current image (including any adjustments)
        int cellSize = showCellSizeDialog();  // Set the size of the blood cells to detect (user defined)
        bloodCellDetector.displayCellDetections(image2Detect, cellSize);  // Detect blood cells in the image and display result
    }

    @FXML
    private void quit() {
        System.exit(0);  // Quit the application
    }

    // Popup window with a accept or reject message if the user wants to save the image
    public static boolean showImagePreview(Image image) {
        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle("Confirm Processed Image");

        // Create ImageView to show the image
        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(600); // Adjust popup size with this
        imageView.setPreserveRatio(true);

        // Buttons
        Button acceptButton = new Button("Accept");
        Button rejectButton = new Button("Reject");

        // User choice storage
        final boolean[] userAccepted = {false};

        // Button actions
        acceptButton.setOnAction(e -> {
            userAccepted[0] = true;
            dialogStage.close();
        });

        rejectButton.setOnAction(e -> {
            userAccepted[0] = false;
            dialogStage.close();
        });

        // Layout
        VBox layout = new VBox(10);
        layout.getChildren().addAll(imageView, acceptButton, rejectButton);
        layout.setStyle("-fx-padding: 10; -fx-alignment: center;");

        Scene scene = new Scene(layout);
        dialogStage.setScene(scene);
        dialogStage.setMinWidth(800);
        dialogStage.setMinHeight(600);
        dialogStage.setResizable(true);
        dialogStage.showAndWait(); // Wait until user closes the dialog

        return userAccepted[0];
    }

    public int showCellSizeDialog() {
        TextInputDialog dialog = new TextInputDialog("50"); // Default value
        dialog.getEditor().setPrefWidth(200);
        dialog.setTitle("Blood Cell Detection");
        dialog.setHeaderText("Enter Approximate Blood Cell Size:");
        dialog.setContentText("Size (in pixels):");

        Optional<String> result = dialog.showAndWait();
        
        // Parse user input; if invalid, return a default value
        return result.map(input -> {
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                return 50; // Default fallback value
            }
        }).orElse(50);
    }

    @FXML
    private void resetSliders() { // reset all sliders after a conversion is made
        hueSlider.setValue(0);
        saturationSlider.setValue(0);
        brightnessSlider.setValue(0);
    }


    @FXML
    private void buttonEvents() {
        resetButton.setOnAction(e -> {resetSliders();});
    }
}
