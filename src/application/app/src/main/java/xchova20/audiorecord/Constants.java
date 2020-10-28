package xchova20.audiorecord;

import android.Manifest;

import java.util.HashMap;
import java.util.Map;

/**
 * All application constants.
 */
public class Constants {
    /**
     * Do not instantiate this class.
     */
    private Constants() {}
    // general settings
    public static final String START_FOLDER = "/audiorecord/sessions";
    public static final String START_SERVER_ADDR =  "https://www.prednasky.com/recorder/server.php";
    public static final String CONF = "audiorecord.conf";
    public static final int START_SAMPLE_RATE = 48000;
    public static int BYTES_IN_MB = 1000000;
    public static int MAX_SOUND_LEVEL = 32767;
    public static int MAX_LEVELMETER_LEVEL = 100;
    public static String[] permissions = {
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    // messages displayed or logged
    public static final Map<String, String> msg = new HashMap<String, String>()
    {{
        put("HANDSHAKE_FAIL",  "Handshake failed");
        put("BUFFER_REC_CREATED", "Created buffer for recording of size ");
        put("BUFFER_SEND_CREATED", "Created buffer for sending of size ");
        put("REC_INIT", "Recorder initialized with parameters: ");
        put("REC_START", "Recording started");
        put("WIFI_RETRY", "Wifi not enabled, retrying ...");
        put("UNSENT_PACKET", "There are unsent packets, saving (");
        put("SENDING_IN_PROGRESS", "Sending already in progress, saving (");
        put("GOODBYE", "goodbye");
        put("UNSENT_SUCCESS", "Sent unsent packet (");
        put("LOST_PACKET", "Lost packet, saving (");
        put("PATH_ERR", "Path has to end with 'recordings'");
        put("NO_FILES", "No files found in directory ");
        put("SERVER_ADDR_ERR", "Server address is required!");
        put("DEVICE_NAME_ERR", "Device name is required!");
        put("BUFFER_SIZE_ERR", "Buffer size is required!");
        put("DIR_FAIL", "Failed to create directory ");
        put("FILE_FAIL", "Failed to create file ");
        put("SAVE_UNSENT_ERR", "Could not write unsent packet (");
        put("SAVE_UNSENT_SUCCESS", "Successfully saved packet (");
        put("LOGVIEW_ERR", "Logging window in view undefined\n");
        put("COMPOSE_ERR", "Could not save composed file");
        put("COMPOSE_SUCCESS", "Composed recording can be found in ");
        put("FILE_MANAGER_ERR", "Directory manager not available");
        put("FILE_MANAGER_TITLE", "Manual folder setting");
        put("MANUAL_DIR", "You can input directory manually (absolute path)");
        put("FOLDER_NOT_EXIST", "Folder does not exist");
        put("FOLDER_SET", "Folder set");
        put("AUDIO_ERR", "Invalid combination of channel configuration and sample rate for this device");
        put("PREV_SESSION_START", "Starting to send packets from already ended session ");
        put("PREV_SESSION_FINISH", "All packets from the device have been sent for session ");
        put("PREV_SESSION_DISCONNECT", "Lost connection and did not finish sending packets from already finished session ");
        put("PACKET_SENT", "Sent packet");
        put("SEND_MESSAGE", "Enter message");
        put("MESSAGE_SENT", "Message sent");
        put("LOG_SEND_START_ERROR", "Available only during recording");
        put("LOG_SEND_ERROR", "Manual log failed to send");
        put("STORAGE_FAIL", "Failed to create session directories");
        put("RECORDER_INIT_FAIL", "Failed to initialize recorder");
        put("PERMISSION_ASK", "Please grant permissions");
        put("PERMISSION_ASK_TEXT", "The application requires permissions in order to work correctly");
    }};
}
