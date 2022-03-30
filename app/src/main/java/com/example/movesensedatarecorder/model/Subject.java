package com.example.movesensedatarecorder.model;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Subject implements Serializable {

    private String name, lastName, email, subjID;
    private double height, weight;

    public Subject(String name, String lastName, String email, double height, double weight, String subjID){
        this.name = name;
        this.lastName = lastName;
        this.email = email;
        this.height = height;
        this.weight = weight;
        this.subjID  = subjID;
    }

    public double getHeight() {
        return height;
    }

    public double getWeight() {
        return weight;
    }

    public String getEmail() {
        return email;
    }

    public String getSubjIDID() {
        return subjID;
    }

    public String getLastName() {
        return lastName;
    }

    public String getName() {
        return name;
    }
}
