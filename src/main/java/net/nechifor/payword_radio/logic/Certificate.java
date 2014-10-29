package net.nechifor.payword_radio.logic;

import net.nechifor.payword_radio.util.RSA;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import javax.xml.bind.DatatypeConverter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import net.nechifor.payword_radio.util.Util;

public class Certificate implements Serializable
{
    public InetSocketAddress broker;
    public String user;
    public String deliveryAddress;
    public RSAPublicKey userPublicKey;
    public Date expirationDate;
    /* other info */

    public Certificate()
    {
    }

    public Certificate(Element e)
    {
        broker = Util.stringToAddress(e.getAttribute("broker"));
        user = e.getAttribute("user");
        deliveryAddress = e.getAttribute("deliveryAddress");
        userPublicKey = RSA.publicKeyFromBytes(
                DatatypeConverter.parseBase64Binary(
                    e.getAttribute("userPublicKey")));
        expirationDate =
                new Date(Long.parseLong(e.getAttribute("expirationDate")));
    }

    public Element toElement(Document d)
    {
        Element e = d.createElement("certificate");
        e.setAttribute("broker", Util.addressToString(broker));
        e.setAttribute("user", user);
        e.setAttribute("deliveryAddress", deliveryAddress);
        e.setAttribute("userPublicKey",DatatypeConverter.printBase64Binary(
                userPublicKey.getEncoded()));
        e.setAttribute("expirationDate",
                new Long(expirationDate.getTime()).toString());
        return e;
    }
}
