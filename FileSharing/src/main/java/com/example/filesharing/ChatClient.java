package com.example.filesharing;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.stage.Stage;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.*;

import static com.example.filesharing.ChatController.*;
import static com.example.filesharing.LoginController.usernameTaken;

/**
 * Main client class that creates the client socket, reader and writer and launches the GUI
 *
 * @author ROBERT SHONE – 25132687
 * @author KEURAN KISTAN – 23251646
 * @author TASHEEL GOVENDER – 25002112
 */
public class ChatClient {
    private static volatile boolean isRunning = true;
    public static BufferedReader in;
    public static BufferedWriter out;

    public static String username;
    public static Socket socket;
    public static int myPort = -1;
    //public static int recipientPort;
    public static String messageKey;
    public ProgressBar prgBar;
    public static int running = 1;
    private static int tempPort = 0;
    public static String privateKey;


    public static void main(String[] args) throws IOException {
        launchLogin(); // Open Login GUI
    }

    /**
     * Sets the key with the given one
     * @param cur Key provide
     */
    public static void setMessageKey(String cur) {
        messageKey = cur;
    }

    /**
     * Creates sockets and readers and writers
     *
     * @throws IOException If there is an error in creating reader or writer
     */
    public static void setServer(String host, String ip) throws IOException {
        //create socket and store input and output streams
        try {
            socket = new Socket(host, 5000);
        } catch (IOException e) {
            System.out.println("Server is not online, try again later x");
            System.exit(0);
        }

        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

        out.write(ip);
        out.newLine();
        out.flush();

    }

