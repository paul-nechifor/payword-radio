package net.nechifor.payword_radio;

import java.io.File;
import net.nechifor.payword_radio.net.Broker;
import net.nechifor.payword_radio.net.UserProgram;
import net.nechifor.payword_radio.net.Vendor;

public class Main
{
    public static void main(String[] args)
    {
        // Initalize the XML cannonicalization thing.
        org.apache.xml.security.Init.init();

        if (args.length == 0)
            showSelectMessage();

        if (args[0].equals("--broker"))
            startBroker(args);
        else if(args[0].equals("--vendor"))
            startVendor(args);
        else if(args[0].equals("--user"))
            startUserProgram(args);
        else
            showSelectMessage();
    }

    private static void showSelectMessage()
    {
        System.err.println("Use `--broker` to start a broker server.");
        System.err.println("Use `--vendor` to start a vendor server.");
        System.err.println("Use `--user` to start a user program.");
        System.exit(1);
    }

    private static void startBroker(String[] args)
    {
        if (args.length < 2)
        {
            System.err.println("Use `--broker <saveFile>` to start the " +
                    "broker server.");
            System.exit(1);
        }

        Broker broker = new Broker(2000, new File(args[1]));
        broker.start();
    }

    private static void startVendor(String[] args)
    {
        if (args.length < 2)
        {
            System.err.println("Use `--vendor <saveFile>` to start the " +
                    "vendor server.");
            System.exit(1);
        }
        Vendor vendor = new Vendor(3000, new File(args[1]));
        vendor.start();
    }

    private static void startUserProgram(String[] args)
    {
        if (args.length < 2)
        {
            System.err.println("Use `--user <saveFile>` to start the " +
                    "user program.");
            System.exit(1);
        }
        UserProgram up = new UserProgram(new File(args[1]));
        up.start();
    }
}
