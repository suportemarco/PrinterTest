package br.com.bematech;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Spinner;

import java.util.Locale;

import br.com.bematech.PrinterFunctions.RasterCommand;

public class rasterPrintingActivity extends Activity implements OnItemSelectedListener {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.printingtextasimage);
		
		Spinner spinner_Font = (Spinner) findViewById(R.id.spinner_Font);
		ArrayAdapter<String> ad = new ArrayAdapter<String>(this, R.layout.spinner, new String[] { "Default", "MonoSpace", "Serif", "Sans" });

		spinner_Font.setAdapter(ad);
		ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		
		spinner_Font.setOnItemSelectedListener(this);

		CheckBox italics = (CheckBox) findViewById(R.id.checkbox_italics);
		italics.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				SetTextBox();
			}
		});

		CheckBox checkbox_bold = (CheckBox) findViewById(R.id.checkbox_bold);
		checkbox_bold.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				SetTextBox();
			}
		});

		CheckBox checkbox_CompressAPI = (CheckBox) findViewById(R.id.checkbox_CompressAPI);
		CheckBox checkbox_pageMode = (CheckBox) findViewById(R.id.checkbox_pageMode);

		checkbox_CompressAPI.setChecked(true);

		Spinner spinner_paperWidth = (Spinner) findViewById(R.id.spinner_paperwidth);

		ArrayAdapter<String> ad_paperWidth;

		if (PrinterTypeActivity.getPortSettings().toUpperCase(Locale.US).contains("PORTABLE")) {
			ad_paperWidth = new ArrayAdapter<String>(this, R.layout.spinner, new String[] { "2inch", "3inch", "4inch" });
		} else {
			ad_paperWidth = new ArrayAdapter<String>(this, R.layout.spinner, new String[] { "3inch", "4inch" });
		}
		if (!PrinterTypeActivity.getPortSettings().toUpperCase(Locale.US).contains("ESCPOS")) {
			checkbox_pageMode.setVisibility(View.GONE);
		}

		spinner_paperWidth.setAdapter(ad_paperWidth);
		ad_paperWidth.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		EditText edittext_TextToPrint = (EditText) findViewById(R.id.editText_TextToPrint);
		float textSize = edittext_TextToPrint.getTextSize();

		EditText edittext_textsize = (EditText) findViewById(R.id.editText_TextSize);
		edittext_textsize.setText(Float.toString(textSize));
		edittext_textsize.setOnKeyListener(new OnKeyListener() {

			public boolean onKey(View v, int keyCode, KeyEvent event) {
				SetTextBox();
				return false;
			}

		});

		// setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	}

	public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		SetTextBox();
	}

	public void onNothingSelected(AdapterView<?> arg0) {
		//
	}

	public void SetTextBox() {
		Spinner spinner_Font = (Spinner) findViewById(R.id.spinner_Font);

		EditText text = (EditText) findViewById(R.id.editText_TextToPrint);
		Typeface typeface = Typeface.MONOSPACE;
		switch (spinner_Font.getSelectedItemPosition()) {
		case 0:
			typeface = Typeface.DEFAULT;
			break;
		case 1:
			typeface = Typeface.MONOSPACE;
			break;
		case 2:
			typeface = Typeface.SERIF;
			break;
		case 3:
			typeface = Typeface.SANS_SERIF;
			break;
		}

		CheckBox checkBox_italics = (CheckBox) findViewById(R.id.checkbox_italics);
		int style = 0;
		if (checkBox_italics.isChecked() == true) {
			style = style | Typeface.ITALIC;
		}

		CheckBox checkBox_bold = (CheckBox) findViewById(R.id.checkbox_bold);
		if (checkBox_bold.isChecked() == true) {
			style = style | Typeface.BOLD;
		}

		EditText editText = (EditText) findViewById(R.id.editText_TextSize);
		float textSize = 27;
		try {
			textSize = Float.parseFloat(editText.getText().toString());
		} catch (Exception e) {
			textSize = 27;
		}

		text.setTypeface(typeface, style);
		text.setTextSize(textSize);
	}

	public void PrintText(View view) {
		if (!checkClick.isClickEvent()) {
			return;
		}

		CheckBox checkBox_italics = (CheckBox) findViewById(R.id.checkbox_italics);
		int style = 0;
		boolean italics = false;
		boolean compressionEnable = false;
		boolean pageModeEnable = false;

		if (checkBox_italics.isChecked() == true) {
			italics = true;
		}

		CheckBox checkBox_bold = (CheckBox) findViewById(R.id.checkbox_bold);
		if (checkBox_bold.isChecked() == true) {
			style = style | Typeface.BOLD;
		}

		CheckBox checkbox_CompressAPI = (CheckBox) findViewById(R.id.checkbox_CompressAPI);
		if (checkbox_CompressAPI.isChecked() == true) {
			compressionEnable = true;
		}

		CheckBox checkbox_pageMode = (CheckBox) findViewById(R.id.checkbox_pageMode);
		if (checkbox_pageMode.isChecked() == true) {
			pageModeEnable = true;
		}

		Spinner spinner_Font = (Spinner) findViewById(R.id.spinner_Font);
		Typeface font = Typeface.DEFAULT;
		switch (spinner_Font.getSelectedItemPosition()) {
		case 0:
			font = Typeface.DEFAULT;
			break;
		case 1:
			font = Typeface.MONOSPACE;
			break;
		case 2:
			font = Typeface.SERIF;
			break;
		case 3:
			font = Typeface.SANS_SERIF;
			break;
		}

		// paint.setTypeface(typeface)

		EditText editText_textsize = (EditText) findViewById(R.id.editText_TextSize);
		float textSize = 0;
		String sizeString = editText_textsize.getText().toString();

		if(sizeString == null || sizeString.length() == 0) {
			postMessage("Failure", "Size is Empty.");
			editText_textsize.requestFocus();
			return;	
		}
		textSize = Float.parseFloat(sizeString);
		
		if(textSize < 5.0) {
			postMessage("Failure", "Size is too small.");
			editText_textsize.requestFocus();
			return;
		}

		String portName = PrinterTypeActivity.getPortName();
		String portSettings = PrinterTypeActivity.getPortSettings();

		Spinner spinner_paperWidth = (Spinner) findViewById(R.id.spinner_paperwidth);
		int paperWidth = 0;

		if (portSettings.toUpperCase(Locale.US).contains("PORTABLE")) {
			paperWidth = 408;
			switch (spinner_paperWidth.getSelectedItemPosition()) {
			case 0:
				paperWidth = 384; // 2inch (384 dot)
				break;
			case 1:
				paperWidth = 576; // 3inch (576 dot)
				break;
			case 2:
				paperWidth = 832; // 4inch (832 dot)
				break;
			}
		} else {
			paperWidth = 576;
			switch (spinner_paperWidth.getSelectedItemPosition()) {
			case 0:
				paperWidth = 576; // 3inch (576 dot)
				break;
			case 1:
				paperWidth = 832; // 4inch (832 dot)
				break;
			}
		}

		EditText editText_text = (EditText) findViewById(R.id.editText_TextToPrint);
		String textToPrint = editText_text.getText().toString();

		Paint paint = new Paint();
		paint.setStyle(Paint.Style.FILL);
		paint.setColor(Color.BLACK);
		paint.setAntiAlias(true);
		Typeface typeface = Typeface.create(font, style);
		paint.setTypeface(typeface);
		paint.setTextSize(textSize * 2);
		TextPaint textpaint = new TextPaint(paint);
		if (italics) {
			textpaint.setTextSkewX((float) -0.25);
		}

		StaticLayout staticLayout = new StaticLayout(textToPrint, textpaint, paperWidth, Layout.Alignment.ALIGN_NORMAL, 1, 0, false);
		int height = staticLayout.getHeight();

		if (portSettings.toUpperCase(Locale.US).contains("PORTABLE") && portSettings.toUpperCase(Locale.US).contains("ESCPOS") && (pageModeEnable == true)) {
			if (height > 2378) {
				height = 2378;
			}
		}

		try {
			Bitmap bitmap = Bitmap.createBitmap(staticLayout.getWidth(), height, Bitmap.Config.RGB_565);
			Canvas c = new Canvas(bitmap);
			c.drawColor(Color.WHITE);
			c.translate(0, 0);
			staticLayout.draw(c);

			if (portSettings.toUpperCase(Locale.US).contains("PORTABLE") && portSettings.toUpperCase(Locale.US).contains("ESCPOS")) {
				MiniPrinterFunctions.PrintBitmap(this, portName, portSettings, bitmap, paperWidth, compressionEnable, pageModeEnable);
			} else {
				RasterCommand rasterType = RasterCommand.Standard;
				if (portSettings.toUpperCase(Locale.US).contains("PORTABLE")) {
					rasterType = RasterCommand.Graphics;
				}
				PrinterFunctions.PrintBitmap(this, portName, portSettings, bitmap, paperWidth, compressionEnable, rasterType);
			}
		} catch (IllegalArgumentException e) {
			postMessage("Failure", "Size is too large.");
			editText_textsize.requestFocus();
		} catch (OutOfMemoryError e) {
			postMessage("Failure", "Size is too large.");
			editText_textsize.requestFocus();
		}
	}

	protected void postMessage(String titleText, String messageText) {

	    Builder dialog = new Builder(this);
	    dialog.setNegativeButton("Ok", null);
	    AlertDialog alert = dialog.create();
	    alert.setTitle(titleText);
	    alert.setMessage(messageText);
	    alert.setCancelable(false);
	    alert.show();
	}
	
	public void Help(View view) {
		if (!checkClick.isClickEvent()) {
			return;
		}

		String helpString = "";
		String portSettings = PrinterTypeActivity.getPortSettings();
		if (portSettings.toUpperCase(Locale.US).contains("PORTABLE") && portSettings.toUpperCase(Locale.US).contains("ESCPOS")) {
			helpString =
					"<UnderlineTitle>Define Bit Image</UnderlineTitle><br/><br/>" +
					"<Code>ASCII:</Code> <CodeDef>ESC X 4 <StandardItalic>x y d1...dk</StandardItalic></CodeDef><br/>" +
					"<Code>Hex:</Code> <CodeDef>1B 58 34 <StandardItalic>x y d1...dk</StandardItalic></CodeDef><br/><br/>" +
					"<rightMov>x</rightMov> <rightMov_NOI>Width of the image divided by 8</rightMov_NOI><br/>" +
					"<rightMov>y</rightMov> <rightMov_NOI>Vertical number of dots to be printed.  This value shouldn't exceed 24.  If you need to print an image taller than 24 then you should use this command multiple times</rightMov_NOI><br/><br/><br/><br/><br/><br/>" +
					"<rightMov>d1...dk</rightMov> <rightMov_NOI2>The dots that should be printed.  When all vertical dots are printed the head moves horizonaly to the next vertical set of dots</rightMov_NOI2><br/><br/><br/><br/><br/><br/>" +
					"<UnderlineTitle>Print Bit Image</UnderlineTitle><br/><br/>" +
					"<Code>ASCII:</Code> <CodeDef>ESC X 2 <StandardItalic>y</StandardItalic></CodeDef><br/>" +
					"<Code>Hex:</Code> <CodeDef>1B 58 32 <StandardItalics>y</StandardItalics></CodeDef><br/><br/>" +
					"<rightMov>y</rightMov> <rightMov_NOI>The value y must be the same value that was used in ESC X 4 command for define a bit image</rightMov_NOI><br/><br/><br/><br/>" +
					"Note: The command ESC X 2 must be used after each usage of ESC X 4 inorder to print images";
		} else if (portSettings.toUpperCase(Locale.US).contains("PORTABLE")) {
			helpString =
					"<html>" +
							"<head>" +
							"<style type=\"text/css\">" +
							"Code {color:blue;}" +
							"CodeDef {color:blue;font-weight:bold}" +
							"TitleBold {font-weight:bold}" +
							"It1 {font-style:italic; font-size:12}" +
							"LargeTitle{font-size:20px}" +
							"SectionHeader{font-size:17;font-weight:bold}" +
							"UnderlineTitle {text-decoration:underline}" +
							"div_cutParam {position:absolute; top:100; left:30; width:200px;font-style:italic;}" +
							"div_cutParam0 {position:absolute; top:130; left:30; font-style:italic;}" +
							"div_cutParam1 {position:absolute; top:145; left:30; font-style:italic;}" +
							"div_cutParam2 {position:absolute; top:160; left:30; font-style:italic;}" +
							"div_cutParam3 {position:absolute; top:175; left:30; font-style:italic;}" +
							".div-tableBarcodeWidth{display:table;}" +
							".div-table-rowBarcodeWidth{display:table-row;}" +
							".div-table-colBarcodeWidthHeader{display:table-cell;border:1px solid #000000;background: #800000;color:#ffffff}" +
							".div-table-colBarcodeWidthHeader2{display:table-cell;border:1px solid #000000;background: #800000;color:#ffffff}" +
							".div-table-colBarcodeWidth{display:table-cell;border:1px solid #000000;}" +
							"rightMov {left:30px; font-style:italic;}" +
							"rightMov_NOI {left:55px;}" +
							"rightMov_NOI2 {left:90px;}" +
							"StandardItalic {font-style:italic;}" +
							".div-tableCut{display:table;}" +
							".div-table-rowCut{display:table-row;}" +
							".div-table-colFirstCut{display:table-cell;width:40px}" +
							".div-table-colCut{display:table-cell;}" +
							".div-table-colRaster{display:table-cell; border:1px solid #000000;}" +
							"</style>" +
							"</head>" +
							"<body>" +
							"<UnderlineTitle>Print Raster Graphics data</UnderlineTitle><br/><br/>" +
							"<Code>ASCII:</Code><CodeDef>ESC GS S <StandardItalic>m xL xH yL yH n1 [d11 d12 ... d1k] n2 [d21 d22 ... d2k]</StandardItalic></CodeDef><br/>" +
							"<Code>Hex:</Code><CodeDef>1B 1D 53 <StandardItalic>m xL xH yL yH n1 [d11 d12 ... d1k] n2 [d21 d22 ... d2k]</StandardItalic></CodeDef><br/><br/>" +
							"<rightMov>m</rightMov><rightMov_NOI>'m' specifies the number of transfer blocks and the tone.</rightMov_NOI><br/><br/>" +
							"<rightMov>xL, xH</rightMov><br/>" +
							"<rightMov_NOI>(xL + xH x 256) specifies the number of horizontal data bytes. </rightMov_NOI><br/><br/>" +
							"<rightMov>yL, yH</rightMov><br/>" +
							"<rightMov_NOI>(yL + yX x 256) specifies the number of dots in the vertical direction.</rightMov_NOI><br/><br/>" +
							"<rightMov>k</rightMov><rightMov_NOI>'k' indicates the number of data.</rightMov_NOI><br/>" +
							"<rightMov>d</rightMov><rightMov_NOI>(d1, d2, ... dk) specifies the image data to define.</rightMov_NOI><br/><br/><br/>" +
							"<UnderlineTitle>n/8mm line feed</UnderlineTitle><br/>" +
							"<Code>ASCII:</Code><CodeDef>ESC I <StandardItalic>n</StandardItalic></CodeDef><br/>" +
							"<Code>Hex:</Code><CodeDef>1B 49 <StandardItalic>n</StandardItalic></CodeDef><br/>" +
							"<rightMov>1 &le; n &le; 255</rightMov> <br/>" +
							"<rightMov_NOI>" +
							"Executes a n/8mm paper feed.<br/>" +
							"If print data exists in the line buffer, it prints that data." +
							"Using this command will intermittently feed paper, therefore, it is normally recommended that this command not be used." +
							"</rightMov_NOI><br/><br/><br/><br/><br/><br/>" +
							"</td>" +
							"</tr>" +
							"</table><br/><br/>" +
							"</body>" +
							"</html>";
		} else {
			helpString =
					"<UnderlineTitle>Enter Raster Mode</UnderlineTitle><br/><br/>" +
					"<Code>ASCII:</Code> <CodeDef>ESC * r A</CodeDef><br/>" +
					"<Code>Hex:</Code> <CodeDef>1B 2A 72 41</CodeDef><br/><br/>" +
					"<UnderlineTitle>Initialize raster mode</UnderlineTitle><br/>" +
					"<Code>ASCII:</Code> <CodeDef>ESC * r R</CodeDef><br/>" +
					"<Code>Hex:</Code> <CodeDef>1B 2A 72 52</CodeDef><br/><br/>" +
					"<UnderlineTitle>Set Raster EOT mode</UnderlineTitle><br/><br/>" +
					"<Code>ASCII:</Code> <CodeDef>ESC * r E <StandardItalic>n</StandardItalic> NUL</CodeDef><br/>" +
					"<Code>Hex:</Code> <CodeDef>1B 2A 72 45 <StandardItalic>n</StandardItalic> 00</CodeDef><br/>" +
					"<div class=\"div-tableCut\">" +
					"<div class=\"div-table-rowCut\">" +
						"<div class=\"div-table-colRaster\"><center>n</center></div>" +
						"<div class=\"div-table-colRaster\"><center>FormFeed</center></div>" +
						"<div class=\"div-table-colRaster\"><center>Cut Feed</center></div>" +
						"<div class=\"div-table-colRaster\"><center>Cutter</center></div>" +
						"<div class=\"div-table-colRaster\"><center>Presenter</center></div>" +
					"</div>" +
						"<div class=\"div-table-rowCut\">" +
						"<div class=\"div-table-colRaster\"><center>0</center></div>" +
						"<div class=\"div-table-colRaster\"><center>Default</center></div>" +
						"<div class=\"div-table-colRaster\"><center>Default</center></div>" +
						"<div class=\"div-table-colRaster\"><center>Default</center></div>" +
						"<div class=\"div-table-colRaster\"><center>Default</center></div>" +
					"</div>" +
						"<div class=\"div-table-rowCut\">" +
						"<div class=\"div-table-colRaster\"><center>1</center></div>" +
						"<div class=\"div-table-colRaster\"><center>&#x25CB;</center></div>" +
						"<div class=\"div-table-colRaster\"><center>-</center></div>" +
						"<div class=\"div-table-colRaster\"><center>-</center></div>" +
						"<div class=\"div-table-colRaster\"><center>-</center></div>" +
					"</div>" +
						"<div class=\"div-table-rowCut\">" +
						"<div class=\"div-table-colRaster\"><center>2</center></div>" +
						"<div class=\"div-table-colRaster\"><center>&#x25CB;</center></div>" +
						"<div class=\"div-table-colRaster\"><center>&#x25CB;</center></div>" +
						"<div class=\"div-table-colRaster\"><center>-</center></div>" +
						"<div class=\"div-table-colRaster\"><center>-</center></div>" +
					"</div>" +
						"<div class=\"div-table-rowCut\">" +
						"<div class=\"div-table-colRaster\"><center>3</center></div>" +
						"<div class=\"div-table-colRaster\"><center>&#x25CB;</center></div>" +
						"<div class=\"div-table-colRaster\"><center>TearBar</center></div>" +
						"<div class=\"div-table-colRaster\"><center>-</center></div>" +
						"<div class=\"div-table-colRaster\"><center>-</center></div>" +
					"</div>" +
						"<div class=\"div-table-rowCut\">" +
						"<div class=\"div-table-colRaster\"><center>8</center></div>" +
						"<div class=\"div-table-colRaster\"><center>&#x25CB;</center></div>" +
						"<div class=\"div-table-colRaster\"><center>-</center></div>" +
						"<div class=\"div-table-colRaster\"><center>Full Cut</center></div>" +
						"<div class=\"div-table-colRaster\"><center>-</center></div>" +
					"</div>" +
						"<div class=\"div-table-rowCut\">" +
						"<div class=\"div-table-colRaster\"><center>9</center></div>" +
						"<div class=\"div-table-colRaster\"><center>&#x25CB;</center></div>" +
						"<div class=\"div-table-colRaster\"><center>&#x25CB;</center></div>" +
						"<div class=\"div-table-colRaster\"><center>Full Cut</center></div>" +
						"<div class=\"div-table-colRaster\"><center>-</center></div>" +
					"</div>" +
						"<div class=\"div-table-rowCut\">" +
						"<div class=\"div-table-colRaster\"><center>12</center></div>" +
						"<div class=\"div-table-colRaster\"><center>&#x25CB;</center></div>" +
						"<div class=\"div-table-colRaster\"><center>-</center></div>" +
						"<div class=\"div-table-colRaster\"><center>Partial Cut</center></div>" +
						"<div class=\"div-table-colRaster\"><center>-</center></div>" +
					"</div>" +
						"<div class=\"div-table-rowCut\">" +
						"<div class=\"div-table-colRaster\"><center>13</center></div>" +
						"<div class=\"div-table-colRaster\"><center>&#x25CB;</center></div>" +
						"<div class=\"div-table-colRaster\"><center>&#x25CB;</center></div>" +
						"<div class=\"div-table-colRaster\"><center>Partial Cut</center></div>" +
						"<div class=\"div-table-colRaster\"><center>-</center></div>" +
					"</div>" +
						"<div class=\"div-table-rowCut\">" +
						"<div class=\"div-table-colRaster\"><center>36</center></div>" +
						"<div class=\"div-table-colRaster\"><center>&#x25CB;</center></div>" +
						"<div class=\"div-table-colRaster\"><center>-</center></div>" +
						"<div class=\"div-table-colRaster\"><center>Full Cut</center></div>" +
						"<div class=\"div-table-colRaster\"><center>Eject</center></div>" +
					"</div>" +
						"<div class=\"div-table-rowCut\">" +
						"<div class=\"div-table-colRaster\"><center>37</center></div>" +
						"<div class=\"div-table-colRaster\"><center>&#x25CB;</center></div>" +
						"<div class=\"div-table-colRaster\"><center>&#x25CB;</center></div>" +
						"<div class=\"div-table-colRaster\"><center>Full Cut</center></div>" +
						"<div class=\"div-table-colRaster\"><center>Eject</center></div>" +
					"</div>" +
					"</div><br/><br/>" +
					"<UnderlineTitle>Set Raster FF mode</UnderlineTitle><br/><br/>" +
					"<Code>ASCII:</Code> <CodeDef>ESC * r F <StandardItalic>n</StandardItalic> NUL</CodeDef><br/>" +
					"<Code>Hex:</Code> <CodeDef>1B 2A 72 46 <StandardItalic>n</StandardItalic> 00</CodeDef><br/>" +
						"<div class=\"div-tableCut\">" +
						"<div class=\"div-table-rowCut\">" +
						"<div class=\"div-table-colRaster\"><center>n</center></div>" +
						"<div class=\"div-table-colRaster\"><center>FormFeed</center></div>" +
						"<div class=\"div-table-colRaster\"><center>Cut Feed</center></div>" +
						"<div class=\"div-table-colRaster\"><center>Cutter</center></div>" +
						"<div class=\"div-table-colRaster\"><center>Presenter</center></div>" +
					"</div>" +
						"<div class=\"div-table-rowCut\">" +
						"<div class=\"div-table-colRaster\"><center>0</center></div>" +
						"<div class=\"div-table-colRaster\"><center>Default</center></div>" +
						"<div class=\"div-table-colRaster\"><center>Default</center></div>" +
						"<div class=\"div-table-colRaster\"><center>Default</center></div>" +
						"<div class=\"div-table-colRaster\"><center>Default</center></div>"	+
					"</div>" +
						"<div class=\"div-table-rowCut\">" +
						"<div class=\"div-table-colRaster\"><center>1</center></div>" +
						"<div class=\"div-table-colRaster\"><center>&#x25CB;</center></div>" +
						"<div class=\"div-table-colRaster\"><center>-</center></div>" +
						"<div class=\"div-table-colRaster\"><center>-</center></div>" +
						"<div class=\"div-table-colRaster\"><center>-</center></div>" +
					"</div>" +
						"<div class=\"div-table-rowCut\">" +
						"<div class=\"div-table-colRaster\"><center>2</center></div>" +
						"<div class=\"div-table-colRaster\"><center>&#x25CB;</center></div>" +
						"<div class=\"div-table-colRaster\"><center>&#x25CB;</center></div>" +
						"<div class=\"div-table-colRaster\"><center>-</center></div>" +
						"<div class=\"div-table-colRaster\"><center>-</center></div>" +
					"</div>" +
						"<div class=\"div-table-rowCut\">" +
						"<div class=\"div-table-colRaster\"><center>3</center></div>" +
						"<div class=\"div-table-colRaster\"><center>&#x25CB;</center></div>" +
						"<div class=\"div-table-colRaster\"><center>TearBar</center></div>" +
						"<div class=\"div-table-colRaster\"><center>-</center></div>" +
						"<div class=\"div-table-colRaster\"><center>-</center></div>" +
					"</div>" +
						"<div class=\"div-table-rowCut\">" +
						"<div class=\"div-table-colRaster\"><center>8</center></div>" +
						"<div class=\"div-table-colRaster\"><center>&#x25CB;</center></div>" +
						"<div class=\"div-table-colRaster\"><center>-</center></div>" +
						"<div class=\"div-table-colRaster\"><center>Full Cut</center></div>" +
						"<div class=\"div-table-colRaster\"><center>-</center></div>" +
					"</div>" +
						"<div class=\"div-table-rowCut\">" +
						"<div class=\"div-table-colRaster\"><center>9</center></div>" +
						"<div class=\"div-table-colRaster\"><center>&#x25CB;</center></div>" +
						"<div class=\"div-table-colRaster\"><center>&#x25CB;</center></div>" +
						"<div class=\"div-table-colRaster\"><center>Full Cut</center></div>" +
						"<div class=\"div-table-colRaster\"><center>-</center></div>" +
					"</div>" +
						"<div class=\"div-table-rowCut\">" +
						"<div class=\"div-table-colRaster\"><center>12</center></div>" +
						"<div class=\"div-table-colRaster\"><center>&#x25CB;</center></div>" +
						"<div class=\"div-table-colRaster\"><center>-</center></div>" +
						"<div class=\"div-table-colRaster\"><center>Partial Cut</center></div>" +
						"<div class=\"div-table-colRaster\"><center>-</center></div>" +
					"</div>" +
						"<div class=\"div-table-rowCut\">" +
						"<div class=\"div-table-colRaster\"><center>13</center></div>" +
						"<div class=\"div-table-colRaster\"><center>&#x25CB;</center></div>" +
						"<div class=\"div-table-colRaster\"><center>&#x25CB;</center></div>" +
						"<div class=\"div-table-colRaster\"><center>Partial Cut</center></div>" +
						"<div class=\"div-table-colRaster\"><center>-</center></div>" +
					"</div>" +
						"<div class=\"div-table-rowCut\">" +
						"<div class=\"div-table-colRaster\"><center>36</center></div>" +
						"<div class=\"div-table-colRaster\"><center>&#x25CB;</center></div>" +
						"<div class=\"div-table-colRaster\"><center>-</center></div>" +
						"<div class=\"div-table-colRaster\"><center>Full Cut</center></div>" +
						"<div class=\"div-table-colRaster\"><center>Eject</center></div>" +
					"</div>" +
						"<div class=\"div-table-rowCut\">" +
						"<div class=\"div-table-colRaster\"><center>37</center></div>" +
						"<div class=\"div-table-colRaster\"><center>&#x25CB;</center></div>" +
						"<div class=\"div-table-colRaster\"><center>&#x25CB;</center></div>" +
						"<div class=\"div-table-colRaster\"><center>Full Cut</center></div>" +
						"<div class=\"div-table-colRaster\"><center>Eject</center></div>" +
					"</div>" +
					"</div><br/><br/>" +
					"<UnderlineTitle>Set raster page length</UnderlineTitle><br/>" +
					"<Code>ASCII:</Code> <CodeDef>ESC * r P <StandardItalic>n</StandardItalic> NUL</CodeDef><br/>" +
					"<Code>Hex:</Code> <CodeDef>1B 2A 72 50 <StandardItalic>n</StandardItalic> 00</CodeDef><br/><br/>" +
					"<rightMov>0 = Continuous print mode (no page length setting)</rightMov><br/><br/>" +
					"<rightMov>1&#60;n = Specify page length</rightMov><br/><br/>" +
					"<UnderlineTitle>Set raster print quality</UnderlineTitle><br/>" +
					"<Code>ASCII:</Code> <CodeDef>ESC * r Q <StandardItalic>n</StandardItalic> NUL</CodeDef><br/>" +
					"<Code>Hex:</Code> <CodeDef>1B 2A 72 51 <StandardItalic>n</StandardItalic> 00</CodeDef><br/><br/>" +
					"<rightMov>0 = Specify high speed printing</rightMov><br/>" +
					"<rightMov>1 = Normal print quality</rightMov><br/>" +
					"<rightMov>2 = High print quality</rightMov><br/><br/>" +
					"<UnderlineTitle>Set raster left margin</UnderlineTitle><br/><br/>" +
					"<Code>ASCII:</Code> <CodeDef>ESC * r m l <StandardItalic>n</StandardItalic> NUL</CodeDef><br/>" +
					"<Code>Hex:</Code> <CodeDef>1B 2A 72 6D 6C <StandardItalic>n</StandardItalic> 00</CodeDef><br/><br/>" +
					"<UnderlineTitle>Send raster data (auto line feed)</UnderlineTitle><br/><br/>" +
					"<Code>ASCII:</Code> <CodeDef>b <StandardItalic>n1 n2 d1 d2 ... dk</StandardItalic></CodeDef><br/>" +
					"<Code>Hex:</Code> <CodeDef>62 <StandardItalic>n1 n2 d1 d2 ... dk</StandardItalic></CodeDef><br/><br/>" +
					"<rightMov>n1 = (image width / 8) Mod 256</rightMov><br/>" +
					"<rightMov>n2 = image width / 256</rightMov><br/>" +
					"<rightMov>k = n1 + n2 * 256</rightMov><br/>" +
					"* Each byte send in d1 ... dk represents 8 horizontal bits.  The values n1 and n2 tell the printer how many byte are sent with d1 ... dk.  The printer automatically feeds when the last value for d1 ... dk is sent<br/><br/>" +
					"<UnderlineTitle>Quit raster mode</UnderlineTitle><br/></br>" +
					"<Code>ASCII:</Code> <CodeDef>ESC * r B</CodeDef><br/>" +
					"<Code>Hex:</Code> <CodeDef>1B 2A 72 42</CodeDef><br/><br/>" +
					"* This command automatically executes a EOT(cut) command before quiting.  Use the set EOT command to change the action of this command.";
		}
		helpMessage.SetMessage(helpString);

		Intent myIntent = new Intent(this, helpMessage.class);
		startActivityFromChild(this, myIntent, 0);
	}

}
