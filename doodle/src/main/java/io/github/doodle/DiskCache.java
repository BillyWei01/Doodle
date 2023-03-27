package io.github.doodle;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;

/**
 * Disk caches manager.
 * Support limit files count and total size,
 * when caches out of limit, remove the "Least Recently Used".
 */
final class DiskCache {
    private static final String TAG = "DiskCache";

    private static final String JOURNAL_NAME = "journal";
    private static final byte[] HEADER = new byte[]{0x64, 0x69, 0x73, 0x6B};
    private static final int VERSION = 1;
    private static final int DATA_START = 8;
    private static final int PAGE_SIZE = 4096;

    // key: 16 bytes, order: 4 bytes, extra: 4 bytes.
    private static final int RECORD_SIZE = 24;

    private final String cachePath;
    private final int maxCount;
    private final long capacity;
    private long sum = 0;
    private int accessOrder = 1;

    private FileChannel channel;
    private MappedByteBuffer buffer;
    private Map<CacheKey, Record> journal;
    private int journalEnd;

    DiskCache(String relativePath, int maxCount, long capacity) {
        this.cachePath = Utils.getCachePath() + relativePath;
        this.maxCount = maxCount;
        this.capacity = capacity;
    }

    String keyToPath(CacheKey key) {
        return cachePath + key.toHex();
    }

    synchronized String getPath(CacheKey key) {
        if (!checkJournal()) return null;
        Record record = getRecord(key);
        return record != null ? keyToPath(key) : null;
    }

    synchronized CacheInfo getCacheInfo(CacheKey key) {
        if (!checkJournal()) return null;
        Record record = getRecord(key);
        return record != null ? new CacheInfo(keyToPath(key), record.isRGB565()) : null;
    }

    synchronized boolean needToSave(CacheKey key) {
        return (checkJournal()) && !journal.containsKey(key);
    }

    synchronized void record(CacheKey key, File file, boolean isRGB565) {
        if (!checkJournal()) return;
        if (key.h1 == 0 && key.h2 == 0) {
            return;
        }
        try {
            Record record = getRecord(key);
            if (record == null && file.exists()) {
                long fileLen = file.length();
                if (fileLen > 0 && fileLen < capacity && fileLen <= Integer.MAX_VALUE) {
                    append(key, (int) fileLen, isRGB565);
                    checkSize();
                }
            }
        } catch (Throwable e) {
            LogProxy.e(TAG, e);
        }
    }

    private Record getRecord(CacheKey key) {
        Record record = journal.get(key);
        if (record != null) {
            record.order = accessOrder;
            buffer.putInt(record.orderOffset, accessOrder);
            accessOrder++;
        }
        return record;
    }

    synchronized void delete(CacheKey key) {
        if (!checkJournal()) return;
        Record record = journal.get(key);
        if (record != null) {
            journal.remove(key);
            try {
                File file = new File(keyToPath(key));
                if (!file.exists() || Utils.deleteQuietly(file)) {
                    buffer.putInt(record.orderOffset, 0);
                }
            } catch (Throwable e) {
                LogProxy.e(TAG, e);
            }
        }
    }

    private boolean checkJournal() {
        if (capacity <= 0 || maxCount <= 0) {
            return false;
        }
        if (journal == null) {
            journal = new HashMap<>();
            try {
                readJournal();
            } catch (Throwable e) {
                LogProxy.e(TAG, e);
            }
        }
        return buffer != null;
    }

    private void initBuffer() {
        buffer.position(0);
        buffer.put(HEADER);
        buffer.putInt(VERSION);
        paddingZero(DATA_START);
    }

