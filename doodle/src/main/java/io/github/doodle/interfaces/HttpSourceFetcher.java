package io.github.doodle.interfaces;

import java.io.IOException;
import java.io.InputStream;

public interface HttpSourceFetcher {
    /**
     * Provide a InputStream for Doodle to get the image/video data.
     *
     * Note:
     * Doodle handle source cache inside.
     * If you use okhttp as HttpSourceFetcher,
     * it's suggested to call 'cacheControl(CacheControl.Builder().noStore().noCache().build())'.
     *
     * @param url The remote image url (start with "http")
     * @return InputStream to get image/video data.
     * @throws IOException If failed to get the data.
     */
    InputStream getInputStream(String url) throws IOException;
}
