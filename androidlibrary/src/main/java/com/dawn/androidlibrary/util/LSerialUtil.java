package com.dawn.androidlibrary.util;

import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;

import android_serialport_api.SerialPort;

/**
 * 串口工具类
 */
@SuppressWarnings("unused")
public class LSerialUtil {
    public enum SerialType{
        TYPE_HEX, TYPE_ASCII
    }
    /**
     * 串口名称
     */
    private final static String PATH_NAME_0 = "/dev/ttyS0";
    private final static String PATH_NAME_1 = "/dev/ttyS1";
    private final static String PATH_NAME_2 = "/dev/ttyS2";
    private final static String PATH_NAME_3 = "/dev/ttyS3";
    private final static String PATH_NAME_4 = "/dev/ttyS4";
    private final static String PATH_NAME_5 = "/dev/ttyS5";
    private final static String PATH_NAME_6 = "/dev/ttyS6";
    private final static int baudRateDefault = 115200;
    private final static int dataBitsDefault = 8;
    private final static int stopBitsDefault = 1;
    private final static char parityDefault = 'N';
    private String mPathName = PATH_NAME_1;//串口地址
    private int mBaudRate = baudRateDefault;//波特率
    private int mDataBits = dataBitsDefault;//数据位
    private int mStopBits = stopBitsDefault;//停止位
    private char mParity = parityDefault;//校验
    private SerialType mSerialType = SerialType.TYPE_HEX;
    private PrintWriter mPrintWriter;//串口字符串输出
    private BufferedReader mBufferedReader;//串口字符串接收
    private OutputStream mOutputStream;//串口16进制输出
    private InputStream mInputStream;//串口16进制输入
    private OnSerialListener mListener;//回调函数
    private String receiverHexStrCache = "";//缓存接收到的16进制字符串
    private String receiverStrCache = "";//缓存接受到字符串

    private SerialHandler mHandler = new SerialHandler(this);
    private final static int h_join_hex_str = 1;//缓存字符串，防止接收时一条信息分两条发收不到
    private final static int d_join_hex_str = 20;//缓存的时间

    private final static int h_join_str = 2;
    private final static int d_join_str = 10;

    public LSerialUtil(int port, int baudRate, SerialType serialType, OnSerialListener listener){
        this(port, baudRate, dataBitsDefault, stopBitsDefault, parityDefault, serialType, listener);
    }

    /**
     * 初始化
     * @param port 串口
     * @param baudRate 波特率
     * @param dataBits 数据位
     * @param stopBits 停止位
     * @param parity 校验位
     * @param serialType 串口类型
     * @param listener 回调
     */
    @SuppressWarnings("WeakerAccess")
    public LSerialUtil(int port, int baudRate, int dataBits, int stopBits, char parity, SerialType serialType, OnSerialListener listener){
        switch (port){
            case 0:
                this.mPathName = PATH_NAME_0;
                break;
            case 1:
                this.mPathName = PATH_NAME_1;
                break;
            case 2:
                this.mPathName = PATH_NAME_2;
                break;
            case 3:
                this.mPathName = PATH_NAME_3;
                break;
            case 4:
                this.mPathName = PATH_NAME_4;
                break;
            case 5:
                this.mPathName = PATH_NAME_5;
                break;
            case 6:
                this.mPathName = PATH_NAME_6;
                break;
        }
        this.mBaudRate = baudRate;
        this.mDataBits = dataBits;
        this.mStopBits = stopBits;
        this.mParity = parity;
        this.mSerialType = serialType;
        this.mListener = listener;
        startPort();
    }

