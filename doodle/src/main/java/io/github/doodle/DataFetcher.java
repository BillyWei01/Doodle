package io.github.doodle;

import android.graphics.*;
import android.net.Uri;
import android.media.MediaMetadataRetriever;
import android.content.res.AssetFileDescriptor;
import android.os.Build;
import android.media.MediaDataSource;

import java.io.*;

import io.github.doodle.enums.MediaType;
import io.github.doodle.interfaces.DataParser;

/**
 * Data fetcher. <br/>
 * <p>
 * Doodle support data types:
 * <p> <br/>
 * Remote image <br/>
 * http:// <br/>
 * https://
 * <p> <br/>
 * File<br/>
 * file://<br/>
 * file:///android_asset/
 * <p> <br/>
 * Resource (raw, drawable) <br/>
 * android.resource://
 * <p> <br/>
 * Media <br/>
 * content://
 */
class DataFetcher implements Closeable {
    private static final String FILE_PREFIX = "file://";
    private static final int FILE_PREFIX_LENGTH = FILE_PREFIX.length();
    private static final String ASSET_PREFIX = "file:///android_asset/";
    private static final int ASSET_PREFIX_LENGTH = ASSET_PREFIX.length();

    private static final int HEADER_LEN = 26;

    private final String path;
    private final DataLoader loader;
    final boolean fromSourceCache;

    private MediaType mediaType;
    private byte[] header;
    private byte[] data;

    DataFetcher(String path, DataLoader dataLoader, boolean fromSourceCache) {
        this.path = path;
        this.loader = dataLoader;
        this.fromSourceCache = fromSourceCache;
    }

    static DataFetcher parse(Request request) throws IOException {
        DataLoader loader;
        boolean fromSourceCache = false;
        String path = request.path;
        if (path.startsWith("http")) {
            CacheKey key = new CacheKey(path);
            String cachePath = Downloader.getCachePath(key);
            if (cachePath != null) {
                loader = new FileLoader(new File(cachePath));
                fromSourceCache = true;
            } else {
                if (request.onlyIfCached) {
                    throw new IOException("No cache");
                }
                if (request.diskCacheStrategy.savaSource()) {
                    loader = new FileLoader(Downloader.download(path, key));
                } else {
                    loader = new StreamLoader(path, Downloader.getInputStream(path), null);
                }
            }
        } else if (path.startsWith(ASSET_PREFIX)) {
            loader = new StreamLoader(path, Utils.appContext.getAssets().open(path.substring(ASSET_PREFIX_LENGTH)), null);
        } else if (path.startsWith(FILE_PREFIX)) {
            loader = new FileLoader(new File(path.substring(FILE_PREFIX_LENGTH)));
        } else {
            InputStream inputStream = handleByDataParsers(path);
            if (inputStream != null) {
                loader = new StreamLoader(path, inputStream, null);
            } else {
                Uri uri = request.uri != null ? request.uri : Uri.parse(path);
                loader = new StreamLoader(path, Utils.getContentResolver().openInputStream(uri), uri);
            }
        }
        return new DataFetcher(path, loader, fromSourceCache);
    }

    private static InputStream handleByDataParsers(String path) {
        if (Config.dataParsers != null) {
            for (DataParser parser : Config.dataParsers) {
                InputStream inputStream = parser.parse(path);
                if (inputStream != null) {
                    return inputStream;
                }
            }
        }
        return null;
    }

    @Override
    public void close() throws IOException {
        loader.close();
    }

    byte[] getData() throws IOException {
        if (data == null) {
            data = loader.loadData();
        }
        return data;
    }

    byte[] getHeader() throws IOException {
        if (header == null) {
            header = new byte[HEADER_LEN];
            if (data != null) {
                System.arraycopy(data, 0, header, 0, HEADER_LEN);
            } else {
                loader.loadHeader(header);
            }
        }
        return header;
    }

