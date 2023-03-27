package io.github.doodle;

import io.github.doodle.enums.*;
import java.util.Map;

import java.io.IOException;

/**
 * Information wrapper for custom decoding. <br/>
 * Including source info and decoding params. <br/>
 * Source info api includes media type, header and data(all bytes).<br/>
 * Note: <br/>
 * To get source info, Doodle will create DataFetcher to read the data of source file.
 */
public final class DecodingInfo {
    public final String path;

    public final int targetWidth;
    public final int targetHeight;
    public final ClipType clipType;
    public final DecodeFormat decodeFormat;
    public final Map<String, String> options;

    private final Request request;
    DataFetcher dataFetcher;

    DecodingInfo(Request request) {
        this.request = request;
        path = request.path;
        targetWidth = request.targetWidth;
        targetHeight = request.targetHeight;
        clipType = request.clipType;
        decodeFormat = request.decodeFormat;
        options = request.options;
    }

    DataFetcher getDataFetcher() throws IOException {
        if (dataFetcher == null) {
            dataFetcher = DataFetcher.parse(request);
        }
        return dataFetcher;
    }

    /**
     * @return Media type of file.
     */
    public MediaType getMediaType() throws IOException {
        return getDataFetcher().getMediaType();
    }

    /**
     * @return Header of file, 26 bytes.
     */
    public byte[] getHeader() throws IOException {
        return getDataFetcher().getHeader();
    }

    /**
     * @return All bytes of file.
     */
    public byte[] getData() throws IOException {
        return getDataFetcher().getData();
    }
}
