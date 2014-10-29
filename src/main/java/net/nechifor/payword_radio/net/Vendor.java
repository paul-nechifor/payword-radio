package net.nechifor.payword_radio.net;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.HashMap;
import javax.xml.bind.DatatypeConverter;
import org.w3c.dom.Element;
import net.nechifor.payword_radio.logic.Certificate;
import net.nechifor.payword_radio.logic.PayWordChain;
import net.nechifor.payword_radio.logic.SignedCommitment;
import net.nechifor.payword_radio.util.RSA;
import net.nechifor.payword_radio.util.Util;


public class Vendor extends Listener
{
    private String brokerAddress = "127.0.0.1:2000";
    private String bankAccount = "5234-5123123-234";
    private File saveFile;

    private RSAPublicKey brokerPublicKey;

    private int timeInterval = 5;
    private int[] centsPerInterval = new int[] {2, 4};
    private String[] streams = new String[] {"files/downstream.wav",
            "files/dreamland.wav"};

    private HashMap<String, BuyerInfo> users = new HashMap<String, BuyerInfo>();

    public Vendor(int port, File saveFile)
    {
        super(port);
        this.saveFile = saveFile;
    }

    public void start()
    {
        if (saveFile.exists())
        {
            Object[] objects = Util.deserialize(saveFile, 1);
            brokerPublicKey = (RSAPublicKey) objects[0];
        }
        else
        {
            brokerPublicKey = registerWithBroker();
            Util.serialize(new Object[]{brokerPublicKey}, saveFile);
        }
        runServer();
    }

    @Override
    public void receivedMessage(final Message message, final Socket socket)
    {
        System.out.println("\n\nMessage: " + message.type());
        System.out.println(message);

        Thread thread = new Thread(new Runnable() { public void run()
        {
            if (message.type().equals("commit"))
                onCommit(message, socket);
            else if (message.type().equals("cost"))
                onCost(message, socket);
            else if (message.type().equals("pay"))
                onPay(message, socket);

            Util.closeSocket(socket);
        }});
        thread.start();
    }

    private void onCommit(Message message, Socket socket)
    {
        int stream = Integer.parseInt(message.get("stream"));
        SignedCommitment sc = new SignedCommitment(
                (Element) message.xml.getFirstChild());

        BuyerInfo info = new BuyerInfo();
        info.signedCommitment = sc;
        info.centsPayed = 0;
        info.startTime = (int) (System.currentTimeMillis() / 1000);
        info.lastWord = sc.commitment.wordZero;
        Certificate certificate = sc.commitment.userCertificate.certificate;
        users.put(certificate.user, info);

        if (!sc.commitment.userCertificate.verifySignature(brokerPublicKey))
        {
            System.out.println("Certificate is not valid. Aborting.");
            return;
        }
        System.out.println("Certificate is valid.");

        if (certificate.expirationDate.before(new Date()))
        {
            System.out.println("Certificate is expired.");
            return;
        }
        System.out.println("Certificate is not expired.");

        if (!sc.verifySignature(certificate.userPublicKey))
        {
            System.out.println("Commitment is not valid. Aborting.");
            return;
        }
        System.out.println("Commitment is valid.");

        FileInputStream in = null;
        OutputStream out = null;
        try
        {
            in = new FileInputStream(streams[stream]);
            out = socket.getOutputStream();

            byte[] buf = new byte[4096];
            int len;
            double playTime;
            int totalCentsPayed;
            double intervalsPassed;
            int intervalsPayedFor;
            while ( (len = in.read(buf)) > 0)
            {
                playTime = (int) (System.currentTimeMillis()/1000)
                        - info.startTime;
                synchronized (info)
                {
                    totalCentsPayed = info.centsPayed;
                }

                intervalsPassed = playTime / timeInterval;
                intervalsPayedFor = totalCentsPayed / centsPerInterval[stream];

                // Grace interval
                if (intervalsPassed > intervalsPayedFor + 1)
                {
                    System.out.printf("User '%s' stopped paying.",
                            certificate.user);
                    break;
                }

                if (info.fraudAttempt)
                {
                    System.out.println("Aborting because of fraud attempt.");
                    break;
                }

                // If all is good, push new data.
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        }
        catch (SocketException ex)
        {
            try
            {
                in.close();
                out.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
                System.exit(1);
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            System.exit(1);
        }

        System.out.println("Stream ended.");

        cashIn(info);
    }

    private void onCost(Message message, Socket socket)
    {
        int stream = Integer.parseInt(message.get("stream"));
        Message response = new Message("costInfo");
        response.set("timeInterval", timeInterval);
        response.set("cents", centsPerInterval[stream]);
        response.writeToSocket(socket);
    }

    private void onPay(Message message, Socket socket)
    {
        String user = message.get("user");
        String payWord = message.get("word");
        int index = Integer.parseInt(message.get("index"));

        if (!users.containsKey(user))
        {
            System.out.println("No such user: " + user);
            return;
        }

        BuyerInfo b = users.get(user);
        int centsPaying = index - b.centsPayed;

        if (index <= b.centsPayed)
        {
            System.out.println("Double spending attempt detected.");
            b.fraudAttempt = true;
            return;
        }
        if (!PayWordChain.verifyDistance(payWord, b.lastWord, centsPaying))
        {
            System.out.println("Fraud attempt detected.");
            b.fraudAttempt = true;
            return;
        }

        System.out.println("PayWords are valid.");

        // If all is well.
        synchronized (b)
        {
            b.centsPayed += centsPaying;
            b.lastWord = payWord;
        }
    }

    private RSAPublicKey registerWithBroker()
    {
        Message message = new Message("registerVendor");
        message.set("bankAccount", bankAccount);
        Socket socket = Util.newSocket(Util.stringToAddress(brokerAddress));
        Message response = message.askForResponse(socket);
        return (RSAPublicKey) RSA.publicKeyFromBytes(
                DatatypeConverter.parseBase64Binary(response.get("publicKey")));
    }

    private void cashIn(BuyerInfo info)
    {
        Message message = new Message("transferMoney");
        message.set("bankAccount", bankAccount);
        message.set("lastWord", info.lastWord);
        message.set("lastIndex", info.centsPayed);
        message.xml.appendChild(info.signedCommitment.toElement(
                message.xml.getOwnerDocument()));

        Socket socket = Util.newSocket(Util.stringToAddress(brokerAddress));
        Message response = message.askForResponse(socket);

        System.out.printf("Cashing in from '%s'.\n",info.signedCommitment
                .commitment.userCertificate.certificate.user);
        if (response.get("success").equals("false"))
            System.out.println("Transfering error: " + response.get("reason"));
        else
            System.out.println("Transfer successful.");
    }
}

class BuyerInfo
{
    public SignedCommitment signedCommitment;
    public int startTime;
    public int centsPayed;
    public String lastWord;
    public boolean fraudAttempt = false;
}
