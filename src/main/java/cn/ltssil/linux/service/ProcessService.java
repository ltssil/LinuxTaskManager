package cn.ltssil.linux.service;

import cn.ltssil.linux.model.ProcessInfo;
import oshi.SystemInfo;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class ProcessService {

    private final SystemInfo systemInfo = new SystemInfo();
    private final OperatingSystem operatingSystem = systemInfo.getOperatingSystem();

    public List<ProcessInfo> getAllProcesses() {


        List<OSProcess> osProcesses =
                operatingSystem.getProcesses();
        List<ProcessInfo> result = new ArrayList<>();

        for (OSProcess process : osProcesses) {
            ProcessInfo info = toProcessInfo(process);
            if (info != null) {
                result.add(info);
            }
        }

        result.sort(Comparator.comparingLong(ProcessInfo::getPid));
        return result;
    }

    public Optional<ProcessInfo> getProcessByPid(long pid) {
        OSProcess process = operatingSystem.getProcess((int) pid);
        if (process == null) {
            return Optional.empty();
        }

        ProcessInfo info = toProcessInfo(process);
        return Optional.ofNullable(info);
    }

    public boolean killProcess(long pid) {
        return ProcessHandle.of(pid)
                .map(handle -> {
                    boolean ok = handle.destroy();
                    if (!ok) {
                        ok = handle.destroyForcibly();
                    }
                    return ok;
                })
                .orElse(false);
    }

    public long getProcessCount() {
        return getAllProcesses().size();
    }

    public List<ProcessInfo> sortByPidAsc(List<ProcessInfo> list) {
        return list.stream()
                .sorted(Comparator.comparingLong(ProcessInfo::getPid))
                .toList();
    }

    public List<ProcessInfo> sortByPidDesc(List<ProcessInfo> list) {
        return list.stream()
                .sorted(Comparator.comparingLong(ProcessInfo::getPid).reversed())
                .toList();
    }

    public List<ProcessInfo> sortByCpuDesc(List<ProcessInfo> list) {
        return list.stream()
                .sorted(Comparator.comparingDouble(ProcessInfo::getCpuUsage).reversed())
                .toList();
    }

    public List<ProcessInfo> sortByMemoryDesc(List<ProcessInfo> list) {
        return list.stream()
                .sorted(Comparator.comparingLong(ProcessInfo::getMemoryMb).reversed())
                .toList();
    }

    public List<ProcessInfo> sortByNameAsc(List<ProcessInfo> list) {
        return list.stream()
                .sorted(Comparator.comparing(ProcessInfo::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private ProcessInfo toProcessInfo(OSProcess process) {
        String rawName = safeText(process.getName());
        String rawCommand = safeText(process.getPath());

        if (isUnknown(rawName) && isUnknown(rawCommand)) {
            return null;
        }

        String command = rawCommand;
        if (command.isBlank() || isUnknown(command)) {
            command = "N/A";
        }

        String name = rawName;
        if (name.isBlank() || isUnknown(name)) {
            if (!"N/A".equals(command)) {
                name = extractFileName(command);
            } else {
                return null;
            }
        }

        double cpuUsage = process.getProcessCpuLoadCumulative() * 100.0;
        if (cpuUsage < 0) {
            cpuUsage = 0;
        }

        long memoryMb = process.getResidentSetSize() / 1024 / 1024;

        return new ProcessInfo(
                process.getProcessID(),
                name,
                command,
                cpuUsage,
                memoryMb
        );
    }

    private boolean isUnknown(String text) {
        return text == null || text.isBlank() || "unknown".equalsIgnoreCase(text.trim());
    }

    private String safeText(String text) {
        return text == null ? "" : text.trim();
    }

    private String extractFileName(String path) {
        try {
            return Paths.get(path).getFileName().toString();
        } catch (Exception e) {
            int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
            if (slash >= 0 && slash < path.length() - 1) {
                return path.substring(slash + 1);
            }
            return path;
        }
    }
}