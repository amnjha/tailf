package com.example.tailf.file;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

import static java.nio.file.StandardWatchEventKinds.*;

@EnableScheduling
@Component
public class FileChangeEventListener implements DisposableBean, Runnable {
	private static final Logger LOGGER = LoggerFactory.getLogger(FileChangeEventListener.class);
	private static long lastModifiedTs = 0;
	private Thread thread;
	private volatile boolean continueRunning;
	private String watchFilePath;
	private String fileName;
	@Autowired
	private ChangePublisher changePublisher;

	FileChangeEventListener(@Value("${log.file.watch.path}") String watchFilePath) {
		this.thread = new Thread(this);
		this.thread.start();
		this.watchFilePath = watchFilePath;
		continueRunning = false;
	}

	private String validateAndGetParent(String watchFile) {
		Path filePath = Paths.get(watchFile);
		boolean isRegularFile = Files.isRegularFile(filePath);
		if (!isRegularFile) {
			throw new IllegalArgumentException(watchFile + " is not a regular file");
		}

		Path parent = filePath.getParent();
		this.fileName = filePath.getFileName().toString();
		return parent.toString();
	}

	@Override
	public void run() {
		watchFileForEvents();
	}

	@Override
	public void destroy() {
		continueRunning = false;
	}

	public void watchFileForEvents() {
		try {
			WatchService watchService = FileSystems.getDefault().newWatchService();
			String parentPath = validateAndGetParent(watchFilePath);
			Path path = Paths.get(parentPath);
			path.register(watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
			boolean poll = true;

			while (poll && continueRunning) {
				WatchKey key = watchService.take();
				for (WatchEvent<?> event : key.pollEvents()) {
					Path modifiedFilePath = (Path) event.context();
					if (modifiedFilePath.endsWith(fileName)) {
						if (event.kind() == ENTRY_MODIFY) {
							changePublisher.publishFileChange(watchFilePath);
						} else if (event.kind() == ENTRY_DELETE) {
							changePublisher.resetReadPointer(watchFilePath, false);
						} else if (event.kind() == ENTRY_CREATE) {
							changePublisher.resetReadPointer(watchFilePath, true);
						}
					}
				}
				poll = key.reset();
			}
		} catch (InterruptedException e) {
			continueRunning = false;
			LOGGER.info("Stopping Thread due to interrupt signal");
		} catch (IOException e) {
			LOGGER.error("IO Exception while registering file watcher, cannot continue.");
			LOGGER.error(e.getMessage());
			System.exit(1);
		}
	}

	// Read File Every 500 Ms
	private static boolean fileExists = true;

	@Scheduled(fixedRate = 500)
	public void checkModification() throws IOException {
		String filePath = "/Users/amajha/Documents/sample.log";
		File file = new File(filePath);
		long lastModified = file.lastModified();
		if(!file.exists()){
			fileExists =false;
			changePublisher.resetReadPointer(filePath,false);
		} else if(!fileExists){ //File Did not exist previously but has been created now
			changePublisher.resetReadPointer(filePath,true);
			fileExists  = true;
		}

		if (lastModified > lastModifiedTs) {
			lastModifiedTs = lastModified;
			changePublisher.publishFileChange(filePath);
		}
	}
}