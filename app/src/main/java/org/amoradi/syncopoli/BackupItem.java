package org.amoradi.syncopoli;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;

import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

class BackupItem implements Parcelable {
    private static final String TAG = "Syncopoli";

    public enum Direction {
        INCOMING,
        OUTGOING,
        LOCAL
    };

    public String name;
    public ArrayList<String> sources;
    public String destination;
    public Date lastUpdate;
    public Direction direction;

    public String rsync_options;

    public BackupItem() {
    }

    public BackupItem(BackupItem other) {
        this.name = other.name;

        if (other.sources.size() > 0) {
            this.sources = new ArrayList<String>();
            this.sources.addAll(other.sources);
        }
            
        this.destination = other.destination;
        this.lastUpdate = other.lastUpdate;
        this.direction = other.direction;
        this.rsync_options = other.rsync_options;
    }

    @Override
    public String toString() {
        return "BackupItem { \"" + name + "\"}";
    }

    public String getLogFileName() {
        return Uri.encode("log_" + this.name);
    }

    public int describeContents() {
		return 0;
	}

	public String getSourcesAsString() {
        if (sources != null) {
            return TextUtils.join("\n", sources);
        } else {
            return "";
        }
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeString(name);

        if (sources != null) {
            out.writeStringList(sources);
        } else {
            out.writeStringList(new ArrayList<String>());
        }

        out.writeString(destination);

        Format ft = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // blame this code on the idiosyncracies of java and their
        // multitude of similarly sounding, but not-quite-the-same
        // exceptions
        // If you have a better way to do this, patches welcome
        if (lastUpdate != null && (lastUpdate instanceof Date)) {
            out.writeString(ft.format(lastUpdate));
        } else {
            out.writeString(ft.format(new Date()));
        }

		if (direction == Direction.OUTGOING) {
			out.writeString("OUTGOING");
		} else if (direction == Direction.INCOMING) {
            out.writeString("INCOMING");
        } else {
            out.writeString("LOCAL");
		}

		out.writeString(rsync_options);
	}
	
	public static final Parcelable.Creator<BackupItem> CREATOR
		= new Parcelable.Creator<BackupItem>()
	{
		public BackupItem createFromParcel(Parcel in) {
			BackupItem b = new BackupItem();
			b.name = in.readString();
			b.sources = in.createStringArrayList();
			b.destination = in.readString();

			SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			String x = in.readString();
			try {
				b.lastUpdate = ft.parse(x);
			} catch (ParseException e) {
				Log.e(TAG, "Could not parse date string from parcelable: " + x);
				b.lastUpdate = new Date();
			}

			String y = in.readString();
			if (y.equals("OUTGOING")) {
				b.direction = Direction.OUTGOING;
			} else if (y.equals("INCOMING")) {
                b.direction = Direction.INCOMING;
            } else {
                b.direction = Direction.LOCAL;
			}

			b.rsync_options = in.readString();

			return b;
		}

		public BackupItem[] newArray(int size) {
			return new BackupItem[size];
		}
	};

}
