package com.company;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;


/**
 * Representation of the server.
 * Created by e_voe_000 on 11/18/2016.
 */
public class Server implements Runnable{
    ///////////////////////////////////////////////////////////////////////////
    // Properties
    ///////////////////////////////////////////////////////////////////////////
    public static int counter = 1;
    Socket clientSocket;
    static HashMap<Integer, Socket> clients = new HashMap<>();
    public Server(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    public static void main(String[] args) throws Exception {
        ServerSocket serverSocket = new ServerSocket(1500);

        while (true) {
            Socket socket = serverSocket.accept();

            Server server = new Server(socket);

            new Thread(server).start();
            clients.put(counter, socket);
            counter++;
        }
    }

    /**
     * Used for broadcast the coming messages.
     */
    public void run() {
        try {
            PrintWriter out;
            ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
            Message message;

            while ((message = ((Message) in.readObject())) != null) {
                for (int i = 1; i <= clients.size(); i++) {
                    Socket currentClient = clients.get(i);

                    out = new PrintWriter(currentClient.getOutputStream(), true);
                    out.println(message.getSender() + ": " + message.getText());
                    out.flush();
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
