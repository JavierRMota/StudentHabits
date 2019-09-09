package technology.mota.studenthabits.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.usage.UsageEvents;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.telephony.TelephonyManager;
import android.transition.Fade;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.gson.JsonObject;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import technology.mota.studenthabits.GlideApp;
import technology.mota.studenthabits.R;
import technology.mota.studenthabits.data.AppItem;
import technology.mota.studenthabits.data.DataManager;
import technology.mota.studenthabits.data.SendableItem;
import technology.mota.studenthabits.db.DbIgnoreExecutor;
import technology.mota.studenthabits.service.AlarmService;
import technology.mota.studenthabits.service.AppService;
import technology.mota.studenthabits.util.AppUtil;
import technology.mota.studenthabits.util.PreferenceManager;

public class MainActivity extends AppCompatActivity {

    private LinearLayout mSort;
    private Switch mSwitch;
    private TextView mSwitchText;
    private RecyclerView mList;
    private MyAdapter mAdapter;
    private AlertDialog mDialog;
    private SwipeRefreshLayout mSwipe;
    private TextView mSortName;
    private long mTotal;
    private PackageManager mPackageManager;
    private Button sendButton;
    public String id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // https://guides.codepath.com/android/Shared-Element-Activity-Transition
        getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        getWindow().setExitTransition(new Fade(Fade.OUT));
        setContentView(R.layout.activity_main);
        mPackageManager = getPackageManager();

        mSort = findViewById(R.id.sort_group);
        mSortName = findViewById(R.id.sort_name);
        mSwitch = findViewById(R.id.enable_switch);
        mSwitchText = findViewById(R.id.enable_text);
        mAdapter = new MyAdapter();

        mList = findViewById(R.id.list);
        mList.setLayoutManager(new LinearLayoutManager(this));
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(mList.getContext(), DividerItemDecoration.VERTICAL);
        dividerItemDecoration.setDrawable(getResources().getDrawable(R.drawable.divider, getTheme()));
        mList.addItemDecoration(dividerItemDecoration);
        mList.setAdapter(mAdapter);

        sendButton = findViewById(R.id.sendButton);

        initLayout();
        initEvents();
        initSort();

