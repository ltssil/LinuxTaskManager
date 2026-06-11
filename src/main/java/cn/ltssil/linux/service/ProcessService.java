package cn.ltssil.linux.service;

import cn.ltssil.linux.model.ProcessInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProcessService {

    public List<ProcessInfo> getAllProcesses() {
        List<ProcessInfo> list = new ArrayList<>();

        ProcessHandle.allProcesses().forEach(handle -> {
            long pid = handle.pid();

            String name = handle.info().command()
                    .map(command -> {
                        int lastSlash = command.lastIndexOf('/');
                        return lastSlash >= 0 ? command.substring(lastSlash + 1) : command;
                    })
                    .orElse("unknown");

            String command = handle.info().command().orElse("unknown");

            list.add(new ProcessInfo(pid, name, command));
        });

        return list;
    }

    public Optional<ProcessInfo> getProcessByPid(long pid) {
        return ProcessHandle.of(pid).map(handle -> {
            String name = handle.info().command()
                    .map(command -> {
                        int lastSlash = command.lastIndexOf('/');
                        return lastSlash >= 0 ? command.substring(lastSlash + 1) : command;
                    })
                    .orElse("unknown");

            String command = handle.info().command().orElse("unknown");

            return new ProcessInfo(pid, name, command);
        });
    }
}