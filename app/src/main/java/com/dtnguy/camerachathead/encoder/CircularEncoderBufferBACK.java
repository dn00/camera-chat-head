package com.dtnguy.camerachathead.encoder;///*
// * Copyright 2014 Google Inc. All rights reserved.
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *      http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//package com.dtnguy.presnap4.encoder;
//
//
//import android.media.MediaCodec;
//import android.media.MediaFormat;
//import android.util.Log;
//
//import java.nio.ByteBuffer;
//
///**
// * Holds encoded video data in a circular buffer.
// * <p>
// * This is actually a pair of circular buffers, one for the raw data and one for the meta-data
// * (flags and PTS).
// * <p>
// * Not thread-safe.
// */
//public class CircularEncoderBufferBACK {
//    private static final String TAG = "CircularEncoderBuffer";
//    private static final boolean EXTRA_DEBUG = true;
//    private static final boolean VERBOSE = false;
//    private String buftype;
//
//
//
//    // Raw data (e.g. AVC NAL units) held here.
//    //
//    // The MediaMuxer writeSampleData() function takes a ByteBuffer.  If it's a "direct"
//    // ByteBuffer it'll access the data directly, if it's a regular ByteBuffer it'll use
//    // JNI functions to access the backing byte[] (which, in the current VM, is done without
//    // copying the data).
//    //
//    // It's much more convenient to work with a byte[], so we just wrap it with a ByteBuffer
//    // as needed.  This is a bit awkward when we hit the edge of the buffer, but for that
//    // we can just do an allocation and data copy (we know it happens at most once per file
//    // save operation).
//    private ByteBuffer mDataBufferWrapper;
//    private ByteBuffer audiomDataBufferWrapper;
//    private byte[] mDataBuffer;
//    private byte[] audiomDataBuffer;
//
//    // Meta-data held here.  We're using a collection of arrays, rather than an array of
//    // objects with multiple fields, to minimize allocations and heap footprint.
//    private int[] mPacketFlags;
//    private long[] mPacketPtsUsec;
//    private int[] mPacketStart;
//    private int[] mPacketLength;
//
//    // Data is added at head and removed from tail.  Head points to an empty node, so if
//    // head==tail the list is empty.
//    private int mMetaHead;
//    private int mMetaTail;
//
//    private int bitRate;
//    private int dataBufferSize;
//    private double mTimePerPacketMs;
//    /**
//     * Allocates the circular buffers we use for encoded data and meta-data.
//     */
//    public CircularEncoderBufferBACK(MediaFormat mediaFormat, int desiredSpanSec) {
//        // For the encoded data, we assume the encoded bit rate is close to what we request.
//        //
//        // There would be a minor performance advantage to using a power of two here, because
//        // not all ARM CPUs support integer modulus.
//        bitRate = mediaFormat.getInteger(MediaFormat.KEY_BIT_RATE);
//        //buftype = "video";
//        dataBufferSize = bitRate * desiredSpanSec / 8;
//        mDataBuffer = new byte[dataBufferSize];
//        mDataBufferWrapper = ByteBuffer.wrap(mDataBuffer);
//
//        int mSpanMs = (int) (((long) 1000 * 8 * dataBufferSize)/(bitRate));
//
//        // We want to calculate how many packets fit in our mBufferSize
//        String mimeType = mediaFormat.getString(MediaFormat.KEY_MIME);
//        boolean isVideo = mimeType.equals(MediaFormat.MIMETYPE_VIDEO_AVC);
//        boolean isAudio = mimeType.equals(MediaFormat.MIMETYPE_AUDIO_AAC);
//        double packetSize;
//        double  packetsPerSecond;
//        if (isVideo) {
//            packetsPerSecond = mediaFormat.getInteger(MediaFormat.KEY_FRAME_RATE);
//            buftype = "Video";
//        }
//        else if (isAudio) {
//            double sampleRate = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
//            packetsPerSecond = sampleRate/1024;
//            buftype = "Audio";
//        }
//        else {
//            throw new RuntimeException("Media format provided is neither AVC nor AAC");
//        }
//        mTimePerPacketMs =  1000./packetsPerSecond;
//        packetSize = (bitRate/packetsPerSecond)/8;
//        int estimatedPacketCount = (int) (dataBufferSize/packetSize + 1);
//        // Meta-data is smaller than encoded data for non-trivial frames, so we over-allocate
//        // a bit.  This should ensure that we drop packets because we ran out of (expensive)
//        // data storage rather than (inexpensive) metadata storage.
//        //int metaBufferCount = frameRate * desiredSpanSec * 2;
//        int metaBufferCount = estimatedPacketCount * 2;
//        mPacketFlags = new int[metaBufferCount];
//        mPacketPtsUsec = new long[metaBufferCount];
//        mPacketStart = new int[metaBufferCount];
//        mPacketLength = new int[metaBufferCount];
//
//        //double packetsPerSecond = frameRate;
//        //double mTimePerPacketMs = 1000./packetsPerSecond;
//        //Log.d(TAG, "mTimePerPacketMs: " + mTimePerPacketMs);
//
//        Log.d(TAG, "BitRate=" + bitRate +
//                " span=" + String.format("%,d", mSpanMs) + "msec" +
//                " buffer size=" + String.format("%,d", dataBufferSize / 1000) + "kB" +
//                " packet count=" + metaBufferCount);
////        if (VERBOSE) {
////            Log.d(TAG, "CBE: bitRate=" + bitRate + " frameRate=" + frameRate +
////                    " desiredSpan=" + desiredSpanSec + ": dataBufferSize=" + dataBufferSize +
////                    " metaBufferCount=" + metaBufferCount);
////        }
//    }
//    public CircularEncoderBufferBACK(int bitRate, int sampleRate, int desiredSpanSec, boolean b) {
//        // For the encoded data, we assume the encoded bit rate is close to what we request.
//        //
//        // There would be a minor performance advantage to using a power of two here, because
//        // not all ARM CPUs support integer modulus.
//        buftype = "audio";
//
//        int dataBufferSize = bitRate * desiredSpanSec / 8;
//        audiomDataBuffer = new byte[dataBufferSize];
//        audiomDataBufferWrapper = ByteBuffer.wrap(audiomDataBuffer);
//        mDataBufferWrapper = audiomDataBufferWrapper;
//        mDataBuffer = audiomDataBuffer;
//
//        // Meta-data is smaller than encoded data for non-trivial frames, so we over-allocate
//        // a bit.  This should ensure that we drop packets because we ran out of (expensive)
//        // data storage rather than (inexpensive) metadata storage.
//        int metaBufferCount = ((dataBufferSize/(bitRate/sampleRate)/8) + 1) * 2;
//        mPacketFlags = new int[metaBufferCount];
//        mPacketPtsUsec = new long[metaBufferCount];
//        mPacketStart = new int[metaBufferCount];
//        mPacketLength = new int[metaBufferCount];
//
//        //double packetsPerSecond = frameRate;
//        //double mTimePerPacketMs = 1000./packetsPerSecond;
//        //Log.d(TAG, "mTimePerPacketMs: " + mTimePerPacketMs);
//
////        if (VERBOSE) {
////            Log.d(TAG, "CBE: bitRate=" + bitRate + " frameRate=" + frameRate +
////                    " desiredSpan=" + desiredSpanSec + ": dataBufferSize=" + dataBufferSize +
////                    " metaBufferCount=" + metaBufferCount);
////        }
//    }
//    public String getType()
//    {
//        return buftype;
//    }
//    /**
//     * Computes the amount of time spanned by the buffered data, based on the presentation
//     * time stamps.
//     */
//    public long computeTimeSpanUsec() {
//        final int metaLen = mPacketStart.length;
//
//        if (mMetaHead == mMetaTail) {
//            // empty list
//            return 0;
//        }
//
//        // head points to the next available node, so grab the previous one
//        int beforeHead = (mMetaHead + metaLen - 1) % metaLen;
//        return mPacketPtsUsec[beforeHead] - mPacketPtsUsec[mMetaTail];
//    }
//
//    /**
//     * Adds a new encoded data packet to the buffer.
//     *
//     * @param buf The data.  Set position() to the start offset and limit() to position+size.
//     *     The position and limit may be altered by this method.
//     **//* @param size Number of bytes in the packet.
//     * @param flags MediaCodec.BufferInfo flags.
//     * @param ptsUsec Presentation time stamp, in microseconds.
//     */
//    public void add(ByteBuffer buf, int flags, long ptsUsec) {
//        int size = buf.limit() - buf.position();
//        if (VERBOSE) {
//            Log.d(TAG, "add size=" + size + " flags=0x" + Integer.toHexString(flags) +
//                    " pts=" + ptsUsec);
//        }
//        //Log.d(TAG," adding " + buftype + " " + ptsUsec );
//        while (!canAdd(size)) {
//            removeTail();
//        }
//        final int dataLen = mDataBuffer.length;
//        final int metaLen = mPacketStart.length;
//        int packetStart = getHeadStart();
//        mPacketFlags[mMetaHead] = flags;
//        mPacketPtsUsec[mMetaHead] = ptsUsec;
//        mPacketStart[mMetaHead] = packetStart;
//        mPacketLength[mMetaHead] = size;
//
//        // Copy the data in.  Take care if it gets split in half.
//        if (packetStart + size < dataLen) {
//            // one chunk
//            buf.get(mDataBuffer, packetStart, size);
//        } else {
//            // two chunks
//            int firstSize = dataLen - packetStart;
//            if (VERBOSE) { Log.v(TAG, "split, firstsize=" + firstSize + " size=" + size); }
//            buf.get(mDataBuffer, packetStart, firstSize);
//            buf.get(mDataBuffer, 0, size - firstSize);
//        }
//
//        mMetaHead = (mMetaHead + 1) % metaLen;
//
//        if (EXTRA_DEBUG) {
//            // The head packet is the next-available spot.
//            mPacketFlags[mMetaHead] = 0x77aaccff;
//            mPacketPtsUsec[mMetaHead] = -1000000000L;
//            mPacketStart[mMetaHead] = -100000;
//            mPacketLength[mMetaHead] = Integer.MAX_VALUE;
//        }
//    }
//
//    /**
//     * Returns the index of the oldest sync frame.  Valid until the next add().
//     * <p>
//     * When sending output to a MediaMuxer, start here.
//     */
//    public int getFirstIndex() {
//        final int metaLen = mPacketStart.length;
//
//        int index = mMetaTail;
//        while (index != mMetaHead) {
//            if ((mPacketFlags[index] & MediaCodec.BUFFER_FLAG_SYNC_FRAME) != 0) {
//                break;
//            }
//            index = (index + 1) % metaLen;
//        }
//
//        if (index == mMetaHead) {
//            Log.w(TAG, "HEY: could not find sync frame in buffer");
//            index = -1;
//        }
//        return index;
//    }
//    //    public ByteBuffer getTailChunk(MediaCodec.BufferInfo info) {
////        int index = getFirstIndex();
////        return getChunk(index, info);
////    }
////    public int getNSecondsIndex(int n) {
////        final int metaLen = mPacketStart.length;
////        //TODO change hardcoded 15 to variable frame rate
////        //int index = mMetaTail;
////        int index = (mMetaHead + metaLen - (n*15 + 1)) % metaLen;
////        while (index != mMetaHead) {
////            if ((mPacketFlags[index] & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0) {
////                return index;
////            }
////            index = (index + 1) % metaLen;
////        }
////        return index;
////    }
//    public int getNSecondsIndex(int n) {
//        final int metaLen = mPacketStart.length;
//        int index = (mMetaHead + metaLen - 1) % metaLen;
//        final long s = mPacketPtsUsec[index] - (1000000L * n);
//        if (s <= 0)
//        {
//            return getFirstIndex();
//        }
//        while (mPacketPtsUsec[index] >= s)
//        {
//            index = (index + metaLen - 1) % metaLen;
//        }
//        return index;
//    }
//    public ByteBuffer getSingleFrame(int index, MediaCodec.BufferInfo info) {
//        final int dataLen = mDataBuffer.length;
//        int packetStart = mPacketStart[index];
//        int length = mPacketLength[index];
//
//        info.flags = mPacketFlags[index];
//        info.offset = packetStart;
//        info.presentationTimeUs = mPacketPtsUsec[index];
//        info.size = length;
//
//        ByteBuffer tempBuf = ByteBuffer.allocateDirect(1);
//        tempBuf.put(mDataBuffer,mPacketStart[index], mPacketLength[index]);
//        info.offset = 0;
//        return tempBuf;
//    }
//    public ByteBuffer getNSecondsChunk(int index, MediaCodec.BufferInfo info) {
//        final int dataLen = mDataBuffer.length;
//        int packetStart = mPacketStart[index];
//        int length = mPacketLength[index];
//
//        info.flags = mPacketFlags[index];
//        info.offset = packetStart;
//        info.presentationTimeUs = mPacketPtsUsec[index];
//        info.size = length;
//        ByteBuffer tempBuf = ByteBuffer.allocateDirect(length);
//        int firstSize = dataLen - packetStart;
//        tempBuf.put(mDataBuffer, mPacketStart[index], firstSize);
//        info.offset = 0;
//        return tempBuf;
//    }
//    /**
//     * Returns the index of the next packet, or -1 if we've reached the end.
//     */
//    public int getNextIndex(int index) {
//        final int metaLen = mPacketStart.length;
//        int next = (index + 1) % metaLen;
//        //Log.d(TAG,"next: " + next);
////        if (video) {
////            if (next == mMetaHead) {
////                next = -1;
////            }
////        } else {
////            next = (next + 1) % metaLen;
////        }
//        if (next == mMetaHead) {
//            next = -1;
//            //next = (next + metaLen + 1) % metaLen;
//        }
////        if (next == mMetaHead) {
////            next = -1;
////        }
//        return next;
//    }
//
//    /**
//     * Returns a reference to a "direct" ByteBuffer with the data, and fills in the
//     * BufferInfo.
//     * <p>
//     * The caller must not modify the contents of the returned ByteBuffer.  Altering
//     * the position and limit is allowed.
//     */
//    public ByteBuffer getChunk(int index, MediaCodec.BufferInfo info) {
//        final int dataLen = mDataBuffer.length;
//        int packetStart = mPacketStart[index];
//        int length = mPacketLength[index];
//
//        info.flags = mPacketFlags[index];
//        info.offset = packetStart;
//        info.presentationTimeUs = mPacketPtsUsec[index];
//        info.size = length;
//
//        if (packetStart + length <= dataLen) {
//            // one chunk; return full buffer to avoid copying data
//            return mDataBufferWrapper;
//        } else {
//            // two chunks
//            ByteBuffer tempBuf = ByteBuffer.allocateDirect(length);
//            int firstSize = dataLen - packetStart;
//            tempBuf.put(mDataBuffer, mPacketStart[index], firstSize);
//            tempBuf.put(mDataBuffer, 0, length - firstSize);
//            info.offset = 0;
//            return tempBuf;
//        }
//    }
////    public ByteBuffer getNSecondsChunk(int index, MediaCodec.BufferInfo info) {
////        final int dataLen = mDataBuffer.length;
////        int packetStart = mPacketStart[index];
////        int length = mPacketLength[index];
////
////        info.flags = mPacketFlags[index];
////        info.offset = packetStart;
////        info.presentationTimeUs = mPacketPtsUsec[index];
////        info.size = length;
////
////        if (packetStart + length <= dataLen) {
////            // one chunk; return full buffer to avoid copying data
////            return mDataBufferWrapper;
////        } else {
////            // two chunks
////            ByteBuffer tempBuf = ByteBuffer.allocateDirect(length);
////            int firstSize = dataLen - packetStart;
////            tempBuf.put(mDataBuffer, packetStart, firstSize);
////            tempBuf.put(mDataBuffer, 0, length - firstSize);
////            info.offset = 0;
////            return tempBuf;
////        }
////    }
//
//    public int getPacketNum() {
//        // We don't count the +1 meta slot reserved for the head.
//        final int metaLen = mPacketStart.length;
//
//        int usedMeta = (mMetaHead + metaLen - mMetaTail) % metaLen ;
//        return usedMeta;
//    }
//    /**
//     * Computes the data buffer offset for the next place to store data.
//     * <p>
//     * Equal to the start of the previous packet's data plus the previous packet's length.
//     */
//
//    private int getHeadStart() {
//        if (mMetaHead == mMetaTail) {
//            // list is empty
//            return 0;
//        }
//
//        final int dataLen = mDataBuffer.length;
//        final int metaLen = mPacketStart.length;
//
//        int beforeHead = (mMetaHead + metaLen - 1) % metaLen;
//        return (mPacketStart[beforeHead] + mPacketLength[beforeHead] + 1) % dataLen;
//    }
//
//    /**
//     * Determines whether this is enough space to fit "size" bytes in the data buffer, and
//     * one more packet in the meta-data buffer.
//     *
//     * @return True if there is enough space to add without removing anything.
//     */
//    private boolean canAdd(int size) {
//        final int dataLen = mDataBuffer.length;
//        final int metaLen = mPacketStart.length;
//
//        if (size > dataLen) {
//            throw new RuntimeException("Enormous packet: " + size + " vs. buffer " +
//                    dataLen);
//        }
//        if (mMetaHead == mMetaTail) {
//            // empty list
//            return true;
//        }
//
//        // Make sure we can advance head without stepping on the tail.
//        int nextHead = (mMetaHead + 1) % metaLen;
//        if (nextHead == mMetaTail) {
//            if (VERBOSE) {
//                Log.v(TAG, "ran out of metadata (head=" + mMetaHead + " tail=" + mMetaTail +")");
//            }
//            return false;
//        }
//
//        // Need the byte offset of the start of the "tail" packet, and the byte offset where
//        // "head" will store its data.
//        int headStart = getHeadStart();
//        int tailStart = mPacketStart[mMetaTail];
//        int freeSpace = (tailStart + dataLen - headStart) % dataLen;
//        if (size > freeSpace) {
//            if (VERBOSE) {
//                Log.v(TAG, "ran out of data (tailStart=" + tailStart + " headStart=" + headStart +
//                        " req=" + size + " free=" + freeSpace + ")");
//            }
//            return false;
//        }
//
//        if (VERBOSE) {
//            Log.v(TAG, "OK: size=" + size + " free=" + freeSpace + " metaFree=" +
//                    ((mMetaTail + metaLen - mMetaHead) % metaLen - 1));
//        }
//
//        return true;
//    }
//
//    /**
//     * Removes the tail packet.
//     */
//    private void removeTail() {
//        if (mMetaHead == mMetaTail) {
//            throw new RuntimeException("Can't removeTail() in empty buffer");
//        }
//        final int metaLen = mPacketStart.length;
//        mMetaTail = (mMetaTail + 1) % metaLen;
//    }
//}





