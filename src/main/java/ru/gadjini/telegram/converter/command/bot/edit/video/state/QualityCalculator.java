package ru.gadjini.telegram.converter.command.bot.edit.video.state;

public class QualityCalculator {

    private QualityCalculator() {

    }

    public static int getQuality(EditVideoState editVideoState) {
        int currentOverallBitrate = editVideoState.getCurrentOverallBitrate();
        int audioOverallBitrate = editVideoState.getSettings().getAudioBitrate()
                .equals(EditVideoAudioBitrateState.AUTO) ? editVideoState.getCurrentAudioOverallBitrate()
                : editVideoState.getSettings().getParsedAudioBitrate() * editVideoState.getAudioStreamsCount();
        double estimatedOverallBitrate = editVideoState.getSettings().getVideoBitrate()
                + audioOverallBitrate;

        double factor = currentOverallBitrate / estimatedOverallBitrate;

        return (int) Math.round(EditVideoQualityState.MAX_QUALITY / factor);
    }

    public static int getQuality(EditVideoState editVideoState, int videoBitrate, int audioBitrate) {
        int currentOverallBitrate = editVideoState.getCurrentOverallBitrate();
        int audioOverallBitrate = audioBitrate * editVideoState.getAudioStreamsCount();
        double estimatedOverallBitrate = videoBitrate
                + audioOverallBitrate;

        double factor = currentOverallBitrate / estimatedOverallBitrate;

        return (int) Math.round(EditVideoQualityState.MAX_QUALITY / factor);
    }

    public static int getCompressionRate(EditVideoState editVideoState) {
        int quality = getQuality(editVideoState);

        return EditVideoQualityState.MAX_QUALITY - quality;
    }
}
