package com.example.nehal.googleplacesapidemo;

import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * Created by nehal on 12/7/17.
 */

public interface Api {

    @GET("maps/api/directions/json")
    Call<Object> getDirections(@Query("origin") String origin, @Query("destination") String destination,@Query("sensor") String sensor, @Query("alternatives") String alternatives,@Query("key") String key);
}