        if (DataManager.getInstance().hasPermission(getApplicationContext())) {
            process();
            startService(new Intent(this, AlarmService.class));
        }
    }

    private void initLayout() {
        mSwipe = findViewById(R.id.swipe_refresh);
        if (DataManager.getInstance().hasPermission(getApplicationContext())) {
            mSwitchText.setText(R.string.enable_apps_monitoring);
            mSwitch.setVisibility(View.GONE);
            mSort.setVisibility(View.VISIBLE);
            mSwipe.setEnabled(true);
            sendButton.setVisibility(View.VISIBLE);
        } else {
            mSwitchText.setText(R.string.enable_apps_monitor);
            mSwitch.setVisibility(View.VISIBLE);
            mSort.setVisibility(View.GONE);
            sendButton.setVisibility(View.GONE);
            mSwitch.setChecked(false);
            mSwipe.setEnabled(false);
        }
    }

    private void initSort() {
        if (DataManager.getInstance().hasPermission(getApplicationContext())) {
            mSort.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    triggerSort();
                }
            });
        }
    }

    private void triggerSort() {
        mDialog = new AlertDialog.Builder(MainActivity.this)
                .setTitle(R.string.sort)
                .setSingleChoiceItems(R.array.sort, PreferenceManager.getInstance().getInt(PreferenceManager.PREF_LIST_SORT), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        PreferenceManager.getInstance().putInt(PreferenceManager.PREF_LIST_SORT, i);
                        process();
                        mDialog.dismiss();
                    }
                })
                .create();
        mDialog.show();
    }


    private void initEvents() {
        if (!DataManager.getInstance().hasPermission(getApplicationContext())) {
            mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if (b) {
                        Intent intent = new Intent(MainActivity.this, AppService.class);
                        intent.putExtra(AppService.SERVICE_ACTION, AppService.SERVICE_ACTION_CHECK);
                        startService(intent);
                    }
                }
            });
        }
        mSwipe.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                process();
            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (!DataManager.getInstance().hasPermission(getApplicationContext())) {
            mSwitch.setChecked(false);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (DataManager.getInstance().hasPermission(this)) {
            mSwipe.setEnabled(true);
            mSort.setVisibility(View.VISIBLE);
            mSwitch.setVisibility(View.GONE);
            initSort();
            process();
        }
    }

    private void process() {
        if (DataManager.getInstance().hasPermission(getApplicationContext())) {
            mList.setVisibility(View.INVISIBLE);
            int sortInt = PreferenceManager.getInstance().getInt(PreferenceManager.PREF_LIST_SORT);
            mSortName.setText(getSortName(sortInt));
            new MyAsyncTask().execute(sortInt, 0);
        }
    }

    private String getSortName(int sortInt) {
        return getResources().getStringArray(R.array.sort)[sortInt];
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AppItem info = mAdapter.getItemInfoByPosition(item.getOrder());
        switch (item.getItemId()) {
            case R.id.ignore:
                DbIgnoreExecutor.getInstance().insertItem(info);
                process();
                Toast.makeText(this, R.string.ignore_success, Toast.LENGTH_SHORT).show();
                return true;
            case R.id.open:
                startActivity(mPackageManager.getLaunchIntentForPackage(info.mPackageName));
                return true;
            case R.id.more:
                Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + info.mPackageName));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                startActivityForResult(new Intent(MainActivity.this, SettingsActivity.class), 1);
                return true;
            case R.id.sort:
                triggerSort();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(">>>>>>>>", "result code " + requestCode + " " + resultCode);
        if (resultCode > 0) process();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mDialog != null) mDialog.dismiss();
    }

    class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {

        private List<AppItem> mData;

        MyAdapter() {
            super();
            mData = new ArrayList<>();
        }

        void updateData(List<AppItem> data) {
            mData = data;
            notifyDataSetChanged();
        }

        AppItem getItemInfoByPosition(int position) {
            if (mData.size() > position) {
                return mData.get(position);
            }
            return null;
        }

        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_list, parent, false);
            return new MyViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(MyViewHolder holder, int position) {
            AppItem item = getItemInfoByPosition(position);
            holder.mName.setText(item.mName);
            holder.mUsage.setText(AppUtil.formatMilliSeconds(item.mUsageTime));
            holder.mTime.setText(String.format(Locale.getDefault(),
                    "%s · %d %s · %s",
                    new SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.getDefault()).format(new Date(item.mEventTime)),
                    item.mCount,
                    getResources().getString(R.string.times_only), AppUtil.humanReadableByteCount(item.mMobile))
            );
            if (mTotal > 0) {
                holder.mProgress.setProgress((int) (item.mUsageTime * 100 / mTotal));
            } else {
                holder.mProgress.setProgress(0);
            }
            GlideApp.with(MainActivity.this)
                    .load(AppUtil.getPackageIcon(MainActivity.this, item.mPackageName))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .transition(new DrawableTransitionOptions().crossFade())
                    .into(holder.mIcon);
            holder.setOnClickListener(item);
        }

        @Override
        public int getItemCount() {
            return mData.size();
        }

        class MyViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener {

            private TextView mName;
            private TextView mUsage;
            private TextView mTime;
            private ImageView mIcon;
            private ProgressBar mProgress;

            MyViewHolder(View itemView) {
                super(itemView);
                mName = itemView.findViewById(R.id.app_name);
                mUsage = itemView.findViewById(R.id.app_usage);
                mTime = itemView.findViewById(R.id.app_time);
                mIcon = itemView.findViewById(R.id.app_image);
                mProgress = itemView.findViewById(R.id.progressBar);
                itemView.setOnCreateContextMenuListener(this);
            }

            @SuppressLint("RestrictedApi")
            void setOnClickListener(final AppItem item) {
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(MainActivity.this, DetailActivity.class);
                        intent.putExtra(DetailActivity.EXTRA_PACKAGE_NAME, item.mPackageName);
                        intent.putExtra(DetailActivity.EXTRA_DAY, 0);
                        ActivityOptionsCompat options = ActivityOptionsCompat.
                                makeSceneTransitionAnimation(MainActivity.this, mIcon, "profile");
                        startActivityForResult(intent, 1, options.toBundle());
                    }
                });
            }

            @Override
            public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
                int position = getAdapterPosition();
                AppItem item = getItemInfoByPosition(position);
                contextMenu.setHeaderTitle(item.mName);
                contextMenu.add(Menu.NONE, R.id.open, position, getResources().getString(R.string.open));
                if (item.mCanOpen) {
                    contextMenu.add(Menu.NONE, R.id.more, position, getResources().getString(R.string.app_info));
                }
                contextMenu.add(Menu.NONE, R.id.ignore, position, getResources().getString(R.string.ignore));
            }
        }
    }
    public void createID() {
        SharedPreferences pref = getSharedPreferences("StudentHabits", MODE_PRIVATE);
        if (id.isEmpty()) {
            Toast.makeText(this, "No id", Toast.LENGTH_SHORT).show();
            return;
        }
        pref.edit().putString("DEVICE_ID", id).commit();
        Toast.makeText(this, "ID CREATED: "+id , Toast.LENGTH_SHORT).show();
    }
    public void sendDataUtil(){
        if (id.isEmpty()) {
            Toast.makeText(this, "No id", Toast.LENGTH_SHORT).show();
            return;
        }
        DataManager manager = DataManager.getInstance();
        List<AppItem> items = manager.getApps(this.getApplicationContext(), 0, 0);
        List<SendableItem> itemsS = new ArrayList<SendableItem>();
        for (AppItem i : items) {
            List<AppItem> appItems = manager.getTargetAppTimeline(this,i.mPackageName,0);
            SendableItem sendableItem = new SendableItem();
            List<AppItem> newList = new ArrayList<>();
            for (AppItem item : appItems) {
                if (item.mEventType == UsageEvents.Event.USER_INTERACTION || item.mEventType == UsageEvents.Event.NONE) {
                    continue;
                }
                if (item.mEventType == UsageEvents.Event.MOVE_TO_BACKGROUND) {
                    AppItem newItem = item.copy();
                    newItem.mEventType = -1;
                    newList.add(newItem);
                }
                newList.add(item);
            }
            for (AppItem detail : newList){
                if (detail.mEventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    sendableItem = new SendableItem();
                    sendableItem.mName = detail.mName;
                    sendableItem.mPackageName = detail.mPackageName;
                    sendableItem.mMobileTraffic = detail.mMobile;
                    sendableItem.mIsSystem = AppUtil.isSystemApp(getPackageManager(), detail.mPackageName) ? 1 : 0;
                    sendableItem.mTimeStampStart = detail.mEventTime;
                    sendableItem.mDate = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.getDefault()).format(new Date(sendableItem.mTimeStampStart));
                }
                if (detail.mEventType == -1) {
                    sendableItem.mDuration = detail.mUsageTime;
                }
                if (detail.mEventType == UsageEvents.Event.MOVE_TO_BACKGROUND) {
                    sendableItem.mTimeStampEnd = detail.mEventTime;
                    itemsS.add(sendableItem);
                }
            }
        }
        ArrayList<JSONObject> array = new ArrayList<JSONObject>();
        for ( SendableItem item : itemsS) {
            JSONObject json = new JSONObject();
            try {
                json.put("name", item.mName);
                json.put("pkg_name", item.mPackageName);
                json.put("start", item.mTimeStampStart);
                json.put("end", item. mTimeStampEnd);
                json.put("date", item.mDate);
                json.put("duration", item. mDuration);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            array.add(json);
        }
        JSONObject request = new JSONObject();
        try {
            request.put("id", id);
            request.put("data", array);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        AndroidNetworking.post("https://mota.technology/StudentHabits/API/DATA/")
                .addJSONObjectBody(request)
                .setPriority(Priority.MEDIUM)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String error  = response.getString("error");
                            if (error == "false") {
                                Toast.makeText(MainActivity.this, "Data sent", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(MainActivity.this, response.toString(), Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    @Override
                    public void onError(ANError error) {
                        Toast.makeText(MainActivity.this, "Data response not received", Toast.LENGTH_SHORT).show();
                    }
                });
        Log.e("JSON:", array.toString());
    }
    public void sendData(View v) {
        Toast.makeText(this, "Sending...", Toast.LENGTH_SHORT).show();
        SharedPreferences pref = getSharedPreferences("StudentHabits", MODE_PRIVATE);
        id = pref.getString("DEVICE_ID", "");
        if (id.isEmpty()) {
            AndroidNetworking.post("https://mota.technology/StudentHabits/API/NEW/")
                    .setPriority(Priority.MEDIUM)
                    .build()
                    .getAsJSONObject(new JSONObjectRequestListener() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                id = response.getString("id");
                                createID();
                                sendDataUtil();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        @Override
                        public void onError(ANError error) {
                            Toast.makeText(MainActivity.this, "ID no response", Toast.LENGTH_SHORT).show();
                        }
                    });

        } else {
            sendDataUtil();
        }

    }


    @SuppressLint("StaticFieldLeak")
    class MyAsyncTask extends AsyncTask<Integer, Void, List<AppItem>> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mSwipe.setRefreshing(true);
        }

        @Override
        protected List<AppItem> doInBackground(Integer... integers) {
            return DataManager.getInstance().getApps(getApplicationContext(), integers[0], integers[1]);
        }

        @Override
        protected void onPostExecute(List<AppItem> appItems) {
            mList.setVisibility(View.VISIBLE);
            mTotal = 0;
            for (AppItem item : appItems) {
                if (item.mUsageTime <= 0) continue;
                mTotal += item.mUsageTime;
                item.mCanOpen = mPackageManager.getLaunchIntentForPackage(item.mPackageName) != null;
            }
            mSwitchText.setText(String.format(getResources().getString(R.string.total), AppUtil.formatMilliSeconds(mTotal)));
            mSwipe.setRefreshing(false);
            mAdapter.updateData(appItems);
        }
    }
}
