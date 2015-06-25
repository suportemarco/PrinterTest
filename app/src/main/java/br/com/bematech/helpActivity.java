package br.com.bematech;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class helpActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.help);

		TextView textView_help = (TextView) findViewById(R.id.textView_Help);
		String helpText = 
						"PORT NAME PARAMETERS\n" 
						+ "If using WLAN/Ethernet...\n"
						+ "TCP:192.168.222.244\n" 
						+ "Enter your IP address\n" 
						+ "\n" 
						+ "If using USB...\n" 
						+ "USB: (No value)\n" 
						+ "USB:USB serial number\n" 
						+ "\n" 
						+ "If using Bluetooth...\n" 
						+ "BT: (No value) Uses first Star printer paired with the device\n" 
						+ "BT:Device_Name\n" 
						+ "BT:MAC Address\n" 
						+ "\n" 
						+ "PORT SETTINGS PARAMETERS\n" 
						+ "Port Settings should be blank for Star POS printers\n" 
						+ "\n" 
						+ "Port Settings should be 'portable' for Star portable printers.";
		textView_help.setText(helpText);
	}
}
