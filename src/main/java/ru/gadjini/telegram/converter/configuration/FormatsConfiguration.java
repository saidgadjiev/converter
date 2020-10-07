package ru.gadjini.telegram.converter.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.*;

@Configuration
public class FormatsConfiguration {

    private static final Map<FormatCategory, Map<List<Format>, List<Format>>> FORMATS = new LinkedHashMap<>();

    static {
        Map<List<Format>, List<Format>> documents = new LinkedHashMap<>();
        documents.put(List.of(DOC), List.of(PDF, DOCX, TXT, EPUB, RTF, TIFF));
        documents.put(List.of(DOCX), List.of(PDF, DOC, TXT, EPUB, RTF, TIFF));
        documents.put(List.of(PDF), List.of(DOC, DOCX, EPUB, TIFF));
        documents.put(List.of(TEXT), List.of(PDF, DOC, DOCX, TXT));
        documents.put(List.of(TXT), List.of(PDF, DOC, DOCX));
        documents.put(List.of(XLS, XLSX), List.of(PDF));
        documents.put(List.of(PPTX, PPT, PPTM, POTX, POT, POTM, PPS, PPSX, PPSM), List.of(PDF));
        documents.put(List.of(AZW), List.of(AZW3, EPUB, DOCX, FB2, HTMLZ, OEB, LIT, LRF, MOBI, PDB, PMLZ, RB, PDF, RTF, SNB, TCR, TXT, TXTZ, ZIP));
        documents.put(List.of(AZW3), List.of(EPUB, DOCX, FB2, HTMLZ, OEB, LIT, LRF, MOBI, PDB, PMLZ, RB, PDF, RTF, SNB, TCR, TXT, TXTZ, ZIP));
        documents.put(List.of(AZW4), List.of(AZW3, EPUB, DOCX, FB2, HTMLZ, OEB, LIT, LRF, MOBI, PDB, PMLZ, RB, PDF, RTF, SNB, TCR, TXT, TXTZ, ZIP));
        documents.put(List.of(CBZ), List.of(AZW3, EPUB, DOCX, FB2, HTMLZ, OEB, LIT, LRF, MOBI, PDB, PMLZ, RB, PDF, RTF, SNB, TCR, TXT, TXTZ, ZIP));
        documents.put(List.of(CBR), List.of(AZW3, EPUB, DOCX, FB2, HTMLZ, OEB, LIT, LRF, MOBI, PDB, PMLZ, RB, PDF, RTF, SNB, TCR, TXT, TXTZ, ZIP));
        documents.put(List.of(CBC), List.of(AZW3, EPUB, DOCX, FB2, HTMLZ, OEB, LIT, LRF, MOBI, PDB, PMLZ, RB, PDF, RTF, SNB, TCR, TXT, TXTZ, ZIP));
        documents.put(List.of(CHM), List.of(AZW3, EPUB, DOCX, FB2, HTMLZ, OEB, LIT, LRF, MOBI, PDB, PMLZ, RB, PDF, RTF, SNB, TCR, TXT, TXTZ, ZIP));
        documents.put(List.of(DJVU), List.of(AZW3, EPUB, DOCX, FB2, HTMLZ, OEB, LIT, LRF, MOBI, PDB, PMLZ, RB, PDF, RTF, SNB, TCR, TXT, TXTZ, ZIP));
        documents.put(List.of(EPUB), List.of(AZW3, FB2, HTMLZ, OEB, LIT, LRF, MOBI, PDB, PMLZ, RB, PDF, RTF, SNB, TCR, TXT, TXTZ, ZIP));
        documents.put(List.of(FB2), List.of(AZW3, EPUB, DOCX, HTMLZ, OEB, LIT, LRF, MOBI, PDB, PMLZ, RB, PDF, RTF, SNB, TCR, TXT, TXTZ, ZIP));
        documents.put(List.of(FBZ), List.of(AZW3, EPUB, DOCX, FB2, HTMLZ, OEB, LIT, LRF, MOBI, PDB, PMLZ, RB, PDF, RTF, SNB, TCR, TXT, TXTZ, ZIP));
        documents.put(List.of(HTMLZ), List.of(AZW3, EPUB, DOCX, FB2, OEB, LIT, LRF, MOBI, PDB, PMLZ, RB, PDF, RTF, SNB, TCR, TXT, TXTZ, ZIP));
        documents.put(List.of(LIT), List.of(AZW3, EPUB, DOCX, FB2, HTMLZ, OEB, LRF, MOBI, PDB, PMLZ, RB, PDF, RTF, SNB, TCR, TXT, TXTZ, ZIP));
        documents.put(List.of(LRF), List.of(AZW3, EPUB, DOCX, FB2, HTMLZ, OEB, LIT, MOBI, PDB, PMLZ, RB, PDF, RTF, SNB, TCR, TXT, TXTZ, ZIP));
        documents.put(List.of(MOBI), List.of(AZW3, EPUB, DOCX, FB2, HTMLZ, OEB, LIT, LRF, PDB, PMLZ, RB, PDF, RTF, SNB, TCR, TXT, TXTZ, ZIP));
        documents.put(List.of(ODT), List.of(AZW3, EPUB, DOCX, FB2, HTMLZ, OEB, LIT, LRF, MOBI, PDB, PMLZ, RB, PDF, RTF, SNB, TCR, TXT, TXTZ, ZIP));
        documents.put(List.of(PRC), List.of(AZW3, EPUB, DOCX, FB2, HTMLZ, OEB, LIT, LRF, MOBI, PDB, PMLZ, RB, PDF, RTF, SNB, TCR, TXT, TXTZ, ZIP));
        documents.put(List.of(PDB), List.of(AZW3, EPUB, DOCX, FB2, HTMLZ, OEB, LIT, LRF, MOBI, PMLZ, RB, PDF, RTF, SNB, TCR, TXT, TXTZ, ZIP));
        documents.put(List.of(PML), List.of(AZW3, EPUB, DOCX, FB2, HTMLZ, OEB, LIT, LRF, MOBI, PDB, PMLZ, RB, PDF, RTF, SNB, TCR, TXT, TXTZ, ZIP));
        documents.put(List.of(RB), List.of(AZW3, EPUB, DOCX, FB2, HTMLZ, OEB, LIT, LRF, MOBI, PDB, PMLZ, PDF, RTF, SNB, TCR, TXT, TXTZ, ZIP));
        documents.put(List.of(SNB), List.of(AZW3, EPUB, DOCX, FB2, HTMLZ, OEB, LIT, LRF, MOBI, PDB, PMLZ, RB, PDF, RTF, TCR, TXT, TXTZ, ZIP));
        documents.put(List.of(TCR), List.of(AZW3, EPUB, DOCX, FB2, HTMLZ, OEB, LIT, LRF, MOBI, PDB, PMLZ, RB, PDF, RTF, SNB, TXT, TXTZ, ZIP));
        documents.put(List.of(TXTZ), List.of(AZW3, EPUB, DOCX, FB2, HTMLZ, OEB, LIT, LRF, MOBI, PDB, PMLZ, RB, PDF, RTF, SNB, TCR, TXT, ZIP));

        FORMATS.put(FormatCategory.DOCUMENTS, documents);

        Map<List<Format>, List<Format>> web = new LinkedHashMap<>();

        web.put(List.of(URL), List.of(PDF, PNG, HTML));
        web.put(List.of(HTML), List.of(PDF, PNG));
        web.put(List.of(HTMLZ), List.of(AZW3, EPUB, DOCX, FB2, OEB, LIT, LRF, MOBI, PDB, PMLZ, RB, PDF, RTF, SNB, TCR, TXT, TXTZ, ZIP));

        FORMATS.put(FormatCategory.WEB, web);

        Map<List<Format>, List<Format>> images = new LinkedHashMap<>();
        images.put(List.of(PNG), List.of(PDF, DOC, DOCX, JPG, JP2, BMP, WEBP, TIFF, ICO, HEIC, HEIF, SVG, STICKER));
        images.put(List.of(PHOTO), List.of(PDF, DOC, DOCX, PNG, JPG, JP2, BMP, WEBP, TIFF, ICO, HEIC, HEIF, SVG, STICKER));
        images.put(List.of(JPG), List.of(PDF, DOC, DOCX, PNG, JP2, BMP, WEBP, TIFF, ICO, HEIC, HEIF, SVG, STICKER));
        images.put(List.of(TIFF), List.of(PDF, DOCX, DOC));
        images.put(List.of(BMP), List.of(PDF, PNG, JPG, JP2, WEBP, TIFF, ICO, HEIC, HEIF, SVG, STICKER));
        images.put(List.of(WEBP), List.of(PDF, PNG, JPG, JP2, BMP, TIFF, ICO, HEIC, HEIF, SVG, STICKER));
        images.put(List.of(SVG), List.of(PDF, PNG, JPG, JP2, BMP, WEBP, TIFF, ICO, HEIC, HEIF, STICKER));
        images.put(List.of(HEIC, HEIF), List.of(PDF, PNG, JPG, JP2, BMP, WEBP, TIFF, ICO, SVG, STICKER));
        images.put(List.of(ICO), List.of(PDF, PNG, JPG, JP2, BMP, WEBP, TIFF, HEIC, HEIF, SVG, STICKER));
        images.put(List.of(JP2), List.of(PDF, PNG, JPG, BMP, WEBP, TIFF, ICO, HEIC, HEIF, SVG, STICKER));
        images.put(List.of(TGS), List.of(GIF));
        images.put(List.of(IMAGES), List.of(PDF, TIFF, DOC, DOCX));
        FORMATS.put(FormatCategory.IMAGES, images);

        Map<List<Format>, List<Format>> videos = new LinkedHashMap<>();
        videos.put(List.of(MP4), List.of(_3GP, AVI, FLV, M4V, MKV, MOV, MPEG, MPG, MTS, VOB, WEBM, WMV, COMPRESS));
        videos.put(List.of(_3GP), List.of(MP4, AVI, FLV, M4V, MKV, MOV, MPEG, MPG, MTS, VOB, WEBM, WMV, COMPRESS));
        videos.put(List.of(AVI), List.of(MP4, _3GP, FLV, M4V, MKV, MOV, MPEG, MPG, MTS, VOB, WEBM, WMV, COMPRESS));
        videos.put(List.of(FLV), List.of(MP4, _3GP, AVI, M4V, MKV, MOV, MPEG, MPG, MTS, VOB, WEBM, WMV, COMPRESS));
        videos.put(List.of(M4V), List.of(MP4, _3GP, AVI, FLV, MKV, MOV, MPEG, MPG, MTS, VOB, WEBM, WMV, COMPRESS));
        videos.put(List.of(MKV), List.of(MP4, _3GP, AVI, FLV, M4V, MOV, MPEG, MPG, MTS, VOB, WEBM, WMV, COMPRESS));
        videos.put(List.of(MOV), List.of(MP4, _3GP, AVI, FLV, M4V, MKV, MPEG, MPG, MTS, VOB, WEBM, WMV, COMPRESS));
        videos.put(List.of(MPEG), List.of(MP4, _3GP, AVI, FLV, M4V, MKV, MOV, MPG, MTS, VOB, WEBM, WMV, COMPRESS));
        videos.put(List.of(MPG), List.of(MP4, _3GP, AVI, FLV, M4V, MKV, MOV, MPEG, MTS, VOB, WEBM, WMV, COMPRESS));
        videos.put(List.of(MTS), List.of(MP4, _3GP, AVI, FLV, M4V, MKV, MOV, MPEG, MPG, VOB, WEBM, WMV, COMPRESS));
        videos.put(List.of(VOB), List.of(MP4, _3GP, AVI, FLV, M4V, MKV, MOV, MPEG, MPG, MTS, WEBM, WMV, COMPRESS));
        videos.put(List.of(WEBM), List.of(MP4, _3GP, AVI, FLV, M4V, MKV, MOV, MPEG, MPG, MTS, VOB, WMV, COMPRESS));
        videos.put(List.of(WMV), List.of(MP4, _3GP, AVI, FLV, M4V, MKV, MOV, MPEG, MPG, MTS, VOB, WMV, COMPRESS));

        FORMATS.put(FormatCategory.VIDEO, videos);
    }

    @Bean
    @ConditionalOnProperty(
            value = "converter",
            havingValue = "video"
    )
    public Map<FormatCategory, Map<List<Format>, List<Format>>> videoFormats() {
        return Map.of(FormatCategory.VIDEO, FORMATS.get(FormatCategory.VIDEO));
    }

    @Bean
    @ConditionalOnProperty(
            value = "converter",
            havingValue = "default"
    )
    public Map<FormatCategory, Map<List<Format>, List<Format>>> defaultFormats() {
        return Map.of(FormatCategory.DOCUMENTS, FORMATS.get(FormatCategory.DOCUMENTS), FormatCategory.IMAGES, FORMATS.get(FormatCategory.IMAGES));
    }

    @Bean
    @ConditionalOnProperty(
            value = "converter",
            havingValue = "all",
            matchIfMissing = true
    )
    public Map<FormatCategory, Map<List<Format>, List<Format>>> allFormats() {
        return FORMATS;
    }
}