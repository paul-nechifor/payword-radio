package si_t6.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public abstract class Listener
{
    protected int port;
    protected InetSocketAddress startedOn;
    
    public Listener(int port)
    {
        this.port = port;
    }

    public abstract void receivedMessage(Message message, Socket socket);

    public void runServer()
    {
        try
        {
            ServerSocket server = new ServerSocket(port);
            startedOn = new InetSocketAddress("127.0.0.1", port);

            while (true)
            {
                final Socket client = server.accept();
                Message message = Message.readFromSocket(client);
                receivedMessage(message, client);
            }
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
            System.exit(1);
        }
    }
}