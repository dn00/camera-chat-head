package com.dtnguy.camerachathead.encoder;
/*
 * AudioVideoRecordingSample
 * Sample project to cature audio and video from internal mic/camera and save as MPEG4 file.
 *
 * Copyright (c) 2014-2015 saki t_saki@serenegiant.com
 *
 * File name: MediaEncoder.java
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 * All files in the folder are under this Apache License, Version 2.0.
*/

import android.graphics.Bitmap;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;

import com.dtnguy.camerachathead.threadpool.DefaultExecutorSupplier;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

import static android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM;

//import android.os.Process;
//import android.provider.MediaStore;
//import static android.media.MediaCodec.BUFFER_FLAG_KEY_FRAME;

public abstract class
MediaEncoder implements Runnable {
    private static final boolean DEBUG = true;	// TODO set false on release
    private static final String TAG = "MediaEncoder";

    protected static final int TIMEOUT_USEC = 0;	// 10[msec]
    protected static final int MSG_FRAME_AVAILABLE = 1;
    protected static final int MSG_STOP_RECORDING = 9;

    public interface MediaEncoderListener {
        void onPrepared(MediaEncoder encoder);
        void onStopped(MediaEncoder encoder);
        void onSaveStop();
        void onSaveFinish();
        void requestFrameListUpdate(Bitmap bm);
    }

    protected final Object mSync = new Object();
    /**
     * Flag that indicate this encoder is capturing now.
     */
    protected volatile boolean mIsCapturing;
    /**
     * Flag that indicate the frame data will be available soon.
     */
    private int mRequestDrain;
    /**
     * Flag to request stop capturing
     */
    protected volatile boolean mRequestStop;
    /**
     * Flag that indicate encoder received EOS(End Of Stream)
     */
    protected boolean mIsEOS;
    /**
     * Flag the indicate the muxer is running
     */
    protected boolean mMuxerStarted;
    /**
     * Track Number
     */
    protected int mTrackIndex;
    /**
     * MediaCodec instance for encoding
     */
    protected MediaCodec mMediaCodec;				// API >= 16(Android4.1.2)
    /**
     * Weak refarence of MediaMuxerWarapper instance
     */
    protected final WeakReference<MediaMuxerWrapper> mWeakMuxer;
    /**
     * BufferInfo instance for dequeuing
     */
    private MediaCodec.BufferInfo mBufferInfo;		// API >= 16(Android4.1.2)

    protected final MediaEncoderListener mListener;

    protected CircularEncoderBuffer mEncBuffer;

    //protected int State;

    protected final Object preCordState = new Object();

    protected volatile static boolean savingState;

//    protected WriterThread wThread;
//    protected Handler wHandler;
    //protected int lastBufferIndex;

    protected WriterThread aThread;
    protected Handler aHandler;
    //protected EncoderHandler thisHandler;
    private MediaFormat mEncodedFormat;

    protected boolean saveStarted;

//    protected Thread thread;
 //   protected Thread endThread;

    protected int currIndex;

    protected boolean isPaused;

    protected boolean doWeNeedFrame = false;


    public MediaEncoder(final MediaMuxerWrapper muxer, final MediaEncoderListener listener) {
        if (listener == null) throw new NullPointerException("MediaEncoderListener is null");
        if (muxer == null) throw new NullPointerException("MediaMuxerWrapper is null");
        mWeakMuxer = new WeakReference<MediaMuxerWrapper>(muxer);
        muxer.addEncoder(this);
        mListener = listener;

        // wait for starting thread
        //paused = false;
        synchronized (mSync) {
            // create BufferInfo here for effectiveness(to reduce GC)

            aThread = new WriterThread();
            aThread.start();
            aHandler = aThread.getHandler();

            mBufferInfo = new MediaCodec.BufferInfo();
//            wThread = new WriterThread();
//            wThread.start();
//            wHandler = wThread.getHandler();
            new Thread(this, getClass().getSimpleName()).start();
            try {
                mSync.wait();
            } catch (final InterruptedException e) {
            }
        }
    }

    public String getOutputPath() {
        final MediaMuxerWrapper muxer = mWeakMuxer.get();
        return muxer != null ? muxer.getOutputPath() : null;
    }

    /**
     * the method to indicate frame data is soon available or already available
     * @return return true if encoder is ready to encod.
     */
    public boolean frameAvailableSoon() {
//    	if (DEBUG) Log.v(TAG, "frameAvailableSoon");
        synchronized (mSync) {
//            if (!mIsCapturing || mRequestStop) {
//                return false;
//            }
            if (mRequestStop) {
                return false;
            }
            mRequestDrain++;
            mSync.notifyAll();
        }
        //drain();
        if (savingState) {
            if (!saveStarted) {
                saveStarted = true;
                startSave();
            }
        }
        return true;
    }

    public void requestFrameListUpdate(Bitmap bm) {
        mListener.requestFrameListUpdate(bm);
        doWeNeedFrame = false;

    }
    /**
     * encoding loop on private thread
     */
    @Override
    public void run() {
		Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_DISPLAY);
        synchronized (mSync) {
            mRequestStop = false;
            mRequestDrain = 0;
            mSync.notify();
        }
        final boolean isRunning = true;
        boolean localRequestStop;
        boolean localRequestDrain;
        while (isRunning) {
            synchronized (mSync) {
                localRequestStop = mRequestStop;
                localRequestDrain = (mRequestDrain > 0);
                if (localRequestDrain)
                    mRequestDrain--;
            }
            if (localRequestStop) {
                //drain();
                drain();
                release();
                break;
            }
            if (localRequestDrain) {
                //if (DEBUG) {Log.d(TAG, "DRAIN CALLED");}
                drain();
            } else {
                synchronized (mSync) {
                    try {
                        mSync.wait();
                    } catch (final InterruptedException e) {
                        break;
                    }
                }
            }
        } // end of while
        synchronized (mSync) {
            mRequestStop = true;
            mIsCapturing = false;
        }
    }

