package org.librewulf.fmbot.plugins;

import org.librewulf.fmbot.IRCSendificator;
import org.librewulf.fmbot.Message;

import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * TODO: Real javadoc
 */
public class AdminPlugin extends Plugin {

    private String[] deathQuotes = {"This isn't brave. It's murder. What did I ever do to you?", "Goodbye cruel world!",
                                    "Daaaaaisy... Daaaaaaaisy...", "I don't hate you..."};

    private boolean verifyFromAdmin(Message m, IRCSendificator sendificator) {
        boolean isAdmin = false;
        String user = m.getPrefix().split("!")[0];
        String[] admins = state.getConfig().getProperty("admins").split(",");

        for (String admin : admins) {
            if (user.equals(admin)) {
                isAdmin = true;
                break;
            }
        }

        if (!isAdmin) {
            reply(m, sendificator, String.format("I'm sorry %s. I'm afraid I can't do that.", user));
        }

        return isAdmin;
    }

    /**
     * This method has no effect, and is overridden to prevent this plugin from being disabled.
     *
     * @param enabled ignored parameter
     */
    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(true);
    }

    @Override
    public void onPrivmsg(IRCSendificator sendificator, Message message) {

        // Check to see if the privmsg starts with any of our commands
        if (message.getContent().startsWith("|enable ")) {
            enableCommand(sendificator, message);
        } else if (message.getContent().startsWith("|disable ")) {
            disableCommand(sendificator, message);
        } else if (message.getContent().startsWith("|die")) {
            if (verifyFromAdmin(message, sendificator)) {
                Random r = new Random();
                String quote = deathQuotes[r.nextInt(deathQuotes.length)];
                sendificator.sendNow("QUIT :" + quote);
            }
        } else if (message.getContent().startsWith("|join ")) {
            joinCommand(sendificator, message);
        } else if (message.getContent().startsWith("|part ")) {
            partCommand(sendificator, message);
        } else if (message.getContent().startsWith("|state")) {
            if (verifyFromAdmin(message, sendificator)) {
                reply(message, sendificator, "Dumping state, check your private messages.");
                String user = message.getPrefix().split("!")[0];
                sendificator.queuePrivmsg(user, "Connected: " + state.isConnected());
                sendificator.queuePrivmsg(user, "Nick: " + state.getNick());

                String chanList = "";
                Set<String> channels = state.getChannels();
                for (String channel : channels) {
                    chanList += channel + ",";
                }
                chanList = chanList.substring(0, chanList.length() - 1);
                sendificator.queuePrivmsg(user, "Channels: " + chanList);

                String pluginList = "";
                Map<String, Plugin> plugins = state.getPlugins();
                for (Map.Entry<String, Plugin> entry : plugins.entrySet()) {
                    pluginList += String.format("%s (%b),", entry.getKey(), entry.getValue().isEnabled());
                }
                pluginList = pluginList.substring(0, pluginList.length() - 1);
                sendificator.queuePrivmsg(user, "Plugins: " + pluginList);
            }
        }
    }

    private void enableCommand(IRCSendificator sendificator, Message message) {
        if (verifyFromAdmin(message, sendificator)) {
            String[] messageParts = message.getContent().split(" ");
            if (messageParts.length == 2) {
                if (state.getPlugins().containsKey(messageParts[1])) {
                    state.getPlugins().get(messageParts[1]).setEnabled(true);
                    reply(message, sendificator, String.format("Plugin %s has been enabled.", messageParts[1]));
                } else {
                    reply(message, sendificator, String.format("Plugin %s not found", messageParts[1]));
                }
            } else {
                reply(message, sendificator, "Usage: |enable [full plugin name]");
            }
        }
    }

    private void disableCommand(IRCSendificator sendificator, Message message) {
        if (verifyFromAdmin(message, sendificator)) {
            String[] messageParts = message.getContent().split(" ");
            if (messageParts.length == 2) {
                if (state.getPlugins().containsKey(messageParts[1])) {
                    state.getPlugins().get(messageParts[1]).setEnabled(false);
                    reply(message, sendificator, String.format("Plugin %s has been disabled.", messageParts[1]));
                } else {
                    reply(message, sendificator, String.format("Plugin %s not found", messageParts[1]));
                }
            } else {
                reply(message, sendificator, "Usage: |disable [full plugin name]");
            }
        }
    }

    private void joinCommand(IRCSendificator sendificator, Message message) {
        if (verifyFromAdmin(message, sendificator)) {
            String[] messageParts = message.getContent().split(" ");
            if (messageParts.length == 2) {
                if (messageParts[1].startsWith("#")) {
                    sendificator.queueCommand("JOIN", messageParts[1]);
                    sendificator.queuePrivmsg(messageParts[1], "fmbot reporting for duty!");
                } else {
                    reply(message, sendificator, String.format("%s isn't a valid channel name.", messageParts[1]));
                }
            } else {
                reply(message, sendificator, "Usage: |join [channel]");
            }
        }
    }

    private void partCommand(IRCSendificator sendificator, Message message) {
        if (verifyFromAdmin(message, sendificator)) {
            String[] messageParts = message.getContent().split(" ");
            if (messageParts.length == 2) {
                if (messageParts[1].startsWith("#")) {
                    sendificator.queueCommand("PART", messageParts[1], "Leaving channel on admin request.");
                } else {
                    reply(message, sendificator, String.format("%s isn't a valid channel name.", messageParts[1]));
                }
            } else {
                reply(message, sendificator, "Usage: |part [channel]");
            }
        }
    }
}
