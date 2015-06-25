package br.com.bematech;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.View;
import android.widget.EditText;

public class UsbSettingActivity extends Activity {
	
	private String portName;
	private String portSettings;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.usbsetting);
		
		SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
		portName = pref.getString("usbSettingPortName", "BT:Star Micronics");
		portSettings = pref.getString("usbSettingPortSettings", "");

		final EditText editText = (EditText) findViewById(R.id.editText_usbSerial);
		editText.setFilters(getEditTextFilters());
	}
	
	public void ApplyUsbSerialNumber(View view) {
		EditText editText_serialNumber = (EditText) findViewById(R.id.editText_usbSerial);
		String strSerialNumber = editText_serialNumber.getText().toString();
		
		if (strSerialNumber.length() < 1 || strSerialNumber.length() > 8) {
			new AlertDialog.Builder(this).setTitle(getString(R.string.error)).setMessage(getString(R.string.usb_interface_invalidDigits)).setNegativeButton("OK", null).show();
			return;
		}
		if (!strSerialNumber.matches("^[0-9A-Z]+$")) {
			new AlertDialog.Builder(this).setTitle(getString(R.string.error)).setMessage(getString(R.string.usb_interface_invalidCharacters)).setNegativeButton("OK", null).show();
			return;
		}
		
		PrinterFunctions.EnableUSBSerialNumber(this, portName, portSettings, strSerialNumber.getBytes());
		
		new AlertDialog.Builder(this).setTitle(getString(R.string.complete)).setMessage(getString(R.string.complete_description_usb)).setCancelable(false).setPositiveButton("OK", null).show();
		return;
	}
	
	public void ClearUsbSerialNumber(View view) {
		PrinterFunctions.DisableUSBSerialNumber(this, portName, portSettings);
		new AlertDialog.Builder(this).setTitle(getString(R.string.complete)).setMessage(getString(R.string.complete_description_usb)).setCancelable(false).setPositiveButton("OK", null).show();
	}
	
	// for editText of deviceName and iOSPortName
	private InputFilter[] getEditTextFilters() {
		// Input value limit
		InputFilter inputFilter = new InputFilter() {
			public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
				if (source.toString().matches("^[0-9A-Z]+$")) {
					return source.toString();
				}
				return "";
			}
		};

		InputFilter lengthFilter = new InputFilter.LengthFilter(8); // Limit the number of characters

		InputFilter[] filters = new InputFilter[] { inputFilter, lengthFilter };

		return filters;
	}
}