package br.com.bematech;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class CommandTypeActivity extends Activity {
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.commandtype);
	}

	public void onClickLineModeButton(View view) {
		if (!checkClick.isClickEvent()) {
			return;
		}

		Intent myIntent = new Intent(this, StarIOSDKPOSPrinterLineModeActivity.class);
		startActivityFromChild(this, myIntent, 0);
	}

	public void onClickLineModeDotPrinterButton(View view) {
		if (!checkClick.isClickEvent()) {
			return;
		}

		Intent myIntent = new Intent(this, StarIOSDKDotPOSPrinterLineModeActivity.class);
		startActivityFromChild(this, myIntent, 0);
	}

	public void onClickRasterModeButton(View view) {
		if (!checkClick.isClickEvent()) {
			return;
		}

		Intent myIntent = new Intent(this, StarIOSDKPOSPrinterRasterModeActivity.class);
		startActivityFromChild(this, myIntent, 0);
	}
}