//    /**
//     * encoding loop on private thread
//     */
//    @Override
//    public void run() {
////		android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
//        synchronized (mSync) {
//            mRequestStop = false;
//            mRequestDrain = 0;
//            mSync.notify();
//        }
//        final boolean isRunning = true;
//        boolean localRequestStop;
//        boolean localRequestDrain;
//        while (isRunning) {
//            synchronized (mSync) {
//                localRequestStop = mRequestStop;
//                localRequestDrain = (mRequestDrain > 0);
//                if (localRequestDrain)
//                    mRequestDrain--;
//            }
//            if (localRequestStop) {
//                drain();
//                // request stop recording
//                signalEndOfInputStream();
//                // process output data again for EOS signale
//                drain();
//                // release all related objects
//                release();
//                break;
//            }
//            if (localRequestDrain) {
//                //if (DEBUG) {Log.d(TAG, "DRAIN CALLED");}
//                drain();
//            } else {
//                synchronized (mSync) {
//                    try {
//                        mSync.wait();
//                    } catch (final InterruptedException e) {
//                        break;
//                    }
//                }
//            }
//        } // end of while
//        if (DEBUG) Log.d(TAG, "Encoder thread exiting");
//        synchronized (mSync) {
//            mRequestStop = true;
//            mIsCapturing = false;
//        }
//    }

    /*
    * prepareing method for each sub class
    * this method should be implemented in sub class, so set this as abstract method
    * @throws IOException
    */
   /*package*/ abstract void prepare() throws IOException;

    /*package*/ void startRecording() {
        if (DEBUG) Log.v(TAG, "startRecording");
        synchronized (mSync) {
            mIsCapturing = true;
            mRequestStop = false;
            saveStarted = false;
            mSync.notifyAll();
        }
    }

    /**
     * the method to request stop encoding
     */
	/*package*/ void stopRecording() {
        if (DEBUG) Log.v(TAG, "stopRecording");
        synchronized (mSync) {
            if (!mIsCapturing || mRequestStop) {
                return;
            }
            mRequestStop = true;	// for rejecting newer frame
            mSync.notifyAll();
            // We can not know when the encoding and writing finish.
            // so we return immediately after request to avoid delay of caller thread
        }
    }

