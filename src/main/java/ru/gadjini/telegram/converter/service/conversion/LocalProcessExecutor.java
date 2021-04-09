package ru.gadjini.telegram.converter.service.conversion;

import org.springframework.stereotype.Service;
import ru.gadjini.telegram.smart.bot.commons.exception.ProcessException;
import ru.gadjini.telegram.smart.bot.commons.exception.ProcessTimedOutException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

@Service
public class LocalProcessExecutor {

    public void execute(int timeOutInSeconds, Runnable action) {
        CompletableFuture<Void> feature = CompletableFuture.runAsync(action);

        try {
            feature.get(timeOutInSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            feature.cancel(true);
            throw new ProcessTimedOutException();
        } catch (Exception e) {
            throw new ProcessException(e);
        }
    }

    public <T> T execute(int timeOutInSeconds, Supplier<T> action) {
        CompletableFuture<T> feature = CompletableFuture.supplyAsync(action);

        try {
            return feature.get(timeOutInSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            feature.cancel(true);
            throw new ProcessTimedOutException();
        } catch (Exception e) {
            throw new ProcessException(e);
        }
    }
}
