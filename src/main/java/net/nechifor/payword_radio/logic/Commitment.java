package net.nechifor.payword_radio.logic;

import java.net.InetSocketAddress;
import java.util.Date;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import net.nechifor.payword_radio.util.Util;

public class Commitment
{
    public InetSocketAddress vendor;
    public SignedCertificate userCertificate;
    public String wordZero;
    public Date date;

    public Commitment()
    {
    }

    public Commitment(Element e)
    {
        vendor = Util.stringToAddress(e.getAttribute("vendor"));
        userCertificate = new SignedCertificate((Element) e.getFirstChild());
        wordZero = e.getAttribute("wordZero");
        date = new Date(Long.parseLong(e.getAttribute("date")));
    }

    public Element toElement(Document d)
    {
        Element e = d.createElement("commitment");
        e.setAttribute("vendor", Util.addressToString(vendor));
        e.setAttribute("wordZero", wordZero);
        e.setAttribute("date", new Long(date.getTime()).toString());
        e.appendChild(userCertificate.toElement(d));
        return e;
    }
}