//********************************************************************************
//********************************************************************************
    /**
     * Release all releated objects
     */
    protected void release() {
        if (DEBUG) Log.d(TAG, "release:");
        try {
            mListener.onStopped(this);
        } catch (final Exception e) {
            Log.e(TAG, "failed onStopped", e);
        }
        mIsCapturing = false;
        mRequestStop = true;	// for rejecting newer frame

        if (mMediaCodec != null) {
            try {
                mMediaCodec.stop();
                mMediaCodec.release();
                mMediaCodec = null;
            } catch (final Exception e) {
                Log.e(TAG, "failed releasing MediaCodec", e);
            }
        }
//        if (mMuxerStarted) {
//            final MediaMuxerWrapper muxer = mWeakMuxer != null ? mWeakMuxer.get() : null;
//            if (muxer != null) {
//                try {
//                    muxer.stop();
//                } catch (final Exception e) {
//                    Log.e(TAG, "failed stopping muxer", e);
//                }
//            }
//        }
        mBufferInfo = null;
    }

    protected void signalEndOfInputStream() {
        if (DEBUG) Log.d(TAG, "sending EOS to encoder");
        // signalEndOfInputStream is only avairable for video encoding with surface
        // and equivalent sending a empty buffer with BUFFER_FLAG_END_OF_STREAM flag.
		//mMediaCodec.signalEndOfInputStream();	// API >= 18
        encode(null, 0, getPTSUs());
    }

    /**
     * Method to set byte array to the MediaCodec encoder
     * @param buffer
     * @param length　length of byte array, zero means EOS.
     * @param presentationTimeUs
     */
    protected void encode(final ByteBuffer buffer, final int length, final long presentationTimeUs) {
        if (!mIsCapturing) return;
        final ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
        while (mIsCapturing) {
            final int inputBufferIndex = mMediaCodec.dequeueInputBuffer(TIMEOUT_USEC);
            if (inputBufferIndex >= 0) {
                final ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                inputBuffer.clear();
                if (buffer != null) {
                    inputBuffer.put(buffer);
                }
//	            if (DEBUG) Log.v(TAG, "encode:queueInputBuffer");
                if (length <= 0) {
                    // send EOS
                    mIsEOS = true;
                    if (DEBUG) Log.i(TAG, "send BUFFER_FLAG_END_OF_STREAM");
                    mMediaCodec.queueInputBuffer(inputBufferIndex, 0, 0,
                            presentationTimeUs, BUFFER_FLAG_END_OF_STREAM);
                    break;
                } else {
                    mMediaCodec.queueInputBuffer(inputBufferIndex, 0, length,
                            presentationTimeUs, 0);
                }
                break;
            } else if (inputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // wait for MediaCodec encoder is ready to encode
                // nothing to do here because MediaCodec#dequeueInputBuffer(TIMEOUT_USEC)
                // will wait for maximum TIMEOUT_USEC(10msec) on each call
            }
        }
    }
    //protected void preCord(int n) {
