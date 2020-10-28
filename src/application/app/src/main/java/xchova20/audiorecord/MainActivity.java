package xchova20.audiorecord;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static java.lang.Math.abs;


/**
 * Main app activity.
 */
public class MainActivity extends AppCompatActivity {
    // main properties
    private String serverAddress;
    private String deviceName;
    private String sessionName;
    private float bufferSize;
    private String folder;
    private boolean isAbsolute;
    private boolean handshake = false;
    private int sampleRate;
    private int channelConfig;
    private int audioFormat;
    private int minBufferSize;
    private boolean status = false;
    private boolean isHeadset = false;
    private int lastSavedPackage = -1;
    private boolean online = false;
    private Date startRecording;

    // objects
    private AudioRecord recorder;
    private AudioManager audioManager;
    private Thread levelMeterThread;
    private Thread streamThread;
    private Thread handshakeThread;
    private Thread senderThread;
    private Thread senderUnsentPacketThread;
    private List<Thread> logSenderThreads = new ArrayList<>();
    private UIHandler uiHandler;
    private Sender sender;
    private Storage storage;
    private Connectivity connectivity;
    private Functions functions;
    private SharedPreferences config;
    private NotificationManager notifications;


    /**
     * Gets thread used for recording and sending audio. This is where all recording is being
     * made.
     *
     * @return thread for streaming
     */
    public Thread streamThreadFactory() {
        return new Thread(new Runnable() {
            /**
             * Thread run method.
             */

            @Override
            public void run() {
                bufferSize = functions.getBufferSize(uiHandler.bufferSizeField);
                sessionName = functions.getSessionName(deviceName);
                sender = new Sender(sessionName, folder, isAbsolute, serverAddress, uiHandler.logTextView, false);
                storage = new Storage(sender, folder, isAbsolute, sessionName);
                connectivity = new Connectivity(getApplicationContext());
                if (!storage.createSessionDirs()) {
                    sender.saveLog(Constants.msg.get("STORAGE_FAIL"));
                    return;
                }
                handshakeThread = handshakeThreadFactory();
                handshakeThread.run();
                if (!handshake) addLogSender(Constants.msg.get("HANDSHAKE_FAIL"));
                else online = true;
                invalidateOptionsMenu();
                showNotification();

                final byte[] recordBuffer = new byte[minBufferSize];
                addLogSender(Constants.msg.get("BUFFER_REC_CREATED") + minBufferSize);
                byte[] sendBuffer = new byte[Math.round(bufferSize)];
                addLogSender(Constants.msg.get("BUFFER_SEND_CREATED") + bufferSize);
                recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, minBufferSize);

                addLogSender(functions.recorderInitLog(sampleRate, channelConfig, uiHandler.internalRadio.isChecked(), getResources().getString(R.string.app_version)));
                recorder.startRecording();
                startRecording = new Date();
                sender.startRecording = startRecording;
                addLogSender(Constants.msg.get("REC_START"));

                int bytesRead;
                int bytesForSending = 0;

                while (status == true) {
                    bytesRead = recorder.read(recordBuffer, 0, minBufferSize);
                    uiHandler.audioLevel.setProgress((int) functions.getMaxAmplitude(recordBuffer));

                    /** if there are any unsent packets, do send them as top priority, requirements:
                        - handshake (server has to know the session)
                        - unsent packets must exist (so there is something to send)
                        - device has to be online (otherwise it cannot send anything)
                        - sender thread cannot be running (could mess up sequence) */
                    if (handshake && storage.packetsUnsent() && connectivity.isOnline() && !isSendingInProgress()) {
                        senderUnsentPacketThread = unsentPacketSenderFactory();
                        senderUnsentPacketThread.start();
                    }
                    /** if bytes do not overflow the send buffer, save them to buffer **/
                    if (bytesForSending + bytesRead < bufferSize) {
                        System.arraycopy(recordBuffer, 0, sendBuffer, bytesForSending, minBufferSize);
                        bytesForSending += bytesRead;
                    }
                    /** otherwise, empty the send buffer and send them (or save, if sending fails OR sending is already in progress) **/
                    else {
                        if (!connectivity.isWifiOn() && !connectivity.isOnline()) {
                            connectivity.turnWifiOn();
                            sender.saveLog(Constants.msg.get("WIFI_RETRY"));
                        }
                        if (!handshake && handshakeThread == null) {
                            handshakeThread = handshakeThreadFactory();
                            handshakeThread.start();
                        }
                        final byte[] finalSendBuffer = new byte[bytesForSending];
                        System.arraycopy(sendBuffer, 0, finalSendBuffer, 0, bytesForSending);
                        // if there are unsent packets or sending is already in progress, save the recording
                        if (storage.packetsUnsent() || !handshake || isSendingInProgress()) {
                            String nextPackage = String.valueOf((lastSavedPackage == -1 )? abs(lastSavedPackage) : lastSavedPackage + 1);
                            if (senderThread != null && senderThread.isAlive()) {
                                addLogSender(Constants.msg.get("SENDING_IN_PROGRESS") + nextPackage + ")");
                            } else addLogSender(Constants.msg.get("UNSENT_PACKET") + nextPackage + ")");
                            lastSavedPackage = storage.save(android.util.Base64.encodeToString(finalSendBuffer, 0), lastSavedPackage);
                            addLogSender(Constants.msg.get("SAVE_UNSENT_SUCCESS") + nextPackage + ")");
                        } else {
                            senderThread = packetSenderFactory(finalSendBuffer);
                            senderThread.start();
                        }
                        sendBuffer = new byte[Math.round(bufferSize)];
                        bytesForSending = 0;
                        System.arraycopy(recordBuffer, 0, sendBuffer, 0, minBufferSize);
                        bytesForSending += bytesRead;
                    }
                }
                if (!isSendingInProgress()) sender.sendGoodbye();
            }


            /**
             * Detects whether any sending of actual recorded or unsent packets is in progress.
             *
             * @return true if sending is in progress, false if not
             */
            private Boolean isSendingInProgress() {
                return ((senderUnsentPacketThread != null && senderUnsentPacketThread.isAlive()) || (senderThread != null && senderThread.isAlive()));
            }

        });
    }

    /**
     * Add log sender into list of log senders.
     * @param message log message
     */
    private void addLogSender(String message) {
        Thread logSender = logSenderFactory(message);
        logSenderThreads.add(logSender);
        logSender.start();
    }

    /**
     * Sends handshake. This thread is a singleton.
     * Upon successful handshake, sets online status and redraws elements showing connection
     * status.
     *
     * @return handshake thread
     */
    private Thread handshakeThreadFactory() {
        return new Thread(new Runnable() {
            public void run() {
               handshake = sender.sendHandshake();
               handshakeThread = null;
               if (handshake) {
                   online = true;
                   invalidateOptionsMenu();
                   showNotification();
               }
            }
        });
    }

    /**
     * Log sender factory.
     * Waits for its turn to be sent (so that logs aren't sent in bad order), sends itself,
     * dies and removes itself from list of log senders. Active waiting is suitable, because
     * every log sender dies within 3s (HTTP timeout).
     *
     * @param message log message
     * @return log sender thread
     */
    private Thread logSenderFactory(final String message) {
        return new Thread(new Runnable() {
            public void run() {
                while (logSenderThreads.get(0) != Thread.currentThread()) {
                    if (!status) {
                        logSenderThreads.remove(logSenderThreads.indexOf(Thread.currentThread()));
                        return;
                    }
                }
                if (handshake) sender.sendLog(message);
                else sender.saveLog(message);
                logSenderThreads.remove(0);
            }
        });
    }


    /**
     * Creates thread for sending packets. This thread is singleton and cannot run simultaneously
     * with unsentPacketSender.
     * It finishes by sending a packet or saving it, in case sending fails.
     *
     * @param content packet content
     * @see #unsentPacketSenderFactory()
     * @return thread for sending normal packets
     */
    private Thread packetSenderFactory(final byte[] content) {
        return new Thread(new Runnable() {
            public void run() {
                if (!sender.sendRecording(content)) {
                    online = false;
                    invalidateOptionsMenu();
                    showNotification();
                    sender.saveLog(Constants.msg.get("LOST_PACKET") + (lastSavedPackage == -1 ? (lastSavedPackage + 2) + "" : (lastSavedPackage + 1) + "") + ")");
                    lastSavedPackage = storage.save(android.util.Base64.encodeToString(content, 0), lastSavedPackage);
                } else addLogSender(Constants.msg.get("PACKET_SENT"));
                senderThread = null;
                if (!status) sender.sendGoodbye();
            }
        });
    }

    /**
     * Creates thread for sending unsent packets. This thread is singleton and cannot run
     * simultaneously with packetSender. It finishes when there are no more unsent packets or when
     * recording is stopped.
     *
     * @see #packetSenderFactory(byte[])
     * @return thread for sending unsent packets
     */
    Thread unsentPacketSenderFactory() {
        return new Thread(new Runnable() {
            public void run() {
                while (storage.packetsUnsent()) {
                    if (!status) break;
                    String[] packetData = storage.getNextUnsentPacket();
                    if (packetData[0] != null) {
                        if (sender.sendRecording(packetData[0])) {
                            if (!online && status) {
                                online = true;
                                invalidateOptionsMenu();
                                showNotification();
                            }
                            addLogSender(Constants.msg.get("UNSENT_SUCCESS") + packetData[1] + ")");
                            storage.removeUnsentPacket(packetData[1]);
                        } else {
                            if (!connectivity.isWifiOn() && !connectivity.isOnline()) {
                                connectivity.turnWifiOn();
                                sender.saveLog(Constants.msg.get("WIFI_RETRY"));
                            }
                        }
                    }
                }
                senderUnsentPacketThread = null;
                if (!status) sender.sendGoodbye();
            }
        });
    }

    /**
     * Creates thread for sending unsent packets from previous sessions. There can be multiple of these
     * threads, one for each session. Thread stops if sending of packet fails.
     *
     * @return thread for sending unsent packets from previous sessions
     */
    Thread previousUnsentPacketSenderFactory(final String folderNew, final String sessionNameNew) {
        return new Thread(new Runnable() {
            public void run() {
                Sender sender = new Sender(sessionNameNew, folderNew, isAbsolute, serverAddress, uiHandler.logTextView, true);
                Storage storage = new Storage(sender, folderNew, isAbsolute, sessionNameNew);
                sender.sendLog(Constants.msg.get("PREV_SESSION_START") + sessionNameNew);
                while (storage.packetsUnsent()) {
                    String[] packetData = storage.getNextUnsentPacket();
                    if (packetData[0] != null) {
                        if (sender.sendRecording(packetData[0])) {
                            storage.removeUnsentPacket(packetData[1]);
                        } else {
                            sender.saveLog(Constants.msg.get("PREV_SESSION_DISCONNECT") + sessionNameNew);
                            return;
                        }
                    }
                }
                sender.sendLog(Constants.msg.get("PREV_SESSION_FINISH") + sessionNameNew);
                sender.sendGoodbye();
            }
        });
    }


    /**
     * Broadcast receiver used for Bluetooth recording. Streaming thread runs within it, so that
     * it streaming thread can record Bluetooth audio.
     *
     * @see #streamThreadFactory()
     */
    public BroadcastReceiver mBluetoothScoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1);
            streamThread = streamThreadFactory();
            if (AudioManager.SCO_AUDIO_STATE_CONNECTED == state) {
                streamThread.start();
            }
        }
    };

    /**
     * Initiates recording by Bluetooth detection and starting a streaming thread. If Bluetooth
     * recording is not selected, streaming thread starts right up. If Bluetooth is chosen and
     * Bluetooth device is connected, streaming thread is started within Bluetooth broadcast
     * receiver.
     *
     * @return true if streaming has started, false if there was an error
     * @see #mBluetoothScoReceiver
     */
    public boolean startRecording() {
        // detect bluetooth headset
        if (!functions.arePermissionsGranted(getApplicationContext())) return false;
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        isHeadset = (bluetoothAdapter != null && BluetoothProfile.STATE_CONNECTED == bluetoothAdapter.getProfileConnectionState(BluetoothProfile.HEADSET));
        if (isHeadset && !uiHandler.internalRadio.isChecked()) {
            IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED);
            registerReceiver(mBluetoothScoReceiver, intentFilter);
            audioManager = (AudioManager) getApplicationContext().getSystemService(getApplicationContext().AUDIO_SERVICE);
            audioManager.setMode(audioManager.MODE_NORMAL);
            audioManager.setBluetoothScoOn(true);
            audioManager.startBluetoothSco();
        } else if (uiHandler.internalRadio.isChecked()) {
            this.streamThread = streamThreadFactory();
            this.streamThread.start();
        } else return false;
        return true;
    }

    /**
     * Stop recording and resets recording variables. Disconnects from Bluetooth device.
     */
    private void stopRecording() {
        status = false;
        online = false;
        lastSavedPackage = -1;
        invalidateOptionsMenu();
        hideNotification();
        if (recorder != null) {
            try {
                recorder.stop();
            } catch (IllegalStateException e) { /* Recorder was not initialized */ }
            recorder.release();
        }
        if (isHeadset && !uiHandler.internalRadio.isChecked()) {
            unregisterReceiver(mBluetoothScoReceiver);
            audioManager.stopBluetoothSco();
            audioManager.setMode(audioManager.MODE_NORMAL);
            audioManager.setBluetoothScoOn(false);
        }
    }

    /**
     * Takes values from fields within the app and sets corresponding variables.
     * Upon every start of recording, all settings must be updated from fields within the app,
     * validation is expected beforehand.
     *
     * @return true if combination of settings is valid, false if not
     */
    private boolean setupFields() {
        serverAddress = uiHandler.serverAddressField.getText().toString();
        deviceName = uiHandler.deviceNameField.getText().toString().replaceAll("\\s+", "");
        bufferSize = Float.parseFloat(uiHandler.bufferSizeField.getText().toString()) * Constants.BYTES_IN_MB;
        sampleRate = Integer.parseInt(uiHandler.sampleRateSelect.getSelectedItem().toString());
        if (uiHandler.monoRadio.isChecked()) channelConfig = AudioFormat.CHANNEL_IN_MONO;
        else channelConfig = AudioFormat.CHANNEL_IN_STEREO;
        minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
        if (minBufferSize < 0) {
            Toast.makeText(getApplicationContext(), Constants.msg.get("AUDIO_ERR"), Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }


    /**
     * Creates thread for audio level meter widget. Thread records audio and periodically updates
     * the widget with new values. This thread is run when recording is not on. It is interrupted
     * upon recording start, as streaming thread does audio level meter widget management and also
     * audio sending. This thread does only audio level meter widget management.
     *
     * @return Thread for recording audio just for the purpose of updating its level widget
     */
    private Thread levelMeterThreadFactory() {
        return new Thread(new Runnable() {
            public void run() {
                if (!functions.arePermissionsGranted(getApplicationContext(), Manifest.permission.RECORD_AUDIO)) return;
                minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
                final byte[] recordBuffer = new byte[minBufferSize];
                recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, minBufferSize);
                if (recorder.getState() != AudioRecord.STATE_INITIALIZED) {
                    MainActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(MainActivity.this, Constants.msg.get("RECORDER_INIT_FAIL"), Toast.LENGTH_SHORT).show();
                        }
                    });
                    return;
                }
                recorder.startRecording();
                int bytesRead;
                while (true) {
                    bytesRead = recorder.read(recordBuffer, 0, minBufferSize);
                    if (bytesRead > 0)
                        uiHandler.audioLevel.setProgress((int) functions.getMaxAmplitude(recordBuffer));
                    if (Thread.currentThread().isInterrupted()) {
                        recorder.stop();
                        recorder.release();
                        break;
                    }
                }
            }
        });
    }

    /**
     * Changes main saving folder setting. Folder must exist.
     *
     * @param absolute is the path absolute
     * @return true if setting was successful, false if directory does not exist
     */
    public boolean setFolder(String path, boolean absolute) {
        File dir;
        if (absolute) dir = new File(path);
        else dir = new File(Environment.getExternalStorageDirectory(), path);
        if (dir.exists() && dir.isDirectory()) {
            isAbsolute = absolute;
            uiHandler.dirChooser.setText("Directory: " + path);
            folder = path;
            sender.setFolder(folder, absolute);
            storage.setFolder(folder, absolute);
            SharedPreferences.Editor edit = config.edit();
            edit.putString("folder", folder);
            edit.putString("isAbsolute", String.valueOf(isAbsolute));
            edit.apply();
            sendPreviouslyUnsent();
            return true;
        }
        return false;
    }

    /**
     * Offline composes full audio record from its fragments.
     * When offline recording, audio is saved in fragments and server is meant to compose them.
     * This method composes audio on device without the need of server.
     *
     * @param directory Directory containing the fragments.
     *                  It must contain at least 1 numbered file and it must end with recordings
     *                  folder.
     * @param absolute Absoluteness of directory path
     * @return true if combination of settings is valid, false if not
     */
    private String composeRecording(String directory, Boolean absolute) {
        if (!directory.endsWith("recordings")) return Constants.msg.get("PATH_ERR");
        Storage storage = new Storage();
        File[] files = storage.getAllFilesFromDirectory(directory, absolute);
        if (files.length == 0) return Constants.msg.get("NO_FILES") + directory;
        String completeFile = storage.appendFiles(files);
        try {
            storage.createDir(directory + "/composed", isAbsolute);
            storage.writeToFile(directory + "/composed/recording.raw", isAbsolute, completeFile);
        } catch (Exception e) {
            return Constants.msg.get("COMPOSE_ERR");
        }
        return Constants.msg.get("COMPOSE_SUCCESS") + directory + "/composed";
    }

    /**
     * Manual path input for directory choosing.
     * Requires configuring of action upon success.
     *
     * @param input input textfield
     * @return Alert dialog
     */
    private AlertDialog.Builder chooseDirDialog(EditText input) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(Constants.msg.get("FILE_MANAGER_ERR"));
        alert.setMessage(Constants.msg.get("MANUAL_DIR"));
        alert.setView(input);
        input.setText(folder);
        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {}
        });
        return alert;
    }

    /**
     * Handler for actions with bound callbacks, mainly directory choosers.
     *
     * @param requestCode code identifying operation performed
     * @param resultCode code identifying result, unused
     * @param data result returned from the action, usually file path
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 9999:
                if (data != null) {
                    String path = "/" + data.getData().getPath().split(":", 2)[1];
                    setFolder(path, false);
                    Toast.makeText(getApplicationContext(), Constants.msg.get("FOLDER_SET"), Toast.LENGTH_SHORT).show();
                }
                break;
            case 1111:
                if (data != null) {
                    String path = data.getData().getPath().split(":", 2)[1];
                    String composeResult = composeRecording(path, false);
                    new AlertDialog.Builder(this).setTitle("Composition results")
                            .setMessage(composeResult)
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                }
        }
    }

    /**
     * Override to display a menu.
     * @param menu application upper menu
     * @return true, always display menu
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        if (online) menu.add(0, 0, 0, "Online").setIcon(R.drawable.online).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        else menu.add(0, 0, 0, "Offline").setIcon(R.drawable.offline).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        return true;
    }

    /**
     * Thread for sending goodbye from menu.
     *
     * @return Thread for goodbye
     */
    public Thread goodbyeThreadFactory() {
            return new Thread(new Runnable() {
                @Override
                public void run() {
                    sender.sendGoodbye();
                }
        });
    }

    /**
     * Menu actions handler.
     * Carries out actions based on menu buttons pressed.
     *
     * @param item menu item selected
     * @return true, no menu button is a wrong choice
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.compose_recording:
                Intent ii = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                ii.addCategory(Intent.CATEGORY_DEFAULT);
                PackageManager packageManager = getApplicationContext().getPackageManager();
                List<ResolveInfo> list = packageManager.queryIntentActivities(ii, PackageManager.MATCH_DEFAULT_ONLY);
                if (!list.isEmpty())
                    startActivityForResult(Intent.createChooser(ii, "Choose directory"), 1111);
                else {
                    final EditText input = new EditText(this);
                    final Context context = this;
                    AlertDialog.Builder alert = chooseDirDialog(input);
                    alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            String composeResult = composeRecording(input.getText().toString(), true);
                            new AlertDialog.Builder(context).setTitle("Composition results")
                                    .setMessage(composeResult)
                                    .setPositiveButton(android.R.string.ok, null)
                                    .show();
                        }
                    });
                    alert.show();
                }
                break;
            case R.id.send_log:
                sendMessage();
                break;
            case R.id.set_folder:
                AlertDialog.Builder alert;
                final EditText input_folder = new EditText(this);
                alert = chooseDirDialog(input_folder);
                alert.setTitle(Constants.msg.get("FILE_MANAGER_TITLE"));
                alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    String value = input_folder.getText().toString();
                    if (!setFolder(value, true)) Toast.makeText(getApplicationContext(), Constants.msg.get("FOLDER_NOT_EXIST"), Toast.LENGTH_SHORT).show();
                    else Toast.makeText(getApplicationContext(), Constants.msg.get("FOLDER_SET"), Toast.LENGTH_SHORT).show();
                    }
                });
                alert.show();
                break;
            case R.id.about:
                View messageView = getLayoutInflater().inflate(R.layout.about, null, false);
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.app_name);
                builder.setView(messageView);
                builder.create();
                builder.show();
                break;
            case R.id.exit:
                if (status) {
                    goodbyeThreadFactory().run();
                    stopRecording();
                }
                onDestroy();
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(1);
                break;
            default:
                break;
        }

        return true;
    }

    /**
     * Method called on app startup.
     *
     * @param icicle app bundle
     */
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        notifications = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        functions = new Functions();
        config = this.getSharedPreferences(Constants.CONF, MODE_PRIVATE);
        sampleRate = Constants.START_SAMPLE_RATE;
        channelConfig = AudioFormat.CHANNEL_IN_MONO;
        audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        bufferSize = Constants.BYTES_IN_MB;

        // config file detection
        if (config.contains("serverAddress")) serverAddress = config.getString("serverAddress", "");
        else serverAddress = Constants.START_SERVER_ADDR;
        if (config.contains("folder")) folder = config.getString("folder", "");
        else folder = Constants.START_FOLDER;
        if (config.contains("isAbsolute")) isAbsolute = Boolean.parseBoolean(config.getString("isAbsolute", "false"));
        else isAbsolute = false;
        if (config.contains("deviceName")) deviceName = config.getString("deviceName", "");
        else deviceName = android.os.Build.MODEL;


        final SharedPreferences.Editor edit = config.edit();
        uiHandler = new UIHandler(getWindow().getDecorView().getRootView());
        uiHandler.dirChooser.setText("Directory: " + folder);
        uiHandler.logTextView.setMovementMethod(new ScrollingMovementMethod());
        uiHandler.serverAddressField.setText(serverAddress);
        uiHandler.serverAddressField.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void afterTextChanged(Editable s) {
                edit.putString("serverAddress", String.valueOf(s));
                edit.apply();
                sender.serverAddress = String.valueOf(s);
                storage.sender.serverAddress = String.valueOf(s);
            }
        });
        uiHandler.deviceNameField.setText(deviceName);
        uiHandler.deviceNameField.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void afterTextChanged(Editable s) {
                edit.putString("deviceName", String.valueOf(s));
                edit.apply();
            }
        });
        uiHandler.audioLevel.setMax(Constants.MAX_LEVELMETER_LEVEL);
        uiHandler.bufferSizeField.setText("1");
        uiHandler.monoRadio.toggle();
        uiHandler.internalRadio.toggle();
        uiHandler.setupSampleRates(sampleRate, this);

        sessionName = functions.getSessionName(deviceName);
        sender = new Sender(sessionName, folder, isAbsolute, serverAddress, uiHandler.logTextView, false);
        storage = new Storage(sender, folder, isAbsolute, sessionName);
        connectivity = new Connectivity(getApplicationContext());

        functions.askPermissions(this);
        sendPreviouslyUnsent();

        levelMeterThread = levelMeterThreadFactory();
        levelMeterThread.start();
        final MainActivity context = this;

        uiHandler.recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (status) {
                    stopRecording();
                    while (streamThread != null && streamThread.isAlive());
                    levelMeterThread = levelMeterThreadFactory();
                    levelMeterThread.start();
                    Drawable play = getResources().getDrawable(R.drawable.play);
                    uiHandler.recordButton.setBackground(play);
                    Toast.makeText(getApplicationContext(), "Recording stopped", Toast.LENGTH_LONG).show();
                } else {
                    if (uiHandler.validateFields() && setupFields()) {
                        levelMeterThread.interrupt();
                        while (levelMeterThread != null && levelMeterThread.isAlive());
                        if (startRecording()) {
                            status = true;
                            Drawable stop = getResources().getDrawable(R.drawable.stop);
                            uiHandler.recordButton.setBackground(stop);
                            Toast.makeText(getApplicationContext(), "Recording started", Toast.LENGTH_LONG).show();
                        } else {
                            if (!functions.arePermissionsGranted(context)) functions.permissionAlert(context, MainActivity.this);
                            else Toast.makeText(getApplicationContext(), "External mic not detected", Toast.LENGTH_LONG).show();
                            levelMeterThread = levelMeterThreadFactory();
                            levelMeterThread.start();
                        }
                    }
                }
            }
        });
        uiHandler.dirChooser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent ii = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                ii.addCategory(Intent.CATEGORY_DEFAULT);
                PackageManager packageManager = getApplicationContext().getPackageManager();
                List<ResolveInfo> list = packageManager.queryIntentActivities(ii, PackageManager.MATCH_DEFAULT_ONLY);
                if (!list.isEmpty())
                    startActivityForResult(Intent.createChooser(ii, "Choose directory"), 9999);
                else {
                    final EditText input = new EditText(context);
                    AlertDialog.Builder alert = chooseDirDialog(input);
                    alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            String value = input.getText().toString();
                            if (!setFolder(value, true)) Toast.makeText(getApplicationContext(), Constants.msg.get("FOLDER_NOT_EXIST"), Toast.LENGTH_SHORT).show();
                            else Toast.makeText(getApplicationContext(), Constants.msg.get("FOLDER_SET"), Toast.LENGTH_SHORT).show();
                        }
                    });
                    alert.show();
                }
            }
        });

        final GestureDetector gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            public boolean onDoubleTap(MotionEvent e) {
                sendMessage();
                return true;
            }
        });
        uiHandler.logTextView.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return gestureDetector.onTouchEvent(event);
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        hideNotification();
        // in case of no recording
        if (levelMeterThread != null && levelMeterThread.isAlive()) levelMeterThread.interrupt();
        // in case of recording
        status = false;
    }

    /**
     * Sends unsent audio fragments from previous sessions.
     * Essentially, client to server synchronization happening when the saving folder changes.
     */
    private void sendPreviouslyUnsent() {
        // do not try to send while offline
        if (!connectivity.isOnline()) return;
        File[] sessions = storage.getAllDirectoriesFromDirectory(folder);
        if (sessions == null) return;
        for (File session : sessions) {
            // if recording is in progress, do not touch recorded session folder
            if (streamThread != null && streamThread.isAlive()) {
                if (session.getName() == sessionName) continue;
            }
            File[] unsentPackets = storage.getAllFilesFromDirectory(folder + "/" + session.getName()+ "/recordings", isAbsolute);
            if (unsentPackets != null) {
                Storage storage = new Storage(sender, folder, isAbsolute, session.getName());
                if (storage.packetsUnsent() && connectivity.isOnline()) {
                    Thread sender = previousUnsentPacketSenderFactory(folder, session.getName());
                    sender.start();
                }
            }
        }
    }

    /**
     * Shows "currently recording" notification.
     */
    private void showNotification() {
        NotificationChannel channel;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            channel = new NotificationChannel("default", "default", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("default");
            notifications.createNotificationChannel(channel);
        }

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getApplicationContext(), "default")
            .setContentTitle("Recording")
            .setOngoing(true)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now);
        if (online) mBuilder.setContentText("Ongoing recording, status online");
        else mBuilder.setContentText("Ongoing recording, status offline");
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent i = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(i);
        Notification notification = mBuilder.build();
        notification.flags = Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;
        notifications.notify(0, notification);
    }

    /**
     * Hides "currently recording" notification.
     */
    private void hideNotification() {
        notifications.cancelAll();
    }


    /**
     * Pops up input window for sending messages during recording.
     */
    private void sendMessage() {
        if (streamThread != null && streamThread.isAlive()) {
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            final SharedPreferences.Editor edit = config.edit();
            final EditText input_log = new EditText(this);
            input_log.setText(config.getString("message", ""));
            input_log.setSelection(input_log.getText().length());
            alert.setTitle(Constants.msg.get("SEND_MESSAGE"));
            alert.setView(input_log);
            alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {}
            });
            alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    edit.putString("message", String.valueOf(input_log.getText().toString()));
                    edit.apply();
                    String log = "Message: " + input_log.getText().toString();
                    addLogSender(log);
                    Toast.makeText(getApplicationContext(), Constants.msg.get("MESSAGE_SENT"), Toast.LENGTH_SHORT).show();
                }
            });
            AlertDialog dialog = alert.create();
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
            dialog.show();
        } else Toast.makeText(getApplicationContext(), Constants.msg.get("LOG_SEND_START_ERROR"), Toast.LENGTH_SHORT).show();
    }
}