package com.example.tailf.file;

import com.sun.tools.javac.util.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@SpringBootTest
public class ChangePublisherTest {

	@Autowired
	private ChangePublisher changePublisher;

	@Test
	public void testQueueing() {
		for (int i = 0; i < 100; i++) {
			changePublisher.putToQueue(UUID.randomUUID().toString());
		}

		Queue<String> queueValues = changePublisher.getQueue();

		Assert.checkNonNull(queueValues);
		Assert.check(!queueValues.isEmpty());
		Assert.check(queueValues.size() == 10);
	}

	@Test
	public void testPublishingChanges() throws IOException, InterruptedException {
		File file = new File("/Users/amajha/Documents/sample.log");
		file.delete();

		file.createNewFile();
		FileWriter fileWriter = new FileWriter(file);
		List<String> list = IntStream.range(0,5).mapToObj(e-> UUID.randomUUID().toString()).collect(Collectors.toList());
		for (String val : list) {
			fileWriter.write(val + "\n");
		}
		fileWriter.flush();
		fileWriter.close();

		TimeUnit.SECONDS.sleep(5);
		Queue<String> queue = changePublisher.getQueue();
		queue.forEach(System.out::println);
		Assert.check(queue.size() == 5);
		for (String s : list) {
			Assert.check(queue.contains(s));
		}
	}

	/*@Test
	public void testReadNLines() throws IOException {
		List<String> lines = ChangePublisher.readLastNLines(100, "/Users/amajha/Documents/sample.log");
		Assert.check(lines.size() <= 10);

		lines = ChangePublisher.readLastNLines(1, "/Users/amajha/Documents/sample.log");
		Assert.check(lines.size() == 1);
	}*/
}
