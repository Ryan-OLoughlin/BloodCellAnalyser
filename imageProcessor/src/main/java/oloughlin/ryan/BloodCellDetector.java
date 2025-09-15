package oloughlin.ryan;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class BloodCellDetector {


    // Display the image with cell detections
    public void displayCellDetections(Image processedImage, int cellSize) {
        Stage stage = new Stage();
        Group group = detectCells(processedImage, cellSize);
        
        // Create scene with dimensions matching the image (+ 200 to add space for labels)
        double width = processedImage.getWidth() + 200;
        double height = processedImage.getHeight();
        Scene scene = new Scene(group, width, height);
        
        stage.setScene(scene);
        stage.setMinWidth(width);
        stage.setMinHeight(height);
        stage.setResizable(true);
        stage.setTitle("Blood Cell Detections");
        stage.show();
        }

    // Detect cells in the image and return a Group with the original image and detection rectangles
    public Group detectCells(Image processedImage, int cellSize) {
        int width = (int) processedImage.getWidth();
        int height = (int) processedImage.getHeight();
        int n = width * height;

        // Initialize union–find structures
        int[] parent = new int[n];

        // Array to mark pixel type: 0 = white (ignored), 1 = red, 2 = purple
        int[] colorType = new int[n];

        PixelReader pr = processedImage.getPixelReader();
        for (int i = 0; i < n; i++) {
            parent[i] = -1;     // Size of each set begins at 1. Stored as negative value
        }

        // Classify each pixel based on its hue and saturation
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int idx = y * width + x;
                Color color = pr.getColor(x, y);
                double hue = color.getHue();
                double saturation = color.getSaturation();
                // Red pixels: hue in range 0 or 360
                if ((hue < 20 || hue > 340) && saturation > 0.5) {
                    colorType[idx] = 1; // Red
                } 
                // Purple pixels: hue in range 230-310. (Much wider hue range as I was gettting no hits for purple)
                else if (hue > 230 && hue < 310 && saturation > 0.4) {
                    colorType[idx] = 2; // Purple
                } else {
                    colorType[idx] = 0; // White (ignored)
                }
            }
        }

        // Union adjacent pixels (right and bottom neighbors) if they share the same type
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int idx = y * width + x;
                if (colorType[idx] == 0) continue; // Skip white pixels
                // Right neighbor
                if (x + 1 < width) {
                    int idxRight = y * width + (x + 1);
                    // If they are the same color, union into a single set
                    if (colorType[idxRight] == colorType[idx]) {
                        union(parent, idx, idxRight);
                    }
                }
                // Bottom neighbor
                if (y + 1 < height) {
                    int idxBottom = (y + 1) * width + x;
                    // If they are the same color, union into a single set
                    if (colorType[idxBottom] == colorType[idx]) {
                        union(parent, idx, idxBottom);
                    }
                }
            }
        }

        // Create boundaries for marking each disjoint set
        // For each set, we track min/max x and y and count the pixels
        class Boundary {
            int minX, minY, maxX, maxY, count, type;
            public Boundary(int x, int y, int type) {
                this.minX = x;
                this.maxX = x;
                this.minY = y;
                this.maxY = y;
                this.count = 1;
                this.type = type;
            }
            public void update(int x, int y) {
                if (x < minX) minX = x;
                if (x > maxX) maxX = x;
                if (y < minY) minY = y;
                if (y > maxY) maxY = y;
                count++;
            }
        }
        // Maps the location of each set along with its color
        HashMap<Integer, Boundary> boundaries = new HashMap<>();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int idx = y * width + x;
                // Skips blank/white pixels
                if (colorType[idx] == 0) continue;
                // Use union find to get root of set
                int root = find(parent, idx);
                // If the set is not in the map, add it, otherwise update the existing boundary root
                if (!boundaries.containsKey(root)) {
                    boundaries.put(root, new Boundary(x, y, colorType[idx]));
                } else {
                    boundaries.get(root).update(x, y);
                }
            }
        }

        // Filter out small boundaries (noise) with a treshold
        List<Boundary> boundaryList = new ArrayList<>();
        int noiseThreshold = (cellSize * cellSize) / 4;
        for (Boundary b : boundaries.values()) {
            if (b.count >= noiseThreshold) {
                boundaryList.add(b);
            }
        }

        // Sort boundaries in reading order (top-to-bottom, then left-to-right)
        // Starts from minY, then minX (0,0 top-left corner)
        boundaryList.sort(Comparator.comparingInt(
            (Boundary b) -> b.minY).thenComparingInt(b -> b.minX));

        // User-defined cellSize to determine expected cell area
        // For red blood cells, an individual cell is roughly cellSize^2 pixels
        int expectedRedCellArea = cellSize * cellSize;

        // If a red region's pixel count exceeds 2 times the expected area
        // consider it a cluster.
        int redClusterThreshold = expectedRedCellArea * 2;

        // Create detection rectangles with text labels, cell counter to display
        List<Rectangle> rectangles = new ArrayList<>();
        List<Text> labels = new ArrayList<>();
        int cellCounter = 1;

        // Label to display estimated cell count on mouse hover
        Label estimateLabel = new Label();
        estimateLabel.setFont(Font.font(14));
        estimateLabel.setVisible(false);
        estimateLabel.setTextFill(Color.WHITE);
        estimateLabel.setStyle("-fx-background-color: rgba(0,0,0,0.7); -fx-padding: 4px; -fx-border-radius: 5px;");

        // Label to display total red cell count
        Label redCellCountLabel = new Label();
        redCellCountLabel.setFont(Font.font(14));
        redCellCountLabel.setVisible(true);
        redCellCountLabel.setTextFill(Color.WHITE);
        redCellCountLabel.setStyle("-fx-background-color: rgba(0,0,0,0.7); -fx-padding: 4px; -fx-border-radius: 5px;");
        int redCellCount = 0;

        // Label to display total white cell count
        Label whiteCellCountLabel = new Label();
        whiteCellCountLabel.setFont(Font.font(14));
        whiteCellCountLabel.setVisible(true);
        whiteCellCountLabel.setTextFill(Color.WHITE);
        whiteCellCountLabel.setStyle("-fx-background-color: rgba(0,0,0,0.7); -fx-padding: 4px; -fx-border-radius: 5px;");
        int whiteCellCount = 0;

        // Label to  display total red cluster count
        Label redClusterCountLabel = new Label();
        redClusterCountLabel.setFont(Font.font(14));
        redClusterCountLabel.setVisible(true);
        redClusterCountLabel.setTextFill(Color.WHITE);
        redClusterCountLabel.setStyle("-fx-background-color: rgba(0,0,0,0.7); -fx-padding: 4px; -fx-border-radius: 5px;");
        int redClusterCount = 0;


        // Create rectangles to mark boundaries on image
        for (Boundary b : boundaryList) {
            int rectX = b.minX;
            int rectY = b.minY;
            int rectWidth = b.maxX - b.minX + 1;
            int rectHeight = b.maxY - b.minY + 1;
            Rectangle rect = new Rectangle(rectX, rectY, rectWidth, rectHeight);
            rect.setFill(Color.TRANSPARENT);
            rect.setStrokeWidth(3);

            if (b.type == 1) { // Red blood cell(s)
                if (b.count > redClusterThreshold) {
                    rect.setStroke(Color.BLUE); // Cluster of red blood cells
                    // Estimate the number of cells in cluster and round up to avoid clusters of 1
                    int estimatedCellCount = b.count / expectedRedCellArea;
                    if(estimatedCellCount < 2) estimatedCellCount = 2;
                    int roundedCellCount = estimatedCellCount;
                    // Display estimated cell count on mouse hover
                    rect.setOnMouseEntered(e -> {
                        estimateLabel.setText("Estimated cell count: " + roundedCellCount);
                        estimateLabel.setLayoutX(e.getSceneX() + 10);
                        estimateLabel.setLayoutY(e.getSceneY() + 10);
                        estimateLabel.setVisible(true);
                    });
                    rect.setOnMouseExited(e -> {
                        estimateLabel.setVisible(false);
                    });
                    redClusterCount++;

                } 
                else {
                    rect.setStroke(Color.GREEN); // Individual red blood cell
                    redCellCount++;
                }
            } 
            else if (b.type == 2) { // White blood cell
                rect.setStroke(Color.PURPLE);
                whiteCellCount++;
            }
            rectangles.add(rect);
        

            // Create a label for this cell/cluster
            // Position it at the center of the rectangle
            double centerX = rectX + rectWidth / 2.0;
            double centerY = rectY + rectHeight / 2.0;
            Text label = new Text(centerX, centerY, String.valueOf(cellCounter));
            label.setFill(Color.BLACK);
            label.setFont(Font.font(14));
            labels.add(label);
            cellCounter++;
        }

        // Display total red cell count
        redCellCountLabel.setText("Red Blood Cells: " + redCellCount);
        redCellCountLabel.setLayoutX(width + 10);
        redCellCountLabel.setLayoutY(100);

        // Display total white cell count
        whiteCellCountLabel.setText("White Blood Cells: " + whiteCellCount);
        whiteCellCountLabel.setLayoutX(width + 10);
        whiteCellCountLabel.setLayoutY(200 + 10);

        // Display total red cluster count
        redClusterCountLabel.setText("Red Blood Cell Clusters: " + redClusterCount);
        redClusterCountLabel.setLayoutX(width + 10);
        redClusterCountLabel.setLayoutY(300);

        // Create a Group containing the image, detection rectangles, and labels
        Group group = new Group();
        ImageView resultImageView = new ImageView(processedImage);
        group.getChildren().add(resultImageView);
        group.getChildren().addAll(rectangles);
        group.getChildren().addAll(labels);
        group.getChildren().add(estimateLabel);
        group.getChildren().add(redCellCountLabel);
        group.getChildren().add(whiteCellCountLabel);
        group.getChildren().add(redClusterCountLabel);
        return group;
    }    

    // Recursive find with path compression using
    // the ternary operator
    public static int find(int[] a, int id) {
        return a[id] < 0 ? id : (a[id] = find(a,a[id]));
        }

    // Union-by-size of disjoint sets containing elements p and q. Whichever
    // negative-valued root has the largest absolute value is used as the merged root
    public static void union(int[] a, int p, int q) {
        int rootp = find(a,p);
        int rootq = find(a,q);
        if(rootp == rootq) return;
        int biggerRoot = a[rootp]<a[rootq] ? rootp : rootq;
        int smallerRoot = biggerRoot==rootp ? rootq : rootp;
        int smallSize=a[smallerRoot];
        a[smallerRoot] = biggerRoot;
        a[biggerRoot] += smallSize;     // Value of merged root recalculated as the (negative)
                                        // total number of elements in the merged set
    }
}



// TODO
// total cells + red clusters
// total white blood cellSize