package com.example.filesharing;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.Key;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

import static com.example.filesharing.ChatClient.*;

/**
 * Controls the clients user interface containing the global chat and online users
 * Controls all actions performed by the user on the interface
 *
 * @author ROBERT SHONE – 25132687
 * @author KEURAN KISTAN – 23251646
 * @author TASHEEL GOVENDER – 25002112
 */
public class ChatController {

    @FXML
    public Button btnSearch;
    @FXML
    public Button btnUpload;
    @FXML
    private TextField txtSearch;
    @FXML
    private ListView<String> filesListView;
    @FXML
    private ProgressBar prgBar;
    @FXML
    private Button btnPause;

    public static int running = 1;
    public static int downloading = 0;
    public String key;



    public void initialize() throws IOException {
        filesListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        filesListView.setOnMouseClicked((MouseEvent event) -> {
            if (event.getClickCount() == 2) {
                String selectedFile = filesListView.getSelectionModel().getSelectedItem();
                System.out.println("\nDouble-clicked file: " + selectedFile);
                String messageKey = generateMessageKey();
                setMessageKey(messageKey);
                try {
                    String encryptedMK = encryptMessage(messageKey, privateKey);
                    sendMessage("得る " + selectedFile + " " + encryptedMK);
                    System.out.println("\nMessage-key generated: " + messageKey);
                    System.out.println("Encrypted Message-key generated: " + encryptedMK);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }


            }
        });


        ChatClient chatClient = new ChatClient();
        chatClient.receiveMessage(filesListView, prgBar);
    }

    /**
     * Encrypts a given message using AES encryption with the provided key.
     *
     * @param message the message to be encrypted.
     * @param key the encryption key. Must be 16 bytes long; if not, it will be padded or truncated.
     * @return the encrypted message encoded as a Base64 string.
     * @throws Exception if any cryptographic operation fails.
     */
    public static String encryptMessage(String message, String key) throws Exception {
        // Ensure the key is 16 bytes long
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        keyBytes = Arrays.copyOf(keyBytes, 16); // Pad or truncate to 16 bytes

        Key secretKey = new SecretKeySpec(keyBytes, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);

        byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
        byte[] encryptedBytes = cipher.doFinal(messageBytes);

        // Encode the encrypted bytes to a base64 string
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    /**
     * Gets the current running state.
     *
     * @return the running state, where 1 indicates running and 0 indicates paused
     */
    public static int getRunning() {
        return running;
    }

    /**
     * Sets the downloading state.
     *
     * @param num the downloading state to set, where 1 indicates downloading and 0 indicates not downloading
     */
    public static void setDownloading(int num) {
        downloading = num;
    }

    /**
     * Handles the search button click event.
     * Sends a search message if the search text is not empty.
     *
     * @param actionEvent the action event triggered by clicking the search button
     * @throws IOException if an I/O error occurs when sending the message
     */
    public void clickSearch(ActionEvent actionEvent) throws IOException {
        String text = txtSearch.getText();
        if (!text.isEmpty()) {
            filesListView.getItems().clear();
            sendMessage("検索 " + text);
        }
    }

    /**
     * Handles the pause button click event.
     * Toggles the running state and updates the pause button text accordingly.
     *
     * @param actionEvent the action event triggered by clicking the pause button
     * @throws IOException if an I/O error occurs
     */
    @FXML
    void clickPause(ActionEvent actionEvent) throws IOException {
        if (downloading == 1) {
            if (running == 1) {
                btnPause.setText("Play");
                running = 0;
            } else {
                btnPause.setText("Pause");
                running = 1;
            }
        }
    }

    /**
     * Generates a random message key using SecureRandom.
     *
     * @return a random message key as a hexadecimal string
     */
    public static String generateMessageKey() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] keyBytes = new byte[80 / 8]; // Convert bits to bytes
        secureRandom.nextBytes(keyBytes);
        BigInteger keyInt = new BigInteger(1, keyBytes); // Create a positive BigInteger
        return keyInt.toString(16); // Convert BigInteger to hexadecimal string
    }


    /**
     * Sends message when the client clicks send
     *
     * @param actionEvent send button event
     * @throws IOException If message can not be sent
     */
    @FXML
    void clickUpload(ActionEvent actionEvent) throws IOException {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select a file");

        // Show the file chooser dialog
        Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);

        // Handle the selected file (e.g., copy it to a new folder)
        if (selectedFile != null) {
            // Destination folder
            String destinationFolder = "Uploads";

            // Create the destination folder if it doesn't exist
            File destFolder = new File(destinationFolder);
            if (!destFolder.exists()) {
                destFolder.mkdirs();
            }

            // Copy the file to the destination folder
            Path sourcePath = selectedFile.toPath();
            Path destPath = new File(destinationFolder, selectedFile.getName()).toPath();
            Files.copy(sourcePath, destPath, StandardCopyOption.REPLACE_EXISTING);

            System.out.println("File copied to: " + destPath);

        }
    }

    /**
     * Updates the ListView with the given file name.
     *
     * @param client the name of the file to add to the list
     * @param onlineUsersListView the ListView to update
     */
    public static void setFileList(String client, ListView<String> onlineUsersListView) {
        Platform.runLater(() -> {
            // Clear the previous content of the listView
            //onlineUsersListView.getItems().clear();

            // Populate the VBox with the list of online users
            onlineUsersListView.getItems().add(client);
        });
    }




}



