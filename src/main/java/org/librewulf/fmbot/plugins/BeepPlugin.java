package org.librewulf.fmbot.plugins;

import org.librewulf.fmbot.IRCSendificator;
import org.librewulf.fmbot.Message;

import java.util.Random;

public class BeepPlugin extends Plugin {

    @Override
    public void onPrivmsg(IRCSendificator sendificator, Message message) {
        if (message.getContent().startsWith("|beep")) {

            Random r = new Random();
            String beepStr = "";

            // How many beeps?
            int beeps = r.nextInt(5) + 1;

            while (beeps > 0) {
                if (r.nextBoolean()) {
                    beepStr += "beep";
                } else {
                    beepStr += "boop";
                }

                if (beeps > 1) {
                    beepStr += " ";
                }

                beeps--;
            }

            // How many !s?
            int bangs = r.nextInt(3) + 1;

            while (bangs > 0) {
                beepStr += "!";
                bangs--;
            }

            // Capitalize the first letter
            beepStr = beepStr.substring(0, 1).toUpperCase() + beepStr.substring(1);

            reply(message, sendificator, beepStr);
        }
    }

}
