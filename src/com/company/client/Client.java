package com.company.client;

import sun.nio.cs.ext.MS874;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Map;

/**
 * Representation of the client.
 *
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
    private static Encryption encryption = Encryption.getInstance();
    //Port number
    private final static int PORT = 1337;
    //map of public keys

    /**
     * Main function.
     *
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

                writer.println(sendMessage(text));
                writer.flush();
            }
        }
    }

    /**
     * Asynchronously listens server messages
     *
     * @deprecated Use BufferedReader.readLine();
     */
    public void run() {
        String responseLine;
        try {
            while ((responseLine = in.readLine()) != null) {
                if (responseLine.equals("+OK Goodbye")) {
                    break;
                }
                processResponse(responseLine);
            }
            close = true;
        } catch (IOException e) {
            // TODO: 11/22/17  implement
        }
    }

    private void processResponse(String message) {
        boolean isPublicKeyMsg = false;
        String[] split = message.split(" ");
        String type = split[0];

        switch (type) {
            case "PK":
                isPublicKeyMsg = true;
                encryption.processPublicKeys(split[1], split[2]);
                break;
            case "PM":
                split[2] = encryption.decrypt(split[2]);
                break;
            default:
                break;
        }

        if (!isPublicKeyMsg) {
            String msg = Arrays.toString(split);
            msg = msg.substring(1, msg.length() - 1).replace(",", "");
            System.out.println(msg);
        }
    }


    private String sendMessage(String fullMessage) {
        if (fullMessage.contains(" ")) {
            String[] split = fullMessage.split(" ");
            if (split.length > 1) {
                String type = split[0];

                switch (type) {
                    case "HELO":
                        if (split.length > 2) {
                            String[] tempArray = new String[3];
                            for (int i = 0; i < 3; i++) {
                                tempArray[i] = split[i];
                            }
                            tempArray[2] = encryption.getPublicKey();
                            split = tempArray;
                        } else if (split.length == 2){
                            String[] tempArray = new String[3];
                            for (int i = 0; i < 2; i++) {
                                tempArray[i] = split[i];
                            }
                            tempArray[2] = encryption.getPublicKey();
                            split = tempArray;
                        }
                        break;
                    case "PM":
                        String toUserName = split[1];
                        String message = split[2];
                        split[2] = encryption.encrypt(toUserName, message);
                        break;
                    default:
                        break;
                }

                String msg = Arrays.toString(split);
                msg = msg.substring(1, msg.length() - 1).replace(",", "");

                return msg;
            } else {
                return fullMessage;
            }
        } else {
            return fullMessage;
        }
    }
}
