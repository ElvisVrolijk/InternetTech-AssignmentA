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
    private static ServerSocket serverFileSocket;
    private static Set<ClientThread> threads;
    private static Set<Group> groups;
    private static Map<String, String> publicKeys;
    private static ArrayList<FileTransfer> fileTransactions;
    private static ServerConfiguration conf;


    public Server(ServerConfiguration conf) {
        this.conf = conf;
        this.groups = new HashSet<>();
        this.fileTransactions = new ArrayList<>();
        this.publicKeys = new HashMap<>();
    }

    /**
     * Runs the server. The server listens for incoming client connections
     * by opening a socket on a specific port.
     */
    public void run() {
        // Create a socket to wait for clients.
        try {
            serverSocket = new ServerSocket(conf.SERVER_PORT);
            serverFileSocket = new ServerSocket(conf.SERVER_FILE_PORT);
            threads = new HashSet<>();

            while (true) {
                // Wait for an incoming client-connection request (blocking).
                Socket socket = serverSocket.accept();


// TODO: 1/26/2018 at this point I need to get (somehow) file input stream a
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

    private static class FileSocketConnectorThread implements Runnable {
        Socket fileSocket;
        @Override
        public void run() {
            //Wait for an incoming file-connection request.
            try {
                fileSocket = serverFileSocket.accept();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Socket getFileSocket() {
            return fileSocket;
        }
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

        //file streams
        private DataOutputStream receiveStream;
        private DataInputStream sendStream;

        public ClientThread(Socket socket) {
            this.state = INIT;
            this.socket = socket;
        }

        public String getUsername() {
            return username;
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
                                            } else if (ct == this && ct.getUsername() != null) {
                                                userExists = true;
                                                break;
                                            }
                                        }
                                        if (userExists) {
//                                        state = FINISHED;
                                            writeToClient("-ERR user already logged in!");
                                        } else {
                                            state = CONNECTED;
                                            this.username = message.getTarget();
                                            publicKeys.put(getUsername(), message.getPayload());
                                            writeToClient("+OK " + getUsername());
                                            writeToClient(this.helpMessage());
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
                            case GU:
                                boolean groupFound = false;
                                for (Group group : groups) {
                                    if (message.getTarget() != null && group.getName().equals(message.getTarget())) {
                                        groupFound = true;
                                        for (ClientThread member : group.getMembers()) {
                                            if (group.isBanned(member)) {
                                                this.writeToClient("[" + member.getUsername() + "] (banned)");
                                            } else {
                                                this.writeToClient("[" + member.getUsername() + "]");
                                            }
                                        }
                                    }
                                }
                                if (!groupFound) {
                                    this.writeToClient("-ERR group " + message.getTarget() + " does NOT exist!");
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
                                            writeToClient("INFO user is not in any group!");
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
                                            userFound = true;
                                            if (message.getPayload().isEmpty()) {
                                                writeToClient("-ERR can't send empty message!");
                                            } else {
                                                ct.writeToClient("PM [" + getUsername() + "] " + message.getPayload());
                                                writeToClient("+OK");
                                            }
                                        }
                                    }

                                    if (!userFound) {
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
                                        writeToClient("-ERR The group [" + message.getTarget() + "] already exists!");
                                    }
                                    writeToClient("+OK");
                                }
                                break;
                            case ADMIN:
                                groupFound = false;
                                for (Group group : groups) {
                                    if (message.getTarget() != null && group.getName().equals(message.getTarget())) {
                                        groupFound = true;
                                        this.writeToClient("[" + group.getAdmin().getUsername() + "] is admin of " + group.getName());
                                    }
                                }
                                if (!groupFound) {
                                    this.writeToClient("-ERR group " + message.getTarget() + " does NOT exist!");
                                }
                                break;
                            case JOIN:
                                groupFound = false;
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
                            case GM:
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
                                                    writeToClient("-ERR you are banned in the group (" + message.getTarget() + ")!");
                                                }
                                            } else {
                                                writeToClient("-ERR you are not a member of the group (" + message.getTarget() + ")!");
                                            }
                                        }
                                    }
                                    if (!groupFound) {
                                        writeToClient("-ERR group not found!");
                                    }
                                }
                                break;
                            case KICK:
                                ClientThread toRemove = null;
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
                                                        toRemove = user;
                                                        writeToClient("+OK User was kicked");
                                                        user.writeToClient("INFO You are kicked from " + message.getTarget());
                                                    } else {
                                                        writeToClient("-ERR You are not the admin of this group");
                                                    }
                                                }
                                            }
                                            group.removeMember(toRemove);
                                        }
                                    }
                                    if (!groupFound) {
                                        this.writeToClient("-ERR group not found!");
                                    }
                                    if (!userFound) {
                                        this.writeToClient("-ERR user not found!");
                                    }
                                }
                                break;
                            case BAN:
                                groupFound = false;
                                userFound = false;
                                if (message.getTarget().isEmpty()) {
                                    writeToClient("-ERR empty group name is not allowed! (example: BAN groupName memberName)");
                                } else if (message.getPayload().isEmpty()) {
                                    writeToClient("-ERR empty member name is not allowed! (example: BAN groupName memberName)");
                                } else {
                                    for (Group group : groups) {
                                        if (group.getName().equals(message.getTarget())) {
                                            groupFound = true;
                                            for (ClientThread user : group.getMembers()) {
                                                if (user.getUsername().equals(message.getPayload())) {
                                                    userFound = true;
                                                    if (group.isAdmin(this)) {
                                                        group.banMember(user);
                                                        this.writeToClient("+OK user was banned");
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
                            case UNBAN:
                                groupFound = false;
                                userFound = false;
                                ClientThread toUnban = null;
                                if (message.getTarget().isEmpty()) {
                                    writeToClient("-ERR empty group name is not allowed! (example: BAN groupName userName)");
                                } else if (message.getPayload().isEmpty()) {
                                    writeToClient("-ERR empty user name is not allowed! (example: BAN groupName userName)");
                                } else {
                                    for (Group group : groups) {
                                        if (group.getName().equals(message.getTarget())) {
                                            groupFound = true;
                                            for (ClientThread user : group.getBanned()) {
                                                if (user.getUsername().equals(message.getPayload())) {
                                                    userFound = true;
                                                    if (group.isAdmin(this)) {
                                                        toUnban = user;
                                                        this.writeToClient("+OK user was unbanned");
                                                        user.writeToClient("INFO You are unbanned in " + message.getTarget());
                                                    } else {
                                                        writeToClient("-ERR You are not the admin of this group");
                                                    }
                                                }
                                            }
                                            group.unbanMember(toUnban);
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
                                this.writeToClient(helpMessage());
                                break;
                            case FILE:
                                for (ClientThread ct : threads) {
                                    if (ct.getUsername().equals(message.getTarget()) && ct != this) {
                                        ct.writeToClient("FILE " + message.getPayload() + " [" + getUsername() + "]: run a command:\nACCEPT " + getUsername() + " / REJECT " + getUsername());
                                        this.writeToClient("OPEN_CONNECTION");
                                        this.writeToClient("Waiting to be accepted...");
                                        //create a thread and wait until user will try to connect it's file socket
                                        FileSocketConnectorThread ftt = new FileSocketConnectorThread();
                                        while (ftt.getFileSocket() == null) {
                                            ftt.run();
                                        }
                                        this.sendStream = new DataInputStream(ftt.getFileSocket().getInputStream());
                                    }
                                }
                                break;
                            case ACCEPT:
                                userFound = false;
                                for (ClientThread ct : threads) {
                                    if (ct.getUsername().equals(message.getTarget()) && message.getTarget() != null) {
                                        userFound = true;
                                        this.writeToClient("OPEN_CONNECTION");
                                        FileSocketConnectorThread ftt = new FileSocketConnectorThread();
                                        while (ftt.getFileSocket() == null) {
                                            ftt.run();
                                        }

                                        this.receiveStream = new DataOutputStream(ftt.getFileSocket().getOutputStream());

                                        this.writeToClient("RECEIVING_FILE");

                                        FileTransfer fileTransfer = new FileTransfer(ct.sendStream, this.receiveStream);

                                        while (!fileTransfer.isDone()) {
                                            ct.writeToClient("SENDING_FILE");
                                            fileTransfer.run();
                                        }

                                        ct.writeToClient("FILE_SENT");
                                        this.writeToClient("FILE_RECEIVED");
                                    }
                                }
                                if (!userFound) {
                                    this.writeToClient("-ERR User " + message.getTarget() + " does NOT exist or did NOT send you a file!");
                                }
                                break;
                            case REJECT:
                                userFound = false;
                                for (ClientThread ct : threads) {
                                    if (ct.getUsername().equals(message.getTarget()) && message.getTarget() != null) {
                                        userFound = true;
                                        ct.sendStream = null;
                                        ct.writeToClient("INFO User " + getUsername() + " has rejected your file!");
                                        this.writeToClient("+OK You rejected a file from [" + ct.getUsername() + "]");
                                    }
                                }
                                if (!userFound) {
                                    this.writeToClient("-ERR User " + message.getTarget() + " does NOT exist or did NOT send you a file!");
                                }
                                break;
                            case QUIT:

                                for (Group group : groups) {
                                    if (group.getAdmin().equals(this)) {
                                        for (ClientThread member : group.getMembersExcept(this)) {
                                            member.writeToClient("INFO group " + group.getName() + " does NOT exist anymore! Admin [" + this.getUsername() + "] has left the chat!");
                                        }
                                        groups.remove(group);
                                    }
                                }

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
                System.out.println(this.getUsername() + " disconnected without saying goodbye! :(");
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

        public String helpMessage() {
            return
                    "UL, (Example UL) used to get all users in server!\n" +
                    "GL,(Example GL) used to get all groups in server!\n" +
                    "GLU,(Example GLU name) used to get all group that the user is in!\n" +
                    "GU,(Example GU groupName) used to get all members of the specific group!\n" +
                    "PM,(Example PM name message) used to get send a private message!\n" +
                    "BCST,(Example BCST message) used to send all users in server a message!\n" +
                    "FILE,(Example FILE name fileName) used to send a file to the specific user!\n" +
                    "CG,(Example CG name) used to create a group!\n" +
                    "GM,(Example GM groupName message) used to message all users in the group!\n" +
                    "ADMIN,(Example ADMIN groupName) used to get the admin of the specific group!\n" +
                    "JOIN,(Example JOIN groupName) used to join a group!\n" +
                    "LEAVE,(Example LEAVE groupName) used to leave a group!\n" +
                    "KICK,(Example KICK groupName name) used to kick a user out of a group!\n" +
                    "BAN,(Example BAN groupName name) used to ban a user from a group!\n" +
                    "UNBAN,(Example UNBAN groupName name) used to unban a user in a group!\n" +
                    "HELP,(Example HELP) used to get info for the key words!\n" +
                    "QUIT, (Example QUIT) used to leave the server";
        }
    }
}
