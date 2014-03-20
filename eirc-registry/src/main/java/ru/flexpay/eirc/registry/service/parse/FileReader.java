package ru.flexpay.eirc.registry.service.parse;

import com.google.common.collect.Lists;
import org.complitex.dictionary.service.executor.ExecuteException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.List;

/**
 * @author Pavel Sknar
 */
public class FileReader {
    public static final String DEFAULT_CHARSET = "Cp1251";
    public static final int MAX_RECORD_LENGTH = 10000;

    private BufferedInputStream is;
    private String fileName;
    private long fileLength = -1;

    private String charset;
    private int b;
    private byte[] record = new byte[MAX_RECORD_LENGTH];
    private Message message;
    private long position;


    public FileReader(InputStream is, String fileName, long fileLength) {
        this(is, fileName, fileLength, -1);
    }

    public FileReader(InputStream is, String fileName, long fileLength, int bufferSize) {
        this.fileName = fileName;
        this.fileLength = fileLength;
        this.is = bufferSize > 0? new BufferedInputStream(is, bufferSize) : new BufferedInputStream(is);
        this.charset = DEFAULT_CHARSET;
        this.position = 0;
    }

    public String getFileName() {
        return fileName;
    }

    public long getFileLength() {
        return fileLength;
    }

    @SuppressWarnings ({"unchecked"})
    public List<Message> getMessages(List<Message> listMessage, Integer minReadChars)
            throws ExecuteException, RegistryFormatException {
        if (listMessage == null) {
            listMessage = Lists.newArrayList();
        } else if (!listMessage.isEmpty()) {
            return listMessage;
        }

        try {
            Long startPoint = getPosition();
            FileReader.Message message;

            do {
                message = readMessage();
                listMessage.add(message);
            } while (message != null && (getPosition() - startPoint) < minReadChars);

            return listMessage;
        } catch (IOException e) {
            throw new ExecuteException("Failed open stream", e);
        }
    }

    public Message readMessage() throws IOException, RegistryFormatException {

        if (b == -1) {
            return null;
        }

        int ind = 0;
        while ((b = is.read()) != -1) {
            position++;
            if (b == ParseRegistryConstants.MESSAGE_TYPE_HEADER
                    || b == ParseRegistryConstants.MESSAGE_TYPE_RECORD
                    || b == ParseRegistryConstants.MESSAGE_TYPE_FOOTER) {
                if (message == null) {
                    message = new Message();
                    message.setType(b);
                    message.setPosition(position);
                } else {
                    break;
                }
            } else if (message != null) {
                if (ind >= MAX_RECORD_LENGTH) {
                    throw new RegistryFormatException("Message is too long", position);
                }
                record[ind] = (byte) b;
                ind++;
            }
        }

        if (message != null) {
            message.setBody(new String(record, 0, ind, charset).trim());
        }

        Message result = message;
        message = new Message();
        message.setType(b);
        message.setPosition(position);

        return result;
    }

    public void setInputStream(InputStream is) throws IOException {
        if (is == null) {
            this.is = null;
        } else {
            this.is = new BufferedInputStream(is);
            this.is.skip(position);
        }
    }

    public long getPosition() {
        return position;
    }

    public void close() throws IOException {
        is.close();
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    @Override
    public String toString() {
        return "FileReader{" +
                "position=" + position +
                ", message=" + message +
                '}';
    }

    public static class Message implements Serializable {

        private int type;
        private String body;
        private long position;

        public Message() {
        }

        public int getType() {
            return type;
        }

        public void setType(int type) {
            this.type = type;
        }

        public String getBody() {
            return body;
        }

        public void setBody(String body) {
            this.body = body;
        }

        public long getPosition() {
            return position;
        }

        public void setPosition(long position) {
            this.position = position;
        }


        @Override
        public String toString() {
            return "Message{" +
                    "type=" + type +
                    ", body='" + body + '\'' +
                    ", position=" + position +
                    '}';
        }
    }
}
