package ru.gadjini.telegram.converter.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;

import java.util.*;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.*;

@Configuration
public class FormatsConfiguration {

    public static final String ALL_CONVERTER = "all";

    public static final String DEFAULT_CONVERTER = "default";

    public static final String VIDEO_CONVERTER = "video";

    public static final String AUDIO_CONVERTER = "audio";

    private static final Map<FormatCategory, Map<List<Format>, List<Format>>> FORMATS = new LinkedHashMap<>();

    static {
        Map<List<Format>, List<Format>> audios = new HashMap<>();
        audios.put(List.of(M4B), List.of(AAC, AIFF, AMR, FLAC, M4A, MP3, OGG, OPUS, RA, SPX, VOICE, WAV, WMA, RM));
        audios.put(List.of(AMR), List.of(AAC, AIFF, FLAC, M4A, MP3, OGG, OPUS, RA, SPX, VOICE, WAV, WMA, RM));
        audios.put(List.of(OPUS), List.of(AAC, AIFF, AMR, FLAC, M4A, MP3, OGG, RA, SPX, VOICE, WAV, WMA, RM));
        audios.put(List.of(SPX), List.of(AAC, AIFF, AMR, FLAC, M4A, MP3, OGG, OPUS, RA, SPX, VOICE, WAV, WMA, RM));
        audios.put(List.of(OGG), List.of(AAC, AIFF, AMR, FLAC, M4A, MP3, OPUS, RA, SPX, VOICE, WAV, WMA, RM));
        audios.put(List.of(FLAC), List.of(AAC, AIFF, AMR, M4A, MP3, OGG, OPUS, RA, SPX, VOICE, WAV, WMA, RM));
        audios.put(List.of(M4A), List.of(AAC, AIFF, AMR, FLAC, MP3, OGG, OPUS, RA, SPX, VOICE, WAV, WMA, RM));
        audios.put(List.of(MP3), List.of(AAC, AIFF, AMR, FLAC, M4A, OGG, OPUS, RA, SPX, VOICE, WAV, WMA, RM));
        audios.put(List.of(AIFF), List.of(AAC, AMR, FLAC, M4A, MP3, OGG, OPUS, RA, SPX, VOICE, WAV, WMA, RM));
        audios.put(List.of(WMA), List.of(AAC, AIFF, AMR, FLAC, M4A, MP3, OGG, OPUS, RA, SPX, VOICE, WAV, RM));
        audios.put(List.of(MID), List.of(AAC, AIFF, AMR, FLAC, M4A, MP3, OGG, OPUS, RA, SPX, WAV, WMA, RM));
        audios.put(List.of(AAC), List.of(AIFF, AMR, FLAC, M4A, MP3, OGG, OPUS, RA, SPX, VOICE, WAV, WMA, RM));
        audios.put(List.of(RA), List.of(AAC, AIFF, AMR, FLAC, M4A, MP3, OGG, OPUS, SPX, VOICE, WAV, WMA, RM));
        audios.put(List.of(WAV), List.of(AAC, AIFF, AMR, FLAC, M4A, MP3, OGG, OPUS, RA, SPX, VOICE, WMA, RM));
        audios.put(List.of(RM), List.of(AMR, AIFF, FLAC, MP3, OGG, WAV, WMA, SPX, OPUS, M4A, VOICE, RA, AAC));
        FORMATS.put(FormatCategory.AUDIO, audios);

        Map<List<Format>, List<Format>> documents = new HashMap<>();
        documents.put(List.of(EPUB), List.of(AZW3, DOC, DOCX, FB2, HTMLZ, LIT, LRF, MOBI, OEB, PDB, PDF, PMLZ, RB, RTF, SNB, TCR, TXT, TXTZ, ZIP));
        documents.put(List.of(DOTM), List.of(DOC, DOCM, DOCX, DOT, DOTX, HTML, MHTML, ODT, OTT, PCL, PS, SVG, TXT, XPS));
        documents.put(List.of(PS), List.of(DOC, DOCX, HTML, PDF, PPTX, SVG, XML, XPS));
        documents.put(List.of(PPT), List.of(HTML, ODP, OTP, PDF, POT, POTM, POTX, PPS, PPSM, PPSX, PPTM, PPTX, SWF, TIFF, XPS));
        documents.put(List.of(POT), List.of(HTML, ODP, OTP, PDF, POTM, POTX, PPS, PPSM, PPSX, PPT, PPTM, PPTX, SWF, TIFF, XPS));
        documents.put(List.of(ODP), List.of(HTML, OTP, PDF, POT, POTM, POTX, PPS, PPSM, PPSX, PPT, PPTM, PPTX, SWF, TIFF, XPS));
        documents.put(List.of(CGM), List.of(DOC, DOCX, HTML, PDF, PPTX, SVG, XML, XPS));
        documents.put(List.of(PDF), List.of(DOC, DOCX, EPUB, HTML, PPTX, SVG, TIFF, XML, XPS, PNG, JPG));
        documents.put(List.of(RTF), List.of(AZW3, DOCX, EPUB, FB2, HTMLZ, LIT, LRF, MOBI, OEB, PDB, PDF, PMLZ, RB, SNB, TCR, TXT, TXTZ, ZIP));
        documents.put(List.of(PCL), List.of(DOC, DOCX, HTML, PDF, PPTX, SVG, XML, XPS));
        documents.put(List.of(PPSM), List.of(HTML, ODP, OTP, PDF, POT, POTM, POTX, PPS, PPSX, PPT, PPTM, PPTX, SWF, TIFF, XPS));
        documents.put(List.of(DOCX), List.of(DOC, DOCM, DOT, DOTM, DOTX, EPUB, HTML, MHTML, ODT, OTT, PCL, PDF, PS, RTF, SVG, TIFF, TXT, XPS));
        documents.put(List.of(POTM), List.of(HTML, ODP, OTP, PDF, POT, POTX, PPS, PPSM, PPSX, PPT, PPTM, PPTX, SWF, TIFF, XPS));
        documents.put(List.of(SXC), List.of(CSV, DIF, FODS, HTML, MHTML, NUMBERS, ODS, PDF, SVG, TIFF, TSV, XLAM, XLS, XLSB, XLSM, XLSX, XLTM, XLTX, XPS));
        documents.put(List.of(MOBI), List.of(AZW3, DOCX, EPUB, FB2, HTMLZ, LIT, LRF, OEB, PDB, PDF, PMLZ, RB, RTF, SNB, TCR, TXT, TXTZ, ZIP));
        documents.put(List.of(CHM), List.of(AZW3, DOCX, EPUB, FB2, HTMLZ, LIT, LRF, MOBI, OEB, PDB, PDF, PMLZ, RB, RTF, SNB, TCR, TXT, TXTZ, ZIP));
        documents.put(List.of(XML), List.of(DOC, DOCX, PDF, PDF_IMPORT, TXT));
        documents.put(List.of(CBZ), List.of(AZW3, DOCX, EPUB, FB2, HTMLZ, LIT, LRF, MOBI, OEB, PDB, PDF, PMLZ, RB, RTF, SNB, TCR, TXT, TXTZ, ZIP));
        documents.put(List.of(FODS), List.of(CSV, DIF, HTML, MHTML, NUMBERS, ODS, PDF, SVG, SXC, TIFF, TSV, XLAM, XLS, XLSB, XLSM, XLSX, XLTM, XLTX, XPS));
        documents.put(List.of(PPS), List.of(HTML, ODP, OTP, PDF, POT, POTM, POTX, PPSM, PPSX, PPT, PPTM, PPTX, SWF, TIFF, XPS));
        documents.put(List.of(PRC), List.of(AZW3, DOCX, EPUB, FB2, HTMLZ, LIT, LRF, MOBI, OEB, PDB, PDF, PMLZ, RB, RTF, SNB, TCR, TXT, TXTZ, ZIP));
        documents.put(List.of(TCR), List.of(AZW3, DOCX, EPUB, FB2, HTMLZ, LIT, LRF, MOBI, OEB, PDB, PDF, PMLZ, RB, RTF, SNB, TXT, TXTZ, ZIP));
        documents.put(List.of(PPSX), List.of(HTML, ODP, OTP, PDF, POT, POTM, POTX, PPS, PPSM, PPT, PPTM, PPTX, SWF, TIFF, XPS));
        documents.put(List.of(DOTX), List.of(DOC, DOCM, DOCX, DOT, DOTM, HTML, MHTML, ODT, OTT, PCL, PS, SVG, TXT, XPS));
        documents.put(List.of(TSV), List.of(CSV, DIF, FODS, HTML, MHTML, NUMBERS, ODS, PDF, SVG, SXC, TIFF, XLAM, XLS, XLSB, XLSM, XLSX, XLTM, XLTX, XPS));
        documents.put(List.of(POTX), List.of(HTML, ODP, OTP, PDF, POT, POTM, PPS, PPSM, PPSX, PPT, PPTM, PPTX, SWF, TIFF, XPS));
        documents.put(List.of(LRF), List.of(AZW3, DOCX, EPUB, FB2, HTMLZ, LIT, MOBI, OEB, PDB, PDF, PMLZ, RB, RTF, SNB, TCR, TXT, TXTZ, ZIP));
        documents.put(List.of(CBC), List.of(AZW3, DOCX, EPUB, FB2, HTMLZ, LIT, LRF, MOBI, OEB, PDB, PDF, PMLZ, RB, RTF, SNB, TCR, TXT, TXTZ, ZIP));
        documents.put(List.of(RB), List.of(AZW3, DOCX, EPUB, FB2, HTMLZ, LIT, LRF, MOBI, OEB, PDB, PDF, PMLZ, RTF, SNB, TCR, TXT, TXTZ, ZIP));
        documents.put(List.of(FB2), List.of(AZW3, DOCX, EPUB, HTMLZ, LIT, LRF, MOBI, OEB, PDB, PDF, PMLZ, RB, RTF, SNB, TCR, TXT, TXTZ, ZIP));
        documents.put(List.of(TXT), List.of(CSV, DOC, DOCM, DOCX, DOT, DOTM, DOTX, HTML, MHTML, ODT, OTT, PCL, PDF, PS, SVG, TXT, XML, XPS));
        documents.put(List.of(ODS), List.of(CSV, DIF, FODS, HTML, MHTML, NUMBERS, PDF, SVG, SXC, TIFF, TSV, XLAM, XLS, XLSB, XLSM, XLSX, XLTM, XLTX, XPS));
        documents.put(List.of(DJVU), List.of(AZW3, DOCX, EPUB, FB2, HTMLZ, LIT, LRF, MOBI, OEB, PDB, PDF, PMLZ, RB, RTF, SNB, TCR, TXT, TXTZ, ZIP));
        documents.put(List.of(LIT), List.of(AZW3, DOCX, EPUB, FB2, HTMLZ, LRF, MOBI, OEB, PDB, PDF, PMLZ, RB, RTF, SNB, TCR, TXT, TXTZ, ZIP));
        documents.put(List.of(TXTZ), List.of(AZW3, DOCX, EPUB, FB2, HTMLZ, LIT, LRF, MOBI, OEB, PDB, PDF, PMLZ, RB, RTF, SNB, TCR, TXT, ZIP));
        documents.put(List.of(AZW), List.of(AZW3, DOCX, EPUB, FB2, HTMLZ, LIT, LRF, MOBI, OEB, PDB, PDF, PMLZ, RB, RTF, SNB, TCR, TXT, TXTZ, ZIP));
        documents.put(List.of(AZW3), List.of(DOCX, EPUB, FB2, HTMLZ, LIT, LRF, MOBI, OEB, PDB, PDF, PMLZ, RB, RTF, SNB, TCR, TXT, TXTZ, ZIP));
        documents.put(List.of(PPTX), List.of(HTML, ODP, OTP, PDF, POT, POTM, POTX, PPS, PPSM, PPSX, PPT, PPTM, SWF, TIFF, XPS));
        documents.put(List.of(PML), List.of(AZW3, DOCX, EPUB, FB2, HTMLZ, LIT, LRF, MOBI, OEB, PDB, PDF, PMLZ, RB, RTF, SNB, TCR, TXT, TXTZ, ZIP));
        documents.put(List.of(MHT), List.of(DOC, DOCX, HTML, PDF, PPTX, SVG, XML, XPS));
        documents.put(List.of(PDB), List.of(AZW3, DOCX, EPUB, FB2, HTMLZ, LIT, LRF, MOBI, OEB, PDF, PMLZ, RB, RTF, SNB, TCR, TXT, TXTZ, ZIP));
        documents.put(List.of(XLSB), List.of(CSV, DIF, FODS, HTML, MHTML, NUMBERS, ODS, PDF, SVG, SXC, TIFF, TSV, XLAM, XLS, XLSM, XLSX, XLTM, XLTX, XPS));
        documents.put(List.of(PPTM), List.of(HTML, ODP, OTP, PDF, POT, POTM, POTX, PPS, PPSM, PPSX, PPT, PPTX, SWF, TIFF, XPS));
        documents.put(List.of(DOC), List.of(DOCM, DOCX, DOT, DOTM, DOTX, EPUB, HTML, MHTML, ODT, OTT, PCL, PDF, PS, RTF, SVG, TIFF, TXT, XPS));
        documents.put(List.of(XLS), List.of(CSV, DIF, FODS, HTML, MHTML, NUMBERS, ODS, PDF, SVG, SXC, TIFF, TSV, XLAM, XLSB, XLSM, XLSX, XLTM, XLTX, XPS));
        documents.put(List.of(MHTML), List.of(CSV, DIF, DOC, DOCM, DOCX, DOT, DOTM, DOTX, FODS, HTML, NUMBERS, ODS, ODT, OTT, PCL, PS, SVG, SXC, TSV, TXT, XLAM, XLS, XLSB, XLSM, XLSX, XLTM, XLTX, XPS));
        documents.put(List.of(XLSX), List.of(CSV, DIF, FODS, HTML, MHTML, NUMBERS, ODS, PDF, SVG, SXC, TIFF, TSV, XLAM, XLS, XLSB, XLSM, XLTM, XLTX, XPS));
        documents.put(List.of(ODT), List.of(AZW3, DOC, DOCM, DOCX, DOT, DOTM, DOTX, EPUB, FB2, HTML, HTMLZ, LIT, LRF, MHTML, MOBI, OEB, OTT, PCL, PDB, PDF, PMLZ, PS, RB, RTF, SNB, SVG, TCR, TXT, TXTZ, XPS, ZIP));
        documents.put(List.of(CBR), List.of(AZW3, DOCX, EPUB, FB2, HTMLZ, LIT, LRF, MOBI, OEB, PDB, PDF, PMLZ, RB, RTF, SNB, TCR, TXT, TXTZ, ZIP));
        documents.put(List.of(DOCM), List.of(DOC, DOCX, DOT, DOTM, DOTX, HTML, MHTML, ODT, OTT, PCL, PS, SVG, TXT, XPS));
        documents.put(List.of(FBZ), List.of(AZW3, DOCX, EPUB, FB2, HTMLZ, LIT, LRF, MOBI, OEB, PDB, PDF, PMLZ, RB, RTF, SNB, TCR, TXT, TXTZ, ZIP));
        documents.put(List.of(OTP), List.of(HTML, ODP, PDF, POT, POTM, POTX, PPS, PPSM, PPSX, PPT, PPTM, PPTX, SWF, TIFF, XPS));
        documents.put(List.of(AZW4), List.of(AZW3, DOCX, EPUB, FB2, HTMLZ, LIT, LRF, MOBI, OEB, PDB, PDF, PMLZ, RB, RTF, SNB, TCR, TXT, TXTZ, ZIP));
        documents.put(List.of(DOT), List.of(DOC, DOCM, DOCX, DOTM, DOTX, HTML, MHTML, ODT, OTT, PCL, PS, SVG, TXT, XPS));
        documents.put(List.of(OTT), List.of(DOC, DOCM, DOCX, DOT, DOTM, DOTX, HTML, MHTML, ODT, PCL, PS, SVG, TXT, XPS));
        documents.put(List.of(SNB), List.of(AZW3, DOCX, EPUB, FB2, HTMLZ, LIT, LRF, MOBI, OEB, PDB, PDF, PMLZ, RB, RTF, TCR, TXT, TXTZ, ZIP));
        documents.put(List.of(NUMBERS), List.of(CSV, DIF, FODS, HTML, MHTML, ODS, PDF, SVG, SXC, TIFF, TSV, XLAM, XLS, XLSB, XLSM, XLSX, XLTM, XLTX, XPS));
        documents.put(List.of(TEXT), List.of(CSV, DOC, DOCX, PDF, TXT, XML));
        documents.put(List.of(CSV), List.of(DIF, FODS, HTML, MHTML, NUMBERS, ODS, PDF, SVG, SXC, TIFF, TSV, TXT, XLAM, XLS, XLSB, XLSM, XLSX, XLTM, XLTX, XPS));
        documents.put(List.of(XPS), List.of(DOC, DOCX, HTML, PDF, PPTX, SVG, XML, XPS));
        documents.put(List.of(PDFS), List.of(MERGE_PDFS));
        FORMATS.put(FormatCategory.DOCUMENTS, documents);

        Map<List<Format>, List<Format>> images = new HashMap<>();
        images.put(List.of(ICO), List.of(BMP, HEIC, HEIF, JP2, JPG, PDF, PNG, STICKER, TIFF, WEBP));
        images.put(List.of(TIFF), List.of(DOC, DOCX, PDF));
        images.put(List.of(BMP), List.of(DOC, DOCX, HEIC, HEIF, ICO, JP2, JPG, PDF, PNG, STICKER, SVG, TIFF, WEBP));
        images.put(List.of(SVG), List.of(BMP, DOC, DOCX, HEIC, HEIF, HTML, ICO, JP2, JPG, PDF, PNG, PPTX, STICKER, SVG, TIFF, WEBP, XML, XPS));
        images.put(List.of(IMAGES), List.of(DOC, DOCX, PDF, TIFF));
        images.put(List.of(JPG), List.of(BMP, DOC, DOCX, HEIC, HEIF, ICO, JP2, PDF, PNG, STICKER, SVG, TIFF, WEBP));
        images.put(List.of(PHOTO), List.of(BMP, DOC, DOCX, HEIC, HEIF, ICO, JP2, JPG, PDF, STICKER, SVG, TIFF, WEBP));
        images.put(List.of(JP2), List.of(BMP, DOC, DOCX, HEIC, HEIF, ICO, JPG, PDF, PNG, STICKER, SVG, TIFF, WEBP));
        images.put(List.of(HEIF), List.of(BMP, DOC, DOCX, ICO, JP2, JPG, PDF, STICKER, SVG, TIFF, WEBP));
        images.put(List.of(TGS), List.of(GIF));
        images.put(List.of(HEIC), List.of(BMP, DOC, DOCX, ICO, JP2, JPG, PDF, STICKER, SVG, TIFF, WEBP));
        images.put(List.of(PNG), List.of(BMP, DOC, DOCX, HEIC, HEIF, ICO, JP2, JPG, PDF, STICKER, SVG, TIFF, WEBP));
        images.put(List.of(WEBP), List.of(BMP, DOC, DOCX, HEIC, HEIF, ICO, JP2, JPG, PDF, PNG, STICKER, SVG, TIFF));
        images.put(List.of(IMAGES), List.of(PDF, TIFF, DOC, DOCX));
        FORMATS.put(FormatCategory.IMAGES, images);

        Map<List<Format>, List<Format>> videos = new HashMap<>();
        videos.put(List.of(MOV), List.of(AAC, AIFF, AMR, AVI, COMPRESS, FLAC, FLV, M4A, M4V, MKV, MP3, MP4, MPEG, MPG, MTS, OGG, OPUS, RA, SPX, TS, VOB, VOICE, WAV, WEBM, WMA, WMV, _3GP));
        videos.put(List.of(VOB), List.of(AAC, AIFF, AMR, AVI, COMPRESS, FLAC, FLV, M4A, M4V, MKV, MOV, MP3, MP4, MPEG, MPG, MTS, OGG, OPUS, RA, SPX, TS, VOICE, WAV, WEBM, WMA, WMV, _3GP));
        videos.put(List.of(AVI), List.of(AAC, AIFF, AMR, COMPRESS, FLAC, FLV, M4A, M4V, MKV, MOV, MP3, MP4, MPEG, MPG, MTS, OGG, OPUS, RA, SPX, TS, VOB, VOICE, WAV, WEBM, WMA, WMV, _3GP));
        videos.put(List.of(M4V), List.of(AAC, AIFF, AMR, AVI, COMPRESS, FLAC, FLV, M4A, MKV, MOV, MP3, MP4, MPEG, MPG, MTS, OGG, OPUS, RA, SPX, TS, VOB, VOICE, WAV, WEBM, WMA, WMV, _3GP));
        videos.put(List.of(_3GP), List.of(AAC, AIFF, AMR, AVI, COMPRESS, FLAC, FLV, M4A, M4V, MKV, MOV, MP3, MP4, MPEG, MPG, MTS, OGG, OPUS, RA, SPX, TS, VOB, VOICE, WAV, WEBM, WMA, WMV));
        videos.put(List.of(MPEG), List.of(AAC, AIFF, AMR, AVI, COMPRESS, FLAC, FLV, M4A, M4V, MKV, MOV, MP3, MP4, MPG, MTS, OGG, OPUS, RA, SPX, TS, VOB, VOICE, WAV, WEBM, WMA, WMV, _3GP));
        videos.put(List.of(TS), List.of(AAC, AIFF, AMR, AVI, FLAC, FLV, M4A, M4V, MKV, MOV, MP3, MP4, MPEG, MPG, MTS, OGG, OPUS, RA, SPX, VOB, VOICE, WAV, WEBM, WMA, WMV, _3GP));
        videos.put(List.of(MP4), List.of(AAC, AIFF, AMR, AVI, COMPRESS, FLAC, FLV, M4A, M4V, MKV, MOV, MP3, MPEG, MPG, MTS, OGG, OPUS, RA, SPX, TS, VOB, VOICE, WAV, WEBM, WMA, WMV, _3GP));
        videos.put(List.of(WMV), List.of(AAC, AIFF, AMR, AVI, COMPRESS, FLAC, FLV, M4A, M4V, MKV, MOV, MP3, MP4, MPEG, MPG, MTS, OGG, OPUS, RA, SPX, TS, VOB, VOICE, WAV, WMA, WMV, _3GP));
        videos.put(List.of(WEBM), List.of(AVI, COMPRESS, FLV, M4V, MKV, MOV, MP4, MPEG, MPG, MTS, TS, VOB, WMV, _3GP));
        videos.put(List.of(MTS), List.of(AAC, AIFF, AMR, AVI, COMPRESS, FLAC, FLV, M4A, M4V, MKV, MOV, MP3, MP4, MPEG, MPG, OGG, OPUS, RA, SPX, TS, VOB, VOICE, WAV, WEBM, WMA, WMV, _3GP));
        videos.put(List.of(MPG), List.of(AAC, AIFF, AMR, AVI, COMPRESS, FLAC, FLV, M4A, M4V, MKV, MOV, MP3, MP4, MPEG, MTS, OGG, OPUS, RA, SPX, TS, VOB, VOICE, WAV, WEBM, WMA, WMV, _3GP));
        videos.put(List.of(MKV), List.of(AAC, AIFF, AMR, AVI, COMPRESS, FLAC, FLV, M4A, M4V, MOV, MP3, MP4, MPEG, MPG, MTS, OGG, OPUS, RA, SPX, TS, VOB, VOICE, WAV, WEBM, WMA, WMV, _3GP));
        videos.put(List.of(FLV), List.of(AAC, AIFF, AMR, AVI, COMPRESS, FLAC, M4A, M4V, MKV, MOV, MP3, MP4, MPEG, MPG, MTS, OGG, OPUS, RA, SPX, TS, VOB, VOICE, WAV, WEBM, WMA, WMV, _3GP));
        FORMATS.put(FormatCategory.VIDEO, videos);

        Map<List<Format>, List<Format>> web = new HashMap<>();
        web.put(List.of(URL), List.of(HTML, PDF, PNG));
        web.put(List.of(HTMLZ), List.of(AZW3, DOCX, EPUB, FB2, LIT, LRF, MOBI, OEB, PDB, PDF, PMLZ, RB, RTF, SNB, TCR, TXT, TXTZ, ZIP));
        web.put(List.of(HTML), List.of(CSV, DIF, FODS, NUMBERS, ODS, PDF, PNG, SXC, TSV, XLAM, XLS, XLSB, XLSM, XLSX, XLTM, XLTX, XPS));
        FORMATS.put(FormatCategory.WEB, web);
    }

