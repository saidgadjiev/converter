package ru.gadjini.telegram.converter.service.conversion.aspose;

import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Supplier;

@Service
public class AsposeExecutorService {

    private final ThreadPoolExecutor executorService = createThreadPool();

    private final Map<Integer, CompletableFuture<Boolean>> callbacks = new ConcurrentHashMap<>();

    public CompletableFuture<Boolean> submit(AsposeTask asposeTask) {
        CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();
        callbacks.put(asposeTask.getId(), completableFuture);
        completableFuture.completeAsync(new AsposeTaskSupplier(asposeTask), executorService);

        return completableFuture;
    }

    public void shutdown() {
        callbacks.forEach((integer, completableFuture) -> completableFuture.cancel(true));
        executorService.shutdownNow();
    }

    private ThreadPoolExecutor createThreadPool() {
        return new ThreadPoolExecutor(2, 2, 0L, TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                (r, executor) -> {
                    AsposeTask job = getJob(r);

                    CompletableFuture<Boolean> completableFuture = callbacks.get(job.getId());
                    if (completableFuture != null && !completableFuture.isDone()) {
                        callbacks.remove(job.getId());
                        completableFuture.complete(false);
                    }
                });
    }

    private AsposeTask getJob(Runnable runnable) {
        try {
            Field field = runnable.getClass().getDeclaredField("fn");
            field.setAccessible(true);
            AsposeTaskSupplier supplier = (AsposeTaskSupplier) field.get(runnable);

            return supplier.getAsposeTask();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public interface AsposeTask extends Runnable {

        int getId();
    }

    private class AsposeTaskSupplier implements Supplier<Boolean> {

        private AsposeTaskSupplier(AsposeTask asposeTask) {
            this.asposeTask = asposeTask;
        }

        private AsposeTask asposeTask;

        @Override
        public Boolean get() {
            asposeTask.run();
            callbacks.remove(asposeTask.getId());

            return true;
        }

        private AsposeTask getAsposeTask() {
            return asposeTask;
        }
    }
}