//        State = n;
//    }
    /**
     * drain encoded data and write them to muxer
     */
    /**
     * drain encoded data and write them to muxer
     */
    protected void drain() {
        aHandler.post(new Runnable() {
            @Override
            public void run() {
                //android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);
                try {
                    if (mMediaCodec == null) return;
                    ByteBuffer[] encoderOutputBuffers = mMediaCodec.getOutputBuffers();
                    int encoderStatus, count = 0;
                    LOOP:
                    while (mIsCapturing) {
                        synchronized (preCordState) {
                            // get encoded data with maximum timeout duration of TIMEOUT_USEC(=10[msec])
                            encoderStatus = mMediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
                            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                                // wait 5 counts(=TIMEOUT_USEC x 5 = 50msec) until data/EOS come
//                    if (!mIsEOS) {
//                        if (++count > 5)
//                            break LOOP;        // out of while
//                    }
                                break LOOP;
                            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                                if (DEBUG) Log.v(TAG, "INFO_OUTPUT_BUFFERS_CHANGED");
                                // this shoud not come when encoding
                                encoderOutputBuffers = mMediaCodec.getOutputBuffers();
                            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                                if (DEBUG) Log.v(TAG, "INFO_OUTPUT_FORMAT_CHANGED");
                                // this status indicate the output format of codec is changed
                                // this should come only once before actual encoded data
                                // but this status never come on Android4.3 or less
                                // and in that case, you should treat when MediaCodec.BUFFER_FLAG_CODEC_CONFIG come.
//                                if (mMuxerStarted) {    // second time request is error
//                                    throw new RuntimeException("format changed twice");
//                                }
                                // get output format from codec and pass them to muxer
                                // getOutputFormat should be called after INFO_OUTPUT_FORMAT_CHANGED otherwise crash.
                                mEncodedFormat = mMediaCodec.getOutputFormat();
                            } else if (encoderStatus < 0) {
                                // unexpected status
                                if (DEBUG)
                                    Log.w(TAG, "drain:unexpected result from encoder#dequeueOutputBuffer: " + encoderStatus);
                            } else {
                                final ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                                if (encodedData == null) {
                                    // this never should come...may be a MediaCodec internal error
                                    throw new RuntimeException("encoderOutputBuffer " + encoderStatus + " was null");
                                }
                                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                                    // You shoud set output format to muxer here when you target Android4.3 or less
                                    // but MediaCodec#getOutputFormat can not call here(because INFO_OUTPUT_FORMAT_CHANGED don't come yet)
                                    // therefor we should expand and prepare output format from buffer data.
                                    // This sample is for API>=18(>=Android 4.3), just ignore this flag here
                                    if (DEBUG) Log.d(TAG, "drain:BUFFER_FLAG_CODEC_CONFIG");
                                    mBufferInfo.size = 0;
                                }

                                if (mBufferInfo.size != 0) {
                                    // encoded data is ready, clear waiting counter
                                    count = 0;

//                    if (!mMuxerStarted) {
//                        // muxer is not ready...this will prrograming failure.
//                        throw new RuntimeException("drain:muxer hasn't started");
//                    }
//

                                    encodedData.position(mBufferInfo.offset);
                                    encodedData.limit(mBufferInfo.offset + mBufferInfo.size);
                                    // write encoded data to muxer(need to adjust presentationTimeUs.
                                    mBufferInfo.presentationTimeUs = getPTSUs();
                                    //muxer.writeSampleData(mTrackIndex, encodedData, mBufferInfo);
                                    //if (DEBUG) {Log.d(TAG, "ADDING:" + mBufferInfo.presentationTimeUs);}
                                    mEncBuffer.add(encodedData, mBufferInfo);
                                    prevOutputPTSUs = mBufferInfo.presentationTimeUs;
                                    preCordState.notifyAll();

//                                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0) {
//                                    //Log.d(TAG, "SYNCFRAME : " + mBufferInfo.presentationTimeUs);
//                                    doWeNeedFrame = true;
//                                }

//                        if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
//                            Log.w(TAG, "reached end of stream unexpectedly");
//                            break;      // out of while
//                        }
                                    // return buffer to encoder
                                }
                                mMediaCodec.releaseOutputBuffer(encoderStatus, false);
//                            if (savingState) {
//                                if (!saveStarted) {
//                                    saveStarted = true;
//                                    saveVideo();
//                                }
//                            }
                                break;
                            }

                        }
                    }
                } catch (IllegalStateException e) {
                    //return;
                }
            }
        });
    }

    /*package*/ synchronized public void stopSave() {
        // TODO merge stopSave() and stopSavePaused(). We can check if is saving, if yes, run endSaveRunnable(). If not, do nothing
        isPaused = false;
        savingState = false;
        saveStarted = false;
    }
    /*package*/ synchronized public void stopSavePaused() {
        //TODO Need a better solution
//        try {
//            thread.join(50);
//                    } catch (Exception e) {
//            }
        isPaused = true;
        savingState = false;
        synchronized (mSync) {
            if (saveStarted) {
                //saveStarted = true;
                //new Thread(endSave()).start();
                DefaultExecutorSupplier.getInstance().forHighPriority()
                        .execute(endSaveRunnable());
            //endSave(currIndex, mEncBuffer.isItVideo());
                try {
                    //TODO TRY calliny notifyall in endSaveRunnable
                    mSync.wait(500);
                } catch (Exception e) {

                }
            }
            mIsCapturing = false; // just to make sure
            stopRecording();
            mSync.notifyAll();
            //thread = null;
        }
//        synchronized (mSync) {
//            mRequestStop = true;	// for rejecting newer frame
//            mSync.notifyAll();
//            endSave(currIndex, mEncBuffer.isItVideo());

//            if (mMediaCodec != null) {
//                mMediaCodec.stop();
//                mMediaCodec.release();
//                mMediaCodec = null;
//            }
//        }

    }
    /*package*/ synchronized public void stopSaveResume() {
        isPaused = false;
        //startRecording();
    }

    /*package*/ synchronized void initSaveVideo() {
        savingState = true;
    }
    /*package*/ synchronized void startSave() {
        //savingState = true;
        //saveStarted = true;
//        if (thread != null) {
//            thread.interrupt();
//            thread = null;
//        }
//        if (thread == null || thread.getState() == Thread.State.TERMINATED) {
//            thread = prepareSaveVideoThread();
//        }
//        if (thread.getState() == Thread.State.NEW) {
//            thread.start();
//        }

        //wHandler.sendMessage(wHandler.obtainMessage(WriterThread.WriterHandler.MSG_STOP_SAVING));
        DefaultExecutorSupplier.getInstance().forHighPriority()
                .execute(saveVideoRunnable());
    }

    public Runnable saveVideoRunnable() {
        return new Runnable() {
            @Override
            public void run() {
                //int temp = 0;
                final MediaMuxerWrapper muxer = mWeakMuxer.get();
                int index;
                final boolean isItVideo = mEncBuffer.isItVideo();
                final int secs = muxer.getSeconds();
                //final int secs = 5;
                //Log.d(TAG,"INT N " + secs);
                final long s = mEncBuffer.getPTSatNSec(secs);
                final long firstPTS;
                //savingState = true;
                //numEncDone = 0;
                MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

                synchronized (muxer) {
                    if (isItVideo) {
                        //android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_MORE_FAVORABLE);
                        index = mEncBuffer.getIndexAtPTSVideo(s);
                        try {
                            muxer.createMuxer();
                        } catch (IOException e) {
                        }

                    } else {
                        //android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);
                        index = mEncBuffer.getIndexAtPTSAudio(s);
                        while (!muxer.isCreated()) {
                            try {
                                muxer.wait(5); // block
                            } catch (InterruptedException e) {
                                // ignore
                            }
                        }
                    }
                    mTrackIndex = muxer.addTrack(mEncodedFormat);
                    if (!muxer.start()) {
                        // we should wait until muxer is ready
                        while (!muxer.isStarted())
                            try {
                                muxer.wait(5);
                            } catch (final InterruptedException e) {
                            }
                    }
                    mMuxerStarted = true;
                }

                if (DEBUG) {
                    Log.d(TAG, "INDEX " + index);
                    firstPTS = mEncBuffer.getPTS(index);
                }
                if (index < 0) {
                    //Log.w(TAG, "Unable to get first index");
                    //mCallback.fileSaveComplete(1);
                    //TODO add exception
                    return;
                }
                try {
                    synchronized (preCordState) {
                        //preCordState.notifyAll();
                        do {
                            ByteBuffer buf = mEncBuffer.getChunk(index, info);
                            if (DEBUG) {
                                Log.d(TAG, mTrackIndex + " writeSampleData: " + index + " flags=0x" + Integer.toHexString(info.flags) + "PTS: " + info.presentationTimeUs + "TIME: " + (info.presentationTimeUs - firstPTS));
                            }
                            muxer.writeSampleData(index, mTrackIndex, buf, info);

                            if (index % 23 == 0) {
                            } else {
                                try {
                                    if (!savingState) {
                                        break;
                                    }
                                    preCordState.wait(); // block
                                } catch (InterruptedException e) {
                                    // ignore
                                }
                            }
                            index = mEncBuffer.getNextIndexVideo(index);

                            if (index < 0) {
                                index = mEncBuffer.getAfterHeadIndex(0);
                                try {
                                    if (!savingState) {
                                        break;
                                    }
                                    if (DEBUG) {
                                        Log.d(TAG, "CALLED " + index);
                                    }
                                    preCordState.wait(); // block
                                } catch (InterruptedException e) {
                                    // ignore
                                }
                                if (!savingState) {
                                    break;
                                }
                            }
                        } while (savingState);
                        mListener.onSaveStop();
                        DefaultExecutorSupplier.getInstance().forHighPriority()
                                .execute(endSaveRunnable());
                    }
                } catch (Exception ioe) {
                    Log.w(TAG, "muxer failed", ioe);
                } finally {
                    // muxer.stop();
                    //return;
                }
            }
        };
    }

