package com.example.rclark.devicesync.backend;

/**
 * Created by rclar on 4/25/2016.
 */
public class Joker {
    public String getJoke(){
        return "This is totally a funny joke";
    }

    //lets just add another function which will return a modified joke string
    public String getJokes(String jokeid) {
        return "This is totally a funny joke " + jokeid;
    }
}
