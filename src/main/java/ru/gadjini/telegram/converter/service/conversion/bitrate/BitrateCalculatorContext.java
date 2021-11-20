package ru.gadjini.telegram.converter.service.conversion.bitrate;

import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.converter.service.mediainfo.MediaInfoService;

import java.util.List;

public class BitrateCalculatorContext {

    private String in;

    private FFprobeDevice.WHD whd;

    private List<FFprobeDevice.FFProbeStream> streams;

    private List<MediaInfoService.MediaInfoTrack> mediaInfoTracks;

    private Integer overallBitrate;

    private boolean manualBitrateCalculated;

    private Integer videoBitrate;

    private Integer audioBitrate;

    private Integer imageVideoBitrate;

    public String getIn() {
        return in;
    }

    public BitrateCalculatorContext setIn(String in) {
        this.in = in;

        return this;
    }

    public void setMediaInfoTracks(List<MediaInfoService.MediaInfoTrack> tracks) {
        this.mediaInfoTracks = tracks;
    }

    public List<MediaInfoService.MediaInfoTrack> getMediaInfoTracks() {
        return mediaInfoTracks;
    }

    public List<FFprobeDevice.FFProbeStream> getStreams() {
        return streams;
    }

    public BitrateCalculatorContext setStreams(List<FFprobeDevice.FFProbeStream> streams) {
        this.streams = streams;

        return this;
    }

    public FFprobeDevice.WHD getWhd() {
        return whd;
    }

    public void setWhd(FFprobeDevice.WHD whd) {
        this.whd = whd;
    }

    public Integer getImageVideoBitrate() {
        return imageVideoBitrate;
    }

    public void setImageVideoBitrate(Integer imageVideoBitrate) {
        this.imageVideoBitrate = imageVideoBitrate;
    }

    public Integer getAudioBitrate() {
        return audioBitrate;
    }

    public void setAudioBitrate(Integer audioBitrate) {
        this.audioBitrate = audioBitrate;
    }

    public Integer getVideoBitrate() {
        return videoBitrate;
    }

    public void setVideoBitrate(Integer videoBitrate) {
        this.videoBitrate = videoBitrate;
    }

    public Integer getOverallBitrate() {
        return overallBitrate;
    }

    public void setOverallBitrate(Integer overallBitrate) {
        this.overallBitrate = overallBitrate;
    }

    public boolean isManualBitrateCalculated() {
        return manualBitrateCalculated;
    }

    public void setManualBitrateCalculated(boolean manualBitrateCalculated) {
        this.manualBitrateCalculated = manualBitrateCalculated;
    }
}