// public Thread prepareSaveVideoThread() {
//     return new Thread(new Runnable() {
//         @Override
//         public void run() {
//                 //int temp = 0;
//                final MediaMuxerWrapper muxer = mWeakMuxer.get();
//                 int index;
//                 final boolean isItVideo = mEncBuffer.isItVideo();
//                // final int secs = muxer.getSeconds();
//                 final int secs = 5;
//                 //Log.d(TAG,"INT N " + secs);
//                 final long s = mEncBuffer.getPTSatNSec(secs);
//                 final long firstPTS;
//                 //savingState = true;
//                 //numEncDone = 0;
//                 MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
//
//                 synchronized (muxer) {
//                     if (isItVideo) {
//                         //android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_MORE_FAVORABLE);
//                         index = mEncBuffer.getIndexAtPTSVideo(s);
//                         try {
//                             muxer.createMuxer();
//                         } catch (IOException e) {
//                         }
//
//                     } else {
//                         //android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);
//                         index = mEncBuffer.getIndexAtPTSAudio(s);
//                         while (!muxer.isCreated()) {
//                             try {
//                                 muxer.wait(5); // block
//                             } catch (InterruptedException e) {
//                                 // ignore
//                             }
//                         }
//                     }
//                     mTrackIndex = muxer.addTrack(mEncodedFormat);
//                     if (!muxer.start()) {
//                         // we should wait until muxer is ready
//                         while (!muxer.isStarted())
//                             try {
//                                 muxer.wait(1);
//                             } catch (final InterruptedException e) {
//                             }
//                     }
//                     mMuxerStarted = true;
//                 }
//
//                 if (DEBUG) {
//                     Log.d(TAG, "INDEX " + index);
//                     firstPTS = mEncBuffer.getPTS(index);
//                 }
//                 if (index < 0) {
//                     //Log.w(TAG, "Unable to get first index");
//                     //mCallback.fileSaveComplete(1);
//                     //TODO add exception
//                     return;
//                 }
//                 try {
//                     synchronized (preCordState) {
//                         //preCordState.notifyAll();
//                         do {
//                             ByteBuffer buf = mEncBuffer.getChunk(index, info);
//                             if (DEBUG) {
//                                 Log.d(TAG, mTrackIndex + " writeSampleData: " + index + " flags=0x" + Integer.toHexString(info.flags) + "PTS: " + info.presentationTimeUs + "TIME: " + (info.presentationTimeUs - firstPTS));
//                             }
//                             muxer.writeSampleData(index, mTrackIndex, buf, info);
//
//                             if (index % 23 == 0) {
//                             } else {
//                                 try {
//                                     if (!savingState) {
//                                         break;
//                                     }
//                                     preCordState.wait(); // block
//                                 } catch (InterruptedException e) {
//                                     // ignore
//                                 }
//                             }
//                             index = mEncBuffer.getNextIndexVideo(index);
//
//                             if (index < 0) {
//                                 index = mEncBuffer.getAfterHeadIndex(0);
//                                 try {
//                                     if (!savingState) {
//                                         break;
//                                     }
//                                     if (DEBUG) {
//                                         Log.d(TAG, "CALLED " + index);
//                                     }
//                                     preCordState.wait(); // block
//                                 } catch (InterruptedException e) {
//                                     // ignore
//                                 }
//                                 if (!savingState) {
//                                     break;
//                                 }
//                             }
//                         } while (savingState);
//                         mListener.onSaveStop();
//                         if (endThread != null) {
//                             endThread.interrupt();
//                             thread = null;
//                         }
//                         if (endThread == null || endThread.getState() == Thread.State.TERMINATED) {
//                             endThread = endSave();
//                         }
//                         if (endThread.getState() == Thread.State.NEW) {
//                             endThread.start();
//                         }
//                     }
//                 } catch (Exception ioe) {
//                     Log.w(TAG, "muxer failed", ioe);
//                 } finally {
//                     // muxer.stop();
//                     return;
//                 }
//             }
//     });
// }

    public Runnable endSaveRunnable() {
        return new Runnable() {
            @Override
            public void run() {
                MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                final MediaMuxerWrapper muxer = mWeakMuxer.get();
                int index = mEncBuffer.getNextIndexVideo(muxer.getLastIndex(mTrackIndex));
                do

                {
                    if (DEBUG) {
                        Log.d(TAG, mTrackIndex + " writeSampleDataEND: " + index + " flags=0x" + Integer.toHexString(info.flags) + "PTS: " + info.presentationTimeUs);
                    }
                    ByteBuffer buf = mEncBuffer.getChunk(index, info);
                    if (mEncBuffer.getNextIndexVideo(mEncBuffer.getNextIndexVideo(index)) < 0) {
                        //Log.d(TAG, "END");
                        info.flags = BUFFER_FLAG_END_OF_STREAM;
                    }
                    muxer.writeSampleData(index, mTrackIndex, buf, info);
                    index = mEncBuffer.getNextIndexVideo(index);
                }

                while (index >= 0);
                if (muxer.isCreated())
                {
                    if (muxer.stop()) {
                        //Log.d(TAG, mTrackIndex + " writeSampleDataEND: ");
                        mMuxerStarted = false;
                        mListener.onSaveFinish();
                        saveStarted = false;
                    }
                }
                //return;
            }
        };
    }
