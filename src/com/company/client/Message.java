package com.company.client;

import java.io.Serializable;

/**
 * Class message represents data sharing format between clients
 * Created by S1mpler on 12/1/2016.
 */
public class Message implements Serializable {
    ///////////////////////////////////////////////////////////////////////////
    // Properties
    ///////////////////////////////////////////////////////////////////////////
    private String sender;
    private String text;

    /**
     * Constructor
     * @param sender Username of the client that sends the message.
     * @param text Text of the message that client sends.
     */
    public Message(String sender, String text) {
        this.sender = sender;
        this.text = text;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Getters
    ///////////////////////////////////////////////////////////////////////////
    public String getText() {
        return text;
    }

    public String getSender() {
        return sender;
    }
}
