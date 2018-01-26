package com.company.server;

import java.io.*;
import java.net.Socket;

/**
 * Created by s1mpler on 1/17/18.
 */
public class FileTransfer implements Runnable {

    ///////////////////////////////////////////////////////////////////////////
    // Properties
    ///////////////////////////////////////////////////////////////////////////

    private DataInputStream senderStream;
    private DataOutputStream receiverStream;

    private boolean done;

    /**
     * Constructor
     * @param senderStream Sender's data output stream.
     * @param receiverStream Receiver's data input stream
     */
    public FileTransfer(DataInputStream senderStream, DataOutputStream receiverStream) {
        this.senderStream = senderStream;
        this.receiverStream = receiverStream;

        this.done = false;
    }

    public FileTransfer() {
    }

    @Override
    public void run() {
        int read = 0;
        try {
            while((read = senderStream.read()) > 0) {
                receiverStream.write(read);
            }



        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Getters
    ///////////////////////////////////////////////////////////////////////////

    public boolean isDone() {
        return done;
    }

    public DataInputStream getSenderStream() {
        return senderStream;
    }

    public DataOutputStream getReceiverStream() {
        return receiverStream;
    }

    public void setSenderStream(DataInputStream senderStream) {
        this.senderStream = senderStream;
    }

    public void setReceiverStream(DataOutputStream receiverStream) {
        this.receiverStream = receiverStream;
    }
}
