package com.company.client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;


/**
 * Representation of the server.
 * Created by e_voe_000 on 11/18/2016.
 */
public class Server implements Runnable {
    ///////////////////////////////////////////////////////////////////////////
    // Properties
    ///////////////////////////////////////////////////////////////////////////
    private Socket clientSocket;

    private ObjectInputStream in;
    private PrintWriter out;
    private static String clientUsername;

    private static HashMap<String, Socket> clients = new HashMap<>();

    public Server(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    public static void main(String[] args) throws Exception {
        ServerSocket serverSocket = new ServerSocket(1500);
        while (true) {
            Socket socket = serverSocket.accept();

            Server server = new Server(socket);

            server.setClientUsername(socket);

            new Thread(server).start();

            clients.put(clientUsername, socket);

            System.out.println(clientUsername + " connected!");
        }
    }

    /**
     * Used for broadcast the coming messages.
     */
    public void run() {
        try {
            Message message;

            String sendMsg = "send";
            String msgTo = "to";
            String sendAllMsg = "sendAll";

            chatIntro();

            while ((message = ((Message) in.readObject())) != null) {
                String a[] = message.getText().split(" ");

                if (a[0].equals(sendAllMsg)) {
                    for (Map.Entry<String, Socket> client : clients.entrySet()) {
                        Socket currentClient = clients.get(client.getKey());

                        out = new PrintWriter(currentClient.getOutputStream(), true);
                        out.println(message.getSender() + ": " + a[1]);
                        out.flush();
                    }
                } else if (a[0].equals(sendMsg) && a[2].equals(msgTo)) {
                    if (clients.containsKey(a[3])) {
                        Socket receiverClient = clients.get(a[3]);

                        out = new PrintWriter(receiverClient.getOutputStream(), true);
                        out.println(message.getSender() + ": " + a[1]);
                        out.flush();
                    } else {
                        out = new PrintWriter(clientSocket.getOutputStream(), true);
                        out.println(a[3] + " is offline");
                        out.flush();
                    }
                } else {
                    out = new PrintWriter(clientSocket.getOutputStream(), true);
                    out.println("Not a valid message");
                    out.flush();
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setClientUsername(Socket socket) throws IOException, ClassNotFoundException {
        in = new ObjectInputStream(socket.getInputStream());
        String initialMessage = (String) in.readObject();
        String a[] = initialMessage.split(" ");
        if (a[0].equals("username:")) {
            clientUsername = a[1];
        }
    }

    private void chatIntro() throws IOException {
        out = new PrintWriter(clientSocket.getOutputStream(), true);
        out.println("Hi " + clientUsername);
        out.println("To send a message to all users type: sendAll then your message.");
        out.println("Example: sendAll Hi everyone!");
        out.println("To send a message to a specific user type: send then your message " +
                "\nthen to then you username you want to send your message to.");
        out.println("Example: send Hi to Bob");

        out.flush();
    }
}
