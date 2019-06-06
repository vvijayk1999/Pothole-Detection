package com.pothole.pothole;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

public class Permissons {

    //Request Permisson
    public static void Request_STORAGE(Activity act, int code)
    {

        ActivityCompat.requestPermissions(act, new
                String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},code);
    }
    public static void Request_FINE_LOCATION(Activity act,int code)
    {
        ActivityCompat.requestPermissions(act, new
                String[]{Manifest.permission.ACCESS_FINE_LOCATION},code);
    }
    public static void Request_PHONE_STATE(Activity act,int code)
    {
        ActivityCompat.requestPermissions(act, new
                String[]{Manifest.permission.READ_PHONE_STATE},code);
    }

    //Check Permisson
    public static boolean Check_STORAGE(Activity act)
    {
        int result = ContextCompat.checkSelfPermission(act,android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return result == PackageManager.PERMISSION_GRANTED;
    }
    public static boolean Check_FINE_LOCATION(Activity act)
    {
        int result = ContextCompat.checkSelfPermission(act, Manifest.permission.ACCESS_FINE_LOCATION);
        return result == PackageManager.PERMISSION_GRANTED;
    }
    public static boolean Check_PHONE_STATE(Activity act)
    {
        int result = ContextCompat.checkSelfPermission(act, Manifest.permission.READ_PHONE_STATE);
        return result == PackageManager.PERMISSION_GRANTED;
    }
}