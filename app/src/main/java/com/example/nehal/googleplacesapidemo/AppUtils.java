package com.example.nehal.googleplacesapidemo;

import android.content.Context;

import com.google.gson.GsonBuilder;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by nehal on 12/7/17.
 */

public class AppUtils {

    public static Api getRetrofitApi(String url, final Context context) {
        Retrofit retrofit = new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create(new GsonBuilder()
                        .excludeFieldsWithoutExposeAnnotation()
                        .create()))
                .baseUrl(url)
                 .build();

        Api api = retrofit.create(Api.class);
        return api;
    }
}
