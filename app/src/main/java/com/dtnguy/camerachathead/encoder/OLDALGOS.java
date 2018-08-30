package com.dtnguy.camerachathead.encoder;
//
//    ///WORKS!
//    public void saveVideo() {
//        Thread thread = new Thread() {
//            @Override
//            public void run() {
//                long firstPTS;
//                long prevPTS;
//                boolean isItVideo = mEncBuffer.isItVideo();
//                int secs = 5;
//                final long s = mEncBuffer.getPTSatNSec(secs);
//                int index;
//                if (isItVideo) {
//                    android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_MORE_FAVORABLE);
//                    index = mEncBuffer.getIndexAtPTSVideo(s, secs);
//                } else {
//                    android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);
//                    index = mEncBuffer.getIndexAtPTSAudio(s, secs);
//                }
//                firstPTS = mEncBuffer.getPTS(index);
//                if (DEBUG) {
//                    Log.d(TAG, "INDEX " + index);
//                }
//                if (index < 0) {
//                    //Log.w(TAG, "Unable to get first index");
//                    //mCallback.fileSaveComplete(1);
//                    //TODO add exception
//                    return;
//                }
//                savingState = true;
//                numEncDone = 0;
//                MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
//                final MediaMuxerWrapper muxer = mWeakMuxer.get();
//                try {
//                    synchronized (preCordState) {
//                        preCordState.notifyAll();
//                        mTrackIndex = muxer.addTrack(mEncodedFormat);
//                        mMuxerStarted = true;
//                        if (!muxer.start()) {
//                            // we should wait until muxer is ready
//                            synchronized (muxer) {
//                                while (!muxer.isStarted())
//                                    try {
//                                        muxer.wait(1);
//                                    } catch (final InterruptedException e) {
//                                    }
//                            }
//                        }
//                        muxer.resetVideoCountMS();
//                        do {
//
//                            if (isItVideo) {
//                                if (DEBUG) { Log.d(TAG, mTrackIndex + " writeSampleData: " + index + " flags=0x" + Integer.toHexString(info.flags) + "PTS: " + info.presentationTimeUs + "TIME: " + (info.presentationTimeUs - firstPTS));}
//
//                                ByteBuffer buf = mEncBuffer.getChunk(index, info);
//                                muxer.writeSampleData(mTrackIndex, buf, info);
//                                //muxer.addVideoCountMS(timePerPacketMS);
//                                try {
//                                    preCordState.wait(); // block
//                                } catch (InterruptedException e) {
//                                    // ignore
//                                }
//                                index = mEncBuffer.getNextIndex(index);
//                            } else {
//                                do {
//                                    ByteBuffer buf = mEncBuffer.getChunk(index, info);
//                                    //Log.d(TAG, "AUDIO " +index + "LAST" + info.presentationTimeUs);
//                                    muxer.writeSampleData(mTrackIndex, buf, info);
//                                    index = mEncBuffer.getNextIndexVideo(index);
//                                } while (index >= 0);
//                                try {
//                                    preCordState.wait(); // block
//                                } catch (InterruptedException e) {
//                                    // ignore
//                                }
//                                index = mEncBuffer.getNextIndex(index);
//                                if (index <= 0) {
//                                    index = mEncBuffer.getAfterHeadIndex(1);
//                                }
//                            }
//                        } while (savingState);
//                       // Log.d(TAG, mTrackIndex + "LAST" + mEncBuffer.getAfterHeadIndex(1));
//                        try {
//                            preCordState.wait(); // block
//                        } catch (InterruptedException e) {
//                            // ignore
//                        }
//                        do {
//                            if (DEBUG) { Log.d(TAG, mTrackIndex + " writeSampleDataEND: " + index + " flags=0x" + Integer.toHexString(info.flags) + "PTS: " + info.presentationTimeUs );}
//                            ByteBuffer buf = mEncBuffer.getChunk(index, info);
//                            muxer.writeSampleData(mTrackIndex, buf, info);
//                            index = mEncBuffer.getNextIndexVideo(index);
//                        } while (index >= 0);
//                    }
//                    saveStarted = false;
//                } catch (Exception ioe) {
//                    Log.w(TAG, "muxer failed", ioe);
//                } finally {
//                    if (muxer != null) {
//                        muxer.stop();
//
//                    }
//                }
//            }
//        };
//        thread.start();
//    }