    MediaType getMediaType() throws IOException {
        if (mediaType == null) {
            mediaType = MediaTypeParser.parse(getHeader());
        }
        return mediaType;
    }

    String getFilePath() {
        return (loader instanceof FileLoader) ? ((FileLoader) loader).filePath : null;
    }

    boolean possiblyExif() throws IOException {
        return ExifHelper.possiblyExif(getHeader());
    }

    boolean isVideo() throws IOException {
        MediaType type = getMediaType();
        if (type != MediaType.UNKNOWN) {
            return type.isVideo();
        } else {
            if (path.startsWith("content://media/external/video/")) {
                return true;
            }
            int len = path.length();
            if (len > 3) {
                String tail = path.substring(len - 3, len).toLowerCase();
                return tail.equals("mp4") || tail.equals("mov");
            }
            return false;
        }
    }

    int getOrientation() throws IOException {
        if (data != null) {
            return ExifHelper.getOrientation(new ByteArrayInputStream(data));
        }
        return loader.loadOrientation();
    }

    Bitmap decode(BitmapFactory.Options options) throws IOException {
        if (data != null) {
            return BitmapFactory.decodeByteArray(data, 0, data.length, options);
        }
        return loader.loadBitmap(options);
    }

    Bitmap decodeRegion(Rect rect, BitmapFactory.Options options) throws IOException {
        if (data != null) {
            return BitmapRegionDecoder.newInstance(data, 0, data.length, false).decodeRegion(rect, options);
        }
        return loader.loadBitmap(rect, options);
    }

    void setDataSource(MediaMetadataRetriever retriever) throws IOException {
        if (data != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            retriever.setDataSource(new MediaDataSource() {
                @Override
                public int readAt(long position, byte[] buffer, int offset, int size) {
                    if (position >= data.length) {
                        return -1;
                    }
                    int count = Math.min(data.length - (int) position, size);
                    System.arraycopy(data, (int) position, buffer, offset, count);
                    return count;
                }

                @Override
                public long getSize() {
                    return data.length;
                }

                @Override
                public void close() {
                }
            });
        } else {
            loader.loadDataSource(retriever);
        }
    }

    interface DataLoader extends Closeable {
        byte[] loadData() throws IOException;

        void loadHeader(byte[] header) throws IOException;

        int loadOrientation() throws IOException;

        Bitmap loadBitmap(BitmapFactory.Options options) throws IOException;

        Bitmap loadBitmap(Rect rect, BitmapFactory.Options options) throws IOException;

        void loadDataSource(MediaMetadataRetriever retriever) throws IOException;
    }

    private static class FileLoader implements DataLoader {
        private final RandomAccessFile accessFile;
        private final FileDescriptor fd;
        final String filePath;

        FileLoader(File file) throws IOException {
            accessFile = new RandomAccessFile(file, "r");
            fd = accessFile.getFD();
            filePath = file.getPath();
        }

        @Override
        public void close() throws IOException {
            accessFile.close();
        }

        @Override
        public void loadHeader(byte[] header) throws IOException {
            accessFile.readFully(header);
            accessFile.seek(0L);
        }

        @Override
        public int loadOrientation() throws IOException {
            int orientation = ExifHelper.getOrientation(new FileInputStream(fd));
            accessFile.seek(0L);
            return orientation;
        }

        @Override
        public byte[] loadData() throws IOException {
            byte[] bytes = Utils.streamToBytes(new FileInputStream(fd));
            accessFile.seek(0);
            return bytes;
        }

        @Override
        public Bitmap loadBitmap(BitmapFactory.Options options) throws IOException {
            if (options.inJustDecodeBounds) {
                BitmapFactory.decodeFileDescriptor(fd, null, options);
                accessFile.seek(0L);
                return null;
            }
            return BitmapFactory.decodeFileDescriptor(fd, null, options);
        }

        @Override
        public Bitmap loadBitmap(Rect rect, BitmapFactory.Options options) throws IOException {
            return BitmapRegionDecoder.newInstance(fd, false).decodeRegion(rect, options);
        }

