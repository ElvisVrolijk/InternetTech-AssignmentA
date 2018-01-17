package com.company.server;

import java.io.*;
import java.net.Socket;

/**
 * Created by s1mpler on 1/17/18.
 */
public class FileTransfer implements Runnable {

    private Server.ClientThread sender;
    private Server.ClientThread receiver;
    private Socket socket;
    private InputStream in;
    private OutputStream out;

    public FileTransfer(Socket socket, Server.ClientThread sender, Server.ClientThread receiver) {
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

    public Server.ClientThread getSender() {
        return sender;
    }

    public Server.ClientThread getReceiver() {
        return receiver;
    }

    public Socket getSocket() {
        return socket;
    }

    public InputStream getIn() {
        return in;
    }

    public OutputStream getOut() {
        return out;
    }

    public void setReceiver(Server.ClientThread receiver) {
        this.receiver = receiver;
    }
}