//    }    public void saveVideo() {
//        Thread thread = new Thread() {
//            @Override
//            public void run() {
//                int tempIndex = 0;
//                boolean temp = false;
//                boolean firstFrame = true;
//                savingState = true;
//                numEncDone = 0;
//                int index = mEncBuffer.getNSecondsIndex(5);
//                if (DEBUG) {
//                    Log.d(TAG, "INDEX " + index);
//                }
//                if (index < 0) {
//                    //Log.w(TAG, "Unable to get first index");
//                    //mCallback.fileSaveComplete(1);
//                    //TODO add exception
//                    return;
//                }
//                MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
//                final MediaMuxerWrapper muxer = mWeakMuxer.get();
//                try {
//                    synchronized (preCordState) {
//                        preCordState.notifyAll();
//                        mTrackIndex = muxer.addTrack(mEncodedFormat);
//                        mMuxerStarted = true;
//                        if (!muxer.start()) {
//                            // we should wait until muxer is ready
//                            synchronized (muxer) {
//                                while (!muxer.isStarted())
//                                    try {
//                                        muxer.wait(1);
//                                    } catch (final InterruptedException e) {
//                                    }
//                            }
//                        }
//                        //// Precorded section loop (0 seconds to N seconds);
//                        do {
//                            if (DEBUG) {
//                                Log.d(TAG, mTrackIndex + " writeSampleData: " + index + " flags=0x" + Integer.toHexString(info.flags) + "PTS: " + info.presentationTimeUs);
//                            }
//                            //index = mEncBuffer.findPTSIndex(prevPTS);
//                            ByteBuffer buf = mEncBuffer.getChunk(index, info);
//
//                            if (info.presentationTimeUs > 0) {
//                                Log.d(TAG, mTrackIndex + " WRITE: " + index);
//                                muxer.writeSampleData(mTrackIndex, buf, info);
//                            }
//                            try {
//                                preCordState.wait(); // block
//                            } catch (InterruptedException e) {
//                                // ignore
//                            }
//                            index = mEncBuffer.getNextIndex(index);
//
//                            //tempIndex = index;
////                            index = mEncBuffer.getNextIndex(index);
////                            if (index < 0) {
////                                temp = true;
////                                try {
////                                    Log.d(TAG, mTrackIndex + " wait0: " + index);
////                                    preCordState.wait(); // block
////                                } catch (InterruptedException e) {
////                                    // ignore
////                                }
////                                index = mEncBuffer.getAfterHeadIndex(1);
////                            }
////                            if (!temp) {
////                                if (index % 2 == 0 || index == -1) {
////                                    try {
////                                        Log.d(TAG, mTrackIndex + " wait1: " + index);
////                                        preCordState.wait(); // block
////                                    } catch (InterruptedException e) {
////                                        // ignore
////                                    }
////                                }
////                            } else {
////                                try {
////                                    Log.d(TAG, mTrackIndex + " wait2: " + index);
////                                    preCordState.wait(); // block
////                                } catch (InterruptedException e) {
////                                    // ignore
////                                }
////                            }
//                        } while (savingState);
//                        ++numEncDone;
//                        synchronized (reCordState) {
//                            if (numEncDone == 1) {
//                                try {
//                                    Log.d(TAG, mTrackIndex + " waitRecord: ");
//                                    reCordState.wait(); // block
//                                } catch (InterruptedException e) {
//                                    // ignore
//                                }
//                            } else {
//                                reCordState.notifyAll();
//                            }
//                        }
//
//                        Log.d(TAG, "ENDPrecorded : " + index);
//                        do {
//                            if (DEBUG) {
//                                Log.d(TAG, mTrackIndex + " writeSampleDataEND: " + index + " flags=0x" + Integer.toHexString(info.flags) + "PTS: " + info.presentationTimeUs);
//                            }
//                            ByteBuffer buf = mEncBuffer.getChunk(index, info);
//                            muxer.writeSampleData(mTrackIndex, buf, info);
//                            index = mEncBuffer.getNextIndex(index);
//                        } while (index > 0);
//
//                        Log.d(TAG, "END : " + mTrackIndex);
//                    }
//                    ++numEncDone;
//                    synchronized (reCordState) {
//                        if (numEncDone == 3) {
//                            try {
//                                Log.d(TAG, mTrackIndex + " waitRecordEND: ");
//                                reCordState.wait(); // block
//                            } catch (InterruptedException e) {
//                                // ignore
//                            }
//                        } else {
//                            reCordState.notifyAll();
//                        }
//                    }
//
//                } catch (Exception ioe) {
//                    Log.w(TAG, "muxer failed", ioe);
//                } finally {
//                        if (muxer != null) {
//                            muxer.stop();
//
//                    }
//                }
//            }
//        };
//        thread.start();
//    }


