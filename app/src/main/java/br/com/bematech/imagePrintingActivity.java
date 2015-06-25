package br.com.bematech;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.Locale;

import br.com.bematech.PrinterFunctions.RasterCommand;

public class imagePrintingActivity extends Activity implements OnItemSelectedListener {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.printingimage);

		Spinner spinner_Image = (Spinner) findViewById(R.id.spinner_Image);
		ArrayAdapter<String> ad = new ArrayAdapter<String>(this, R.layout.spinner, new String[] { "image1", "image2", "image3", "image4" });
		ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		
		spinner_Image.setAdapter(ad);
		spinner_Image.setOnItemSelectedListener(this);

		String[] paper_width_array;
		if (PrinterTypeActivity.getPortSettings().toUpperCase(Locale.US).contains("PORTABLE")) {
			paper_width_array = new String[] { "2inch", "3inch", "4inch" };
		} else {
			paper_width_array = new String[] { "3inch", "4inch" };
		}
		Spinner spinner_paper_width = (Spinner) findViewById(R.id.spinner_paper_width);
		ArrayAdapter<String> ad_paper_width = new ArrayAdapter<String>(this, R.layout.spinner, paper_width_array);
		spinner_paper_width.setAdapter(ad_paper_width);
		ad_paper_width.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		
		//setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		CheckBox checkbox_bitImage_CompressAPI = (CheckBox) findViewById(R.id.checkbox_bitImage_CompressAPI);
		CheckBox checkbox_bitImage_pageMode = (CheckBox) findViewById(R.id.checkbox_bitImage_pageMode);

		if (PrinterTypeActivity.getPortSettings().toUpperCase(Locale.US).contains("ESCPOS")) {
			//
		} else {
			TextView textView_paper_width = (TextView) findViewById(R.id.textView_paper_width);
			checkbox_bitImage_pageMode.setVisibility(View.GONE);
			textView_paper_width.setVisibility(View.GONE);
		}

		checkbox_bitImage_CompressAPI.setChecked(true);
	}

	public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {

		ImageView imageView_Image = (ImageView) findViewById(R.id.imageView_Image);
		switch (arg2) {
		case 0:
			imageView_Image.setImageResource(R.drawable.image1);
			break;
		case 1:
			imageView_Image.setImageResource(R.drawable.image2);
			break;
		case 2:
			imageView_Image.setImageResource(R.drawable.image3);
			break;
		case 3:
			imageView_Image.setImageResource(R.drawable.image4);
			break;
		}
	}

	public void onNothingSelected(AdapterView<?> arg0) {
		//
	}

	public void PrintText(View view) {
		if (!checkClick.isClickEvent()) {
			return;
		}

		Spinner spinner_Image = (Spinner) findViewById(R.id.spinner_Image);
		int source = R.drawable.image1;
		switch (spinner_Image.getSelectedItemPosition()) {
		case 0:
			source = R.drawable.image1;
			break;
		case 1:
			source = R.drawable.image2;
			break;
		case 2:
			source = R.drawable.image3;
			break;
		case 3:
			source = R.drawable.image4;
			break;
		}
		getResources();

		Spinner spinner_paper_width = (Spinner) findViewById(R.id.spinner_paper_width);
		int paperWidth = 576;
		if (spinner_paper_width.getSelectedItem().toString().equals("2inch") ) {
			paperWidth = 384; // 2inch (384 dot)
		} else if (spinner_paper_width.getSelectedItem().toString().equals("3inch") ) {
			paperWidth = 576; // 3inch (576 dot)
		} else if (spinner_paper_width.getSelectedItem().toString().equals("4inch") ) {
			paperWidth = 832; // 4inch (832 dot)
		}

		boolean compressionEnable = false;
		boolean pageModeEnable = false;

		CheckBox checkbox_bitImage_CompressAPI = (CheckBox) findViewById(R.id.checkbox_bitImage_CompressAPI);
		if (checkbox_bitImage_CompressAPI.isChecked() == true) {
			compressionEnable = true;
		}

		CheckBox checkbox_bitImage_pageMode = (CheckBox) findViewById(R.id.checkbox_bitImage_pageMode);
		if (checkbox_bitImage_pageMode.isChecked() == true) {
			pageModeEnable = true;
		}

		String portName = PrinterTypeActivity.getPortName();
		String portSettings = PrinterTypeActivity.getPortSettings();

		if (portSettings.toUpperCase(Locale.US).contains("PORTABLE") && portSettings.toUpperCase(Locale.US).contains("ESCPOS")) {
			MiniPrinterFunctions.PrintBitmapImage(this, portName, portSettings, getResources(), source, paperWidth, compressionEnable, pageModeEnable);
		} else {
			RasterCommand rasterType = RasterCommand.Standard;
			if (portSettings.toUpperCase(Locale.US).contains("PORTABLE")) {
				rasterType = RasterCommand.Graphics;
			}
			PrinterFunctions.PrintBitmapImage(this, portName, portSettings, getResources(), source, paperWidth, compressionEnable, rasterType);
		}
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
					"<rightMov>d1...dk</rightMov> <rightMov_NOI2>The dots that should be printed.  When all vertical dots are printed the head moves horizontally to the next vertical set of dots</rightMov_NOI2><br/><br/><br/><br/><br/><br/>" +
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
					"<div class=\"div-table-rowCut\">" + "<div class=\"div-table-colRaster\"><center>8</center></div>" +
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
					"<div class=\"div-table-colRaster\"><center>-</center></div>" + "</div>" +
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
