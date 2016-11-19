package com.company;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Created by e_voe_000 on 11/18/2016.
 */
public class Client {

    public static void main(String[] args) {
        Socket socket = null;

        ObjectInputStream in = null;
        ObjectOutputStream out = null;

        try {
            socket = new Socket("localhost", 1500);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
        } catch (UnknownHostException e) {
            System.out.println("Don't know about host: locahost");
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (socket != null && out != null && in != null) {
            try {
                out.writeObject("hello");

                String responseLine;

                while ((responseLine = (String) in.readObject()) != null) {
                    System.out.println("Server: " + responseLine);
                }

                out.close();
                in.close();
                socket.close();

            } catch (UnknownHostException e) {
                System.out.println("Trying to connect to unknown host: " + e);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                System.out.println("class not found");
            }

        }

    }
}
