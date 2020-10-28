package xchova20.audiorecord;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.widget.EditText;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import static java.lang.Math.abs;
import static xchova20.audiorecord.Constants.MAX_LEVELMETER_LEVEL;
import static xchova20.audiorecord.Constants.MAX_SOUND_LEVEL;

/**
 * General purpose functions.
 */
public class Functions {
    /**
     * Gets unique session name from device name and timestamp.
     *
     * @param deviceName device name
     * @return unique session name
     */
    public String getSessionName(String deviceName) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
        return timeStamp + "_" + deviceName.replaceAll("\\s+","");
    }

    /**
     * Gets normalized amplitude from a audio samples.
     * Amplitude is used to show audio level in audio level widget.
     *
     * @param buffer audio samples
     * @return normalized amplitude value
     */
    public double getMaxAmplitude(byte[] buffer) {
        double max = 0;
        short[] shortArray = new short[buffer.length/2];
        ByteBuffer byteBuff = ByteBuffer.wrap(buffer);
        byteBuff.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortArray);
        for (int i = 0; i < shortArray.length; i++) if (abs(shortArray[i]) > max) max = abs(shortArray[i]);
        return max/MAX_SOUND_LEVEL*MAX_LEVELMETER_LEVEL;
    }

    /**
     * Gets buffer size from the buffer size field and changes it to B.
     *
     * @param bufferSizeField buffer size field
     * @return
     */
    public float getBufferSize(EditText bufferSizeField) {
        return Float.parseFloat(bufferSizeField.getText().toString()) * Constants.BYTES_IN_MB;
    }

    /**
     * Builds recorder init message for log.
     * @param sampleRate sample rate
     * @param channelConfig channel config
     * @param isInternal recorder source origin
     * @param version app version
     * @return message for the log
     */
    public String recorderInitLog(int sampleRate, int channelConfig, boolean isInternal, String version) {
        String message = Constants.msg.get("REC_INIT") + "sample rate " + sampleRate + " Hz, ";
        if (channelConfig == AudioFormat.CHANNEL_IN_MONO) message += "channel MONO, ";
        else message += "channel STEREO, ";
        if (isInternal) message += "source MIC, ";
        else message += "source BLUETOOTH, ";
        message += "encoding PCM 16BIT, ";
        message += "version " +  version;
        return message;
    }

    /**
     * Get seconds difference between two dates.
     * @param date_1 first date
     * @param date_2 second date
     * @return number of seconds between these dates
     */
    public long getSecondsDiff(Date date_1, Date date_2) {
        return (date_2.getTime()-date_1.getTime())/1000;
    }

    /**
     * Shows permission asking alert
     * @param context context
     * @param activity activity
     */
    public void permissionAlert(Context context, final Activity activity) {
        new AlertDialog.Builder(context)
            .setTitle(Constants.msg.get("PERMISSION_ASK"))
            .setMessage(Constants.msg.get("PERMISSION_ASK_TEXT"))
            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    askPermissions(activity);
                }
            })
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show();
    }

    /**
     * Asks for dangerous permissions (those that aren't given automatically) by system.
     *
     */
    public void askPermissions(Activity activity) {
        int REQUEST_CODE = 1;
        ActivityCompat.requestPermissions(
                activity,
                Constants.permissions,
                REQUEST_CODE
        );
    }

    /**
     * Checks if permissions are granted.
     * @param context context
     * @return true if all granted, false if not
     */
    public boolean arePermissionsGranted(Context context) {
        for (String permission : Constants.permissions) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if permission is granted.
     * @param context context
     * @param permission permission from manifest
     * @return true if is granted, false if not
     */
    public boolean arePermissionsGranted(Context context, String permission) {
        if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) return false;
        return true;
    }
}

