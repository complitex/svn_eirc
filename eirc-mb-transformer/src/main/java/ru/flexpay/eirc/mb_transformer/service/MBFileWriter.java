package ru.flexpay.eirc.mb_transformer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.Signature;
import java.security.SignatureException;

public class MBFileWriter {

	private final static Logger log = LoggerFactory.getLogger(MBFileWriter.class);

	private static final char DEFAULT_ESCAPE_CHARACTER = '"';
	private static final char DEFAULT_SEPARATOR = ',';
	private static final char DEFAULT_QUOTE_CHARACTER = '"';

    private char NO_QUOTE_CHARACTER = '\u0000';
    private char NO_ESCAPE_CHARACTER = '\u0000';
    private String DEFAULT_LINE_END = "\n";

	private ByteBuffer buffer;
	private long size;
	private char separator;
	private char quotechar;
	private char escapechar;
	private String lineEnd;
	private String fileEncoding;
	private Signature signature;

	public void writeLine(String[] nextLine, StringBuilder sb) throws IOException, SignatureException {

		if (nextLine == null) {
			return;
		}

		for (int i = 0; i < nextLine.length; i++) {

			if (i != 0) {
				sb.append(separator);
			}

			String nextElement = nextLine[i];
			if (nextElement == null) {
				continue;
			}

			appendCell(sb, nextElement);
		}
		sb.append(lineEnd);

		log.debug("Write line: {}", sb);
		write(sb);
	}

	public void write(byte[] bytes, int off, int len) throws SignatureException, IOException {

		if (signature != null) {
			signature.update(bytes, off, len);
		}

		size += len;
		buffer.put(bytes, off, len);
	}

	public void write(byte[] bytes) throws SignatureException, IOException {

		if (signature != null) {
			signature.update(bytes);
		}

		size += bytes.length;
		buffer.put(bytes);
	}

	public void write(CharSequence cs) throws IOException, SignatureException {

		byte[] bytes = cs.toString().getBytes(getFileEncoding());
		write(bytes);
	}

	public void writeLine(String nextLine) throws IOException, SignatureException {

		if (nextLine == null) {
			return;
		}

		StringBuilder sb = new StringBuilder();
		appendCell(sb, nextLine);
		sb.append(lineEnd);

		log.debug("Write line: {}", sb);
		write(sb);
	}

	public void writeLine(byte[] nextLine) throws IOException, SignatureException {

		if (nextLine == null) {
			return;
		}

		log.debug("Write line: {}", nextLine);

		write(nextLine);
		write(lineEnd);
	}

	public void writeCharToLine(char ch, int count) throws IOException, SignatureException {

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < count; i++) {
			sb.append(ch);
		}
		if (quotechar != NO_QUOTE_CHARACTER) {
			sb.append(quotechar);
		}

		sb.append(lineEnd);
		log.debug("Write line: {}", sb);
		write(sb);
	}

	private void appendCell(StringBuilder sb, String nextLine) {

		if (quotechar != NO_QUOTE_CHARACTER) {
			sb.append(quotechar);
		}

		for (int i = 0; i < nextLine.length(); i++) {

			char nextChar = nextLine.charAt(i);
			if (escapechar != NO_ESCAPE_CHARACTER && nextChar == quotechar) {
				sb.append(escapechar).append(nextChar);
			} else if (escapechar != NO_ESCAPE_CHARACTER && nextChar == escapechar) {
				sb.append(escapechar).append(nextChar);
			} else {
				sb.append(nextChar);
			}
		}

		if (quotechar != NO_QUOTE_CHARACTER) {
			sb.append(quotechar);
		}
	}

	public byte[] getSign() throws SignatureException {
		return signature != null ? signature.sign() : null;
	}

	public void setSignature(Signature signature) {
		this.signature = signature;
	}

	public long getFileSize() {
		return size;
	}

	public void setFileEncoding(String fileEncoding) {
		this.fileEncoding = fileEncoding;
	}

	public String getFileEncoding() {
		return fileEncoding != null ? fileEncoding : "Cp866";
	}
}
