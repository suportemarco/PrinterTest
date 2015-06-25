package br.com.bematech;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.starmicronics.stario.PortInfo;
import com.starmicronics.stario.StarIOPort;
import com.starmicronics.stario.StarIOPortException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.view.WindowManager.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

public class StarIOSDKMobilePrinterLineModeActivity extends Activity {

	private Context me = this;
	private String strInterface = "";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		setContentView(R.layout.main);

		EditText portNameField = (EditText) findViewById(R.id.editText_PortName);
		SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
		portNameField.setText(pref.getString("portName", "BT:Star Micronics"));

		InitializeComponent();
		// setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	}

	public void Help(View view) {
		if (!checkClick.isClickEvent())
			return;
		Intent myIntent = new Intent(this, helpActivity.class);
		startActivityFromChild(this, myIntent, 0);
	}

	public void GetStatus(View view) {
		if (!checkClick.isClickEvent())
			return;
		EditText portNameField = (EditText) findViewById(R.id.editText_PortName);
		String portName = portNameField.getText().toString();
		String portSettings = getPortSettingsOption(portName);

		// The portable printer and non portable printer have the same
		PrinterFunctions.CheckStatus(this, portName, portSettings);
	}

	public void GetFirmwareInfo(View view) {
		if (!checkClick.isClickEvent())
			return;
		EditText portNameField = (EditText) findViewById(R.id.editText_PortName);
		String portName = portNameField.getText().toString();
		String portSettings = getPortSettingsOption(portName);

		// The portable printer and non portable printer have the same
		PrinterFunctions.CheckFirmwareVersion(this, portName, portSettings);
	}

	public void ShowBarcode(View view) {
		if (!checkClick.isClickEvent())
			return;
		EditText portNameField = (EditText) findViewById(R.id.editText_PortName);
		PrinterTypeActivity.setPortName(portNameField.getText().toString());
		PrinterTypeActivity.setPortSettings(getPortSettingsOption(PrinterTypeActivity.getPortName()));

		Intent myIntent = new Intent(this, barcodeselector.class);
		startActivityFromChild(this, myIntent, 0);

	}

	public void ShowBarcode2d(View view) {
		if (!checkClick.isClickEvent())
			return;
		EditText portNameField = (EditText) findViewById(R.id.editText_PortName);
		PrinterTypeActivity.setPortName(portNameField.getText().toString());
		PrinterTypeActivity.setPortSettings(getPortSettingsOption(PrinterTypeActivity.getPortName()));

		Intent myIntent = new Intent(this, barcodeselector2d.class);
		startActivityFromChild(this, myIntent, 0);
	}

	public void ShowTextFormating(View view) {
		if (!checkClick.isClickEvent())
			return;
		EditText portNameField = (EditText) findViewById(R.id.editText_PortName);
		PrinterTypeActivity.setPortName(portNameField.getText().toString());
		PrinterTypeActivity.setPortSettings(getPortSettingsOption(PrinterTypeActivity.getPortName()));

		Intent myIntent = new Intent(this, textFormattingSelectLanguageActivity.class);
		myIntent.putExtra("TEXTFORMATTING", 1);
		startActivityFromChild(this, myIntent, 0);

	}

	public void ShowImagePrinting(View view) {
		if (!checkClick.isClickEvent())
			return;
		EditText portNameField = (EditText) findViewById(R.id.editText_PortName);
		PrinterTypeActivity.setPortName(portNameField.getText().toString());

		PrinterTypeActivity.setPortSettings(getPortSettingsOption(PrinterTypeActivity.getPortName()));

		Intent myIntent = new Intent(this, imagePrintingActivity.class);
		startActivityFromChild(this, myIntent, 0);
	}

