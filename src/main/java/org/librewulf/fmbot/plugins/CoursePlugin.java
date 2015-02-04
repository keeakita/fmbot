package org.librewulf.fmbot.plugins;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.librewulf.fmbot.IRCSendificator;
import org.librewulf.fmbot.Message;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * This plugin fetches OSU course information from BuckeyeLink and displays it.
 *
 * USAGE:
 * |course [FULL SUBJECT NAME] [COURSE NUMBER]
 */
public class CoursePlugin extends Plugin {

    // Why Oracle why
    private static final String COURSE_SEARCH_URI = "https://courses.erp.ohio-state" +
            ".edu/psc/csosuct/PUBLIC/PUB/c/COMMUNITY_ACCESS.OSR_CAT_SRCH.GBL?PortalActualURL=https%3a%2f%2fcourses" +
            ".erp.ohio-state.edu%2fpsc%2fcsosuct%2fPUBLIC%2fPUB%2fc%2fCOMMUNITY_ACCESS.OSR_CAT_SRCH" +
            ".GBL&PortalContentURL=https%3a%2f%2fcourses.erp.ohio-state" +
            ".edu%2fpsc%2fcsosuct%2fPUBLIC%2fPUB%2fc%2fCOMMUNITY_ACCESS.OSR_CAT_SRCH" +
            ".GBL&PortalContentProvider=PUB&PortalCRefLabel=Course%20Catalog%20Search&PortalRegistryName=PUBLIC" +
            "&PortalServletURI=https%3a%2f%2fportal.erp.ohio-state" +
            ".edu%2fpsp%2fihosuct%2f&PortalURI=https%3a%2f%2fportal.erp.ohio-state" +
            ".edu%2fpsc%2fihosuct%2f&PortalHostNode=PUBLH&NoCrumbs=yes&PortalKeyStruct=yes";

    private static final String COURSE_POST_URI = "https://courses.erp.ohio-state" +
            ".edu/psc/csosuct/PUBLIC/PUB/c/COMMUNITY_ACCESS.OSR_CAT_SRCH.GBL";

    @Override
    public void onPrivmsg(IRCSendificator sendificator, Message message) {
        String[] msgContents = message.getContent().split(" ");
        if (msgContents[0].equals("|course")) {

            if (msgContents.length < 3) {
                reply(message, sendificator, "Usage: |course <full subject name> <number>");
                return;
            }

            // TODO: Better way to do this?
            String courseNumber = msgContents[msgContents.length - 1];
            String subject = StringUtils.join(Arrays.copyOfRange(msgContents, 1, msgContents.length - 1), " ");

            try {
                CloseableHttpClient httpClient = HttpClients.createDefault();
                HttpClientContext context = HttpClientContext.create(); // A place to store our "session"

                // Do a GET for our cookie and ICSID. This may not work, but you have to risk it to get the biscuit.
                HttpGet getReq = new HttpGet();
                getReq.setURI(new URI(COURSE_SEARCH_URI));

                getReq.setConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.BROWSER_COMPATIBILITY).build()); //ugh

                CloseableHttpResponse resp = httpClient.execute(getReq, context);

                // We have to get some ID out of the page. Since PS gives invalid XML we have to clean it.
                HtmlCleaner hc = new HtmlCleaner();
                TagNode tn = hc.clean(resp.getEntity().getContent());
                TagNode hidden_input = tn.findElementByAttValue("id", "ICSID", true, true);
                String icsid = hidden_input.getAttributes().get("value");

                // Prepare our POST
                HttpPost endpoint = new HttpPost();
                endpoint.setURI(new URI(COURSE_POST_URI));

                endpoint.setConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.BROWSER_COMPATIBILITY).build()); //ugh

                // Don't ask about all this, I have no clue
                List<NameValuePair> params = new ArrayList<>();
                params.add(new BasicNameValuePair("ICAJAX", "1"));
                params.add(new BasicNameValuePair("ICNAVTYPEDROPDOWN", "0"));
                params.add(new BasicNameValuePair("ICType", "Panel"));
                params.add(new BasicNameValuePair("ICElementNum", "0"));
                params.add(new BasicNameValuePair("ICStateNum", "1"));
                params.add(new BasicNameValuePair("ICAction", "OSR_CAT_SRCH_WK_BUTTON1"));
                params.add(new BasicNameValuePair("ICXPos", "0"));
                params.add(new BasicNameValuePair("ICYPos", "0"));
                params.add(new BasicNameValuePair("ResponseDiffFrame", "-1"));
                params.add(new BasicNameValuePair("TargetFrameName", "None"));
                params.add(new BasicNameValuePair("FacetPath", "None"));
                params.add(new BasicNameValuePair("ICFocus", ""));
                params.add(new BasicNameValuePair("ICSaveWarningFilter", "0"));
                params.add(new BasicNameValuePair("ICChanged", "-1"));
                params.add(new BasicNameValuePair("ICResubmit", "0"));
                params.add(new BasicNameValuePair("ICActionPrompt", "false"));
                params.add(new BasicNameValuePair("ICFind", ""));
                params.add(new BasicNameValuePair("ICAddCount", ""));
                params.add(new BasicNameValuePair("ICAPPCLSDATA", ""));

                params.add(new BasicNameValuePair("ICSID", icsid)); // The ID from earlier
                params.add(new BasicNameValuePair("OSR_CAT_SRCH_WK_DESCR", subject));
                params.add(new BasicNameValuePair("OSR_CAT_SRCH_WK_CATALOG_NBR", courseNumber));

                // Give it a go
                endpoint.setEntity(new UrlEncodedFormEntity(params));
                resp = httpClient.execute(endpoint, context);

                // Now the result is HTML embedded in XML CDATA (I know...)

                // Unless we shove this XML into a Document, XPATH gives errors. No clue.
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                Document doc = db.parse(resp.getEntity().getContent());

                XPath xpath = XPathFactory.newInstance().newXPath();
                XPathExpression expr = xpath.compile("/PAGE/FIELD[@id=\"win0divPAGECONTAINER\"]/text()");
                String htmlStr = expr.evaluate(doc);

                // Clean and parse the inner HTML. I want to get off Mr. Ellison's Wild Ride.
                tn = hc.clean(htmlStr);
                TagNode textArea = tn.findElementByAttValue("id", "OSR_CAT_SRCH_DESCRLONG$0", true, true);
                String description = textArea.getText().toString();

                String trimmedDesc = StringUtils.replaceChars(description, '\n', ' ');

                reply(message, sendificator, trimmedDesc);

            } catch (Exception e) {
                System.err.println("Error fetching the course info: " + e.getMessage());
                reply(message, sendificator, "Sorry, something seems to have gone wrong.");
            }
        }

    }
}
