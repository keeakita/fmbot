package org.librewulf.fmbot.plugins;

import org.apache.commons.codec.binary.Base64;
import org.librewulf.fmbot.IRCSendificator;
import org.librewulf.fmbot.Message;

import java.util.regex.Pattern;

public class SaslPlugin extends Plugin {

    @Override
    public void onEnable() {
        if (state.getConfig().getProperty("sasl_user") == null ||
                state.getConfig().getProperty("sasl_pass") == null) {

            System.err.println("SASL config not found, disabling plugin.");
            this.setEnabled(false);
        }
    }

    @Override
    public void onConnect(IRCSendificator sendificator) {
        sendificator.sendNow("CAP REQ :sasl");
    }

    // TODO: Putting everything in onRawMessage like this is shitty. Make
    // plugin have an onMessage that does IRC messages.
    @Override
    public void onRawMessage(IRCSendificator sendificator, String message) {
        if (Message.isIRCMessage(message)) {
            Message ircMessage = new Message(message);

            try {
                int code = Integer.parseInt(ircMessage.getCommand(), 10);
                if (code >= 904 && code <= 907) {
                    sendificator.queueRaw("CAP END");
                    // Job is done, no reason to keep checking messages
                    this.setEnabled(false);
                } else if (code == 900 || code == 903) {
                    sendificator.queueRaw("CAP END");
                    // Same as comment above
                    this.setEnabled(false);
                }
            } catch (NumberFormatException n) { }
        }

        String trimmed = message.trim();

        // TODO: once the message rewrite happens you can use onPrivmsg for this
        if (Pattern.matches("^:[^\\s]+ CAP [^\\s]+ ACK :sasl$", trimmed)) {

            sendificator.queueRaw("AUTHENTICATE PLAIN");

        } else if (trimmed.equals("AUTHENTICATE +")) {
            String user = state.getConfig().getProperty("sasl_user");
            String pass = state.getConfig().getProperty("sasl_pass");
            String auth = user + '\0' + user + '\0' + pass;

            sendificator.sendNow("AUTHENTICATE " +
                    Base64.encodeBase64String(auth.getBytes())); }
    }
}
