package si_t6.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.xml.security.c14n.Canonicalizer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

public class Util
{
    public static String addressToString(InetSocketAddress a)
    {
        return a.getAddress().getHostAddress() + ":" + a.getPort();
    }

    public static InetSocketAddress stringToAddress(String s)
    {
        String[] split = s.split(":");
        return new InetSocketAddress(split[0], Integer.parseInt(split[1]));
    }
    
    public static Document emptyDocument()
    {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try
        {
            return dbf.newDocumentBuilder().newDocument();
        }
        catch (ParserConfigurationException ex)
        {
            ex.printStackTrace();
            System.exit(1);
        }
        return null;
    }

    public static String elementToString(Element element)
    {
        try
        {
            Source source = new DOMSource(element);
            StringWriter stringWriter = new StringWriter();
            Result result = new StreamResult(stringWriter);
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer();
            transformer.transform(source, result);
            return stringWriter.getBuffer().toString();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.exit(1);
        }
        return null;
    }

    public static String cannonicalXml(Element e)
    {
        try
        {
            Canonicalizer canon = Canonicalizer.getInstance(
                    Canonicalizer.ALGO_ID_C14N_OMIT_COMMENTS);
            return new String(canon.canonicalizeSubtree(e));
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            System.exit(1);
        }
        return null;
    }

    public static Element loadXmlFromString(String xml)
    {
        try
        {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = dbf.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(xml));
            return builder.parse(is).getDocumentElement();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.exit(1);
        }

        return null;
    }

    public static void closeSocket(Socket socket)
    {
        try
        {
            socket.close();
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    public static Socket newSocket(String ipAddress, int port)
    {
        try
        {
            return new Socket(ipAddress, port);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            System.exit(1);
        }
        return null;
    }

    public static Socket newSocket(InetSocketAddress a)
    {
        return newSocket(a.getAddress().getHostAddress(), a.getPort());
    }

    public static Object[] deserialize(File saveFile, int numberOfObjects)
    {
        try
        {
            FileInputStream fis = new FileInputStream(saveFile);
            ObjectInputStream in = new ObjectInputStream(fis);
            Object[] ret = new Object[numberOfObjects];
            for (int i = 0; i < numberOfObjects; i++)
                ret[i] = in.readObject();
            return ret;
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            System.exit(1);
        }
        return null;
    }

    public static void serialize(Object[] objects, File saveFile)
    {
        try
        {
            FileOutputStream fos = new FileOutputStream(saveFile);
            ObjectOutputStream out = new ObjectOutputStream(fos);
            for (Object object : objects)
                out.writeObject(object);
            out.close();
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    public static MessageDigest sha1Digest()
    {
        try
        {
            return MessageDigest.getInstance("SHA1");
        }
        catch (NoSuchAlgorithmException ex)
        {
            ex.printStackTrace();
            System.exit(1);
        }
        return null;
    }

    public static byte[] randomBytes(int length)
    {
        byte[] ret = new byte[length];

        for (int i = 0; i < length; i++)
            ret[i] = (byte) (Math.random() * 256);

        return ret;
    }
}
