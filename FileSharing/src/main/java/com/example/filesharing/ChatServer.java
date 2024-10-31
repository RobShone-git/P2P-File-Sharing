package com.example.filesharing;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

/**
 * Server class that is run to start the server.
 *  Starts the server socket and waits for clients to join
 *
 * @author ROBERT SHONE – 25132687
 * @author KEURAN KISTAN – 23251646
 * @author TASHEEL GOVENDER – 25002112
 */
public class ChatServer {


    public static Clients clients;
    static HashMap<Integer, String> groupCallMembers = new HashMap<>();


    public static void main(String[] args) throws IOException {
        //Create server
        ServerSocket serverSocket = new ServerSocket(5000);
        System.out.println("Server is running...");

        //Create Clients which maps usernames to client objects
        clients = new Clients();

        //Create threads so multiple clients can be accpeted at same time
        new Thread(() -> {
            try {
                //Wait for connection to server and create separate thread to handle that client
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    new Thread(new ClientHandler(clientSocket)).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

    }

    public static void shutdownServer() {
        System.exit(0);
    }

    /**
     * Handles the connection between the server and the client
     *
     * @author ROBERT SHONE – 25132687
     * @author KEURAN KISTAN – 23251646
     * @author TASHEEL GOVENDER – 25002112
     */
    private static class ClientHandler implements Runnable {
        private Socket clientSocket;
        private BufferedWriter writer;
        private BufferedReader reader;
        private String username;

        /**
         * Constructor for the class
         *
         * @param socket Client socket
         */
        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try {
                writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
                reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                String ip = reader.readLine();

                int port;
                // Check port
                while (true) {
                    String temp_port = reader.readLine();
                    // If port not used send back the ok
                    if (clients.getPort(Integer.parseInt(temp_port))) {
                        writer.write("false");
                        writer.newLine();
                        writer.flush();
                    } else {
                        writer.write("true");
                        writer.newLine();
                        writer.flush();
                        port = Integer.parseInt(temp_port);
                        break;
                    }

                }

                // Get a unique username
                boolean isUsernameUnique = false;

                // Keep asking for a username until a unique one is provided
                while (!isUsernameUnique) {
                    username = reader.readLine();

                    if (username == null) {
                        clientSocket.close();
                        writer.close();
                        reader.close();
                        return;
                    }
                    // Check if the username is unique
                    if (clients.getClient(username) != null) {
                        // Found a duplicate
                        writer.write("False");
                    } else {
                        // Username is unique
                        writer.write("True");
                        System.out.println("User has connected!");
                        isUsernameUnique = true;
                    }
                    writer.newLine();
                    writer.flush();
                }

                // Add new client to hash-map
                Client client = new Client(reader, writer, port, ip);
                clients.addClient(username, client);

                // Handle client messages
                String message;
                while ((message = reader.readLine()) != null) {
                    if (!message.isEmpty()) {
                        // search for file
                        if (message.startsWith("検索")) {
                            broadcast(message + " " + username, writer);
                            System.out.println(message + " " + username);

                        // File search matches
                        } else if (message.startsWith("確認済み")) {

                            String temp[] = message.split(" ");
                            String fileName = temp[1];
                            String user = temp[2];

                            System.out.println("Sending matches to origin");

                            BufferedWriter bf = clients.getClient(user).getWriter();
                            bf.write("確認済み " + fileName);
                            bf.newLine();
                            bf.flush();


                        // request to retrieve file
                        } else if (message.startsWith("得る")) {
                            broadcast(message + " " + username + " " + client.getPort() + " " + client.getIP(), writer);

                        // Signal that it has the file being requested to download
                        } else if (message.startsWith("もっている")) {
                            String temp[] = message.split(" ");
                            String response = temp[1];
                            String user = temp[2];
                            String messageKey = temp[3];
                            System.out.println(user + " is requesting a file from " + username + "  with message-key: " + messageKey);
                            if (response.equals("true")) {
                                System.out.println("Request is accepted");
                            } else {
                                System.out.println("Request is denied");
                            }

                            BufferedWriter bf = clients.getClient(user).getWriter();
                            bf.write("もっている " + messageKey + " " + response);
                            bf.newLine();
                            bf.flush();

                        } else {
                            // Broadcast the message to all clients
                            System.out.println("!!!!!!!!!!!!!!!!!");
                            //broadcast(username + ": " + message + "\n", writer);

                        }
                    } else {
                        handleDisconnection();
                        break;
                    }
                }
                handleDisconnection();
            } catch (IOException e) {
                try {
                    handleDisconnection();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }

            }
        }


        /**
         * Sends message to all clients
         *
         * @param message Message to be sent
         * @param writer Sender of the message
         */
        private static synchronized void broadcast(String message, BufferedWriter writer) {

            for (Client client : clients.getAllClients().values()) {

                if (client.getWriter() != writer) {
                    BufferedWriter clientWriter = client.getWriter();
                    try {
                        clientWriter.write(message);
                        clientWriter.newLine();
                        clientWriter.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }
        }

        /**
         * Handles the disconnection of the client
         *
         * @throws IOException If disconnection is unsuccessful
         */
        private void handleDisconnection() throws IOException {
            System.out.println("User has Disconnected!");
            if (username != null) {
                Client deserter = clients.getClient(username);
                clientSocket.close();
                deserter.getWriter().close();
                deserter.getReader().close();
                clients.removeClient(username);
                //broadcast(username + " has left the chat.\n", null);
                // Update online client list on clients side
                //broadcast(clients.getAllUsernames(), null);
            }

        }
    }

}
