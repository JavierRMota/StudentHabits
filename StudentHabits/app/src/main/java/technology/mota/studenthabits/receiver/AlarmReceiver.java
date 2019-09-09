package technology.mota.studenthabits.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import technology.mota.studenthabits.service.AlarmService;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        context.startService(new Intent(context, AlarmService.class));
    }
}
