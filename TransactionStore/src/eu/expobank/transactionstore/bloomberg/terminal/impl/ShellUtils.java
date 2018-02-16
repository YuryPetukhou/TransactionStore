package eu.expobank.transactionstore.bloomberg.terminal.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class ShellUtils {
	
    /**
     * @return a list of processes currently running
     * @throws RuntimeException if the request sent to the OS to get the list of running processes fails
     */
    public static List<String> getRunningProcesses() {
        List<String> processes = new ArrayList<>();

        try {
            Process p = Runtime.getRuntime().exec(System.getenv("windir") + "\\system32\\" + "tasklist.exe");
            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            int i = 0;
            while ((line = input.readLine()) != null) {
                if (!line.isEmpty()) {
                    String process = line.split(" ")[0];
                    if (process.contains("exe")) {
                        processes.add(process);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not retrieve the list of running processes from the OS");
        }

        return processes;
    }

    /**
     * 
     * @param processName the name of the process, for example "explorer.exe"
     * @return true if the process is currently running
     * @throws RuntimeException if the request sent to the OS to get the list of running processes fails
     */
    public static boolean isProcessRunning(String processName) {
        List<String> processes = getRunningProcesses();
        return processes.contains(processName);
    }
}
