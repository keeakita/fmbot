package org.librewulf.fmbot;

import java.io.*;
import java.net.Socket;
import java.util.Properties;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

public class Main {

    private static Properties config;

    /**
     * Main function
     */
    public static void main(String[] args) {
        // Read our config file

        try {
            FileReader configFile = new FileReader("data/config.properties");
            config = new Properties();
            config.load(configFile);
        } catch (FileNotFoundException e) {
            System.err.println("Config file not found, please see the README.");
            System.exit(404);
        } catch (IOException e) {
            System.err.println("Unknown IOException while opening config file: " + e.getMessage());
            System.exit(500);
        }

        // Create a socket
        SocketFactory factory;

        if (config.getProperty("ssl").equals("True")) {
            factory = SSLSocketFactory.getDefault();
        } else {
            factory = SocketFactory.getDefault();
        }

        // Connect
        try {
            Socket sock = factory.createSocket(config.getProperty("address"),
                    Integer.parseInt(config.getProperty("port")));

            BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));

            // Instantiate our Sendificator
            IRCSendificator sendificator = new IRCSendificator(sock.getOutputStream());

            (new Thread(sendificator)).start();

            // Tell the server about us
            sendificator.sendNow("NICK " + config.getProperty("nick"));
            sendificator.sendNow("USER " + config.getProperty("nick") + " +iw * :" + config.getProperty("realname"));

            String line;
            while ((line = in.readLine()) != null) {
                System.out.println(line);
                // Handle the PING
                if (line.indexOf("PING") == 0) {
                    sendificator.pong(line.split(" ")[1]);
                } else {
                    Message m = new Message(line);

                    // Connect once MOTD is over
                    if (m.getCommand().equals("376")) {
                        onReadyForCommands(sendificator);
                    }

                    // beep
                    if (m.getCommand().equals("PRIVMSG") && m.getContent().startsWith("|beep")) {
                        reply(sendificator, m, "Beep boop.");
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Unknown IOException while using socket: " + e);
            System.exit(500);
        }
    }

    public static void onReadyForCommands(IRCSendificator sendificator) {
        // Register with NickServ
        if (config.containsKey("nickserv_pass")) {
            sendificator.queuePrivmsg("NickServ", "identify " + config.getProperty("nickserv_pass"));
        }

        String[] channels = config.getProperty("channels").split(",");
        for (String channel : channels) {
            sendificator.queueCommand("JOIN", channel);
            sendificator.queuePrivmsg(channel, config.getProperty("nick") + " reporting for duty!");
        }
    }

    public static void reply(IRCSendificator sendificator, Message source, String reply) {
        assert source.getCommand().equals("PRIVMSG");

        if (source.getDestination().startsWith("#")) {
            // It's a channel
            sendificator.queuePrivmsg(source.getDestination(), reply);
        } else {
            // It's a user
            String user = source.getSource().split("!")[0];
            sendificator.queuePrivmsg(user, reply);
        }
    }
}
