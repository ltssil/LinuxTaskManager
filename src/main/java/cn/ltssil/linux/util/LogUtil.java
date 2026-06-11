package cn.ltssil.linux.util;

import cn.ltssil.linux.model.ProcessInfo;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class LogUtil {

    public static void export(List<ProcessInfo> list)
            throws IOException {

        FileWriter writer =
                new FileWriter("process_log.txt");

        writer.write(
                "PID\tNAME\tCOMMAND\n"
        );

        for (ProcessInfo p : list) {

            writer.write(
                    p.getPid()
                            + "\t"
                            + p.getName()
                            + "\t"
                            + p.getCommand()
                            + "\n"
            );
        }

        writer.close();
    }
}