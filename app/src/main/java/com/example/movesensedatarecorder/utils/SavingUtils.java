package com.example.movesensedatarecorder.utils;

import com.example.movesensedatarecorder.model.Subject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

public class SavingUtils {

    public static List<Subject> readSubjectFile(String FILE_NAME, File oldfile) throws IOException, ClassNotFoundException {
        //https://stackoverflow.com/questions/16111496/java-how-can-i-write-my-arraylist-to-a-file-and-read-load-that-file-to-the
        FileInputStream fis = new FileInputStream(oldfile);
        ObjectInputStream ois = new ObjectInputStream(fis);
        List<Subject> subjSet = new ArrayList<>();
        subjSet = (List<Subject>) ois.readObject();
        ois.close();
        return subjSet;
    }
}