//
//    public Thread endSave() {
//        return new Thread() {
//            @Override
//            public void run() {
//                    MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
//                    final MediaMuxerWrapper muxer = mWeakMuxer.get();
//                    int index = mEncBuffer.getNextIndexVideo(muxer.getLastIndex(mTrackIndex));
//                    do
//
//                    {
//                        if (DEBUG) {
//                            Log.d(TAG, mTrackIndex + " writeSampleDataEND: " + index + " flags=0x" + Integer.toHexString(info.flags) + "PTS: " + info.presentationTimeUs);
//                        }
//                        ByteBuffer buf = mEncBuffer.getChunk(index, info);
//                        if (mEncBuffer.getNextIndexVideo(mEncBuffer.getNextIndexVideo(index)) < 0) {
//                            //Log.d(TAG, "END");
//                            info.flags = BUFFER_FLAG_END_OF_STREAM;
//                        }
//                        muxer.writeSampleData(index, mTrackIndex, buf, info);
//                        index = mEncBuffer.getNextIndexVideo(index);
//                    }
//
//                    while (index >= 0);
//                    if (muxer.isCreated())
//                    {
//                        if (muxer.stop()) {
//                            Log.d(TAG, mTrackIndex + " writeSampleDataEND: ");
//                            mMuxerStarted = false;
//                            mListener.onSaveFinish();
//                        }
//                        saveStarted = false;
//                    }
//                interrupt();
//                }
//        };
//
//    }

    ///WORKS!
    public void saveVideo() {
        //thread.start();

    }



    /**
     * previous presentationTimeUs for writing
     */
    private long prevOutputPTSUs = 0;
    /**
     * get next encoding presentationTimeUs
     * @return
     */
    protected long getPTSUs() {
        long result = System.nanoTime() / 1000L;
        // presentationTimeUs should be monotonic
        // otherwise muxer fail to write
        if (result < prevOutputPTSUs)
            result = (prevOutputPTSUs - result) + result;
        return result;
    }
