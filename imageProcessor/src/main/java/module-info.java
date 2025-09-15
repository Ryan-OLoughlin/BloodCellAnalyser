module oloughlin.ryan {
    requires javafx.controls;
    requires javafx.fxml;

    requires jmh.core;
    requires static jmh.generator.annprocess;
    
    opens oloughlin.ryan to javafx.fxml, jmh.core, org.junit.jupiter;
    opens oloughlin.ryan.jmh_generated to jmh.core;
    exports oloughlin.ryan;
}