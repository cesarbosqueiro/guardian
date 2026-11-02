package com.craftwar.launcher.sync;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

final class Parallel {

    private static final int THREADS = 6;

    private Parallel() {
    }

    static void run(List<Callable<Void>> tasks, Progress progress) throws IOException {
        if (tasks.isEmpty()) return;
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        AtomicInteger done = new AtomicInteger();
        try {
            List<Future<Void>> futures = new ArrayList<Future<Void>>(tasks.size());
            for (Callable<Void> t : tasks) {
                futures.add(pool.submit(() -> {
                    t.call();
                    progress.advance(done.incrementAndGet(), tasks.size());
                    return null;
                }));
            }
            for (Future<Void> f : futures) f.get();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw cause instanceof IOException ? (IOException) cause : new IOException(cause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("interrupted");
        } finally {
            pool.shutdownNow();
        }
    }
}
