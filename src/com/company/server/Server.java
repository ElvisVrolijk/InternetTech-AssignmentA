package com.company.server;

import com.company.server.group.Group;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

import static com.company.server.ServerState.*;

// TODO: 1/17/18 remove user if it is disconnected
// TODO: 1/17/18 file transportation
// TODO: 1/17/18 encryption
// TODO: 1/17/18 message validation
public class Server {

    private ServerSocket serverSocket;
    private static Set<ClientThread> threads;
    private static Set<Group> groups;
    private static Map<String, String> publicKeys;
    private static Set<FileTransfer> fileTransfers;
    private static ServerConfiguration conf;


    public Server(ServerConfiguration conf) {
        this.conf = conf;
        this.groups = new HashSet<>();
        this.fileTransfers = new HashSet<>();
        this.publicKeys = new HashMap<>();
    }


    public ClientThread findClientByUsername(String username) {
        for (ClientThread ct : threads) {
            if (ct.getUsername().equals(username)) {
                return ct;
            }
        }
        return null;
    }

    /**
     * Runs the server. The server listens for incoming client connections
     * by opening a socket on a specific port.
     */
    public void run() {
        // Create a socket to wait for clients.
        try {
            serverSocket = new ServerSocket(conf.SERVER_PORT);
            threads = new HashSet<>();

            while (true) {
                // Wait for an incoming client-connection request (blocking).
                Socket socket = serverSocket.accept();

                // When a new connection has been established, start a new thread.
                ClientThread ct = new ClientThread(socket);
                threads.add(ct);
                new Thread(ct).start();
                System.out.println("Num clients: " + threads.size());

                // Simulate lost connections if configured.
                if (conf.doSimulateConnectionLost()) {
                    DropClientThread dct = new DropClientThread(ct);
                    new Thread(dct).start();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String helpMessage() {
        // TODO: 11/30/17 create HELP request to see all the commands.
        return "";
    }

    /**
     * This thread sleeps for somewhere between 10 to 20 seconds and then drops the
     * client thread. This is done to simulate a lost in connection.
     */
    private class DropClientThread implements Runnable {
        ClientThread ct;

        DropClientThread(ClientThread ct) {
            this.ct = ct;
        }

        public void run() {
            try {
                // Drop a client thread between 10 to 20 seconds.
                int sleep = (10 + new Random().nextInt(10)) * 1000;
                Thread.sleep(sleep);
                ct.kill();
                threads.remove(ct);
                System.out.println("Num clients: " + threads.size());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * This inner class is used to handle all communication between the server and a
     * specific client.
     */
    public static class ClientThread implements Runnable {

        private DataInputStream is;
        private OutputStream os;
        private Socket socket;
        private ServerState state;
        private String username;
        private FileTransfer fileSocket;

        public void setFileSocket(FileTransfer fileSocket) {
            this.fileSocket = fileSocket;
        }

        public ClientThread(Socket socket) {
            this.state = INIT;
            this.socket = socket;
        }

        public String getUsername() {
            return username;
        }

        public OutputStream getOutputStream() {
            return os;
        }

        public void run() {
            try {
                // Create input and output streams for the socket.
                os = socket.getOutputStream();
                is = new DataInputStream(socket.getInputStream());
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));

                // According to the protocol we should send HELO <welcome message>
                state = CONNECTING;
                String welcomeMessage = "HELO " + conf.WELCOME_MESSAGE;
                writeToClient(welcomeMessage);

                while (!state.equals(FINISHED)) {
                    // Wait for message from the client.
                    String line = reader.readLine();
                    if (line != null) {
                        // Log incoming message for debug purposes.
                        boolean isIncomingMessage = true;
                        logMessage(isIncomingMessage, line);

                        // Parse incoming message.
                        Message message = new Message(line);

                        boolean isEmptyPayload = message.getPayload().isEmpty();
                        boolean isEmptyTarget = message.getPayload().isEmpty();
                        // Process message.
                        switch (message.getMessageType()) {
                            case HELO:
                                // Check username format.
                                if (!isEmptyTarget) {
                                    boolean isValidUsername = message.getTarget().matches("[a-zA-Z0-9_]{3,14}");
                                    if (!isValidUsername) {
//                                    state = FINISHED;
                                        writeToClient("-ERR username has an invalid format (only characters, numbers and underscores are allowed)");
                                    } else {
                                        // Check if user already exists.
                                        boolean userExists = false;
                                        for (ClientThread ct : threads) {
                                            if (ct != this && message.getTarget().equals(ct.getUsername())) {
                                                userExists = true;
                                                break;
                                            }
                                        }
                                        if (userExists) {
//                                        state = FINISHED;
                                            writeToClient("-ERR user already logged in");
                                        } else {
                                            state = CONNECTED;
                                            this.username = message.getTarget();
                                            publicKeys.put(getUsername(), message.getPayload());
                                            writeToClient("+OK " + getUsername());
                                            for (ClientThread ct : threads) {
                                                if (ct != this) {
                                                    ct.writeToClient("PK " + getUsername() + " " + message.getPayload());
                                                }
                                            }

                                             for (Map.Entry<String, String> entry : publicKeys.entrySet()) {
                                                if (!Objects.equals(entry.getKey(), getUsername())) {
                                                    writeToClient("PK " + entry.getKey() + " " + entry.getValue());
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    writeToClient("-ERR username has an invalid format (only characters, numbers and underscores are allowed)");
                                }
                                break;
                            case UL:
                                if (!isEmptyPayload) {
                                    writeToClient("-ERR no extra commands needed! (example: GL");
                                } else {
                                    for (ClientThread ct : threads) {
                                        if (ct.getUsername() != null) {
                                            writeToClient("[" + ct.getUsername() + "]");
                                        }
                                    }
                                    writeToClient("+OK");
                                }
                                break;
                            case GL:
                                if (!isEmptyPayload) {
                                    writeToClient("-ERR no extra commands needed! (example: UL");
                                } else {
                                    for (Group group : groups) {
                                        writeToClient("[" + group.getName() + "]");
                                    }
                                    writeToClient("+OK");
                                }
                                break;
                            case GLU:
                                if (isEmptyPayload) {
                                    writeToClient("-ERR command needs a target! (example: GLU name");
                                } else {
                                    boolean exists = false;
                                    boolean inGroup = false;

                                    for (ClientThread ct : threads) {
                                        if (ct.getUsername() != null) {
                                            if (ct.getUsername().equals(message.getTarget())) {
                                                exists = true;
                                                for (Group group : groups) {
                                                    if (group.isMember(ct)) {
                                                        inGroup = true;
                                                        writeToClient("[" + group.getName() + "]");
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    if (exists) {
                                        if (!inGroup) {
                                            writeToClient("-ERR user is not in any group!");
                                        } else {
                                            writeToClient("+OK");
                                        }
                                    } else {
                                        writeToClient("-ERR user does not exists!");
                                    }
                                }
                                break;
                            case PM:
                                boolean userFound = false;
                                if (isEmptyTarget) {
                                    writeToClient("-ERR username needed! (example: PM name message)");
                                } else {
                                    for (ClientThread ct : threads) {
                                        if (ct.getUsername().equals(message.getTarget())) {
                                            if (message.getPayload().isEmpty()) {
                                                writeToClient("-ERR can't send empty message!");
                                            } else {
                                                ct.writeToClient("PM [" + getUsername() + "] " + message.getPayload());
                                                userFound = true;
                                            }
                                        }
                                    }
                                    if (userFound) {
                                        writeToClient("+OK");
                                    } else {
                                        writeToClient("-ERR user does not exists!");
                                    }
                                }
                                break;
                            case BCST:
                                if (message.getPayload().isEmpty()) {
                                    writeToClient("-ERR can't send empty broadcast!");
                                } else {
                                    // Broadcast to other clients.
                                    for (ClientThread ct : threads) {
                                        if (ct != this) {
                                            ct.writeToClient("BCST [" + getUsername() + "] " + message.getPayload());
                                        }
                                    }
                                    writeToClient("+OK");
                                }
                                break;
                            case CG:
                                boolean alreadyExists = false;
                                if (message.getTarget().isEmpty()) {
                                    writeToClient("-ERR group name needed! (example: CG name)");
                                } else {
                                    for (Group group : groups) {
                                        if (group.getName().equals(message.getTarget())) {
                                            alreadyExists = true;
                                        }
                                    }
                                    if (!alreadyExists) {
                                        groups.add(new Group(message.getTarget(), this));
                                        writeToClient("+OK Group [" + message.getTarget() + "] was created");
                                    } else {
                                        writeToClient("-ERR The group [" + message.getTarget() + "] already exists");
                                    }
                                    writeToClient("+OK");
                                }
                                break;
                            case JOIN:
                                boolean groupFound = false;
                                if (message.getTarget().isEmpty()) {
                                    writeToClient("-ERR can't join a group with no name! (example: JOIN name)");
                                } else {
                                    for (Group group : groups) {
                                        if (group.getName().equals(message.getTarget())) {
                                            groupFound = true;
                                            // check if user is not a member (banned)
                                            if (group.addMember(this)) {
                                                writeToClient("+OK You joined the group!");
                                            } else {
                                                if (group.isBanned(this)) {
                                                    writeToClient("-ERR you are banned!");
                                                } else if (group.isMember(this)) {
                                                    writeToClient("-ERR you are already in this group!");
                                                }
                                            }
                                        }
                                    }
                                    if (!groupFound) {
                                        writeToClient("-ERR group not found!");
                                    }
                                }
                                break;
                            case LEAVE:
                                groupFound = false;
                                if (message.getTarget().isEmpty()) {
                                    writeToClient("-ERR no group name edit! (example: LEAVE groupName)");
                                } else {
                                    for (Group group : groups) {
                                        if (group.getName().equals(message.getTarget())) {
                                            groupFound = true;
                                            if (group.isMember(this)) {
                                                group.removeMember(this);
                                                writeToClient("+OK You leave the group");
                                            }
                                        }
                                    }
                                    if (!groupFound) {
                                        writeToClient("-ERR group not found!");
                                    }
                                }
                                break;
                            case GROUP:
                                groupFound = false;
                                if (message.getTarget().isEmpty()) {
                                    writeToClient("-ERR no group name edit! (example: GROUP groupName message)");
                                } else if (message.getPayload().isEmpty()) {
                                    writeToClient("-ERR empty message is not aloud! (example: GROUP groupName message)");
                                } else {
                                    for (Group group : groups) {
                                        if (group.getName().equals(message.getTarget())) {
                                            groupFound = true;
                                            if (group.isMember(this)) {
                                                if (!group.isBanned(this)) {
                                                    for (ClientThread member : group.getMembersExcept(this)) {
                                                        member.writeToClient("GROUP (" + message.getTarget() + ") [" + getUsername() + "] " + message.getPayload());
                                                    }
                                                    writeToClient("+OK");
                                                } else {
                                                    writeToClient("-ERR you are banned from group '" + message.getTarget() + "'!");
                                                }
                                            } else {
                                                writeToClient("-ERR you are not a member of the group '" + message.getTarget() + "'!");
                                            }
                                        }
                                    }
                                    if (!groupFound) {
                                        writeToClient("-ERR group not found!");
                                    }
                                }
                                break;
                            case KICK:
                                groupFound = false;
                                userFound = false;
                                if (message.getTarget().isEmpty()) {
                                    writeToClient("-ERR no group name edit! (example: KICK groupName memberName)");
                                } else if (message.getPayload().isEmpty()) {
                                    writeToClient("-ERR empty member name is not aloud! (example: KICK groupName memberName)");
                                } else {
                                    for (Group group : groups) {
                                        if (group.getName().equals(message.getTarget())) {
                                            groupFound = true;
                                            for (ClientThread user : group.getMembers()) {
                                                if (user.getUsername().equals(message.getPayload())) {
                                                    userFound = true;
                                                    if (group.isAdmin(this)) {
                                                        group.removeMember(user);
                                                        writeToClient("+OK User was kicked");
                                                        user.writeToClient("INFO You are kicked from " + message.getTarget());
                                                    } else {
                                                        writeToClient("-ERR You are not the admin of this group");
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    if (!groupFound) {
                                        writeToClient("-ERR group not found!");
                                    }
                                    if (!userFound) {
                                        writeToClient("-ERR user not found!");
                                    }
                                }
                                break;
                            case BAN:
                                groupFound = false;
                                userFound = false;
                                if (message.getTarget().isEmpty()) {
                                    writeToClient("-ERR no group name edit! (example: KICK groupName memberName)");
                                } else if (message.getPayload().isEmpty()) {
                                    writeToClient("-ERR empty member name is not aloud! (example: KICK groupName memberName)");
                                } else {
                                    for (Group group : groups) {
                                        if (group.getName().equals(message.getTarget())) {
                                            groupFound = true;
                                            for (ClientThread user : group.getMembers()) {
                                                if (user.getUsername().equals(message.getPayload())) {
                                                    userFound = true;
                                                    if (group.isAdmin(this)) {
                                                        group.banMember(user);
                                                        writeToClient("+OK user was banned");
                                                        user.writeToClient("INFO You are banned in " + message.getTarget());
                                                    } else {
                                                        writeToClient("-ERR You are not the admin of this group");
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    if (!groupFound) {
                                        writeToClient("-ERR group not found!");
                                    }
                                    if (!userFound) {
                                        writeToClient("-ERR user not found!");
                                    }
                                }
                                break;
                            case HELP:
                                System.out.println("UL, (Example UL) used to get all users in server!\n" +
                                        "        GL,(Example GL) used to get all groups in server!\n" +
                                        "        GLU,(Example GLU name) used to get all group that the user is in!\n" +
                                        "        PM,(Example PM name message) used to get send a private message!\n" +
                                        "        BCST,(Example BCST message) used to send all users in server a message!\n" +
                                        "        CG,(Example CG name) used to create a group!\n" +
                                        "        JOIN,(Example JOIN groupName) used to join a group!\n" +
                                        "        LEAVE,(Example LEAVE groupName) used to leave a group!\n" +
                                        "        GROUP,(Example GROUP groupName message) used to message all users int he group!\n" +
                                        "        KICK,(Example KICK groupName name) used to kick a user out of a group!\n" +
                                        "        BAN,(Example BAN groupName name) used to ban a user from a group!\n" +
                                        "        HELP,(Example HELP) used to get info for the key words!\n" +
                                        "        QUIT, (Example QUIT) used to leave the server");
                                break;
                            case FILE:
                                fileSocket = new FileTransfer(new Socket(), this, null);
                                for (ClientThread ct : threads) {
                                    if (ct.getUsername().equals(message.getTarget()) && ct != this) {
                                        ct.writeToClient("FILE [" + getUsername() + "]: run command ACCEPT to start loading or REJECT to cancel");
                                        fileSocket.setReceiver(ct);
                                    }
                                }
                                break;
                            case ACCEPT:
                                for (FileTransfer ft : fileTransfers) {
                                    if (ft.getReceiver().getUsername().equals(this.getUsername())) {
                                        //this file is for you
                                        if (message.getTarget().equals(ft.getSender().getUsername())) {
                                            //this file is from the right person
                                            for (ClientThread ct : threads) {
                                                if (ct.getUsername().equals(message.getTarget()) && ct != this) {
                                                    ct.writeToClient("FILE [" + ct.getUsername() + "]: accepted");
                                                    fileSocket.setReceiver(ct);
                                                }
                                            }
                                        }
                                    }
                                }
                                break;
                            case REJECT:
                                break;
                            case QUIT:
                                // FIXME: 11/29/17 doesnt disconnect the user
                                // Close connection
                                state = FINISHED;
                                writeToClient("+OK Goodbye");
                                threads.remove(this);
                                break;
                            case UNKOWN:
                                // Unkown command has been sent
                                writeToClient("-ERR Unknown command");
                                break;
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                threads.remove(this);
            }
        }

        /**
         * An external process can stop the client using this method.
         */
        public void kill() {
            state = FINISHED;
        }

        /**
         * Write a message to this client thread.
         *
         * @param message The message to be sent to the (connected) client.
         */
        private void writeToClient(String message) {
            boolean shouldDropPacket = false;
            boolean shouldCorruptPacket = false;

            // Check if we need to behave badly by dropping some messages.
            if (conf.doSimulateDroppedPackets()) {
                // Randomly select if we are going to drop this message or not.
                int random = new Random().nextInt(6);
                if (random == 0) {
                    // Drop message.
                    shouldDropPacket = true;
                    System.out.println("[DROPPED] " + message);
                }
            }

            // Check if we need to behave badly by corrupting some messages.
            if (conf.doSimulateCorruptedPackets()) {
                // Randomly select if we are going to corrupt this message or not.
                int random = new Random().nextInt(4);
                if (random == 0) {
                    // Corrupt message.
                    shouldCorruptPacket = true;
                }
            }

            // Do the actual message sending here.
            if (!shouldDropPacket) {
                if (shouldCorruptPacket) {
                    message = corrupt(message);
                    System.out.println("[CORRUPT] " + message);
                }
                PrintWriter writer = new PrintWriter(os);
                writer.println(message);
                writer.flush();

                // Echo the message to the server console for debugging purposes.
                boolean isIncomingMessage = false;
                logMessage(isIncomingMessage, message);
            }
        }

        /**
         * This methods implements a (naive) simulation of a corrupt message by replacing
         * some charaters at random indexes with the charater X.
         *
         * @param message The message to be corrupted.
         * @return Returns the message with some charaters replaced with X's.
         */
        private String corrupt(String message) {
            Random random = new Random();
            int x = random.nextInt(4);
            char[] messageChars = message.toCharArray();

            while (x < messageChars.length) {
                messageChars[x] = 'X';
                x = random.nextInt(10);
            }

            return new String(messageChars);
        }

        /**
         * Util method to print (debug) information about the server's incoming and outgoing messages.
         *
         * @param isIncoming Indicates whether the message was an incoming message. If false then
         *                   an outgoing message is assumed.
         * @param message    The message received or sent.
         */
        private void logMessage(boolean isIncoming, String message) {
            String directionString = conf.CLI_COLOR_OUTGOING + ">> ";  // Outgoing message.
            if (isIncoming) {
                directionString = conf.CLI_COLOR_INCOMING + "<< ";     // Incoming message.
            }

            // Add username to log if present.
            // Note when setting up the connection the user is not known.
            if (getUsername() == null) {
                System.out.println(directionString + message + conf.RESET_CLI_COLORS);
            } else {
                System.out.println(directionString + "[" + getUsername() + "] " + message + conf.RESET_CLI_COLORS);
            }
        }
    }
}
