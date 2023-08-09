package io.github.honhimw;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * @author hon_him
 * @since 2022-06-01
 */
@SuppressWarnings("unused")
public class GZipUtils {

    public static final int BUFFER = 1024;

    /**
     * compress
     */
    public static byte[] compress(byte[] data) throws IOException {
        ByteArrayInputStream baips = new ByteArrayInputStream(data);
        ByteArrayOutputStream baops = new ByteArrayOutputStream();
        compress(baips, baops);
        byte[] output = baops.toByteArray();
        baops.flush();
        baops.close();
        baips.close();
        return output;
    }

    public static void compress(InputStream ips, OutputStream ops) throws IOException {
        GZIPOutputStream gzops = new GZIPOutputStream(ops);
        int count;
        byte[] data = new byte[BUFFER];
        while ((count = ips.read(data, 0, BUFFER)) != -1) {
            gzops.write(data, 0, count);
        }
        gzops.finish();
        gzops.flush();
        gzops.close();
    }

    /**
     * decompress
     */
    public static byte[] decompress(byte[] data) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        decompress(bais, baos);
        data = baos.toByteArray();
        baos.flush();
        baos.close();
        bais.close();
        return data;
    }

    public static void decompress(InputStream is, OutputStream os) throws IOException {
        GZIPInputStream gzips = new GZIPInputStream(is);
        int count;
        byte[] data = new byte[BUFFER];
        while ((count = gzips.read(data, 0, BUFFER)) != -1) {
            os.write(data, 0, count);
        }
        gzips.close();
    }

}
