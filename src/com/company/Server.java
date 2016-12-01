package com.company;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;


/**
 * Created by e_voe_000 on 11/18/2016.
 */
public class Server implements Runnable{

    public static int counter = 1;
    Socket clientSocket;
    ///////////////////////////////////////////////////////////////////////////
    // Properties
    ///////////////////////////////////////////////////////////////////////////
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

    public void run() {
        try {
            PrintWriter out;
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String line;

            while ((line = in.readLine()) != null) {
                for(int i = 1; i <= clients.size(); i++) {
                    Socket currentClient = clients.get(i);

                    out = new PrintWriter(currentClient.getOutputStream(), true);
                    out.println(line);
                    out.flush();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
