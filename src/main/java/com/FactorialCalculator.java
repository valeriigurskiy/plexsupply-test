package com;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class FactorialCalculator {

    private static final int MAX_OPERATIONS_PER_SECOND = 100;
    private static final int OPERATIONS_INTERVAL_SECONDS = 1;
    private static final ForkJoinPool FJK = ForkJoinPool.commonPool();

    private final String inputFile;
    private final String outputFile;
    private final ExecutorService threadPool;

    private final Semaphore rateLimiter = new Semaphore(0);
    private final Map<Integer, String> results = new HashMap<>();
    private final ReentrantLock resultsLock = new ReentrantLock();
    private final Condition nextReady = resultsLock.newCondition();
    private final BlockingQueue<Task> tasks = new LinkedBlockingQueue<>();
    private final ScheduledExecutorService semaphoreScheduler = Executors.newSingleThreadScheduledExecutor();

    private int totalTasks = 0;
    private boolean inputFileReadingDone = false;

    private volatile boolean readerStarted = false;

    public FactorialCalculator(int poolSize, String inputPath, String outputPath) {
        validatePath(inputPath);
        validatePoolSize(poolSize);
        this.inputFile = inputPath;
        this.outputFile = outputPath;
        this.threadPool = Executors.newFixedThreadPool(poolSize);
        System.out.printf("Factorial calculator init with pool size: %s%n", poolSize);
    }

    private void validatePoolSize(int poolSize) {
        if (poolSize < 1) {
            throw new UnsupportedOperationException("Pool size can't be less than 1.");
        }
    }

    private void validatePath(String path) throws UnsupportedOperationException {
        if (!Files.exists(Path.of(path))) {
            throw new UnsupportedOperationException("File with path %s doesn't exists.".formatted(path));
        }
    }

    public void start() {
        semaphoreScheduler.scheduleAtFixedRate(() -> {
            rateLimiter.drainPermits();
            rateLimiter.release(MAX_OPERATIONS_PER_SECOND);
        }, 0, OPERATIONS_INTERVAL_SECONDS, TimeUnit.SECONDS);

        startReaderThread();
        startWriterThread();
        processTasks();
    }

    private synchronized void startReaderThread() {
        if (readerStarted) {
            throw new UnsupportedOperationException("You can't start reader more than one time.");
        }
        readerStarted = true;

        new Thread(this::readerTask).start();
        System.out.println("Reader task started.");
    }

    private void readerTask() {
        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile))) {
            String line;
            int id = 0;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (!line.isEmpty()) {
                    try {
                        int number = Integer.parseInt(line);
                        tasks.put(new Task(id++, number));
                        totalTasks++;
                    } catch (NumberFormatException e) {
                        System.err.printf(
                                "Line %d. Can't convert string [%s] to integer number, ignoring...%n", id, line
                        );
                    }
                }
            }
        } catch (Exception e) {
            System.out.printf("Exception occurred in reader task: %s%n", e.getMessage());
        } finally {
            System.out.printf("Reading %s completed. Total tasks: %s%n", inputFile, totalTasks);
            inputFileReadingDone = true;
        }
    }

    private void startWriterThread() {
        new Thread(this::writerTask).start();
    }

    private void writerTask() {
        System.out.println("Writer task started.");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            int id = 0;

            while (!inputFileReadingDone || id < totalTasks) {
                String resultLine;

                resultsLock.lock();
                try {
                    while (!results.containsKey(id)) {
                        if (inputFileReadingDone && id >= totalTasks) {
                            break;
                        }

                        nextReady.await();
                    }

                    resultLine = results.remove(id);
                } finally {
                    resultsLock.unlock();
                }

                if (resultLine != null) {
                    writer.write(resultLine);
                    writer.newLine();
                    writer.flush();
                    id++;
                }
            }
        } catch (IOException | InterruptedException e) {
            System.err.printf("Error occurred in writer task: %s%n", e.getMessage());
        } finally {
            semaphoreScheduler.shutdownNow();
        }
    }

    private void processTasks() {
        System.out.println("Process task started.");

        while (!inputFileReadingDone || !tasks.isEmpty()) {
            Task task = tasks.poll();
            if (task != null) {
                threadPool.submit(() -> calculateFactorial(task));
            }
        }

        threadPool.shutdown();
    }

    private void calculateFactorial(Task task) {
        try {
            rateLimiter.acquire();

            BigInteger factorial = BigInteger.ONE;

            if (task.number() > 1) {
                factorial = FJK.invoke(new FactorialRecursiveTask(1, task.number()));
            }

            String line = task.number() + " = " + factorial;

            resultsLock.lock();
            try {
                results.put(task.id(), line);
                nextReady.signalAll();
            } finally {
                resultsLock.unlock();
            }
        } catch (InterruptedException e) {
            System.err.printf("Error occurred in calculate factorial method: %s%n", e.getMessage());
        }
    }

}
