package br.com.bematech;

import java.util.Locale;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

public class barcodeselector2d extends Activity implements OnItemClickListener {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.barcodeselector);

		ListView listViewBarCode = (ListView) findViewById(R.id.listView_barcodeList);
		ListAdapter ad = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, new String[] { "QR Code", "PDF417" });

		listViewBarCode.setAdapter(ad);
		listViewBarCode.setOnItemClickListener(this);
	}

	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		if (arg2 == 0) {
			if (PrinterTypeActivity.getPortSettings().toUpperCase(Locale.US).contains("PORTABLE") && PrinterTypeActivity.getPortSettings().toUpperCase(Locale.US).contains("ESCPOS")) {
				Intent myIntent = new Intent(this, QrcodeMiniActivity.class);
				startActivityFromChild(this, myIntent, 0);
			} else {
				Intent myIntent = new Intent(this, QRCodeActivity.class);
				startActivityFromChild(this, myIntent, 0);
			}
		} else if (arg2 == 1) {
			if (PrinterTypeActivity.getPortSettings().toUpperCase(Locale.US).contains("PORTABLE") && PrinterTypeActivity.getPortSettings().toUpperCase(Locale.US).contains("ESCPOS")) {
				Intent myIntent = new Intent(this, pdf417miniActivity.class);
				startActivityFromChild(this, myIntent, 0);
			} else {
				Intent myIntent = new Intent(this, pdf417Activity.class);
				startActivityFromChild(this, myIntent, 0);
			}
		}
	}
}
