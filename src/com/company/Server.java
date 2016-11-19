package com.company;
import java.io.*;
import java.net.*;


/**
 * Created by e_voe_000 on 11/18/2016.
 */
public class Server {

    public static void main(String[] args) {
        ServerSocket serverSocket = null;


        Socket clientSocket = null;
        ObjectInputStream in = null;
        ObjectOutputStream out = null;

        String line;

        try {
            serverSocket = new ServerSocket(1500);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            clientSocket = serverSocket.accept();
            in = new ObjectInputStream(clientSocket.getInputStream());
            out = new ObjectOutputStream(clientSocket.getOutputStream());

            while (true) {
                line = ((String) in.readObject());
                System.out.println(line);
                out.writeObject(new String("message was " + line));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            System.out.println("class not found");
        }
    }



}
