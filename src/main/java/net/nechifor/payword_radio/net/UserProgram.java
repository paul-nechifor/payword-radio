package net.nechifor.payword_radio.net;

import java.awt.EventQueue;
import java.io.EOFException;
import java.io.File;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.Scanner;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.xml.bind.DatatypeConverter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import net.nechifor.payword_radio.gui.MainWindow;
import net.nechifor.payword_radio.logic.Commitment;
import net.nechifor.payword_radio.logic.PayWordChain;
import net.nechifor.payword_radio.util.RSA;
import net.nechifor.payword_radio.logic.SignedCertificate;
import net.nechifor.payword_radio.logic.SignedCommitment;
import net.nechifor.payword_radio.util.Util;

public class UserProgram
{
    // These are serialized.
    private SignedCertificate signedCertificate;
    private RSAPrivateKey privateKey;
    private RSAPublicKey publicKey;

    // These are not serialized.
    private File saveFile;
    private PayWordChain currentChain;
    private SignedCommitment currentCommitment;
    private String currentVendor;
    private boolean isConnected = false;
    private boolean continuePaying = true;
    private int centsPerInterval;
    private int timeInterval;
    private MainWindow mainWindow;
    private SourceDataLine line;

    public UserProgram(File saveFile)
    {
        this.saveFile = saveFile;
    }
    
