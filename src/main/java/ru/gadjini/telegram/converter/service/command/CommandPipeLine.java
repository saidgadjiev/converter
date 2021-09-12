package ru.gadjini.telegram.converter.service.command;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CommandPipeLine {

    private List<String> commands = new ArrayList<>();

    public void addCommand(String command) {
        commands.add(command);
    }

    public void addCommand(String[] command) {
        commands.add(String.join(" ", command));
    }

    public String[] build() {
        List<String> cmd = new ArrayList<>();
        cmd.add("bash");
        cmd.add("-c");
        StringBuilder pipe = new StringBuilder();
        for (Iterator<String> ite = commands.iterator(); ite.hasNext(); ) {
            pipe.append(ite.next());
            if (ite.hasNext()) {
                pipe.append(" | ");
            }
        }
        cmd.add(pipe.toString());

        return cmd.toArray(String[]::new);
    }
}
