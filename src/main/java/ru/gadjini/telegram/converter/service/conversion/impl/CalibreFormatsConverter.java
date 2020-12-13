package ru.gadjini.telegram.converter.service.conversion.impl;

import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConvertResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.conversion.device.SmartCalibre;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.*;

@Component
public class CalibreFormatsConverter extends BaseAny2AnyConverter {

    private static final String TAG = "calibre2";

    private static final Map<List<Format>, List<Format>> MAP = new HashMap<>() {{
        put(List.of(PDF), List.of(EPUB));
        put(List.of(DOC, DOCX), List.of(EPUB, RTF));
        put(List.of(TXTZ), List.of(AZW3, EPUB, DOCX, FB2, HTMLZ, OEB, LIT, LRF, MOBI, PDB, PMLZ, RB, PDF, RTF, SNB, TCR, TXT, ZIP));
        put(List.of(TCR), List.of(AZW3, EPUB, DOCX, FB2, HTMLZ, OEB, LIT, LRF, MOBI, PDB, PMLZ, RB, PDF, RTF, SNB, TXT, TXTZ, ZIP));
        put(List.of(RTF), List.of(AZW3, EPUB, DOCX, FB2, HTMLZ, OEB, LIT, LRF, MOBI, PDB, PMLZ, RB, PDF, TCR, SNB, TXT, TXTZ, ZIP));
        put(List.of(SNB), List.of(AZW3, EPUB, DOCX, FB2, HTMLZ, OEB, LIT, LRF, MOBI, PDB, PMLZ, RB, PDF, RTF, TCR, TXT, TXTZ, ZIP));
        put(List.of(RB), List.of(AZW3, EPUB, DOCX, FB2, HTMLZ, OEB, LIT, LRF, MOBI, PDB, PMLZ, PDF, RTF, SNB, TCR, TXT, TXTZ, ZIP));
        put(List.of(PML), List.of(AZW3, EPUB, DOCX, FB2, HTMLZ, OEB, LIT, LRF, MOBI, PDB, PMLZ, RB, PDF, RTF, SNB, TCR, TXT, TXTZ, ZIP));
        put(List.of(PDB), List.of(AZW3, EPUB, DOCX, FB2, HTMLZ, OEB, LIT, LRF, MOBI, PMLZ, RB, PDF, RTF, SNB, TCR, TXT, TXTZ, ZIP));

        put(List.of(CBZ), List.of(AZW3, EPUB, DOCX, FB2, HTMLZ, OEB, LIT, LRF, MOBI, PDB, PMLZ, RB, PDF, RTF, SNB, TCR, TXT, TXTZ, ZIP));
        put(List.of(CBR), List.of(AZW3, EPUB, DOCX, FB2, HTMLZ, OEB, LIT, LRF, MOBI, PDB, PMLZ, RB, PDF, RTF, SNB, TCR, TXT, TXTZ, ZIP));
        put(List.of(CBC), List.of(AZW3, EPUB, DOCX, FB2, HTMLZ, OEB, LIT, LRF, MOBI, PDB, PMLZ, RB, PDF, RTF, SNB, TCR, TXT, TXTZ, ZIP));
        put(List.of(AZW), List.of(AZW3, EPUB, DOCX, FB2, HTMLZ, OEB, LIT, LRF, MOBI, PDB, PMLZ, RB, PDF, RTF, SNB, TCR, TXT, TXTZ, ZIP));
        put(List.of(AZW3), List.of(EPUB, DOCX, FB2, HTMLZ, OEB, LIT, LRF, MOBI, PDB, PMLZ, RB, PDF, RTF, SNB, TCR, TXT, TXTZ, ZIP));
        put(List.of(AZW4), List.of(AZW3, EPUB, DOCX, FB2, HTMLZ, OEB, LIT, LRF, MOBI, PDB, PMLZ, RB, PDF, RTF, SNB, TCR, TXT, TXTZ, ZIP));
        put(List.of(CHM), List.of(AZW3, EPUB, DOCX, FB2, HTMLZ, OEB, LIT, LRF, MOBI, PDB, PMLZ, RB, PDF, RTF, SNB, TCR, TXT, TXTZ, ZIP));
        put(List.of(EPUB), List.of(AZW3, DOCX, DOC, FB2, HTMLZ, OEB, LIT, LRF, MOBI, PDB, PMLZ, RB, PDF, RTF, SNB, TCR, TXT, TXTZ, ZIP));
        put(List.of(FB2), List.of(AZW3, EPUB, DOCX, HTMLZ, OEB, LIT, LRF, MOBI, PDB, PMLZ, RB, PDF, RTF, SNB, TCR, TXT, TXTZ, ZIP));
        put(List.of(FBZ), List.of(AZW3, EPUB, DOCX, FB2, HTMLZ, OEB, LIT, LRF, MOBI, PDB, PMLZ, RB, PDF, RTF, SNB, TCR, TXT, TXTZ, ZIP));
        put(List.of(HTMLZ), List.of(AZW3, EPUB, DOCX, FB2, OEB, LIT, LRF, MOBI, PDB, PMLZ, RB, PDF, RTF, SNB, TCR, TXT, TXTZ, ZIP));
        put(List.of(LIT), List.of(AZW3, EPUB, DOCX, FB2, HTMLZ, OEB, LRF, MOBI, PDB, PMLZ, RB, PDF, RTF, SNB, TCR, TXT, TXTZ, ZIP));
        put(List.of(LRF), List.of(AZW3, EPUB, DOCX, FB2, HTMLZ, OEB, LIT, MOBI, PDB, PMLZ, RB, PDF, RTF, SNB, TCR, TXT, TXTZ, ZIP));
        put(List.of(MOBI), List.of(AZW3, EPUB, DOCX, FB2, HTMLZ, OEB, LIT, LRF, PDB, PMLZ, RB, PDF, RTF, SNB, TCR, TXT, TXTZ, ZIP));
        put(List.of(ODT), List.of(AZW3, EPUB, DOCX, FB2, HTMLZ, OEB, LIT, LRF, MOBI, PDB, PMLZ, RB, PDF, RTF, SNB, TCR, TXT, TXTZ, ZIP));
        put(List.of(PRC), List.of(AZW3, EPUB, DOCX, FB2, HTMLZ, OEB, LIT, LRF, MOBI, PDB, PMLZ, RB, PDF, RTF, SNB, TCR, TXT, TXTZ, ZIP));
    }};

