package ru.gadjini.telegram.converter.service.sox;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.smart.bot.commons.service.ProcessExecutor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class SoxService {

    private ProcessExecutor processExecutor;

    @Autowired
    public SoxService(ProcessExecutor processExecutor) {
        this.processExecutor = processExecutor;
    }

    public String getSampleRate(String filePath) throws InterruptedException {
        return processExecutor.executeWithResult(new String[]{
                "soxi", "-r", filePath
        });
    }

    public String getChannels(String filePath) throws InterruptedException {
        return processExecutor.executeWithResult(new String[]{
                "soxi", "-c", filePath
        });
    }

    public void convert(String in, String out, String... options) throws InterruptedException {
        List<String> cmd = new ArrayList<>();
        cmd.add("sox");
        cmd.add(in);
        cmd.addAll(Arrays.asList(options));
        cmd.add(out);

        processExecutor.executeWithResult(cmd.toArray(String[]::new));
    }

    public void mix(String in1, String in2, int startAtSeconds, Integer bitRate, String out) throws InterruptedException {
        processExecutor.executeWithResult(new String[]{
               "bash", "-c", bitRate == null ? "sox -m " + in1 + " '|sox " + in2 + " -p pad " + startAtSeconds + "'"  + out
                : "sox -m " + in1 + " '|sox " + in2 + " -p pad " + startAtSeconds + "' -C " + bitRate / 1000 + " "  + out
        });
    }
}
