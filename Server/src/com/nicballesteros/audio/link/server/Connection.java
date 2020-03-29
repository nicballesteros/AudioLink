package com.nicballesteros.audio.link.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class Connection implements Runnable{
    private Socket client;

    public Connection(Socket client) {
        this.client = client;
    }

    @Override
    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            String line = "";
            while(line != null) {
                line = in.readLine();
                if(line != null) {
                    System.out.println(line);
                }
            }
            System.out.println("Stopping thread");
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
