package cn.ltssil.linux.model;

public class ProcessInfo {
    private final long pid;
    private final String name;
    private final String command;

    public ProcessInfo(long pid, String name, String command) {
        this.pid = pid;
        this.name = name;
        this.command = command;
    }

    public long getPid() {
        return pid;
    }

    public String getName() {
        return name;
    }

    public String getCommand() {
        return command;
    }
}