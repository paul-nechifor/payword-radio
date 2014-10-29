package net.nechifor.payword_radio.net;

import java.io.File;
import java.io.Serializable;
import java.net.Socket;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Scanner;
import javax.xml.bind.DatatypeConverter;
import org.w3c.dom.Element;
import net.nechifor.payword_radio.logic.Certificate;
import net.nechifor.payword_radio.logic.PayWordChain;
import net.nechifor.payword_radio.util.RSA;
import net.nechifor.payword_radio.logic.SignedCertificate;
import net.nechifor.payword_radio.logic.SignedCommitment;
import net.nechifor.payword_radio.util.Util;

public class Broker extends Listener
{
    private File saveFile;
    private HashMap<String, UserInfo> users;
    private HashMap<String, VendorInfo> vendors;
    private RSAPrivateKey privateKey;
    private RSAPublicKey publicKey;

    public Broker(int port, File saveFile)
    {
        super(port);
        this.saveFile = saveFile;

        if (saveFile.exists())
        {
            deserialize();
        }
        else
        {
            users = new HashMap<String, UserInfo>();
            vendors = new HashMap<String, VendorInfo>();
            KeyPair keyPair = RSA.generateKeyPair(1024);
            privateKey = (RSAPrivateKey) keyPair.getPrivate();
            publicKey = (RSAPublicKey) keyPair.getPublic();
        }
    }

    public void start()
    {
        Thread thread = new Thread(new Runnable(){public void run()
        {
            runServer();
        }});
        thread.start();

        Scanner in = new Scanner(System.in);
        in.nextLine();

        serialize();
        System.exit(0);
    }

    @Override
    public void receivedMessage(Message message, Socket socket)
    {
        if (message.type().equals("register"))
            onRegister(message, socket);
        else if (message.type().equals("registerVendor"))
            onRegisterVendor(message, socket);
        else if (message.type().equals("transferMoney"))
            onTransferMoney(message, socket);

        System.out.println("\n\nMessage: " + message.type());
        System.out.println(message);

        Util.closeSocket(socket);
    }

    private void onRegister(Message message, Socket socket)
    {
        Certificate c = new Certificate();
        c.broker = startedOn;
        c.user = message.get("name");
        c.deliveryAddress = message.get("deliveryAddress");
        c.userPublicKey = RSA.publicKeyFromBytes(
                DatatypeConverter.parseBase64Binary(message.get("publicKey")));
        c.expirationDate = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.DATE, 30);
        c.expirationDate = calendar.getTime();


        SignedCertificate sc = new SignedCertificate();
        sc.certificate = c;
        sc.generateSignature(privateKey);
        
        UserInfo u = new UserInfo();
        u.name = message.get("name");
        u.creditCard = message.get("creditCard");
        u.signedCertificate = sc;
        u.sum = 0.0;
        users.put(u.name, u);

        Message response = new Message("registerInfo");
        Element sce = sc.toElement(response.xml.getOwnerDocument());
        response.xml.appendChild(sce);
        response.writeToSocket(socket);
    }

    private void onRegisterVendor(Message message, Socket socket)
    {
        VendorInfo v = new VendorInfo();
        v.bankAccount = message.get("bankAccount");
        v.sum = 0.0;
        vendors.put(v.bankAccount, v);

        Message response = new Message("brockerInfo");
        response.set("publicKey",
                DatatypeConverter.printBase64Binary(publicKey.getEncoded()));
        response.writeToSocket(socket);
    }

    private void onTransferMoney(Message message, Socket socket)
    {
        String bankAccount = message.get("bankAccount");
        int lastIndex = Integer.parseInt(message.get("lastIndex"));
        String lastWord = message.get("lastWord");
        SignedCommitment sc = new SignedCommitment(
                (Element) message.xml.getFirstChild());
        SignedCertificate sCert = sc.commitment.userCertificate;
        String wordZero = sc.commitment.wordZero;

        String errorMessage = "";

        UserInfo u = users.get(sCert.certificate.user);
        VendorInfo v = vendors.get(bankAccount);

        if (!u.signedCertificate.equals(sCert))
            errorMessage = "Certificate isn't issued by me.";

        if (!sc.verifySignature(sCert.certificate.userPublicKey))
            errorMessage = "Commitment isn't valid.";

        if (!PayWordChain.verifyDistance(lastWord, wordZero, lastIndex))
            errorMessage = "PayWord chain isn't valid.";

        // Add one grace day.
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(sCert.certificate.expirationDate);
        calendar.add(Calendar.DATE, 1);
        Date date = calendar.getTime();

        if (date.before(new Date()))
            errorMessage = "The certificate is expired.";

        Message response = new Message("transferResponse");

        if (errorMessage.length() > 0)
        {
            response.set("success", "false");
            response.set("reason", errorMessage);
            System.out.println(errorMessage);
        }
        else
        {
            double extract = lastIndex / 100.0;
            System.out.printf("Transfered %.2f from '%s' to '%s'\n", extract,
                    u.creditCard, bankAccount);

            u.sum -= extract;
            System.out.printf("Total sum on %s's credit card: " +
                    "%.2f\n", u.name, u.sum);

            v.sum += extract;
            System.out.printf("Total sum on vendor's bank account: " +
                    "%.2f\n", v.sum);

            response.set("success", "true");
        }

        response.writeToSocket(socket);
    }

    private void deserialize()
    {
        Object[] objects = Util.deserialize(saveFile, 4);
        users = (HashMap<String, UserInfo>) objects[0];
        vendors = (HashMap<String, VendorInfo>) objects[1];
        privateKey = (RSAPrivateKey) objects[2];
        publicKey = (RSAPublicKey) objects[3];
    }

    private void serialize()
    {
        Util.serialize(new Object[] {users, vendors, privateKey, publicKey},
                saveFile);
    }
}

class UserInfo implements Serializable
{
    public String name;
    public String creditCard;
    public SignedCertificate signedCertificate;
    public double sum;
}

class VendorInfo implements Serializable
{
    public String bankAccount;
    public double sum;
}
