package org.librewulf.fmbot;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MessageTest {

    // Test an initial NOTICE message we get
    @Test
    public void testConstructorNotice() {
        Message m = new Message(":leguin.freenode.net NOTICE * " +
                ":*** Looking up your hostname...\r\n");

        assertEquals("leguin.freenode.net", m.getPrefix());
        assertEquals("NOTICE", m.getCommand());
        assertEquals("*", m.getDestination());
        assertEquals("*** Looking up your hostname...", m.getContent());
    }


    /*
     * Test for a message with no : before it's content, but one later. In this
     * case, 005 ISUPPORT.
     */
    @Test
    public void testConstructorISUPPORT() {
        Message m = new Message(":leguin.freenode.net 005 lwbot CHANTYPES=# " +
                "EXCEPTS INVEX CHANMODES=eIbq,k,flj,CFLMPQScgimnprstz " +
                "CHANLIMIT=#:120 PREFIX=(ov)@+ MAXLIST=bqeI:100 MODES=4 " +
                "NETWORK=freenode KNOCK STATUSMSG=@+ CALLERID=g :are " +
                "supported by this server\r\n");

        assertEquals("leguin.freenode.net", m.getPrefix());
        assertEquals("005", m.getCommand());
        assertEquals("lwbot", m.getDestination());
        assertEquals("CHANTYPES=# " +
                "EXCEPTS INVEX CHANMODES=eIbq,k,flj,CFLMPQScgimnprstz " +
                "CHANLIMIT=#:120 PREFIX=(ov)@+ MAXLIST=bqeI:100 MODES=4 " +
                "NETWORK=freenode KNOCK STATUSMSG=@+ CALLERID=g are " +
                "supported by this server", m.getContent());
    }


    /*
     * Test for the End of Message of The Day message. This is important to
     * get right because we use it to know when to start sending commands.
     */
    @Test
    public void testConstructorENDOFMOTD() {
        Message m = new Message(":leguin.freenode.net 376 lwbot :End of " +
                "/MOTD command.\r\n");
        assertEquals("leguin.freenode.net", m.getPrefix());
        assertEquals("376", m.getCommand());
        assertEquals("lwbot", m.getDestination());
        assertEquals("End of /MOTD command.", m.getContent());
    }


    /*
     * Test for MODE message. This is an example of a command where the sender
     * is not the server, and in this case is our own name.
     */
    @Test
    public void testConstructorMODE() {
        Message m = new Message(":lwbot MODE lwbot :+i\r\n");
        assertEquals("lwbot", m.getPrefix());
        assertEquals("MODE", m.getCommand());
        assertEquals("lwbot", m.getDestination());
        assertEquals("+i", m.getContent());
    }

    /*
     * Test for a JOIN message. This shows a message where there are only 3
     * feilds, content not being one of them.
     */
    @Test
    public void testConstructorJOIN() {
        Message m = new Message(
            ":lwbot!~lwbot@d118-75-29-164.try.wideopenwest.com JOIN " +
            "#osuosc\r\n");

        assertEquals("lwbot!~lwbot@d118-75-29-164.try.wideopenwest.com",
                m.getPrefix());
        assertEquals("JOIN", m.getCommand());
        assertEquals("#osuosc", m.getDestination());
        assertEquals("", m.getContent());
    }

    /*
     * Test for a PRIVMSG. This is one of the most important messages, since
     * it's how our bot will interact with others.
     */
    @Test
    public void testConstructorPRIVMSG() {
        Message m = new Message(
            ":bsilvereagle!~bsilverea@osuosc/bsilvereagle PRIVMSG #osuosc " +
            ":Wil Wheaton is a mortal enemy!!\r\n");

        assertEquals("bsilvereagle!~bsilverea@osuosc/bsilvereagle",
                m.getPrefix());
        assertEquals("PRIVMSG", m.getCommand());
        assertEquals("#osuosc", m.getDestination());
        assertEquals("Wil Wheaton is a mortal enemy!!", m.getContent());
    }
}