        @Override
        public void loadDataSource(MediaMetadataRetriever retriever) {
            retriever.setDataSource(fd);
        }
    }

    private static class StreamLoader implements DataLoader {
        private final InputStream inputStream;
        private final String path;
        private final Uri uri;
        private AssetFileDescriptor afd = null;

        private File tempFile = null;
        private RandomAccessFile accessFile = null;

        StreamLoader(String path, InputStream in, Uri uri) {
            this.path = path;
            this.inputStream = in.markSupported() ? in : new RecycledInputStream(in);
            this.uri = uri;
        }

        @Override
        public void close() throws IOException {
            Utils.closeQuietly(afd);
            Utils.closeQuietly(accessFile);
            Utils.deleteQuietly(tempFile);
            inputStream.close();
        }

        private void reset() throws IOException {
            if (inputStream instanceof RecycledInputStream) {
                ((RecycledInputStream) inputStream).rewind();
            } else {
                inputStream.reset();
            }
        }

        @Override
        public void loadHeader(byte[] header) throws IOException {
            inputStream.mark(Integer.MAX_VALUE);
            Utils.readFully(inputStream, header);
            reset();
        }

        @Override
        public int loadOrientation() throws IOException {
            inputStream.mark(Integer.MAX_VALUE);
            int orientation = ExifHelper.getOrientation(inputStream);
            reset();
            return orientation;
        }

        @Override
        public byte[] loadData() throws IOException {
            inputStream.mark(Integer.MAX_VALUE);
            byte[] bytes = Utils.streamToBytes(inputStream);
            reset();
            return bytes;
        }

        @Override
        public Bitmap loadBitmap(BitmapFactory.Options options) throws IOException {
            if (options.inJustDecodeBounds) {
                inputStream.mark(Integer.MAX_VALUE);
                BitmapFactory.decodeStream(inputStream, null, options);
                reset();
                return null;
            }
            return BitmapFactory.decodeStream(inputStream, null, options);
        }

        @Override
        public Bitmap loadBitmap(Rect rect, BitmapFactory.Options options) throws IOException {
            return BitmapRegionDecoder.newInstance(inputStream, false).decodeRegion(rect, options);
        }

        @Override
        public void loadDataSource(MediaMetadataRetriever retriever) throws IOException {
            if (uri != null) {
                retriever.setDataSource(Utils.appContext, uri);
            } else {
                // Only http and assets source can flow to there
                if (path.startsWith("http")) {
                    if (inputStream instanceof RecycledInputStream) {
                        ((RecycledInputStream) inputStream).rewind();
                        // If the request choose DiskCacheStrategy ALL or SOURCE, it should flow to FileSource.
                        // When the code run to here, that means the source is an video file from internet,
                        // and the network request should be connecting.
                        handleRemoteVideo(retriever);
                    } else {
                        // should never be here
                        throw new IllegalStateException("Wrong stream type: " + inputStream.getClass());
                    }
                } else if (path.startsWith(ASSET_PREFIX)) {
                    afd = Utils.appContext.getAssets().openFd(path.substring(ASSET_PREFIX_LENGTH));
                    retriever.setDataSource(afd.getFileDescriptor());
                } else {
                    // should never be here
                    throw new IllegalStateException("Not support");
                }
            }
        }

        private void handleRemoteVideo(MediaMetadataRetriever retriever) throws IOException {
            // MediaMetadataRetriever doesn't support setDataSource(inputStream),
            // and we don't known how many bytes does the data have.
            // So we have to download the file to disk temporary and delete it after decoding.
            tempFile = Downloader.downloadTemporary(inputStream, path);
            if (tempFile.exists()) {
                accessFile = new RandomAccessFile(tempFile, "r");
                retriever.setDataSource(accessFile.getFD());
            } else {
                throw new IOException("Download failed");
            }
        }
    } // end of StreamSource
}