    @Bean
    @ConditionalOnProperty(
            value = "converter",
            havingValue = VIDEO_CONVERTER
    )
    public Map<FormatCategory, Map<List<Format>, List<Format>>> videoFormats() {
        return Map.of(FormatCategory.VIDEO, FORMATS.get(FormatCategory.VIDEO));
    }

    @Bean
    @ConditionalOnProperty(
            value = "converter",
            havingValue = AUDIO_CONVERTER
    )
    public Map<FormatCategory, Map<List<Format>, List<Format>>> audioFormats() {
        return Map.of(FormatCategory.AUDIO, FORMATS.get(FormatCategory.AUDIO));
    }

    @Bean
    @ConditionalOnProperty(
            value = "converter",
            havingValue = DEFAULT_CONVERTER
    )
    public Map<FormatCategory, Map<List<Format>, List<Format>>> defaultFormats() {
        return Map.of(
                FormatCategory.DOCUMENTS, FORMATS.get(FormatCategory.DOCUMENTS),
                FormatCategory.IMAGES, FORMATS.get(FormatCategory.IMAGES),
                FormatCategory.WEB, FORMATS.get(FormatCategory.WEB)
        );
    }

    @Bean
    @ConditionalOnProperty(
            value = "converter",
            havingValue = ALL_CONVERTER,
            matchIfMissing = true
    )
    public Map<FormatCategory, Map<List<Format>, List<Format>>> allFormats() {
        return FORMATS;
    }

    @Bean
    public Set<FormatCategory> formatCategories(Map<FormatCategory, Map<List<Format>, List<Format>>> formats) {
        return formats.keySet();
    }
}
