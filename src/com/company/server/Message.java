package com.company.server;

public class Message {
    public enum MessageType {
        HELO,   // login
        UL,     // list all users
        GL,     // list all groups
        GLU,    // list the groups of a user
        GU,     // list the members of a group
        GM,     // private message to a group
        ADMIN,  // show admin of the group
        PM,     // private message to a user
        BCST,   // broadcast a message to every user
        CG,     // create a group
        JOIN,   // join a group
        LEAVE,  // leave a group
        KICK,   // kick a user from the group (admin)
        BAN,    // ban a user in the group (admin) TODO fix : nothing happened
        UNBAN,  // unban a user in the group (admin)
        FILE,   // send a file to a user
        ACCEPT, // accept a file from the user
        REJECT, // reject a file from the user
        HELP,   // list all commands
        QUIT,   // quit the chat
        UNKOWN  // internal command (not for user)
    }

    private String line;
    private String target = null;

    public Message(String line) {
        this.line = line;
    }

    /**
     * Parses the first word in the message in an attempt to get the message type.
     *
     * @return Return a message type if it can be parsed correctly or UKNOWN if
     * the message type cannot be derived.
     */
    public MessageType getMessageType() {
        MessageType result = MessageType.UNKOWN;
        try {
            if (line != null && line.length() > 0) {
                String[] splits = line.split("\\s+");
                result = MessageType.valueOf(splits[0]);
            }
        } catch (IllegalArgumentException iaex) {
            System.out.println("[ERROR] Unknown command");
        }
        return result;
    }

    /**
     * Gets the payload of the message. This is interpreted as the raw message line
     * without the message type.
     *
     * @return Returns the raw line minus the message type. If the message type is
     * unkown then raw line is returned.
     */
    public String getPayload() { // TODO: 11/29/17 Change to switch

        switch (getMessageType()) {
            case UNKOWN:
                return line;
            case FILE:
                return line.split(" ")[2];
            case KICK:
                return line.split(" ")[2];
            case BAN:
                return line.split(" ")[2];
            case UNBAN:
                return line.split(" ")[2];
            default:
                // Return an empty string if the raw line was null or
                // the length of the line is smaller than the message type plus one (this
                // should prevent index out of bounds in the substring).
                if (line == null || line.length() < getMessageType().name().length() + 1) {
                    return "";
                }
                // Return the part after the message type (excluding whitespace).
                if (target != null) {
                    return line.substring(getMessageType().name().length() + 1 + target.length() + 1);
                } else {
                    return line.substring(getMessageType().name().length() + 1);
                }
        }
    }

    /**
     * @return Returns the target string.
     */
    public String getTarget() {
        if (getMessageType().equals(MessageType.PM)
                || getMessageType().equals(MessageType.HELO)
                || getMessageType().equals(MessageType.JOIN)
                || getMessageType().equals(MessageType.CG)
                || getMessageType().equals(MessageType.GM)
                || getMessageType().equals(MessageType.ADMIN)
                || getMessageType().equals(MessageType.LEAVE)
                || getMessageType().equals(MessageType.KICK)
                || getMessageType().equals(MessageType.BAN)
                || getMessageType().equals(MessageType.UNBAN)
                || getMessageType().equals(MessageType.GLU)
                || getMessageType().equals(MessageType.GU)
                || getMessageType().equals(MessageType.FILE)
                || getMessageType().equals(MessageType.ACCEPT)
                || getMessageType().equals(MessageType.REJECT)) {

            if (line == null || line.length() < getMessageType().name().length() + 1) {
                return "";
            }
            this.target = line.split(" ")[1];
            return this.target;
        } else {
            return null;
        }
    }
}
