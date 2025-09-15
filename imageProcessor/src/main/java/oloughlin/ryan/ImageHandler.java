package oloughlin.ryan;

import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class ImageHandler {



    public void displayTricolor(Image image2Convert) {
        Stage triStage = new Stage();
        HBox hbox = new HBox();
        triStage.setScene(new Scene(hbox));
        ImageView triView = new ImageView();
        hbox.getChildren().addAll(triView);
        triView.setImage(extractTricolor(image2Convert));
        triStage.show();
    }


    public WritableImage extractTricolor(Image image2Convert) {
        if (image2Convert == null) {
            System.out.println("No image loaded to convert.");
            return null;
        }
        // Get the image width and height
        int width = (int) image2Convert.getWidth();
        int height = (int) image2Convert.getHeight();

        if (width <= 0 || height <= 0) {
            System.err.println("Invalid image dimensions");
            return null;
        }
        // New empty image for processing with R/W
        WritableImage processedImage = new WritableImage(width, height);
        PixelWriter pw = processedImage.getPixelWriter();
        PixelReader pr = image2Convert.getPixelReader();

        // Loop through each pixel in the image and process 3 colour choices
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = pr.getColor(x,y);
                if (isColorCloseTo(color, Color.RED)) {
                    color = Color.RED;
                } 
                else if (isColorCloseTo(color, Color.PURPLE)) {
                    color = Color.PURPLE;
                } 
                else {
                    color = Color.WHITE;
                    
                }
                pw.setColor(x, y, color);
            }
        }
        return processedImage;
    }

    // Find colours close to the target colour
    // HSB comparison, more hits than RGB comparison
    public boolean isColorCloseTo(Color color, Color targetColor) {
        // Get HSB for both
        double hue1 = color.getHue();
        double hue2 = targetColor.getHue();

        // Calc min distance between hues, check for wrapping
        double hueDiff = Math.abs(hue1 - hue2);
        if (hueDiff > 180) {
            hueDiff = 360 - hueDiff;
        }

        double satDiff = Math.abs(color.getSaturation() - targetColor.getSaturation());
        double briDiff = Math.abs(color.getBrightness() - targetColor.getBrightness());

        // Tolerance values
        double hueTol = 20;     // degrees
        double satTol = 0.5;    // 0-1
        double briTol = 0.5;    // 0-1

        return hueDiff < hueTol && satDiff < satTol && briDiff < briTol;
    }
}