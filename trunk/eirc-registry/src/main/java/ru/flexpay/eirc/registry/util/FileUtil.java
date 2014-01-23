package ru.flexpay.eirc.registry.util;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.util.io.IOUtils;
import org.slf4j.Logger;
import ru.flexpay.eirc.registry.service.parse.ParseRegistryConstants;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.Signature;
import java.security.SignatureException;

/**
 * @author Pavel Sknar
 */
public abstract class FileUtil {
    
    private static final String LINE_END = "\n";
    
    public static boolean isRegistry(File file, Logger log) throws IOException {

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            String line = reader.readLine();

            return (line.length() > 0 && line.codePointAt(0) == ParseRegistryConstants.MESSAGE_TYPE_HEADER);

        } finally {
            IOUtils.closeQuietly(reader);
        }
    }

    public static void writeLine(ByteBuffer buffer, String line, Signature privateSignature, Charset charset) throws IOException {
        byte[] lineBytes = StringUtils.isEmpty(line)? null : getEncodingBytes(line, charset);
        byte[] endLine = getEncodingBytes(LINE_END, charset);
        if (privateSignature != null) {
            try {
                if (lineBytes != null) {
                    privateSignature.update(lineBytes);
                }
                privateSignature.update(lineBytes);
            } catch (SignatureException e) {
                throw new IOException("Can not update signature", e);
            }
        }
        if (lineBytes != null) {
            buffer.put(lineBytes);
        }
        buffer.put(endLine);
    }

    public static void writeCharToLine(ByteBuffer buffer, char ch, int count, Signature privateSignature, Charset charset) throws IOException {

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(ch);
        }

        sb.append('\n');
        write(buffer, sb.toString(), privateSignature, charset);
    }

    public static void write(ByteBuffer buffer, String s, Signature privateSignature, Charset charset) throws IOException {
        byte[] bytes = getEncodingBytes(s, charset);
        if (privateSignature != null) {
            try {
                privateSignature.update(bytes);
            } catch (SignatureException e) {
                throw new IOException("Can not update signature", e);
            }
        }
        buffer.put(bytes);
    }

    private static void write(ByteBuffer buffer, long value, Charset charset) {
        buffer.put(getEncodingBytes(value, charset));
    }

    private static byte[] getEncodingBytes(long value, Charset charset) {
        return getEncodingBytes(Long.toString(value), charset);
    }

    private static byte[] getEncodingBytes(String s, Charset charset) {
        return s.getBytes(charset);
    }
}
