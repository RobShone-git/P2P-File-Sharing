package com.example.filesharing;

import java.util.HashMap;
import java.io.BufferedReader;
import java.io.BufferedWriter;

/**
 * Stores sll the online clients so that it is easier to access and manage
 *
 * @author ROBERT SHONE – 25132687
 * @author KEURAN KISTAN – 23251646
 * @author TASHEEL GOVENDER – 25002112
 */
public class Clients {
    private HashMap<String, Client> clients;

    public Clients() {
        clients = new HashMap<>();
    }

    /**
     * Add client to the hashmap
     *
     * @param username username of client
     * @param client client object of client
     */
    public void addClient(String username, Client client) {
        clients.put(username, client);
    }

    /**
     *  Get a client by their username
     *
     * @param username Username of client
     * @return Client object of matching client
     */
    public Client getClient(String username) {
        return clients.get(username);
    }

    public boolean getPort(int num) {
        for (Client client : clients.values()) {
            if (client.getPort() == num) {
                return true; // Port found, return true
            }
        }
        return false; // Port not found, return false
    }

    /**
     * Get the hashmap of all clients and their username
     *
     * @return hashmap of clients
     */
    public HashMap<String, Client> getAllClients() {
        return clients;
    }

    /**
     * Remove a client by username
     *
     * @param username User to be removed
     */
    public void removeClient(String username) {
        clients.remove(username);
    }


}
/**
 * Contains information about each client
 *
 * @author ROBERT SHONE – 25132687
 * @author KEURAN KISTAN – 23251646
 * @author TASHEEL GOVENDER – 25002112
 */
class Client {
    private BufferedReader reader;
    private BufferedWriter writer;
    private String IP;
    private int port;
    private boolean inCall;

    /**
     * Constructor for the class
     *
     * @param reader clients reader
     * @param writer clients writer
     */
    public Client(BufferedReader reader, BufferedWriter writer, int num, String ip) {
        this.reader = reader;
        this.writer = writer;
        this.port = num;
        this.inCall = false;
        this.IP = ip;
    }

    /**
     * Returns clients reader
     *
     * @return clients reader
     */
    public BufferedReader getReader() {
        return reader;
    }

    /**
     * Returns clients writer
     *
     * @return clients writer
     */
    public BufferedWriter getWriter() {
        return writer;
    }


    /**
     * Retrieves the port number.
     *
     * @return The port number.
     */
    public int getPort() {
        return port;
    }

    /**
     * Retrieves the IP address.
     *
     * @return The IP address.
     */
    public String getIP() {
        return IP;
    }

}

