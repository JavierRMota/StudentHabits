package technology.mota.studenthabits.data;

import java.util.Locale;

public class SendableItem {
    public String mPackageName;
    public String mName;
    public String mDate;
    public int mIsSystem;
    public long mDuration;
    public long mTimeStampStart;
    public long mTimeStampEnd;
    public long mMobileTraffic;

    @Override
    public String toString() {
        return String.format(Locale.getDefault(), "%s %s %s %d %d %d %d %d", mPackageName, mName, mDate, mIsSystem, mDuration, mTimeStampStart,mTimeStampEnd, mMobileTraffic);
    }
}
