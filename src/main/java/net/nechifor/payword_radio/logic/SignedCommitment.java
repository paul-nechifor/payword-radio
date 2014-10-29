package net.nechifor.payword_radio.logic;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import net.nechifor.payword_radio.util.RSA;
import net.nechifor.payword_radio.util.Util;

public class SignedCommitment
{
    public Commitment commitment;
    public String signature;

    public SignedCommitment()
    {
    }

    public SignedCommitment(Element e)
    {
        signature = e.getAttribute("signature");
        commitment = new Commitment((Element) e.getFirstChild());
    }

    public Element toElement(Document d)
    {
        Element e = d.createElement("signedCommitment");
        e.setAttribute("signature", signature);
        e.appendChild(commitment.toElement(d));
        return e;
    }

    public void generateSignature(RSAPrivateKey key)
    {
        Document d = Util.emptyDocument();
        byte[] bytes = Util.cannonicalXml(commitment.toElement(d)).getBytes();
        signature = RSA.sign(bytes, key);
    }

    public boolean verifySignature(RSAPublicKey key)
    {
        Document d = Util.emptyDocument();
        byte[] bytes = Util.cannonicalXml(commitment.toElement(d)).getBytes();
        return RSA.verify(bytes, signature, key);
    }
}
