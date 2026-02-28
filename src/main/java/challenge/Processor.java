package challenge;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.RandomAccessFile;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

public class Processor {

    private static final int CHUNK_BUF_SIZE = 4 * 1024 * 1024;

    public static void process(String inputPath, String outputPath) {
        try (RandomAccessFile raf = new RandomAccessFile(inputPath, "r");
             FileChannel channel = raf.getChannel();
             Arena arena = Arena.ofShared()) {

            long fileSize = channel.size();
            MemorySegment mapped = channel.map(FileChannel.MapMode.READ_ONLY, 0, fileSize, arena);

            int numThreads = Runtime.getRuntime().availableProcessors();
            long chunkSize = fileSize / numThreads;

            List<long[]> boundaries = new ArrayList<>();
            long start = 0;
            for (int i = 1; i < numThreads; i++) {
                long boundary = start + chunkSize;
                if (boundary >= fileSize) {
                    boundary = fileSize;
                } else {
                    while (boundary < fileSize && mapped.get(ValueLayout.JAVA_BYTE, boundary) != '\n') {
                        boundary++;
                    }
                    if (boundary < fileSize) boundary++;
                }
                boundaries.add(new long[]{start, boundary});
                start = boundary;
            }
            boundaries.add(new long[]{start, fileSize});

            List<Callable<HashMap<String, LongLongMap>>> tasks = new ArrayList<>();
            for (long[] range : boundaries) {
                long cStart = range[0];
                long cEnd = range[1];
                tasks.add(() -> processChunk(mapped, cStart, cEnd));
            }

            HashMap<String, LongLongMap> result;
            try (var executor = Executors.newFixedThreadPool(numThreads)) {
                var futures = executor.invokeAll(tasks);
                result = futures.getFirst().get();
                for (int i = 1; i < futures.size(); i++) {
                    mergeMaps(result, futures.get(i).get());
                }
            }

            writeJson(result, outputPath);
            System.err.println("Processed " + result.size() + " unique paths to " + outputPath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to process data", e);
        }
    }

    private static HashMap<String, LongLongMap> processChunk(MemorySegment seg, long start, long end) {
        HashMap<String, LongLongMap> map = new HashMap<>(64);
        long pos = start;
        byte[] buf = new byte[CHUNK_BUF_SIZE];

        while (pos < end) {
            int toRead = (int) Math.min(CHUNK_BUF_SIZE, end - pos);
            MemorySegment.copy(seg, ValueLayout.JAVA_BYTE, pos, buf, 0, toRead);

            int lastNewline = toRead;
            if (pos + toRead < end) {
                lastNewline = toRead - 1;
                while (lastNewline >= 0 && buf[lastNewline] != '\n') {
                    lastNewline--;
                }
                if (lastNewline < 0) {
                    pos += toRead;
                    continue;
                }
                lastNewline++;
            }

            int p = 0;
            while (p < lastNewline) {
                int lineEnd = p;
                while (lineEnd < lastNewline && buf[lineEnd] != '\n') {
                    lineEnd++;
                }

                if (lineEnd > p) {
                    int commaPos = lineEnd - 1;
                    while (commaPos > p && buf[commaPos] != ',') {
                        commaPos--;
                    }

                    if (commaPos > p && commaPos + 11 <= lineEnd) {
                        int pathStart = findPathStart(buf, p, commaPos);
                        if (pathStart >= 0) {
                            int pathLen = commaPos - pathStart;
                            String path = new String(buf, pathStart, pathLen).intern();
                            long dateKey = encodeDate(buf, commaPos + 1);

                            LongLongMap dates = map.get(path);
                            if (dates == null) {
                                dates = new LongLongMap();
                                map.put(path, dates);
                            }
                            dates.addTo(dateKey, 1L);
                        }
                    }
                }
                p = lineEnd + 1;
            }

            pos += lastNewline;
        }

        return map;
    }

    private static int findPathStart(byte[] buf, int start, int end) {
        for (int i = start; i < end - 2; i++) {
            if (buf[i] == ':' && buf[i + 1] == '/' && buf[i + 2] == '/') {
                for (int j = i + 3; j < end; j++) {
                    if (buf[j] == '/') return j;
                }
                return -1;
            }
        }
        return -1;
    }

    private static long encodeDate(byte[] data, int dateStart) {
        int y = (data[dateStart] - '0') * 1000 +
                (data[dateStart + 1] - '0') * 100 +
                (data[dateStart + 2] - '0') * 10 +
                (data[dateStart + 3] - '0');
        int m = (data[dateStart + 5] - '0') * 10 +
                (data[dateStart + 6] - '0');
        int d = (data[dateStart + 8] - '0') * 10 +
                (data[dateStart + 9] - '0');
        return y * 10000L + m * 100L + d;
    }

    private static void mergeMaps(HashMap<String, LongLongMap> a,
                                   HashMap<String, LongLongMap> b) {
        for (var entry : b.entrySet()) {
            LongLongMap existing = a.get(entry.getKey());
            if (existing == null) {
                a.put(entry.getKey(), entry.getValue());
            } else {
                existing.mergeFrom(entry.getValue());
            }
        }
    }

    private static String decodeDate(long encoded) {
        int d = (int)(encoded % 100); encoded /= 100;
        int m = (int)(encoded % 100); encoded /= 100;
        int y = (int)encoded;
        StringBuilder sb = new StringBuilder(10);
        if (y < 1000) sb.append('0');
        if (y < 100) sb.append('0');
        if (y < 10) sb.append('0');
        sb.append(y).append('-');
        if (m < 10) sb.append('0');
        sb.append(m).append('-');
        if (d < 10) sb.append('0');
        sb.append(d);
        return sb.toString();
    }

    private static void writeJson(HashMap<String, LongLongMap> map, String outputPath) throws Exception {
        TreeMap<String, TreeMap<String, Long>> sorted = new TreeMap<>();
        for (var entry : map.entrySet()) {
            TreeMap<String, Long> dates = new TreeMap<>();
            entry.getValue().forEach((dateKey, count) -> dates.put(decodeDate(dateKey), count));
            sorted.put(entry.getKey(), dates);
        }

        try (BufferedWriter out = new BufferedWriter(new FileWriter(outputPath), 1024 * 1024)) {
            out.write("{\n");
            int total = sorted.size();
            int i = 0;

            for (var entry : sorted.entrySet()) {
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