    private SmartCalibre convertDevice;

    @Autowired
    public CalibreFormatsConverter(SmartCalibre convertDevice) {
        super(MAP);
        this.convertDevice = convertDevice;
    }

    @Override
    public ConvertResult doConvert(ConversionQueueItem fileQueueItem) {
        SmartTempFile in = fileQueueItem.getDownloadedFile(fileQueueItem.getFirstFileId());

        try {
            SmartTempFile out = getFileService().createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getTargetFormat().getExt());
            try {
                convertDevice.convert(in.getAbsolutePath(), out.getAbsolutePath(), getOptions(fileQueueItem));

                String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), fileQueueItem.getTargetFormat().getExt());
                return new FileResult(fileName, out, null);
            } catch (Throwable e) {
                out.smartDelete();
                throw e;
            }
        } finally {
            in.smartDelete();
        }
    }

    private String[] getOptions(ConversionQueueItem queueItem) {
        if (Set.of(CBZ, CBR, CBC).contains(queueItem.getFirstFileFormat())) {
            return new String[]{
                    "--dont-grayscale", "--landscape", "--no-sort", "--disable-trim", "--title", FilenameUtils.removeExtension(queueItem.getFirstFileName())
            };
        }
        return new String[]{
                "--title", FilenameUtils.removeExtension(queueItem.getFirstFileName())
        };
    }
}