    /**
     * 开启串口
     */
    private void startPort(){
        try {
            SerialPort mSerialPort = new SerialPort(new File(this.mPathName), this.mBaudRate, mDataBits, mStopBits, mParity);
            switch (mSerialType){
                case TYPE_HEX:
                    mOutputStream = mSerialPort.getOutputStream();
                    mInputStream = mSerialPort.getInputStream();
                    receiverHexMsg();
                    break;
                case TYPE_ASCII:
                    mPrintWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(mSerialPort.getOutputStream(), "UTF-8")), true);
                    mBufferedReader = new BufferedReader(new InputStreamReader(mSerialPort.getInputStream(), "UTF-8"));
                    receiverAsciiMsg();
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            LLog.exception(e);
            if(mListener != null)
                mListener.startError();
        }
    }

    /**
     * 接收串口数据
     */
    private void receiverHexMsg(){
        new Thread(){
            @Override
            public void run() {
                super.run();
                while (!isInterrupted()){
                    byte[] buffer = new byte[128];
                    int size;
                    try {
                        size = mInputStream.read(buffer);
                    } catch (IOException e) {
                        e.printStackTrace();
                        if(mListener != null)
                            mListener.receiverError();
                        break;
                    }
                    if(size > 0){
                        //如果接收到字符，需要进行字符拼接，防止一次数据分多次发送
                        joinHexReceiverStr(LStringUtil.byte2HexStr(buffer, size));
                    }
                }
            }
        }.start();
    }

    /**
     * 拼接字符串
     */
    private void joinHexReceiverStr(String str){
        receiverHexStrCache += str;
        mHandler.removeMessages(h_join_hex_str);
        mHandler.sendEmptyMessageDelayed(h_join_hex_str, d_join_hex_str);
    }

    /**
     * 发送bytes数据
     */
    public void sendHexMsg(@NonNull byte[] bytes){
        try{
            if(mOutputStream != null)
                mOutputStream.write(bytes);
        }catch (Exception e){
            e.printStackTrace();
            if(mListener != null)
                mListener.startError();
        }
    }

    /**
     * 接收字符串格式的数据
     */
    private void receiverAsciiMsg(){
        new Thread(){
            @Override
            public void run() {
                super.run();
                String receiverMsg;
                while (!isInterrupted()){
                    try {
                        if((receiverMsg = mBufferedReader.readLine()) != null){
                            joinReceiverStr(receiverMsg);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        if(mListener != null)
                            mListener.receiverError();
                        break;
                    }
                }
            }
        }.start();
    }

    /**
     * 拼接字符串
     */
    private void joinReceiverStr(String str){
        receiverStrCache += str;
        mHandler.removeMessages(h_join_str);
        mHandler.sendEmptyMessageDelayed(h_join_str, d_join_str);
    }

    /**
     * 串口发送字符串格式的数据
     */
    public void sendAsciiMsg(@NonNull String msg){
        try{
            if(mPrintWriter != null){
                mPrintWriter.write(msg);
                mPrintWriter.write("\n\r");
                mPrintWriter.flush();
            }
        }catch (Exception e){
            e.printStackTrace();
            if(mListener != null)
                mListener.sendError();
        }
    }

    private static class SerialHandler extends Handler{
        private final WeakReference<LSerialUtil> mSerialUtil;
        public SerialHandler(LSerialUtil serialUtil){
            mSerialUtil = new WeakReference<>(serialUtil);
        }

        public LSerialUtil getHandler(){
            return mSerialUtil.get();
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            LSerialUtil serialUtil = mSerialUtil.get();
            switch (msg.what){
                case h_join_hex_str://拼接
                    if(serialUtil.mListener != null)
                        serialUtil.mListener.getReceiverStr(serialUtil.receiverHexStrCache);
                    serialUtil.receiverHexStrCache = "";
                    break;
                case h_join_str:
                    if(serialUtil.mListener != null)
                        serialUtil.mListener.getReceiverStr(serialUtil.receiverStrCache);
                    serialUtil.receiverStrCache = "";
                    break;
            }
        }
    }

    /**
     * 串口的回调接口
     */
    public interface OnSerialListener{
        void startError();
        void receiverError();
        void sendError();
        void getReceiverStr(String str);
    }
}
