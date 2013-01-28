package com.bumptech.photos.resize.cache.disk;

import android.support.v4.util.LruCache;
import com.bumptech.photos.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: sam
 * Date: 1/28/13
 * Time: 9:58 AM
 * To change this template use File | Settings | File Templates.
 */
public class Journal {
    private static int MAX_LINES = 10000;
    private MemoryJournal memoryJournal = null;
    private final File journalFile;
    private BufferedWriter journalWriter;
    private final int maxCacheSize;
    private int numLines = 0;
    private final EvictionListener evictionListener;
    private boolean loading = false;

    public interface EvictionListener {
        public void onKeyEvicted(String safeKey);
    }

    private enum Action{
        GET,
        SET,
        DEL,
    }

    public Journal(File journalFile, int maxCacheSize, EvictionListener evictionListener) {
        this.maxCacheSize = maxCacheSize;
        this.journalFile = journalFile;
        this.evictionListener = evictionListener;
    }

    private class MemoryJournal extends LruCache<String, Integer> {
        private final EvictionListener evictionListener;

        public MemoryJournal(int maxSize, EvictionListener evictionListener) {
            super(maxSize);
            this.evictionListener = evictionListener;
        }

        @Override
        protected void entryRemoved(boolean evicted, String key, Integer oldValue, Integer newValue) {
            super.entryRemoved(evicted, key, oldValue, newValue);
            if (evicted && !loading) {
                evictionListener.onKeyEvicted(key);
            }
        }

        @Override
        protected int sizeOf(String key, Integer value) {
            return value;
        }
    }

    public void open() throws IOException {
        journalFile.createNewFile();
        journalWriter = new BufferedWriter(new FileWriter(journalFile, true));
        if (memoryJournal == null) {
            memoryJournal = new MemoryJournal(maxCacheSize, evictionListener);
            replayFromDisk();
        }
    }

    public void close() throws IOException {
        if (journalWriter != null) {
            journalWriter.close();
            journalWriter = null;
        }
    }

    private void replayFromDisk() throws IOException {
        loading = true;
        final Pattern regex = Pattern.compile(" ");
        String line;
        BufferedReader reader = null;
        numLines = 0;
        try {
            reader = new BufferedReader(new FileReader(journalFile));
            while ((line = reader.readLine()) != null) {
                numLines++;
                String[] splitLine = regex.split(line);
                final Action action = Action.valueOf(splitLine[0]);
                if (action == Action.SET) {
                    memoryJournal.put(splitLine[1], Integer.parseInt(splitLine[2]));
                } else if (action == Action.GET) {
                    memoryJournal.get(splitLine[1]);
                } else {
                    memoryJournal.remove(splitLine[1]);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("DISKCACHE: corrupt journal, rebuilding from disk");
            rebuildFromDisk();
        } finally {
            if (reader != null)
                reader.close();
        }
        loading = false;
    }

    private void rebuildFromDisk() {
        File directory = journalFile.getParentFile();
        final File[] files = directory.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.getName() != journalFile.getName();
            }
        });
        List<File> fileList = Arrays.asList(files);
        Collections.sort(fileList, new Comparator<File>() {
            @Override
            public int compare(File file, File file2) {
                final long arg1 = file.lastModified();
                final long arg2 = file2.lastModified();
                if (arg1 == arg2) {
                    return 0;
                } else if (arg1 > arg1) {
                    return -1;
                } else {
                    return 1;
                }
            }
        });

        memoryJournal.evictAll();
        for (File f : fileList) {
            memoryJournal.put(f.getName(), (int) f.length());
        }

        try {
            compact();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void put(String safeKey, int size) throws IOException {
        writeLine(buildLine(Action.SET, safeKey, String.valueOf(size)));
        memoryJournal.put(safeKey, size);
    }

    public void get(String safeKey) throws IOException {
        writeLine(buildLine(Action.GET, safeKey));
        memoryJournal.get(safeKey);
    }

    public void delete(String safeKey) throws IOException {
        writeLine(buildLine(Action.DEL, safeKey));
        memoryJournal.remove(safeKey);
    }

    private void writeLine(String line) throws IOException {
        journalWriter.write(line);
        journalWriter.newLine();
        numLines++;

        if (shouldCompact()) {
            compact();
        }
    }

    private void compact() throws IOException {
        numLines = 0;
        journalWriter = new BufferedWriter(new FileWriter(journalFile));
        final Map<String, Integer> snapshot = memoryJournal.snapshot();
        for (String key : snapshot.keySet()) {
            writeLine(buildLine(Action.SET, key, String.valueOf(snapshot.get(key))));
        }
    }

    private boolean shouldCompact() {
        return numLines > MAX_LINES;
    }

    private static String buildLine(Action action, String... args) {
        StringBuilder builder = new StringBuilder().append(action.name());
        for (String arg : args) {
            builder.append(" ");
            builder.append(arg);
        }
        return builder.toString();
    }
}
