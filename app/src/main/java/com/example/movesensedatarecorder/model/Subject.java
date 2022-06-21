package com.example.movesensedatarecorder.model;

import android.os.Build;

import java.io.Serializable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Subject implements Serializable {

    private String name, lastName,subjID;

    public Subject(String name, String lastName,String subjID){
        this.name = name;
        this.lastName = lastName;
        this.subjID  = subjID;
    }

    public String getSubjID() {
        return subjID;
    }

    public String getLastName() {
        return lastName;
    }

    public String getName() {
        return name;
    }

    public String toCsvRow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Stream.of(name, lastName, subjID)
                    .map(value -> value.replaceAll("\"", "\"\""))
                    .map(value -> Stream.of("\"", ",").anyMatch(value::contains) ? "\"" + value + "\"" : value)
                    .collect(Collectors.joining(","));
        } return null;
    }
}