	public void MCR(View view) {
		if (!checkClick.isClickEvent())
			return;
		EditText portNameField = (EditText) findViewById(R.id.editText_PortName);
		PrinterTypeActivity.setPortName(portNameField.getText().toString());
		PrinterTypeActivity.setPortSettings(getPortSettingsOption(PrinterTypeActivity.getPortName()));

		PrinterFunctions.MCRStart(this, PrinterTypeActivity.getPortName(), PrinterTypeActivity.getPortSettings());
	}

	public void SampleReceipt(View view) {
		if (!checkClick.isClickEvent())
			return;

		EditText portNameField = (EditText) findViewById(R.id.editText_PortName);
		PrinterTypeActivity.setPortName(portNameField.getText().toString());
		PrinterTypeActivity.setPortSettings(getPortSettingsOption(PrinterTypeActivity.getPortName()));

		Intent myIntent = new Intent(this, SampleReciptActivity.class);
		myIntent.putExtra("PRINTERTYPE", 1);
		startActivityFromChild(this, myIntent, 0);

	}

	public void PortDiscovery(View view) {
		if (!checkClick.isClickEvent())
			return;
		final String item_list[] = new String[] { "Bluetooth", "USB", "All", };

		strInterface = "Bluetooth";

		Builder portDiscoveryDialog = new Builder(this);
		portDiscoveryDialog.setIcon(android.R.drawable.checkbox_on_background);
		portDiscoveryDialog.setTitle("Port Discovery List");
		portDiscoveryDialog.setCancelable(false);
		portDiscoveryDialog.setSingleChoiceItems(item_list, 0, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				strInterface = item_list[whichButton];
			}
		});

		portDiscoveryDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
				((AlertDialog) dialog).getButton(DialogInterface.BUTTON_NEGATIVE).setEnabled(false);
				if (strInterface.equals("Bluetooth")) {
					getPortDiscovery("Bluetooth");
				} else if (strInterface.equals("USB")) {
					getPortDiscovery("USB");
				} else {
					getPortDiscovery("All");
				}

			}
		});
		portDiscoveryDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				//
			}
		});
		portDiscoveryDialog.show();
	}

	public void BluetoothSetting(View view) {
		if (!checkClick.isClickEvent())
			return;
		EditText portNameField = (EditText) findViewById(R.id.editText_PortName);

		// PrinterFunctions.BluetoothSetting(this);

		String portName = portNameField.getText().toString();
		String portSettings = getPortSettingsOption(portName);

		// Check bluetooth interface
		if (!portName.startsWith("BT:")) {
			new Builder(me).setTitle(getString(R.string.error)).setMessage(getString(R.string.bluetooth_interface_only)).setNegativeButton("OK", null).show();
		} else {
			SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
			Editor editor = pref.edit();
			editor.putString("bluetoothSettingPortName", portName);
			editor.putString("bluetoothSettingPortSettings", portSettings);
			editor.putString("bluetoothSettingDeviceType", "PortablePrinter");
			editor.commit();

			Intent myIntent = new Intent(this, BluetoothSettingActivity.class);
			startActivityFromChild(this, myIntent, 0);
		}
	}

	public void USBSetting(View view) {
		if (!checkClick.isClickEvent())
			return;
		EditText portNameField = (EditText) findViewById(R.id.editText_PortName);

		// PrinterFunctions.BluetoothSetting(this);

		String portName = portNameField.getText().toString();
		String portSettings = getPortSettingsOption(portName);

		// Check bluetooth interface
		if (!portName.startsWith("USB:")) {
			new Builder(me).setTitle(getString(R.string.error)).setMessage(getString(R.string.usb_interface_only)).setNegativeButton("OK", null).show();
		} else {
			SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
			Editor editor = pref.edit();
			editor.putString("usbSettingPortName", portName);
			editor.putString("usbSettingPortSettings", portSettings);
			editor.commit();

			Intent myIntent = new Intent(this, UsbSettingActivity.class);
			startActivityFromChild(this, myIntent, 0);
		}
	}
	
	private void InitializeComponent() { // delete view of some function button for POS Printer Line mode
		TextView titleText = (TextView) findViewById(R.id.textView1);
		titleText.setText("Star Micronics Portable Printer Samples");
		
		Spinner spinner_bluetooth_connectRetry_type = (Spinner) findViewById(R.id.spinner_bluetooth_connectRetry_type);
		ArrayAdapter<String> ad_bluetooth_connectRetry_type = new ArrayAdapter<String>(this, R.layout.spinner, new String[] { "OFF", "ON" });
		spinner_bluetooth_connectRetry_type.setAdapter(ad_bluetooth_connectRetry_type);
		ad_bluetooth_connectRetry_type.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		
		Spinner spinner_tcp_port_number = (Spinner) findViewById(R.id.spinner_tcp_port_number);
		spinner_tcp_port_number.setVisibility(View.INVISIBLE);
	
        Spinner spinner_bluetooth_communication_type = (Spinner)findViewById(R.id.spinner_bluetooth_communication_type);
//        ArrayAdapter<String> ad_bluetooth_communication_type = new ArrayAdapter<String>(this, R.layout.spinner, new String[] {"POS", "Portable"});
//        spinner_bluetooth_communication_type.setAdapter(ad_bluetooth_communication_type);
//        ad_bluetooth_communication_type.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_bluetooth_communication_type.setVisibility(View.INVISIBLE);	//TODO:
		
		Spinner spinner_sensor_active = (Spinner) findViewById(R.id.spinner_SensorActive);
		spinner_sensor_active.setVisibility(View.GONE);

		Button openCashDrawer1Button = (Button) findViewById(R.id.button_OpenCashDrawer);
		openCashDrawer1Button.setVisibility(View.GONE);

		Button openCashDrawer2Button = (Button) findViewById(R.id.button_OpenCashDrawer2);
		openCashDrawer2Button.setVisibility(View.GONE);

		Button cutButton = (Button) findViewById(R.id.button_Cut);
		cutButton.setVisibility(View.GONE);

		Button rasterPrintingButton = (Button) findViewById(R.id.button_RasterPrinting);
		rasterPrintingButton.setVisibility(View.GONE);

		Button imagePrintingButton = (Button) findViewById(R.id.button_ImagePrinting);
		imagePrintingButton.setVisibility(View.GONE);

		Button textFormatingJPButton = (Button) findViewById(R.id.button_KanjiTextFormating);
		textFormatingJPButton.setVisibility(View.GONE);

		TextView portNumberText = (TextView) findViewById(R.id.textView2);
		portNumberText.setVisibility(View.GONE);

		TextView bluetoothText = (TextView) findViewById(R.id.textView3);
		bluetoothText.setVisibility(View.INVISIBLE);

		TextView sensorActiveText = (TextView) findViewById(R.id.textView4);
		sensorActiveText.setVisibility(View.INVISIBLE);
	}

	private String getBluetoothRetrySettings() {
		String retrySetting = "";

		Spinner spinner_bluetooth_connectRetry_type = (Spinner) findViewById(R.id.spinner_bluetooth_connectRetry_type);
		switch (spinner_bluetooth_connectRetry_type.getSelectedItemPosition()) {
		case 0:
			retrySetting = "";
			break;
		case 1:
			retrySetting = ";l";
			break;
		}

		return retrySetting;
	}

	private String getTCPPortSettings() {
		String portSettings = "";

		Spinner spinner_tcp_port_number = (Spinner) findViewById(R.id.spinner_tcp_port_number);
		switch (spinner_tcp_port_number.getSelectedItemPosition()) {
		case 0:
			portSettings = "";
			break;
		case 1:
			portSettings = ";9100";
			break;
		case 2:
			portSettings = ";9101";
			break;
		case 3:
			portSettings = ";9102";
			break;
		case 4:
			portSettings = ";9103";
			break;
		case 5:
			portSettings = ";9104";
			break;
		case 6:
			portSettings = ";9105";
			break;
		case 7:
			portSettings = ";9106";
			break;
		case 8:
			portSettings = ";9107";
			break;
		case 9:
			portSettings = ";9108";
			break;
		case 10:
			portSettings = ";9109";
			break;
		}

		return portSettings;
	}

	//TODO:動作確認後、削除