    /**
     * Validates the users inputted username to ensure it is unique
     *
     * @param message          message containing username
     * @param lblUsernameTaken label that will be displayed if username is not unique
     * @throws IOException If user interface can not be launched
     */
    public static void checkUsername(String message, Label lblUsernameTaken) throws IOException {

        while (myPort == -1) {
            Random rand = new Random();
            // Generate a random 4-digit number
            tempPort = rand.nextInt(9000) + 1000;
            // Send port to server to see if there's duplicates
            out.write(tempPort + "");
            out.newLine();
            out.flush();

            // Get back signal if there duplicates
            String cur = in.readLine();
            if (cur.equals("true")) {
                break;
            }
        }

        myPort = tempPort;

        username = message;
        out.write(message);
        out.newLine();
        out.flush();

        String result = in.readLine();

        if (result == null) {
            System.out.println("Server is down");
            System.exit(0);
        }

        //check is false then username is taken
        if (result.equals("False")) {
            usernameTaken(lblUsernameTaken);
        } else {
            System.out.println("This is my port: " + myPort);
            Platform.runLater(() -> {
                try {
                    privateKey = generateKey();
                    //setPrivateKey(privateKey);
                    System.out.println("Private key generated: " + privateKey);
                    launchChat();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }

    }

    /**
     * Generates a key that should be unique for each client
     */
    public static String generateKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(128); // For AES-128
        SecretKey secretKey = keyGenerator.generateKey();
        byte[] keyBytes = secretKey.getEncoded();
        return Base64.getEncoder().encodeToString(keyBytes);
    }


    /**
     * Sends message to other users
     *
     * @param message the message that will be sent
     * @throws IOException If the writer does not work
     */
    public static void sendMessage(String message) throws IOException {
        if (isRunning) {
            out.write(message);
            out.newLine();
            out.flush();
        }
    }

    /**
     * Launches login screen
     */
    private static void launchLogin() throws IOException {
        // Start the login GUI
        Application.launch(LoginApplication.class);


    }

    /**
     * Launches request screen
     */
    private boolean launchRequest() throws IOException {

        RequestApplication requestapplication = new RequestApplication();

        // Launch the GUI in a new thread
        RequestApplication req = new RequestApplication();
        Platform.runLater(() -> {
            Stage chatStage = new Stage();
            try {
                req.start(chatStage);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        // Wait for the button to be pressed and get the result
        try {
            boolean out = requestapplication.awaitButtonPress();
            requestapplication.setResultBack();
            return out;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            System.out.println("Thread was interrupted, failed to complete operation");
            return false;
        }
    }

    /**
     * Launches the user interface containing the global chat and online users list
     *
     * @throws IOException If it can not launch the user interface
     */
    private static void launchChat() throws IOException {
        // Close the login GUI
        closeLogin();

        // Start the chat GUI directly
        ChatGuiApplication chatApp = new ChatGuiApplication(username);
        Stage chatStage = new Stage();
        chatApp.start(chatStage);

        // Add event handler to detect when the chat GUI is closed
        chatStage.setOnCloseRequest(event -> {
            try {
                isRunning = false;
                socket.close();
                in.close();
                out.close();

            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        });

    }

    /**
     * Receives and processes messages in a separate thread.
     * This method handles different types of messages including file upload requests, file download requests,
     * and other notifications, and updates the UI components accordingly.
     *
     * @param filesListView the ListView component to update with file names
     * @param bar the ProgressBar to track file upload/download progress
     */
    public void receiveMessage(ListView filesListView, ProgressBar bar) {
        prgBar = bar;
        // Create a separate thread for receiving messages
        new Thread(() -> {
            while (isRunning) {
                String message = null;
                try {
                    synchronized (in) {
                        // Synchronize the readLine operation
                        message = in.readLine();

                        if (message == null) {
                            System.out.println("Server Disconnected :(");
                            break;
                            //System.exit(0);
                        }
                    }

                    // Check uploads for file being requested
                    if (message != null && message.startsWith("検索")) {
                        String temp[] = message.split(" ");
                        checkUploads(temp[1], temp[2]);

                        // This is for matches of file name
                    } else if (message != null && message.startsWith("確認済み")) {
                        String fileName = message.split(" ")[1];
                        System.out.println("Add to list view");
                        setFileList(fileName, filesListView);

                        // Request for file to download
                    } else if (message != null && message.startsWith("得る")) {
                        String temp[] = message.split(" ");
                        String fileName = temp[1];
                        String messageKey = temp[2];
                        String user = temp[3];

                        File selectedFile = isFileUploaded(fileName);
                        // if file is found
                        if (selectedFile != null) {

                            // launch request window

                            new Thread(() -> {
                                boolean result = false;
                                try {
                                    result = launchRequest();
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }

                                if (result) {
                                    System.out.println("accepted");
                                    try {
                                        sendMessage("もっている " + "true " + user + " " + messageKey);
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }

                                    int recipientPort = Integer.parseInt(temp[4]);
                                    String recipientIP = temp[5];
                                    // Start tcp connection and sending

                                    try {
                                        sendTCP(selectedFile, recipientIP, recipientPort);
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    } catch (InterruptedException e) {
                                        throw new RuntimeException(e);
                                    }


                                } else {
                                    System.out.println("declined");
                                    try {
                                        sendMessage("もっている " + "false " + user + " " + messageKey);
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                            }).start();
                        }


                        // Message response with port of sender
                    } else if (message != null && message.startsWith("もっている")) {
                        String temp[] = message.split(" ");
                        String response = temp[2];
                        String key = temp[1];

                        System.out.println("\nEncrypted Message key received: " + key);

                        String decryptedKey = decryptMessage(key, privateKey);
                        System.out.println("Decrypted Message key received: " + decryptedKey);

                        if (response.equals("true") && messageKey.equals(decryptedKey)) {
                            System.out.println("\nReceived client details");

                            // If the message key is the same as the one sent out
                            System.out.println("Key matches");
                            // Start tcp connection and receiving
                            setupTCP();

                        } else {
                            System.out.println("\nDownload request has been denied by user");
                        }


                        // Request for file to download
                    } else if (message != null) {
                        //playRingtone();

                    }

                } catch (SocketException e) {
                    // Socket closed, exit the loop
                    break;
                } catch (IOException e) {

                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    /**
     * Decrypts a given encrypted message using AES decryption with the provided key.
     *
     * @param encryptedMessage the encrypted message encoded as a Base64 string.
     * @param key the decryption key. Must be 16 bytes long; if not, it will be padded or truncated.
     * @return the decrypted message as a UTF-8 string.
     * @throws Exception if any cryptographic operation fails.
     */
    public static String decryptMessage(String encryptedMessage, String key) throws Exception {
        // Ensure the key is 16 bytes long
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        keyBytes = Arrays.copyOf(keyBytes, 16); // Pad or truncate to 16 bytes

        Key secretKey = new SecretKeySpec(keyBytes, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);

        byte[] encryptedBytes = Base64.getDecoder().decode(encryptedMessage);
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    /**
     * Checks if a file with the given name exists in the "Uploads" folder.
     *
     * @param fileName the name of the file to check
     * @return the file if it exists in the "Uploads" folder, otherwise {@code null}
     */
    public File isFileUploaded(String fileName) {
        String uploadsFolder = "Uploads";
        File uploadsDir = new File(uploadsFolder);

        // Check if the "uploads" folder exists
        if (!uploadsDir.exists() || !uploadsDir.isDirectory()) {
            System.out.println("Uploads folder doesn't exist or is not a directory.");
            return null;
        }

        // Get the list of files in the "uploads" folder
        File[] filesInUploads = uploadsDir.listFiles();
        if (filesInUploads != null) {
            for (File file : filesInUploads) {
                // Check if any file in the "uploads" folder has the exact filename
                if (file.getName().equals(fileName)) {
                    return file;
                }
            }
        }

        // No file with the exact filename found in the "uploads" folder
        return null;
    }


    /**
     * Checks for files in the "Uploads" folder that match the given file name using exact match,
     * substring match, or close match based on the Levenshtein distance.
     * If matching files are found, a confirmation message is sent for each match.
     *
     * @param fileName the name of the file to check for matches
     * @param username the username to include in the confirmation message
     * @throws IOException if an I/O error occurs when accessing the "Uploads" folder
     */
    public void checkUploads(String fileName, String username) throws IOException {
        List<String> matchingFiles = new ArrayList<>();
        String uploadsFolder = "Uploads";
        File uploadsDir = new File(uploadsFolder);

        // Check if the "uploads" folder exists
        if (!uploadsDir.exists() || !uploadsDir.isDirectory()) {
            System.out.println("Uploads folder doesn't exist or is not a directory.");
            return;
        }

        // Get the list of files in the "uploads" folder
        File[] filesInUploads = uploadsDir.listFiles();
        if (filesInUploads != null) {
            for (File file : filesInUploads) {
                String existingFileName = file.getName();
                String name = existingFileName.substring(0, existingFileName.length()-4);
                // Check for exact match
                if (existingFileName.equals(fileName)) {
                    matchingFiles.add(existingFileName);
                }
                // Check for substring match
                else if (existingFileName.toLowerCase().contains(fileName.toLowerCase())) {
                    matchingFiles.add(existingFileName);
                }
                // Check for close match

                else if (levenshteinDistance(fileName.toLowerCase(), name.toLowerCase()) <= 2) {
                    matchingFiles.add(existingFileName);
                }
            }
        }

        if (!matchingFiles.isEmpty()) {
            System.out.println("\nFound files that match");
            for (String file : matchingFiles) {
                sendMessage("確認済み " + file + " " + username);
            }

        } else {
            // No file with the same name found in the "uploads" folder
            System.out.println("No file with the same name found in the 'uploads' folder.");
        }
    }

    // Method to calculate Levenshtein Distance between two strings

    /**
     * Calculates the Levenshtein distance between two strings.
     * The Levenshtein distance is a measure of the minimum number of single-character edits
     * (insertions, deletions, or substitutions) required to change one string into the other.
     *
     * @param s1 the first string
     * @param s2 the second string
     * @return the Levenshtein distance between the two strings
     */
    public static int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                    dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
                }
            }
        }

        return dp[s1.length()][s2.length()];
    }


    /**
     * Closes the login screen
     */
    private static void closeLogin() {
        // Get the stage of the login GUI
        Stage loginStage = LoginApplication.getStage();

        // Close the login GUI
        loginStage.close();
    }

    /**
     * Sets up a TCP server to accept incoming file transfer connections.
     * This method starts a server socket on the specified port and listens for incoming connections.
     * When a connection is accepted, it spawns a new thread to handle the file reception.
     *
     * @throws IOException          if an I/O error occurs when opening the socket or accepting connections
     * @throws InterruptedException if the thread is interrupted while waiting for connections
     */
    public void setupTCP() throws IOException, InterruptedException {
        ServerSocket serverSocket = new ServerSocket(myPort);

        System.out.println("Start TCP");

        new Thread(() -> {
            try {
                //Wait for connection to server and create separate thread to handle that client
                Socket clientSocket = serverSocket.accept();
                System.out.println("Connection accepted");
                receiveFileTCP(clientSocket);
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    /**
     * Receives a file from a client over a TCP connection and saves it to the local file system.
     *
     * @param clientSocket the socket connected to the client sending the file
     * @throws IOException          if an I/O error occurs when receiving the file or writing it to the disk
     * @throws InterruptedException if the thread is interrupted while sleeping
     */
    private void receiveFileTCP(Socket clientSocket) throws IOException, InterruptedException {
        setDownloading(1);

        // Create input stream to receive data from client
        DataInputStream inputStream = new DataInputStream(clientSocket.getInputStream());
        DataOutputStream outputStream = new DataOutputStream(clientSocket.getOutputStream());

        int fileNameLength = inputStream.readInt();

        if (fileNameLength > 0) {
            byte[] fileNameBytes = new byte[fileNameLength];
            inputStream.readFully(fileNameBytes, 0, fileNameBytes.length);
            String fileName = new String(fileNameBytes);

            int fileContentLength = inputStream.readInt();

            if (fileContentLength > 0) {

                long totalFileSize = fileContentLength;
                long bytesRead = 0;

                byte[] fileContentBytes = new byte[fileContentLength];

                while (bytesRead <= totalFileSize) {

                    running = getRunning();
                    // If its paused then go into infinite loop
                    while(running == 0) {
                        running = getRunning();
                        Thread.sleep(200);
                    }


                    // Calculate the progress
                    double progress = (double) bytesRead / totalFileSize;

                    // Update the progress bar in ReceiverController
                    // Platform.runLater(() -> {
                    prgBar.setProgress(progress);
                    //});

                    // Set the amount of bytes to read per loop
                    int bytesToRead = 10;

                    // Read data chunk
                    int chunkSize = (int) Math.min(fileContentBytes.length - bytesRead, bytesToRead);
                    int bytes = inputStream.read(fileContentBytes, (int) bytesRead, chunkSize);

                    // Update bytesRead
                    if (bytes > 0) {
                        bytesRead += bytes;
                    } else {
                        // Break out of loop when no more bytes are being read
                        break;
                    }
                }
                setDownloading(0);

                outputStream.writeInt(1);
                inputStream.close();
                outputStream.close();
                clientSocket.close();

                // Specify the destination folder
                String destinationFolder = "Downloads/";
                File folder = new File(destinationFolder);

                // Check if the destination folder exists, if not, create it
                if (!folder.exists()) {
                    boolean created = folder.mkdirs(); // Create the folder and any necessary parent folders
                    if (!created) {
                        System.err.println("Failed to create destination folder: " + destinationFolder);
                        return; // Exit the method if folder creation fails
                    }
                }

                File file = new File(destinationFolder + fileName);

                // Write the file content to the destination file
                try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                    fileOutputStream.write(fileContentBytes);
                    System.out.println("File downloaded and saved to: " + file.getAbsolutePath());
                } catch (Exception e) {
                    System.out.println("Failed to save file: " + e.getMessage());
                }

            }
        }
    }

    /**
     * Sends a file to a specified host and port using TCP.
     *
     * @param selectedFile  the file to be sent
     * @param host          the hostname or IP address of the recipient
     * @param recipientPort the port number of the recipient
     * @throws IOException          if an I/O error occurs when creating the socket or reading the file
     * @throws InterruptedException if the thread is interrupted while sleeping
     */
    private void sendTCP(File selectedFile, String host, int recipientPort) throws IOException, InterruptedException {
        Thread.sleep(1000);
        Socket socket = new Socket(host, recipientPort);

        DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
        DataInputStream inputStream = new DataInputStream(socket.getInputStream());

        if (selectedFile != null) {
            try {

                FileInputStream fileInputStream = new FileInputStream(selectedFile.getAbsolutePath());

                String fileName = selectedFile.getName();
                byte[] fileNameBytes = fileName.getBytes();

                byte[] fileContentBytes = new byte[(int) selectedFile.length()];
                fileInputStream.read(fileContentBytes);

                dataOutputStream.writeInt(fileNameBytes.length);
                dataOutputStream.write(fileNameBytes);

                dataOutputStream.writeInt(fileContentBytes.length);
                dataOutputStream.write(fileContentBytes);

                int end = inputStream.readInt();
                if (end == 1) {
                    dataOutputStream.close();
                    inputStream.close();
                    socket.close();
                }

            } catch (IOException e) {
                System.out.println("Connection to Receiver has been terminated");
                System.exit(0);
            }

        }

    }


}
