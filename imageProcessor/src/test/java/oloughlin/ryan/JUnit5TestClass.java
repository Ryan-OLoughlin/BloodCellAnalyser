package oloughlin.ryan;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;



public class JUnit5TestClass {

    @BeforeAll
    public static void initializeJavaFX() {
        System.setProperty("java.awt.headless", "true");
        Platform.startup(() -> {});
    }

    @Test
    public void testExtractTricolor() {
        ImageHandler imageHandler = new ImageHandler();
        int width = 1000;
        int height = 1000;
        WritableImage image = new WritableImage(width, height);
        PixelWriter pw = image.getPixelWriter();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (x < 3) {
                    pw.setColor(x, y, Color.RED);
                } else if (x < 6) {
                    pw.setColor(x, y, Color.PURPLE);
                } else {
                    pw.setColor(x, y, Color.WHITE);
                }
            }
        }
        WritableImage tricolorImage = imageHandler.extractTricolor(image);
        assertNotNull(tricolorImage);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Color color = tricolorImage.getPixelReader().getColor(x, y);
                if (x < 3) {
                    assertEquals(Color.RED, color);
                } else if (x < 6) {
                    assertEquals(Color.PURPLE, color);
                } else {
                    assertEquals(Color.WHITE, color);
                }
            }
        }
    }

    @Test
    public void testDetectCells() {
        BloodCellDetector bloodCellDetector = new BloodCellDetector();
        int width = 1000;
        int height = 1000;
        WritableImage image = new WritableImage(width, height);
        PixelWriter pw = image.getPixelWriter();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (x < 5 && y < 5) {
                    pw.setColor(x, y, Color.RED);
                } else {
                    pw.setColor(x, y, Color.WHITE);
                }
            }
        }
        int cellSize = 2;
        Group cellDetections = bloodCellDetector.detectCells(image, cellSize);
        assertNotNull(cellDetections);
        
        // Verify detection results
        int rectCount = 0;
        int textCount = 0;
        Rectangle firstRect = null;
        
        for (Node node : cellDetections.getChildren()) {
            if (node instanceof Rectangle) {
                rectCount++;
                if (firstRect == null) {
                    firstRect = (Rectangle) node;
                }
            } else if (node instanceof Text) {
                textCount++;
            }
        }
        
        assertTrue(rectCount > 0, "Should detect at least one cell");
        assertEquals(rectCount, textCount, "Rectangle and text counts should match");
        assertNotNull(firstRect, "Should find at least one rectangle");
        assertTrue(firstRect.getStroke() == Color.GREEN || firstRect.getStroke() == Color.BLUE,
            "Rect should be green (individual) or blue (cluster)");
    }

    @Test
    public void testDetectPurpleCells() {
        BloodCellDetector detector = new BloodCellDetector();
        WritableImage image = new WritableImage(100, 100);
        PixelWriter pw = image.getPixelWriter();
        pw.setColor(10, 10, Color.PURPLE);
        pw.setColor(10, 11, Color.PURPLE);
        
        Group detections = detector.detectCells(image, 2);
        List<Rectangle> rects = new ArrayList<>();
        for (Node node : detections.getChildren()) {
            if (node instanceof Rectangle) {
                rects.add((Rectangle) node);
            }
        }
            
        assertFalse(rects.isEmpty(), "Should detect purple cells");
        assertEquals(Color.PURPLE, rects.get(0).getStroke(), 
            "Purple cells should have purple border");
    }

    @Test
    public void testDetectCellsEmptyImage() {
        BloodCellDetector detector = new BloodCellDetector();
        WritableImage image = new WritableImage(100, 100);
        PixelWriter pw = image.getPixelWriter();
        for (int x = 0; x < 100; x++) {
            for (int y = 0; y < 100; y++) {
                pw.setColor(x, y, Color.WHITE);
            }
        }
        
        Group detections = detector.detectCells(image, 2);
        int rectCount = 0;
        for (Node node : detections.getChildren()) {
            if (node instanceof Rectangle) {
                rectCount++;
            }
        }
            
        assertEquals(0, rectCount, "Should find no cells in empty image");
    }

}
