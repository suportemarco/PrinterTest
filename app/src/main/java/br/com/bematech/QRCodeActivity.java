package br.com.bematech;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.Locale;

import br.com.bematech.PrinterFunctions.CorrectionLevelOption;
import br.com.bematech.PrinterFunctions.Model;

public class QRCodeActivity extends Activity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.qrcode);

		Spinner spinner_CorrectionLevel = (Spinner) findViewById(R.id.spinner_CorrectionLevel);
		ArrayAdapter<String> ad = new ArrayAdapter<String>(this, R.layout.spinner, new String[] { "L 7%", "M 15%", "Q 25%", "H 30%" });
		spinner_CorrectionLevel.setAdapter(ad);
		ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		Spinner spinner_Model = (Spinner) findViewById(R.id.spinner_Model);
		ad = new ArrayAdapter<String>(this, R.layout.spinner, new String[] { "Model 1", "Model 2" });
		spinner_Model.setAdapter(ad);
		ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		
		String portSettings = PrinterTypeActivity.getPortSettings();
		if(portSettings.toUpperCase(Locale.US).contains("PORTABLE"))
		{
			spinner_Model.setVisibility(View.GONE);
			TextView ModelText = (TextView) findViewById(R.id.textView_Model);
			ModelText.setVisibility(View.GONE);
		}

		Spinner spinner_CellSize = (Spinner) findViewById(R.id.spinner_CellSize);
		ad = new ArrayAdapter<String>(this, R.layout.spinner, new String[] { "1 dot ", "2 dots ", "3 dots ", "4 dots ", "5 dots ", "6 dots ", "7 dots ", "8 dots " });
		spinner_CellSize.setAdapter(ad);
		ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner_CellSize.setSelection(4);
	}

	public void PrintBarCode(View view) {
		if (!checkClick.isClickEvent())
			return;
		String portName = PrinterTypeActivity.getPortName();
		String portSettings = PrinterTypeActivity.getPortSettings();

		Spinner spinner_CorrectionLevel = (Spinner) findViewById(R.id.spinner_CorrectionLevel);
		PrinterFunctions.CorrectionLevelOption correctionLevel = CorrectionLevelOption.Low;
		switch (spinner_CorrectionLevel.getSelectedItemPosition()) {
		case 0:
			correctionLevel = CorrectionLevelOption.Low;
			break;
		case 1:
			correctionLevel = CorrectionLevelOption.Middle;
			break;
		case 2:
			correctionLevel = CorrectionLevelOption.Q;
			break;
		case 3:
			correctionLevel = CorrectionLevelOption.High;
			break;
		}

		Spinner spinner_Model = (Spinner) findViewById(R.id.spinner_Model);
		PrinterFunctions.Model model = Model.Model1;

		if(portSettings.toUpperCase(Locale.US).contains("PORTABLE"))
		{
			model = Model.Model2;
		}
		else
		{
			switch (spinner_Model.getSelectedItemPosition()) {
			case 0:
				model = Model.Model1;
				break;
			case 1:
				model = Model.Model2;
				break;
			}	
		}


		Spinner spinner_CellSize = (Spinner) findViewById(R.id.spinner_CellSize);
		byte cellSize = (byte) (spinner_CellSize.getSelectedItemPosition() + 1);
		EditText textView = (EditText) findViewById(R.id.editText_QRCodeData);
		byte[] barCodeData = textView.getText().toString().getBytes();

		PrinterFunctions.PrintQrCode(this, portName, portSettings, correctionLevel, model, cellSize, barCodeData);
	}

	public void Help(View view) {
		if (!checkClick.isClickEvent()) {
			return;
		}

		String helpString ="";
		String portSettings = PrinterTypeActivity.getPortSettings();
		if (portSettings.toUpperCase(Locale.US).contains("PORTABLE")) {		
			helpString =
					"<body><p>StarMicronics supports all the latest high data density " +
					"for QR Codes.  QR Codes  are handy for distributing URLs, Music, Images, E-Mails, " +
					"Contacts and much more.  Is public domain and great for storing Japanese Kanji " +
					"and Kana characters.  They can be scanned with almost all smart phones which is great " +
					"if you want to for example, put a QR Code to hyperlink your company's Facebook profile on " +
					"the bottom of every receipt. <br/><br/>" +
					"<SectionHeader>(1)Set error correction level</SectionHeader><br/>" +
					"<Code>ASCII:</Code> <CodeDef>ESC GS y S 1 <StandardItalic>n</StandardItalic></CodeDef><br/>" +
					"<Code>Hex:</Code> <CodeDeF>1B 1D 79 53 31 <StandardItalic>n</StandardItalic></CodeDef><br/><br/>" +
					"<SectionHeader>(2)Specify size of cell</SectionHeader><br/>" +
					"<Code>ASCII:</Code> <CodeDef>ESC GS y S 2 <StandardItalic>n</StandardItalic></CodeDef><br/>" +
					"<Code>Hex:</Code> <CodeDef>1B 1D 79 53 32 <StandardItalic>n</StandardItalic></CodeDef><br/><br/>" +
					"<SectionHeader>(3)Set barcode data</SectionHeader><br/>" +
					"<Code>ASCII:</Code> <CodeDef>ESC GS y D 1 NUL <StandardItalic>nL nH d1d2 ... dk</StandardItalic></CodeDef><br/>" +
					"<Code>Hex:</Code> <CodeDef>1B 1D 79 44 31 00 <StandardItalic>nL nH d1d2 ... dk</StandardItalic></CodeDef><br/><br/>" +
					"<SectionHeader>(4)Print barcode</SectionHeader><br/>" +
					"<Code>ASCII:</Code> <CodeDef>ESC GS y P</CodeDef><br/>" +
					"<Code>Hex:</Code> <CodeDef>1B 1D 79 50</CodeDef><br/></br>" +
					"* Note the QR Code is a registered trademark of DENSO WEB" +
					"</body><html>";
		}
		else
		{
			helpString =
					"<body><p>StarMicronics supports all the latest high data density " +
					"for QR Codes.  QR Codes  are handy for distributing URLs, Music, Images, E-Mails, " +
					"Contacts and much more.  Is public domain and great for storing Japanese Kanji " +
					"and Kana characters.  They can be scanned with almost all smart phones which is great " +
					"if you want to for example, put a QR Code to hyperlink your company's Facebook profile on " +
					"the bottom of every receipt. <br/><br/>" +
					"<SectionHeader>(1) Set Barcode model</SectionHeader><br/>" +
					"<Code>ASCII:</Code> <CodeDef>ESC GS y S 0 <StandardItalic>n</StandardItalic></CodeDef><br/>" +
					"<Code>Hex:</Code> <CodeDef>1B 1D 79 53 30 <StandardItalic>n</StandardItalic></CodeDef><br/><br/>" +
					"<SectionHeader>(2)Set error correction level</SectionHeader><br/>" +
					"<Code>ASCII:</Code> <CodeDef>ESC GS y S 1 <StandardItalic>n</StandardItalic></CodeDef><br/>" +
					"<Code>Hex:</Code> <CodeDeF>1B 1D 79 53 31 <StandardItalic>n</StandardItalic></CodeDef><br/><br/>" +
					"<SectionHeader>(3)Specify size of cell</SectionHeader><br/>" +
					"<Code>ASCII:</Code> <CodeDef>ESC GS y S 2 <StandardItalic>n</StandardItalic></CodeDef><br/>" +
					"<Code>Hex:</Code> <CodeDef>1B 1D 79 53 32 <StandardItalic>n</StandardItalic></CodeDef><br/><br/>" +
					"<SectionHeader>(4)Set barcode data</SectionHeader><br/>" +
					"<Code>ASCII:</Code> <CodeDef>ESC GS y D 1 NUL <StandardItalic>nL nH d1d2 ... dk</StandardItalic></CodeDef><br/>" +
					"<Code>Hex:</Code> <CodeDef>1B 1D 79 44 31 00 <StandardItalic>nL nH d1d2 ... dk</StandardItalic></CodeDef><br/><br/>" +
					"<SectionHeader>(5)Print barcode</SectionHeader><br/>" +
					"<Code>ASCII:</Code> <CodeDef>ESC GS y P</CodeDef><br/>" +
					"<Code>Hex:</Code> <CodeDef>1B 1D 79 50</CodeDef><br/></br>" +
					"* Note the QR Code is a registered trademark of DENSO WEB" +
					"</body><html>";			
		}

		helpMessage.SetMessage(helpString);

		Intent myIntent = new Intent(this, helpMessage.class);
		startActivityFromChild(this, myIntent, 0);
	}
}
