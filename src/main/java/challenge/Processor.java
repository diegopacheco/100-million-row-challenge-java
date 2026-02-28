package challenge;

import java.io.BufferedWriter;
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
            int numThreads = Runtime.getRuntime().availableProcessors();
            long chunkSize = fileSize / numThreads;

            List<long[]> boundaries = new ArrayList<>();
            long start = 0;
            byte[] buf = new byte[1];
            for (int i = 1; i < numThreads; i++) {
                long boundary = start + chunkSize;
                if (boundary >= fileSize) {
                    boundary = fileSize;
                } else {
                    raf.seek(boundary);
                    while (boundary < fileSize) {
                        raf.read(buf);
                        boundary++;
                        if (buf[0] == '\n') break;
                    }
                }
                boundaries.add(new long[]{start, boundary});
                start = boundary;
            }
            boundaries.add(new long[]{start, fileSize});

            List<Callable<TreeMap<String, TreeMap<String, Long>>>> tasks = new ArrayList<>();
            for (long[] range : boundaries) {
                long cStart = range[0];
                long cEnd = range[1];
                tasks.add(() -> processChunk(inputPath, cStart, cEnd));
            }

            TreeMap<String, TreeMap<String, Long>> result;
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                var futures = executor.invokeAll(tasks);
                result = new TreeMap<>();
                for (var future : futures) {
                    mergeMaps(result, future.get());
                }
            }

            writeJson(result, outputPath);
            System.err.println("Processed " + result.size() + " unique paths to " + outputPath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to process data", e);
        }
    }

    private static TreeMap<String, TreeMap<String, Long>> processChunk(String path, long start, long end) throws Exception {
        TreeMap<String, TreeMap<String, Long>> map = new TreeMap<>();
        long length = end - start;
        if (length <= 0) return map;

        try (RandomAccessFile raf = new RandomAccessFile(path, "r");
             FileChannel channel = raf.getChannel()) {

            long offset = start;
            while (offset < end) {
                long remaining = end - offset;
                int segSize = (int) Math.min(remaining, Integer.MAX_VALUE - 8);
                MappedByteBuffer mmap = channel.map(FileChannel.MapMode.READ_ONLY, offset, segSize);
                byte[] data = new byte[segSize];
                mmap.get(data);

                int pos = 0;
                int lastLineEnd = 0;
                while (pos < segSize) {
                    int lineEnd = pos;
                    while (lineEnd < segSize && data[lineEnd] != '\n') {
                        lineEnd++;
                    }

                    if (lineEnd == segSize && offset + segSize < end) {
                        break;
                    }

                    if (lineEnd > pos) {
                        parseLine(data, pos, lineEnd, map);
                    }

                    lastLineEnd = lineEnd + 1;
                    pos = lineEnd + 1;
                }

                offset += (lastLineEnd > 0) ? lastLineEnd : segSize;
            }
        }

        return map;
    }

    private static void parseLine(byte[] data, int start, int end, TreeMap<String, TreeMap<String, Long>> map) {
        int commaPos = -1;
        for (int i = end - 1; i >= start; i--) {
            if (data[i] == ',') {
                commaPos = i;
                break;
            }
        }
        if (commaPos <= start || commaPos + 11 > end) return;

        String url = new String(data, start, commaPos - start);
        String date = new String(data, commaPos + 1, 10);

        int protocolEnd = url.indexOf("://");
        if (protocolEnd < 0) return;
        int pathStart = url.indexOf('/', protocolEnd + 3);
        if (pathStart < 0) return;

        String urlPath = url.substring(pathStart);
        map.computeIfAbsent(urlPath, _ -> new TreeMap<>())
           .merge(date, 1L, Long::sum);
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

    private static void writeJson(TreeMap<String, TreeMap<String, Long>> map, String outputPath) throws Exception {
        try (BufferedWriter out = new BufferedWriter(new FileWriter(outputPath), 1024 * 1024)) {
            out.write("{\n");
            int total = map.size();
            int i = 0;

            for (var entry : map.entrySet()) {
                String escapedPath = entry.getKey().replace("/", "\\/");
                out.write("    \"" + escapedPath + "\": {\n");

                TreeMap<String, Long> dates = entry.getValue();
                int dateTotal = dates.size();
                int j = 0;

                for (var dateEntry : dates.entrySet()) {
                    out.write("        \"" + dateEntry.getKey() + "\": " + dateEntry.getValue());
                    if (j < dateTotal - 1) {
                        out.write(",");
                    }
                    out.write("\n");
                    j++;
                }

                out.write("    }");
                if (i < total - 1) {
                    out.write(",");
                }
                out.write("\n");
                i++;
            }

            out.write("}\n");
        }
    }
}
