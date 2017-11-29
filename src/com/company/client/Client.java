package com.company.client;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;

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

    //Port number
    private final static int PORT = 1337;

    /**
     * Main function.
     * @param args Process environment variables/
     */
    public static void main(String[] args) {
        try {
            socket = new Socket("localhost", PORT);
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
            client.processMessage(socket, out);

            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    ///////////////////////////////////////////////////////////////////////////
    // Methods
    ///////////////////////////////////////////////////////////////////////////

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
     *  Asynchronously listens server messages
     *  @deprecated Use BufferedReader.readLine();
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
            // TODO: 11/22/17  implement
        }
    }
}
