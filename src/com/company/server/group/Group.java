package com.company.server.group;

import com.company.client.Client;
import com.company.server.Server.ClientThread;

import java.util.ArrayList;
import java.util.List;

/**
 * Representation of a group.
 * Created by s1mpler on 11/29/17.
 */
public class Group {
    private String name;

    private ClientThread admin;

    private List<ClientThread> members;
    private List<ClientThread> banned;


    ///////////////////////////////////////////////////////////////////////////
    // Constructors
    ///////////////////////////////////////////////////////////////////////////

    public Group(String name, ClientThread admin) {
        this.members = new ArrayList<>();
        this.banned = new ArrayList<>();

        this.name = name;
        this.admin = admin;
        this.members.add(admin);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Methods
    ///////////////////////////////////////////////////////////////////////////

    public boolean isAdmin(ClientThread user) {
        return admin.equals(user);
    }

    public boolean addMember(ClientThread user) {
        if (!isBanned(user) && !members.contains(user)) {
            members.add(user);
            return true;
        }
        return false;
    }

    public void removeMember(ClientThread user) {
        if (members.contains(user)) {
            members.remove(user);
        }
    }

    public void banMember(ClientThread user) {
        removeMember(user);
        banned.add(user);
    }

    public void unbanMember(ClientThread user) {
        banned.remove(user);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Helpers
    ///////////////////////////////////////////////////////////////////////////

    public boolean isBanned(ClientThread user) {
        return banned.contains(user);
    }

    public boolean isMember(ClientThread user) {
        return members.contains(user);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Getters & Setters
    ///////////////////////////////////////////////////////////////////////////

    public String getName() {
        return name;
    }

    public List<ClientThread> getBanned() {
        return banned;
    }

    public List<ClientThread> getMembers() {
        return members;
    }

    public List<ClientThread> getMembersExcept(ClientThread one) {
        List<ClientThread> allExceptOne = new ArrayList<ClientThread>();
        for (ClientThread member : members) {
            if (member != one) {
                allExceptOne.add(member);
            }
        }
        return allExceptOne;
    }

    public ClientThread getAdmin() {
        return admin;
    }

    public void setName(String name) {
        this.name = name;
    }
}
