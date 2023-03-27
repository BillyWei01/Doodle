package io.github.doodle;


import java.io.IOException;
import java.io.InputStream;

/**
 * For {@link java.io.BufferedInputStream} in some version has bug
 * in {@link #skip(long)} (sometimes double the buffer even buffer capacity is enough).
 * We just copy some code from source code in API level 26,
 * and change some implement to match this project.
 */
final class RecycledInputStream extends InputStream {

    /**
     * The maximum size of array to allocate.
     * Some VMs reserve some header words in an array.
     * Attempts to allocate larger arrays may result in
     * OutOfMemoryError: Requested array size exceeds VM limit
     */
    private static final int MAX_BUFFER_SIZE = Integer.MAX_VALUE - 8;

    private volatile InputStream in;

    private volatile byte[] buf;

    /**
     * The index one greater than the index of the last valid byte in
     * the buffer.
     */
    private int count;

    /**
     * The current position in the buffer. This is the index of the next
     * character to be read from the <code>buf</code> array.
     */
    private int pos;

    /**
     * The value of the <code>pos</code> field at the time the last
     * <code>mark</code> method was called.
     */
    private int markpos = -1;

    /**
     * The maximum read ahead allowed after a call to the
     * <code>mark</code> method before subsequent calls to the
     * <code>reset</code> method fail.
     */
    private int marklimit;

    /**
     * @param in the underlying input stream.
     * @throws IllegalArgumentException if {@code size <= 0}.
     */
    RecycledInputStream(InputStream in) {
        this.in = in;
        buf = ByteArrayPool.getBasicArray();
    }

    /**
     * Fills the buffer with more data, taking into account
     * shuffling and other tricks for dealing with marks.
     * This method assumes that all data has already been read in,
     * hence pos > count.
     */
    private void fill() throws IOException {
        byte[] buffer = buf;
        if (markpos < 0)
            pos = 0;            /* no mark: throw away the buffer */
        else if (pos >= buffer.length)  /* no room left in buffer */
            if (markpos > 0) {  /* can throw away early part of the buffer */
                int sz = pos - markpos;
                System.arraycopy(buffer, markpos, buffer, 0, sz);
                pos = sz;
                markpos = 0;
            } else if (buffer.length >= marklimit) {
                markpos = -1;   /* buffer got too big, invalidate mark */
                pos = 0;        /* drop buffer contents */
            } else if (buffer.length >= MAX_BUFFER_SIZE) {
                throw new OutOfMemoryError("Required array size too large");
            } else {            /* grow buffer */
                int newSize = (pos <= MAX_BUFFER_SIZE - pos) ?
                        pos * 2 : MAX_BUFFER_SIZE;
                if (newSize > marklimit)
                    newSize = marklimit;
                byte[] newBuffer = ByteArrayPool.getArray(newSize);
                System.arraycopy(buffer, 0, newBuffer, 0, pos);
                ByteArrayPool.recycleArray(buffer);
                buf = newBuffer;
                buffer = newBuffer;
            }
        count = pos;
        int n = in.read(buffer, pos, buffer.length - pos);
        if (n > 0)
            count = n + pos;
    }

    /**
     * See
     * the general contract of the <code>read</code>
     * method of <code>InputStream</code>.
     *
     * @return the next byte of data, or <code>-1</code> if the end of the
     * stream is reached.
     * @throws IOException if this input stream has been closed by
     *                     invoking its {@link #close()} method,
     *                     or an I/O error occurs.
     */
    public int read() throws IOException {
        if (pos >= count) {
            fill();
            if (pos >= count)
                return -1;
        }
        return buf[pos++] & 0xff;
    }

    /**
     * Read characters into a portion of an array, reading from the underlying
     * stream at most once if necessary.
     */
    private int read1(byte[] b, int off, int len) throws IOException {
        int avail = count - pos;
        if (avail <= 0) {
            /* If the requested length is at least as large as the buffer, and
               if there is no mark/reset activity, do not bother to copy the
               bytes into the local buffer.  In this way buffered streams will
               cascade harmlessly. */
            if (len >= buf.length && markpos < 0) {
                return in.read(b, off, len);
            }
            fill();
            avail = count - pos;
            if (avail <= 0) return -1;
        }
        int cnt = Math.min(avail, len);
        System.arraycopy(buf, pos, b, off, cnt);
        pos += cnt;
        return cnt;
    }

