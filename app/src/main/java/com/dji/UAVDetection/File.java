package com.dji.UAVDetection;

import androidx.annotation.NonNull;

import com.amap.api.maps2d.model.LatLng;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class File {
    private static File instance = null;

    private File() {
    }

    public static synchronized File getInstance() {
        if (instance == null)
            instance = new File();
        return instance;
    }

    // -----------------------------------------------------------------------------------------------------------
    public final static String flyFilePath  = "data/data/com.dji.PHANTOM/waypoint1.txt";
    public final static String posFilePath  = "data/data/com.dji.PHANTOM/object1.txt";
    public final static String pos2FilePath = "data/data/com.dji.PHANTOM/waypoint2.txt";


    public void writeLatLng(LatLng latLng, String filePath) {
        switch (filePath.toLowerCase(Locale.ROOT)) {
            case "fly": {
                writeFunc(latLng, flyFilePath);
                break;
            }
            case "position": {
                writeFunc(latLng, posFilePath);
                break;
            }
            case "position2": {
                writeFunc(latLng, pos2FilePath);
                break;
            }

            default:
                break;
        }
    }

    public void deleteLatLng(@NonNull String filePath) {
        switch (filePath.toLowerCase(Locale.ROOT)) {
            case "fly": {
                deleteFunc(flyFilePath);
                break;
            }
            case "position": {
                deleteFunc(posFilePath);
                break;
            }
            case "position2": {
                deleteFunc(pos2FilePath);
                break;
            }

            default:
                break;
        }
    }

    public void readLatLng(ArrayList<String> arrayList, String filePath) {
        try {
            java.io.File file = null;
            switch (filePath.toLowerCase(Locale.ROOT)) {
                case "fly": {
                    file = new java.io.File(flyFilePath);
                    break;
                }
                case "position": {
                    file = new java.io.File(posFilePath);
                    break;
                }
                case "position2": {
                    file = new java.io.File(pos2FilePath);
                    break;
                }
                default:
                    break;
            }
            RandomAccessFile fileR = new RandomAccessFile(file, "r");
            // 按行读取字符串
            String str;
            while ((str = fileR.readLine()) != null) {
                arrayList.add(str);
            }
            fileR.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeFunc(@NonNull LatLng latLng, String filePath) {
        try {
            FileWriter fr = new FileWriter(filePath, true);
            fr.write(latLng.latitude + "," + latLng.longitude + '\n');
            fr.flush();
            fr.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    private void deleteFunc(String filePath) {
        try {
            FileWriter fileWriter = new FileWriter(filePath);
            fileWriter.write("");
            fileWriter.flush();
            fileWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



}