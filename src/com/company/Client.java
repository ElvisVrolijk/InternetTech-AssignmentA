package com.company;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Created by e_voe_000 on 11/18/2016.
 */
public class Client implements Runnable {

    private static Socket socket = null;

    private static DataInputStream in = null;
    private static PrintStream out = null;

    private static BufferedReader inputLine = null;

    private static boolean close = false;

    public static void main(String[] args) {

        try {
            socket = new Socket("localhost", 1500);
            out = new PrintStream(socket.getOutputStream());
            in = new DataInputStream(socket.getInputStream());
            inputLine = new BufferedReader(new InputStreamReader(System.in));
        } catch (UnknownHostException e) {
            System.out.println("Don't know about host: localhost");
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (socket != null && out != null && in != null) {
            try {
                new Thread(new Client()).start();

                while (!close) {
                    out.println(inputLine.readLine());
                }

                out.close();
                in.close();
                socket.close();

            } catch (UnknownHostException e) {
                System.out.println("Trying to connect to unknown host: " + e);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }

    public void run() {

        String responseLine;
        try {
            while ((responseLine = in.readLine()) != null) {
                if (responseLine.equals("Exit")) {
                    break;
                }

                System.out.println(responseLine);
                
            }
            close = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
