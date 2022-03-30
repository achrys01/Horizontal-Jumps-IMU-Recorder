package com.example.movesensedatarecorder.utils;

import android.util.JsonReader;

import com.example.movesensedatarecorder.model.Subject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SubjectUtils {

    public static List<Subject> parseSubjects(JsonReader reader) throws IOException {
        List<Subject> dataSet = new ArrayList<Subject>();
        reader.beginArray();
        while (reader.hasNext()) {
            dataSet.add(readSubject(reader));
        }
        reader.endArray();
        return dataSet;
    }

    public static Subject readSubject(JsonReader reader) throws IOException {

        String firstName = null;
        String lastName = null;
        String email = null;
        String IDnum = null;
        double height = 0;
        double weight = 0;
        reader.beginObject();

        while (reader.hasNext()) {
            String name = reader.nextName();
            switch (name) {
                case "ID":
                    IDnum = reader.nextString();
                    break;
                case "name":
                    firstName = reader.nextString();
                    break;
                case "lastName":
                    lastName = reader.nextString();
                    break;
                case "weight":
                    weight = reader.nextDouble();
                    break;
                case "height":
                    height = reader.nextDouble();
                    break;
                case "email":
                    email = reader.nextString();
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }
        reader.endObject();
        Subject subject = new Subject(firstName, lastName, email, height, weight, IDnum);
        return subject;
    }

}
