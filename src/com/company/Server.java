package com.company;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by e_voe_000 on 11/18/2016.
 */
public class Server {

    public static int counter = 0;
    private ServerSocket serverSocket;
    ///////////////////////////////////////////////////////////////////////////
    // Properties
    ///////////////////////////////////////////////////////////////////////////
    final List<ClientThread> clients = new ArrayList<>();

    public static void main(String[] args) {
        try {
            Server server = new Server();
            server.run();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Server() {
        try {
            serverSocket = new ServerSocket(1500);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() throws IOException {
        while (true) {
            Socket client = serverSocket.accept();
            ClientThread ct = new ClientThread(client);
            clients.add(ct);

            ct.start();
        }
    }

    private class ClientThread extends Thread {

        private ObjectInputStream in = null;
        private ObjectOutputStream out = null;

        private String line;

        private int id;

        public ClientThread(Socket socket) {

            this.id = ++counter;

            try {
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        public void run() {
            try {
                while (true) {
                    line = ((String) in.readObject());
                    System.out.println(line);

                    for (ClientThread client : clients) {
                        client.broadcastMessage(line);
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        private void broadcastMessage(String line) throws IOException{
            out.writeObject(line);
        }
    }


}
