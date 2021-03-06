package com.dtflys.forest.exceptions;

public class ForestFileNotFoundException extends ForestRuntimeException {

    private String filePath;

    public ForestFileNotFoundException(String filePath) {
        super("File \"" + filePath + "\" does not exist");
        this.filePath = filePath;
    }

    public String getFilePath() {
        return filePath;
    }
}