//package com.dtnguy.presnap4.encoder;
//
//import android.media.MediaCodec;
//import android.media.MediaFormat;
//import android.util.Log;
//
//
//import java.lang.reflect.Array;
//import java.nio.ByteBuffer;
//import java.nio.ByteOrder;
//
///**
// * Created by Petrakeas on 7/10/2015.
// */
//public class CircularEncoderBuffer {
//    private static final String TAG = "CircularBuffer";
//
//
//    // Raw data (e.g. AVC NAL units) held here.
//    //
//    // The MediaMuxer writeSampleData() function takes a ByteBuffer.  If it's a "direct"
//    // ByteBuffer it'll access the data directly, if it's a regular ByteBuffer it'll use
//    // JNI functions to access the backing byte[] (which, in the current VM, is done without
//    // copying the data).
//    private ByteBuffer[] mDataBuffer;
//    int mBuffersNum;
//    private ByteOrder mOrder;
//    private int mSpanMs;
//    private int mTotalSpanMs;
//    private int mBufferSize;
//    private int mTotalBufferSize;
//
//    // Meta-data held here.  We're using a collection of arrays, rather than an array of
//    // objects with multiple fields, to minimize allocations and heap footprint.
//    private int[] mPacketFlags;
//    private long[] mPacketPtsUs;
//    private int[] mPacketStart;
//    private int[] mPacketLength;
//    private int mMetaLength;
//    private int mSingleBufferMetaLength;
//
//    // Data is added at head and removed from tail.  Head points to an empty node, so if
//    // head==tail the list is empty. We lose one slot with this convention because if we wrap around
//    // we'll still need to keep one free slot for the head.
//    private int mMetaHead;
//    private int mMetaTail;
//
//    private int mBitrate;
//    private double mTimePerPacketMs;
//
//
//    /**
//     * Allocates the circular buffers we use for encoded data and meta-data.
//     */
//    public CircularEncoderBuffer(MediaFormat mediaFormat, int desiredSpanMs) {
//        mBuffersNum = 1;
//        mDataBuffer = new ByteBuffer[1];
//
//        // For the encoded data, we assume the encoded bit rate is close to what we request.
//        //
//        // There would be a minor performance advantage to using a power of two here, because
//        // not all ARM CPUs support integer modulus.
//        mBitrate = mediaFormat.getInteger(MediaFormat.KEY_BIT_RATE);
//        mBufferSize = (int)((long) mBitrate * desiredSpanMs / (8 * 1000));
//
//        // Try to allocate mBufferSize bytes or at least minBufferSize bytes.
//        int minBufferSize = mBitrate/8;
//        mDataBuffer[0] = ByteBuffer.allocateDirect(mBufferSize);
//        mBufferSize = mDataBuffer[0].capacity();
//        mTotalBufferSize = mBufferSize;
//        mSpanMs = (int) (((long) 1000 * 8 * mBufferSize)/(mBitrate));
//        mTotalSpanMs = mSpanMs;
//
//        // We want to calculate how many packets fit in our mBufferSize
//        String mimeType = mediaFormat.getString(MediaFormat.KEY_MIME);
//        boolean isVideo = mimeType.equals(MediaFormat.MIMETYPE_VIDEO_AVC);
//        boolean isAudio = mimeType.equals(MediaFormat.MIMETYPE_AUDIO_AAC);
//        double packetSize;
//        double  packetsPerSecond;
//        if (isVideo) {
//            packetsPerSecond = mediaFormat.getInteger(MediaFormat.KEY_FRAME_RATE);
//        }
//        else if (isAudio) {
//            double sampleRate = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
//            packetsPerSecond = sampleRate/1024;
//        }
//        else {
//            throw new RuntimeException("Media format provided is neither AVC nor AAC");
//        }
//        mTimePerPacketMs =  1000./packetsPerSecond;
//        packetSize = (mBitrate/packetsPerSecond)/8;
//        int estimatedPacketCount = (int) (mBufferSize/packetSize + 1);
//        // Meta-data is smaller than encoded data for non-trivial frames, so we over-allocate
//        // a bit.  This should ensure that we drop packets because we ran out of (expensive)
//        // data storage rather than (inexpensive) metadata storage.
//        mMetaLength = estimatedPacketCount * 2;
//        mSingleBufferMetaLength = mMetaLength;
//        mPacketFlags = new int[mMetaLength];
//        mPacketPtsUs = new long[mMetaLength];
//        mPacketStart = new int[mMetaLength];
//        mPacketLength = new int[mMetaLength];
//
//        Log.d(TAG, "BitRate=" + mBitrate +
//                " span=" + String.format("%,d", mSpanMs) + "msec" +
//                " buffer size=" + String.format("%,d", mBufferSize / 1000) + "kB" +
//                " packet count=" + mMetaLength);
//
//    }
//
//    public boolean increaseSize() {
//        // allocate another buffer
//        mDataBuffer = increaseArraySize(mDataBuffer, mDataBuffer.length, ByteBuffer[].class, 1);
//        int lastBufferId = mDataBuffer.length - 1;
//        try {
//            mDataBuffer[lastBufferId] = ByteBuffer.allocateDirect(mBufferSize);
//        }
//        catch (OutOfMemoryError E) {
//            Log.w(TAG, "Could not allocate memory to increase size.");
//            return false;
//        }
//        if (mDataBuffer[lastBufferId].capacity() != mBufferSize) {
//            throw new RuntimeException("Allocated size can't be different.");
//        }
//        mDataBuffer[lastBufferId].order(mOrder);
//        mTotalBufferSize += mBufferSize;
//        mTotalSpanMs += mSpanMs;
//
//        // increase meta array size
//        mPacketFlags = increaseArraySize(mPacketFlags, mMetaLength, int[].class, mSingleBufferMetaLength, mMetaTail, mMetaHead);
//        mPacketPtsUs = increaseArraySize(mPacketPtsUs, mMetaLength, long[].class, mSingleBufferMetaLength, mMetaTail, mMetaHead);
//        mPacketStart = increaseArraySize(mPacketStart, mMetaLength, int[].class, mSingleBufferMetaLength, mMetaTail, mMetaHead);
//        mPacketLength = increaseArraySize(mPacketLength, mMetaLength, int[].class, mSingleBufferMetaLength, mMetaTail, mMetaHead);
//        int packetsUsed = getPacketNum();
//        mMetaLength += mSingleBufferMetaLength;
//        mMetaHead = (mMetaTail + packetsUsed) % mMetaLength;
//
//        // Move packets so that we don't wrap around the buffer.
//        //TODO: instead of moving them one by one, move them by buffer. it's X10 faster
//        int index = getFirstIndex();
//        index = getNextIndex(index);        // tail packet is excluded
//        boolean shouldMove = false;
//        while (index >= 0) {
//            if (mPacketStart[index] == 0) { // the packets from this point on should be moved
//                shouldMove = true;
//            }
//            if (shouldMove) {
//                move(index);
//            }
//            index = getNextIndex(index);
//        }
//
//        Log.d(TAG, "Buffer size increased. BitRate=" + mBitrate +
//                " span=" + String.format("%,d", mTotalSpanMs) + "msec" +
//                " buffer size=" + String.format("%,d", mTotalBufferSize / 1000) + "kB" +
//                " packet count=" + mMetaLength);
//
//        return true;
//    }
//
//    /**
//     * Creates and returns a new array of arrayType with size: sourceSize + sizeIncrement. Also, it
//     * copies all the elements of the source array to the new array without re-ordering them.
//     */
//    private static <A> A increaseArraySize (A sourceArray, int sourceSize, Class<A> arrayType, int sizeIncrement) {
//        int newSize = sourceSize + sizeIncrement;
//        A newArray = arrayType.cast(Array.newInstance(arrayType.getComponentType(), newSize));
//        System.arraycopy(sourceArray, 0, newArray, 0, sourceSize);
//        return newArray;
//    }
//
//    /**
//     * Creates and returns a new array of arrayType with size: sourceSize + sizeIncrement. The elements
//     * of the source array are copied to the new array in a new order according to the provided tail
//     * and head index of the original array.
//     *
//     * The index of the tail remains the same. The following elements are added after until the head
//     * is reached. The elements may be wrapped-around the new array.
//     */
//    private static <A> A increaseArraySize (A sourceArray, int sourceSize, Class<A> arrayType, int sizeIncrement, int tailIndex, int headIndex) {
//        int newSize = sourceSize + sizeIncrement;
//        A newArray = arrayType.cast(Array.newInstance(arrayType.getComponentType(), newSize));
//        if (headIndex > tailIndex) { // source elements are not wrapped around
//            System.arraycopy(sourceArray, tailIndex, newArray, tailIndex, headIndex - tailIndex);
//        }
//        else { // source elements are wrapped around
//            System.arraycopy(sourceArray, tailIndex, newArray, tailIndex, sourceSize - tailIndex);
//            int remainSize = headIndex - 0;
//            if (remainSize <= sizeIncrement) { // can fit in newArray without wrapping around
//                System.arraycopy(sourceArray, 0, newArray, sourceSize, remainSize);
//            }
//            else { // we have to wrap-around in the new array
//                System.arraycopy(sourceArray, 0, newArray, sourceSize, sizeIncrement);
//                int secondPartSize = remainSize - sizeIncrement;
//                System.arraycopy(sourceArray, sizeIncrement, newArray, 0, secondPartSize);
//            }
//        }
//        return newArray;
//    }
//
//    public boolean isEmpty() {
//        return (mMetaHead == mMetaTail);
//    }
//    /**
//     * Returns the index of the tail which corresponds to the oldest packet. Valid until the
//     * next removeTail().
//     */
//    public int getFirstIndex() {
//        if (isEmpty()) {
//            return -1;
//        }
//        return mMetaTail;
//    }
//    ///Make sure n is > 0
//getNSecondsIndex
//    public long getPTS(int index, long pts) {
//        if (index < 0)
//        {
//            return pts;
//        }
//        return mPacketPtsUs[index];
//    }
//    /**
//     * Returns the index of the next packet, or -1 if we've reached the end.
//     */
//    public int getNextIndex(int index) {
//        int next = (index + 1) % mMetaLength;
////        if (next == getAfterHeadIndex(1)) {
////            next = -1;
////        }
//        if (next == mMetaHead) {
//            next = -1;
//        }
//        return next;
//    }
//
//    /**
//     * Returns the index of the next packet, or -1 if we've passed the tail.
//     */
//    public int getPreviousIndex(int index) {
//        if (index == mMetaTail) {
//            return -1;
//        }
//        int previous = (index - 1 + mMetaLength) % mMetaLength;
//        return previous;
//    }
//
//    /**
//     * Returns the index before the head which corresponds to the newest packet (newest). Valid until
//     * the next add() or increaseSize().
//     */
//    public int getLastIndex() {
//        if (isEmpty()) {
//            return -1;
//        }
//        return (mMetaHead + mMetaLength - 1) % mMetaLength;
//    }
//
//    /**
//     * Returns the current total number of added packets.
//     */
//    public int getPacketNum() {
//        // We don't count the +1 meta slot reserved for the head.
//        int usedMeta = (mMetaHead + mMetaLength - mMetaTail) % mMetaLength ;
//        return usedMeta;
//    }
//
//    private int getFreeMeta() {
//        int packetNum = getPacketNum();
//        // we subtrack 1 slot, for the space reserved for the head
//        int freeMeta = mMetaLength - packetNum -1;
//        return freeMeta;
//    }
//
//    /**
//     * Computes the data buffer offset for the next place to store data.
//     * <p/>
//     * Equal to the start of the previous packet's data plus the previous packet's length.
//     */
//    private int getHeadStart() {
//        if (isEmpty()) {
//            return 0;
//        }
//        int beforeHead = getLastIndex();
//        return (mPacketStart[beforeHead] + mPacketLength[beforeHead]) % mTotalBufferSize;
//    }
//
//    /**
//     * Returns the free space from the specified headstart until the tail of the data buffer.
//     */
//    private int getFreeSpace(int headStart) {
//        if (isEmpty()) {
//            return mTotalBufferSize;
//        }
//        // Need the byte offset of the start of the "tail" packet, and the byte offset where
//        // "head" will store its data.
//        int tailStart = mPacketStart[mMetaTail];
//        int freeSpace = (tailStart + mTotalBufferSize - headStart) % mTotalBufferSize;
//        return freeSpace;
//    }
//
//    private int getUsedSpace() {
//        if (isEmpty()) {
//            return 0;
//        }
//        int freeSpace = getFreeSpace(getHeadStart());
//        int usedSpace = mTotalBufferSize - freeSpace;
//        return usedSpace;
//    }
//
//    /**
//     * Computes the amount of time spanned by the buffered data, based on the presentation
//     * time stamps.
//     */
//    private double computeTimeSpanMs() {
//        if (isEmpty()) {
//            return 0;
//        }
//        double timeSpan = 0;
//        int index = getFirstIndex();
//        while (index >= 0) {
//            timeSpan += mTimePerPacketMs;
//            index = getNextIndex(index);
//        }
//
//        return timeSpan;
//    }
//
//    private void printStatus() {
//        int usedSpace = getUsedSpace();
//        double usedSpacePercent = 100. * (double) usedSpace / mTotalBufferSize;
//        String usedSpaceString = String.format("%.2f", usedSpacePercent);
//
//        int usedMeta = getPacketNum();
//
//        Log.v(TAG, "Used " + usedSpaceString + "% from  " + String.format("%,d", mTotalBufferSize / 1000) + "kB"
//                + ", meta used=" + usedMeta + "/" + (mMetaLength - 1));
//    }
//    public int getNSecondsIndex(int n) {
//
//        //final int metaLen = mPacketStart.length;
//        int index = (mMetaHead + mMetaLength - 1) % mMetaLength;
//        final long s = mPacketPtsUs[index] - (1000000L * n);
//        //Log.d(TAG,"ZERO: " + mPacketPtsUs[mMetaLength - 1]);
//        if (mPacketPtsUs[mMetaLength - 1] <= 0)
//        {
//            return 0;
//        }
//        while (mPacketPtsUs[index] >= s)
//        {
//            index = (index + mMetaLength - 1) % mMetaLength;
//        }
//        return index;
//    }
//    public long getNSecondsPTS(int n) {
//
//        final int metaLen = mPacketStart.length;
//        int index = (mMetaHead + mMetaLength - 1) % mMetaLength;
//        final long s = mPacketPtsUs[index] - (1000000L * n);
//        //Log.d(TAG,"ZERO: " + mPacketPtsUs[mMetaLength - 1]);
//        while (mPacketPtsUs[index] >= s)
//        {
//            index = (index + mMetaLength - 1) % mMetaLength;
//            if (index == mMetaHead) {
//                return -1;
//            }
//        }
//        return mPacketPtsUs[index];
//    }
//    public int findPTSIndex(long pts) {
//        int index = (mMetaHead + mMetaLength - 1) % mMetaLength;
//        while (mPacketPtsUs[index] > pts)
//        {
//            index = (index + mMetaLength - 1) % mMetaLength;
//            if (index == mMetaHead) {
//                return -1;
//            }
//        }
//        return index;
//    }
//    /**
//     * Adds a new encoded data packet to the buffer.
//     *
//     * @return the index where the packet was stored or -1 if it failed to add the packet.
//     */
//    public int add(ByteBuffer buf, MediaCodec.BufferInfo info) {
//        int size = info.size;
////        Log.d(TAG, "add size=" + size + " flags=0x" + Integer.toHexString(info.flags) +
////                    " pts=" + info.presentationTimeUs);
////
//
//        if (mOrder == null) {
//            mOrder = buf.order();
//            for (int i = 0; i < mDataBuffer.length; i++) {
//                mDataBuffer[i].order(mOrder);
//            }
//        }
//        if (mOrder != buf.order()) {
//            throw new RuntimeException("Byte ordering changed");
//        }
//
//        if (!canAdd(size)) {
//            //return -1;
//            removeTail();
//        }
//
//        int headStart = getHeadStart();
//        // Check if we have to write to the beginning of the next/same data buffer.
//        int bufferStart =  (headStart / mBufferSize) * mBufferSize;      // 0 for single buffer
//        int bufferEnd = bufferStart + mBufferSize -1;
//        if (headStart + size -1 > bufferEnd) {
//            headStart = (bufferStart + mBufferSize) % mTotalBufferSize; // 0 for single buffer
//        }
//
//        int packetStart = headStart % mBufferSize;          // always 0 when changing buffer
//        int bufferId = headStart / mBufferSize;                         // 0 for single buffer
//
//        buf.limit(info.offset + info.size);
//        buf.position(info.offset);
//        mDataBuffer[bufferId].limit(packetStart + info.size);
//        mDataBuffer[bufferId].position(packetStart);
//        mDataBuffer[bufferId].put(buf);
//
//        mPacketFlags[mMetaHead] = info.flags;
//        mPacketPtsUs[mMetaHead] = info.presentationTimeUs;
//        mPacketStart[mMetaHead] = headStart;
//        mPacketLength[mMetaHead] = size;
//
//        int currentIndex = mMetaHead;
//        mMetaHead = (mMetaHead + 1) % mMetaLength;
//
////        if (HorizonApp.VERBOSE) {
////            printStatus();
////        }
//
//        return currentIndex;
//    }
//
//    /**
//     * Determines whether this is enough space to fit "size" bytes in the data buffer, and
//     * a packet in each meta-data array.
//     *
//     * @return True if there is enough space to add without removing anything.
//     */
//    private boolean canAdd(int size) {
//        if (size > mBufferSize) {
//            throw new RuntimeException("Enormous packet: " + size + " vs. buffer " +
//                    mBufferSize);
//        }
//        if (isEmpty()) {
////            if (HorizonApp.VERBOSE) {
////                int headStart = getHeadStart();
////                int freeSpace = getFreeSpace(headStart);
////                Log.v(TAG, "OK headStart=" + String.format("%,d", headStart) +
////                        " req=" + size + " free=" + freeSpace + ")");
////            }
//            return true;
//        }
//
//        // Make sure we can advance head without stepping on the tail.
//        int nextHead = (mMetaHead + 1) % mMetaLength;
//        if (nextHead == mMetaTail) {
////            if (HorizonApp.VERBOSE) {
////                Log.v(TAG, "Ran out of metadata (head=" + mMetaHead + " tail=" + mMetaTail + ")");
////            }
//            return false;
//        }
//
//        // Make sure we have enough free space in the data buffer.
//        int headStart = getHeadStart();
//        int freeSpace = getFreeSpace(headStart);
//        if (size > freeSpace) {
////            if (HorizonApp.VERBOSE) {
////                int tailStart = mPacketStart[mMetaTail];
////                Log.v(TAG, "Ran out of data (tailStart=" + tailStart + " headStart=" + headStart +
////                        " req=" + size + " free=" + freeSpace + ")");
////            }
//            return false;
//        }
//
//        // Check if the packet can't fit until the end of its data buffer. If true, we'll write to
//        // the beginning of the next/same data buffer. We need to check again for free space.
//        int bufferStart =  (headStart / mBufferSize) * mBufferSize;     // 0 for single buffer
//        int bufferEnd = bufferStart + mBufferSize -1;
//        if (headStart + size -1 > bufferEnd) {
//            headStart = (bufferStart + mBufferSize) % mTotalBufferSize; // 0 for single buffer
//            freeSpace = getFreeSpace(headStart);
//            if (size > freeSpace) {
////                if (HorizonApp.VERBOSE) {
////                    int tailStart = mPacketStart[mMetaTail];
////                    Log.v(TAG, "Ran out of data (tailStart=" + String.format("%,d", tailStart) +
////                            " headStart=" + String.format("%,d", headStart) +
////                            " req=" + size + " free=" + freeSpace + ")");
////                }
//                return false;
//            }
//        }
//
////        if (HorizonApp.VERBOSE) {
////            int tailStart = mPacketStart[mMetaTail];
////            Log.v(TAG, "OK (tailStart=" + String.format("%,d",tailStart) +
////                    " headStart=" + String.format("%,d",headStart) +
////                    " req=" + size + " free=" + freeSpace + ")");
////        }
//
//        return true;
//    }
//
//    /**
//     * Moves the provided packet's position in the data buffer so that it is placed after the previous
//     * packet position.
//     */
//    private void move(int index) {
//        int previousIndex = getPreviousIndex(index);
//        if (previousIndex == -1) {
//            throw new RuntimeException("Can't move tail packet.");
//        }
//        int headStart = (mPacketStart[previousIndex] + mPacketLength[previousIndex]) % mTotalBufferSize;
//        int size = mPacketLength[index];
//
//        // Check if we have to write to the beginning of the next/same data buffer.
//        int bufferStart =  (headStart / mBufferSize) * mBufferSize;
//        int bufferEnd = bufferStart + mBufferSize -1;
//        if (headStart + size -1 > bufferEnd) {
//            headStart = (bufferStart + mBufferSize) % mTotalBufferSize;
//        }
//
//        int packetStart = headStart % mBufferSize;
//        int bufferId = headStart / mBufferSize;
//
//        MediaCodec.BufferInfo sourceInfo = new MediaCodec.BufferInfo();
//        ByteBuffer sourceBuffer = getChunk(index, sourceInfo);
//        mDataBuffer[bufferId].limit(packetStart + size);
//        mDataBuffer[bufferId].position(packetStart);
//        mDataBuffer[bufferId].put(sourceBuffer);
//
//        mPacketStart[index] = headStart;
//    }
//
//    /**
//     * Returns a reference to a "direct" ByteBuffer with the data, and fills in the
//     * BufferInfo.
//     * <p/>
//     * The caller must not modify the contents of the returned ByteBuffer.  Altering
//     * the position and limit is allowed.
//     */
//    public ByteBuffer getChunk(int index, MediaCodec.BufferInfo info) {
//        if (isEmpty()) {
//            throw new RuntimeException("Can't return chunk of empty buffer");
//        }
//
//        int packetStart = mPacketStart[index] % mBufferSize;
//        int bufferId = mPacketStart[index] / mBufferSize;
//
//        info.flags = mPacketFlags[index];
//        info.presentationTimeUs = mPacketPtsUs[index];
//        info.offset = packetStart;
//        info.size = mPacketLength[index];
//
//        ByteBuffer byteBuffer = mDataBuffer[bufferId].duplicate();
//        byteBuffer.order(mOrder);
//        byteBuffer.limit(info.offset + info.size);
//        byteBuffer.position(info.offset);
//
//        return byteBuffer;
//    }
//
//    public ByteBuffer getTailChunk(MediaCodec.BufferInfo info) {
//        int index = getFirstIndex();
//        return getChunk(index, info);
//    }
//    public ByteBuffer getNewestChunk(MediaCodec.BufferInfo info) {
//        int index = getAfterHeadIndex(1);
//        return getChunk(index, info);
//    }
//    public boolean isNewestIndex(int n) {
//        int index = (mMetaHead + mMetaLength - 1) % mMetaLength;
//        if (n == index) {
//            return true;
//        }
//        return false;
//    }
//    /**
//     * Removes the tail packet.
//     */
//    public void removeTail() {
////        if (HorizonApp.VERBOSE) {
////            Log.d(TAG, "remove tail:" + mMetaTail + " pts=" + mPacketPtsUs[mMetaTail]);
////        }
//        if (isEmpty()) {
//            throw new RuntimeException("Can't removeTail() in empty buffer");
//        }
//        mMetaTail = (mMetaTail + 1) % mMetaLength;
//
////        if (HorizonApp.VERBOSE) {
////            printStatus();
////        }
//    }
//
//}











