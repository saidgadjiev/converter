package ru.gadjini.telegram.converter.service.conversion.api.result;

public class EmptyConversionResult implements ConversionResult {

    private boolean deleteSrcFiles;

    public EmptyConversionResult(boolean deleteSrcFiles) {
        this.deleteSrcFiles = deleteSrcFiles;
    }

    @Override
    public boolean deleteSrcFiles() {
        return deleteSrcFiles;
    }

    @Override
    public ResultType resultType() {
        return ResultType.EMPTY;
    }
}
