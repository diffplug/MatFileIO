/*-
 * #%L
 * Mat-File IO
 * %%
 * Copyright (C) 2018 HEBI Robotics
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package us.hebi.matlab.mat.types;

import us.hebi.matlab.mat.util.ByteConverter;
import us.hebi.matlab.mat.util.ByteConverters;
import us.hebi.matlab.mat.util.Bytes;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import static us.hebi.matlab.mat.util.Bytes.*;

/**
 * @author Florian Enner
 * @since 26 Aug 2018
 */
public abstract class AbstractSource implements Source {

    @Override
    public AbstractSource order(ByteOrder byteOrder) {
        this.byteOrder = byteOrder;
        return this;
    }

    @Override
    public ByteOrder order() {
        if (byteOrder == null) {
            throw new IllegalStateException("Byte order has not been initialized");
        }
        return byteOrder;
    }

    @Override
    public byte readByte() throws IOException {
        readBytes(bytes, 0, 1);
        return bytes[0];
    }

    @Override
    public short readShort() throws IOException {
        readBytes(bytes, 0, SIZEOF_SHORT);
        return byteConverter.getShort(order(), bytes, 0);
    }

    @Override
    public int readInt() throws IOException {
        readBytes(bytes, 0, SIZEOF_INT);
        return byteConverter.getInt(order(), bytes, 0);
    }

    @Override
    public long readLong() throws IOException {
        readBytes(bytes, 0, SIZEOF_LONG);
        return byteConverter.getLong(order(), bytes, 0);
    }

    @Override
    public float readFloat() throws IOException {
        readBytes(bytes, 0, SIZEOF_FLOAT);
        return byteConverter.getFloat(order(), bytes, 0);
    }

    @Override
    public double readDouble() throws IOException {
        readBytes(bytes, 0, SIZEOF_DOUBLE);
        return byteConverter.getDouble(order(), bytes, 0);
    }

    @Override
    public void readByteBuffer(ByteBuffer buffer) throws IOException {
        if (buffer.hasArray()) {
            // Fast path
            int offset = buffer.arrayOffset() + buffer.position();
            int length = buffer.remaining();
            readBytes(buffer.array(), offset, length);
            buffer.position(buffer.limit());
        } else {
            // Slow path
            while (buffer.hasRemaining()) {
                int length = Math.min(buffer.remaining(), bytes.length);
                readBytes(bytes, 0, length);
                buffer.put(bytes, 0, length);
            }
        }
    }

    @Override
    public void readShorts(short[] buffer, int offset, int length) throws IOException {
        for (int i = 0; i < length; ) {
            int n = Math.min((length - i) * SIZEOF_SHORT, bytes.length);
            readBytes(bytes, 0, n);
            for (int j = 0; j < n; j += SIZEOF_SHORT, i++) {
                buffer[offset + i] = byteConverter.getShort(order(), bytes, j);
            }
        }
    }

    @Override
    public void readInts(int[] buffer, int offset, int length) throws IOException {
        for (int i = 0; i < length; ) {
            int n = Math.min((length - i) * SIZEOF_INT, bytes.length);
            readBytes(bytes, 0, n);
            for (int j = 0; j < n; j += SIZEOF_INT, i++) {
                buffer[offset + i] = byteConverter.getInt(order(), bytes, j);
            }
        }
    }

    @Override
    public void readLongs(long[] buffer, int offset, int length) throws IOException {
        for (int i = 0; i < length; ) {
            int n = Math.min((length - i) * SIZEOF_LONG, bytes.length);
            readBytes(bytes, 0, n);
            for (int j = 0; j < n; j += SIZEOF_LONG, i++) {
                buffer[offset + i] = byteConverter.getLong(order(), bytes, j);
            }
        }
    }

    @Override
    public void readFloats(float[] buffer, int offset, int length) throws IOException {
        for (int i = 0; i < length; ) {
            int n = Math.min((length - i) * SIZEOF_FLOAT, bytes.length);
            readBytes(bytes, 0, n);
            for (int j = 0; j < n; j += SIZEOF_FLOAT, i++) {
                buffer[offset + i] = byteConverter.getFloat(order(), bytes, j);
            }
        }
    }

    @Override
    public void readDoubles(double[] buffer, int offset, int length) throws IOException {
        for (int i = 0; i < length; ) {
            int n = Math.min((length - i) * SIZEOF_DOUBLE, bytes.length);
            readBytes(bytes, 0, n);
            for (int j = 0; j < n; j += SIZEOF_DOUBLE, i++) {
                buffer[offset + i] = byteConverter.getDouble(order(), bytes, j);
            }
        }
    }

    @Override
    public void skip(long numBytes) throws IOException {
        long n = 0;
        while (n < numBytes) {
            long count = Math.min(numBytes - n, bytes.length);
            readBytes(bytes, 0, (int) count);
            n += count;
        }
    }

    @Override
    public Source readInflated(int numBytes, int inflateBufferSize) throws IOException {
        InputStream subInputStream = readBytesAsStream(numBytes);
        InputStream inflaterInput = new InflaterInputStream(subInputStream, new Inflater(), inflateBufferSize);
        return Sources.wrapInputStream(inflaterInput, bytes.length).order(order());
    }

    /**
     * @return stream that reads up to the number of specified bytes. Close() shall not close this source
     */
    protected abstract InputStream readBytesAsStream(long numBytes) throws IOException;

    protected AbstractSource(int bufferSize) {
        // Make sure size is always a multiple of 8, and that it can hold the 116 byte description
        int size = Math.max(Bytes.nextPowerOfTwo(bufferSize), 128);
        this.bytes = new byte[size];
    }

    private ByteOrder byteOrder = null;
    private final byte[] bytes;
    private static final ByteConverter byteConverter = ByteConverters.getFastest();

}
