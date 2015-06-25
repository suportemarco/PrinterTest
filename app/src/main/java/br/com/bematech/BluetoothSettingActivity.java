package br.com.bematech;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.text.InputFilter;
import android.text.InputType;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.text.Spanned;
import android.util.Log;

import com.starmicronics.stario.StarBluetoothManager;
import com.starmicronics.stario.StarIOPort;
import com.starmicronics.stario.StarBluetoothManager.StarBluetoothSecurity;
import com.starmicronics.stario.StarBluetoothManager.StarDeviceType;
import com.starmicronics.stario.StarIOPortException;
import com.starmicronics.stario.StarBluetoothManager.StarBluetoothSettingCapability;

public class BluetoothSettingActivity extends Activity implements View.OnClickListener {
	BluetoothSettingActivity me = this;
	boolean changePinCode = false;
	int selectedIndex;

	ListView listView = null;
	Button button = null;

	List<Map<String, String>> listViewItems = new ArrayList<Map<String, String>>();
	SimpleAdapter adapter;

	private boolean fwVerCheckResult = false;
	private String portName = null;
	private String portSettings = null;
	private StarBluetoothManager printerManager = null;
	private String bluetoothDeviceName = null;
	private String iOSPortName = null;
	private boolean autoConnect = false;
	private StarBluetoothSecurity securityType = StarBluetoothSecurity.SSP;
	private static String pinCode = null;
	static final private int TIMEOUTMILLIS = 10000;