//
////////WORK
///*
// * Copyright 2014 Google Inc. All rights reserved.
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *      http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//package com.dtnguy.presnap4.encoder;
//
//
//import android.media.MediaCodec;
//import android.media.MediaFormat;
//import android.util.Log;
//
//import java.nio.ByteBuffer;
//
///**
// * Holds encoded video data in a circular buffer.
// * <p>
// * This is actually a pair of circular buffers, one for the raw data and one for the meta-data
// * (flags and PTS).
// * <p>
// * Not thread-safe.
// */
//public class CircularEncoderBuffer {
//    private static final String TAG = "CircularEncoderBuffer";
//    private static final boolean EXTRA_DEBUG = true;
//    private static final boolean VERBOSE = false;
//    private boolean isItVideo;
//
//
//
//    // Raw data (e.g. AVC NAL units) held here.
//    //
//    // The MediaMuxer writeSampleData() function takes a ByteBuffer.  If it's a "direct"
//    // ByteBuffer it'll access the data directly, if it's a regular ByteBuffer it'll use
//    // JNI functions to access the backing byte[] (which, in the current VM, is done without
//    // copying the data).
//    //
//    // It's much more convenient to work with a byte[], so we just wrap it with a ByteBuffer
//    // as needed.  This is a bit awkward when we hit the edge of the buffer, but for that
//    // we can just do an allocation and data copy (we know it happens at most once per file
//    // save operation).
//    private ByteBuffer mDataBufferWrapper;
//    private ByteBuffer audiomDataBufferWrapper;
//    private byte[] mDataBuffer;
//    private byte[] audiomDataBuffer;
//
//    // Meta-data held here.  We're using a collection of arrays, rather than an array of
//    // objects with multiple fields, to minimize allocations and heap footprint.
//    private int[] mPacketFlags;
//    private long[] mPacketPtsUsec;
//    private int[] mPacketStart;
//    private int[] mPacketLength;
//
//    // Data is added at head and removed from tail.  Head points to an empty node, so if
//    // head==tail the list is empty.
//    private int mMetaHead;
//    private int mMetaTail;
//
//    private int bitRate;
//    private int dataBufferSize;
//    private double mTimePerPacketMs;
//    /**
//     * Allocates the circular buffers we use for encoded data and meta-data.
//     */
//    public CircularEncoderBuffer(MediaFormat mediaFormat, int desiredSpanSec) {
//        // For the encoded data, we assume the encoded bit rate is close to what we request.
//        //
//        // There would be a minor performance advantage to using a power of two here, because
//        // not all ARM CPUs support integer modulus.
//        bitRate = mediaFormat.getInteger(MediaFormat.KEY_BIT_RATE);
//        //buftype = "video";
//        dataBufferSize = bitRate * desiredSpanSec / 8;
//        mDataBuffer = new byte[dataBufferSize];
//        mDataBufferWrapper = ByteBuffer.wrap(mDataBuffer);
//
//        int mSpanMs = (int) (((long) 1000 * 8 * dataBufferSize)/(bitRate));
//
//        // We want to calculate how many packets fit in our mBufferSize
//        String mimeType = mediaFormat.getString(MediaFormat.KEY_MIME);
//        boolean isVideo = mimeType.equals(MediaFormat.MIMETYPE_VIDEO_AVC);
//        boolean isAudio = mimeType.equals(MediaFormat.MIMETYPE_AUDIO_AAC);
//        double packetSize;
//        double  packetsPerSecond;
//        if (isVideo) {
//            packetsPerSecond = mediaFormat.getInteger(MediaFormat.KEY_FRAME_RATE);
//            isItVideo = true;
//        }
//        else if (isAudio) {
//            double sampleRate = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
//            packetsPerSecond = sampleRate/1024;
//            isItVideo = false;
//        }
//        else {
//            throw new RuntimeException("Media format provided is neither AVC nor AAC");
//        }
//        mTimePerPacketMs =  1000./packetsPerSecond;
//        packetSize = (bitRate/packetsPerSecond)/8;
//        int estimatedPacketCount = (int) (dataBufferSize/packetSize + 1);
//        // Meta-data is smaller than encoded data for non-trivial frames, so we over-allocate
//        // a bit.  This should ensure that we drop packets because we ran out of (expensive)
//        // data storage rather than (inexpensive) metadata storage.
//        //int metaBufferCount = frameRate * desiredSpanSec * 2;
//        int metaBufferCount = estimatedPacketCount * 2;
//        mPacketFlags = new int[metaBufferCount];
//        mPacketPtsUsec = new long[metaBufferCount];
//        mPacketStart = new int[metaBufferCount];
//        mPacketLength = new int[metaBufferCount];
//
//        //double packetsPerSecond = frameRate;
//        //double mTimePerPacketMs = 1000./packetsPerSecond;
//        //Log.d(TAG, "mTimePerPacketMs: " + mTimePerPacketMs);
//
//        Log.d(TAG, "BitRate=" + bitRate +
//                " span=" + String.format("%,d", mSpanMs) + "msec" +
//                " buffer size=" + String.format("%,d", dataBufferSize / 1000) + "kB" +
//                " packet count=" + metaBufferCount);
////        if (VERBOSE) {
////            Log.d(TAG, "CBE: bitRate=" + bitRate + " frameRate=" + frameRate +
////                    " desiredSpan=" + desiredSpanSec + ": dataBufferSize=" + dataBufferSize +
////                    " metaBufferCount=" + metaBufferCount);
////        }
//    }
//
//    public boolean isItVideo()
//    {
//        return isItVideo;
//    }
//    public long getPTS(int n)
//    {
//        return mPacketPtsUsec[n];
//    }
//
//    /**
//     * Computes the amount of time spanned by the buffered data, based on the presentation
//     * time stamps.
//     */
//    public long computeTimeSpanUsec() {
//        final int metaLen = mPacketStart.length;
//
//        if (mMetaHead == mMetaTail) {
//            // empty list
//            return 0;
//        }
//
//        // head points to the next available node, so grab the previous one
//        int beforeHead = (mMetaHead + metaLen - 1) % metaLen;
//        return mPacketPtsUsec[beforeHead] - mPacketPtsUsec[mMetaTail];
//    }
//
//    /**
//     * Adds a new encoded data packet to the buffer.
//     *
//     * @param buf The data.  Set position() to the start offset and limit() to position+size.
//     *     The position and limit may be altered by this method.
//     **//* @param size Number of bytes in the packet.
//     * @param flags MediaCodec.BufferInfo flags.
//     * @param ptsUsec Presentation time stamp, in microseconds.
//     */
//    public void add(ByteBuffer buf, int flags, long ptsUsec) {
//        int size = buf.limit() - buf.position();
//        if (VERBOSE) {
//            Log.d(TAG, "add size=" + size + " flags=0x" + Integer.toHexString(flags) +
//                    " pts=" + ptsUsec);
//        }
//        //Log.d(TAG," adding " + buftype + " " + ptsUsec );
//        while (!canAdd(size)) {
//            removeTail();
//        }
//        final int dataLen = mDataBuffer.length;
//        final int metaLen = mPacketStart.length;
//        int packetStart = getHeadStart();
//        mPacketFlags[mMetaHead] = flags;
//        mPacketPtsUsec[mMetaHead] = ptsUsec;
//        mPacketStart[mMetaHead] = packetStart;
//        mPacketLength[mMetaHead] = size;
//
//        // Copy the data in.  Take care if it gets split in half.
//        if (packetStart + size < dataLen) {
//            // one chunk
//            buf.get(mDataBuffer, packetStart, size);
//        } else {
//            // two chunks
//            int firstSize = dataLen - packetStart;
//            if (VERBOSE) { Log.v(TAG, "split, firstsize=" + firstSize + " size=" + size); }
//            buf.get(mDataBuffer, packetStart, firstSize);
//            buf.get(mDataBuffer, 0, size - firstSize);
//        }
//
//        mMetaHead = (mMetaHead + 1) % metaLen;
//
//        if (EXTRA_DEBUG) {
//            // The head packet is the next-available spot.
//            mPacketFlags[mMetaHead] = 0x77aaccff;
//            mPacketPtsUsec[mMetaHead] = -1000000000L;
//            mPacketStart[mMetaHead] = -100000;
//            mPacketLength[mMetaHead] = Integer.MAX_VALUE;
//        }
//    }
//
//    /**
//     * Returns the index of the oldest sync frame.  Valid until the next add().
//     * <p>
//     * When sending output to a MediaMuxer, start here.
//     */
//    public int getFirstIndex() {
//        final int metaLen = mPacketStart.length;
//
//        int index = mMetaTail;
//        while (index != mMetaHead) {
//            if ((mPacketFlags[index] & MediaCodec.BUFFER_FLAG_SYNC_FRAME) != 0) {
//                break;
//            }
//            index = (index + 1) % metaLen;
//        }
//
//        if (index == mMetaHead) {
//            Log.w(TAG, "HEY: could not find sync frame in buffer");
//            index = -1;
//        }
//        return index;
//    }
//    //    public ByteBuffer getTailChunk(MediaCodec.BufferInfo info) {
////        int index = getFirstIndex();
////        return getChunk(index, info);
////    }
////    public int getNSecondsIndex(int n) {
////        final int metaLen = mPacketStart.length;
////        //TODO change hardcoded 15 to variable frame rate
////        //int index = mMetaTail;
////        int index = (mMetaHead + metaLen - (n*15 + 1)) % metaLen;
////        while (index != mMetaHead) {
////            if ((mPacketFlags[index] & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0) {
////                return index;
////            }
////            index = (index + 1) % metaLen;
////        }
////        return index;
////    }
//
//    public long getPTSatNSec(int n) {
//        final int metaLen = mPacketStart.length;
//        int index = (mMetaHead + metaLen - 1) % metaLen;
//        long s =  mPacketPtsUsec[index] - (1000000L * n);
//        //Log.d(TAG, "getPTSatNSec " + s);
//        return  mPacketPtsUsec[index] - (1000000L * n);
//
//    }
//
//    public int getIndexAtPTSVideo(long s, int n) {
//        final int metaLen = mPacketStart.length;
//        int index = (mMetaHead + metaLen - 1) % metaLen;
//        do
//        {
//            index = (index + metaLen - 1) % metaLen;
//            //if (index == 0) { index = (index + metaLen - 1) % metaLen; }
//            //Log.d(TAG, "index" + index + "flags " + mPacketFlags[index]);
//            if (index == 0) {
//                if (mPacketPtsUsec[(index + metaLen - 1) % metaLen] == 0) {
//                    return 0;
//                }
//            }
//
//        } while (mPacketPtsUsec[index] >= s || (mPacketFlags[index] & MediaCodec.BUFFER_FLAG_KEY_FRAME) == 0);
//        do {
//            index = (index + metaLen + 1) % metaLen;
//            if (index == 0) {
//                if (mPacketPtsUsec[(index + metaLen - 1) % metaLen] == 0) {
//                    return 0;
//                }
//            }
//        } while ((mPacketFlags[index] & MediaCodec.BUFFER_FLAG_KEY_FRAME) == 0);
//
//        return index;
//    }
//
//    public int getIndexAtPTSAudio(long s, int n) {
//
//        final int metaLen = mPacketStart.length;
//        int index = (mMetaHead + metaLen - 1) % metaLen;
//        long test = (mPacketPtsUsec[index]) - (s + (n*1000000L));
//        //Log.d(TAG, "Audio " + mPacketPtsUsec[index] + "Diff " + test);
//        //final boolean isFilled = isBufferFilled();
////        if ((mPacketPtsUsec[index]) - (s + (n*1000000L)) <= 0)
////        {
////            Log.d(TAG, "AudioNOTFILLED");
////            return 0;
////        }
//        while (mPacketPtsUsec[index] >= s)
//        {
//            index = (index + metaLen - 1) % metaLen;
//            if (index == 0) {
//                if (mPacketPtsUsec[(index + metaLen - 1) % metaLen] == 0) {
//                    return 0;
//                }
//            }
//        }
//        return index;
//    }
//    private boolean isBufferFilled() {
//        final int dataLen = mDataBuffer.length;
//        if ((mPacketStart[mMetaTail] + dataLen - getHeadStart()) % dataLen >= 0) {
//            return true;
//        } else {
//            return false;
//        }
//    }
//    public int getAfterHeadIndex(int n) {
//        final int metaLen = mPacketStart.length;
//        return (mMetaHead + metaLen - n) % metaLen;
//    }
////    public int getIndexAtPTS(long pts) {
////
////        final int metaLen = mPacketStart.length;
////        int index = (mMetaHead + metaLen - 1) % metaLen;
////        //final long s = mPacketPtsUsec[index] - (1000000L * n);
////        //Log.d(TAG,"ZERO: " + mPacketPtsUs[mMetaLength - 1]);
//////        if (mPacketPtsUsec[mPacketPtsUsec.length - 1] <= 0)
//////        {
//////            return 1;
//////        }
////        while (mPacketPtsUsec[index] >= pts)
////        {
////            index = (index + metaLen - 1) % metaLen;
////            if (index == mMetaHead) { return 1; }
////        }
////        return index;
////    }
//
//
//
//    public double getmTimePerPacketMs() {
//        return mTimePerPacketMs;
//    }
//    public ByteBuffer getSingleFrame(int index, MediaCodec.BufferInfo info) {
//        final int dataLen = mDataBuffer.length;
//        int packetStart = mPacketStart[index];
//        int length = mPacketLength[index];
//
//        info.flags = mPacketFlags[index];
//        info.offset = packetStart;
//        info.presentationTimeUs = mPacketPtsUsec[index];
//        info.size = length;
//
//        ByteBuffer tempBuf = ByteBuffer.allocateDirect(1);
//        tempBuf.put(mDataBuffer,mPacketStart[index], mPacketLength[index]);
//        info.offset = 0;
//        return tempBuf;
//    }
//    public ByteBuffer getNSecondsChunk(int index, MediaCodec.BufferInfo info) {
//        final int dataLen = mDataBuffer.length;
//        int packetStart = mPacketStart[index];
//        int length = mPacketLength[index];
//
//        info.flags = mPacketFlags[index];
//        info.offset = packetStart;
//        info.presentationTimeUs = mPacketPtsUsec[index];
//        info.size = length;
//        ByteBuffer tempBuf = ByteBuffer.allocateDirect(length);
//        int firstSize = dataLen - packetStart;
//        tempBuf.put(mDataBuffer, mPacketStart[index], firstSize);
//        info.offset = 0;
//        return tempBuf;
//    }
//    /**
//     * Returns the index of the next packet, or -1 if we've reached the end.
//     */
//    public int getNextIndex(int index) {
//        final int metaLen = mPacketStart.length;
//        int next = (index + 1) % metaLen;
//        //Log.d(TAG,"next: " + next);
////        if (video) {
////            if (next == mMetaHead) {
////                next = -1;
////            }
////        } else {
////            next = (next + 1) % metaLen;
////        }
//        if (next == mMetaHead) {
//            next = -1;
//            //next = (next + metaLen + 1) % metaLen;
//        }
//        return next;
//    }
//    public int getNextIndexVideo(int index) {
//        final int metaLen = mPacketStart.length;
//        int next = (index + 1) % metaLen;
//        if (next == mMetaHead) {
//            return -1;
//            //next = (next + metaLen + 1) % metaLen;
//        }
//        if (next == 0) {
//            if (!(mPacketPtsUsec[next] == 0)) {
//                return 0;
//            } else {
//                return -1;
//            }
//        }
//        return next;
//    }
//    public int getNextIndexVideoo(int index) {
//        final int metaLen = mPacketStart.length;
//        int next = (index + 1) % metaLen;
//        if (next == (mMetaHead + metaLen - 3) % metaLen) {
//            return -1;
//            //next = (next + metaLen + 1) % metaLen;
//        }
//        if (next == 0) {
//            if (!(mPacketPtsUsec[next] == 0)) {
//                return 0;
//            } else {
//                return -1;
//            }
//        }
//        return next;
//    }
//    //
////    private boolean isZeroIndexFilled() {
////        if (isEmpty()) {
////            return false;
////        }
////        return mPacketPtsUsec[0] != 0;
////    }
//    public boolean isEmpty() {
//        return (mMetaHead == mMetaTail);
//    }
//    public int getNextIndexAudio(int index) {
//        final int metaLen = mPacketStart.length;
//        int next = (index + 1) % metaLen;
//        if (next == mMetaHead) {
//            next = (next + metaLen - 1) % metaLen;
//            //next = (next + metaLen + 1) % metaLen;
//        }
//        if (next == 0) {
//            next = 1;
//        }
//        return next;
//    }
//    /**
//     * Returns a reference to a "direct" ByteBuffer with the data, and fills in the
//     * BufferInfo.
//     * <p>
//     * The caller must not modify the contents of the returned ByteBuffer.  Altering
//     * the position and limit is allowed.
//     */
//    public ByteBuffer getChunk(int index, MediaCodec.BufferInfo info) {
//        final int dataLen = mDataBuffer.length;
//        int packetStart = mPacketStart[index];
//        int length = mPacketLength[index];
//
//        info.flags = mPacketFlags[index];
//        info.offset = packetStart;
//        info.presentationTimeUs = mPacketPtsUsec[index];
//        info.size = length;
//
//        if (packetStart + length <= dataLen) {
//            // one chunk; return full buffer to avoid copying data
//            return mDataBufferWrapper;
//        } else {
//            // two chunks
//            ByteBuffer tempBuf = ByteBuffer.allocateDirect(length);
//            int firstSize = dataLen - packetStart;
//            tempBuf.put(mDataBuffer, mPacketStart[index], firstSize);
//            tempBuf.put(mDataBuffer, 0, length - firstSize);
//            info.offset = 0;
//            return tempBuf;
//        }
//    }
////    public ByteBuffer getNSecondsChunk(int index, MediaCodec.BufferInfo info) {
////        final int dataLen = mDataBuffer.length;
////        int packetStart = mPacketStart[index];
////        int length = mPacketLength[index];
////
////        info.flags = mPacketFlags[index];
////        info.offset = packetStart;
////        info.presentationTimeUs = mPacketPtsUsec[index];
////        info.size = length;
////
////        if (packetStart + length <= dataLen) {
////            // one chunk; return full buffer to avoid copying data
////            return mDataBufferWrapper;
////        } else {
////            // two chunks
////            ByteBuffer tempBuf = ByteBuffer.allocateDirect(length);
////            int firstSize = dataLen - packetStart;
////            tempBuf.put(mDataBuffer, packetStart, firstSize);
////            tempBuf.put(mDataBuffer, 0, length - firstSize);
////            info.offset = 0;
////            return tempBuf;
////        }
////    }
//
//    public int getPacketNum() {
//        // We don't count the +1 meta slot reserved for the head.
//        final int metaLen = mPacketStart.length;
//
//        int usedMeta = (mMetaHead + metaLen - mMetaTail) % metaLen ;
//        return usedMeta;
//    }
//    /**
//     * Computes the data buffer offset for the next place to store data.
//     * <p>
//     * Equal to the start of the previous packet's data plus the previous packet's length.
//     */
//
//    private int getHeadStart() {
//        if (mMetaHead == mMetaTail) {
//            // list is empty
//            return 0;
//        }
//
//        final int dataLen = mDataBuffer.length;
//        final int metaLen = mPacketStart.length;
//
//        int beforeHead = (mMetaHead + metaLen - 1) % metaLen;
//        return (mPacketStart[beforeHead] + mPacketLength[beforeHead] + 1) % dataLen;
//    }
//
//    /**
//     * Determines whether this is enough space to fit "size" bytes in the data buffer, and
//     * one more packet in the meta-data buffer.
//     *
//     * @return True if there is enough space to add without removing anything.
//     */
//    private boolean canAdd(int size) {
//        final int dataLen = mDataBuffer.length;
//        final int metaLen = mPacketStart.length;
//
//        if (size > dataLen) {
//            throw new RuntimeException("Enormous packet: " + size + " vs. buffer " +
//                    dataLen);
//        }
//        if (mMetaHead == mMetaTail) {
//            // empty list
//            return true;
//        }
//
//        // Make sure we can advance head without stepping on the tail.
//        int nextHead = (mMetaHead + 1) % metaLen;
//        if (nextHead == mMetaTail) {
//            if (VERBOSE) {
//                Log.v(TAG, "ran out of metadata (head=" + mMetaHead + " tail=" + mMetaTail +")");
//            }
//            return false;
//        }
//
//        // Need the byte offset of the start of the "tail" packet, and the byte offset where
//        // "head" will store its data.
//        int headStart = getHeadStart();
//        int tailStart = mPacketStart[mMetaTail];
//        int freeSpace = (tailStart + dataLen - headStart) % dataLen;
//        if (size > freeSpace) {
//            if (VERBOSE) {
//                Log.v(TAG, "ran out of data (tailStart=" + tailStart + " headStart=" + headStart +
//                        " req=" + size + " free=" + freeSpace + ")");
//            }
//            return false;
//        }
//
//        if (VERBOSE) {
//            Log.v(TAG, "OK: size=" + size + " free=" + freeSpace + " metaFree=" +
//                    ((mMetaTail + metaLen - mMetaHead) % metaLen - 1));
//        }
//
//        return true;
//    }
//
//    /**
//     * Removes the tail packet.
//     */
//    private void removeTail() {
//        if (mMetaHead == mMetaTail) {
//            throw new RuntimeException("Can't removeTail() in empty buffer");
//        }
//        final int metaLen = mPacketStart.length;
//        mMetaTail = (mMetaTail + 1) % metaLen;
//    }
//}