    /**
     * Reads bytes from this byte-input stream into the specified byte array,
     * starting at the given offset.
     *
     * @param b   destination buffer.
     * @param off offset at which to start storing bytes.
     * @param len maximum number of bytes to read.
     * @return the number of bytes read, or <code>-1</code> if the end of
     * the stream has been reached.
     * @throws IOException if this input stream has been closed by
     *                     invoking its {@link #close()} method,
     *                     or an I/O error occurs.
     */
    public int read(byte[] b, int off, int len)
            throws IOException {
        if ((off | len | (off + len) | (b.length - (off + len))) < 0) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return 0;
        }

        int n = 0;
        for (; ; ) {
            int nread = read1(b, off + n, len - n);
            if (nread <= 0)
                return (n == 0) ? nread : n;
            n += nread;
            if (n >= len)
                return n;
            // if not closed but no bytes available, return
            InputStream input = in;
            if (input != null && input.available() <= 0)
                return n;
        }
    }

    /**
     * See the general contract of the <code>skip</code>
     * method of <code>InputStream</code>.
     *
     * @throws IOException if the stream does not support seek,
     *                     or if this input stream has been closed by
     *                     invoking its {@link #close()} method, or an
     *                     I/O error occurs.
     */
    public long skip(long n) throws IOException {
        if (n <= 0) {
            return 0;
        }
        long avail = count - pos;

        if (avail <= 0) {
            // If no mark position set then don't keep in buffer
            if (markpos < 0)
                return in.skip(n);

            // Fill in buffer to save bytes for reset
            fill();
            avail = count - pos;
            if (avail <= 0)
                return 0;
        }

        long skipped = Math.min(avail, n);
        pos += skipped;
        return skipped;
    }

    public int available() throws IOException {
        int n = count - pos;
        int avail = in.available();
        return n > (Integer.MAX_VALUE - avail)
                ? Integer.MAX_VALUE
                : n + avail;
    }

    /**
     * See the general contract of the <code>mark</code>
     * method of <code>InputStream</code>.
     *
     * @param readLimit the maximum limit of bytes that can be read before
     *                  the mark position becomes invalid.
     * @see #reset()
     */
    public void mark(int readLimit) {
        marklimit = readLimit;
        markpos = pos;
    }

    /**
     * See the general contract of the <code>reset</code>
     * method of <code>InputStream</code>.
     * <p>
     * If <code>markpos</code> is <code>-1</code>
     * (no mark has been set or the mark has been
     * invalidated), an <code>IOException</code>
     * is thrown. Otherwise, <code>pos</code> is
     * set equal to <code>markpos</code>.
     *
     * @throws IOException if this stream has not been marked or,
     *                     if the mark has been invalidated, or the stream
     *                     has been closed by invoking its {@link #close()}
     *                     method, or an I/O error occurs.
     */
    public void reset() throws IOException {
        if (markpos < 0)
            throw new IOException("Resetting to invalid mark");
        pos = markpos;
    }

    /**
     * Rewinds the stream, set position to zero and clear mark.
     */
    void rewind() {
        pos = 0;
        markpos = -1;
        marklimit = 0;
    }

    /**
     * Tests if this input stream supports the <code>mark</code>
     * and <code>reset</code> methods. The <code>markSupported</code>
     * method of <code>BufferedInputStream</code> returns
     * <code>true</code>.
     *
     * @return a <code>boolean</code> indicating if this stream type supports
     * the <code>mark</code> and <code>reset</code> methods.
     * @see InputStream#mark(int)
     * @see InputStream#reset()
     */
    public boolean markSupported() {
        return true;
    }

    /**
     * Closes this input stream and releases any system resources
     * associated with the stream.
     * Once the stream has been closed, further read(), available(), reset(),
     * or skip() invocations will throw an IOException.
     * Closing a previously closed stream has no effect.
     *
     * @throws IOException if an I/O error occurs.
     */
    public void close() throws IOException {
        if (in != null) {
            in.close();
            in = null;
        }
        if (buf != null) {
            ByteArrayPool.recycleArray(buf);
            buf = null;
        }
    }
}
