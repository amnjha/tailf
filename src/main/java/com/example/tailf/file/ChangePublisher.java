package com.example.tailf.file;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class ChangePublisher {
	private static final Logger LOGGER = LoggerFactory.getLogger(ChangePublisher.class);
	private static final int BUFFER_SIZE = 8192;
	private static int lastLineRead = 0;
	int maxSize = 10;
	ByteArrayOutputStream baos = new ByteArrayOutputStream();
	int bufPos = 0;
	ByteBuffer buf = null;
	RandomAccessFile file = new RandomAccessFile(new File("/Users/amajha/Documents/sample.log"), "r");
	FileChannel channel = file.getChannel();
	long filePos = file.length();
	private Map<String, WebSocketSession> socketSessionCache = new HashMap<>();
	private Queue<String> queue = new LinkedList<>();
	private byte lastLineBreak = '\n';

	public ChangePublisher(@Value("${log.file.watch.path}") String path) throws IOException {
		List<String> lines = readLastNLines(maxSize, path);
		putToQueue(lines.toArray(new String[0]));
	}

	public List<String> readLastNLines(int n, String pathName) throws IOException {

		/*List<String> fileLines = Files.readAllLines(Paths.get(pathName));
		int totalLines = fileLines.size();
		if (totalLines > n) {
			fileLines = fileLines.subList(totalLines - n, totalLines);
		}
		lastLineRead += totalLines;
		return fileLines;*/

		List<String> lines = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			String line = readLine();
			if (line == null)
				break;
			lines.add(line);
		}

		return lines;
	}

	public Queue<String> getQueue() {
		return queue;
	}

	public void resetReadPointer(String filePath, boolean isCreate) throws IOException {
		lastLineRead = 0;
		queue.clear();

		if (isCreate) {
			publishFileChange(filePath);
		}
	}

	public void registerSession(WebSocketSession socketSession) {
		this.socketSessionCache.put(socketSession.getId(), socketSession);
	}

	public void invalidateSession(WebSocketSession socketSession) {
		this.socketSessionCache.remove(socketSession.getId());
	}

	public void publishFileChange(String pathName) throws IOException {
		Stream<String> lineStream = Files.lines(Paths.get(pathName));
		List<String> updatedLines = lineStream.skip(lastLineRead).collect(Collectors.toList());
		lastLineRead += updatedLines.size();

		putToQueue(updatedLines.toArray(new String[updatedLines.size()]));
		updatedLines.forEach(LOGGER::info);
		if (socketSessionCache != null) {
			//String content = updatedLines.stream().collect(Collectors.joining("<br>"));
			for (String line : updatedLines) {
				for (WebSocketSession webSocketSession : socketSessionCache.values()) {
					if (webSocketSession.isOpen()) {
						webSocketSession.sendMessage(new TextMessage(line));
					}
				}
			}
		}
	}

	public void putToQueue(String... values) {
		for (String value : values) {
			queue.add(value);
		}
		if (queue.size() >= maxSize) {
			int extraElements = queue.size() - maxSize;
			for (int i = 0; i < extraElements; i++) {
				queue.remove();
			}
		}
	}

	public Queue<String> getInitialMessage() {
		return queue;
	}

	public String  readLineAfterOffset() throws IOException {
		File file = new File("");
		FileInputStream fis = new FileInputStream(file);
		BufferedInputStream inputStream = new BufferedInputStream(fis);
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
		inputStream.skip(0);

		String line = null;
		while((line = reader.readLine())!=null){

		}

		return null;
	}

	public String readLine() throws IOException {
		byte c;
		while (true) {
			if (bufPos < 0) {
				if (filePos == 0) {
					if (baos == null) {
						return null;
					}
					String line = bufToString(baos, StandardCharsets.UTF_8.name());
					baos = null;
					return line;
				}

				long start = Math.max(filePos - BUFFER_SIZE, 0);
				long end = filePos;
				long len = end - start;

				buf = channel.map(FileChannel.MapMode.READ_ONLY, start, len);
				bufPos = (int) len;
				filePos = start;

				// Ignore Empty New Lines
				c = buf.get(--bufPos);
				if (c == '\r' || c == '\n')
					while (bufPos > 0 && (c == '\r' || c == '\n')) {
						bufPos--;
						c = buf.get(bufPos);
					}
				if (!(c == '\r' || c == '\n'))
					bufPos++;// IS THE NEW LENE
			}

			/*
			 * This will ignore all blank new lines.
			 */
			while (bufPos-- > 0) {
				c = buf.get(bufPos);
				if (c == '\r' || c == '\n') {
					// skip \r\n
					while (bufPos > 0 && (c == '\r' || c == '\n')) {
						c = buf.get(--bufPos);
					}
					// restore cursor
					if (!(c == '\r' || c == '\n'))
						bufPos++;// IS THE NEW Line
					return bufToString(baos, StandardCharsets.UTF_8.name());
				}
				baos.write(c);
			}

		}
	}

	private String bufToString(ByteArrayOutputStream baos, String encoding) throws UnsupportedEncodingException {
		if (baos.size() == 0) {
			return "";
		}

		byte[] bytes = baos.toByteArray();
		for (int i = 0; i < bytes.length / 2; i++) {
			byte t = bytes[i];
			bytes[i] = bytes[bytes.length - i - 1];
			bytes[bytes.length - i - 1] = t;
		}

		baos.reset();
		if (encoding != null)
			return new String(bytes, encoding);
		else
			return new String(bytes);
	}
}
