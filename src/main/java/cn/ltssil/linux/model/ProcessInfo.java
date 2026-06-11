package cn.ltssil.linux.model;

public class ProcessInfo {
    private final long pid;
    private final String name;
    private final String command;
    private final double cpuUsage;
    private final long memoryMb;

    public ProcessInfo(long pid, String name, String command, double cpuUsage, long memoryMb) {
        this.pid = pid;
        this.name = name;
        this.command = command;
        this.cpuUsage = cpuUsage;
        this.memoryMb = memoryMb;
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

    public double getCpuUsage() {
        return cpuUsage;
    }

    public long getMemoryMb() {
        return memoryMb;
    }
}