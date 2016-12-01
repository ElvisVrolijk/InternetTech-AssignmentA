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
    private static ObjectOutputStream out = null;

    private static BufferedReader inputLine = null;

    private static boolean close = false;

    private Scanner scanner = new Scanner(System.in);
    ///////////////////////////////////////////////////////////////////////////
    // Properties
    ///////////////////////////////////////////////////////////////////////////
    private String username;


    public static void main(String[] args) {
        try {
            socket = new Socket("localhost", 1500);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new DataInputStream(socket.getInputStream());
            inputLine = new BufferedReader(new InputStreamReader(System.in));
        } catch (UnknownHostException e) {
            System.out.println("Don't know about host: localhost");
        } catch (IOException e) {
            e.printStackTrace();
        }

        //handle input
        try {
            new Client().proccesMessage(socket, out);

            in.close();
            out.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Used for writing messages
     * @param socket Socket of the current client
     * @param out Output stream
     * @throws IOException
     */
    private void proccesMessage(Socket socket, ObjectOutputStream out)  throws IOException {
        if (socket != null && out != null) {

            setUsername(); // wait for username setting

            new Thread(new Client()).start();
            String text;
            while (!close) {
                text = inputLine.readLine();
                out.writeObject(new Message(username, text));
                out.flush();
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Used for setting the user's name
     */
    private void setUsername() {
        System.out.print("Enter username: ");
        username = scanner.nextLine();
    }
}
