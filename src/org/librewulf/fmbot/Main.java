package org.librewulf.fmbot;

import org.librewulf.fmbot.plugins.Plugin;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Properties;

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

        // Load plugins
        ClassLoader loader = ClassLoader.getSystemClassLoader();
        String[] pluginNames = config.getProperty("plugins").split(",");
        // A map containing Plugin instances by name
        HashMap<String, Plugin> plugins = new HashMap<>();

        for (String pluginName : pluginNames) {
            try {
                Class plugin = loader.loadClass(pluginName);
                plugins.put(pluginName, (Plugin) plugin.newInstance());
            } catch (ClassNotFoundException eNoClass) {
                System.err.println("Plugin " + pluginName + " not found, check that it exists and is in the " +
                        "CLASSPATH.");
            } catch (IllegalAccessException eIllegalAccess) {
                System.err.println("IllegalAccessException instantiating " + pluginName + ", make sure the "
                        + "constructor is accessible.");
            } catch (InstantiationException eInstance) {
                System.err.println("Error instantiating " + pluginName + ", is there a zero argument constructor?");
            }
        }

        // Initialize state
        BotState state = new BotState(config, plugins);
        for (Plugin p : plugins.values()) {
            p.initState(state);
        }

        // Load ignored nicks string into array
        String[] ignoreNicks = config.getProperty("ignore").split(",");


        // All plugins have been loaded, run the initialize methods and start threads
        for (Plugin p : plugins.values()) {
            p.onInitialize();
            p.setEnabled(true);
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

            // We should be connected now
            state.setConnected(true);

            // Instantiate our Sendificator
            IRCSendificator sendificator = new IRCSendificator(sock.getOutputStream());

            // onConnect
            for (Plugin p : plugins.values()) {
                if (p.isEnabled()) {
                    p.onConnect(sendificator);
                }
            }

            // Tell the server about us
            String[] nicks = config.getProperty("nick").split(",");
            int nickTries = 1;
            sendificator.sendNow("NICK " + nicks[0]);
            sendificator.sendNow("USER " + nicks[0] + " +iw * :" + config.getProperty("realname"));
            state.setNick(nicks[0]);

            String line;
            while ((line = in.readLine()) != null) {
                System.out.println(line);

                // onRawMessage
                for (Plugin p : plugins.values()) {
                    if (p.isEnabled()) {
                        p.onRawMessage(sendificator, line);
                    }
                }

                // Handle the PING
                if (line.indexOf("PING") == 0) {
                    sendificator.pong(line.split(" ")[1]);
                } else {
                    // TODO: Make sure it can be put in a message then onIRCMessage
                    Message m = new Message(line);

                    // 443 means we can't use this nick, attempt to fall back
                    if (m.getCommand().equals("433")) {
                        if (nickTries < nicks.length) {
                            System.err.println(String.format("Nick %s is already taken, falling back to backup %s",
                                    nicks[nickTries - 1], nicks[nickTries]));

                            sendificator.sendNow("NICK " + nicks[nickTries]);
                            state.setNick(nicks[nickTries]);
                            nickTries++;
                        } else {
                            // Abort, we have no more nicks to try
                            System.err.println("All specified nicks are taken. Aborting.");
                            sendificator.sendNow("QUIT :Out of nicknames");
                        }
                    }

                    // Connect once MOTD is over
                    if (m.getCommand().equals("376")) {
                        // Register with NickServ
                        if (config.containsKey("nickserv_pass")) {
                            sendificator.queuePrivmsg("NickServ", "identify " + config.getProperty("nickserv_pass"));
                        }

                        String[] channels = config.getProperty("channels").split(",");
                        for (String channel : channels) {
                            sendificator.queueCommand("JOIN", channel);
                            sendificator.queuePrivmsg(channel, state.getNick() + " reporting for duty!");
                        }

                        // onEndOfMOTD
                        for (Plugin p : plugins.values()) {
                            if (p.isEnabled()) {
                                p.onEndOfMOTD(sendificator, m);
                            }
                        }
                    } else if (m.getCommand().equals("PRIVMSG")) {
                        // Should we ignore this request?
                        String senderNick = m.getSource().split("!")[0];

                        boolean ignore = false;
                        for (String nick : ignoreNicks) {
                            if (senderNick.equals(nick)) {
                                ignore = true;
                            }
                        }

                        if (!ignore) {
                            // onPrivmsg
                            for (Plugin p : plugins.values()) {
                                if (p.isEnabled()) {
                                    p.onPrivmsg(sendificator, m);
                                }
                            }
                        }
                    } else if (m.getCommand().equals("JOIN")) {
                        // Was it the bot that joined?
                        if (m.getSource().split("!")[0].equals(state.getNick())) {
                            state.addChannel(m.getDestination());
                        }

                        for (Plugin p : plugins.values()) {
                            p.onUserJoin(sendificator, m);
                        }
                    } else if (m.getCommand().equals("PART")) {
                        // Was it the bot that left?
                        if (m.getSource().split("!")[0].equals(state.getNick())) {
                            state.removeChannel(m.getDestination());
                        }

                        for (Plugin p : plugins.values()) {
                            p.onUserLeave(sendificator, m);
                        }
                    } else if ( m.getCommand().equals("QUIT")) {
                        // Was it the bot that quit?
                        if (m.getSource().split("!")[0].equals(state.getNick())) {
                            state.setConnected(false);
                            // We're disconnected, disable our plugins so they have a chance to clean up
                            for (Plugin p : plugins.values()) {
                                p.setEnabled(false);
                            }

                            System.exit(0);
                        }

                        for (Plugin p : plugins.values()) {
                            p.onUserLeave(sendificator, m);
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Unknown IOException while using socket: " + e);
            System.exit(500);
        }
    }
}
