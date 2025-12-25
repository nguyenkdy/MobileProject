package com.example.mynoesapplication.RetrofitClient;

import com.example.mynoesapplication.Data.AiRequest;
import com.example.mynoesapplication.Data.AiResponse;

import okhttp3.HttpUrl;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

import java.util.concurrent.TimeUnit;

public class AiApiService {

    private static final String BASE_URL = "https://generativelanguage.googleapis.com/";
    private static final String API_KEY = "YOUR_API_KEY_HERE";

    public interface Api {
        // existing text-only endpoint
        @POST("v1beta/models/gemini-2.5-flash:generateContent")
        Call<AiResponse> summarize(@Body AiRequest req);

        // new: multipart endpoint that accepts a file and a prompt part
        @Multipart
        @POST("v1beta/models/gemini-2.5-flash:generateContent")
        Call<AiResponse> summarizePdf(
                @Part MultipartBody.Part file,
                @Part("prompt") RequestBody prompt
        );
    }

    private static Api api;

    public static Api getApi() {
        if (api != null) return api;

        HttpLoggingInterceptor log = new HttpLoggingInterceptor();
        log.setLevel(HttpLoggingInterceptor.Level.BASIC);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request original = chain.request();
                    HttpUrl newUrl = original.url()
                            .newBuilder()
                            .addQueryParameter("key", API_KEY)
                            .build();

                    Request newReq = original.newBuilder()
                            .url(newUrl)
                            .build();

                    return chain.proceed(newReq);
                })
                .addInterceptor(log)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        api = retrofit.create(Api.class);
        return api;
    }
}
