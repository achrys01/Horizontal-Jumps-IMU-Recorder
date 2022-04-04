package com.example.movesensedatarecorder.utils;

import android.util.Log;

import com.example.movesensedatarecorder.model.DataPoint;

import java.lang.reflect.Executable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DataUtils {

    public static ArrayList<DataPoint> IMU6DataConverter(byte[] data){
        ArrayList<DataPoint> dataPointList = new ArrayList<>();
        int len = data.length;
        int sensorNum = 2; //IMU6 has 2 sensors: acc and gyro
        int offset = 2;
        int dataSize = 4;
        int coordinates = 3;
        int numOfSamples = (len - 6) / (sensorNum * coordinates * dataSize); //sensorNum data types, 3 coordinates, 4 bytes each
        // parse and interpret the data, ...
        //Log.i("data length: ", String.valueOf(len));
        //Log.i("data: ", Arrays.toString(data));
        if(((len - 6f) / (sensorNum * coordinates * dataSize)) % 1 == 0){

            for (int i = 0; i < numOfSamples; i++) {
                int time = DataUtils.fourBytesToInt(data, offset);

                int sampleOffset = offset + i * 12;
                float accX = DataUtils.fourBytesToFloat(data, sampleOffset + dataSize);
                float accY = DataUtils.fourBytesToFloat(data, sampleOffset + 2 * dataSize);
                float accZ = DataUtils.fourBytesToFloat(data, sampleOffset + 3 * dataSize);

                float gyroX = DataUtils.fourBytesToFloat(data, sampleOffset + dataSize + (numOfSamples) * 12);
                float gyroY = DataUtils.fourBytesToFloat(data, sampleOffset + 2 * dataSize + (numOfSamples) * 12);
                float gyroZ = DataUtils.fourBytesToFloat(data, sampleOffset + 3 * dataSize + (numOfSamples) * 12);

                DataPoint datapoint = new DataPoint(time, accX, accY, accZ, gyroX, gyroY, gyroZ);
                dataPointList.add(datapoint);
                //return datapoint;
            }
            return dataPointList;
        } else {
            return null;
        }
    }

    public static int fourBytesToInt(byte[] bytes, int offset) {
        return ByteBuffer.wrap(bytes, offset, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    public static float fourBytesToFloat(byte[] bytes, int offset) {
        return ByteBuffer.wrap(bytes, offset, 4).order(ByteOrder.LITTLE_ENDIAN).getFloat();
    }

    public static byte[] stringToAsciiArray(byte id, String command) {
        if (id > 127) throw new IllegalArgumentException("id= " + id);
        char[] chars = command.trim().toCharArray();
        byte[] ascii = new byte[chars.length + 2];
        ascii[0] = 1;
        ascii[1] = id;
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] > 127) throw new IllegalArgumentException("ascii val= " + (int) chars[i]);
            ascii[i + 2] = (byte) chars[i];
        }
        return ascii;
    }

    public static byte[] stringToAsciiArray(String str) {
        char[] chars = str.trim().toCharArray();
        byte[] ascii = new byte[chars.length];
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] > 127) throw new IllegalArgumentException("ascii val= " + (int) chars[i]);
            ascii[i] = (byte) chars[i];
        }
        return ascii;
    }

    public static String getAccAsStr(DataPoint dataPoint) {
        DecimalFormat df = new DecimalFormat("0.00");
        String accStr = "X: " + df.format(dataPoint.getAccX()) + " Y: " + df.format(dataPoint.getAccY()) + " Z: " + df.format(dataPoint.getAccZ());
        return accStr;
    }

    public static String getGyroAsStr(DataPoint dataPoint) {
        DecimalFormat df = new DecimalFormat("0.00");
        String gyroStr = "X: " + df.format(dataPoint.getGyroX()) + " Y: " + df.format(dataPoint.getGyroY()) + " Z: " + df.format(dataPoint.getGyroZ());
        return gyroStr;
    }

}