//    public void saveVideo() {
//        Thread thread = new Thread() {
//            @Override
//            public void run() {
//                int tempIndex = 0;
//                boolean temp = false;
//                boolean firstFrame = true;
//                savingState = true;
//                int index = mEncBuffer.getNSecondsIndex(5);
//                long prevPTS = mEncBuffer.getPTS(index, 0);
//                if (DEBUG) {
//                    Log.d(TAG, "INDEX " + index);
//                }
//                if (index < 0) {
//                    //Log.w(TAG, "Unable to get first index");
//                    //mCallback.fileSaveComplete(1);
//                    //TODO add exception
//                    return;
//                }
//                MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
//                final MediaMuxerWrapper muxer = mWeakMuxer.get();
//                try {
//                    synchronized (preCordState) {
//                        preCordState.notifyAll();
//                        mTrackIndex = muxer.addTrack(mEncodedFormat);
//                        mMuxerStarted = true;
//                        if (!muxer.start()) {
//                            // we should wait until muxer is ready
//                            synchronized (muxer) {
//                                while (!muxer.isStarted())
//                                    try {
//                                        muxer.wait(1);
//                                    } catch (final InterruptedException e) {
//                                    }
//                            }
//                        }
//                        //// Precorded section loop (0 seconds to N seconds);
//
//                        do {
//                            if (DEBUG) {
//                                Log.d(TAG, mTrackIndex + " writeSampleData: " + index + " flags=0x" + Integer.toHexString(info.flags) + "PTS: " + info.presentationTimeUs);
//                            }
//                            //index = mEncBuffer.findPTSIndex(prevPTS);
//                            ByteBuffer buf = mEncBuffer.getChunk(index, info);
//                            muxer.writeSampleData(mTrackIndex, buf, info);
//                            //tempIndex = index;
//                            if (index < 0)
//                            {
//                                temp = true;
//                            }
//                            if (!temp) {
//                                Log.d(TAG, " Next " );
////                                if (index % 2 == 0) {
////                                    try {
////                                        preCordState.wait(); // block
////                                    } catch (InterruptedException e) {
////                                        // ignore
////                                    }
////                                }
//                                index = mEncBuffer.getNextIndex(index);
//
//                            } else {
//                                Log.d(TAG, " HEAD " );
//                                try {
//                                    preCordState.wait(); // block
//                                } catch (InterruptedException e) {
//                                    // ignore
//                                }
//                                index = mEncBuffer.getNextIndex(index);
//
//                            }
//                        } while (savingState);
//                        Log.d(TAG, "ENDPrecorded : " + index);
//                        try {
//                            preCordState.wait(); // block
//                        } catch (InterruptedException e) {
//                            // ignore
//                        }
//                        //index = tempIndex;
//                        do {
//                            ByteBuffer buf = mEncBuffer.getChunk(index, info);
//                            muxer.writeSampleData(mTrackIndex, buf, info);
//                            index = mEncBuffer.getNextIndex(index);
//                        } while (index > 0);
//                    }
//                    Log.d(TAG, "END : " + savingState);
//                } catch (Exception ioe) {
//                    Log.w(TAG, "muxer failed", ioe);
//                } finally {
//                    if (muxer != null) {
//                        muxer.stop();
//
//                    }
//                }
//            }
//        };
//        thread.start();
//    }

