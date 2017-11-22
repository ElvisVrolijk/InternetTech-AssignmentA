package com.company;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

/**
 * Representation of the client.
 * Created by e_voe_000 on 11/18/2016.
 */
public class Client implements Runnable {

    ///////////////////////////////////////////////////////////////////////////
    // Fields
    ///////////////////////////////////////////////////////////////////////////
    private static Socket socket = null;

    private static DataInputStream in = null;
    private static OutputStream out = null;

    private static BufferedReader inputLine = null;

    private static boolean close = false;

    private static Scanner scanner = new Scanner(System.in);
    ///////////////////////////////////////////////////////////////////////////
    // Properties
    ///////////////////////////////////////////////////////////////////////////
    private static String username;


    public static void main(String[] args) {
        try {
            socket = new Socket("localhost", 1337);
            out = socket.getOutputStream();
            in = new DataInputStream(socket.getInputStream());
            inputLine = new BufferedReader(new InputStreamReader(System.in));
        } catch (UnknownHostException e) {
            System.out.println("Don't know about host: localhost");
        } catch (IOException e) {
            e.printStackTrace();
        }

        //handle input
        try {
            Client client = new Client();
//            client.setUsername();
            client.processMessage(socket, out);

            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Used for writing messages
     *
     * @param socket Socket of the current client
     * @param out    Output stream
     * @throws IOException
     */
    private void processMessage(Socket socket, OutputStream out) throws IOException {
        if (socket != null && out != null) {
            PrintWriter writer = new PrintWriter(out);

            new Thread(this).start();
            String text;
            while (!close) {
                text = inputLine.readLine();
                if (text.equals("Exit")) {
                    break;
                }
                writer.println(text);
                writer.flush();
            }
        }
    }

    /**
     * Used for read messages
     */
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
        } catch (IOException ignored) {
            System.out.println(username + " disconnected!");
        }
    }

    /**
     * Used for setting the user's name
     */
//    private void setUsername() throws IOException {
//        System.out.print("Enter username: ");
//        username = scanner.nextLine();
//        out.writeObject("username: " + username);
//        out.flush();
//    }
}
