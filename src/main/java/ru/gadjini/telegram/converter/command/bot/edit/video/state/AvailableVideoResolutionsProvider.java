package ru.gadjini.telegram.converter.command.bot.edit.video.state;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class AvailableVideoResolutionsProvider {

    public static List<Integer> getPermittedVideoResolutions(int currentResolution, Collection<Integer> resolutions) {
        return resolutions.stream().filter(f -> f <= currentResolution).collect(Collectors.toList());
    }
}
