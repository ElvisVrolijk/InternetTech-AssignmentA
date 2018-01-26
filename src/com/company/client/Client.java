package com.company.client;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Representation of the client.
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

    //
    private final static Timer resendMessage = new Timer();
    private String lastMsg = "";
    private boolean msgSent = false;
    private boolean gotResponse = true;
    private boolean curruptMessage = false;

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
                lastMsg = text;
                sendMessage(writer, text);

                msgSent = true;

                resendMessage(writer);
            }
        }
    }

    private void sendMessage(PrintWriter writer, String text) {
        writer.println(checkMessage(text));
        writer.flush();
    }

    private void resendMessage(PrintWriter writer) {
        if (msgSent) {
            resendMessage.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (!gotResponse) {
                        // resend message if did not get response
                        sendMessage(writer, lastMsg);
                    }
                }
            }, 5000);
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
                processResponse(responseLine);

                gotResponse = true;

                checkRespose(responseLine);

                if (responseLine.equals("+OK Goodbye")) {
                    close = true;
                }
            }

        } catch (IOException e) {
            // TODO: 11/22/17  implement
        }
    }

    private void checkRespose(String response) {
        // used to get the response status.
        String[] split = response.split(" ");
        String status = split[0];

        // used to get the las command sent
        String[] split2 = lastMsg.split(" ");
        String command = split2[0];

        // if the commands HELO or BCST were used, check for corrupted response.
        if (command.equals("HELO") || command.equals("BCST")) {
            switch (status) {
                case "+OK":
                    lastMsg = "";
                    break;
                case "-ERR":
                    lastMsg = "";
                    break;
                default:
                    System.out.println("This message is corrupted!");
                    lastMsg = "";
                    break;
            }
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


    private String checkMessage(String fullMessage) {
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
                        } else if (split.length == 2) {
                            String[] tempArray = new String[3];
                            for (int i = 0; i < 2; i++) {
                                tempArray[i] = split[i];
                            }
                            tempArray[2] = encryption.getPublicKey();
                            split = tempArray;
                        }
                        break;
                    case "PM":
                        if (split.length > 2) {
                            String toUserName = split[1];
                            String message = "";
                            for (int i = 2; i < split.length; i++) {
                                message = message + " " + split[i];
                            }

                            String[] tempArray = new String[3];
                            for (int i = 0; i < 2; i++) {
                                tempArray[i] = split[i];
                            }
                            split = tempArray;

                            split[2] = encryption.encrypt(toUserName, message);
                        } else if (split.length == 2) {
                            String[] tempArray = new String[3];
                            for (int i = 0; i < 2; i++) {
                                tempArray[i] = split[i];
                            }
                            tempArray[2] = "";
                            split = tempArray;
                        }
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
