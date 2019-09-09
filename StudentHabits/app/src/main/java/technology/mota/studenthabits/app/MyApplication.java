package technology.mota.studenthabits.app;

import android.app.Application;
import android.content.Intent;


import com.androidnetworking.AndroidNetworking;

import java.util.ArrayList;
import java.util.List;

import technology.mota.studenthabits.AppConst;
import technology.mota.studenthabits.BuildConfig;
import technology.mota.studenthabits.data.AppItem;
import technology.mota.studenthabits.data.DataManager;
import technology.mota.studenthabits.db.DbHistoryExecutor;
import technology.mota.studenthabits.db.DbIgnoreExecutor;
import technology.mota.studenthabits.service.AppService;
import technology.mota.studenthabits.util.CrashHandler;
import technology.mota.studenthabits.util.PreferenceManager;



public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        PreferenceManager.init(this);
        getApplicationContext().startService(new Intent(getApplicationContext(), AppService.class));
        DbIgnoreExecutor.init(getApplicationContext());
        DbHistoryExecutor.init(getApplicationContext());
        DataManager.init();
        addDefaultIgnoreAppsToDB();
        if (AppConst.CRASH_TO_FILE) CrashHandler.getInstance().init();
        AndroidNetworking.initialize(getApplicationContext());
    }

    private void addDefaultIgnoreAppsToDB() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<String> mDefaults = new ArrayList<>();
                mDefaults.add("com.android.settings");
                mDefaults.add(BuildConfig.APPLICATION_ID);
                for (String packageName : mDefaults) {
                    AppItem item = new AppItem();
                    item.mPackageName = packageName;
                    item.mEventTime = System.currentTimeMillis();
                    DbIgnoreExecutor.getInstance().insertItem(item);
                }
            }
        }).run();
    }


}
