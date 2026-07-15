package org.liondevelopers.resybot;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Owns the pool of concurrent {@link MonitorJob}s. Used by the web layer (Phase 2)
 * to start, list, and stop background monitoring jobs.
 */
public class MonitorManager {

    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "monitor-job");
        t.setDaemon(true);
        return t;
    });
    private final Map<String, MonitorJob> jobs = new ConcurrentHashMap<>();
    private final Map<String, Future<?>> futures = new ConcurrentHashMap<>();

    /** Starts a new background monitoring job and returns its id. */
    public String start(MonitorRequest request) {
        String id = UUID.randomUUID().toString().substring(0, 8);
        MonitorJob job = new MonitorJob(id, request);
        jobs.put(id, job);
        futures.put(id, executor.submit(job));
        return id;
    }

    public MonitorJob get(String id) {
        return jobs.get(id);
    }

    public Collection<MonitorJob> list() {
        return jobs.values();
    }

    /** Stops and removes a job; returns true if it existed. */
    public boolean stop(String id) {
        MonitorJob job = jobs.remove(id);
        Future<?> future = futures.remove(id);
        if (job != null) {
            job.stop();
        }
        if (future != null) {
            future.cancel(true);
        }
        return job != null;
    }

    public void shutdown() {
        jobs.values().forEach(MonitorJob::stop);
        executor.shutdownNow();
    }
}
