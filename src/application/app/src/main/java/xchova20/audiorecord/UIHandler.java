package xchova20.audiorecord;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.support.design.widget.TextInputLayout;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.Arrays;

/**
 * Encapsulate all UI elements.
 */
public class UIHandler {
    private final View view;
    public TextView dirChooser;
    public Button recordButton;
    public EditText serverAddressField;
    public EditText deviceNameField;
    public EditText bufferSizeField;
    public ProgressBar audioLevel;
    public Spinner sampleRateSelect;
    public RadioButton monoRadio;
    public RadioButton internalRadio;
    public TextView logTextView;

    /**
     * Instantiates UIHandler.
     * Assigns all attributes to actualy UI elements from a view.
     *
     * @param view application view with all UI elements
     */
    public UIHandler(View view) {
        this.view = view;
        this.dirChooser = view.findViewById(R.id.dirchooser);
        this.recordButton = view.findViewById(R.id.record);
        this.logTextView = view.findViewById(R.id.log_text_view);
        this.serverAddressField = view.findViewById(R.id.server_address);
        this.deviceNameField = view.findViewById(R.id.device_name);
        this.audioLevel = view.findViewById(R.id.audio_level);
        this.bufferSizeField = view.findViewById(R.id.buffer_size);
        this.sampleRateSelect = view.findViewById(R.id.sample_rate_select);
        this.monoRadio = view.findViewById(R.id.mono_radio);
        this.internalRadio = view.findViewById(R.id.internal_radio);
    }

    /**
     * Gets all sample rates supported by a device.
     *
     * @return list of supported sample rates
     */
    public String[] getSampleRates() {
        String[] rates = new String[] {"11025", "16000", "22050", "44100", "48000", "96000"};
        String[] possibleRates = new String[] {};
        for (String rate : rates) {
            int bufferSize = AudioRecord.getMinBufferSize(Integer.parseInt(rate), AudioFormat.CHANNEL_OUT_DEFAULT, AudioFormat.ENCODING_PCM_16BIT);
            if (bufferSize > 0) {
                possibleRates  = Arrays.copyOf(possibleRates, possibleRates.length + 1);
                possibleRates[possibleRates.length - 1] = rate;
            }
        }
        return possibleRates;
    }

    /**
     * Setup sample rate widget. Fills its options with supported sample rates.
     *
     * @param sampleRate sample rate selected by the user
     * @param context application context
     */
    public void setupSampleRates(int sampleRate, Context context) {
        String[] sampleRates = getSampleRates();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, R.layout.custom_spinner, sampleRates);
        sampleRateSelect.setAdapter(adapter);
        sampleRateSelect.setBackground(ContextCompat.getDrawable(context, R.drawable.abc_edit_text_material));
        for (int i=0; i<sampleRates.length; i++) {
            if (sampleRates[i].equals(String.valueOf(sampleRate))) {
                sampleRateSelect.setSelection(i);
                break;
            }
        }
    }

    /**
     * Validates values in all application fields.
     *
     * @return true if all fields have valid input, false if not
     */
    public boolean validateFields() {
        boolean fieldState = true;
        TextInputLayout serverAddressFieldLayout =  view.findViewById(R.id.server_address_layout);
        TextInputLayout deviceNameFieldLayout =  view.findViewById(R.id.device_name_layout);
        TextInputLayout bufferSizeFieldLayout =  view.findViewById(R.id.buffer_size_layout);

        if(serverAddressField.getText().toString().trim().equals("")) {
            serverAddressFieldLayout.setError(Constants.msg.get("SERVER_ADDR_ERR"));
            fieldState = false;
        } else serverAddressFieldLayout.setError(null);

        if(deviceNameField.getText().toString().trim().equals("")) {
            deviceNameFieldLayout.setError(Constants.msg.get("DEVICE_NAME_ERR"));
            fieldState = false;
        } else  deviceNameFieldLayout.setError(null);

        if(bufferSizeField.getText().toString().trim().equals("")) {
            bufferSizeFieldLayout.setError(Constants.msg.get("BUFFER_SIZE_ERR"));
            fieldState = false;
        } else bufferSizeFieldLayout.setError(null);
        return fieldState;
    }
}
