package com.example.rclark.devicesync.backend;

/**
 * Created by rclark on 4/25/2016.
 */
/** The object model for the data we are sending through endpoints */
public class MyBean {

    private String myData;

    public String getData() {
        return myData;
    }

    public void setData(String data) {
        myData = data;
    }
}