    public void start()
    {
        if (saveFile.exists())
            deserialize();
        else
        {
            register();
            serialize();
            System.out.println("Restart to run GUI.");
            System.exit(0);
        }

        final UserProgram that = this;

    	EventQueue.invokeLater(new Runnable() { public void run()
        {
            try
            {
                UIManager.setLookAndFeel(
                        UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {}

            mainWindow = new MainWindow(that);
        }});
    }

    public void connectToVendor(String address)
    {
        if (isConnected)
            return;

        isConnected = true;
        continuePaying = true;

        currentVendor = address;
        InetSocketAddress socketAddress = Util.stringToAddress(address);
        currentCommitment = createCommitmentFor(socketAddress, 10000);
        
        Socket socket = Util.newSocket(socketAddress);

        int stream = Integer.parseInt(mainWindow.streamCode.getText());

        // Get cost information.
        Message message = new Message("cost");
        message.set("stream", stream);
        Message response = message.askForResponse(socket);
        centsPerInterval = Integer.parseInt(response.get("cents"));
        timeInterval = Integer.parseInt(response.get("timeInterval"));

        mainWindow.costLb.setText(String.format("Cost: %.2f per %d seconds",
                centsPerInterval/100.0, timeInterval));

        socket = Util.newSocket(socketAddress);

        message = new Message("commit");
        message.set("stream", stream);
        Document d = message.xml.getOwnerDocument();
        message.xml.appendChild(currentCommitment.toElement(d));
        message.writeToSocket(socket);

        startPaying();
        play(socket);
    }
    
    public void disconnect()
    {
        isConnected = false;
    }

    private void register()
    {
        KeyPair keyPair = RSA.generateKeyPair(1024);
        privateKey = (RSAPrivateKey) keyPair.getPrivate();
        publicKey = (RSAPublicKey) keyPair.getPublic();

        System.out.println("Register information\n====================\n");
        Scanner in = new Scanner(System.in);
        System.out.print("Name: ");
        String name = in.nextLine();
        System.out.print("Credit card: ");
        String creditCard = in.nextLine();
        System.out.print("Delivery address: ");
        String deliveryAddress = in.nextLine();

        Message message = new Message("register");
        message.set("name", name);
        message.set("creditCard", creditCard);
        message.set("publicKey",
                DatatypeConverter.printBase64Binary(publicKey.getEncoded()));
        message.set("deliveryAddress", deliveryAddress);

        Socket socket = Util.newSocket("127.0.0.1", 2000);
        Message response = message.askForResponse(socket);

        System.out.println(Util.cannonicalXml(response.xml));

        signedCertificate = new SignedCertificate(
                (Element) response.xml.getFirstChild());
    }


    private SignedCommitment createCommitmentFor(InetSocketAddress address,
            int wordLength)
    {
        currentChain = new PayWordChain(wordLength);

        Commitment c = new Commitment();
        c.vendor = address;
        c.userCertificate = signedCertificate;
        c.wordZero = currentChain.words[0];
        c.date = new Date();

        SignedCommitment sc = new SignedCommitment();
        sc.commitment = c;
        sc.generateSignature(privateKey);

        return sc;
    }

    private void serialize()
    {
        Object[] o = new Object[]{signedCertificate, privateKey, publicKey};
        Util.serialize(o, saveFile);
    }

    private void deserialize()
    {
        Object[] objects = Util.deserialize(saveFile, 3);
        signedCertificate = (SignedCertificate) objects[0];
        privateKey = (RSAPrivateKey) objects[1];
        publicKey = (RSAPublicKey) objects[2];
    }

    private void play(Socket socket)
    {

        try
        {
            AudioInputStream stream
                    = AudioSystem.getAudioInputStream(socket.getInputStream());

            AudioFormat format = stream.getFormat();

            // Create line if it doesn't exist.
            if (line == null)
            {
                SourceDataLine.Info info = new DataLine.Info(
                    SourceDataLine.class, stream.getFormat(),
                    ((int)stream.getFrameLength()*format.getFrameSize()));
                line = (SourceDataLine) AudioSystem.getLine(info);
            }
            line.open(stream.getFormat());
            line.start();

            // Continuously read and play chunks of audio
            int numRead = 0;
            byte[] buf = new byte[line.getBufferSize()];

            stopPlaying:
            while ((numRead = stream.read(buf, 0, buf.length)) >= 0)
            {
                if (!isConnected)
                    break stopPlaying;

                int offset = 0;
                while (offset < numRead)
                {
                    if (!isConnected)
                        break stopPlaying;

                    offset += line.write(buf, offset, numRead-offset);
                }
            }
            line.drain();
            line.stop();
        }
        catch (EOFException e)
        {
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.exit(1);
        }

        Util.closeSocket(socket);
        isConnected = false;

        JOptionPane.showMessageDialog(mainWindow, "Stream ended.");
    }

    private void pay()
    {
        currentChain.current += centsPerInterval;

        Message message = new Message("pay");
        message.set("user", signedCertificate.certificate.user);
        message.set("word", currentChain.words[currentChain.current]);
        message.set("index", currentChain.current);

        InetSocketAddress address = Util.stringToAddress(currentVendor);
        Socket socket = Util.newSocket(address);
        message.writeToSocket(socket);
        Util.closeSocket(socket);
    }

    private void startPaying()
    {
        final int startTime = (int) (System.currentTimeMillis() / 1000);
        final UserProgram that = this;
        
        Runnable runnable = new Runnable()
        {
            public void run()
            {
                double playTime;
                int centsPayed = 0;
                double intervalsPassed;
                int intervalsPayedFor;

                sleepFor(500);
                
                while (isConnected && continuePaying)
                {
                    playTime = (int) (System.currentTimeMillis()/1000)
                            - startTime;

                    intervalsPassed = playTime / timeInterval;
                    intervalsPayedFor = centsPayed / centsPerInterval;
                    
                    if (intervalsPassed > intervalsPayedFor)
                    {
                        that.pay();
                        centsPayed += centsPerInterval;
                        mainWindow.totalPayedLb.setText(String.format(
                                "Total payed: %.2f", centsPayed/100.0));
                    }
                    
                    sleepFor(100);
                }
            }

            private void sleepFor(int milis)
            {
                try
                {
                    Thread.sleep(100);
                }
                catch (InterruptedException ex)
                {
                }
            }
        };

        Thread thread = new Thread(runnable);
        thread.start();
    }

    public void stopPaying()
    {
        continuePaying = false;
    }
}
