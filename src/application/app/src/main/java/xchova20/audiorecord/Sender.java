package xchova20.audiorecord;

import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


/**
 * Class for sending and saving logs.
 */
public class Sender {
    public static int EXIT_FAILURE = -1;

    public TextView logTextView;
    private String sessionName;
    public String serverAddress;
    private String logPath;
    private Boolean isAbsolute;
    public Date startRecording;
    private Boolean previousSession;
    private Functions functions = new Functions();

    /**
     * Log constructor.
     *
     * @param sessionName session name
     * @param folder saving folder
     * @param isAbsolute absoluteness of folder
     * @param serverAddress server address
     * @param logTextView log preview widget
     * @param previousSession signals if sending is going to be for previously ended session
     */
    public Sender(String sessionName, String folder, Boolean isAbsolute, String serverAddress, TextView logTextView, Boolean previousSession) {
        this.sessionName = sessionName;
        this.serverAddress = serverAddress;
        this.logTextView = logTextView;
        this.previousSession = previousSession;
        this.setFolder(folder, isAbsolute);
    }

    /**
     * Sets log path
     * @param folder saving folder
     * @param isAbsolute absoluteness of path
     */
    public void setFolder(String folder, Boolean isAbsolute) {
        this.logPath = folder + "/" + sessionName + "/log.txt";
        this.isAbsolute = isAbsolute;
    }


    /**
     * Sends log to server and also saves it.
     *
     * @param message log message
     * @return true if message has been sent, false if not
     * @see #saveLog(String)
     * @see #sendRequest(Map)
     */
    public boolean sendLog(final String message)  {
        long elapsed = 0;
        if (startRecording != null) elapsed = functions.getSecondsDiff(startRecording, new Date());
        final String timeStamp = new SimpleDateFormat("dd.MM.yyyy HH:mm").format(Calendar.getInstance().getTime());
        this.saveLog(message, timeStamp);
        final long finalElapsed = elapsed;
        Map<String, String> params = new HashMap<String, String>()
        {{
            put("session", sessionName);
            put("type", "log");
            put("elapsed", String.valueOf(finalElapsed));
            put("previous_session", String.valueOf(previousSession));
            put("time", timeStamp);
            put("message", message);
        }};
        return sendRequest(params);
    }

    /**
     * Adds a log message into log preview widget.
     *
     * @param message log message
     */
    private void addMessageLogView(final String message) {
        new Handler(Looper.getMainLooper()).post(new Runnable(){
            @Override
            public void run() {
                logTextView.append(message + "\n");
                final ScrollView scrollView = (ScrollView) logTextView.getParent();
                scrollView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        scrollView.fullScroll(ScrollView.FOCUS_DOWN);
                    }
                },100);
            }
        });
    }

    /**
     * Saves log message into local log file.
     *
     * @param message Log message
     * @param timeStamp Log timestamp
     */
    public void saveLog(final String message, final String timeStamp) {
        long elapsed = 0;
        if (startRecording != null) elapsed = functions.getSecondsDiff(startRecording, new Date());
        String logMessage = timeStamp + " - " + sessionName + " - " + String.valueOf(elapsed) + "s - " + message + "\n";
        String logMessageShort = timeStamp + " - "+ message;
        if (logTextView != null)  addMessageLogView(logMessageShort);
        else logMessage += Constants.msg.get("LOGVIEW_ERR");
        try {
            File file;
            if (isAbsolute) file = new File(logPath);
            else file = new File(Environment.getExternalStorageDirectory().toString() + logPath);
            FileOutputStream stream = new FileOutputStream(file, true);
            try {
                stream.write(logMessage.getBytes());
            } finally {
                stream.close();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Saves log message into local log file.
     *
     * @param message Log message
     */
    public void saveLog(final String message) {
        String timeStamp = new SimpleDateFormat("dd.MM.yyyy HH:mm").format(Calendar.getInstance().getTime());
        saveLog(message, timeStamp);
    }

    /**
     * Sends recording to a server.
     *
     * @param buffer bytes of recorded audio
     * @return true if sending was successful, false if not
     */
    public boolean sendRecording(byte[] buffer) {
        final String audioString = android.util.Base64.encodeToString(buffer, 0);
        Map<String, String> params = new HashMap<String, String>() {{
            put("session", sessionName);
            put("previous_session", String.valueOf(previousSession));
            put("type", "record");
            put("bytes", String.valueOf(audioString.length()));
            put("data", audioString);
        }};
        return this.sendRequest(params);
    }

    /**
     * Sends recording to a server.
     *
     * @param audioString string of recorded audio
     * @return true if sending was successful, false if not
     */
    public boolean sendRecording(final String audioString) {
        Map<String, String> params = new HashMap<String, String>() {{
            put("session", sessionName);
            put("previous_session", String.valueOf(previousSession));
            put("type", "record");
            put("bytes", String.valueOf(audioString.length()));
            put("data", audioString);
        }};
        return this.sendRequest(params);
    }


    /**
     * Sender a request on a server.
     *
     * @param params request data
     * @return true if sending was successful, false if not
     */
    private boolean sendRequest(Map<String, String> params)  {
        boolean status = true;
        String formatted_params = "";
        for (Map.Entry<String, String> entry : params.entrySet()) {
            formatted_params += entry.getKey() + "=" + entry.getValue() + "&";
        }
        byte[] postData = formatted_params.getBytes(StandardCharsets.UTF_8);
        int postDataLength = postData.length;
        String request = serverAddress;
        URL url = null;
        try {
            url = new URL(request);
        } catch (MalformedURLException e) {
            this.saveLog("Invalid server URL " + serverAddress);
            System.exit(EXIT_FAILURE);
        }
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            this.saveLog("Could not connect to server " + serverAddress);
            System.exit(EXIT_FAILURE);
        }
        conn.setDoOutput(true);
        conn.setInstanceFollowRedirects(false);
        try {
            conn.setRequestMethod("POST");
        } catch (ProtocolException e) {
            this.saveLog("Invalid request");
            System.exit(EXIT_FAILURE);
        }
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("charset", "utf-8");
        conn.setRequestProperty("Content-Length", Integer.toString(postDataLength));
        conn.setConnectTimeout(3000);
        conn.setUseCaches(false);
        try {
            DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
            wr.write(postData);
        } catch (Exception e) {
            // could not connect to server
            return false;
        }
        InputStream is = null;
        try {
            is = conn.getInputStream();
        } catch (IOException e) {
            // no response is coming, disconnected and thats ok
        } finally {
            if (is != null) {
                java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
                String output = s.hasNext() ? s.next() : "";
                if (output == "ERROR") status = false;
                try {
                    is.close();
                } catch (IOException e) {
                    this.saveLog("Could not close input stream");
                }
            }
        }
        return status;
    }

    /**
     * Sends a handshake to a server.
     *
     * @return true if server responded, false if not
     */
    public boolean sendHandshake()  {
        Map<String, String> params = new HashMap<String, String>()
        {{
            put("type", "handshake");
            put("session", sessionName);
        }};
        return sendRequest(params);
    }

    /**
     * Signalizes server about the end of session.
     */
    public void sendGoodbye()  {
        if (!previousSession) sendLog(Constants.msg.get("GOODBYE"));
        String log;
        try {
            log = new Storage().readFromFile(logPath, isAbsolute);
        } catch (IOException e) {
            log = "";
        }
        final String finalLog = log;
        Map<String, String> params = new HashMap<String, String>() {{
            put("type", Constants.msg.get("GOODBYE"));
            put("session", sessionName);
            put("data", finalLog);
        }};
        sendRequest(params);
    }
}
