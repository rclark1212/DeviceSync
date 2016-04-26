package com.example.rclark.devicesync.backend;

/**
 * Created by rclar on 4/25/2016.
 */

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;

import javax.inject.Named;

/** An endpoint class we are exposing */
@Api(
        name = "myApi",
        version = "v1",
        namespace = @ApiNamespace(
                ownerDomain = "backend.devicesync.rclark.example.com",
                ownerName = "backend.devicesync.rclark.example.com",
                packagePath=""
        )
)

public class MyEndpoint {

    /** A simple endpoint method that takes a name and says Hi back */
    @ApiMethod(name = "sayHi")
    public MyBean sayHi(@Named("name") String name) {

        MyBean response = new MyBean();
        response.setData("Hi, " + name);

        return response;
    }

    @ApiMethod(name = "getJoke")
    public MyBean getJoke() {
        Joker myJoker = new Joker();

        MyBean response = new MyBean();
        response.setData(myJoker.getJoke());

        return response;
    }

    @ApiMethod(name = "getJokes")
    public MyBean getJokes(@Named("jokeid") String jokeid) {
        Joker myJoker = new Joker();

        MyBean response = new MyBean();
        response.setData(myJoker.getJokes(jokeid));

        return response;
    }
}
