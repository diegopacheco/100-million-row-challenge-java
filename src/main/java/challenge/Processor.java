package challenge;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.RandomAccessFile;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

public class Processor {

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

            List<Callable<HashMap<Long, HashMap<Long, Long>>>> tasks = new ArrayList<>();
            for (long[] range : boundaries) {
                long cStart = range[0];
                long cEnd = range[1];
                tasks.add(() -> processChunk(mapped, cStart, cEnd));
            }

            HashMap<Long, HashMap<Long, Long>> result;
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                var futures = executor.invokeAll(tasks);
                result = futures.getFirst().get();
                for (int i = 1; i < futures.size(); i++) {
                    mergeMaps(result, futures.get(i).get());
                }
            }

            writeJson(result, mapped, outputPath);
            System.err.println("Processed " + result.size() + " unique paths to " + outputPath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to process data", e);
        }
    }

    private static long encodePath(MemorySegment seg, long pathStart, int pathLen) {
        long hash = pathLen;
        for (int i = 0; i < Math.min(pathLen, 8); i++) {
            hash = hash * 31 + (seg.get(ValueLayout.JAVA_BYTE, pathStart + i) & 0xFF);
        }
        if (pathLen > 8) {
            for (int i = pathLen - 4; i < pathLen; i++) {
                hash = hash * 31 + (seg.get(ValueLayout.JAVA_BYTE, pathStart + i) & 0xFF);
            }
        }
        return hash;
    }

    private static long encodeDate(MemorySegment seg, long dateStart) {
        int y = (seg.get(ValueLayout.JAVA_BYTE, dateStart) - '0') * 1000 +
                (seg.get(ValueLayout.JAVA_BYTE, dateStart + 1) - '0') * 100 +
                (seg.get(ValueLayout.JAVA_BYTE, dateStart + 2) - '0') * 10 +
                (seg.get(ValueLayout.JAVA_BYTE, dateStart + 3) - '0');
        int m = (seg.get(ValueLayout.JAVA_BYTE, dateStart + 5) - '0') * 10 +
                (seg.get(ValueLayout.JAVA_BYTE, dateStart + 6) - '0');
        int d = (seg.get(ValueLayout.JAVA_BYTE, dateStart + 8) - '0') * 10 +
                (seg.get(ValueLayout.JAVA_BYTE, dateStart + 9) - '0');
        return y * 10000L + m * 100L + d;
    }

    private static HashMap<Long, HashMap<Long, Long>> processChunk(MemorySegment seg, long start, long end) {
        HashMap<Long, HashMap<Long, Long>> map = new HashMap<>(64);
        long pos = start;

        while (pos < end) {
            long lineEnd = pos;
            while (lineEnd < end && seg.get(ValueLayout.JAVA_BYTE, lineEnd) != '\n') {
                lineEnd++;
            }

            if (lineEnd > pos) {
                long commaPos = lineEnd - 1;
                while (commaPos > pos && seg.get(ValueLayout.JAVA_BYTE, commaPos) != ',') {
                    commaPos--;
                }

                if (commaPos > pos && commaPos + 11 <= lineEnd) {
                    long urlStart = pos;
                    long urlEnd = commaPos;
                    long dateStart = commaPos + 1;

                    long pathStart = -1;
                    long p = urlStart;
                    while (p < urlEnd - 2) {
                        if (seg.get(ValueLayout.JAVA_BYTE, p) == ':' &&
                            seg.get(ValueLayout.JAVA_BYTE, p + 1) == '/' &&
                            seg.get(ValueLayout.JAVA_BYTE, p + 2) == '/') {
                            long afterProtocol = p + 3;
                            while (afterProtocol < urlEnd) {
                                if (seg.get(ValueLayout.JAVA_BYTE, afterProtocol) == '/') {
                                    pathStart = afterProtocol;
                                    break;
                                }
                                afterProtocol++;
                            }
                            break;
                        }
                        p++;
                    }

                    if (pathStart >= 0) {
                        int pathLen = (int)(urlEnd - pathStart);
                        long pathKey = encodePath(seg, pathStart, pathLen);
                        long dateKey = encodeDate(seg, dateStart);

                        HashMap<Long, Long> dates = map.get(pathKey);
                        if (dates == null) {
                            dates = new HashMap<>(1024);
                            map.put(pathKey, dates);
                        }
                        dates.merge(dateKey, 1L, Long::sum);
                    }
                }
            }
            pos = lineEnd + 1;
        }

        return map;
    }

    private static void mergeMaps(HashMap<Long, HashMap<Long, Long>> a,
                                   HashMap<Long, HashMap<Long, Long>> b) {
        for (var entry : b.entrySet()) {
            HashMap<Long, Long> existing = a.get(entry.getKey());
            if (existing == null) {
                a.put(entry.getKey(), entry.getValue());
            } else {
                for (var dateEntry : entry.getValue().entrySet()) {
                    existing.merge(dateEntry.getKey(), dateEntry.getValue(), Long::sum);
                }
            }
        }
    }

    private static String decodeDate(long encoded) {
        int d = (int)(encoded % 100); encoded /= 100;
        int m = (int)(encoded % 100); encoded /= 100;
        int y = (int)encoded;
        return String.format("%04d-%02d-%02d", y, m, d);
    }

    private static void writeJson(HashMap<Long, HashMap<Long, Long>> map,
                                   MemorySegment seg, String outputPath) throws Exception {
        HashMap<Long, String> pathLookup = buildPathLookup(seg);

        TreeMap<String, TreeMap<String, Long>> sorted = new TreeMap<>();
        for (var entry : map.entrySet()) {
            String path = pathLookup.getOrDefault(entry.getKey(), "unknown-" + entry.getKey());
            TreeMap<String, Long> dates = new TreeMap<>();
            for (var dateEntry : entry.getValue().entrySet()) {
                dates.put(decodeDate(dateEntry.getKey()), dateEntry.getValue());
            }
            sorted.put(path, dates);
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

    private static HashMap<Long, String> buildPathLookup(MemorySegment seg) {
        HashMap<Long, String> lookup = new HashMap<>(64);
        long fileSize = seg.byteSize();
        long pos = 0;
        int found = 0;

        while (pos < fileSize && found < 200) {
            long lineEnd = pos;
            while (lineEnd < fileSize && seg.get(ValueLayout.JAVA_BYTE, lineEnd) != '\n') {
                lineEnd++;
            }

            if (lineEnd > pos) {
                long commaPos = lineEnd - 1;
                while (commaPos > pos && seg.get(ValueLayout.JAVA_BYTE, commaPos) != ',') {
                    commaPos--;
                }

                if (commaPos > pos) {
                    long urlEnd = commaPos;
                    long p = pos;
                    while (p < urlEnd - 2) {
                        if (seg.get(ValueLayout.JAVA_BYTE, p) == ':' &&
                            seg.get(ValueLayout.JAVA_BYTE, p + 1) == '/' &&
                            seg.get(ValueLayout.JAVA_BYTE, p + 2) == '/') {
                            long afterProtocol = p + 3;
                            while (afterProtocol < urlEnd) {
                                if (seg.get(ValueLayout.JAVA_BYTE, afterProtocol) == '/') {
                                    int pathLen = (int)(urlEnd - afterProtocol);
                                    long key = encodePath(seg, afterProtocol, pathLen);
                                    if (!lookup.containsKey(key)) {
                                        byte[] pathBytes = new byte[pathLen];
                                        MemorySegment.copy(seg, ValueLayout.JAVA_BYTE, afterProtocol, pathBytes, 0, pathLen);
                                        lookup.put(key, new String(pathBytes));
                                        found++;
                                    }
                                    break;
                                }
                                afterProtocol++;
                            }
                            break;
                        }
                        p++;
                    }
                }
            }
            pos = lineEnd + 1;
        }

        return lookup;
    }
}