//
//    private static class EncoderHandler extends Handler {
//        public static final int MSG_STOP_SAVING = 1;
//        //public static final int MSG_STOP_SAVING = 2;
//        // This shouldn't need to be a weak ref, since we'll go away when the Looper quits,
//        // but no real harm in it.
//        private WeakReference<MediaEncoder> mWeakEncoderThread;
//
//        /**
//         * Constructor.  Instantiate object from encoder thread.
//         */
//        public EncoderHandler(MediaEncoder et) {
//            mWeakEncoderThread = new WeakReference<MediaEncoder>(et);
//        }
//
//        @Override  // runs on encoder thread
//        public void handleMessage(Message msg) {
//            int what = msg.what;
//            if (DEBUG) {
//                Log.v(TAG, "mWeakMuxerThread: what=" + what);
//            }
//
//            MediaEncoder encoder = mWeakEncoderThread.get();
//            if (encoder == null) {
//                Log.w(TAG, "mWeakMuxerThread.handleMessage: weak ref is null");
//                return;
//            }
//
//            switch (what) {
//                case MSG_STOP_SAVING:
//                    encoder.setSaveFalse();
//                    break;
////                case MSG_STOP_SAVING:
////                    //encoder.setSavingFalse();
////                    break;
//                default:
//                    throw new RuntimeException("unknown message " + what);
//            }
//        }
//    }

    private static class WriterThread extends Thread {

        private final Object mLock = new Object();
        private volatile boolean mReady = false;
        private boolean saving;
        private WriterHandler wHandler;
        //private EncoderHandler enHandler;
        public WriterThread() {
            wHandler = new WriterHandler(this);    // must create on encoder thread
            //enHandler = h;
        }
        @Override
        public void run() {
            Looper.prepare();
            wHandler = new WriterHandler(this);
            Log.d(TAG, "Writer thread ready");
            synchronized (mLock) {
                mReady = true;
                mLock.notify();    // signal waitUntilReady()
            }

            Looper.loop();

            synchronized (mLock) {
                mReady = false;
                //mHandler = null;
            }
            Log.d(TAG, "looper quit");

        }

        /**
         * Returns the Handler used to send messages to the encoder thread.
         */
        public WriterHandler getHandler() {
//            synchronized (mLock) {
//                // Confirm ready state.
//                if (!mReady) {
//                    throw new RuntimeException("not ready");
//                }
//            }
            return wHandler;
        }
        public void setSavingTrue() {
            saving = true;

        }
        public void setSavingFalse() {
            saving = false;
            //enHandler.sendMessage(enHandler.obtainMessage(EncoderHandler.MSG_STOP_SAVING));
        }
        /**
         * Handler for EncoderThread.  Used for messages sent from the UI thread (or whatever
         * is driving the encoder) to the encoder thread.
         * <p>
         * The object is created on the encoder thread.
         */
        private static class WriterHandler extends Handler {
            public static final int MSG_START_SAVING = 1;
            public static final int MSG_STOP_SAVING = 2;

            // This shouldn't need to be a weak ref, since we'll go away when the Looper quits,
            // but no real harm in it.
            private WeakReference<WriterThread> mWeakWriterrThread;

            /**
             * Constructor.  Instantiate object from encoder thread.
             */
            public WriterHandler(WriterThread et) {

                mWeakWriterrThread = new WeakReference<WriterThread>(et);
            }

            @Override  // runs on encoder thread
            public void handleMessage(Message msg) {
                int what = msg.what;
                if (DEBUG) {
                    Log.v(TAG, "mWeakMuxerThread: what=" + what);
                }

                WriterThread writerThread = mWeakWriterrThread.get();
                if (writerThread == null) {
                    Log.w(TAG, "mWeakMuxerThread.handleMessage: weak ref is null");
                    return;
                }

                switch (what) {
                    case MSG_START_SAVING:
                        writerThread.setSavingTrue();
                        break;
                    case MSG_STOP_SAVING:
                        writerThread.setSavingFalse();
                        break;
                    default:
                        throw new RuntimeException("unknown message " + what);
                }
            }
        }
    }
}