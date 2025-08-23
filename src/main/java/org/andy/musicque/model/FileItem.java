package org.andy.musicque.model;

public class FileItem {
    private final String name;
    private final String duration;
    private final String type;
    private final String filePath;

    public FileItem(String name, String filePath, String duration, String type) {
       this.filePath = filePath;
        this.name = name;
        this.duration = duration;
        this.type = type;
    }

    public String getName() { return name; }
    public String getDuration() { return duration; }
    public String getType() { return type; }
    public String getFilePath() { return filePath; }
}
