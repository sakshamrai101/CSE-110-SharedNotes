package edu.ucsd.cse110.sharednotes.model;

import android.util.Log;

import androidx.annotation.AnyThread;
import androidx.annotation.MainThread;
import androidx.annotation.WorkerThread;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class NoteAPI {
    // TODO: Implement the API using OkHttp!
    // TODO: - getNote (maybe getNoteAsync)
    // TODO: - putNote (don't need putNotAsync, probably)
    // TODO: Read the docs: https://square.github.io/okhttp/
    // TODO: Read the docs: https://sharednotes.goto.ucsd.edu/docs

    private volatile static NoteAPI instance = null;
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private OkHttpClient client;

    public NoteAPI() {
        this.client = new OkHttpClient();
    }

    public static NoteAPI provide() {
        if (instance == null) {
            instance = new NoteAPI();
        }
        return instance;
    }

    /**
     * An example of sending a GET request to the server.
     *
     * The /echo/{msg} endpoint always just returns {"message": msg}.
     *
     * This method should can be called on a background thread (Android
     * disallows network requests on the main thread).
     */
    @WorkerThread
    public String echo(String msg) {
        // URLs cannot contain spaces, so we replace them with %20.
        String encodedMsg = msg.replace(" ", "%20");

        var request = new Request.Builder()
                .url("https://sharednotes.goto.ucsd.edu/echo/" + encodedMsg)
                .method("GET", null)
                .build();

        try (var response = client.newCall(request).execute()) {
            assert response.body() != null;
            var body = response.body().string();
            Log.i("ECHO", body);
            return body;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    Note getNote(String title) throws ExecutionException, InterruptedException, TimeoutException {
        title = title.replace(" ", "%20");
        var request = new Request.Builder()
                .url("https://sharednotes.goto.ucsd.edu/notes/" +title)
                .method("GET", null)
                .build();

        var executor = Executors.newSingleThreadExecutor();
        Callable<Note> callable = () ->{
            var response = client.newCall(request).execute();
            assert response.body() != null;
            var body = response.body().string();
            Log.i("GET NOTE", body);
            return Note.fromJSON(body);
        };
        Future<Note> noteFuture = executor.submit(callable);
        Note note = noteFuture.get(1, TimeUnit.SECONDS);
        return note;
    }
    String post(String url, String json)  {
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        try (Response response = client.newCall(request).execute()) {
            return response.body().string();
        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public void putNote(Note note){
        var title = note.title.replace(" ", "%20");
        var body = RequestBody.create(toJson(note), JSON);
        Log.i("Test", toJson(note));
        var request = new Request.Builder()
                .url("https://sharednotes.goto.ucsd.edu/notes/" + title)
                .method("PUT", body)
                .build();
        var executor = Executors.newSingleThreadExecutor();
        executor.execute(() ->{
            String body1 = "";
            try {
                Response response = client.newCall(request).execute();
                assert response.body() != null;
                body1 = response.body().string();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            Log.i("PUT NOTE", body1);
        });


    }

    public String toJson(Note note) {
        String toJson = "{  " + "\"content\":" + "\"" + note.content + "\"" + "," + "\"version\":" + "\"" + note.version + "\"" + "}";
        Log.i("Formatted Json", toJson);
        return toJson;

    }


    @AnyThread
    public Future<String> echoAsync(String msg) {
        var executor = Executors.newSingleThreadExecutor();
        var future = executor.submit(() -> echo(msg));

        // We can use future.get(1, SECONDS) to wait for the result.
        return future;
    }
}
