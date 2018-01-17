package com.company.server;

import java.io.*;
import java.net.Socket;

/**
 * Created by s1mpler on 1/17/18.
 */
public class FileTransfer implements Runnable {

    private Socket sender;
    private Socket receiver;
    private Socket socket;
    private InputStream in;
    private OutputStream out;

    public FileTransfer(Socket socket, Socket sender, Socket receiver) {
        this.socket = socket;
        this.sender = sender;
        this.receiver = receiver;

        try {
            this.in = this.socket.getInputStream();
            this.out = this.socket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {

    }

    public void setReceiver(Socket receiver) {
        this.receiver = receiver;
    }
}
