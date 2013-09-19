package si_t6.logic;

import java.security.MessageDigest;
import javax.xml.bind.DatatypeConverter;
import si_t6.util.Util;

public class PayWordChain
{
    final public static int BYTE_LENGTH = 20;

    public String[] words;
    public int current; // Last word used.

    public PayWordChain(int length)
    {
        this.words = new String[length + 1];

        byte[] byteWord = Util.randomBytes(BYTE_LENGTH);
        words[length] = DatatypeConverter.printHexBinary(byteWord);

        MessageDigest md = Util.sha1Digest();

        for (int i = length - 1; i >= 0; i--)
        {
            byteWord = md.digest(byteWord);
            words[i] = DatatypeConverter.printHexBinary(byteWord);
        }

        current = 0;
    }

    public static boolean verifyDistance(String start, String end, int distance)
    {
        byte[] byteWord = DatatypeConverter.parseHexBinary(start);

        MessageDigest md = Util.sha1Digest();

        for (int i = 0; i < distance; i++)
            byteWord = md.digest(byteWord);

        String endDigest = DatatypeConverter.printHexBinary(byteWord);

        return end.equals(endDigest);
    }
}