package net.nechifor.payword_radio.logic;

import net.nechifor.payword_radio.util.RSA;
import java.io.Serializable;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import net.nechifor.payword_radio.util.Util;

public class SignedCertificate implements Serializable
{
    public Certificate certificate;
    public String signature;

    public SignedCertificate()
    {
    }

    public SignedCertificate(Element e)
    {
        signature = e.getAttribute("signature");
        certificate = new Certificate((Element) e.getFirstChild());
    }
    
    public Element toElement(Document d)
    {
        Element e = d.createElement("signedCertificate");
        e.setAttribute("signature", signature);
        e.appendChild(certificate.toElement(d));
        return e;
    }

    public void generateSignature(RSAPrivateKey key)
    {
        Document d = Util.emptyDocument();
        byte[] bytes = Util.cannonicalXml(certificate.toElement(d)).getBytes();
        signature = RSA.sign(bytes, key);
    }

    public boolean verifySignature(RSAPublicKey key)
    {
        Document d = Util.emptyDocument();
        byte[] bytes = Util.cannonicalXml(certificate.toElement(d)).getBytes();
        return RSA.verify(bytes, signature, key);
    }

    @Override
    public boolean equals(Object o)
    {
        SignedCertificate sc = (SignedCertificate) o;

        return signature.equals(sc.signature);
    }
}
