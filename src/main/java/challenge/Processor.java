package challenge;

import java.io.FileWriter;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

public class Processor {

    public static void process(String inputPath, String outputPath) {
        try (RandomAccessFile raf = new RandomAccessFile(inputPath, "r");
             FileChannel channel = raf.getChannel()) {

            long fileSize = channel.size();
            MappedByteBuffer mmap = channel.map(FileChannel.MapMode.READ_ONLY, 0, fileSize);
            byte[] data = new byte[(int) fileSize];
            mmap.get(data);

            int numThreads = Runtime.getRuntime().availableProcessors();
            int chunkSize = data.length / numThreads;

            List<int[]> boundaries = new ArrayList<>();
            int start = 0;
            for (int i = 1; i < numThreads; i++) {
                int boundary = start + chunkSize;
                while (boundary < data.length && data[boundary] != '\n') {
                    boundary++;
                }
                if (boundary < data.length) {
                    boundary++;
                }
                boundaries.add(new int[]{start, boundary});
                start = boundary;
            }
            boundaries.add(new int[]{start, data.length});

            List<Callable<TreeMap<String, TreeMap<String, Long>>>> tasks = new ArrayList<>();
            for (int[] range : boundaries) {
                int chunkStart = range[0];
                int chunkEnd = range[1];
                tasks.add(() -> processChunk(data, chunkStart, chunkEnd));
            }

            TreeMap<String, TreeMap<String, Long>> result;
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                var futures = executor.invokeAll(tasks);
                result = new TreeMap<>();
                for (var future : futures) {
                    mergeMaps(result, future.get());
                }
            }

            String json = formatJson(result);
            try (FileWriter out = new FileWriter(outputPath)) {
                out.write(json);
            }

            System.err.println("Processed " + result.size() + " unique paths to " + outputPath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to process data", e);
        }
    }

    private static TreeMap<String, TreeMap<String, Long>> processChunk(byte[] data, int start, int end) {
        TreeMap<String, TreeMap<String, Long>> map = new TreeMap<>();
        int pos = start;

        while (pos < end) {
            int lineEnd = pos;
            while (lineEnd < end && data[lineEnd] != '\n') {
                lineEnd++;
            }

            if (lineEnd > pos) {
                String line = new String(data, pos, lineEnd - pos);
                int commaPos = line.lastIndexOf(',');
                if (commaPos > 0 && commaPos + 10 < line.length()) {
                    String url = line.substring(0, commaPos);
                    String date = line.substring(commaPos + 1, commaPos + 11);

                    int protocolEnd = url.indexOf("://");
                    if (protocolEnd >= 0) {
                        int pathStart = url.indexOf('/', protocolEnd + 3);
                        if (pathStart >= 0) {
                            String path = url.substring(pathStart);
                            map.computeIfAbsent(path, _ -> new TreeMap<>())
                               .merge(date, 1L, Long::sum);
                        }
                    }
                }
            }

            pos = lineEnd + 1;
        }

        return map;
    }

    private static void mergeMaps(TreeMap<String, TreeMap<String, Long>> a,
                                   TreeMap<String, TreeMap<String, Long>> b) {
        for (var entry : b.entrySet()) {
            TreeMap<String, Long> dates = a.computeIfAbsent(entry.getKey(), _ -> new TreeMap<>());
            for (var dateEntry : entry.getValue().entrySet()) {
                dates.merge(dateEntry.getKey(), dateEntry.getValue(), Long::sum);
            }
        }
    }

    private static String formatJson(TreeMap<String, TreeMap<String, Long>> map) {
        StringBuilder out = new StringBuilder("{\n");
        int total = map.size();
        int i = 0;

        for (var entry : map.entrySet()) {
            String escapedPath = entry.getKey().replace("/", "\\/");
            out.append("    \"").append(escapedPath).append("\": {\n");

            TreeMap<String, Long> dates = entry.getValue();
            int dateTotal = dates.size();
            int j = 0;

            for (var dateEntry : dates.entrySet()) {
                out.append("        \"").append(dateEntry.getKey()).append("\": ").append(dateEntry.getValue());
                if (j < dateTotal - 1) {
                    out.append(",");
                }
                out.append("\n");
                j++;
            }

            out.append("    }");
            if (i < total - 1) {
                out.append(",");
            }
            out.append("\n");
            i++;
        }

        out.append("}\n");
        return out.toString();
    }
}