//	private String getBluetoothCommunicationType() {
//		String portSettings = "";
//
//		Spinner spinner_bluetooth_communication_type = (Spinner) findViewById(R.id.spinner_bluetooth_communication_type);
//		switch (spinner_bluetooth_communication_type.getSelectedItemPosition()) {
//		case 0:
//			portSettings = "";
//			break;
//		case 1:
//			portSettings = "portable";	// ToDo
//			break;
//		}
//
//		return portSettings;
//	}

	private String getPortSettingsOption(String portName) {
		//TODO:動作確認後、コメントアウト解除
//		String portSettings = getBluetoothCommunicationType(); // 評価用実装
		String portSettings = "portable";

		if (portName.toUpperCase(Locale.US).startsWith("TCP:")) {
			portSettings += getTCPPortSettings();
		} else if (portName.toUpperCase(Locale.US).startsWith("BT:")) {
			portSettings += getBluetoothRetrySettings();
		}

		return portSettings;
	}

	private void getPortDiscovery(String interfaceName) {
		List<PortInfo> BTPortList;
		List<PortInfo> USBPortList;
		final EditText editPortName;

		final ArrayList<PortInfo> arrayDiscovery;
		ArrayList<String> arrayPortName;

		arrayDiscovery = new ArrayList<PortInfo>();
		arrayPortName = new ArrayList<String>();

		try {
			if (true == interfaceName.equals("Bluetooth") || true == interfaceName.equals("All")) {
				BTPortList = StarIOPort.searchPrinter("BT:");

				for (PortInfo portInfo : BTPortList) {
					arrayDiscovery.add(portInfo);
				}
			}
			if (true == interfaceName.equals("USB") || true == interfaceName.equals("All")) {
				USBPortList = StarIOPort.searchPrinter("USB:", this);

				for (PortInfo portInfo : USBPortList) {
					arrayDiscovery.add(portInfo);
				}
			}

			arrayPortName = new ArrayList<String>();

			for (PortInfo discovery : arrayDiscovery) {
				String portName;

				portName = discovery.getPortName();

				if (discovery.getMacAddress().equals("") == false) {
					portName += "\n - " + discovery.getMacAddress();
					if (discovery.getModelName().equals("") == false) {
						portName += "\n - " + discovery.getModelName();
					}
				} else if (interfaceName.equals("USB") || interfaceName.equals("All")) {
					if (!discovery.getModelName().equals("")) {
						portName += "\n - " + discovery.getModelName();
					}
					if (!discovery.getUSBSerialNumber().equals(" SN:")) {
						portName += "\n - " + discovery.getUSBSerialNumber();
					}
				}

				arrayPortName.add(portName);
			}
		} catch (StarIOPortException e) {
			e.printStackTrace();
		}

		editPortName = new EditText(this);

		new Builder(this).setIcon(android.R.drawable.checkbox_on_background).setTitle("Please Select IP Address or Input Port Name").setCancelable(false).setView(editPortName).setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int button) {
				((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
				((AlertDialog) dialog).getButton(DialogInterface.BUTTON_NEGATIVE).setEnabled(false);
				EditText portNameField = (EditText) findViewById(R.id.editText_PortName);
				portNameField.setText(editPortName.getText());
				SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
				Editor editor = pref.edit();
				editor.putString("portName", portNameField.getText().toString());
				editor.commit();
			}
		}).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int button) {
			}
		}).setItems(arrayPortName.toArray(new String[0]), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int select) {
				EditText portNameField = (EditText) findViewById(R.id.editText_PortName);
				portNameField.setText(arrayDiscovery.get(select).getPortName());

				SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
				Editor editor = pref.edit();
				editor.putString("portName", portNameField.getText().toString());
				editor.commit();
			}
		}).show();
	}
}