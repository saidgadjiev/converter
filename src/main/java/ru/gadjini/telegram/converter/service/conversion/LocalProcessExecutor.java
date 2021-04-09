package ru.gadjini.telegram.converter.service.conversion;

import org.springframework.stereotype.Service;
import ru.gadjini.telegram.smart.bot.commons.exception.ProcessException;
import ru.gadjini.telegram.smart.bot.commons.exception.ProcessTimedOutException;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

@Service
public class LocalProcessExecutor {

    public <T> T execute(int timeOutInSeconds, Supplier<T> action, Runnable interrupter) {
        Future<T> future = Executors.newSingleThreadExecutor().submit(action::get);

        try {
            return future.get(timeOutInSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            interrupter.run();

            throw new ProcessTimedOutException();
        } catch (Exception e) {
            throw new ProcessException(e);
        }
    }
}
