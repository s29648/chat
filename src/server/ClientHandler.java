package server;

//TODO broadcast welcome and goodbye messages on connection/disconnection

import client.MessageTypes;
import server.messageFilters.MessageFilter;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Optional;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final Server server;
    private final ArrayList<MessageFilter> messageFilters;
    private String username;
    protected PrintWriter writer;
    public BufferedReader reader;

    public ClientHandler(Socket socket, Server server, ArrayList<MessageFilter> messageFilters) {
        this.clientSocket = socket;
        this.server = server;
        this.messageFilters = messageFilters;
    }

    @Override
    public void run() {
        try {
            InputStreamReader inputStreamReader = new InputStreamReader(clientSocket.getInputStream());

            reader = new BufferedReader(inputStreamReader);
            writer = new PrintWriter(clientSocket.getOutputStream(), true);

            ConnectedClients newClient = new ConnectedClients(clientSocket.getPort(), writer);

            boolean validUsername = false;
            while (!validUsername) {
                // read username
                username = serverGet();

                System.out.println("Client " + username + " connected");

                //validate according to requirements
                Optional<String> error = UsernameValidator.getError(username, server.getBannedPhrases());

                if (error.isPresent()) {
                    // Send the validation error back to the client
                    serverSend(MessageTypes.Error + error.get());

                    continue;
                }

                    // register the client
                    //to prevent from duplicating usernames ConcurrentHashMap.putIfAbsent() used
                validUsername = server.addUser(username, newClient);
                System.out.println("Client " + username + " added");
                System.out.println(validUsername);

                if (!validUsername) {
                    // If the username already exists, inform the client
                    serverSend("ERROR: Username already taken. Please try a different username.");
                } else {
                    // confirm connection
                    serverSend(MessageTypes.ServerConnected + ":" + username + ":" + server.bannedPhrasesString);
                    System.out.println("Client " + username + " connection confirmed");
                    server.broadcastClientList();
                }
            }

            receiveAndSend();

        } catch (IOException e) {
            System.err.println("Connection error with client (" + username + "): " + e.getMessage());
        } finally {
            disconnectClient();
        }
    }

    // receive message from a client and distribute it to other clients
    private void receiveAndSend() throws IOException {
        try {
            String message = serverGet();

            while (message != null) {
                boolean validMessage = true;

                for (MessageFilter filter : messageFilters) {
                    Optional<String> errorMessage = filter.validate(message);

                    if (errorMessage.isPresent()) {
                        serverSend(MessageTypes.Error + ":" + errorMessage.get());
                        message = serverGet();

                        // return if the client disconnected during validation
                        if (message == null) {
                            System.err.println("Client disconnected during message validation.");
                            disconnectClient();
                            return;
                        }

                        validMessage = false;
                        break;
                    }
                }

                if (validMessage) {
                    String[] messageParts = message.split(":", 2);

                    if (messageParts.length < 2) {
                        switch (message) {
                            case "QUERY_BANNED" -> server.sendBannedPhrases(writer);
                            case "DISCONNECT" -> {
                                disconnectClient();
                                return;
                            }
                            default -> System.err.println("Unknown message type: " + message);
                        }
                    } else {
                        String messageType = messageParts[0];
                        String messageContent = messageParts[1];

                        switch (messageType) {
                            case MessageTypes.SentToSpecific -> server.sendToSpecificUsers(username, messageContent);
                            case MessageTypes.ExcludeRecipients -> server.excludeSpecificUsers(username, messageContent);
                            case MessageTypes.Broadcast -> server.broadcastMessage(username, messageContent);
                            default -> System.err.println("Unknown message type: " + messageType);
                        }
                    }
                }
                //read next
                message = serverGet();
            }
        } catch (IOException e) {
            System.err.println("Client disconnected while reading messages.");
            disconnectClient();
        }
    }

    private void disconnectClient() {
        try {
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }

            if (username != null) {
                server.removeUser(username);
                server.broadcastClientList();
            }

            System.out.println("Client disconnected.");

        } catch (IOException e) {
            System.err.println("Error closing client socket: " + e.getMessage());
        }
    }

    private void serverSend(String message) {
        System.out.println(message);
        writer.println(message);
    }

    private String serverGet() throws IOException {
        return reader.readLine();
    }
}



