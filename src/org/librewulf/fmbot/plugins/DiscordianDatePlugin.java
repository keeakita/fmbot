package org.librewulf.fmbot.plugins;

import org.librewulf.fmbot.IRCSendificator;
import org.librewulf.fmbot.Message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * TODO: Real Javadoc
 */
public class DiscordianDatePlugin extends Plugin {
    public DiscordianDatePlugin() {
    }

    @Override
    public void onPrivmsg(IRCSendificator sendificator, Message message) {
        if (message.getContent().startsWith("|ddate")) {
            try {
                Process ddate = Runtime.getRuntime().exec("ddate");
                BufferedReader dIn = new BufferedReader(new InputStreamReader(ddate.getInputStream()));
                String date = dIn.readLine();
                dIn.close();

                reply(message, sendificator, date);

            } catch (IOException e) {
                reply(message, sendificator, "Something went wrong while checking the date. Hail Eris.");
                System.err.println("Unable to exec ddate: " + e.getMessage());
            }
        }
    }
}
