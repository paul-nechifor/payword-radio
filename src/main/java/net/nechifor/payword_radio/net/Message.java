package net.nechifor.payword_radio.net;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import net.nechifor.payword_radio.util.Util;

public class Message
{
    public Element xml;

    private Message()
    {
    }

    public Message(String type)
    {
        Document d = Util.emptyDocument();
        xml = d.createElement("message");
        xml.setAttribute("type", type);
    }

    public String type()
    {
        return xml.getAttribute("type");
    }

    public String get(String firstElementAttribute)
    {
        return xml.getAttribute(firstElementAttribute);
    }

    public void set(String firstElementAttribute, String value)
    {
        xml.setAttribute(firstElementAttribute, value);
    }

    public void set(String firstElementAttribute, int value)
    {
        xml.setAttribute(firstElementAttribute, new Integer(value).toString());
    }

    public void set(String firstElementAttribute, long value)
    {
        xml.setAttribute(firstElementAttribute, new Long(value).toString());
    }

    @Override
    public String toString()
    {
        return Util.cannonicalXml(xml);
    }

    public static Message readFromSocket(Socket socket)
    {
        try
        {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            StringBuilder builder = new StringBuilder();
            String wasRead = null;

            while (true)
            {
                wasRead = reader.readLine();
                if (wasRead != null)
                    builder.append(wasRead);
                else
                    break;
            }

            Message m = new Message();
            m.xml = Util.loadXmlFromString(builder.toString());
            return m;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.exit(1);
        }

        return null;
    }

    public void writeToSocket(Socket socket)
    {
        try
        {
            String mesg = Util.cannonicalXml(xml);
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream()));
            writer.write(mesg);
            writer.flush();
            socket.shutdownOutput();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public Message askForResponse(Socket socket)
    {
        this.writeToSocket(socket);
        Message ret = Message.readFromSocket(socket);
        Util.closeSocket(socket);
        return ret;
    }
}