    private void readJournal() throws IOException {
        File journalFile = new File(cachePath + JOURNAL_NAME);
        if (!Utils.makeFileIfNotExist(journalFile)) {
            return;
        }
        RandomAccessFile accessFile = new RandomAccessFile(journalFile, "rw");
        channel = accessFile.getChannel();
        long length = accessFile.length();
        if (length == 0 || (length & 0xFFF) != 0) {
            buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, PAGE_SIZE);
            initBuffer();
        } else {
            buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, length);
            byte[] header = new byte[4];
            buffer.get(header);
            if (!Arrays.equals(header, HEADER)) {
                initBuffer();
            }
        }

        buffer.position(DATA_START);
        journalEnd = DATA_START;
        int invalidCount = 0;
        int maxOrder = 0;
        while (journalEnd + RECORD_SIZE <= buffer.capacity()) {
            long h1 = buffer.getLong();
            long h2 = buffer.getLong();
            if (h1 == 0 && h2 == 0) {
                break;
            }
            CacheKey key = new CacheKey(h1, h2);
            int orderOffset = buffer.position();
            int order = buffer.getInt();
            if (order > 0) {
                if (order > maxOrder) {
                    maxOrder = order;
                }
                int extra = buffer.getInt();
                Record record = new Record(key, orderOffset, order, extra);
                journal.put(key, record);
                sum += record.getFileSize();
            } else {
                // If order equal to zero, it means the cache file had been deleted.
                buffer.position(buffer.position() + 4);
                invalidCount++;
            }
            journalEnd = buffer.position();
        }
        accessOrder = maxOrder + 1;

        if (invalidCount * RECORD_SIZE > PAGE_SIZE) {
            reorganize();
        }

        checkExists();
    }

    private void reorganize() {
        List<Record> recordList = new ArrayList<>(journal.values());
        Collections.sort(recordList);
        rewrite(recordList);
    }

    // Compare files data from file system and journal.
    // Append to to end if journal miss record;
    // Remote from journal if file had been deleted.
    private void checkExists() throws IOException {
        String[] names = new File(cachePath).list();
        if (names == null || names.length == 0) return;

        Set<CacheKey> fileSet = new HashSet<>(names.length * 4 / 3 + 1);
        for (String name : names) {
            if (JOURNAL_NAME.equals(name)) {
                continue;
            }
            CacheKey key = CacheKey.parse(name);
            if (key != null) {
                fileSet.add(key);
                if (!journal.containsKey(key)) {
                    File file = new File(cachePath, name);
                    long fileLen = file.length();
                    if (fileLen > 0) {
                        if (fileLen < capacity && fileLen < Integer.MAX_VALUE) {
                            append(key, (int) fileLen, false);
                            checkSize();
                        }
                    } else {
                        Utils.deleteQuietly(file);
                    }
                }
            } else {
                Utils.deleteQuietly(new File(cachePath, name));
            }
        }

        Set<CacheKey> journalCopy = new HashSet<>(journal.keySet());
        journalCopy.removeAll(fileSet);
        if (journalCopy.isEmpty()) {
            return;
        }
        for (CacheKey key : journalCopy) {
            File file = new File(keyToPath(key));
            if (!file.exists()) {
                Record record = journal.get(key);
                if (record != null) {
                    journal.remove(key);
                    buffer.putInt(record.orderOffset, 0);
                }
            }
        }
    }

    private void checkSize() {
        if (sum > capacity || journal.size() > maxCount) {
            List<Record> recordList = new ArrayList<>(journal.values());
            List<Record> remainList = new ArrayList<>();
            // Sort by order, desc.
            // Remove the record which has the most little order,
            // until capacity and count less then the limit.
            Collections.sort(recordList);
            long capacityLimit = capacity * 7 / 8;
            int sizeLimit = maxCount * 7 / 8;
            int count = recordList.size();
            int i = 0;
            while (i < count && ((sum > capacityLimit) || (journal.size() > sizeLimit))) {
                Record record = recordList.get(i++);
                if (Utils.deleteQuietly(new File(keyToPath(record.key)))) {
                    journal.remove(record.key);
                    sum -= record.getFileSize();
                } else {
                    remainList.add(record);
                }
            }
            if (remainList.isEmpty()) {
                rewrite(recordList.subList(i, count));
            } else {
                remainList.addAll(recordList.subList(i, count));
                rewrite(remainList);
            }
        }
    }

    private void rewrite(List<Record> list) {
        accessOrder = 1;
        buffer.position(DATA_START);
        for (Record record : list) {
            buffer.putLong(record.key.h1);
            buffer.putLong(record.key.h2);
            record.orderOffset = buffer.position();
            record.order = accessOrder++;
            buffer.putInt(record.order);
            buffer.putInt(record.extra);
        }
        journalEnd = buffer.position();
        paddingZero(journalEnd);
    }

    private void append(CacheKey key, int fileLen, boolean isRGB565) throws IOException {
        int extra = fileLen;
        if (isRGB565) {
            extra |= Record.RGB_565_MASK;
        }
        final int end = journalEnd;
        int bufferSize = buffer.capacity();
        if (end + RECORD_SIZE > bufferSize) {
            int extendSize = bufferSize < 4 * PAGE_SIZE ? PAGE_SIZE : 2 * PAGE_SIZE;
            buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, bufferSize + extendSize);
            paddingZero(bufferSize);
        }
        buffer.position(end);
        buffer.putLong(key.h1);
        buffer.putLong(key.h2);
        buffer.putInt(accessOrder);
        buffer.putInt(extra);
        journalEnd = end + RECORD_SIZE;
        journal.put(key, new Record(key, end + 16, accessOrder, extra));
        sum += fileLen;
        accessOrder++;
    }

    private void paddingZero(int start) {
        buffer.position(start);
        int end = buffer.capacity();
        int n = end - start;
        int count = n >>> 3;
        int remain = n & 0x7;
        for (int i = 0; i < count; i++) {
            buffer.putLong(0L);
        }
        for (int i = 0; i < remain; i++) {
            buffer.put((byte) 0);
        }
    }

    static final class CacheInfo {
        final String path;
        final boolean isRGB565;

        CacheInfo(String path, boolean isRGB565) {
            this.path = path;
            this.isRGB565 = isRGB565;
        }
    }

    private static class Record implements Comparable<Record> {
        // File size can never be negative,
        // so we use highest bit of 'extra' to save the 'isRGB565', for result cache.
        // It is not a clean way, but effective (comparing to save 'isRGB565' info with another byte).
        private static final int RGB_565_MASK = 0x80000000;
        private static final int FILE_LEN_MASK = 0x7fffffff;

        final CacheKey key;
        int orderOffset;
        int order;
        int extra;

        Record(CacheKey key, int orderOffset, int order, int extra) {
            this.key = key;
            this.orderOffset = orderOffset;
            this.order = order;
            this.extra = extra;
        }

        int getFileSize() {
            return extra & FILE_LEN_MASK;
        }

        boolean isRGB565() {
            return (extra & RGB_565_MASK) != 0;
        }

        @Override
        public int compareTo(Record o) {
            return Integer.compare(order, o.order);
        }
    }
}
