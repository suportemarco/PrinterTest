package br.com.bematech;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Locale;

import br.com.bematech.PrinterFunctions.RasterCommand;

public class SampleReciptActivity extends Activity {

	private Context me = this;
	private Intent intent;
	private String strPrintArea = "";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		setContentView(R.layout.language);

		intent = getIntent();

		if (intent.getIntExtra("PRINTERTYPE", 1) == 2) { // RasterMode
			TextView titleSBCSText = (TextView) findViewById(R.id.text_SBCS);
			TextView titleDBCSText = (TextView) findViewById(R.id.text_DBSC);
			TextView titleDBCSDescriptionText = (TextView) findViewById(R.id.text_DBSC_Description);
			titleSBCSText.setVisibility(View.GONE);
			titleDBCSText.setVisibility(View.GONE);
			titleDBCSDescriptionText.setVisibility(View.GONE);

			setTitle("StarIOSDK - Raster Mode Samples");
		} else if ((intent.getIntExtra("PRINTERTYPE", 1) == 1)) { // Thermal LineMode
			TextView rasterDescriptionText = (TextView) findViewById(R.id.text_Raster_Description);
			rasterDescriptionText.setVisibility(View.GONE);

			if (PrinterTypeActivity.getPortSettings().toUpperCase(Locale.US).contains("PORTABLE")) {
				Button TraditionalChineseButton = (Button) findViewById(R.id.button_TraditionalChinese);
				TraditionalChineseButton.setVisibility(View.GONE);
			}

			setTitle("StarIOSDK - Line Mode Samples");
		} else if (intent.getIntExtra("PRINTERTYPE", 1) == 0) { // DotPrinter
			TextView rasterDescriptionText = (TextView) findViewById(R.id.text_Raster_Description);
			rasterDescriptionText.setVisibility(View.GONE);

			setTitle("StarIOSDK - Impact Dot Matrix Printer Samples");
		} else if (intent.getIntExtra("PRINTERTYPE", 1) == 3) { // Portable Printer
			TextView rasterDescriptionText = (TextView) findViewById(R.id.text_Raster_Description);
			rasterDescriptionText.setVisibility(View.GONE);

			TextView DBCSDescriptionText = (TextView) findViewById(R.id.text_DBSC_Description);
			DBCSDescriptionText.setText("These samples will require the correct DBCS character set to be loaded.");

			Button RussianButton = (Button) findViewById(R.id.button_Russian);
			RussianButton.setVisibility(View.GONE);
			
			Button SimplifiedChineseButton = (Button) findViewById(R.id.button_SimplifiedChinese);
			SimplifiedChineseButton.setVisibility(View.GONE);

			setTitle("StarIOSDK - Portable Printer Samples");
		}
	}

	public void English(View view) {
		if (!checkClick.isClickEvent())
			return;

		switch (intent.getIntExtra("PRINTERTYPE", 1)) {
		case 0:// Dot Printer
			PrinterFunctions.PrintSampleReceiptbyDotPrinter(me, PrinterTypeActivity.getPortName(), PrinterTypeActivity.getPortSettings());
			break;

		case 1:// Thermal Line Mode
		default:
			ArrayList<String> list = new ArrayList<String>();
			list.add(getResources().getString(R.string.printArea3inch));
			list.add(getResources().getString(R.string.printArea4inch));
			
			if (PrinterTypeActivity.getPortSettings().toUpperCase(Locale.US).contains("PORTABLE")) {
				list.add(0, getResources().getString(R.string.printArea2inch));
				strPrintArea = getResources().getString(R.string.printArea2inch);
			} else {
				strPrintArea = getResources().getString(R.string.printArea3inch);
			}
			
			final String item_list[] = list.toArray(new String[list.size()]);

			Builder printableAreaDialog = new Builder(this);
			printableAreaDialog.setIcon(android.R.drawable.checkbox_on_background);
			printableAreaDialog.setTitle("Paper Size List");
			printableAreaDialog.setCancelable(false);
			printableAreaDialog.setSingleChoiceItems(item_list, 0, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					strPrintArea = item_list[whichButton];
				}
			});
			printableAreaDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
					((AlertDialog) dialog).getButton(DialogInterface.BUTTON_NEGATIVE).setEnabled(false);

					String commandType = "Line";

					PrinterFunctions.PrintSampleReceipt(me, PrinterTypeActivity.getPortName(), PrinterTypeActivity.getPortSettings(), commandType, getResources(), strPrintArea, null);
				}
			});
			printableAreaDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
				}
			});
			printableAreaDialog.show();

			break;

		case 2:// Thermal Raster Mode
			RasterCommand tmpType = RasterCommand.Standard;
			
			ArrayList<String> list_raster = new ArrayList<String>();
			list_raster.add(getResources().getString(R.string.printArea3inch));
			list_raster.add(getResources().getString(R.string.printArea4inch));
			
			if (PrinterTypeActivity.getPortSettings().toUpperCase(Locale.US).contains("PORTABLE")) {
				list_raster.add(0, getResources().getString(R.string.printArea2inch));
				strPrintArea = getResources().getString(R.string.printArea2inch);
				tmpType = RasterCommand.Graphics;
			} else {
				strPrintArea = getResources().getString(R.string.printArea3inch);
			}
			
			final RasterCommand rasterType = tmpType;
			
			final String item_listRaster[] = list_raster.toArray(new String[list_raster.size()]);

			Builder printableAreaDialogRaster = new Builder(this);
			printableAreaDialogRaster.setIcon(android.R.drawable.checkbox_on_background);
			printableAreaDialogRaster.setTitle("Paper Size List");
			printableAreaDialogRaster.setCancelable(false);
			printableAreaDialogRaster.setSingleChoiceItems(item_listRaster, 0, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					strPrintArea = item_listRaster[whichButton];
				}
			});
			printableAreaDialogRaster.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
					((AlertDialog) dialog).getButton(DialogInterface.BUTTON_NEGATIVE).setEnabled(false);
					String commandType = "Raster";

					PrinterFunctions.PrintSampleReceipt(me, PrinterTypeActivity.getPortName(), PrinterTypeActivity.getPortSettings(), commandType, getResources(), strPrintArea, rasterType);
				}
			});
			printableAreaDialogRaster.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					//
				}
			});
			printableAreaDialogRaster.show();
			break;

		case 3:// Mobile Printer
			final String item_Mobilelist[] = new String[] { getResources().getString(R.string.printArea2inch), getResources().getString(R.string.printArea3inch), getResources().getString(R.string.printArea4inch), };

			strPrintArea = getResources().getString(R.string.printArea2inch);

			Builder printableAreaDialogMobile = new Builder(this);
			printableAreaDialogMobile.setIcon(android.R.drawable.checkbox_on_background);
			printableAreaDialogMobile.setTitle("Paper Size List");
			printableAreaDialogMobile.setCancelable(false);
			printableAreaDialogMobile.setSingleChoiceItems(item_Mobilelist, 0, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					strPrintArea = item_Mobilelist[whichButton];
				}
			});

			printableAreaDialogMobile.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
					((AlertDialog) dialog).getButton(DialogInterface.BUTTON_NEGATIVE).setEnabled(false);

					MiniPrinterFunctions.PrintSampleReceipt(me, PrinterTypeActivity.getPortName(), PrinterTypeActivity.getPortSettings(), strPrintArea);
				}
			});
			printableAreaDialogMobile.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
				}
			});
			printableAreaDialogMobile.show();
			break;

		}

	}

	public void French(View view) {
		if (!checkClick.isClickEvent())
			return;

		switch (intent.getIntExtra("PRINTERTYPE", 1)) {
		case 0:// Dot Printer
			PrinterFunctions.PrintSampleReceiptbyDotPrinter_French(me, PrinterTypeActivity.getPortName(), PrinterTypeActivity.getPortSettings());
			break;

		case 1:// Thermal Line Mode
		default:
			ArrayList<String> list = new ArrayList<String>();
			list.add(getResources().getString(R.string.printArea3inch));
			list.add(getResources().getString(R.string.printArea4inch));
			
			if (PrinterTypeActivity.getPortSettings().toUpperCase(Locale.US).contains("PORTABLE")) {
				list.add(0, getResources().getString(R.string.printArea2inch));
				strPrintArea = getResources().getString(R.string.printArea2inch);
			} else {
				strPrintArea = getResources().getString(R.string.printArea3inch);
			}
			
			final String item_list[] = list.toArray(new String[list.size()]);

			Builder printableAreaDialog = new Builder(this);
			printableAreaDialog.setIcon(android.R.drawable.checkbox_on_background);
			printableAreaDialog.setTitle("Paper Size List");
			printableAreaDialog.setCancelable(false);
			printableAreaDialog.setSingleChoiceItems(item_list, 0, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					strPrintArea = item_list[whichButton];
				}
			});
			printableAreaDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
					((AlertDialog) dialog).getButton(DialogInterface.BUTTON_NEGATIVE).setEnabled(false);

					String commandType = "Line";

					PrinterFunctions.PrintSampleReceipt_French(me, PrinterTypeActivity.getPortName(), PrinterTypeActivity.getPortSettings(), commandType, getResources(), strPrintArea, null);
				}
			});
			printableAreaDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
				}
			});
			printableAreaDialog.show();

			break;

		case 2:// Thermal Raster Mode
			RasterCommand tmpType = RasterCommand.Standard;

			ArrayList<String> list_raster = new ArrayList<String>();
			list_raster.add(getResources().getString(R.string.printArea3inch));
			list_raster.add(getResources().getString(R.string.printArea4inch));
			
			if (PrinterTypeActivity.getPortSettings().toUpperCase(Locale.US).contains("PORTABLE")) {
				list_raster.add(0, getResources().getString(R.string.printArea2inch));
				strPrintArea = getResources().getString(R.string.printArea2inch);
				tmpType = RasterCommand.Graphics;
			} else {
				strPrintArea = getResources().getString(R.string.printArea3inch);
			}
			
			final RasterCommand rasterType = tmpType;

			final String item_listRaster[] = list_raster.toArray(new String[list_raster.size()]);

			Builder printableAreaDialogRaster = new Builder(this);
			printableAreaDialogRaster.setIcon(android.R.drawable.checkbox_on_background);
			printableAreaDialogRaster.setTitle("Paper Size List");
			printableAreaDialogRaster.setCancelable(false);
			printableAreaDialogRaster.setSingleChoiceItems(item_listRaster, 0, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					strPrintArea = item_listRaster[whichButton];
				}
			});
			printableAreaDialogRaster.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
					((AlertDialog) dialog).getButton(DialogInterface.BUTTON_NEGATIVE).setEnabled(false);
					String commandType = "Raster";

					PrinterFunctions.PrintSampleReceipt_French(me, PrinterTypeActivity.getPortName(), PrinterTypeActivity.getPortSettings(), commandType, getResources(), strPrintArea, rasterType);
				}
			});
			printableAreaDialogRaster.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					//
				}
			});
			printableAreaDialogRaster.show();
			break;

		case 3:// Mobile Printer
			final String item_Mobilelist[] = new String[] { getResources().getString(R.string.printArea2inch), getResources().getString(R.string.printArea3inch), getResources().getString(R.string.printArea4inch), };

			strPrintArea = getResources().getString(R.string.printArea2inch);

			Builder printableAreaDialogMobile = new Builder(this);
			printableAreaDialogMobile.setIcon(android.R.drawable.checkbox_on_background);
			printableAreaDialogMobile.setTitle("Paper Size List");
			printableAreaDialogMobile.setCancelable(false);
			printableAreaDialogMobile.setSingleChoiceItems(item_Mobilelist, 0, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					strPrintArea = item_Mobilelist[whichButton];
				}
			});

			printableAreaDialogMobile.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
					((AlertDialog) dialog).getButton(DialogInterface.BUTTON_NEGATIVE).setEnabled(false);

					MiniPrinterFunctions.PrintSampleReceipt_French(me, PrinterTypeActivity.getPortName(), PrinterTypeActivity.getPortSettings(), strPrintArea);
				}
			});
			printableAreaDialogMobile.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
				}
			});
			printableAreaDialogMobile.show();
			break;

		}

	}

	public void Portuguese(View view) {
		if (!checkClick.isClickEvent())
			return;

		switch (intent.getIntExtra("PRINTERTYPE", 1)) {
		case 0:// Dot Printer
			PrinterFunctions.PrintSampleReceiptbyDotPrinter_Portuguese(me, PrinterTypeActivity.getPortName(), PrinterTypeActivity.getPortSettings());
			break;

		case 1:// Thermal Line Mode
		default:
			ArrayList<String> list = new ArrayList<String>();
			list.add(getResources().getString(R.string.printArea3inch));
			list.add(getResources().getString(R.string.printArea4inch));
			
			if (PrinterTypeActivity.getPortSettings().toUpperCase(Locale.US).contains("PORTABLE")) {
				list.add(0, getResources().getString(R.string.printArea2inch));
				strPrintArea = getResources().getString(R.string.printArea2inch);
			} else {
				strPrintArea = getResources().getString(R.string.printArea3inch);
			}
			
			final String item_list[] = list.toArray(new String[list.size()]);

			Builder printableAreaDialog = new Builder(this);
			printableAreaDialog.setIcon(android.R.drawable.checkbox_on_background);
			printableAreaDialog.setTitle("Paper Size List");
			printableAreaDialog.setCancelable(false);
			printableAreaDialog.setSingleChoiceItems(item_list, 0, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					strPrintArea = item_list[whichButton];
				}
			});
			printableAreaDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
					((AlertDialog) dialog).getButton(DialogInterface.BUTTON_NEGATIVE).setEnabled(false);

					String commandType = "Line";

					PrinterFunctions.PrintSampleReceipt_Portuguese(me, PrinterTypeActivity.getPortName(), PrinterTypeActivity.getPortSettings(), commandType, getResources(), strPrintArea, null);
				}
			});
			printableAreaDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
				}
			});
			printableAreaDialog.show();

			break;

		case 2:// Thermal Raster Mode
			RasterCommand tmpType = RasterCommand.Standard;

			ArrayList<String> list_raster = new ArrayList<String>();
			list_raster.add(getResources().getString(R.string.printArea3inch));
			list_raster.add(getResources().getString(R.string.printArea4inch));
			
			if (PrinterTypeActivity.getPortSettings().toUpperCase(Locale.US).contains("PORTABLE")) {
				list_raster.add(0, getResources().getString(R.string.printArea2inch));
				strPrintArea = getResources().getString(R.string.printArea2inch);
				tmpType = RasterCommand.Graphics;
			} else {
				strPrintArea = getResources().getString(R.string.printArea3inch);
			}
			
			final RasterCommand rasterType = tmpType;

			final String item_listRaster[] = list_raster.toArray(new String[list_raster.size()]);

			Builder printableAreaDialogRaster = new Builder(this);
			printableAreaDialogRaster.setIcon(android.R.drawable.checkbox_on_background);
			printableAreaDialogRaster.setTitle("Paper Size List");
			printableAreaDialogRaster.setCancelable(false);
			printableAreaDialogRaster.setSingleChoiceItems(item_listRaster, 0, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					strPrintArea = item_listRaster[whichButton];
				}
			});
			printableAreaDialogRaster.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
					((AlertDialog) dialog).getButton(DialogInterface.BUTTON_NEGATIVE).setEnabled(false);
					String commandType = "Raster";

					PrinterFunctions.PrintSampleReceipt_Portuguese(me, PrinterTypeActivity.getPortName(), PrinterTypeActivity.getPortSettings(), commandType, getResources(), strPrintArea, rasterType);
				}
			});
			printableAreaDialogRaster.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					//
				}
			});
			printableAreaDialogRaster.show();
			break;

		case 3:// Mobile Printer
			final String item_Mobilelist[] = new String[] { getResources().getString(R.string.printArea2inch), getResources().getString(R.string.printArea3inch), getResources().getString(R.string.printArea4inch), };

			strPrintArea = getResources().getString(R.string.printArea2inch);

			Builder printableAreaDialogMobile = new Builder(this);
			printableAreaDialogMobile.setIcon(android.R.drawable.checkbox_on_background);
			printableAreaDialogMobile.setTitle("Paper Size List");
			printableAreaDialogMobile.setCancelable(false);
			printableAreaDialogMobile.setSingleChoiceItems(item_Mobilelist, 0, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					strPrintArea = item_Mobilelist[whichButton];
				}
			});

			printableAreaDialogMobile.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
					((AlertDialog) dialog).getButton(DialogInterface.BUTTON_NEGATIVE).setEnabled(false);

					MiniPrinterFunctions.PrintSampleReceipt_Portuguese(me, PrinterTypeActivity.getPortName(), PrinterTypeActivity.getPortSettings(), strPrintArea);
				}
			});
			printableAreaDialogMobile.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
				}
			});
			printableAreaDialogMobile.show();
			break;

		}

	}

	public void Spanish(View view) {
		if (!checkClick.isClickEvent())
			return;

		switch (intent.getIntExtra("PRINTERTYPE", 1)) {
		case 0:// Dot Printer
			PrinterFunctions.PrintSampleReceiptbyDotPrinter_Spanish(me, PrinterTypeActivity.getPortName(), PrinterTypeActivity.getPortSettings());
			break;

		case 1:// Thermal Line Mode
		default:
			ArrayList<String> list = new ArrayList<String>();
			list.add(getResources().getString(R.string.printArea3inch));
			list.add(getResources().getString(R.string.printArea4inch));
			
			if (PrinterTypeActivity.getPortSettings().toUpperCase(Locale.US).contains("PORTABLE")) {
				list.add(0, getResources().getString(R.string.printArea2inch));
				strPrintArea = getResources().getString(R.string.printArea2inch);
			} else {
				strPrintArea = getResources().getString(R.string.printArea3inch);
			}
			
			final String item_list[] = list.toArray(new String[list.size()]);

			Builder printableAreaDialog = new Builder(this);
			printableAreaDialog.setIcon(android.R.drawable.checkbox_on_background);
			printableAreaDialog.setTitle("Paper Size List");
			printableAreaDialog.setCancelable(false);
			printableAreaDialog.setSingleChoiceItems(item_list, 0, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					strPrintArea = item_list[whichButton];
				}
			});
			printableAreaDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
					((AlertDialog) dialog).getButton(DialogInterface.BUTTON_NEGATIVE).setEnabled(false);

					String commandType = "Line";

					PrinterFunctions.PrintSampleReceipt_Spanish(me, PrinterTypeActivity.getPortName(), PrinterTypeActivity.getPortSettings(), commandType, getResources(), strPrintArea, null);
				}
			});
			printableAreaDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
				}
			});
			printableAreaDialog.show();

			break;

		case 2:// Thermal Raster Mode
			RasterCommand tmpType = RasterCommand.Standard;

			ArrayList<String> list_raster = new ArrayList<String>();
			list_raster.add(getResources().getString(R.string.printArea3inch));
			list_raster.add(getResources().getString(R.string.printArea4inch));
			
			if (PrinterTypeActivity.getPortSettings().toUpperCase(Locale.US).contains("PORTABLE")) {
				list_raster.add(0, getResources().getString(R.string.printArea2inch));
				strPrintArea = getResources().getString(R.string.printArea2inch);
				tmpType = RasterCommand.Graphics;

			} else {
				strPrintArea = getResources().getString(R.string.printArea3inch);
			}
			
			final RasterCommand rasterType = tmpType;

			final String item_listRaster[] = list_raster.toArray(new String[list_raster.size()]);

			Builder printableAreaDialogRaster = new Builder(this);
			printableAreaDialogRaster.setIcon(android.R.drawable.checkbox_on_background);
			printableAreaDialogRaster.setTitle("Paper Size List");
			printableAreaDialogRaster.setCancelable(false);
			printableAreaDialogRaster.setSingleChoiceItems(item_listRaster, 0, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					strPrintArea = item_listRaster[whichButton];
				}
			});
			printableAreaDialogRaster.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
					((AlertDialog) dialog).getButton(DialogInterface.BUTTON_NEGATIVE).setEnabled(false);
					String commandType = "Raster";

					PrinterFunctions.PrintSampleReceipt_Spanish(me, PrinterTypeActivity.getPortName(), PrinterTypeActivity.getPortSettings(), commandType, getResources(), strPrintArea, rasterType);
				}
			});
			printableAreaDialogRaster.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					//
				}
			});
			printableAreaDialogRaster.show();
			break;

		case 3:// Mobile Printer
			final String item_Mobilelist[] = new String[] { getResources().getString(R.string.printArea2inch), getResources().getString(R.string.printArea3inch), getResources().getString(R.string.printArea4inch), };

			strPrintArea = getResources().getString(R.string.printArea2inch);

			Builder printableAreaDialogMobile = new Builder(this);
			printableAreaDialogMobile.setIcon(android.R.drawable.checkbox_on_background);
			printableAreaDialogMobile.setTitle("Paper Size List");
			printableAreaDialogMobile.setCancelable(false);
			printableAreaDialogMobile.setSingleChoiceItems(item_Mobilelist, 0, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					strPrintArea = item_Mobilelist[whichButton];
				}
			});

			printableAreaDialogMobile.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
					((AlertDialog) dialog).getButton(DialogInterface.BUTTON_NEGATIVE).setEnabled(false);

					MiniPrinterFunctions.PrintSampleReceipt_Spanish(me, PrinterTypeActivity.getPortName(), PrinterTypeActivity.getPortSettings(), strPrintArea);
				}
			});
			printableAreaDialogMobile.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
				}
			});
			printableAreaDialogMobile.show();
			break;

		}

	}
	public void Russian(View view) {
		if (!checkClick.isClickEvent())
			return;

		switch (intent.getIntExtra("PRINTERTYPE", 1)) {
		case 0:// Dot Printer
			PrinterFunctions.PrintSampleReceiptbyDotPrinter_Russian(me, PrinterTypeActivity.getPortName(), PrinterTypeActivity.getPortSettings());
			break;

		case 1:// Thermal Line Mode
		default:
			ArrayList<String> list = new ArrayList<String>();
			list.add(getResources().getString(R.string.printArea3inch));
			list.add(getResources().getString(R.string.printArea4inch));
			
			if (PrinterTypeActivity.getPortSettings().toUpperCase(Locale.US).contains("PORTABLE")) {
				// This font only supported by only SM-L200
				list.clear();
				
				list.add(0, getResources().getString(R.string.printArea2inch));
				strPrintArea = getResources().getString(R.string.printArea2inch);
			} else {
				strPrintArea = getResources().getString(R.string.printArea3inch);
			}
			
			final String item_list[] = list.toArray(new String[list.size()]);

			Builder printableAreaDialog = new Builder(this);
			printableAreaDialog.setIcon(android.R.drawable.checkbox_on_background);
			printableAreaDialog.setTitle("Paper Size List");
			printableAreaDialog.setCancelable(false);
			printableAreaDialog.setSingleChoiceItems(item_list, 0, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					strPrintArea = item_list[whichButton];
				}
			});
			printableAreaDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
					((AlertDialog) dialog).getButton(DialogInterface.BUTTON_NEGATIVE).setEnabled(false);

					String commandType = "Line";

					PrinterFunctions.PrintSampleReceipt_Russian(me, PrinterTypeActivity.getPortName(), PrinterTypeActivity.getPortSettings(), commandType, getResources(), strPrintArea, null);
				}
			});
			printableAreaDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
				}
			});
			printableAreaDialog.show();

			break;

		case 2:// Thermal Raster Mode
			RasterCommand tmpType = RasterCommand.Standard;

			ArrayList<String> list_raster = new ArrayList<String>();
			list_raster.add(getResources().getString(R.string.printArea3inch));
			list_raster.add(getResources().getString(R.string.printArea4inch));
			
			if (PrinterTypeActivity.getPortSettings().toUpperCase(Locale.US).contains("PORTABLE")) {
				list_raster.add(0, getResources().getString(R.string.printArea2inch));
				strPrintArea = getResources().getString(R.string.printArea2inch);
				tmpType = RasterCommand.Graphics;
			} else {
				strPrintArea = getResources().getString(R.string.printArea3inch);
			}
			
			final RasterCommand rasterType = tmpType;

			final String item_listRaster[] = list_raster.toArray(new String[list_raster.size()]);

			Builder printableAreaDialogRaster = new Builder(this);
			printableAreaDialogRaster.setIcon(android.R.drawable.checkbox_on_background);
			printableAreaDialogRaster.setTitle("Paper Size List");
			printableAreaDialogRaster.setCancelable(false);
			printableAreaDialogRaster.setSingleChoiceItems(item_listRaster, 0, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					strPrintArea = item_listRaster[whichButton];
				}
			});
			printableAreaDialogRaster.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
					((AlertDialog) dialog).getButton(DialogInterface.BUTTON_NEGATIVE).setEnabled(false);
					String commandType = "Raster";

					PrinterFunctions.PrintSampleReceipt_Russian(me, PrinterTypeActivity.getPortName(), PrinterTypeActivity.getPortSettings(), commandType, getResources(), strPrintArea, rasterType);
				}
			});
			printableAreaDialogRaster.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					//
				}
			});
			printableAreaDialogRaster.show();
			break;

		case 3:// Mobile Printer
			
			break;

		}

	}

	public void Japanese(View view) {

		if (!checkClick.isClickEvent())
			return;

		switch (intent.getIntExtra("PRINTERTYPE", 1)) {
		case 0:// Dot Printer
			PrinterFunctions.PrintSampleReceiptJpbyDotPrinter(me, PrinterTypeActivity.getPortName(), PrinterTypeActivity.getPortSettings());
			break;

		case 1:// Thermal Line Mode
		default:
			ArrayList<String> list = new ArrayList<String>();
			list.add(getResources().getString(R.string.printArea3inch));
			list.add(getResources().getString(R.string.printArea4inch));
			
			if (PrinterTypeActivity.getPortSettings().toUpperCase(Locale.US).contains("PORTABLE")) {
				list.add(0, getResources().getString(R.string.printArea2inch));
				strPrintArea = getResources().getString(R.string.printArea2inch);
			} else {
				strPrintArea = getResources().getString(R.string.printArea3inch);
			}
			
			final String item_list[] = list.toArray(new String[list.size()]);

			Builder printableAreaDialog = new Builder(this);
			printableAreaDialog.setIcon(android.R.drawable.checkbox_on_background);
			printableAreaDialog.setTitle("Paper Size List");
			printableAreaDialog.setCancelable(false);
			printableAreaDialog.setSingleChoiceItems(item_list, 0, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					strPrintArea = item_list[whichButton];
				}
			});
			printableAreaDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
					((AlertDialog) dialog).getButton(DialogInterface.BUTTON_NEGATIVE).setEnabled(false);

					String commandType = "Line";
					PrinterFunctions.PrintSampleReceiptJp(me, PrinterTypeActivity.getPortName(), PrinterTypeActivity.getPortSettings(), commandType, strPrintArea, null);
				}
			});
			printableAreaDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
				}
			});
			printableAreaDialog.show();

			break;

		case 2:// Thermal Raster Mode
			RasterCommand tmpType = RasterCommand.Standard;

			ArrayList<String> list_raster = new ArrayList<String>();
			list_raster.add(getResources().getString(R.string.printArea3inch));
			list_raster.add(getResources().getString(R.string.printArea4inch));
			
			if (PrinterTypeActivity.getPortSettings().toUpperCase(Locale.US).contains("PORTABLE")) {
				list_raster.add(0, getResources().getString(R.string.printArea2inch));
				strPrintArea = getResources().getString(R.string.printArea2inch);
				tmpType = RasterCommand.Graphics;
			} else {
				strPrintArea = getResources().getString(R.string.printArea3inch);
			}

			final RasterCommand rasterType = tmpType;

			final String item_listRaster[] = list_raster.toArray(new String[list_raster.size()]);

			Builder printableAreaDialogRaster = new Builder(this);
			printableAreaDialogRaster.setIcon(android.R.drawable.checkbox_on_background);
			printableAreaDialogRaster.setTitle("Paper Size List");
			printableAreaDialogRaster.setCancelable(false);
			printableAreaDialogRaster.setSingleChoiceItems(item_listRaster, 0, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					strPrintArea = item_listRaster[whichButton];
				}
			});
			printableAreaDialogRaster.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
					((AlertDialog) dialog).getButton(DialogInterface.BUTTON_NEGATIVE).setEnabled(false);
					String commandType = "Raster";

					PrinterFunctions.PrintSampleReceiptJp(me, PrinterTypeActivity.getPortName(), PrinterTypeActivity.getPortSettings(), commandType, strPrintArea, rasterType);
				}
			});
			printableAreaDialogRaster.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					//
				}
			});
			printableAreaDialogRaster.show();
			break;

		case 3:// Mobile Printer
			final String item_Mobilelist[] = new String[] { getResources().getString(R.string.printArea2inch), getResources().getString(R.string.printArea3inch), getResources().getString(R.string.printArea4inch), };

			strPrintArea = getResources().getString(R.string.printArea2inch);

			Builder printableAreaDialogMobile = new Builder(this);
			printableAreaDialogMobile.setIcon(android.R.drawable.checkbox_on_background);
			printableAreaDialogMobile.setTitle("Paper Size List");
			printableAreaDialogMobile.setCancelable(false);
			printableAreaDialogMobile.setSingleChoiceItems(item_Mobilelist, 0, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					strPrintArea = item_Mobilelist[whichButton];
				}
			});

			printableAreaDialogMobile.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
					((AlertDialog) dialog).getButton(DialogInterface.BUTTON_NEGATIVE).setEnabled(false);

					MiniPrinterFunctions.PrintSampleReceiptJp(me, PrinterTypeActivity.getPortName(), PrinterTypeActivity.getPortSettings(), strPrintArea);
				}
			});
			printableAreaDialogMobile.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
				}
			});
			printableAreaDialogMobile.show();
			break;

		}

	}

	public void SimplifiedChinese(View view) {

		if (!checkClick.isClickEvent())
			return;

		switch (intent.getIntExtra("PRINTERTYPE", 1)) {
		case 0:// Dot Printer
			PrinterFunctions.PrintSampleReceiptCHSbyDotPrinter(me, PrinterTypeActivity.getPortName(), PrinterTypeActivity.getPortSettings());
			break;

		case 1:// Thermal Line Mode
		default:
			ArrayList<String> list = new ArrayList<String>();
			
			if (PrinterTypeActivity.getPortSettings().toUpperCase(Locale.US).contains("PORTABLE")) {
				list.add(getResources().getString(R.string.printArea2inch));
				strPrintArea = getResources().getString(R.string.printArea2inch);
			} else {
				list.add(getResources().getString(R.string.printArea3inch));
				list.add(getResources().getString(R.string.printArea4inch));
				strPrintArea = getResources().getString(R.string.printArea3inch);
			}
			
			final String item_list[] = list.toArray(new String[list.size()]);

			Builder printableAreaDialog = new Builder(this);
			printableAreaDialog.setIcon(android.R.drawable.checkbox_on_background);
			printableAreaDialog.setTitle("Paper Size List");
			printableAreaDialog.setCancelable(false);
			printableAreaDialog.setSingleChoiceItems(item_list, 0, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					strPrintArea = item_list[whichButton];
				}
			});
			printableAreaDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
					((AlertDialog) dialog).getButton(DialogInterface.BUTTON_NEGATIVE).setEnabled(false);

					String commandType = "Line";
					PrinterFunctions.PrintSampleReceiptCHS(me, PrinterTypeActivity.getPortName(), PrinterTypeActivity.getPortSettings(), commandType, strPrintArea, null);
				}
			});
			printableAreaDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
				}
			});
			printableAreaDialog.show();

			break;

		case 2:// Thermal Raster Mode
			RasterCommand tmpType = RasterCommand.Standard;

			ArrayList<String> list_raster = new ArrayList<String>();
			list_raster.add(getResources().getString(R.string.printArea3inch));
			list_raster.add(getResources().getString(R.string.printArea4inch));
			
			if (PrinterTypeActivity.getPortSettings().toUpperCase(Locale.US).contains("PORTABLE")) {
				list_raster.add(0, getResources().getString(R.string.printArea2inch));
				strPrintArea = getResources().getString(R.string.printArea2inch);
				tmpType = RasterCommand.Graphics;
			} else {
				strPrintArea = getResources().getString(R.string.printArea3inch);
			}
			
			final RasterCommand rasterType = tmpType;

			final String item_listRaster[] = list_raster.toArray(new String[list_raster.size()]);

			Builder printableAreaDialogRaster = new Builder(this);
			printableAreaDialogRaster.setIcon(android.R.drawable.checkbox_on_background);
			printableAreaDialogRaster.setTitle("Paper Size List");
			printableAreaDialogRaster.setCancelable(false);
			printableAreaDialogRaster.setSingleChoiceItems(item_listRaster, 0, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					strPrintArea = item_listRaster[whichButton];
				}
			});
			printableAreaDialogRaster.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
					((AlertDialog) dialog).getButton(DialogInterface.BUTTON_NEGATIVE).setEnabled(false);
					String commandType = "Raster";

					PrinterFunctions.PrintSampleReceiptCHS(me, PrinterTypeActivity.getPortName(), PrinterTypeActivity.getPortSettings(), commandType, strPrintArea, rasterType);
				}
			});
			printableAreaDialogRaster.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					//
				}
			});
			printableAreaDialogRaster.show();
			break;

		case 3:// Mobile Printer

			break;

		}

	}

	public void TraditionalChinese(View view) {
		if (!checkClick.isClickEvent()) {
			return;
		}

		switch (intent.getIntExtra("PRINTERTYPE", 1)) {
		case 0:// Dot Printer
			PrinterFunctions.PrintSampleReceiptCHTbyDotPrinter(me, PrinterTypeActivity.getPortName(), PrinterTypeActivity.getPortSettings());
			break;

		case 1:// Thermal Line Mode
		default:
			// String commandType = "Line";
			// String strPrintArea = "3inch (80mm)";
			PrinterFunctions.PrintSampleReceiptCHT(me, PrinterTypeActivity.getPortName(), PrinterTypeActivity.getPortSettings(), "Line", getResources(), "3inch (80mm)", null);

			break;

		case 2:// Thermal Raster Mode
			RasterCommand tmpType = RasterCommand.Standard;

			ArrayList<String> list_raster = new ArrayList<String>();
			list_raster.add(getResources().getString(R.string.printArea3inch));
			list_raster.add(getResources().getString(R.string.printArea4inch));
			
			if (PrinterTypeActivity.getPortSettings().toUpperCase(Locale.US).contains("PORTABLE")) {
				list_raster.add(0, getResources().getString(R.string.printArea2inch));
				strPrintArea = getResources().getString(R.string.printArea2inch);
				tmpType = RasterCommand.Graphics;
			} else {
				strPrintArea = getResources().getString(R.string.printArea3inch);
			}
			
			final RasterCommand rasterType = tmpType;

			final String item_listRaster[] = list_raster.toArray(new String[list_raster.size()]);

			Builder printableAreaDialogRaster = new Builder(this);
			printableAreaDialogRaster.setIcon(android.R.drawable.checkbox_on_background);
			printableAreaDialogRaster.setTitle("Paper Size List");
			printableAreaDialogRaster.setCancelable(false);
			printableAreaDialogRaster.setSingleChoiceItems(item_listRaster, 0, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					strPrintArea = item_listRaster[whichButton];
				}
			});
			printableAreaDialogRaster.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
					((AlertDialog) dialog).getButton(DialogInterface.BUTTON_NEGATIVE).setEnabled(false);
					String commandType = "Raster";

					PrinterFunctions.PrintSampleReceiptCHT(me, PrinterTypeActivity.getPortName(), PrinterTypeActivity.getPortSettings(), commandType, getResources(), strPrintArea, rasterType);
				}
			});
			printableAreaDialogRaster.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					//
				}
			});
			printableAreaDialogRaster.show();
			break;

		case 3:// Mobile Printer
			final String item_Mobilelist[] = new String[] { getResources().getString(R.string.printArea2inch), getResources().getString(R.string.printArea3inch), getResources().getString(R.string.printArea4inch), };

			strPrintArea = getResources().getString(R.string.printArea2inch);

			Builder printableAreaDialogMobile = new Builder(this);
			printableAreaDialogMobile.setIcon(android.R.drawable.checkbox_on_background);
			printableAreaDialogMobile.setTitle("Paper Size List");
			printableAreaDialogMobile.setCancelable(false);
			printableAreaDialogMobile.setSingleChoiceItems(item_Mobilelist, 0, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					strPrintArea = item_Mobilelist[whichButton];
				}
			});

			printableAreaDialogMobile.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
					((AlertDialog) dialog).getButton(DialogInterface.BUTTON_NEGATIVE).setEnabled(false);

					MiniPrinterFunctions.PrintSampleReceiptCHT(me, PrinterTypeActivity.getPortName(), PrinterTypeActivity.getPortSettings(), strPrintArea);
				}
			});
			printableAreaDialogMobile.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
				}
			});
			printableAreaDialogMobile.show();
			break;
		}

	}

}