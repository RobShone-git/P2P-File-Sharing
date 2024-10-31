module com.example.chatproject {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;


    opens com.example.filesharing to javafx.fxml;
    exports com.example.filesharing;
}