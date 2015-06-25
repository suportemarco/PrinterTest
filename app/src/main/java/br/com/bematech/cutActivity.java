package br.com.bematech;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import br.com.bematech.PrinterFunctions.CutType;

public class cutActivity extends Activity {
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.cut);
	}

	public void FullCut(View view) {
		if (!checkClick.isClickEvent()) {
			return;
		}

		String portName = PrinterTypeActivity.getPortName();
		String portSettings = PrinterTypeActivity.getPortSettings();

		PrinterFunctions.performCut(this, portName, portSettings, CutType.FULL_CUT);
	}

	public void PartialCut(View view) {
		if (!checkClick.isClickEvent()) {
			return;
		}

		String portName = PrinterTypeActivity.getPortName();
		String portSettings = PrinterTypeActivity.getPortSettings();

		PrinterFunctions.performCut(this, portName, portSettings, CutType.PARTIAL_CUT);
	}

	public void FullCutWithFeed(View view) {
		if (!checkClick.isClickEvent()) {
			return;
		}

		String portName = PrinterTypeActivity.getPortName();
		String portSettings = PrinterTypeActivity.getPortSettings();

		PrinterFunctions.performCut(this, portName, portSettings, CutType.FULL_CUT_FEED);
	}

	public void PartialCutWithFeed(View view) {
		if (!checkClick.isClickEvent()) {
			return;
		}

		String portName = PrinterTypeActivity.getPortName();
		String portSettings = PrinterTypeActivity.getPortSettings();

		PrinterFunctions.performCut(this, portName, portSettings, CutType.PARTIAL_CUT_FEED);
	}

	public void Help(View view) {
		if (!checkClick.isClickEvent()) {
			return;
		}

		String helpString =
				"<UnderlineTitle>CUT</UnderlineTitle><br/><br/>" +
				"<Code>ASCII:</Code> <CodeDef>ESC d <StandardItalic>n</StandardItalic></CodeDef><br/>" +
				"<Code>Hex:</Code> <CodeDef>1B 64 <StandardItalic>n</StandardItalic></CodeDef><br/><br/>" +
				"&nbsp;&nbsp;n = 0,1,2, or 3<br/>" +
				"&nbsp;&nbsp;0 = Full cut at current position.<br/>" +
				"&nbsp;&nbsp;1 = Partial cut at current position.<br/>" +
				"&nbsp;&nbsp;2 = Paper is fed to cutting position, then full cut.<br/>" +
				"&nbsp;&nbsp;3 = Paper is fed to cutting position, then partial cut.<br/><br/>";
		helpMessage.SetMessage(helpString);

		Intent myIntent = new Intent(this, helpMessage.class);
		startActivityFromChild(this, myIntent, 0);
	}
}
