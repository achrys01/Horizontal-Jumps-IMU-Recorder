package com.example.movesensedatarecorder.model;

import android.os.Build;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ExpPoint {

    private String mov, loc, subjID, expID;
    private String accX, accY, accZ, gyroX, gyroY, gyroZ, time;

    public ExpPoint(DataPoint dataPoint, String mExpID, String mMov, String mSubjID, String mLoc){
        this.accX = String.valueOf(dataPoint.getAccX());
        this.accY = String.valueOf(dataPoint.getAccY());
        this.accZ = String.valueOf(dataPoint.getAccZ());
        this.gyroX = String.valueOf(dataPoint.getGyroX());
        this.gyroY = String.valueOf(dataPoint.getGyroY());
        this.gyroZ = String.valueOf(dataPoint.getGyroZ());
        this.time = String.valueOf(dataPoint.getTime());
        this.mov = mMov;
        this.expID = mExpID;
        this.subjID = mSubjID;
        this.loc = mLoc;
    }

    public String toCsvRow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Stream.of(accX, accY, accZ, gyroX, gyroY, gyroZ, time, expID, mov, loc, subjID)
                    .map(value -> value.replaceAll("\"", "\"\""))
                    .map(value -> Stream.of("\"", ",").anyMatch(value::contains) ? "\"" + value + "\"" : value)
                    .collect(Collectors.joining(","));
        } return null;
    }
}
