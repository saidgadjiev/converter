package ru.gadjini.telegram.converter.service.concurrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class SmartExecutorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SmartExecutorService.class);

    private Map<JobWeight, ThreadPoolExecutor> executors;

    private final Map<Integer, Future<?>> processing = new ConcurrentHashMap<>();

    private final Map<Integer, Job> activeTasks = new ConcurrentHashMap<>();

    public SmartExecutorService setExecutors(Map<JobWeight, ThreadPoolExecutor> executors) {
        this.executors = executors;

        return this;
    }

    public int getCorePoolSize(JobWeight weight) {
        return executors.get(weight).getCorePoolSize();
    }

    public void execute(Job job) {
        Future<?> submit = executors.get(job.getWeight()).submit(job);
        job.setCancelChecker(submit::isCancelled);
        processing.put(job.getId(), submit);
        activeTasks.put(job.getId(), job);
    }

    public void complete(int jobId) {
        processing.remove(jobId);
        activeTasks.remove(jobId);
    }

    public void complete(Collection<Integer> jobIds) {
        jobIds.forEach(this::complete);
    }

    public boolean cancel(int jobId, boolean userOriginated) {
        Future<?> future = processing.get(jobId);
        if (future != null && (!future.isCancelled() || !future.isDone())) {
            Job job = activeTasks.get(jobId);
            job.setCanceledByUser(userOriginated);
            future.cancel(true);
            job.cancel();

            return true;
        }

        return false;
    }

    public boolean cancelAndComplete(int jobId, boolean userOriginated) {
        boolean result = cancel(jobId, userOriginated);
        complete(jobId);

        return result;
    }

    public boolean isCanceled(int jobId) {
        if (processing.containsKey(jobId)) {
            return processing.get(jobId).isCancelled();
        }

        return false;
    }

    public void cancel(List<Integer> ids, boolean userOriginated) {
        ids.forEach(jobId -> cancel(jobId, userOriginated));
    }

    public void cancelAndComplete(List<Integer> ids, boolean userOriginated) {
        ids.forEach(integer -> {
            cancel(integer, userOriginated);
            complete(integer);
        });
    }

    public void shutdown() {
        try {
            for (Map.Entry<JobWeight, ThreadPoolExecutor> entry : executors.entrySet()) {
                entry.getValue().shutdown();
            }
            Set<Integer> jobs = new HashSet<>(processing.keySet());
            for (Integer job : jobs) {
                cancelAndComplete(job, false);
            }
            for (Map.Entry<JobWeight, ThreadPoolExecutor> entry : executors.entrySet()) {
                if (!entry.getValue().awaitTermination(10, TimeUnit.SECONDS)) {
                    entry.getValue().shutdownNow();
                }
            }
        } catch (Throwable ex) {
            LOGGER.error(ex.getMessage(), ex);
        }
    }

    public interface Job extends Runnable {

        int getId();

        JobWeight getWeight();

        default void cancel() {

        }

        default void setCancelChecker(Supplier<Boolean> checker) {

        }

        default void setCanceledByUser(boolean canceledByUser) {

        }
    }

    public interface ProgressJob extends Job {

        int getProgressMessageId();

        long getChatId();
    }

    public enum JobWeight {

        LIGHT,

        HEAVY
    }
}
