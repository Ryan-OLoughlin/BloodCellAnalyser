package oloughlin.ryan;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.image.WritableImage;

public class MyBenchmark {

@State(Scope.Benchmark)

    public static class BenchmarkState {
        ImageHandler imageHandler;
        BloodCellDetector bloodCellDetector;
        
        @Param({"500", "1000", "2000"})
        int imageSize;
        
        @Param({"10", "50", "100"})
        int cellSize;

        WritableImage testImage;

        @Setup(Level.Trial)
        public void setup() {
            System.setProperty("java.awt.headless", "true");
            Platform.startup(() -> {});
            
            imageHandler = new ImageHandler();
            bloodCellDetector = new BloodCellDetector();
            testImage = generateTestImage(imageSize, imageSize);
        }

        // Clean up resources
        @TearDown(Level.Trial)
        public void teardown() {
            Platform.exit();
        }

        private WritableImage generateTestImage(int width, int height) {
            WritableImage image = new WritableImage(width, height);
            var writer = image.getPixelWriter();
            
            // White background
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    writer.setArgb(x, y, 0xFFFFFFFF);
                }
            }
            
            // Add random colored pixels (2% density)
            for (int i = 0; i < width * height * 0.02; i++) {
                int x = (int) (Math.random() * width);
                int y = (int) (Math.random() * height);
                int color = Math.random() < 0.5 ? 0xFFFF0000 : 0xFF800080; // Red or purple
                writer.setArgb(x, y, color);
            }
            return image;
        }

    }

    @Benchmark
    @Warmup(iterations = 3, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 5, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public WritableImage benchmarkConvertToTricolor(BenchmarkState state) {
        return state.imageHandler.extractTricolor(state.testImage);
    }

    @Benchmark
    @Warmup(iterations = 3, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 5, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public Group benchmarkDetectCells(BenchmarkState state) {
        return state.bloodCellDetector.detectCells(state.testImage, state.cellSize);
    }

    public static void main(String[] args) throws Exception {
        org.openjdk.jmh.Main.main(args);
    }
}