	private StarBluetoothSettingCapability deviceNameCapability = StarBluetoothSettingCapability.NOSUPPORT;
	private StarBluetoothSettingCapability iOSPortNameCapability = StarBluetoothSettingCapability.NOSUPPORT;
	private StarBluetoothSettingCapability autoConnectCapability = StarBluetoothSettingCapability.NOSUPPORT;
	private StarBluetoothSettingCapability securityTypeCapability = StarBluetoothSettingCapability.NOSUPPORT;
	private StarBluetoothSettingCapability pinCodeCapability = StarBluetoothSettingCapability.NOSUPPORT;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_setting);
		findViews();

		adapter = new SimpleAdapter(this, listViewItems, android.R.layout.simple_list_item_2, new String[] { "main", "sub" }, new int[] { android.R.id.text1, android.R.id.text2 });
		listView.setAdapter(adapter);
		button.setOnClickListener(this);
		refreshSettingList();

		SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
		this.portName = pref.getString("bluetoothSettingPortName", "BT:Star Micronics");
		this.portSettings = pref.getString("bluetoothSettingPortSettings", "");
		StarDeviceType deviceType = pref.getString("bluetoothSettingDeviceType", "DesktopPrinter").toUpperCase(Locale.US).equals("DESKTOPPRINTER") ? StarDeviceType.StarDeviceTypeDesktopPrinter : StarDeviceType.StarDeviceTypePortablePrinter;

		try {
			printerManager = new StarBluetoothManager(portName, portSettings, TIMEOUTMILLIS, deviceType);
		} catch (StarIOPortException e) {
			new Builder(me).setTitle(getString(R.string.error)).setMessage(e.getMessage()).setPositiveButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					finish(); // close Activity
				}
			}).setCancelable(false).show();

			return;
		}

		final ProgressDialog progressDialog = new ProgressDialog(me);
		progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		progressDialog.setCancelable(false);
		progressDialog.show();

		// Need to check PortSettings, because occured error in getPort method if the value is invalid portSettings.

		class FunctionViewOpenTask extends AsyncTask<Void, Void, Boolean> {
			private String errorMessage = "";

			@Override
			protected Boolean doInBackground(Void... params) {

				try {
					// This feature is supported from Ver3.0 or later (SM-S210i, SM-S220i, SM-T300i, SM-T400i).
					// Other models support from Ver1.0.
					fwVerCheckResult = checkFirmwareModelAndVersion();
					
					if (fwVerCheckResult) {
						loadSetting(); // Load Current BluetoothSetting
					}
				} catch (StarIOPortException e) {
					e.printStackTrace();
					errorMessage = e.getMessage();
					return false;
				}

				return true;
			}

			@Override
			protected void onPostExecute(Boolean resultCode) {

				progressDialog.dismiss();

				if (fwVerCheckResult == false) {
					new Builder(me).setTitle(getString(R.string.error)).setMessage(R.string.is_not_supported_by_firmware_message).setPositiveButton("OK", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							finish();
						}
					}).setCancelable(false).show();
				} 
				
				if (resultCode == true) {
					refreshSettingList();
				} else {
					String deviceType;

					if (printerManager.getDeviceType() == StarDeviceType.StarDeviceTypeDesktopPrinter) {
						deviceType = "StarDeviceTypeDesktopPrinter";
					} else {
						deviceType = "StarDeviceTypePortablePrinter";
					}

					errorMessage += "\n- Port Info - \n  portName : " + printerManager.getPortName() + "\n  portSettings : " + printerManager.getPortSettings() + "\n  portTimeoutMillis : " + printerManager.getTimeoutMillis() + "\n  StarDeviceType : " + deviceType;
					new Builder(me).setTitle(getString(R.string.error)).setMessage(getString(R.string.cannot_communicate_message) + " : " + errorMessage).setPositiveButton("OK", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							finish();
						}
					}).setCancelable(false).show();
				}
			}
		}

		new FunctionViewOpenTask().execute();

		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			// for editText of deviceName and iOSPortName
			private InputFilter[] getEditTextFilters() {
				// Input value limit
				InputFilter inputFilter = new InputFilter() {
					public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
						if (source.toString().matches("^[0-9a-zA-Z;:!?#$%&,.@_\\-= \\\\/\\*\\+~\\^\\[\\{\\(\\]\\}\\)\\|]+$")) {
							return source.toString();
						}
						return "";
					}
				};

				InputFilter lengthFilter = new InputFilter.LengthFilter(16); // Limit the number of characters

				InputFilter[] filters = new InputFilter[] { inputFilter, lengthFilter };

				return filters;
			}

			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

				switch (position) {
				case 0: // Device Name
				{
					if (deviceNameCapability != StarBluetoothSettingCapability.SUPPORT) {
						break;
					}
					// Create TextEdit to enter bluetooth device name.
					final EditText editText = new EditText(view.getContext());
					editText.setInputType(InputType.TYPE_CLASS_TEXT); // disable new line
					editText.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS); // For Android 4.2, Auto-completion bug fixes.
					editText.setText(bluetoothDeviceName);

					// set filter
					editText.setFilters(getEditTextFilters());

					// Display AlertDialog
					new Builder(view.getContext()).setTitle(getString(R.string.device_name)).setView(editText).setPositiveButton("OK", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							if (editText.getText().length() == 0) {
								return;
							}
							bluetoothDeviceName = editText.getText().toString();
							iOSPortName = editText.getText().toString();
							refreshSettingList();
						}
					}).setNegativeButton(getString(R.string.cancel), null).show();
					break;
				}
				case 1: // iOS Port Name
				{
					if (iOSPortNameCapability != StarBluetoothSettingCapability.SUPPORT) {
						break;
					}
					final EditText editText = new EditText(view.getContext());
					editText.setInputType(InputType.TYPE_CLASS_TEXT);
					editText.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS); // For Android 4.2, Auto-completion bug fixes.
					editText.setText(iOSPortName);

					// set filter
					editText.setFilters(getEditTextFilters());

					new Builder(view.getContext()).setTitle(getString(R.string.iOS_port_name)).setView(editText).setPositiveButton("OK", new DialogInterface.OnClickListener() {

						public void onClick(DialogInterface dialog, int which) {
							iOSPortName = editText.getText().toString();
							refreshSettingList();
						}
					}).setNegativeButton("Cancel", null).show();
					break;
				}
				case 2: // auto connect
				{
					if (autoConnectCapability != StarBluetoothSettingCapability.SUPPORT) {
						break;
					}
					//For Desktop Type Bluetooth Interface
					if (printerManager.getDeviceType() == StarDeviceType.StarDeviceTypeDesktopPrinter) {
						final String items[] = getResources().getStringArray(R.array.auto_connect_settings);
						selectedIndex = autoConnect ? 0 : 1;

						new Builder(view.getContext()).setTitle(getString(R.string.auto_connect)).setSingleChoiceItems(items, selectedIndex, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								selectedIndex = which;
							}
						}).setPositiveButton("OK", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								// autoConnect = (selectedIndex == 0) ? true : false;
								if (selectedIndex == 0) {
									// Check security type. When auto connect is ON, do not set PINCODE.
									if (securityType == StarBluetoothSecurity.PINCODE) {
										new Builder(me).setTitle(getString(R.string.Confirmation)).setMessage(getString(R.string.AutoConnectionAlert)).setPositiveButton(getString(R.string.AutoConnectionUseSSP), new DialogInterface.OnClickListener() {

											public void onClick(DialogInterface dialog, int which) {
												securityType = StarBluetoothSecurity.SSP;
												autoConnect = true;
												refreshSettingList();
											}
										}).setNegativeButton(getString(R.string.cancel), null).show();
									} else {
										autoConnect = true;
										refreshSettingList();
									}
								} else {
									autoConnect = false;
									refreshSettingList();
								}
							}

						}).setNegativeButton("Cancel", null).show();
					} else {
						final String items[] = getResources().getStringArray(R.array.auto_connect_settings);
						selectedIndex = autoConnect ? 0 : 1;

						new Builder(view.getContext()).setTitle(getString(R.string.auto_connect)).setSingleChoiceItems(items, selectedIndex, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								selectedIndex = which;
							}
						}).setPositiveButton("OK", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								autoConnect = (selectedIndex == 0) ? true : false;
								refreshSettingList();
							}
						}).setNegativeButton("Cancel", null).show();
					}
					break;
				}
				case 3: // Change Security type
				{
					if (securityTypeCapability != StarBluetoothSettingCapability.SUPPORT) {
						break;
					}
					//For Desktop Type Bluetooth Interface
					if (printerManager.getDeviceType() == StarDeviceType.StarDeviceTypeDesktopPrinter) {
						if (autoConnect == true) {
							new Builder(me).setTitle(getString(R.string.Confirmation)).setMessage(getString(R.string.SecuritySettingAlert)).setNegativeButton("OK", null).show();
							break;
						}

						final String items[] = getResources().getStringArray(R.array.desktop_security_settings);
						selectedIndex = (securityType == StarBluetoothSecurity.SSP) ? 0 : 1;

						new Builder(view.getContext()).setTitle(getString(R.string.security)).setSingleChoiceItems(items, selectedIndex, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								selectedIndex = which;
							}
						}).setPositiveButton("OK", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								securityType = (selectedIndex == 0) ? StarBluetoothSecurity.SSP : StarBluetoothSecurity.PINCODE;
								refreshSettingList();
							}
						}).setNegativeButton(getString(R.string.cancel), null).show();
					} else {
						//For Portable Type Bluetooth Interface
						final String items[] = getResources().getStringArray(R.array.portable_security_settings);
						selectedIndex = (securityType == StarBluetoothSecurity.DISABLE) ? 0 : 1;

						new Builder(view.getContext()).setTitle(getString(R.string.security)).setSingleChoiceItems(items, selectedIndex, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								selectedIndex = which;
							}
						}).setPositiveButton("OK", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								securityType = (selectedIndex == 0) ? StarBluetoothSecurity.DISABLE : StarBluetoothSecurity.PINCODE;
								refreshSettingList();
							}
						}).setNegativeButton(getString(R.string.cancel), null).show();
					}
					break;
				}
				case 4: // Change PIN Code
					if (pinCodeCapability != StarBluetoothSettingCapability.SUPPORT) {
						break;
					}
					Intent intent = new Intent(view.getContext(), PINCodeSettingActivity.class);
					startActivity(intent);
					break;
				case 5: // Help
					Intent intentHelp = new Intent(view.getContext(), BluetoothSettingHelpActivity.class);
					startActivity(intentHelp);
					break;
				}
			}
		});
	}

	private void findViews() {
		listView = (ListView) findViewById(R.id.listView_settings);
		button = (Button) findViewById(R.id.button_done);
	}

	private boolean checkFirmwareModelAndVersion() throws StarIOPortException {
		
		StarIOPort port = null;
		try {
			/*
			 * using StarIOPort3.1.jar (support USB Port) Android OS Version: upper 2.2
			 */
			port = StarIOPort.getPort(portName, portSettings, 10000, this);
			/*
			 * using StarIOPort.jar Android OS Version: under 2.1 port = StarIOPort.getPort(portName, portSettings, 10000);
			 */

			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
			}

			Map<String, String> firmware = port.getFirmwareInformation();

			String modelName = firmware.get("ModelName");
			String firmwareVersion = firmware.get("FirmwareVersion");
			
			SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
			Editor editor = pref.edit();
			editor.putString("bluetoothSettingDeviceTypeForPinCode", modelName);
			editor.commit();

			if (modelName.contains("SM-S21") || modelName.contains("SM-S22") || modelName.contains("SM-T30") || modelName.contains("SM-T40")) {
				float version = Float.valueOf(firmwareVersion);
				if (version < 3.0) {
					return false;
				}
			}
		} catch (StarIOPortException e) {
			return false;
		} finally {
			if (port != null) {
				try {
					StarIOPort.releasePort(port);
					try {
						Thread.sleep(500);		// It is necessary after releasePort
					} catch (InterruptedException e) {
					}

				} catch (StarIOPortException e) {
				}
			}
		}
		return true;
	}
	
	private void loadSetting() throws StarIOPortException {
		String errorMessage = "";

		// load current bluetooth settings
		try {
			printerManager.open();

			printerManager.loadSetting();

			deviceNameCapability = printerManager.getBluetoothDeviceNameCapability();
			iOSPortNameCapability = printerManager.getiOSPortNameCapability();
			autoConnectCapability = printerManager.getAutoConnectCapability();
			securityTypeCapability = printerManager.getSecurityTypeCapability();
			pinCodeCapability = printerManager.getPinCodeCapability();

			// get bluetooth settings
			if (deviceNameCapability == StarBluetoothSettingCapability.SUPPORT) {
				bluetoothDeviceName = printerManager.getBluetoothDeviceName();
			}
			if (iOSPortNameCapability == StarBluetoothSettingCapability.SUPPORT) {
				iOSPortName = printerManager.getiOSPortName();
			}
			if (autoConnectCapability == StarBluetoothSettingCapability.SUPPORT) {
				autoConnect = printerManager.getAutoConnect();
			}
			if (securityTypeCapability == StarBluetoothSettingCapability.SUPPORT) {
				securityType = printerManager.getSecurityType();
			}
			if (pinCodeCapability == StarBluetoothSettingCapability.SUPPORT) {

			}

		} catch (StarIOPortException e) {
			bluetoothDeviceName = null;
			iOSPortName = null;
			autoConnect = false;
			securityType = StarBluetoothSecurity.SSP;
			pinCode = null;

			e.printStackTrace();
			errorMessage += e.getMessage();
			throw new StarIOPortException(errorMessage);
		} finally {
			if (printerManager.isOpened()) {
				try {
					printerManager.close();
				} catch (StarIOPortException e) {
					e.printStackTrace();
					errorMessage += " / " + e.getMessage();
					throw new StarIOPortException(errorMessage);
				}
			}
		}
	}

	private void refreshSettingList() {
		listViewItems.clear();

		Map<String, String> map0 = new HashMap<String, String>();
		map0.put("main", getString(R.string.device_name));
		if (deviceNameCapability == StarBluetoothSettingCapability.SUPPORT) {
			map0.put("sub", bluetoothDeviceName);
		} else {
			map0.put("sub", getString(R.string.no_supported_description));
		}
		listViewItems.add(map0);

		Map<String, String> map1 = new HashMap<String, String>();
		map1.put("main", getString(R.string.iOS_port_name));
		if (iOSPortNameCapability == StarBluetoothSettingCapability.SUPPORT) {
			map1.put("sub", iOSPortName);
		} else {
			map1.put("sub", getString(R.string.no_supported_description));
		}
		listViewItems.add(map1);

		Map<String, String> map2 = new HashMap<String, String>();
		map2.put("main", getString(R.string.auto_connect));
		if (autoConnectCapability == StarBluetoothSettingCapability.SUPPORT) {
			String autoConnectSettings[] = getResources().getStringArray(R.array.auto_connect_settings);
			map2.put("sub", autoConnect ? autoConnectSettings[0] : autoConnectSettings[1]);
		} else {
			map2.put("sub", getString(R.string.no_supported_description));
		}
		listViewItems.add(map2);

		Map<String, String> map3 = new HashMap<String, String>();
		map3.put("main", getString(R.string.security));
		if (securityTypeCapability == StarBluetoothSettingCapability.SUPPORT) {
			if (printerManager.getDeviceType() == StarDeviceType.StarDeviceTypeDesktopPrinter) {
				String security_strings[] = getResources().getStringArray(R.array.desktop_security_settings);
				if (securityType == StarBluetoothSecurity.SSP) {
					map3.put("sub", security_strings[0]);
				} else {
					map3.put("sub", security_strings[1]);
				}
			} else {
				String security_strings[] = getResources().getStringArray(R.array.portable_security_settings);
				if (securityType == StarBluetoothSecurity.DISABLE) {
					map3.put("sub", security_strings[0]);
				} else {
					map3.put("sub", security_strings[1]);
				}
			}
		} else {
			map3.put("sub", getString(R.string.no_supported_description));
		}
		listViewItems.add(map3);

		Map<String, String> map4 = new HashMap<String, String>();
		map4.put("main", getString(R.string.change_pin_code));
		if (pinCodeCapability == StarBluetoothSettingCapability.SUPPORT) {
			String changePinCodeSettings[] = getResources().getStringArray(R.array.change_pin_code_description);
			map4.put("sub", (pinCode == null) ? changePinCodeSettings[0] : changePinCodeSettings[1]);
		} else {
			map4.put("sub", getString(R.string.no_supported_description));
		}
		listViewItems.add(map4);

		Map<String, String> map5 = new HashMap<String, String>();
		map5.put("main", getString(R.string.help));
		map5.put("sub", "");
		listViewItems.add(map5);

		adapter.notifyDataSetChanged();
	}

	@Override
	protected void onResume() {
		super.onResume();
		refreshSettingList();

	}

	public void onClick(View v) {
		new Builder(v.getContext()).setMessage(getString(R.string.update_confirmation_message)).setPositiveButton(getString(R.string.update), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {

				final ProgressDialog progressDialog = new ProgressDialog(me);
				progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
				progressDialog.setCancelable(false);
				progressDialog.show();

				class ApplySettingsTask extends AsyncTask<Void, Void, Boolean> {
					private String errorMessage = "";

					@Override
					protected Boolean doInBackground(Void... params) {
						try {
							setPrinterSettings();
						} catch (StarIOPortException e) {
							errorMessage = e.getMessage();
							return false;
						}

						return true;
					}

					@Override
					protected void onPostExecute(Boolean resultCode) {
						progressDialog.dismiss();

						if (resultCode == Boolean.TRUE) {

							new Builder(me).setTitle(getString(R.string.complete)).setMessage(getString(R.string.complete_description)).setCancelable(false).setPositiveButton("OK", new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int which) {
									finish(); // close Activity
								}
							}).show();

						} else {
							new Builder(me).setTitle(getString(R.string.error)).setMessage(getString(R.string.cannot_communicate_message) + " : " + errorMessage).setPositiveButton("OK", null).show();
						}
					}
				}

				new ApplySettingsTask().execute();
			}
		}).setNegativeButton(getString(R.string.do_not_update), null).show();
	}

	private void setPrinterSettings() throws StarIOPortException {
		String errorMessage = "";

		try {
			// open
			printerManager.open();

			// set Device Name
			printerManager.setBluetoothDeviceName(bluetoothDeviceName);

			// set iOS PortName
			printerManager.setiOSPortName(iOSPortName);

			// set Security setting
			printerManager.setSecurityType(securityType);

			// set AutoConnection
			printerManager.setAutoConnect(autoConnect);

			// change PINCode
			if (securityType == StarBluetoothSecurity.PINCODE) {
				if (pinCode != null) {
					printerManager.setPinCode(pinCode);

					if (printerManager.getPinCode() != pinCode) {
						throw new StarIOPortException("Could not set a pinCode.");
					}
				}
			}

			printerManager.apply();
		} catch (StarIOPortException e) {
			e.printStackTrace();
			errorMessage += e.getMessage();
			throw new StarIOPortException(errorMessage);
		} finally {
			if (printerManager.isOpened()) {
				try {
					printerManager.close();
				} catch (StarIOPortException e) {
					e.printStackTrace();
					errorMessage += " / " + e.getMessage();
					throw new StarIOPortException(errorMessage);
				}
			}
		}
	}

	public static void setInputedPinCode(String pinCode) {
		BluetoothSettingActivity.pinCode = pinCode;
	}

	public static String getInputedPinCode() {
		return BluetoothSettingActivity.pinCode;
	}
}
