package io.github.kermit95.signin.app;

import android.app.Application;

/**
 * Created by kermit on 16/5/6.
 */
public class App extends Application{

    private static App instance;

    public static App getInstance(){
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }
}
