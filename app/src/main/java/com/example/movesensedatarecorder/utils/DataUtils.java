package com.example.movesensedatarecorder.utils;

import com.example.movesensedatarecorder.model.DataPoint;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.DecimalFormat;

public class DataUtils {

    //TODO: fix gyro data
    public static DataPoint IMU6DataConverter(byte[] data){
        int len = data.length;
        int sensorNum = 2; //IMU6 has 2 sensors: acc and gyro
        int offset = 2;
        int dataSize = 4;
        int coordinates = 3;
        int numOfSamples = (len - 6) / (sensorNum * coordinates * dataSize); //sensorNum data types, 3 coordinates, 4 bytes each
        // parse and interpret the data, ...
        int time = DataUtils.fourBytesToInt(data, offset);

        float accX = DataUtils.fourBytesToFloat(data, offset + dataSize);
        float accY = DataUtils.fourBytesToFloat(data, offset + 2 * dataSize);
        float accZ = DataUtils.fourBytesToFloat(data, offset + 3 * dataSize);

        float gyroX = DataUtils.fourBytesToFloat(data, offset + dataSize + (numOfSamples) * 12);
        float gyroY = DataUtils.fourBytesToFloat(data, offset + 2 * dataSize + (numOfSamples) * 12);
        float gyroZ = DataUtils.fourBytesToFloat(data, offset + 3 * dataSize + (numOfSamples) * 12);

        DataPoint datapoint = new DataPoint(time, accX, accY,accZ, gyroX, gyroY, gyroZ);
        return datapoint;
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
