package com.solegendary.reignofnether.p2p;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.annotation.Nullable;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Asks the home router (UPnP IGD) to forward a port to this computer. Plain stdlib: SSDP search, the router's
 * description XML, then SOAP calls. Many routers have it off; callers treat null/false as "not available".
 */
public final class Upnp {

    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    private static final Pattern LOCATION = Pattern.compile("(?im)^location:\\s*(\\S+)");

    private final URI control;
    private final String service;
    private final String localIp;

    private Upnp(URI control, String service, String localIp) {
        this.control = control;
        this.service = service;
        this.localIp = localIp;
    }

    /** Finds the router, or null after ~2 s. */
    @Nullable
    public static Upnp discover() {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(700);
            InetSocketAddress group = new InetSocketAddress("239.255.255.250", 1900);
            for (String st : new String[]{ "urn:schemas-upnp-org:device:InternetGatewayDevice:1", "urn:schemas-upnp-org:device:InternetGatewayDevice:2" }) {
                byte[] msg = ("M-SEARCH * HTTP/1.1\r\nHOST: 239.255.255.250:1900\r\nST: " + st
                        + "\r\nMAN: \"ssdp:discover\"\r\nMX: 1\r\n\r\n").getBytes(StandardCharsets.US_ASCII);
                socket.send(new DatagramPacket(msg, msg.length, group));
            }
            byte[] buf = new byte[2048];
            long end = System.currentTimeMillis() + 2000;
            while (System.currentTimeMillis() < end) {
                DatagramPacket p = new DatagramPacket(buf, buf.length);
                try {
                    socket.receive(p);
                } catch (SocketTimeoutException e) {
                    continue;
                }
                Matcher m = LOCATION.matcher(new String(buf, 0, p.getLength(), StandardCharsets.US_ASCII));
                if (m.find()) {
                    Upnp upnp = fromDescription(URI.create(m.group(1)));
                    if (upnp != null)
                        return upnp;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    @Nullable
    private static Upnp fromDescription(URI location) throws Exception {
        String xml = HTTP.send(HttpRequest.newBuilder(location).timeout(Duration.ofSeconds(2)).build(),
                HttpResponse.BodyHandlers.ofString()).body();
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(true);
        f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true); // no XXE from a router
        f.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        Document doc = f.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        NodeList services = doc.getElementsByTagNameNS("*", "service");
        for (int i = 0; i < services.getLength(); i++) {
            Element s = (Element) services.item(i);
            String type = text(s, "serviceType");
            if (type.contains("WANIPConnection") || type.contains("WANPPPConnection")) {
                URI control = location.resolve(text(s, "controlURL"));
                // our address as the router sees it
                try (DatagramSocket probe = new DatagramSocket()) {
                    probe.connect(InetAddress.getByName(location.getHost()), location.getPort() > 0 ? location.getPort() : 80);
                    return new Upnp(control, type, probe.getLocalAddress().getHostAddress());
                }
            }
        }
        return null;
    }

    private static String text(Element e, String tag) {
        NodeList n = e.getElementsByTagNameNS("*", tag);
        return n.getLength() > 0 ? n.item(0).getTextContent().trim() : "";
    }

    /** Forwards `port` on the router to the same port here. TCP or UDP. */
    public boolean map(int port, String protocol) {
        String args = "<NewRemoteHost></NewRemoteHost><NewExternalPort>" + port + "</NewExternalPort><NewProtocol>" + protocol
                + "</NewProtocol><NewInternalPort>" + port + "</NewInternalPort><NewInternalClient>" + localIp
                + "</NewInternalClient><NewEnabled>1</NewEnabled><NewPortMappingDescription>Reign of Nether</NewPortMappingDescription>";
        // a lease, so a crash doesn't leave it open forever; some routers only take permanent ones
        return soap("AddPortMapping", args + "<NewLeaseDuration>7200</NewLeaseDuration>") != null
                || soap("AddPortMapping", args + "<NewLeaseDuration>0</NewLeaseDuration>") != null;
    }

    public void unmap(int port, String protocol) {
        soap("DeletePortMapping", "<NewRemoteHost></NewRemoteHost><NewExternalPort>" + port
                + "</NewExternalPort><NewProtocol>" + protocol + "</NewProtocol>");
    }

    @Nullable
    public InetAddress externalIp() {
        String body = soap("GetExternalIPAddress", "");
        if (body == null)
            return null;
        Matcher m = Pattern.compile("<NewExternalIPAddress>([0-9.]+)</NewExternalIPAddress>").matcher(body);
        try {
            return m.find() ? InetAddress.getByName(m.group(1)) : null;
        } catch (Exception e) {
            return null;
        }
    }

    /** The response body, or null on any failure. */
    @Nullable
    private String soap(String action, String args) {
        String body = "<?xml version=\"1.0\"?><s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"><s:Body><u:" + action + " xmlns:u=\""
                + service + "\">" + args + "</u:" + action + "></s:Body></s:Envelope>";
        try {
            HttpResponse<String> r = HTTP.send(HttpRequest.newBuilder(control).timeout(Duration.ofSeconds(3))
                    .header("Content-Type", "text/xml; charset=\"utf-8\"")
                    .header("SOAPAction", "\"" + service + "#" + action + "\"")
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
            return r.statusCode() == 200 ? r.body() : null;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return "UPnP " + control.getHost() + " (" + service.toLowerCase(Locale.ROOT) + ")";
    }
}
