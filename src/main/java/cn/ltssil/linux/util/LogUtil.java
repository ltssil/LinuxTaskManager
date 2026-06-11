package cn.ltssil.linux.util;

import cn.ltssil.linux.model.ProcessInfo;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

public class LogUtil {

    public static void export(List<ProcessInfo> list) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter("process_log.txt"))) {
            writer.println("PID\tNAME\tCOMMAND\tCPU(%)\tMEMORY(MB)");

            for (ProcessInfo p : list) {
                writer.printf(
                        "%d\t%s\t%s\t%.2f\t%d%n",
                        p.getPid(),
                        p.getName(),
                        p.getCommand(),
                        p.getCpuUsage(),
                        p.getMemoryMb()
                );
            }
        }
    }
}