//    public void saveVideo() {
//        Thread thread = new Thread() {
//            @Override
//            public void run() {
//                int tempIndex = 0;
//                boolean temp = false;
//                //boolean firstFrame = true;
//                savingState = true;
//                int index = mEncBuffer.getNSecondsIndex(5);
//                //long prevPTS = mEncBuffer.getPTS(index, 0);
//                if (DEBUG) {
//                    Log.d(TAG, "INDEX " + index);
//                }
//                if (index < 0) {
//                    //Log.w(TAG, "Unable to get first index");
//                    //mCallback.fileSaveComplete(1);
//                    //TODO add exception
//                    return;
//                }
//                MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
//                final MediaMuxerWrapper muxer = mWeakMuxer.get();
//                try {
//                    synchronized (preCordState) {
//                        preCordState.notifyAll();
//                        mTrackIndex = muxer.addTrack(mEncodedFormat);
//                        mMuxerStarted = true;
//                        if (!muxer.start()) {
//                            // we should wait until muxer is ready
//                            synchronized (muxer) {
//                                while (!muxer.isStarted())
//                                    try {
//                                        muxer.wait(0);
//                                    } catch (final InterruptedException e) {
//                                    }
//                            }
//                        }
//                        //// Precorded section loop (0 seconds to N seconds);
//
//                        do {
//                            //index = mEncBuffer.findPTSIndex(prevPTS);
//                            do {
//                                if (DEBUG) {
//                                    Log.d(TAG, "writeSampleData: " + index + " flags=0x" + Integer.toHexString(info.flags) + "PTS: " + info.presentationTimeUs);
//                                }
//                                ByteBuffer buf = mEncBuffer.getChunk(index, info);
//                                muxer.writeSampleData(mTrackIndex, buf, info);
//                                //prevPTS = info.presentationTimeUs;
//                                //prevPTS = mEncBuffer.getPTS(index, prevPTS);
//                                tempIndex = index;
//                                index = mEncBuffer.getNextIndex(index);
//                                if (temp) {
//                                    try {
//                                        preCordState.wait(); // block
//                                    } catch (InterruptedException e) {
//                                        // ignore
//                                    }
//                                }
//                                if (!savingState) { break; }
//                            } while (index > 0);
//                            Log.d(TAG, mTrackIndex + " WAITING : " + index);
//                            index = tempIndex;
//                            if (!temp) {
//                                Log.d(TAG, "TEMP FALSE : " + index);
//                                temp = true;
//                                Log.d(TAG, "TEMP TRUE : " + index);
//                                try {
//                                    preCordState.wait(); // block
//                                } catch (InterruptedException e) {
//                                    // ignore
//                                }
//                                index = mEncBuffer.getNextIndex(index);
//                            }
//                        } while (savingState);
//
//                        Log.d(TAG, "ENDPrecorded : " + index);
//                    }
//                    Log.d(TAG, "END : " + savingState);
//                } catch (Exception ioe) {
//                    Log.w(TAG, "muxer failed", ioe);
//                } finally {
//                    if (muxer != null) {
//                        muxer.stop();
//
//                    }
//                }
//            }
//        };
//        thread.start();
//    }
//    public void saveAudio() {
//        Thread thread = new Thread() {
//            @Override
//            public void run() {
//                int tempIndex = 0;
//                boolean temp = false;
//                boolean firstFrame = true;
//                savingState = true;
//                int index = mEncBuffer.getNSecondsIndex(5);
//                long prevPTS = mEncBuffer.getPTS(index, 0);
//                if (DEBUG) {
//                    Log.d(TAG, "INDEX " + index);
//                }
//                if (index < 0) {
//                    //Log.w(TAG, "Unable to get first index");
//                    //mCallback.fileSaveComplete(1);
//                    //TODO add exception
//                    return;
//                }
//                MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
//                final MediaMuxerWrapper muxer = mWeakMuxer.get();
//                try {
//                    synchronized (preCordState) {
//                        mTrackIndex = muxer.addTrack(mEncodedFormat);
//                        mMuxerStarted = true;
//                        if (!muxer.start()) {
//                            // we should wait until muxer is ready
//                            synchronized (muxer) {
//                                while (!muxer.isStarted())
//                                    try {
//                                        muxer.wait(100);
//                                    } catch (final InterruptedException e) {
//                                    }
//                            }
//                        }
//                        //// Precorded section loop (0 seconds to N seconds);
//
//                        do {
//                            //index = mEncBuffer.findPTSIndex(prevPTS);
//                            do {
//                                if (DEBUG) {
//                                    Log.d(TAG, "writeSampleData: " + index + " flags=0x" + Integer.toHexString(info.flags) + "PTS: " + info.presentationTimeUs);
//                                }
//                                ByteBuffer buf = mEncBuffer.getChunk(index, info);
//                                muxer.writeSampleData(mTrackIndex, buf, info);
//                                //prevPTS = info.presentationTimeUs;
//                                //prevPTS = mEncBuffer.getPTS(index, prevPTS);
//                                tempIndex = index;
//                                index = mEncBuffer.getNextIndex(index);
//                                if (temp) {
//                                    try {
//                                        preCordState.wait(); // block
//                                    } catch (InterruptedException e) {
//                                        // ignore
//                                    }
//                                }
//                                if (!savingState) { break; }
//                            } while (index > 0);
//                            Log.d(TAG, mTrackIndex + " WAITING : " + index);
//
//                            index = tempIndex;
//                            if (!temp) {
//                                Log.d(TAG, "TEMP FALSE : " + index);
//                                temp = true;
//                                Log.d(TAG, "TEMP TRUE : " + index);
//                                try {
//                                    preCordState.wait(); // block
//                                } catch (InterruptedException e) {
//                                    // ignore
//                                }
//                                index = mEncBuffer.getNextIndex(index);
//                            }
//                            try {
//                                preCordState.wait(); // block
//                            } catch (InterruptedException e) {
//                                // ignore
//                            }
//                        } while (savingState);
//
//                        Log.d(TAG, "ENDPrecorded : " + index);
//                    }
//                    Log.d(TAG, "END : " + savingState);
//                } catch (Exception ioe) {
//                    Log.w(TAG, "muxer failed", ioe);
//                } finally {
//                    if (muxer != null) {
//                        muxer.stop();
//
//                    }
//                }
//            }
//        };
//        thread.start();
//    }
//
//    public void saveVideo() {
//        Thread thread = new Thread() {
//            @Override
//            public void run() {
//                int tempIndex = 0;
//                boolean temp = false;
//                boolean firstFrame = true;
//                savingState = true;
//                int index = mEncBuffer.getNSecondsIndex(5);
//                long prevPTS = mEncBuffer.getPTS(index, 0);
//                if (DEBUG) {
//                    Log.d(TAG, "INDEX " + index);
//                }
//                if (index < 0) {
//                    //Log.w(TAG, "Unable to get first index");
//                    //mCallback.fileSaveComplete(1);
//                    //TODO add exception
//                    return;
//                }
//                MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
//                final MediaMuxerWrapper muxer = mWeakMuxer.get();
//                try {
//                    synchronized (preCordState) {
//                        mTrackIndex = muxer.addTrack(mEncodedFormat);
//                        mMuxerStarted = true;
//                        if (!muxer.start()) {
//                            // we should wait until muxer is ready
//                            synchronized (muxer) {
//                                while (!muxer.isStarted())
//                                    try {
//                                        muxer.wait(100);
//                                    } catch (final InterruptedException e) {
//                                    }
//                            }
//                        }
//                        //// Precorded section loop (0 seconds to N seconds);
//
//                        do {
//                            index = mEncBuffer.findPTSIndex(prevPTS);
//                            do {
//                                if (!savingState) { break; }
//                                if (DEBUG) {
//                                    Log.d(TAG, "writeSampleData: " + index + " flags=0x" + Integer.toHexString(info.flags) + "PTS: " + info.presentationTimeUs);
//                                }
//                                ByteBuffer buf = mEncBuffer.getChunk(index, info);
//                                muxer.writeSampleData(mTrackIndex, buf, info);
//                                //prevPTS = info.presentationTimeUs;
//                                index = mEncBuffer.getNextIndex(index);
//                                prevPTS = mEncBuffer.getPTS(index, prevPTS);
//                                if (temp) {
//                                    try {
//                                        preCordState.wait(); // block
//                                    } catch (InterruptedException e) {
//                                        // ignore
//                                    }
//                                }
//                                if (!savingState) { break; }
//                            } while (index > 0);
//                            temp = true;
//                            try {
//                                preCordState.wait(); // block
//                            } catch (InterruptedException e) {
//                                // ignore
//                            }
//                        } while (savingState);
//
//                        Log.d(TAG, "ENDPrecorded : " + index);
//                    }
//                    Log.d(TAG, "END : " + savingState);
//                } catch (Exception ioe) {
//                    Log.w(TAG, "muxer failed", ioe);
//                } finally {
//                    if (muxer != null) {
//                        muxer.stop();
//
//                    }
//                }
//            }
//        };
//        thread.start();
//    }
//


//    public void stopSave() {
//        savingState = false;
//        if (DEBUG) {Log.d(TAG, "STOPSAVEMESSAGE" );}
//        //wHandler.sendMessage(wHandler.obtainMessage(WriterThread.WriterHandler.MSG_STOP_SAVING));
//    }
////    public void setSaveFalse() {
////        saving = false;
////        if (DEBUG) {Log.d(TAG, "STOPSAVEMESSAGE" );}
////    }