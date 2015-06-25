package br.com.bematech;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import com.starmicronics.stario.StarIOPort;
import com.starmicronics.stario.StarIOPortException;
import com.starmicronics.stario.StarPrinterStatus;

import java.io.UnsupportedEncodingException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import br.com.bematech.RasterDocument.RasPageEndMode;
import br.com.bematech.RasterDocument.RasSpeed;
import br.com.bematech.RasterDocument.RasTopMargin;

public class PrinterFunctions {
	public enum NarrowWide {
		_2_6, _3_9, _4_12, _2_5, _3_8, _4_10, _2_4, _3_6, _4_8
	};

	public enum BarCodeOption {
		No_Added_Characters_With_Line_Feed, Adds_Characters_With_Line_Feed, No_Added_Characters_Without_Line_Feed, Adds_Characters_Without_Line_Feed
	}

	public enum Min_Mod_Size {
		_2_dots, _3_dots, _4_dots
	};

	public enum NarrowWideV2 {
		_2_5, _4_10, _6_15, _2_4, _4_8, _6_12, _2_6, _3_9, _4_12
	};

	public enum CorrectionLevelOption {
		Low, Middle, Q, High
	};

	public enum Model {
		Model1, Model2
	};

	public enum Limit {
		USE_LIMITS, USE_FIXED
	};

	public enum CutType {
		FULL_CUT, PARTIAL_CUT, FULL_CUT_FEED, PARTIAL_CUT_FEED
	};

	public enum Alignment {
		Left, Center, Right
	};

	public enum RasterCommand {
		Standard, Graphics
	};
	
	private static StarIOPort portForMoreThanOneFunction = null;
	
	private static int printableArea = 576; // for raster data

	/**
	 * This function is used to print a PDF417 barcode to standard Star POS printers
	 * 
	 * @param context
	 *     Activity for displaying messages to the user
	 * @param portName
	 *     Port name to use for communication. This should be (TCP:<IPAddress>)
	 * @param portSettings
	 *     Should be blank
	 * @param limit
	 *     Selection of the Method to use specifying the barcode size. This is either 0 or 1. 0 is Use Limit method and 1 is Use Fixed method. See section 3-122 of the manual (Rev 1.12).
	 * @param p1
	 *     The vertical proportion to use. The value changes with the limit select. See section 3-122 of the manual (Rev 1.12).
	 * @param p2
	 *     The horizontal proportion to use. The value changes with the limit select. See section 3-122 of the manual (Rev 1.12).
	 * @param securityLevel
	 *     This represents how well the barcode can be recovered if it is damaged. This value should be 0 to 8.
	 * @param xDirection
	 *     Specifies the X direction size. This value should be from 1 to 10. It is recommended that the value be 2 or less.
	 * @param aspectRatio
	 *     Specifies the ratio of the PDF417 barcode. This values should be from 1 to 10. It is recommended that this value be 2 or less.
	 * @param barcodeData
	 *     Specifies the characters in the PDF417 barcode.
	 */
	public static void PrintPDF417Code(Context context, String portName, String portSettings, Limit limit, byte p1, byte p2, byte securityLevel, byte xDirection, byte aspectRatio, byte[] barcodeData) {
		ArrayList<byte[]> commands = new ArrayList<byte[]>();

		byte[] setBarCodeSize = new byte[] { 0x1b, 0x1d, 0x78, 0x53, 0x30, 0x00, 0x00, 0x00 };
		switch (limit) {
		case USE_LIMITS:
			setBarCodeSize[5] = 0;
			break;
		case USE_FIXED:
			setBarCodeSize[5] = 1;
			break;
		}

		setBarCodeSize[6] = p1;
		setBarCodeSize[7] = p2;
		commands.add(setBarCodeSize);

		commands.add(new byte[] { 0x1b, 0x1d, 0x78, 0x53, 0x31, securityLevel });

		commands.add(new byte[] { 0x1b, 0x1d, 0x78, 0x53, 0x32, xDirection });

		commands.add(new byte[] { 0x1b, 0x1d, 0x78, 0x53, 0x33, aspectRatio });

		byte[] setBarcodeData = new byte[6 + barcodeData.length];
		setBarcodeData[0] = 0x1b;
		setBarcodeData[1] = 0x1d;
		setBarcodeData[2] = 0x78;
		setBarcodeData[3] = 0x44;
		setBarcodeData[4] = (byte) (barcodeData.length % 256);
		setBarcodeData[5] = (byte) (barcodeData.length / 256);
		System.arraycopy(barcodeData, 0, setBarcodeData, 6, barcodeData.length);
		commands.add(setBarcodeData);

		commands.add(new byte[] { 0x1b, 0x1d, 0x78, 0x50 });

		sendCommand(context, portName, portSettings, commands);
	}

	/**
	 * This function is used to print a QR Code on standard Star POS printers
	 * 
	 * @param context
	 *     Activity for displaying messages to the user
	 * @param portName
	 *     Port name to use for communication. This should be (TCP:<IPAddress>)
	 * @param portSettings
	 *     Should be blank
	 * @param correctionLevel
	 *     The correction level for the QR Code. The correction level can be 7, 15, 25, or 30. See section 3-129 (Rev. 1.12).
	 * @param model
	 *     The model to use when printing the QR Code. See section 3-129 (Rev. 1.12).
	 * @param cellSize
	 *     The cell size of the QR Code. The value of this should be between 1 and 8. It is recommended that this value is 2 or less.
	 * @param barCodeData
	 *     Specifies the characters in the QR Code.
	 */
	public static void PrintQrCode(Context context, String portName, String portSettings, CorrectionLevelOption correctionLevel, Model model, byte cellSize, byte[] barCodeData) {
		ArrayList<byte[]> commands = new ArrayList<byte[]>();

		byte[] modelCommand = new byte[] { 0x1b, 0x1d, 0x79, 0x53, 0x30, 0x00 };
		switch (model) {
		case Model1:
			modelCommand[5] = 1;
			break;
		case Model2:
			modelCommand[5] = 2;
			break;
		}

		commands.add(modelCommand);

		byte[] correctionLevelCommand = new byte[] { 0x1b, 0x1d, 0x79, 0x53, 0x31, 0x00 };
		switch (correctionLevel) {
		case Low:
			correctionLevelCommand[5] = 0;
			break;
		case Middle:
			correctionLevelCommand[5] = 1;
			break;
		case Q:
			correctionLevelCommand[5] = 2;
			break;
		case High:
			correctionLevelCommand[5] = 3;
			break;
		}
		commands.add(correctionLevelCommand);

		commands.add(new byte[] { 0x1b, 0x1d, 0x79, 0x53, 0x32, cellSize });

		// Add BarCode data
		commands.add(new byte[] { 0x1b, 0x1d, 0x79, 0x44, 0x31, 0x00, (byte) (barCodeData.length % 256), (byte) (barCodeData.length / 256) });
		commands.add(barCodeData);
		commands.add(new byte[] { 0x1b, 0x1d, 0x79, 0x50 } );

		sendCommand(context, portName, portSettings, commands);
	}

	/**
	 * This function opens the cash drawer connected to the printer This function just send the byte 0x07 to the printer which is the open cashdrawer command It is not possible that the OpenCashDraware and OpenCashDrawer2 are running at the same time.
	 * 
	 * @param context
	 *     Activity for displaying messages to the user
	 * @param portName
	 *     Port name to use for communication. This should be (TCP:<IPAddress>)
	 * @param portSettings
	 *     Should be blank
	 */
	public static void OpenCashDrawer(Context context, String portName, String portSettings) {
		ArrayList<byte[]> commands = new ArrayList<byte[]>();

		commands.add(new byte[] { 0x07 });

		sendCommand(context, portName, portSettings, commands);
	}

	/**
	 * This function opens the cash drawer connected to the printer This function just send the byte 0x1a to the printer which is the open cashdrawer command The OpenCashDrawer2, delay time and power-on time is 200msec fixed. It is not possible that the OpenCashDraware and OpenCashDrawer2 are running at the same time.
	 * 
	 * @param context
	 *     Activity for displaying messages to the user
	 * @param portName
	 *     Port name to use for communication. This should be (TCP:<IPAddress>)
	 * @param portSettings
	 *     Should be blank
	 */
	public static void OpenCashDrawer2(Context context, String portName, String portSettings) {
		ArrayList<byte[]> commands = new ArrayList<byte[]>();

		commands.add(new byte[] { 0x1a });

		sendCommand(context, portName, portSettings, commands);
	}

	/**
	 * This function checks the Firmware Informatin of the printer
	 * 
	 * @param context
	 *     Activity for displaying messages to the user
	 * @param portName
	 *     Port name to use for communication. This should be (TCP:<IPAddress>)
	 * @param portSettings
	 *     Should be blank
	 */
	public static void CheckFirmwareVersion(Context context, String portName, String portSettings) {
		StarIOPort port = null;
		try {
			/*
			 * using StarIOPort3.1.jar (support USB Port) Android OS Version: upper 2.2
			 */
			port = StarIOPort.getPort(portName, portSettings, 10000, context);
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

			String message = "Model Name:" + modelName;
			message += "\nFirmware Version:" + firmwareVersion;

			Builder dialog = new Builder(context);
			dialog.setNegativeButton("OK", null);
			AlertDialog alert = dialog.create();
			alert.setTitle("Firmware Information");
			alert.setMessage(message);
			alert.setCancelable(false);
			alert.show();

		} catch (StarIOPortException e) {
			Builder dialog = new Builder(context);
			dialog.setNegativeButton("OK", null);
			AlertDialog alert = dialog.create();
			alert.setTitle("Failure");
			alert.setMessage("Failed to connect to printer");
			alert.setCancelable(false);
			alert.show();
		} finally {
			if (port != null) {
				try {
					StarIOPort.releasePort(port);
				} catch (StarIOPortException e) {
				}
			}
		}
	}

	/**
	 * This function checks the DipSwitch Informatin of the DK-AirCash
	 * 
	 * @param context
	 *     Activity for displaying messages to the user
	 * @param portName
	 *     Port name to use for communication. This should be (TCP:<IPAddress>)
	 * @param portSettings
	 *     Should be blank
	 */
	public static void CheckDipSwitchSettings(Context context, String portName, String portSettings) {
		StarIOPort port = null;
		try {
			/*
			 * using StarIOPort3.1.jar (support USB Port) Android OS Version: upper 2.2
			 */
			port = StarIOPort.getPort(portName, portSettings, 10000, context);
			/*
			 * using StarIOPort.jar Android OS Version: under 2.1 port = StarIOPort.getPort(portName, portSettings, 10000);
			 */

			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
			}

			Map<String, Boolean> dipswinfo = port.getDipSwitchInformation();

			boolean dipsw11 = dipswinfo.get("DIPSW11");		// Interface selection
			// boolean dipsw12 = dipswinfo.get("DIPSW12");	//Reserved
			boolean dipsw13 = dipswinfo.get("DIPSW13");		// bluetooth communication setting initialization
			boolean dipsw14 = dipswinfo.get("DIPSW14");		// Wired LAN communication setting initialization
			boolean dipsw15 = dipswinfo.get("DIPSW15");		// Set DHCP timeout
			// boolean dipsw16 = dipswinfo.get("DIPSW16");	//Reserved
			// boolean dipsw17 = dipswinfo.get("DIPSW17");	//Reserved
			// boolean dipsw18 = dipswinfo.get("DIPSW18");	//Reserved

			// boolean dipsw21 = dipswinfo.get("DIPSW21");	//Reserved
			// boolean dipsw22 = dipswinfo.get("DIPSW22");	//Reserved
			boolean dipsw23 = dipswinfo.get("DIPSW23");		// Drawer signal polarity selection
			// boolean dipsw24 = dipswinfo.get("DIPSW24");	//Reserved
			// boolean dipsw25 = dipswinfo.get("DIPSW25");	//Reserved
			// boolean dipsw26 = dipswinfo.get("DIPSW26");	//Reserved
			// boolean dipsw27 = dipswinfo.get("DIPSW27");	//Reserved
			boolean dipsw28 = dipswinfo.get("DIPSW28");		// Boot Program Rewrite

			String message = "--DipSwitch1--";
			message += "\n [1-1]　" + (dipsw11 ? context.getResources().getString(R.string.LAN) : context.getResources().getString(R.string.Bluetooth));
			message += "\n [1-2]　" + context.getResources().getString(R.string.dipsw_Reserved);
			message += "\n [1-3]　" + (dipsw13 ? context.getResources().getString(R.string.Disable) : context.getResources().getString(R.string.Enable));
			message += "\n [1-4]　" + (dipsw14 ? context.getResources().getString(R.string.Disable) : context.getResources().getString(R.string.Enable));
			message += "\n [1-5]　" + (dipsw15 ? context.getResources().getString(R.string.DHCP_timeout_20seconds) : context.getResources().getString(R.string.DHCP_timeout_No_timeout));
			message += "\n [1-6]　" + context.getResources().getString(R.string.dipsw_Reserved);
			message += "\n [1-7]　" + context.getResources().getString(R.string.dipsw_Reserved);
			message += "\n [1-8]　" + context.getResources().getString(R.string.dipsw_Reserved);
			message += "\n";
			message += "\n--DipSwitch2--";
			message += "\n [2-1]　" + context.getResources().getString(R.string.dipsw_Reserved);
			message += "\n [2-2]　" + context.getResources().getString(R.string.dipsw_Reserved);
			message += "\n [2-3]　" + (dipsw23 ? context.getResources().getString(R.string.Drawer_polarity_High_Open) : context.getResources().getString(R.string.Drawer_polarity_Low_Open));
			message += "\n [2-4]　" + context.getResources().getString(R.string.dipsw_Reserved);
			message += "\n [2-5]　" + context.getResources().getString(R.string.dipsw_Reserved);
			message += "\n [2-6]　" + context.getResources().getString(R.string.dipsw_Reserved);
			message += "\n [2-7]　" + context.getResources().getString(R.string.dipsw_Reserved);
			message += "\n [2-8]　" + (dipsw28 ? context.getResources().getString(R.string.Disable) : context.getResources().getString(R.string.Enable));
			message += "\n";
			message += "\n (*)Please refer to the specifications for details.";

			Builder dialog = new Builder(context);
			dialog.setNegativeButton("OK", null);
			AlertDialog alert = dialog.create();
			alert.setTitle("DIP Switch Settings Information");
			alert.setMessage(message);
			alert.setCancelable(false);
			alert.show();

		} catch (StarIOPortException e) {
			Builder dialog = new Builder(context);
			dialog.setNegativeButton("OK", null);
			AlertDialog alert = dialog.create();
			alert.setTitle("Failure");
			alert.setMessage(e.getMessage());
			alert.setCancelable(false);
			alert.show();
		} finally {
			if (port != null) {
				try {
					StarIOPort.releasePort(port);
				} catch (StarIOPortException e) {
				}
			}
		}
	}

	/**
	 * This function checks the status of the printer
	 * 
	 * @param context
	 *     Activity for displaying messages to the user
	 * @param portName
	 *     Port name to use for communication. This should be (TCP:<IPAddress>)
	 * @param portSettings
	 *     Should be blank
	 * @param sensorActiveHigh
	 *     boolean variable to tell the sensor active of CashDrawer which is High
	 */
	public static void CheckStatus(Context context, String portName, String portSettings, boolean sensorActiveHigh) {
		StarIOPort port = null;
		try {
			/*
			 * using StarIOPort3.1.jar (support USB Port) Android OS Version: upper 2.2
			 */
			port = StarIOPort.getPort(portName, portSettings, 10000, context);
			/*
			 * using StarIOPort.jar Android OS Version: under 2.1 port = StarIOPort.getPort(portName, portSettings, 10000);
			 */

			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
			}

			StarPrinterStatus status = port.retreiveStatus();

			if (status.offline == false) {
				String message = "Printer is online";

				if (status.compulsionSwitch == false) {
					if (true == sensorActiveHigh) {
						message += "\nCash Drawer: Close";
					} else {
						message += "\nCash Drawer: Open";
					}
				} else {
					if (true == sensorActiveHigh) {
						message += "\nCash Drawer: Open";
					} else {
						message += "\nCash Drawer: Close";
					}
				}

				Builder dialog = new Builder(context);
				dialog.setNegativeButton("OK", null);
				AlertDialog alert = dialog.create();
				alert.setTitle("Printer");
				alert.setMessage(message);
				alert.setCancelable(false);
				alert.show();
			} else {
				String message = "Printer is offline";

				if (status.receiptPaperEmpty == true) {
					message += "\nPaper is empty";
				}

				if (status.coverOpen == true) {
					message += "\nCover is open";
				}

				if (status.compulsionSwitch == false) {
					if (true == sensorActiveHigh) {
						message += "\nCash Drawer: Close";
					} else {
						message += "\nCash Drawer: Open";
					}
				} else {
					if (true == sensorActiveHigh) {
						message += "\nCash Drawer: Open";
					} else {
						message += "\nCash Drawer: Close";
					}
				}

				Builder dialog = new Builder(context);
				dialog.setNegativeButton("OK", null);
				AlertDialog alert = dialog.create();
				alert.setTitle("Printer");
				alert.setMessage(message);
				alert.setCancelable(false);
				alert.show();
			}

		} catch (StarIOPortException e) {
			Builder dialog = new Builder(context);
			dialog.setNegativeButton("OK", null);
			AlertDialog alert = dialog.create();
			alert.setTitle("Failure");
			alert.setMessage("Failed to connect to printer");
			alert.setCancelable(false);
			alert.show();
		} finally {
			if (port != null) {
				try {
					StarIOPort.releasePort(port);
				} catch (StarIOPortException e) {
				}
			}
		}
	}

	/**
	 * This function checks the status of the printer which does not have compulsion switch
	 * 
	 * @param context
	 *     Activity for displaying messages to the user
	 * @param portName
	 *     Port name to use for communication. This should be (TCP:<IPAddress>)
	 * @param portSettings
	 *     Should be blank
	 * @param sensorActiveHigh
	 *     boolean variable to tell the sensor active of CashDrawer which is High
	 */
	public static void CheckStatus(Context context, String portName, String portSettings) {
		StarIOPort port = null;
		try {
			/*
			 * using StarIOPort3.1.jar (support USB Port) Android OS Version: upper 2.2
			 */
			port = StarIOPort.getPort(portName, portSettings, 10000, context);
			/*
			 * using StarIOPort.jar Android OS Version: under 2.1 port = StarIOPort.getPort(portName, portSettings, 10000);
			 */

			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
			}

			StarPrinterStatus status = port.retreiveStatus();

			if (status.offline == false) {
				String message = "Printer is online";

				Builder dialog = new Builder(context);
				dialog.setNegativeButton("OK", null);
				AlertDialog alert = dialog.create();
				alert.setTitle("Printer");
				alert.setMessage(message);
				alert.setCancelable(false);
				alert.show();
			} else {
				String message = "Printer is offline";

				if (status.receiptPaperEmpty == true) {
					message += "\nPaper is empty";
				}

				if (status.coverOpen == true) {
					message += "\nCover is open";
				}

				Builder dialog = new Builder(context);
				dialog.setNegativeButton("OK", null);
				AlertDialog alert = dialog.create();
				alert.setTitle("Printer");
				alert.setMessage(message);
				alert.setCancelable(false);
				alert.show();
			}

		} catch (StarIOPortException e) {
			Builder dialog = new Builder(context);
			dialog.setNegativeButton("OK", null);
			AlertDialog alert = dialog.create();
			alert.setTitle("Failure");
			alert.setMessage("Failed to connect to printer");
			alert.setCancelable(false);
			alert.show();
		} finally {
			if (port != null) {
				try {
					StarIOPort.releasePort(port);
				} catch (StarIOPortException e) {
				}
			}
		}
	}

	/**
	 * This function enable USB serial number of the printer
	 * 
	 * @param context
	 *     Activity for displaying messages to the user
	 * @param portName
	 *     Port name to use for communication. This should be (USB:)
	 * @param portSettings
	 *     Should be blank
	 */
	public static void EnableUSBSerialNumber(Context context, String portName, String portSettings, byte[] serialNumber) {
		StarIOPort port = null;
		try {
			/*
			 * using StarIOPort3.1.jar (support USB Port) Android OS Version: upper 2.2
			 */
			port = StarIOPort.getPort(portName, portSettings, 10000, context);
			/*
			 * using StarIOPort.jar Android OS Version: under 2.1 port = StarIOPort.getPort(portName, portSettings, 10000);
			 */

			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
			}

			ArrayList<byte[]> setSerialNumCommand = new ArrayList<byte[]>();

			setSerialNumCommand.add(new byte[] { 0x1b, 0x23, 0x23, 0x57, 0x38, 0x2c });
			for (int i = 0; i < 8 - serialNumber.length; i++) {
				setSerialNumCommand.add(new byte[] { 0x30 });	// Fill in the top at "0" to be a total 8 digits.
			}
			setSerialNumCommand.add(serialNumber);
			setSerialNumCommand.add(new byte[] { 0x0a, 0x00 });

			byte[] commandToSendToPrinter = convertFromListByteArrayTobyteArray(setSerialNumCommand);
			port.writePort(commandToSendToPrinter, 0, commandToSendToPrinter.length);

			// Wait for 5 seconds until printer recover from software reset
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
			}

			byte[] mswenableCommand = new byte[] { 0x1b, 0x1d, 0x23, 0x2b, 0x43, 0x30, 0x30, 0x30, 0x32, 0x0a, 0x00, 0x1b, 0x1d, 0x23, 0x57, 0x30, 0x30, 0x30, 0x30, 0x30, 0x0a, 0x00 };

			port.writePort(mswenableCommand, 0, mswenableCommand.length);
			
			// Wait for 3 seconds until printer recover from software reset
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
			}

		} catch (StarIOPortException e) {
			Builder dialog = new Builder(context);
			dialog.setNegativeButton("OK", null);
			AlertDialog alert = dialog.create();
			alert.setTitle("Failure");
			alert.setMessage("Failed to connect to printer");
			alert.setCancelable(false);
			alert.show();
		} finally {
			if (port != null) {
				try {
					StarIOPort.releasePort(port);
				} catch (StarIOPortException e) {
				}
			}
		}
	}

	/**
	 * This function disable USB serial number of the printer
	 * 
	 * @param context
	 *     Activity for displaying messages to the user
	 * @param portName
	 *     Port name to use for communication. This should be (USB:)
	 * @param portSettings
	 *     Should be blank
	 */
	public static void DisableUSBSerialNumber(Context context, String portName, String portSettings) {
		StarIOPort port = null;
		try {
			/*
			 * using StarIOPort3.1.jar (support USB Port) Android OS Version: upper 2.2
			 */
			port = StarIOPort.getPort(portName, portSettings, 10000, context);
			/*
			 * using StarIOPort.jar Android OS Version: under 2.1 port = StarIOPort.getPort(portName, portSettings, 10000);
			 */

			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
			}

			byte[] clearSerialNumCommand = new byte[] { 0x1b, 0x23, 0x23, 0x57, 0x38, 0x2c, '?', '?', '?', '?', '?', '?', '?', '?', 0x0a, 0x00 };

			port.writePort(clearSerialNumCommand, 0, clearSerialNumCommand.length);

			// Wait for 5 seconds until printer recover from software reset
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
			}

			byte[] mswedisableCommand = new byte[] { 0x1b, 0x1d, 0x23, 0x2d, 0x43, 0x30, 0x30, 0x30, 0x32, 0x0a, 0x00, 0x1b, 0x1d, 0x23, 0x57, 0x30, 0x30, 0x30, 0x30, 0x30, 0x0a, 0x00 };

			port.writePort(mswedisableCommand, 0, mswedisableCommand.length);
			
			// Wait for 3 seconds until printer recover from software reset
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
			}

		} catch (StarIOPortException e) {
			Builder dialog = new Builder(context);
			dialog.setNegativeButton("OK", null);
			AlertDialog alert = dialog.create();
			alert.setTitle("Failure");
			alert.setMessage("Failed to connect to printer");
			alert.setCancelable(false);
			alert.show();
		} finally {
			if (port != null) {
				try {
					StarIOPort.releasePort(port);
				} catch (StarIOPortException e) {
				}
			}
		}
	}

	/**
	 * This function is used to print barcodes in 39 format
	 * 
	 * @param context
	 *     Activity for displaying messages to the user
	 * @param portName
	 *     Port name to use for communication. This should be (TCP:<IPAddress>)
	 * @param portSettings
	 *     Should be blank
	 * @param barcodeData
	 *     These are the characters that will be printed in the barcode. The characters available for this bar code are listed in section 3-43 (Rev. 1.12).
	 * @param option
	 *     This tells the printer to put characters under the printed barcode or not. This may also be used to line feed after the barcode is printed.
	 * @param height
	 *     The height of the barcode. This is measured in pixels
	 * @param width
	 *     The width of the barcode. This value should be between 1 to 9. See section 3-42 (Rev. 1.12) for more information on the values.
	 */
	public static void PrintCode39(Context context, String portName, String portSettings, byte[] barcodeData, BarCodeOption option, byte height, NarrowWide width) {
		ArrayList<byte[]> commands = new ArrayList<byte[]>();

		byte n1 = 0x34;
		byte n2 = 0;
		switch (option) {
		case No_Added_Characters_With_Line_Feed:
			n2 = 49;
			break;
		case Adds_Characters_With_Line_Feed:
			n2 = 50;
			break;
		case No_Added_Characters_Without_Line_Feed:
			n2 = 51;
			break;
		case Adds_Characters_Without_Line_Feed:
			n2 = 52;
			break;
		}
		byte n3 = 0;
		switch (width) {
		case _2_6:
			n3 = 49;
			break;
		case _3_9:
			n3 = 50;
			break;
		case _4_12:
			n3 = 51;
			break;
		case _2_5:
			n3 = 52;
			break;
		case _3_8:
			n3 = 53;
			break;
		case _4_10:
			n3 = 54;
			break;
		case _2_4:
			n3 = 55;
			break;
		case _3_6:
			n3 = 56;
			break;
		case _4_8:
			n3 = 57;
			break;
		}
		byte n4 = height;
		byte[] command = new byte[6 + barcodeData.length + 1];
		command[0] = 0x1b;
		command[1] = 0x62;
		command[2] = n1;
		command[3] = n2;
		command[4] = n3;
		command[5] = n4;
		for (int index = 0; index < barcodeData.length; index++) {
			command[index + 6] = barcodeData[index];
		}
		command[command.length - 1] = 0x1e;

		commands.add(command);

		sendCommand(context, portName, portSettings, commands);
	}

	/**
	 * This function is used to print barcodes in 93 format
	 * 
	 * @param context
	 *     Activity for displaying messages to the user
	 * @param portName
	 *     Port name to use for communication. This should be (TCP:<IPAddress>)
	 * @param portSettings
	 *     Should be blank
	 * @param barcodeData
	 *     These are the characters that will be printed in the barcode. The characters available for this barcode are listed in section 3-43 (Rev. 1.12).
	 * @param option
	 *     This tells the printer to put characters under the printed barcode or not. This may also be used to line feed after the barcode is printed.
	 * @param height
	 *     The height of the barcode. This is measured in pixels
	 * @param width
	 *     This is the number of dots per module. This value should be between 1 to 3. See section 3-42 (Rev. 1.12) for more information on the values.
	 */
	public static void PrintCode93(Context context, String portName, String portSettings, byte[] barcodeData, BarCodeOption option, byte height, Min_Mod_Size width) {
		ArrayList<byte[]> commands = new ArrayList<byte[]>();

		byte n1 = 0x37;
		byte n2 = 0;
		switch (option) {
		case No_Added_Characters_With_Line_Feed:
			n2 = 49;
			break;
		case Adds_Characters_With_Line_Feed:
			n2 = 50;
			break;
		case No_Added_Characters_Without_Line_Feed:
			n2 = 51;
			break;
		case Adds_Characters_Without_Line_Feed:
			n2 = 52;
			break;
		}
		byte n3 = 0;
		switch (width) {
		case _2_dots:
			n3 = 49;
			break;
		case _3_dots:
			n3 = 50;
			break;
		case _4_dots:
			n3 = 51;
			break;
		}
		byte n4 = height;
		byte[] command = new byte[6 + barcodeData.length + 1];
		command[0] = 0x1b;
		command[1] = 0x62;
		command[2] = n1;
		command[3] = n2;
		command[4] = n3;
		command[5] = n4;
		for (int index = 0; index < barcodeData.length; index++) {
			command[index + 6] = barcodeData[index];
		}
		command[command.length - 1] = 0x1e;

		commands.add(command);

		sendCommand(context, portName, portSettings, commands);
	}

	/**
	 * This function is used to print barcodes in ITF format
	 * 
	 * @param context
	 *     Activity for displaying messages to the user
	 * @param portName
	 *     Port name to use for communication. This should be (TCP:<IPAddress>)
	 * @param portSettings
	 *     Should be blank
	 * @param barcodeData
	 *     These are the characters that will be printed in the barcode. The characters available for this barcode are listed in section 3-43 (Rev. 1.12).
	 * @param option
	 *     This tell the printer to put characters under the printed barcode or not. This may also be used to line feed after the barcode is printed.
	 * @param height
	 *     The height of the barcode. This is measured in pixels
	 * @param width
	 *     The width of the barcode. This value should be between 1 to 9. See section 3-42 (Rev. 1.12) for more information on the values.
	 */
	public static void PrintCodeITF(Context context, String portName, String portSettings, byte[] barcodeData, BarCodeOption option, byte height, NarrowWideV2 width) {
		ArrayList<byte[]> commands = new ArrayList<byte[]>();

		byte n1 = 0x35;
		byte n2 = 0;
		switch (option) {
		case No_Added_Characters_With_Line_Feed:
			n2 = 49;
			break;
		case Adds_Characters_With_Line_Feed:
			n2 = 50;
			break;
		case No_Added_Characters_Without_Line_Feed:
			n2 = 51;
			break;
		case Adds_Characters_Without_Line_Feed:
			n2 = 52;
			break;
		}
		byte n3 = 0;
		switch (width) {
		case _2_5:
			n3 = 49;
			break;
		case _4_10:
			n3 = 50;
			break;
		case _6_15:
			n3 = 51;
			break;
		case _2_4:
			n3 = 52;
			break;
		case _4_8:
			n3 = 53;
			break;
		case _6_12:
			n3 = 54;
			break;
		case _2_6:
			n3 = 55;
			break;
		case _3_9:
			n3 = 56;
			break;
		case _4_12:
			n3 = 57;
			break;
		}
		byte n4 = height;
		byte[] command = new byte[6 + barcodeData.length + 1];
		command[0] = 0x1b;
		command[1] = 0x62;
		command[2] = n1;
		command[3] = n2;
		command[4] = n3;
		command[5] = n4;
		for (int index = 0; index < barcodeData.length; index++) {
			command[index + 6] = barcodeData[index];
		}
		command[command.length - 1] = 0x1e;

		commands.add(command);

		sendCommand(context, portName, portSettings, commands);
	}

	/**
	 * This function is used to print barcodes in the 128 format
	 * 
	 * @param context
	 *     Activity for displaying messages to the user
	 * @param portName
	 *     Port name to use for communication. This should be (TCP:<IPAddress>)
	 * @param portSettings
	 *     Should be blank
	 * @param barcodeData
	 *     These are the characters that will be printed in the barcode. The characters available for this barcode are listed in section 3-43 (Rev. 1.12).
	 * @param option
	 *     This tell the printer to put characters under the printed barcode or not. This may also be used to line feed after the barcode is printed.
	 * @param height
	 *     The height of the barcode. This is measured in pixels
	 * @param width
	 *     This is the number of dots per module. This value should be between 1 to 3. See section 3-42 (Rev. 1.12) for more information on the values.
	 */
	public static void PrintCode128(Context context, String portName, String portSettings, byte[] barcodeData, BarCodeOption option, byte height, Min_Mod_Size width) {
		ArrayList<byte[]> commands = new ArrayList<byte[]>();

		byte n1 = 0x36;
		byte n2 = 0;
		switch (option) {
		case No_Added_Characters_With_Line_Feed:
			n2 = 49;
			break;
		case Adds_Characters_With_Line_Feed:
			n2 = 50;
			break;
		case No_Added_Characters_Without_Line_Feed:
			n2 = 51;
			break;
		case Adds_Characters_Without_Line_Feed:
			n2 = 52;
			break;
		}
		byte n3 = 0;
		switch (width) {
		case _2_dots:
			n3 = 49;
			break;
		case _3_dots:
			n3 = 50;
			break;
		case _4_dots:
			n3 = 51;
			break;
		}
		byte n4 = height;
		byte[] command = new byte[6 + barcodeData.length + 1];
		command[0] = 0x1b;
		command[1] = 0x62;
		command[2] = n1;
		command[3] = n2;
		command[4] = n3;
		command[5] = n4;
		for (int index = 0; index < barcodeData.length; index++) {
			command[index + 6] = barcodeData[index];
		}
		command[command.length - 1] = 0x1e;

		commands.add(command);

		sendCommand(context, portName, portSettings, commands);
	}

	/**
	 * This function shows different cut patterns for Star POS printers.
	 * 
	 * @param context
	 *     Activity for displaying messages to the user
	 * @param portName
	 *     Port name to use for communication. This should be (TCP:<IPAddress>)
	 * @param portSettings
	 *     Should be blank
	 * @param cuttype
	 *     The cut type to perform. The cut types are full cut, full cut with feed, partial cut, and partial cut with feed
	 */
	public static void performCut(Context context, String portName, String portSettings, CutType cuttype) {
		ArrayList<byte[]> commands = new ArrayList<byte[]>();

		byte[] autocutCommand = new byte[] { 0x1b, 0x64, 0x00 };
		switch (cuttype) {
		case FULL_CUT:
			autocutCommand[2] = 48;
			break;
		case PARTIAL_CUT:
			autocutCommand[2] = 49;
			break;
		case FULL_CUT_FEED:
			autocutCommand[2] = 50;
			break;
		case PARTIAL_CUT_FEED:
			autocutCommand[2] = 51;
			break;
		}

		commands.add(autocutCommand);

		sendCommand(context, portName, portSettings, commands);
	}

	/**
	 * This function sends raw text to the printer, showing how the text can be formated. Ex: Changing size
	 * 
	 * @param context
	 *     Activity for displaying messages to the user
	 * @param portName
	 *     Port name to use for communication. This should be (TCP:<IPAddress>)
	 * @param portSettings
	 *     Should be blank
	 * @param slashedZero
	 *     boolean variable to tell the printer to slash zeroes
	 * @param underline
	 *     boolean variable that tells the printer to underline the text
	 * @param invertColor
	 *     boolean variable that tells the printer to should invert text. All white space will become black but the characters will be left white.
	 * @param emphasized
	 *     boolean variable that tells the printer to should emphasize the printed text. This is somewhat like bold. It isn't as dark, but darker than regular characters.
	 * @param upperline
	 *     boolean variable that tells the printer to place a line above the text. This is only supported by newest printers.
	 * @param upsideDown
	 *     boolean variable that tells the printer to print text upside down.
	 * @param heightExpansion
	 *     This integer tells the printer what the character height should be, ranging from 0 to 5 and representing multiples from 1 to 6.
	 * @param widthExpansion
	 *     This integer tell the printer what the character width should be, ranging from 0 to 5 and representing multiples from 1 to 6.
	 * @param leftMargin
	 *     Defines the left margin for text on Star portable printers. This number can be from 0 to 65536. However, remember how much space is available as the text can be pushed off the page.
	 * @param alignment
	 *     Defines the alignment of the text. The printers support left, right, and center justification.
	 * @param textData
	 *     The text to send to the printer.
	 * @param encode
	 *     Set encode for multi-byte character or blank for single byte character.
	 */
	public static void PrintText(Context context, String portName, String portSettings, boolean slashedZero, boolean underline, boolean invertColor, boolean emphasized, boolean upperline, boolean upsideDown, int heightExpansion, int widthExpansion, byte leftMargin, Alignment alignment, byte[] textData, String encode) {
		ArrayList<byte[]> commands = new ArrayList<byte[]>();

		commands.add(new byte[] { 0x1b, 0x40 }); // Initialization

		if (encode.startsWith("Shift_JIS")) {
			byte[] kanjiModeCommand = new byte[] { 0x1b, 0x71, 0x1b, 0x24, 0x31 }; // Shift-JIS Kanji Mode(Disable JIS(ESC q) + Enable Shift-JIS(ESC $ n))
			commands.add(kanjiModeCommand);
		} else if (encode.startsWith("ISO2022JP")) {
			byte[] kanjiModeCommand = new byte[] { 0x1b, 0x24, 0x30 }; // JIS Kanji Mode(Disable Shift-JIS(ESC $ n))
			commands.add(kanjiModeCommand);
		}

		byte[] slashedZeroCommand = new byte[] { 0x1b, 0x2f, 0x00 };
		if (slashedZero) {
			slashedZeroCommand[2] = 49;
		} else {
			slashedZeroCommand[2] = 48;
		}
		commands.add(slashedZeroCommand);

		byte[] underlineCommand = new byte[] { 0x1b, 0x2d, 0x00 };
		if (underline) {
			underlineCommand[2] = 49;
		} else {
			underlineCommand[2] = 48;
		}
		commands.add(underlineCommand);

		byte[] invertColorCommand = new byte[] { 0x1b, 0x00 };
		if (invertColor) {
			invertColorCommand[1] = 0x34;
		} else {
			invertColorCommand[1] = 0x35;
		}
		commands.add(invertColorCommand);

		byte[] emphasizedPrinting = new byte[] { 0x1b, 0x00 };
		if (emphasized) {
			emphasizedPrinting[1] = 69;
		} else {
			emphasizedPrinting[1] = 70;
		}
		commands.add(emphasizedPrinting);

		byte[] upperLineCommand = new byte[] { 0x1b, 0x5f, 0x00 };
		if (upperline) {
			upperLineCommand[2] = 49;
		} else {
			upperLineCommand[2] = 48;
		}
		commands.add(upperLineCommand);

		if (upsideDown) {
			commands.add(new byte[] { 0x0f });
		} else {
			commands.add(new byte[] { 0x12 });
		}

		byte[] characterExpansion = new byte[] { 0x1b, 0x69, 0x00, 0x00 };
		characterExpansion[2] = (byte) (heightExpansion + '0');
		characterExpansion[3] = (byte) (widthExpansion + '0');
		commands.add(characterExpansion);

		commands.add(new byte[] { 0x1b, 0x6c, leftMargin });

		byte[] alignmentCommand = new byte[] { 0x1b, 0x1d, 0x61, 0x00 };
		switch (alignment) {
		case Left:
			alignmentCommand[3] = 48;
			break;
		case Center:
			alignmentCommand[3] = 49;
			break;
		case Right:
			alignmentCommand[3] = 50;
			break;
		}
		commands.add(alignmentCommand);

		// textData Encoding!!
		if (encode == "") {
			commands.add(textData);
		} else {
			String strData = new String(textData);
			byte[] rawData = null;
			try {
				if (encode.startsWith("Shift_JIS")) {
					rawData = strData.getBytes("Shift_JIS"); // Shift JIS code
				} else if (encode.startsWith("ISO2022JP")) {
					byte[] tempDataBytes  = strData.getBytes("ISO2022JP"); // JIS code;
					rawData = ReplaceCommand(tempDataBytes);				
				} else if (encode.startsWith("Big5")) {
					rawData = strData.getBytes("Big5"); // Traditional Chinese
				} else if (encode.startsWith("GB2312")) {
					rawData = strData.getBytes("GB2312"); // Simplified Chinese
				} else {
					rawData = strData.getBytes();
				}
			} catch (UnsupportedEncodingException e) {
				rawData = strData.getBytes();
			}
			commands.add(rawData);
		}

		commands.add(new byte[] { 0x0a });

		sendCommand(context, portName, portSettings, commands);
	}

	/**
	 * This function sends raw text to the printer, showing how the text can be formated. Ex: Changing size
	 * 
	 * @param context
	 *     Activity for displaying messages to the user
	 * @param portName
	 *     Port name to use for communication. This should be (TCP:<IPAddress>)
	 * @param portSettings
	 *     Should be blank
	 * @param slashedZero
	 *     boolean variable to tell the printer to slash zeroes
	 * @param underline
	 *     boolean variable that tells the printer to underline the text
	 * @param twoColor
	 *     boolean variable that tells the printer to should print red or black text.
	 * @param emphasized
	 *     boolean variable that tells the printer to should emphasize the printed text. This is somewhat like bold. It isn't as dark, but darker than regular characters.
	 * @param upperline
	 *     boolean variable that tells the printer to place a line above the text. This is only supported by newest printers.
	 * @param upsideDown
	 *     boolean variable that tells the printer to print text upside down.
	 * @param heightExpansion
	 *     boolean variable that tells the printer to should expand double-tall printing.
	 * @param widthExpansion
	 *     boolean variable that tells the printer to should expand double-wide printing.
	 * @param leftMargin
	 *     Defines the left margin for text on Star portable printers. This number can be from 0 to 65536. However, remember how much space is available as the text can be pushed off the page.
	 * @param alignment
	 *     Defines the alignment of the text. The printers support left, right, and center justification.
	 * @param textData
	 *     The text to send to the printer.
	 * @param encode
	 *     Set encode for multi-byte character or blank for single byte character.
	 */
	public static void PrintTextbyDotPrinter(Context context, String portName, String portSettings, boolean slashedZero, boolean underline, boolean twoColor, boolean emphasized, boolean upperline, boolean upsideDown, boolean heightExpansion, boolean widthExpansion, byte leftMargin, Alignment alignment, byte[] textData, String encode) {
		ArrayList<byte[]> commands = new ArrayList<byte[]>();

		commands.add(new byte[] { 0x1b, 0x40 }); // Initialization

		if (encode.startsWith("Shift_JIS")) {
			commands.add(new byte[] { 0x1b, 0x71, 0x1b, 0x24, 0x31 }); // Shift-JIS Kanji Mode(Disable JIS(ESC q) + Enable Shift-JIS(ESC $ n))
		} else if (encode.startsWith("ISO2022JP")) {
			commands.add(new byte[] { 0x1b, 0x24, 0x30}); // JIS Kanji Mode(Disable Shift-JIS(ESC $ n)
		}

		byte[] slashedZeroCommand = new byte[] { 0x1b, 0x2f, 0x00 };
		if (slashedZero) {
			slashedZeroCommand[2] = 49;
		} else {
			slashedZeroCommand[2] = 48;
		}
		commands.add(slashedZeroCommand);

		byte[] underlineCommand = new byte[] { 0x1b, 0x2d, 0x00 };
		if (underline) {
			underlineCommand[2] = 49;
		} else {
			underlineCommand[2] = 48;
		}
		commands.add(underlineCommand);

		byte[] twoColorCommand = new byte[] { 0x1b, 0x00 };
		if (twoColor) {
			twoColorCommand[1] = 0x34;
		} else {
			twoColorCommand[1] = 0x35;
		}
		commands.add(twoColorCommand);

		byte[] emphasizedPrinting = new byte[] { 0x1b, 0x00 };
		if (emphasized) {
			emphasizedPrinting[1] = 69;
		} else {
			emphasizedPrinting[1] = 70;
		}
		commands.add(emphasizedPrinting);

		byte[] upperLineCommand = new byte[] { 0x1b, 0x5f, 0x00 };
		if (upperline) {
			upperLineCommand[2] = 49;
		} else {
			upperLineCommand[2] = 48;
		}
		commands.add(upperLineCommand);

		if (upsideDown) {
			commands.add(new byte[] { 0x0f });
		} else {
			commands.add(new byte[] { 0x12 });
		}

		byte[] characterheightExpansion = new byte[] { 0x1b, 0x68, 0x00 };
		if (heightExpansion) {
			characterheightExpansion[2] = 49;
		} else {
			characterheightExpansion[2] = 48;
		}
		commands.add(characterheightExpansion);

		byte[] characterwidthExpansion = new byte[] { 0x1b, 0x57, 0x00 };
		if (widthExpansion) {
			characterwidthExpansion[2] = 49;
		} else {
			characterwidthExpansion[2] = 48;
		}
		commands.add(characterwidthExpansion);

		commands.add(new byte[] { 0x1b, 0x6c, leftMargin });

		byte[] alignmentCommand = new byte[] { 0x1b, 0x1d, 0x61, 0x00 };
		switch (alignment) {
		case Left:
			alignmentCommand[3] = 48;
			break;
		case Center:
			alignmentCommand[3] = 49;
			break;
		case Right:
			alignmentCommand[3] = 50;
			break;
		}
		commands.add(alignmentCommand);

		// textData Encoding!!
		if (encode == "") {
			commands.add(textData);
		} else {
			String strData = new String(textData);
			byte[] rawData = null;
			try {
				if (encode.startsWith("Shift_JIS")) {
					rawData = strData.getBytes("Shift_JIS"); // Shift JIS code
				} else if (encode.startsWith("ISO2022JP")) {				
					byte[] tempDataBytes  = strData.getBytes("ISO2022JP"); // JIS code;
					rawData = ReplaceCommand(tempDataBytes);					
				} else if (encode.startsWith("Big5")) {
					rawData = strData.getBytes("Big5"); // Traditional Chinese
				} else if (encode.startsWith("GB2312")) {
					rawData = strData.getBytes("GB2312"); // Simplified Chinese
				} else {
					rawData = strData.getBytes();
				}
			} catch (UnsupportedEncodingException e) {
				rawData = strData.getBytes();
			}
			commands.add(rawData);
		}

		commands.add(new byte[] { 0x0a });

		sendCommand(context, portName, portSettings, commands);
	}

	protected static byte[] ReplaceCommand(byte[] tempDataBytes) {

		byte[] buffer = new byte[tempDataBytes.length];
		int j = 0;
		
		byte[] specifyJISkanjiCharacterModeCommand = new byte[] {0x1b, 0x70};
		byte[] cancelJISkanjiCharacterModeCommand = new byte[] {0x1b, 0x71};
		
		//replace command
		//Because LF(0x0A) command is not performed.
		if(tempDataBytes.length > 0){
			for(int i=0; i<tempDataBytes.length; i++){
				if(tempDataBytes[i] == 0x1b){
					if(tempDataBytes[i+1] == 0x24){// Replace [0x1b 0x24 0x42] to "Specify JIS Kanji Character Mode" command
						buffer[j]   = specifyJISkanjiCharacterModeCommand[0];
						buffer[j+1] = specifyJISkanjiCharacterModeCommand[1];
						j += 2;
					}
					else if(tempDataBytes[i+1] == 0x28){//Replace [0x1b 0x28 0x42] to "Cancel JIS Kanji Character Mode" command
						buffer[j]   = cancelJISkanjiCharacterModeCommand[0];
						buffer[j+1] = cancelJISkanjiCharacterModeCommand[1];
						j += 2;
					}
					
					i += 2;
				}else{
					buffer[j] = tempDataBytes[i];
					j++;
				}
			}
		}
		
		//check 0x00 position
		int datalength = 0;
		for(int i=0; i< buffer.length; i++){
			if(buffer[i] == 0x00){
				datalength = i;
				break;
			}
		}
		
		//copy data
		if(datalength == 0){
			datalength = buffer.length;
		}
		byte[] data = new byte[datalength];		
		System.arraycopy(buffer, 0, data, 0, datalength);

		return data;
	}

	/**
	 * This function is used to print a Java bitmap directly to the printer. There are 2 ways a printer can print images: through raster commands or line mode commands This function uses raster commands to print an image. Raster is supported on the TSP100 and all Star Thermal POS printers. Line mode printing is not supported by the TSP100. There is no example of using this method in this sample.
	 * 
	 * @param context
	 *     Activity for displaying messages to the user
	 * @param portName
	 *     Port name to use for communication. This should be (TCP:<IPAddress>)
	 * @param portSettings
	 *     Should be blank
	 * @param source
	 *     The bitmap to convert to Star Raster data
	 * @param maxWidth
	 *     The maximum width of the image to print. This is usually the page width of the printer. If the image exceeds the maximum width then the image is scaled down. The ratio is maintained.
	 */
	public static void PrintBitmap(Context context, String portName, String portSettings, Bitmap source, int maxWidth, boolean compressionEnable, RasterCommand rasterType) {
		try {
			ArrayList<byte[]> commands = new ArrayList<byte[]>();

			RasterDocument rasterDoc = new RasterDocument(RasSpeed.Medium, RasPageEndMode.FeedAndFullCut, RasPageEndMode.FeedAndFullCut, RasTopMargin.Standard, 0, 0, 0);
			StarBitmap starbitmap = new StarBitmap(source, false, maxWidth);

			if (rasterType == RasterCommand.Standard) {
				commands.add(rasterDoc.BeginDocumentCommandData());

				commands.add(starbitmap.getImageRasterDataForPrinting_Standard(compressionEnable));

				commands.add(rasterDoc.EndDocumentCommandData());
			} else {
				commands.add(starbitmap.getImageRasterDataForPrinting_graphic(compressionEnable));
				commands.add(new byte[] { 0x1b, 0x64, 0x02 }); // Feed to cutter position
			}

			sendCommand(context, portName, portSettings, commands);
		} catch (OutOfMemoryError e) {
			throw e;
		}

	}

	/**
	 * This function is used to print a Java bitmap directly to the printer. There are 2 ways a printer can print images: through raster commands or line mode commands This function uses raster commands to print an image. Raster is supported on the TSP100 and all Star Thermal POS printers. Line mode printing is not supported by the TSP100. There is no example of using this method in this sample.
	 * 
	 * @param context
	 *     Activity for displaying messages to the user
	 * @param portName
	 *     Port name to use for communication. This should be (TCP:<IPAddress>)
	 * @param portSettings
	 *     Should be blank
	 * @param res
	 *     The resources object containing the image data
	 * @param source
	 *     The resource id of the image data
	 * @param maxWidth
	 *     The maximum width of the image to print. This is usually the page width of the printer. If the image exceeds the maximum width then the image is scaled down. The ratio is maintained.
	 */
	public static void PrintBitmapImage(Context context, String portName, String portSettings, Resources res, int source, int maxWidth, boolean compressionEnable, RasterCommand rasterType) {
		ArrayList<byte[]> commands = new ArrayList<byte[]>();

		RasterDocument rasterDoc = new RasterDocument(RasSpeed.Medium, RasPageEndMode.FeedAndFullCut, RasPageEndMode.FeedAndFullCut, RasTopMargin.Standard, 0, 0, 0);
		Bitmap bm = BitmapFactory.decodeResource(res, source);
		StarBitmap starbitmap = new StarBitmap(bm, false, maxWidth);

		if (rasterType == RasterCommand.Standard) {
			commands.add(rasterDoc.BeginDocumentCommandData());

			commands.add(starbitmap.getImageRasterDataForPrinting_Standard(compressionEnable));

			commands.add(rasterDoc.EndDocumentCommandData());
		} else {
			commands.add(starbitmap.getImageRasterDataForPrinting_graphic(compressionEnable));
			commands.add(new byte[] { 0x1b, 0x64, 0x02 }); // Feed to cutter position
		}

		sendCommand(context, portName, portSettings, commands);
	}

	/**
	 * This function shows how to read the MSR data(credit card) of a portable printer. The function first puts the printer into MSR read mode, then asks the user to swipe a credit card The function waits for a response from the user. The user can cancel MSR mode or have the printer read the card.
	 * 
	 * @param context
	 *     Activity for displaying messages to the user
	 * @param portName
	 *     Port name to use for communication. This should be (TCP:<IPAddress> or BT:<Device pair name>)
	 * @param portSettings
	 *     Should be portable as the port settings. It is used for portable printers
	 */
	public static void MCRStart(final Context context, String portName, String portSettings) {
		try {
			/*
			 * using StarIOPort3.1.jar (support USB Port) Android OS Version: upper 2.2
			 */
			portForMoreThanOneFunction = StarIOPort.getPort(portName, portSettings, 10000, context);
			/*
			 * using StarIOPort.jar Android OS Version: under 2.1 portForMoreThanOneFunction = StarIOPort.getPort(portName, portSettings, 10000);
			 */

			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
			}

			portForMoreThanOneFunction.writePort(new byte[] { 0x1b, 0x4d, 0x45 }, 0, 3);

			Builder dialog = new Builder(context);
			dialog.setNegativeButton("Cancel", new OnClickListener() {
				// If the user cancels MSR mode, the character 0x04 is sent to the printer
				// This function also closes the port
				public void onClick(DialogInterface dialog, int which) {
					((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
					((AlertDialog) dialog).getButton(DialogInterface.BUTTON_NEGATIVE).setEnabled(false);
					try {
						portForMoreThanOneFunction.writePort(new byte[] { 0x04 }, 0, 1);
						try {
							Thread.sleep(3000);
						} catch (InterruptedException e) {
						}
					} catch (StarIOPortException e) {

					} finally {
						if (portForMoreThanOneFunction != null) {
							try {
								StarIOPort.releasePort(portForMoreThanOneFunction);
							} catch (StarIOPortException e1) {
							}
						}
					}
				}
			});
			AlertDialog alert = dialog.create();
			alert.setTitle("");
			alert.setMessage("Slide credit card");
			alert.setCancelable(false);
			alert.setButton("OK", new OnClickListener() {
				// If the user presses ok then the magnetic stripe is read and displayed to the user
				// This function also closes the port
				public void onClick(DialogInterface dialog, int which) {
					((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
					((AlertDialog) dialog).getButton(DialogInterface.BUTTON_NEGATIVE).setEnabled(false);
					try {
						byte[] mcrData = new byte[256];
						int counts = 0;
						
						for (int i = 0; i < 5; i++) {
							counts += portForMoreThanOneFunction.readPort(mcrData, counts, mcrData.length);
							try {
								Thread.sleep(100);
							} catch (InterruptedException e) {
							}
						}
						
				        byte[] headerPattern = new byte[]{0x02, 0x45, 0x31, 0x31, 0x1c, 0x1c};
				        byte[] footerPattern = new byte[]{0x1c, 0x03, 0x0d, 0x0a };
				        int headerPatternPos = -1;
				        int fotterPatternPos = -1;
				        
				        byte[] data2 = new byte[headerPattern.length];
				        byte[] data3 = new byte[footerPattern.length];
				       
				        //Check Start header position
				        for(int i = 0; i< (mcrData.length - headerPattern.length +1); i++){
				        	System.arraycopy(mcrData, i, data2, 0, (headerPattern.length));
				               	
				        	if(Arrays.equals(data2, headerPattern))
				        	{
				        		headerPatternPos = i;
				        		break;
				        	}
				        }
				       
				      //Check Start fotter position
				        for(int i = headerPatternPos + headerPattern.length; i< (mcrData.length - footerPattern.length +1); i++){
				       		System.arraycopy(mcrData, i, data3, 0, (footerPattern.length));
				       		        	       	       	
				        	if(Arrays.equals(data3, footerPattern)){       		
				           		fotterPatternPos = i;
				           		break;
				        	}        	
				        }        
				        
				    	if((headerPatternPos < 0) || (fotterPatternPos < 0) ){
							Builder dialog1 = new Builder(context);
							dialog1.setNegativeButton("Ok", null);
							AlertDialog alert = dialog1.create();
							alert.setTitle("No data");
							alert.setMessage("There is nothing available data.");
							alert.show();
				    	} else {
					        byte[] reciveDataList = new byte[fotterPatternPos - headerPatternPos];
					                
					        System.arraycopy(mcrData, headerPatternPos, reciveDataList, 0, fotterPatternPos - headerPatternPos);
	
							Builder dialog1 = new Builder(context);
							dialog1.setNegativeButton("Ok", null);
							AlertDialog alert = dialog1.create();
							alert.setTitle("");
							alert.setMessage(new String(reciveDataList));
							alert.show();
				    	}
						
						portForMoreThanOneFunction.writePort(new byte[] { 0x04 }, 0, 1);
						try {
							Thread.sleep(3000);
						} catch (InterruptedException e) {
						}

					} catch (StarIOPortException e) {

					} finally {
						if (portForMoreThanOneFunction != null) {
							try {
								StarIOPort.releasePort(portForMoreThanOneFunction);
							} catch (StarIOPortException e1) {
							}
						}
					}
				}
			});
			alert.show();
		} catch (StarIOPortException e) {
			Builder dialog = new Builder(context);
			dialog.setNegativeButton("Ok", null);
			AlertDialog alert = dialog.create();
			alert.setTitle("Failure");
			alert.setMessage("Failed to connect to printer");
			alert.setCancelable(false);
			alert.show();
			if (portForMoreThanOneFunction != null) {
				try {
					StarIOPort.releasePort(portForMoreThanOneFunction);
				} catch (StarIOPortException e1) {
				}
			}
		} finally {

		}
	}

	/**
	 * MSR functionality is supported on Star portable printers only.
	 * 
	 * @param context
	 *     Activity for displaying messages to the user that this function is not supported
	 */
	public static void MCRnoSupport(Context context) {
		Builder dialog = new Builder(context);
		dialog.setNegativeButton("OK", null);
		AlertDialog alert = dialog.create();
		alert.setTitle("Feature Not Available");
		alert.setMessage("MSR functionality is supported only on portable printer models");
		alert.setCancelable(false);
		alert.show();
	}

	public static void PrintSampleReceiptCHTbyDotPrinter(Context context, String portName, String portSettings) {
		ArrayList<byte[]> list = new ArrayList<byte[]>();

		list.add(new byte[] { 0x1b, 0x40 }); // Initialization
		// list.add(new byte[]{0x1d, 0x57, (byte) 0x80, 0x01});
		// list.add(new byte[]{0x1b, 0x24, 0x31});
		list.add(new byte[] { 0x1b, 0x44, 0x10, 0x00 }); // <ESC> <D> n1 n2 nk <NUL>
		list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x31 }); // <ESC> <GS> a n

		list.add(new byte[] { 0x1b, 0x57, 0x31 }); // <ESC> <W> n
		list.add(new byte[] { 0x1b, 0x45 }); // <ESC> <E>

		// outputByteBuffer = "[If loaded.. Logo1 goes here]\r\n".getBytes();
		// tempList = new byte[outputByteBuffer.length];
		// CopyArray(outputByteBuffer, tempList);
		// list.add(tempList));
		// list.add(new byte[]{0x1b, 0x1c, 0x70, 0x01, 0x00, '\r', '\n'});
		// //Stored Logo Printing

		list.add(createBIG5(context.getResources().getString(R.string.title_company_name_cht) + "\n"));

		list.add(new byte[] { 0x1b, 0x57, 0x30, 0x00 }); // <ESC> <W> n
		list.add(new byte[] { 0x1b, 0x46 }); // <ESC> <F>

		list.add(createBIG5("------------------------------------------"));

		list.add(new byte[] { 0x1b, 0x57, 0x31 }); // <ESC> <W> n

		list.add(createBIG5(context.getResources().getString(R.string.title_receipt_name_cht) + "\n"));

		list.add(createBIG5(context.getResources().getString(R.string.cht_103) + "\n"));

		list.add(createBIG5(context.getResources().getString(R.string.ev_99999999_cht) + "\n"));

		list.add(new byte[] { 0x1b, 0x57, 0x30, 0x00 }); // <ESC> <W> n
		list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x30 }); // <ESC> <GS> a n

		list.add(createBIG5(context.getResources().getString(R.string.date_cht) + "\n"));

		list.add(createBIG5(context.getResources().getString(R.string.random_code_cht) + "\n"));

		list.add(createBIG5(context.getResources().getString(R.string.seller_cht) + "\n"));

		// 1D barcode example
		list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x01 });
		list.add(new byte[] { 0x1b, 0x62, 0x35, 0x31, 0x33, 0x20 });

		list.add(("999999999\u001e\r\n").getBytes());

		list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x30 }); // <ESC> <GS> a n

		list.add(createBIG5(context.getResources().getString(R.string.Item_list_cht) + "\n"));

		list.add(createBIG5(context.getResources().getString(R.string.Item_list_Number_cht) + "\n\n\n"));

		list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x31 }); // <ESC> <GS> a n

		list.add(createBIG5(context.getResources().getString(R.string.Sales_schedules_cht) + "\n"));

		list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x30 }); // <ESC> <GS> a n

		list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x32 }); // <ESC> <GS> a n
		list.add(createBIG5(context.getResources().getString(R.string.date_2_cht) + "\n"));

		list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x30 }); // <ESC> <GS> a n

		list.add(createBIG5(context.getResources().getString(R.string.ItemInfo_3inch_line_cht) + "\n"));

		list.add(new byte[] { 0x1b, 0x45 }); // <ESC> <E>

		list.add(createBIG5(context.getResources().getString(R.string.sub_3inch_line_cht) + "\n"));

		list.add(createBIG5(context.getResources().getString(R.string.total_3inch_line_cht) + "\n"));

		list.add(new byte[] { 0x1b, 0x46 }); // <ESC> <F>

		list.add(createBIG5("------------------------------------------\n"));

		list.add(createBIG5(context.getResources().getString(R.string.cash_3inch_line_cht) + "\n"));

		list.add(createBIG5(context.getResources().getString(R.string.change_3inch_line_cht) + "\n"));

		list.add(new byte[] { 0x1b, 0x45 }); // <ESC> <E>

		list.add(createBIG5(context.getResources().getString(R.string.Invoice_3inch_line_cht) + "\n"));

		list.add(new byte[] { 0x1b, 0x46 }); // <ESC> <F>

		list.add(createBIG5(context.getResources().getString(R.string.date_3_cht) + "\n"));

		// 1D barcode example
		list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x01 });
		list.add(new byte[] { 0x1b, 0x62, 0x35, 0x31, 0x33, 0x20 });

		list.add(("999999999\u001e\r\n").getBytes());

		list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x30 }); // <ESC> <GS> a n

		list.add(createBIG5(context.getResources().getString(R.string.info_cht) + "\n"));

		list.add(createBIG5(context.getResources().getString(R.string.info_number_cht) + "\n"));

		list.add(new byte[] { 0x1b, 0x64, 0x33 }); // Cut
		list.add(new byte[] { 0x07 }); // Kick cash drawer

		sendCommand(context, portName, portSettings, list);
	}

	public static void PrintSampleReceiptCHSbyDotPrinter(Context context, String portName, String portSettings) {
		ArrayList<byte[]> list = new ArrayList<byte[]>();

		list.add(new byte[] { 0x1b, 0x40 }); // Initialization
		// list.add(new byte[]{0x1d, 0x57, (byte) 0x80, 0x01});
		// list.add(new byte[]{0x1b, 0x24, 0x31});
		list.add(new byte[] { 0x1b, 0x44, 0x10, 0x00 }); // <ESC> <D> n1 n2 nk <NUL>
		list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x31 }); // <ESC> <GS> a n

		list.add(new byte[] { 0x1b, 0x57, 0x31 }); // <ESC> <W> n
		list.add(new byte[] { 0x1b, 0x45 }); // <ESC> <E>

		list.add(createGB2312(context.getResources().getString(R.string.title_company_name_chs) + "\n"));

		list.add(createGB2312(context.getResources().getString(R.string.title_receipt_name_chs) + "\n"));

		list.add(new byte[] { 0x1b, 0x57, 0x30, 0x00 }); // <ESC> <W> n
		list.add(new byte[] { 0x1b, 0x46 }); // <ESC> <F>

		list.add(createGB2312(context.getResources().getString(R.string.address_chs)));

		list.add(createGB2312(context.getResources().getString(R.string.phone_chs) + "\n"));

		list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x30 }); // <ESC> <GS> a n

		list.add(createGB2312(context.getResources().getString(R.string.ItemInfo_3inch_dot_line_chs)));

		list.add(createGB2312(context.getResources().getString(R.string.total_3inch_line_chs) + "\n"));

		list.add(createGB2312(context.getResources().getString(R.string.cash_3inch_line_chs) + "\n"));

		list.add(createGB2312(context.getResources().getString(R.string.findforeclosure_3inch_line_chs) + "\n"));

		list.add(createGB2312(context.getResources().getString(R.string.cardnumber_3inch_line_chs) + "\n"));

		list.add(createGB2312(context.getResources().getString(R.string.cardbalance_3inch_line_chs) + "\n"));

		list.add(createGB2312(context.getResources().getString(R.string.machinenumber_3inch_line_chs) + "\n"));

		list.add(createGB2312(context.getResources().getString(R.string.receiptinfo_3inch_line_chs) + "\n"));

		list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x31 }); // <ESC> <GS> a n

		list.add(createGB2312(context.getResources().getString(R.string.cashier_3inch_line_chs) + "\n"));

		list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x30 }); // <ESC> <GS> a n

		list.add(new byte[] { 0x1b, 0x64, 0x33 }); // Cut
		list.add(new byte[] { 0x07 }); // Kick cash drawer

		sendCommand(context, portName, portSettings, list);

	}

	public static void PrintSampleReceiptJpbyDotPrinter(Context context, String portName, String portSettings) {
		ArrayList<byte[]> list = new ArrayList<byte[]>();

		byte[] outputByteBuffer = null;
		list.add(new byte[] { 0x1b, 0x40 }); // Initialization
		// list.add(new byte[]{0x1d, 0x57, (byte) 0x80, 0x01});
		list.add(new byte[] { 0x1b, 0x24, 0x31 });
		list.add(new byte[] { 0x1b, 0x44, 0x10, 0x00 });
		list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x31 });

		list.add(new byte[] { 0x1b, 0x69, 0x02, 0x00 });
		list.add(new byte[] { 0x1b, 0x45 });

		list.add(createShiftJIS(context.getResources().getString(R.string.title_company_name) + "\n"));

		list.add(new byte[] { 0x1b, 0x69, 0x01, 0x00 });

		list.add(createShiftJIS(context.getResources().getString(R.string.title_receipt_name) + "\n"));

		list.add(new byte[] { 0x1b, 0x69, 0x00, 0x00 });
		list.add(new byte[] { 0x1b, 0x46 });

		list.add(createShiftJIS("------------------------------------------\n"));

		Calendar calendar = Calendar.getInstance();
		int year = calendar.get(Calendar.YEAR);
		int month = calendar.get(Calendar.MONTH);
		int day = calendar.get(Calendar.DAY_OF_MONTH);
		String YMD = (year + context.getResources().getString(R.string.year) + (month + 1) + context.getResources().getString(R.string.month) + day + context.getResources().getString(R.string.day)).toString();

		int hour24 = calendar.get(Calendar.HOUR_OF_DAY);
		int minute = calendar.get(Calendar.MINUTE);
		String TIME = (hour24 + context.getResources().getString(R.string.hour) + minute + context.getResources().getString(R.string.min)).toString();

		list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x30 });

		list.add(createShiftJIS(context.getResources().getString(R.string.date) + YMD + "  " + TIME + "\n"));

		list.add(createShiftJIS("TEL:054-347-XXXX\n\n"));

		list.add(createShiftJIS(context.getResources().getString(R.string.kana_dot_line) + "\n"));

		list.add(createShiftJIS(context.getResources().getString(R.string.personalInfo)));

		list.add(createShiftJIS(context.getResources().getString(R.string.ItemInfo_3inch_dot_line)));

		int sub = 0;
		int tax = 0;
		sub = 10000 + 3800 + 2000 + 15000 + 5000;
		NumberFormat exsub = NumberFormat.getNumberInstance();

		tax = sub * 5 / 100;
		NumberFormat extax = NumberFormat.getNumberInstance();

		outputByteBuffer = createShiftJIS(context.getResources().getString(R.string.sub_3inch_dot_line) + exsub.format(sub) + "\n\n" + context.getResources().getString(R.string.tax_3inch_dot_line) + extax.format(tax) + "\n\n" + context.getResources().getString(R.string.total_3inch_dot_line) + exsub.format(sub) + "\n\n" + context.getResources().getString(R.string.phone) + "\n\n");
		list.add(outputByteBuffer);

		list.add(new byte[] { 0x1b, 0x64, 0x33 }); // Cut
		list.add(new byte[] { 0x07 }); // Kick cash drawer

		sendCommand(context, portName, portSettings, list);
	}

	/**
	 * This function shows how to print the receipt data of a Impact Dot Matrix printer.
	 * 
	 * @param context
	 *     Activity for displaying messages to the user
	 * @param portName
	 *     Port name to use for communication. This should be (TCP:<IPAddress>)
	 * @param portSettings
	 *     Should be blank
	 * @param res
	 *     The resources object containing the image data. ( e.g.) getResources())
	 */
	public static void PrintSampleReceiptbyDotPrinter(Context context, String portName, String portSettings) {
		ArrayList<byte[]> list = new ArrayList<byte[]>();

		list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x01 }); // Alignment (center)

		// list.add("[If loaded.. Logo1 goes here]\r\n".getBytes());
		// list.add(new byte[]{0x1b, 0x1c, 0x70, 0x01, 0x00, '\r', '\n'}); //Stored Logo Printing <ESC> <FC> <p> n m

		list.add("\nStar Clothing Boutique\r\n".getBytes());

		list.add("123 Star Road\r\nCity, State 12345\r\n\r\n".getBytes());

		list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x00 }); // Alignment

		list.add(new byte[] { 0x1b, 0x44, 0x02, 0x10, 0x22, 0x00 }); // Set horizontal tab <ESC> <D> n1 n2 ...nk NUL

		list.add("Date: MM/DD/YYYY".getBytes());

		list.add("             Time:HH:MM PM\r\n".getBytes());

		list.add("------------------------------------------\r\n\r\n".getBytes());

		list.add(new byte[] { 0x1b, 0x45 }); // bold

		list.add("SALE \r\n".getBytes());

		list.add(new byte[] { 0x1b, 0x46 }); // bolf off

		list.add("SKU ".getBytes());

		list.add(new byte[] { 0x09 });

		// Notice that we use a unicode representation because that is how Java
		// expresses these bytes as double byte unicode
		// This will TAB to the next horizontal position
		list.add("Description \u0009 Total\r\n".getBytes());

		list.add("300678566 \u0009PLAIN T-SHIRT\u0009  10.99\r\n".getBytes());

		list.add("300692003 \u0009BLACK DENIM\u0009  29.99\r\n".getBytes());

		list.add("300651148 \u0009BLUE DENIM\u0009  29.99\r\n".getBytes());

		list.add("300642980 \u0009STRIPED DRESS\u0009  49.99\r\n".getBytes());

		list.add("300638471 \u0009BLACK BOOTS\u0009  35.99\r\n\r\n".getBytes());

		list.add("Subtotal \u0009\u0009 156.95\r\n".getBytes());

		list.add("Tax \u0009\u0009   0.00\r\n".getBytes());

		list.add("------------------------------------------\r\n".getBytes());

		list.add("Total".getBytes());

		// Character expansion
		list.add(new byte[] { 0x06, 0x09, 0x1b, 0x69, 0x01, 0x01 });

		list.add("                  $156.95\r\n".getBytes());

		list.add(new byte[] { 0x1b, 0x69, 0x00, 0x00 }); // Cancel Character Expansion

		list.add("------------------------------------------\r\n\r\n".getBytes());
		list.add("Charge\r\n159.95\r\n".getBytes());
		list.add("Visa XXXX-XXXX-XXXX-0123\r\n\r\n".getBytes());

		// Specify/Cancel White/Black Invert
		list.add("\u001b\u0034Refunds and Exchanges\u001b\u0035\r\n".getBytes());

		// Specify/Cancel Underline Printing
		list.add(("Within " + "\u001b\u002d\u0001" + "30 days\u001b\u002d\u0000" + " with receipt\r\n").getBytes());

		// list.add("And tags attached\r\n\r\n".getBytes());

		// 1D barcode example
		// list.add(new byte[]{0x1b, 0x1d, 0x61, 0x01});
		// list.add(new byte[]{0x1b, 0x62, 0x06, 0x02, 0x02});

		// list.add(" 12ab34cd56\u001e\r\n".getBytes());

		list.add(new byte[] { 0x1b, 0x64, 0x02 }); // Cut
		list.add(new byte[] { 0x07 }); // Kick cash drawer

		sendCommand(context, portName, portSettings, list);
	}

	/**
	 * This function shows how to print the receipt data of a Impact Dot Matrix printer.
	 * 
	 * @param context
	 *     Activity for displaying messages to the user
	 * @param portName
	 *     Port name to use for communication. This should be (TCP:<IPAddress>)
	 * @param portSettings
	 *     Should be blank
	 * @param res
	 *     The resources object containing the image data. ( e.g.) getResources())
	 */
	public static void PrintSampleReceiptbyDotPrinter_French(Context context, String portName, String portSettings) {
		ArrayList<byte[]> list = new ArrayList<byte[]>();

		list.add(new byte[] { 0x1b, 0x1d, 0x74, 0x20 }); // Code Page #1252 (Windows Latin-1)

		list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x01 }); // Alignment (center)

		// list.add("[If loaded.. Logo1 goes here]\r\n".getBytes());

		// list.add(new byte[]{0x1b, 0x1c, 0x70, 0x01, 0x00, '\r', '\n'}); //Stored Logo Printing

		// Notice that we use a unicode representation because that is
		// how Java expresses these bytes as double byte unicode

		// Character expansion
		list.add(new byte[] { 0x1b, 0x57, 0x01 });
		
		list.add(new byte[] { 0x1b, 0x68, 0x01 });

		list.add(createCp1252("\nORANGE\r\n"));

		list.add(new byte[] { 0x1b, 0x57, 0x00 });
		
		list.add(new byte[] { 0x1b, 0x68, 0x00 });

		list.add(createCp1252("36 AVENUE LA MOTTE PICQUET\r\n\r\n"));

		list.add(new byte[] { 0x1b, 0x44, 0x02, 0x06, 0x0a, 0x10, 0x14, 0x1a, 0x22, 0x00 }); // Set horizontal tab

		list.add(createCp1252("------------------------------------------\r\n"));
		
		list.add(createCp1252("Date: MM/DD/YYYY    Heure: HH:MM\r\n"));
		
		list.add(createCp1252("Boutique: OLUA23    Caisse: 0001\r\n"));
		list.add(createCp1252("Conseiller: 002970  Ticket: 3881\r\n"));
		
		list.add(createCp1252("------------------------------------------\r\n\r\n"));

		list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x00 }); // Alignment

		list.add(createCp1252("Vous avez été servi par : Souad\r\n\r\n"));

		list.add(createCp1252("CAC IPHONE ORANGE\r\n"));
		
		list.add(createCp1252("3700615033581 \t1\t X\t 19.99€\t  19.99€\r\n\r\n"));
		
		list.add(createCp1252("dont contribution environnementale :\r\n"));
		
		list.add(createCp1252("CAC IPHONE ORANGE\t\t  0.01€\r\n"));

		list.add(createCp1252("------------------------------------------\r\n"));

		list.add(createCp1252("1 Piéce(s) Total :\t\t\t  19.99€\r\n"));
		
		list.add(createCp1252("Mastercard Visa  :\t\t\t  19.99€\r\n\r\n"));
		
		list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x01 }); // Alignment (center)

		list.add(createCp1252("Taux TVA    Montant H.T.   T.V.A\r\n"));
		
		list.add(createCp1252("  20%          16.66€      3.33€\r\n"));

		list.add(createCp1252("Merci de votre visite et. à bientôt.\r\n"));
		
		list.add(createCp1252("Conservez votre ticket il\r\n"));
		
		list.add(createCp1252("vous sera demandé pour tout échange.\r\n"));
		
		list.add(new byte[] { 0x1b, 0x64, 0x02 }); // Cut
		
		list.add(new byte[] { 0x07 }); // Kick cash drawer

		sendCommand(context, portName, portSettings, list);
	}

	/**
	 * This function shows how to print the receipt data of a Impact Dot Matrix printer.
	 * 
	 * @param context
	 *     Activity for displaying messages to the user
	 * @param portName
	 *     Port name to use for communication. This should be (TCP:<IPAddress>)
	 * @param portSettings
	 *     Should be blank
	 * @param res
	 *     The resources object containing the image data. ( e.g.) getResources())
	 */
	public static void PrintSampleReceiptbyDotPrinter_Portuguese(Context context, String portName, String portSettings) {
		ArrayList<byte[]> list = new ArrayList<byte[]>();

		list.add(new byte[] { 0x1b, 0x1d, 0x74, 0x20 }); // Code Page #1252 (Windows Latin-1)

		list.add(new byte[] { 0x1b, 0x44, 0x02, 0x06, 0x0a, 0x10, 0x14, 0x1a, 0x22, 0x24, 0x28, 0x00 }); // Set horizontal tab

		list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x01 }); // Alignment (center)

		// list.add("[If loaded.. Logo1 goes here]\r\n".getBytes());

		// list.add(new byte[]{0x1b, 0x1c, 0x70, 0x01, 0x00, '\r', '\n'}); //Stored Logo Printing

		// Notice that we use a unicode representation because that is
		// how Java expresses these bytes as double byte unicode

		// Character expansion
		list.add(new byte[] { 0x1b, 0x68, 0x01 });

		list.add(createCp1252("\nCOMERCIAL DE ALIMENTOS CARREFOUR LTDA.\r\n"));
		
		list.add(new byte[] { 0x1b, 0x68, 0x00 });

		list.add(createCp1252("Avenida Moyses Roysen, S/N Vila Guilherme\r\n"));
		
		list.add(createCp1252("Cep: 02049-010 – Sao Paulo – SP\r\n"));

		list.add(createCp1252("CNPJ: 62.545.579/0013-69\r\n"));
		list.add(createCp1252("IE:110.819.138.118  IM: 9.041.041-5\r\n"));
		
		list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x00 }); // Alignment

		list.add(new byte[] { 0x1b, 0x44, 0x02, 0x10, 0x22, 0x00 }); // Set horizontal tab

		list.add(createCp1252("------------------------------------------\r\n"));
		
		list.add(createCp1252("MM/DD/YYYY HH:MM:SS  CCF:133939 COO:227808\r\n"));

		list.add(createCp1252("------------------------------------------\r\n"));

		list.add(createCp1252("CUPOM FISCAL\r\n"));
		
		list.add(createCp1252("------------------------------------------\r\n"));

		list.add(createCp1252("01 2505 CAFÉ DO PONTO TRAD A  1un F1 8,15)\r\n"));

		list.add(createCp1252("02 2505 CAFÉ DO PONTO TRAD A  1un F1 8,15)\r\n"));

		list.add(createCp1252("03 2505 CAFÉ DO PONTO TRAD A  1un F1 8,15)\r\n"));

		list.add(createCp1252("04 6129 AGU MIN NESTLE 510ML  1un F1 1,39)\r\n"));

		list.add(createCp1252("05 6129 AGU MIN NESTLE 510ML  1un F1 1,39)\r\n"));

		list.add(createCp1252("------------------------------------------\r\n"));

		// Character expansion
		list.add(new byte[] { 0x1b, 0x69, 0x00, 0x01 });

		list.add(createCp1252("TOTAL  R$ \t\t\t  27,23\r\n"));

		list.add(new byte[] { 0x1b, 0x69, 0x00, 0x00 });

		list.add(createCp1252("DINHEIROv \t\t\t\t\t\t  29,00\r\n"));

		list.add(createCp1252("TROCO R$  \t\t\t\t\t\t   1,77\r\n"));

		list.add(createCp1252("Valor dos Tributos R$2,15 (7,90%)\r\n"));
		
		list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x01 }); // Alignment (center)

		list.add(createCp1252("ITEM(S) CINORADIS 5\r\n"));

		list.add(createCp1252("OP.:15326  PDV:9  BR,BF:93466\r\n"));

		list.add(createCp1252("OBRIGADO PERA PREFERENCIA.\r\n"));

		list.add(new byte[] { 0x1b, 0x57, 0x01 });

		list.add(createCp1252("VOLTE SEMPRE!\r\n"));

		list.add(new byte[] { 0x1b, 0x57, 0x00 });

		list.add(createCp1252("SAC 0800 724 2822\r\n"));

		list.add(createCp1252("------------------------------------------\r\n"));

		list.add(createCp1252("MD5:  fe028828a532a7dbaf4271155aa4e2db\r\n"));
		
		list.add(createCp1252("Calypso_CA CA.20.c13 – Unisys Brasil\r\n"));
		
		list.add(createCp1252("------------------------------------------\r\n"));

		list.add(createCp1252("DARUMA AUTOMAÇÃO   MACH 2\r\n"));

		list.add(createCp1252("ECF-IF VERSÃO:01,00,00 ECF:093\r\n"));
		
		list.add(createCp1252("Lj:0204 OPR:ANGELA JORGE\r\n"));

		list.add(createCp1252("DDDDDDDDDAEHFGBFCC\r\n"));
		
		list.add(createCp1252("MM/DD/YYYY HH:MM:SS\r\n"));
		
		list.add(createCp1252("FAB:DR0911BR000000275026\r\n"));

		list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x00 });

		list.add(new byte[] { 0x1b, 0x64, 0x02 }); // Cut
		
		list.add(new byte[] { 0x07 }); // Kick cash drawer

		sendCommand(context, portName, portSettings, list);
	}

	/**
	 * This function shows how to print the receipt data of a Impact Dot Matrix printer.
	 * 
	 * @param context
	 *     Activity for displaying messages to the user
	 * @param portName
	 *     Port name to use for communication. This should be (TCP:<IPAddress>)
	 * @param portSettings
	 *     Should be blank
	 * @param res
	 *     The resources object containing the image data. ( e.g.) getResources())
	 */
	public static void PrintSampleReceiptbyDotPrinter_Spanish(Context context, String portName, String portSettings) {
		ArrayList<byte[]> list = new ArrayList<byte[]>();

		list.add(new byte[] { 0x1b, 0x1d, 0x74, 0x20 }); // Code Page #1252 (Windows Latin-1)

		list.add(new byte[] { 0x1b, 0x44, 0x02, 0x06, 0x0a, 0x10, 0x14, 0x1a, 0x22, 0x24, 0x28, 0x00 }); // Set horizontal tab

		list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x01 }); // Alignment (center)

		// list.add("[If loaded.. Logo1 goes here]\r\n".getBytes());

		// list.add(new byte[]{0x1b, 0x1c, 0x70, 0x01, 0x00, '\r', '\n'}); //Stored Logo Printing

		// Notice that we use a unicode representation because that is
		// how Java expresses these bytes as double byte unicode

		// Character expansion
		list.add(new byte[] { 0x1b, 0x68, 0x01 });

		list.add(createCp1252("BAR RESTAURANT EL POZO\r\n"));

		list.add(new byte[] { 0x1b, 0x68, 0x00 });

		list.add(createCp1252("C/.ROCAFORT 187 08029 BARCELONA\r\n"));
		
		list.add(createCp1252("NIF :X-3856907Z  TEL :934199465\r\n"));

		list.add(createCp1252("------------------------------------------\r\n"));
		
		list.add(createCp1252("MESA: 100 P: - FECHA: YYYY-MM-DD\r\n"));
		list.add(createCp1252("CAN P/U DESCRIPCION  SUMA\r\n"));

		list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x00 }); // Alignment

		list.add(createCp1252("------------------------------------------\r\n"));

		list.add(createCp1252(" 4\t 3,00\t JARRA  CERVESA \t\t 12,00\r\n"));
		
		list.add(createCp1252(" 1\t 1,60\t COPA DE CERVESA\t\t  1,60\r\n"));
		
		list.add(createCp1252("------------------------------------------\r\n"));

		list.add(createCp1252("\t\t\t\t\t SUB TOTAL :\t\t 13,60\r\n"));
		
		list.add(new byte[] { 0x1b, 0x57, 0x01 });
		
		list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x02 });
		
		list.add(createCp1252("TOTAL:    13,60 EUROS\r\n"));
		
		list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x00 });
		
		list.add(new byte[] { 0x1b, 0x57, 0x00 });

		list.add(createCp1252("NO: 000018851  IVA INCLUIDO\r\n"));
		
		list.add(createCp1252("------------------------------------------\r\n"));

		list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x01 });
		
		list.add(new byte[] { 0x1b, 0x68, 0x01 });
		
		list.add(createCp1252("**** GRACIAS POR SU VISITA! ****\r\n"));

		list.add(new byte[] { 0x1b, 0x68, 0x00 });

		list.add(new byte[] { 0x1b, 0x68, 0x00 });

		list.add(new byte[] { 0x1b, 0x64, 0x02 }); // Cut
		
		list.add(new byte[] { 0x07 }); // Kick cash drawer

		sendCommand(context, portName, portSettings, list);
	}

	/**
	 * This function shows how to print the receipt data of a Impact Dot Matrix printer.
	 * 
	 * @param context
	 *     Activity for displaying messages to the user
	 * @param portName
	 *     Port name to use for communication. This should be (TCP:<IPAddress>)
	 * @param portSettings
	 *     Should be blank
	 * @param res
	 *     The resources object containing the image data. ( e.g.) getResources())
	 */
	public static void PrintSampleReceiptbyDotPrinter_Russian(Context context, String portName, String portSettings) {
		ArrayList<byte[]> list = new ArrayList<byte[]>();

		list.add(new byte[] { 0x1b, 0x1d, 0x74, 0x22 }); // Code Page #1251 (Windows Latin-1)

		list.add(new byte[] { 0x1b, 0x44, 0x02, 0x06, 0x0a, 0x10, 0x14, 0x1a, 0x22, 0x24, 0x28, 0x00 }); // Set horizontal tab

		list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x01 });
	    
		// Character expansion
		list.add(new byte[] { 0x1b, 0x57, 0x01 });
		
		list.add(new byte[] { 0x1b, 0x68, 0x01 });

		list.add(createCp1251("Р  Е  Л  А  К  С\n"));

		list.add(new byte[] { 0x1b, 0x57, 0x00 });
		
		list.add(new byte[] { 0x1b, 0x68, 0x00 });

		list.add(createCp1251("ООО “РЕЛАКС”\n"));
		
		list.add(createCp1251("СПб., Малая Балканская, д. 38, лит. А\n"));
		
		list.add(createCp1251("тел. 307-07-12\n"));
		
		list.add(createCp1251("РЕГ №322736 ИНН : 123321\n"));
		
		list.add(createCp1251("01  Белякова И.А.  КАССА: 0020 ОТД.01\n"));
		
		list.add(createCp1251("ЧЕК НА ПРОДАЖУ  No 84373\n"));

		list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x00 });

		list.add(createCp1251("------------------------------------------\r\n"));
		
		list.add(createCp1251("1. \t Яблоки Айдаред, кг \t 144.50\n"));
		
		list.add(createCp1251("2. \t Соус соевый Sen So \t  36.40\n"));
		
		list.add(createCp1251("3. \t Соус томатный Клас \t  19.90\n"));
		
		list.add(createCp1251("4. \t Ребра свиные в.к м \t  78.20\n"));
		
		list.add(createCp1251("5. \t Масло подсол раф д \t 114.00\n"));
		
		list.add(createCp1251("6. \t Блокнот 10х14см сп \t 164.00\n"));
		
		list.add(createCp1251("7. \t Морс Северная Ягод \t  99.90\n"));
		
		list.add(createCp1251("8. \t Активия Биойогурт  \t  43.40\n"));
		
		list.add(createCp1251("9. \t Бублики Украинские \t  26.90\n"));
		
		list.add(createCp1251("10.\t Активия Биойогурт  \t  43.40\n"));
		
		list.add(createCp1251("11.\t Сахар-песок 1кг    \t  58.40\n"));
		
		list.add(createCp1251("12.\t Хлопья овсяные Ясн \t  38.40\n"));
		
		list.add(createCp1251("13.\t Кинза 50г          \t  39.90\n"));
		
		list.add(createCp1251("14.\t Пемза “Сердечко” .Т\t  37.90\n"));
		
		list.add(createCp1251("15.\t Приправа Santa Mar \t  47.90\n"));
		
		list.add(createCp1251("16.\t Томаты слива Выбор \t 162.00\n"));
		
		list.add(createCp1251("17.\t Бонд Стрит Ред Сел \t  56.90\n"));
		
		list.add(createCp1251("------------------------------------------\r\n"));
		
		list.add(createCp1251("------------------------------------------\r\n"));
		
		list.add(createCp1251("ДИСКОНТНАЯ КАРТА  No: 2440012489765\n"));
		
		list.add(createCp1251("------------------------------------------\r\n"));
		
		list.add(createCp1251("ИТОГО  К  ОПЛАТЕ \t = 1212.00\n"));
		
		list.add(createCp1251("НАЛИЧНЫЕ         \t = 1212.00\n"));
		
		list.add(createCp1251("ВАША СКИДКА : 0.41\n"));

		list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x01 });

		list.add(createCp1251("ЦЕНЫ УКАЗАНЫ С УЧЕТОМ СКИДКИ\n"));
		
		list.add(createCp1251("08-02-2015 09:49  0254.0130604\n"));
		
		list.add(createCp1251("00083213 #060127\n"));
		
		list.add(createCp1251("СПАСИБО ЗА ПОКУПКУ !\n"));
		
		list.add(createCp1251("МЫ  ОТКРЫТЫ ЕЖЕДНЕВНО С 9 ДО 23\n"));
		
		list.add(createCp1251("СОХРАНЯЙТЕ, ПОЖАЛУЙСТА , ЧЕК\n"));

		list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x00 });

		list.add(new byte[] { 0x1b, 0x64, 0x02 }); // Cut
		
		list.add(new byte[] { 0x07 }); // Kick cash drawer

		sendCommand(context, portName, portSettings, list);
	}

	/**
	 * This function shows how to print the receipt data of a thermal POS printer.
	 * 
	 * @param context
	 *     Activity for displaying messages to the user
	 * @param portName
	 *     Port name to use for communication. This should be (TCP:<IPAddress>)
	 * @param portSettings
	 *     Should be blank
	 * @param commandType
	 *     Command type to use for printing. This should be ("Line" or "Raster")
	 * @param res
	 *     The resources object containing the image data. ( e.g.) getResources())
	 * @param strPrintArea
	 *     Printable area size, This should be ("3inch (80mm)" or "4inch (112mm)")
	 */
	public static void PrintSampleReceipt(Context context, String portName, String portSettings, String commandType, Resources res, String strPrintArea, RasterCommand rasterType) {
		if (commandType == "Line") {
			if (strPrintArea.equals("2inch (58mm)")) {
				ArrayList<byte[]> list = new ArrayList<byte[]>();

				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x01 }); // Alignment (center)

				// list.add("[If loaded.. Logo1 goes here]\r\n".getBytes());

				// list.add(new byte[]{0x1b, 0x1c, 0x70, 0x01, 0x00, '\r', '\n'}); //Stored Logo Printing

				list.add("\nStar Clothing Boutique\r\n".getBytes());
				list.add("123 Star Road\r\nCity, State 12345\r\n\r\n".getBytes());

				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x00 }); // Alignment

				list.add(new byte[] { 0x1b, 0x44, 0x02, 0x10, 0x22, 0x00 }); // Set horizontal tab

				list.add("Date: MM/DD/YYYY".getBytes());

				list.add(new byte[] { ' ', 0x09, ' ' }); // Moving Horizontal Tab

				list.add("Time:HH:MM PM\r\n--------------------------------\r\n\r\n".getBytes());

				list.add(new byte[] { 0x1b, 0x45 }); // bold

				list.add("SALE \r\n".getBytes());

				list.add(new byte[] { 0x1b, 0x46 }); // bolf off

				// Notice that we use a unicode representation because that is
				// how Java expresses these bytes as double byte unicode
				// This will TAB to the next horizontal position
				list.add("SKU         Description    Total\r\n".getBytes());
				list.add("300678566   PLAIN T-SHIRT  10.99\r\n".getBytes());
				list.add("300692003   BLACK DENIM    29.99\r\n".getBytes());
				list.add("300651148   BLUE DENIM     29.99\r\n".getBytes());
				list.add("300642980   STRIPED DRESS  49.99\r\n".getBytes());
				list.add("300638471   BLACK BOOTS    35.99\r\n\r\n".getBytes());
				list.add("Subtotal \u0009\u0009          156.95\r\n".getBytes());
				list.add("Tax \u0009\u0009            0.00\r\n".getBytes());
				list.add("--------------------------------\r\n".getBytes());
				list.add("Total".getBytes());

				// Character expansion
				list.add(new byte[] { 0x06, 0x09, 0x1b, 0x69, 0x01, 0x01 });

				list.add("        $156.95\r\n".getBytes());

				list.add(new byte[] { 0x1b, 0x69, 0x00, 0x00 }); // Cancel Character Expansion

				list.add("--------------------------------\r\n\r\n".getBytes());
				list.add("Charge\r\n159.95\r\n".getBytes());
				list.add("Visa XXXX-XXXX-XXXX-0123\r\n\r\n".getBytes());
				list.add("\u001b\u0034Refunds and Exchanges\u001b\u0035\r\n".getBytes()); // Specify/Cancel White/Black Invert
				list.add(("Within " + "\u001b\u002d\u0001" + "30 days\u001b\u002d\u0000" + " with receipt\r\n").getBytes()); // Specify/Cancel Underline Printing
				list.add("And tags attached\r\n\r\n".getBytes());

				// 1D barcode example
				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x01 });
				list.add(new byte[] { 0x1b, 0x62, 0x06, 0x02, 0x02 });

				list.add(" 12ab34cd56\u001e\r\n".getBytes());

				list.add(new byte[] { 0x1b, 0x64, 0x02 }); // Cut
				list.add(new byte[] { 0x07 }); // Kick cash drawer

				sendCommand(context, portName, portSettings, list);
			} else if (strPrintArea.equals("3inch (80mm)")) {
				ArrayList<byte[]> list = new ArrayList<byte[]>();

				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x01 }); // Alignment (center)

				// list.add("[If loaded.. Logo1 goes here]\r\n".getBytes());

				// list.add(new byte[]{0x1b, 0x1c, 0x70, 0x01, 0x00, '\r', '\n'}); //Stored Logo Printing

				list.add("\nStar Clothing Boutique\r\n".getBytes());
				list.add("123 Star Road\r\nCity, State 12345\r\n\r\n".getBytes());

				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x00 }); // Alignment

				list.add(new byte[] { 0x1b, 0x44, 0x02, 0x10, 0x22, 0x00 }); // Set horizontal tab

				list.add("Date: MM/DD/YYYY".getBytes());

				list.add(new byte[] { ' ', 0x09, ' ' }); // Moving Horizontal Tab

				list.add("Time:HH:MM PM\r\n------------------------------------------------\r\n\r\n".getBytes());

				list.add(new byte[] { 0x1b, 0x45 }); // bold

				list.add("SALE \r\n".getBytes());

				list.add(new byte[] { 0x1b, 0x46 }); // bolf off

				list.add("SKU ".getBytes());

				list.add(new byte[] { 0x09 });

				// Notice that we use a unicode representation because that is
				// how Java expresses these bytes as double byte unicode
				// This will TAB to the next horizontal position
				list.add("  Description   \u0009         Total\r\n".getBytes());
				list.add("300678566 \u0009  PLAIN T-SHIRT\u0009         10.99\r\n".getBytes());
				list.add("300692003 \u0009  BLACK DENIM\u0009         29.99\r\n".getBytes());
				list.add("300651148 \u0009  BLUE DENIM\u0009         29.99\r\n".getBytes());
				list.add("300642980 \u0009  STRIPED DRESS\u0009         49.99\r\n".getBytes());
				list.add("300638471 \u0009  BLACK BOOTS\u0009         35.99\r\n\r\n".getBytes());
				list.add("Subtotal \u0009\u0009        156.95\r\n".getBytes());
				list.add("Tax \u0009\u0009          0.00\r\n".getBytes());
				list.add("------------------------------------------------\r\n".getBytes());
				list.add("Total".getBytes());

				// Character expansion
				list.add(new byte[] { 0x06, 0x09, 0x1b, 0x69, 0x01, 0x01 });

				list.add("        $156.95\r\n".getBytes());

				list.add(new byte[] { 0x1b, 0x69, 0x00, 0x00 }); // Cancel Character Expansion

				list.add("------------------------------------------------\r\n\r\n".getBytes());
				list.add("Charge\r\n159.95\r\n".getBytes());
				list.add("Visa XXXX-XXXX-XXXX-0123\r\n\r\n".getBytes());
				list.add("\u001b\u0034Refunds and Exchanges\u001b\u0035\r\n".getBytes()); // Specify/Cancel White/Black Invert
				list.add(("Within " + "\u001b\u002d\u0001" + "30 days\u001b\u002d\u0000" + " with receipt\r\n").getBytes()); // Specify/Cancel Underline Printing
				list.add("And tags attached\r\n\r\n".getBytes());

				// 1D barcode example
				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x01 });
				list.add(new byte[] { 0x1b, 0x62, 0x06, 0x02, 0x02 });

				list.add(" 12ab34cd56\u001e\r\n".getBytes());

				list.add(new byte[] { 0x1b, 0x64, 0x02 }); // Cut
				list.add(new byte[] { 0x07 }); // Kick cash drawer

				sendCommand(context, portName, portSettings, list);
			} else if (strPrintArea.equals("4inch (112mm)")) {
				ArrayList<byte[]> list = new ArrayList<byte[]>();

				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x01 }); // Alignment (center)

				// list.add("[If loaded.. Logo1 goes here]\r\n".getBytes());

				// list.add(new byte[]{0x1b, 0x1c, 0x70, 0x01, 0x00, '\r', '\n'}); //Stored Logo Printing

				list.add("\nStar Clothing Boutique\r\n".getBytes());
				list.add("123 Star Road\r\nCity, State 12345\r\n\r\n".getBytes());

				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x00 }); // Alignment

				list.add(new byte[] { 0x1b, 0x44, 0x02, 0x10, 0x22, 0x00 }); // Set horizontal tab

				list.add("Date: MM/DD/YYYY     \u0009               \u0009       Time:HH:MM PM\r\n".getBytes());
				list.add("---------------------------------------------------------------------\r\n\r\n".getBytes());

				list.add(new byte[] { 0x1b, 0x45 }); // bold

				list.add("SALE \r\n".getBytes());

				list.add(new byte[] { 0x1b, 0x46 }); // bolf off

				list.add("SKU ".getBytes());

				list.add(new byte[] { 0x09 });

				// Notice that we use a unicode representation because that is
				// how Java expresses these bytes as double byte unicode
				// This will TAB to the next horizontal position
				list.add("            Description         \u0009\u0009\u0009                Total\r\n".getBytes());
				list.add("300678566      \u0009            PLAIN T-SHIRT\u0009                       10.99\r\n".getBytes());
				list.add("300692003      \u0009            BLACK DENIM\u0009                         29.99\r\n".getBytes());
				list.add("300651148      \u0009            BLUE DENIM\u0009                          29.99\r\n".getBytes());
				list.add("300642980      \u0009            STRIPED DRESS\u0009                       49.99\r\n".getBytes());
				list.add("300638471      \u0009            BLACK BOOTS\u0009                         35.99\r\n\r\n".getBytes());
				list.add("Subtotal       \u0009                       \u0009                        156.95\r\n".getBytes());
				list.add("Tax            \u0009                       \u0009                          0.00\r\n".getBytes());
				list.add("---------------------------------------------------------------------\r\n".getBytes());
				list.add("Total".getBytes());

				// Character expansion
				list.add(new byte[] { 0x06, 0x09, 0x1b, 0x69, 0x01, 0x01 });

				list.add("\u0009         $156.95\r\n".getBytes());

				list.add(new byte[] { 0x1b, 0x69, 0x00, 0x00 }); // Cancel Character Expansion

				list.add("---------------------------------------------------------------------\r\n\r\n".getBytes());
				list.add("Charge\r\n159.95\r\n".getBytes());
				list.add("Visa XXXX-XXXX-XXXX-0123\r\n\r\n".getBytes());
				list.add("\u001b\u0034Refunds and Exchanges\u001b\u0035\r\n".getBytes()); // Specify/Cancel White/Black Invert
				list.add(("Within " + "\u001b\u002d\u0001" + "30 days\u001b\u002d\u0000" + " with receipt\r\n").getBytes()); // Specify/Cancel Underline Printing
				list.add("And tags attached\r\n\r\n".getBytes());

				// 1D barcode example
				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x01 });
				list.add(new byte[] { 0x1b, 0x62, 0x06, 0x02, 0x02 });

				list.add(" 12ab34cd56\u001e\r\n".getBytes());

				list.add(new byte[] { 0x1b, 0x64, 0x02 }); // Cut
				list.add(new byte[] { 0x07 }); // Kick cash drawer

				sendCommand(context, portName, portSettings, list);
			}
		} else if (commandType == "Raster") {
			if (strPrintArea.equals("2inch (58mm)")) {
				ArrayList<byte[]> list = new ArrayList<byte[]>();

				printableArea = 384; // Printable area in paper is 384(dot)

				RasterDocument rasterDoc = new RasterDocument(RasSpeed.Medium, RasPageEndMode.FeedAndFullCut, RasPageEndMode.FeedAndFullCut, RasTopMargin.Standard, 0, 0, 0);
				
				if (rasterType == RasterCommand.Standard) {
					list.add(rasterDoc.BeginDocumentCommandData());
				}

				String textToPrint = (
						"       Star Clothing Boutique\r\n" +
						"                123 Star Road\r\n" +
						"             City, State 12345\r\n\r\n" +
						"Date: MM/DD/YYYY\r\n" +
						"Time:HH:MM PM\r\n" +
						"---------------------------------------------\r");
				list.add(createRasterCommand(textToPrint, 13, 0, rasterType));

				list.add(createRasterCommand("SALE", 13, Typeface.BOLD, rasterType));

				textToPrint = (
						"SKU              Description   Total\r\n" +
						"300678566  PLAIN T-SHIRT\r\n" +
						"                                              10.99\n" +
						"300692003  BLACK DENIM\n" +
						"                                              29.99\n" +
						"300651148  BLUE DENIM\n" +
						"                                              29.99\n" +
						"300642980  STRIPED DRESS\n" +
						"                                              49.99\n" +
						"300638471 BLACK BOOTS\n" +
						"                                              35.99\n\n" +
						"Subtotal                            156.95\n" +
						"Tax                                         0.00\n" +
						"---------------------------------------------\r\n" +
						"Total                                $156.95\n" +
						"---------------------------------------------\r\n\r\n" +
						"Charge\r\n159.95\r\n" + "Visa XXXX-XXXX-XXXX-0123\r\n");

				list.add(createRasterCommand(textToPrint, 13, 0, rasterType));

				list.add(createRasterCommand("Refunds and Exchanges", 13, Typeface.BOLD, rasterType));

				textToPrint = ("Within 30 days with receipt\r\n" + "And tags attached");
				list.add(createRasterCommand(textToPrint, 13, 0, rasterType));

				Bitmap bm = BitmapFactory.decodeResource(res, R.drawable.qrcode);
				StarBitmap starbitmap = new StarBitmap(bm, false, 146);
				if (rasterType == RasterCommand.Standard) {
					list.add(starbitmap.getImageRasterDataForPrinting_Standard(true));
				} else {
					list.add(starbitmap.getImageRasterDataForPrinting_graphic(true));
				}

				if (rasterType == RasterCommand.Standard) {
					list.add(rasterDoc.EndDocumentCommandData());
					list.add(new byte[] { 0x07 }); // Kick cash drawer
				} else {
					list.add(new byte[] { 0x1b, 0x64, 0x02 }); // Cut
				}

				sendCommand(context, portName, portSettings, list);
			} else if (strPrintArea.equals("3inch (80mm)")) {
				ArrayList<byte[]> list = new ArrayList<byte[]>();

				printableArea = 576; // Printable area in paper is 576(dot)

				RasterDocument rasterDoc = new RasterDocument(RasSpeed.Medium, RasPageEndMode.FeedAndFullCut, RasPageEndMode.FeedAndFullCut, RasTopMargin.Standard, 0, 0, 0);
				
				if (rasterType == RasterCommand.Standard) {
					list.add(rasterDoc.BeginDocumentCommandData());
				}

				String textToPrint = (
						"                       Star Clothing Boutique\r\n" +
						"                             123 Star Road\r\n" +
						"                           City, State 12345\r\n\r\n" +
						"Date: MM/DD/YYYY               Time:HH:MM PM\r\n" +
						"-----------------------------------------------------------------------\r");
				list.add(createRasterCommand(textToPrint, 13, 0, rasterType));

				list.add(createRasterCommand("SALE", 13, Typeface.BOLD, rasterType));

				textToPrint = (
						"SKU \t\t\t                 Description \t\t                Total\r\n" +
						"300678566 \t\t\t      PLAIN T-SHIRT		\t\t    10.99\n" +
						"300692003 \t\t\t      BLACK DENIM		\t\t    29.99\n" +
						"300651148 \t\t\t      BLUE DENIM		\t\t       29.99\n" +
						"300642980 \t\t\t      STRIPED DRESS		\t       49.99\n" +
						"300638471 \t\t\t      BLACK BOOTS		\t\t       35.99\n\n" +
						"Subtotal \t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t   156.95\r\n" +
						"Tax \t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t    0.00\r\n" +
						"-----------------------------------------------------------------------\r\n" +
						"Total   \t                                                      $156.95\r\n" +
						"-----------------------------------------------------------------------\r\n\r\n" +
						"Charge\r\n159.95\r\n" + "Visa XXXX-XXXX-XXXX-0123\r\n");

				list.add(createRasterCommand(textToPrint, 13, 0, rasterType));

				list.add(createRasterCommand("Refunds and Exchanges", 13, Typeface.BOLD, rasterType));

				textToPrint = ("Within 30 days with receipt\r\n" + "And tags attached");
				list.add(createRasterCommand(textToPrint, 13, 0, rasterType));

				Bitmap bm = BitmapFactory.decodeResource(res, R.drawable.qrcode);
				StarBitmap starbitmap = new StarBitmap(bm, false, 146);
				if (rasterType == RasterCommand.Standard) {
					list.add(starbitmap.getImageRasterDataForPrinting_Standard(true));
				} else {
					list.add(starbitmap.getImageRasterDataForPrinting_graphic(true));
				}

				if (rasterType == RasterCommand.Standard) {
					list.add(rasterDoc.EndDocumentCommandData());
					list.add(new byte[] { 0x07 }); // Kick cash drawer
				} else {
					list.add(new byte[] { 0x1b, 0x64, 0x02 }); // Cut
				}

				sendCommand(context, portName, portSettings, list);
			} else if (strPrintArea.equals("4inch (112mm)")) {
				ArrayList<byte[]> list = new ArrayList<byte[]>();

				printableArea = 832; // Printable area in paper is 832(dot)

				RasterDocument rasterDoc = new RasterDocument(RasSpeed.Medium, RasPageEndMode.FeedAndFullCut, RasPageEndMode.FeedAndFullCut, RasTopMargin.Standard, 0, 0, 0);
				
				if (rasterType == RasterCommand.Standard) {
					list.add(rasterDoc.BeginDocumentCommandData());
				}

				String textToPrint = (
						"                                          Star Clothing Boutique\r\n" +
						"                                                123 Star Road\r\n" +
						"                                              City, State 12345\r\n\r\n" +
						"Date: MM/DD/YYYY                                                      Time:HH:MM PM\r\n" +
						"-------------------------------------------------------------------------------------------------------\r");
				list.add(createRasterCommand(textToPrint, 13, 0, rasterType));

				list.add(createRasterCommand("SALE", 13, Typeface.BOLD, rasterType));

				textToPrint = (
						"SKU \t\t\t                                   Description \t\t                                  Total\r\n" +
						"300678566 \t\t\t                        PLAIN T-SHIRT		\t\t                      10.99\n" +
						"300692003 \t\t\t                        BLACK DENIM		\t\t                      29.99\n" +
						"300651148 \t\t\t                        BLUE DENIM		\t\t                         29.99\n" +
						"300642980 \t\t\t                        STRIPED DRESS		\t                         49.99\n" +
						"300638471 \t\t\t                        BLACK BOOTS		\t\t                         35.99\n\n");

				list.add(createRasterCommand(textToPrint, 13, 0, rasterType));

				textToPrint = (
						"Subtotal\t\t\t\t                                                                                  156.95\r\n" +
						"Tax		\t\t\t\t                                                                                         0.00\r\n" +
						"-------------------------------------------------------------------------------------------------------\r" +
						"Total   \t                                                                                       $156.95\r\n" +
						"-------------------------------------------------------------------------------------------------------\r\n\r\n" +
						"Charge\r\n159.95\r\n" + "Visa XXXX-XXXX-XXXX-0123\r\n");

				list.add(createRasterCommand(textToPrint, 13, 0, rasterType));

				list.add(createRasterCommand("Refunds and Exchanges", 13, Typeface.BOLD, rasterType));

				textToPrint = ("Within 30 days with receipt\r\n" + "And tags attached");
				list.add(createRasterCommand(textToPrint, 13, 0, rasterType));

				Bitmap bm = BitmapFactory.decodeResource(res, R.drawable.qrcode);
				StarBitmap starbitmap = new StarBitmap(bm, false, 146);
				if (rasterType == RasterCommand.Standard) {
					list.add(starbitmap.getImageRasterDataForPrinting_Standard(true));
				} else {
					list.add(starbitmap.getImageRasterDataForPrinting_graphic(true));
				}

				if (rasterType == RasterCommand.Standard) {
					list.add(rasterDoc.EndDocumentCommandData());
					list.add(new byte[] { 0x07 }); // Kick cash drawer
				} else {
					list.add(new byte[] { 0x1b, 0x64, 0x02 }); // Cut
				}

				sendCommand(context, portName, portSettings, list);
			}
		}
	}

	/**
	 * This function shows how to print the receipt data of a thermal POS printer.
	 * 
	 * @param context
	 *     Activity for displaying messages to the user
	 * @param portName
	 *     Port name to use for communication. This should be (TCP:<IPAddress>)
	 * @param portSettings
	 *     Should be blank
	 * @param commandType
	 *     Command type to use for printing. This should be ("Line" or "Raster")
	 * @param res
	 *     The resources object containing the image data. ( e.g.) getResources())
	 * @param strPrintArea
	 *     Printable area size, This should be ("3inch (80mm)" or "4inch (112mm)")
	 */
	public static void PrintSampleReceipt_French(Context context, String portName, String portSettings, String commandType, Resources res, String strPrintArea, RasterCommand rasterType) {
		if (commandType == "Line") {
			if (strPrintArea.equals("2inch (58mm)")) {
				ArrayList<byte[]> list = new ArrayList<byte[]>();

				list.add(new byte[] { 0x1b, 0x1d, 0x74, 0x20 }); // Code Page #1252 (Windows Latin-1)

				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x01 }); // Alignment (center)

				// list.add("[If loaded.. Logo1 goes here]\r\n".getBytes());

				// list.add(new byte[]{0x1b, 0x1c, 0x70, 0x01, 0x00, '\r', '\n'}); //Stored Logo Printing

				// Notice that we use a unicode representation because that is
				// how Java expresses these bytes as double byte unicode

				// Character expansion
				list.add(new byte[] { 0x06, 0x09, 0x1b, 0x69, 0x01, 0x01 });

				list.add(createCp1252("\nORANGE\r\n"));
				list.add(new byte[] { 0x1b, 0x69, 0x00, 0x00 }); // Cancel Character Expansion

				list.add(createCp1252("36 AVENUE LA MOTTE PICQUET\r\n\r\n"));

				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x00 }); // Alignment

				list.add(new byte[] { 0x1b, 0x44, 0x02, 0x10, 0x22, 0x00 }); // Set horizontal tab

				list.add(createCp1252("--------------------------------\r\n"));
				
				list.add(createCp1252("Date   : MM/DD/YYYY\r\n"));
				list.add(createCp1252("Heure  : HH:MM\r\n"));
				
				list.add(createCp1252("Boutique: OLUA23    Caisse: 0001\r\n"));
				list.add(createCp1252("Conseiller: 002970  Ticket: 3881\r\n"));
				
				list.add(createCp1252("--------------------------------\r\n\r\n"));
				
				list.add(createCp1252("Vous avez été servi par : Souad\r\n\r\n"));

				list.add(createCp1252("CAC IPHONE ORANGE\r\n"));
				
				list.add(createCp1252("3700615033581 1 X 19.99€  19.99€\r\n\r\n"));
				
				list.add(createCp1252("dont contribution\r\n environnementale :\r\n"));
				
				list.add(createCp1252("CAC IPHONE ORANGE	          0.01€\r\n"));

				list.add(createCp1252("--------------------------------\r\n"));

				list.add(createCp1252("    1 Piéce(s)   Total : 	 19.99€\r\n"));
				
				list.add(createCp1252("      Mastercard Visa  :  19.99€\r\n\r\n"));
				
				list.add(createCp1252("Taux TVA    Montant H.T.   T.V.A\r\n"));
				
				list.add(createCp1252("  20%          16.66€      3.33€\r\n"));

				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x01 }); // Alignment (center)

				list.add(createCp1252("Merci de votre visite et.\r\n"));
				list.add(createCp1252("à bientôt.\r\n"));
				list.add(createCp1252("Conservez votre ticket il\r\n"));
				list.add(createCp1252("vous sera demandé pour \r\n"));
				list.add(createCp1252("tout échange.\r\n"));
				
				// 1D barcode example
				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x01 });
				list.add(new byte[] { 0x1b, 0x62, 0x06, 0x02, 0x02 });

				list.add(createCp1252(" 12ab34cd56\u001e\r\n"));

				list.add(new byte[] { 0x1b, 0x64, 0x02 }); // Cut
				list.add(new byte[] { 0x07 }); // Kick cash drawer

				sendCommand(context, portName, portSettings, list);
			} else if (strPrintArea.equals("3inch (80mm)")) {
				ArrayList<byte[]> list = new ArrayList<byte[]>();

				list.add(new byte[] { 0x1b, 0x1d, 0x74, 0x20 }); // Code Page #1252 (Windows Latin-1)

				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x01 }); // Alignment (center)

				// list.add("[If loaded.. Logo1 goes here]\r\n".getBytes());

				// list.add(new byte[]{0x1b, 0x1c, 0x70, 0x01, 0x00, '\r', '\n'}); //Stored Logo Printing

				// Notice that we use a unicode representation because that is
				// how Java expresses these bytes as double byte unicode

				// Character expansion
				list.add(new byte[] { 0x06, 0x09, 0x1b, 0x69, 0x01, 0x01 });

				list.add(createCp1252("\nORANGE\r\n"));
				list.add(new byte[] { 0x1b, 0x69, 0x00, 0x00 }); // Cancel Character Expansion

				list.add(createCp1252("36 AVENUE LA MOTTE PICQUET\r\n\r\n"));

				list.add(new byte[] { 0x1b, 0x44, 0x02, 0x06, 0x0a, 0x10, 0x14, 0x1a, 0x22, 0x00 }); // Set horizontal tab

				list.add(createCp1252("------------------------------------------------\r\n"));
				
				list.add(createCp1252("Date: MM/DD/YYYY    Heure: HH:MM\r\n"));
				
				list.add(createCp1252("Boutique: OLUA23    Caisse: 0001\r\n"));
				list.add(createCp1252("Conseiller: 002970  Ticket: 3881\r\n"));
				
				list.add(createCp1252("------------------------------------------------\r\n\r\n"));

				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x00 }); // Alignment

				list.add(createCp1252("Vous avez été servi par : Souad\r\n\r\n"));

				list.add(createCp1252("CAC IPHONE ORANGE\r\n"));
				
				list.add(createCp1252("3700615033581 \t1\t X\t 19.99€\t  19.99€\r\n\r\n"));
				
				list.add(createCp1252("dont contribution environnementale :\r\n"));
				
				list.add(createCp1252("CAC IPHONE ORANGE\t\t  0.01€\r\n"));

				list.add(createCp1252("------------------------------------------------\r\n"));

				list.add(createCp1252("1 Piéce(s) Total :\t\t\t  19.99€\r\n"));
				
				list.add(createCp1252("Mastercard Visa  :\t\t\t  19.99€\r\n\r\n"));
				
				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x01 }); // Alignment (center)

				list.add(createCp1252("Taux TVA    Montant H.T.   T.V.A\r\n"));
				
				list.add(createCp1252("  20%          16.66€      3.33€\r\n"));

				list.add(createCp1252("Merci de votre visite et. à bientôt.\r\n"));
				
				list.add(createCp1252("Conservez votre ticket il\r\n"));
				
				list.add(createCp1252("vous sera demandé pour tout échange.\r\n"));
				
				// 1D barcode example
				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x01 });
				list.add(new byte[] { 0x1b, 0x62, 0x06, 0x02, 0x02 });

				list.add(createCp1252(" 12ab34cd56\u001e\r\n"));

				list.add(new byte[] { 0x1b, 0x64, 0x02 }); // Cut
				list.add(new byte[] { 0x07 }); // Kick cash drawer

				sendCommand(context, portName, portSettings, list);
			} else if (strPrintArea.equals("4inch (112mm)")) {
				ArrayList<byte[]> list = new ArrayList<byte[]>();

				list.add(new byte[] { 0x1b, 0x1d, 0x74, 0x20 }); // Code Page #1252 (Windows Latin-1)

				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x01 }); // Alignment (center)

				// list.add("[If loaded.. Logo1 goes here]\r\n".getBytes());

				// list.add(new byte[]{0x1b, 0x1c, 0x70, 0x01, 0x00, '\r', '\n'}); //Stored Logo Printing

				// Notice that we use a unicode representation because that is
				// how Java expresses these bytes as double byte unicode

				// Character expansion
				list.add(new byte[] { 0x06, 0x09, 0x1b, 0x69, 0x01, 0x01 });

				list.add(createCp1252("\nORANGE\r\n"));
				list.add(new byte[] { 0x1b, 0x69, 0x00, 0x00 }); // Cancel Character Expansion

				list.add(createCp1252("36 AVENUE LA MOTTE PICQUET\r\n\r\n"));

				list.add(new byte[] { 0x1b, 0x44, 0x02, 0x06, 0x0a, 0x10, 0x14, 0x1a, 0x22, 0x24, 0x28, 0x3c, 0x00 }); // Set horizontal tab

				list.add(createCp1252("---------------------------------------------------------------------\r\n"));
				
				list.add(createCp1252("Date: MM/DD/YYYY    Heure: HH:MM\r\n"));
				
				list.add(createCp1252("Boutique: OLUA23    Caisse: 0001\r\n"));
				list.add(createCp1252("Conseiller: 002970  Ticket: 3881\r\n"));
				
				list.add(createCp1252("---------------------------------------------------------------------\r\n"));

				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x00 }); // Alignment

				list.add(createCp1252("Vous avez été servi par : Souad\r\n\r\n"));

				list.add(createCp1252("CAC IPHONE ORANGE\r\n"));
				
				list.add(createCp1252("3700615033581 \t1\t X\t 19.99€\t\t\t\t  19.99€\r\n\r\n"));
				
				list.add(createCp1252("dont contribution environnementale :\r\n"));
				
				list.add(createCp1252("CAC IPHONE ORANGE\t\t  0.01€\r\n"));

				list.add(createCp1252("---------------------------------------------------------------------\r\n"));

				list.add(createCp1252("1 Piéce(s) Total :\t\t\t\t\t\t  19.99€\r\n"));
				
				list.add(createCp1252("Mastercard Visa  :\t\t\t\t\t\t  19.99€\r\n\r\n"));
				
				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x01 }); // Alignment (center)

				list.add(createCp1252("Taux TVA    Montant H.T.   T.V.A\r\n"));
				
				list.add(createCp1252("  20%          16.66€      3.33€\r\n"));

				list.add(createCp1252("Merci de votre visite et. à bientôt.\r\n"));
				
				list.add(createCp1252("Conservez votre ticket il\r\n"));
				
				list.add(createCp1252("vous sera demandé pour tout échange.\r\n"));
				
				// 1D barcode example
				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x01 });
				list.add(new byte[] { 0x1b, 0x62, 0x06, 0x02, 0x02 });

				list.add(createCp1252(" 12ab34cd56\u001e\r\n"));

				list.add(new byte[] { 0x1b, 0x64, 0x02 }); // Cut
				list.add(new byte[] { 0x07 }); // Kick cash drawer

				sendCommand(context, portName, portSettings, list);
			}
		} else if (commandType == "Raster") {
			if (strPrintArea.equals("2inch (58mm)")) {
				ArrayList<byte[]> list = new ArrayList<byte[]>();

				printableArea = 384; // Printable area in paper is 384(dot)

				RasterDocument rasterDoc = new RasterDocument(RasSpeed.Medium, RasPageEndMode.FeedAndFullCut, RasPageEndMode.FeedAndFullCut, RasTopMargin.Standard, 0, 0, 0);
				
				if (rasterType == RasterCommand.Standard) {
					list.add(rasterDoc.BeginDocumentCommandData());
				}

				String textToPrint = (
						"                    ORANGE\r\n" +
						"      36 AVENUE LA MOTTE \r\n" +
						"      PICQUET City, State 12345\r\n\r\n" +
						"---------------------------------------------\r" +
						"Date: MM/DD/YYYY\r\n" +
						"Time:HH:MM PM\r\n");
				list.add(createRasterCommand(textToPrint, 13, 0, rasterType));

				textToPrint = (
						"Boutique: OLUA23\r\n" +
						"Caisse: 0001\r\n" +
						"Conseiller: 002970\r\n" +
						"Ticket: 3881\r\n" +
						"---------------------------------------------\n" +
						"Vous avez été servi par : Souad\n" +
						"CAC IPHONE ORANGE\n" +
						"3700615033581 1 X 19.99€\n" +
						"                                            19.99€\n" +
						"dont contribution\r\n" +
						" environnementale :\n" +
						"CAC IPHONE ORANGE      0.01€\n" +
						"---------------------------------------------\n" +
						"        1 Piéce(s)    Total : 	 19.99€\n\n" +
						"        Mastercard Visa  :  19.99€\r\n" +
						"Taux TVA  Montant H.T. T.V.A\n" +
						"  20%             16.66€         3.33€\r\n" +
						"Merci de votre visite et.\n" +
						"à bientôt.\r\n" +
						"Conservez votre ticket il\n" +
						"vous sera demandé pour \n" +
						"tout échange.\r\n");

				list.add(createRasterCommand(textToPrint, 13, 0, rasterType));

				if (rasterType == RasterCommand.Standard) {
					list.add(rasterDoc.EndDocumentCommandData());
					list.add(new byte[] { 0x07 }); // Kick cash drawer
				} else {
					list.add(new byte[] { 0x1b, 0x64, 0x02 }); // Cut
				}

				sendCommand(context, portName, portSettings, list);
			} else if (strPrintArea.equals("3inch (80mm)")) {
				ArrayList<byte[]> list = new ArrayList<byte[]>();

				printableArea = 576; // Printable area in paper is 576(dot)

				RasterDocument rasterDoc = new RasterDocument(RasSpeed.Medium, RasPageEndMode.FeedAndFullCut, RasPageEndMode.FeedAndFullCut, RasTopMargin.Standard, 0, 0, 0);
				
				if (rasterType == RasterCommand.Standard) {
					list.add(rasterDoc.BeginDocumentCommandData());
				}

				String textToPrint = (
						"                                    ORANGE\r\n" +
						"                       36 AVENUE LA MOTTE \r\n" +
						"                    PICQUET City, State 12345\r\n\r\n" +
						"-----------------------------------------------------------------------\n" +
						"         Date: MM/DD/YYYY    Time:HH:MM PM\r\n");
				list.add(createRasterCommand(textToPrint, 13, 0, rasterType));

				textToPrint = (
						"                Boutique: OLUA23    Caisse: 0001\r\n" +
						"                  Conseiller: 002970  Ticket: 3881\r\n" +
						"-----------------------------------------------------------------------\n" +
						"Vous avez été servi par : Souad\n" +
						"CAC IPHONE ORANGE\n" +
						"3700615033581      1     X      19.99€         19.99€\n" +
						"dont contribution environnementale :\n" +
						"CAC IPHONE ORANGE                0.01€\n" +
						"-----------------------------------------------------------------------\n" +
						"        1 Piéce(s)    Total : 	                             19.99€\n\n" +
						"        Mastercard Visa  :                              19.99€\r\n" +
						"                 Taux TVA  Montant H.T. T.V.A\n" +
						"                 20%             16.66€         3.33€\r\n" +
						"             Merci de votre visite et. à bientôt.\r\n" +
						"            Conservez votre ticket il vous sera\n" +
						"                   demandé pour tout échange.\r\n");

				list.add(createRasterCommand(textToPrint, 13, 0, rasterType));

				if (rasterType == RasterCommand.Standard) {
					list.add(rasterDoc.EndDocumentCommandData());
					list.add(new byte[] { 0x07 }); // Kick cash drawer
				} else {
					list.add(new byte[] { 0x1b, 0x64, 0x02 }); // Cut
				}

				sendCommand(context, portName, portSettings, list);
			} else if (strPrintArea.equals("4inch (112mm)")) {
				ArrayList<byte[]> list = new ArrayList<byte[]>();

				printableArea = 832; // Printable area in paper is 832(dot)

				RasterDocument rasterDoc = new RasterDocument(RasSpeed.Medium, RasPageEndMode.FeedAndFullCut, RasPageEndMode.FeedAndFullCut, RasTopMargin.Standard, 0, 0, 0);

				if (rasterType == RasterCommand.Standard) {
					list.add(rasterDoc.BeginDocumentCommandData());
				}

				String textToPrint = (
						"                                                 ORANGE\r\n" +
						"                36 AVENUE LA MOTTE PICQUET City, State 12345\r\n\r\n" +
						"-------------------------------------------------------------------------------------------------------\n" +
						"                      Date: MM/DD/YYYY    Time:HH:MM PM\r\n");
				list.add(createRasterCommand(textToPrint, 13, 0, rasterType));

				textToPrint = (
						"                             Boutique: OLUA23    Caisse: 0001\r\n" +
						"                               Conseiller: 002970  Ticket: 3881\r\n" +
						"-------------------------------------------------------------------------------------------------------\n" +
						"Vous avez été servi par : Souad\n" +
						"CAC IPHONE ORANGE\n" +
						"3700615033581      1     X      19.99€                                19.99€\n" +
						"dont contribution environnementale :\n" +
						"CAC IPHONE ORANGE                             0.01€\n" +
						"-------------------------------------------------------------------------------------------------------\n" +
						"        1 Piéce(s)    Total : 	                                                    19.99€\n\n" +
						"        Mastercard Visa  :                                                     19.99€\r\n" +
						"                              Taux TVA  Montant H.T. T.V.A\n" +
						"                              20%             16.66€         3.33€\r\n" +
						"                          Merci de votre visite et. à bientôt.\r\n" +
						"   Conservez votre ticket il vous sera demandé pour tout échange.\r\n");

				list.add(createRasterCommand(textToPrint, 13, 0, rasterType));

				if (rasterType == RasterCommand.Standard) {
					list.add(rasterDoc.EndDocumentCommandData());
					list.add(new byte[] { 0x07 }); // Kick cash drawer
				} else {
					list.add(new byte[] { 0x1b, 0x64, 0x02 }); // Cut
				}

				sendCommand(context, portName, portSettings, list);
			}
		}
	}

	/**
	 * This function shows how to print the receipt data of a thermal POS printer.
	 * 
	 * @param context
	 *     Activity for displaying messages to the user
	 * @param portName
	 *     Port name to use for communication. This should be (TCP:<IPAddress>)
	 * @param portSettings
	 *     Should be blank
	 * @param commandType
	 *     Command type to use for printing. This should be ("Line" or "Raster")
	 * @param res
	 *     The resources object containing the image data. ( e.g.) getResources())
	 * @param strPrintArea
	 *     Printable area size, This should be ("3inch (80mm)" or "4inch (112mm)")
	 */
	public static void PrintSampleReceipt_Russian(Context context, String portName, String portSettings, String commandType, Resources res, String strPrintArea, RasterCommand rasterType) {
		if (commandType == "Line") {
			if (strPrintArea.equals("2inch (58mm)")) {
				ArrayList<byte[]> list = new ArrayList<byte[]>();

				list.add(new byte[] { 0x1b, 0x1d, 0x74, 0x22 }); // Code Page #1251 (Windows Latin-1)

				list.add(new byte[] { 0x1b, 0x44, 0x02, 0x06, 0x0a, 0x10, 0x14, 0x1a, 0x22, 0x24, 0x28, 0x00 }); // Set horizontal tab

				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x01 });
			    
				list.add(createCp1251("Р  Е  Л  А  К  С\n"));
				
				list.add(createCp1251("ООО “РЕЛАКС”\n"));
				
				list.add(createCp1251("СПб., Малая Балканская, д. 38, лит. А\n"));
				
				list.add(createCp1251("тел. 307-07-12\n"));
				
				list.add(createCp1251("РЕГ №322736 ИНН : 123321\n"));
				
				list.add(createCp1251("01  Белякова И.А.  КАССА: 0020 ОТД.01\n"));
				
				list.add(createCp1251("ЧЕК НА ПРОДАЖУ  No 84373\n"));

				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x00 });

				list.add(createCp1251("--------------------------------\r\n"));
				
				list.add(createCp1251("1.  Яблоки Айдаред, кг    144.50\n"));
				
				list.add(createCp1251("2.  Соус соевый Sen So     36.40\n"));
				
				list.add(createCp1251("3.  Соус томатный Клас     19.90\n"));
				
				list.add(createCp1251("4.  Ребра свиные в.к м     78.20\n"));
				
				list.add(createCp1251("5.  Масло подсол раф д    114.00\n"));
				
				list.add(createCp1251("6.  Блокнот 10х14см сп    164.00\n"));
				
				list.add(createCp1251("7.  Морс Северная Ягод     99.90\n"));
				
				list.add(createCp1251("8.  Активия Биойогурт      43.40\n"));
				
				list.add(createCp1251("9.  Бублики Украинские     26.90\n"));
				
				list.add(createCp1251("10. Активия Биойогурт      43.40\n"));
				
				list.add(createCp1251("11. Сахар-песок 1кг        58.40\n"));
				
				list.add(createCp1251("12. Хлопья овсяные Ясн     38.40\n"));
				
				list.add(createCp1251("13. Кинза 50г              39.90\n"));
				
				list.add(createCp1251("14. Пемза “Сердечко” .Т    37.90\n"));
				
				list.add(createCp1251("15. Приправа Santa Mar     47.90\n"));
				
				list.add(createCp1251("16. Томаты слива Выбор    162.00\n"));
				
				list.add(createCp1251("17. Бонд Стрит Ред Сел     56.90\n"));
				
				list.add(createCp1251("--------------------------------\r\n"));
				
				list.add(createCp1251("--------------------------------\r\n"));
				
				list.add(createCp1251("ДИСКОНТНАЯ КАРТА No:244001248976\n"));

				list.add(createCp1251("--------------------------------\r\n"));
				
				list.add(createCp1251("ИТОГО  К  ОПЛАТЕ  = 1212.00\n"));
				
				list.add(createCp1251("НАЛИЧНЫЕ          = 1212.00\n"));
				
				list.add(createCp1251("ВАША СКИДКА : 0.41\n"));

				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x01 });

				list.add(createCp1251("ЦЕНЫ УКАЗАНЫ С УЧЕТОМ СКИДКИ\n"));
				
				list.add(createCp1251("08-02-2015 09:49  0254.0130604\n"));
				
				list.add(createCp1251("00083213 #060127\n"));
				
				list.add(createCp1251("СПАСИБО ЗА ПОКУПКУ !\n"));
				
				list.add(createCp1251("МЫ  ОТКРЫТЫ ЕЖЕДНЕВНО С 9 ДО 23\n"));
				
				list.add(createCp1251("СОХРАНЯЙТЕ, ПОЖАЛУЙСТА , ЧЕК\n"));

				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x00 });

				// 1D barcode example
				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x01 });
				list.add(new byte[] { 0x1b, 0x62, 0x06, 0x02, 0x02 });

				list.add(createCp1252(" 12ab34cd56\u001e\r\n"));

				list.add(new byte[] { 0x1b, 0x64, 0x02 }); // Cut
				list.add(new byte[] { 0x07 }); // Kick cash drawer

				sendCommand(context, portName, portSettings, list);
			} else if (strPrintArea.equals("3inch (80mm)")) {
				ArrayList<byte[]> list = new ArrayList<byte[]>();

				list.add(new byte[] { 0x1b, 0x1d, 0x74, 0x22 }); // Code Page #1251 (Windows Latin-1)

				list.add(new byte[] { 0x1b, 0x44, 0x02, 0x06, 0x0a, 0x10, 0x14, 0x1a, 0x22, 0x24, 0x28, 0x00 }); // Set horizontal tab

				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x01 });
			    
				list.add(createCp1251("Р  Е  Л  А  К  С\n"));
				
				list.add(createCp1251("ООО “РЕЛАКС”\n"));
				
				list.add(createCp1251("СПб., Малая Балканская, д. 38, лит. А\n"));
				
				list.add(createCp1251("тел. 307-07-12\n"));
				
				list.add(createCp1251("РЕГ №322736 ИНН : 123321\n"));
				
				list.add(createCp1251("01  Белякова И.А.  КАССА: 0020 ОТД.01\n"));
				
				list.add(createCp1251("ЧЕК НА ПРОДАЖУ  No 84373\n"));

				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x00 });

				list.add(createCp1251("------------------------------------------------\r\n"));
				
				list.add(createCp1251("1. \t Яблоки Айдаред, кг \t 144.50\n"));
				
				list.add(createCp1251("2. \t Соус соевый Sen So \t  36.40\n"));
				
				list.add(createCp1251("3. \t Соус томатный Клас \t  19.90\n"));
				
				list.add(createCp1251("4. \t Ребра свиные в.к м \t  78.20\n"));
				
				list.add(createCp1251("5. \t Масло подсол раф д \t 114.00\n"));
				
				list.add(createCp1251("6. \t Блокнот 10х14см сп \t 164.00\n"));
				
				list.add(createCp1251("7. \t Морс Северная Ягод \t  99.90\n"));
				
				list.add(createCp1251("8. \t Активия Биойогурт  \t  43.40\n"));
				
				list.add(createCp1251("9. \t Бублики Украинские \t  26.90\n"));
				
				list.add(createCp1251("10.\t Активия Биойогурт  \t  43.40\n"));
				
				list.add(createCp1251("11.\t Сахар-песок 1кг    \t  58.40\n"));
				
				list.add(createCp1251("12.\t Хлопья овсяные Ясн \t  38.40\n"));
				
				list.add(createCp1251("13.\t Кинза 50г          \t  39.90\n"));
				
				list.add(createCp1251("14.\t Пемза “Сердечко” .Т\t  37.90\n"));
				
				list.add(createCp1251("15.\t Приправа Santa Mar \t  47.90\n"));
				
				list.add(createCp1251("16.\t Томаты слива Выбор \t 162.00\n"));
				
				list.add(createCp1251("17.\t Бонд Стрит Ред Сел \t  56.90\n"));
				
				list.add(createCp1251("------------------------------------------------\r\n"));
				
				list.add(createCp1251("------------------------------------------------\r\n"));
				
				list.add(createCp1251("ДИСКОНТНАЯ КАРТА  No: 2440012489765\n"));
				
				list.add(createCp1251("------------------------------------------------\r\n"));
				
				list.add(createCp1251("ИТОГО  К  ОПЛАТЕ \t = 1212.00\n"));
				
				list.add(createCp1251("НАЛИЧНЫЕ         \t = 1212.00\n"));
				
				list.add(createCp1251("ВАША СКИДКА : 0.41\n"));

				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x01 });

				list.add(createCp1251("ЦЕНЫ УКАЗАНЫ С УЧЕТОМ СКИДКИ\n"));
				
				list.add(createCp1251("08-02-2015 09:49  0254.0130604\n"));
				
				list.add(createCp1251("00083213 #060127\n"));
				
				list.add(createCp1251("СПАСИБО ЗА ПОКУПКУ !\n"));
				
				list.add(createCp1251("МЫ  ОТКРЫТЫ ЕЖЕДНЕВНО С 9 ДО 23\n"));
				
				list.add(createCp1251("СОХРАНЯЙТЕ, ПОЖАЛУЙСТА , ЧЕК\n"));

				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x00 });

				// 1D barcode example
				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x01 });
				list.add(new byte[] { 0x1b, 0x62, 0x06, 0x02, 0x02 });

				list.add(createCp1252(" 12ab34cd56\u001e\r\n"));

				list.add(new byte[] { 0x1b, 0x64, 0x02 }); // Cut
				list.add(new byte[] { 0x07 }); // Kick cash drawer

				sendCommand(context, portName, portSettings, list);
			} else if (strPrintArea.equals("4inch (112mm)")) {
				ArrayList<byte[]> list = new ArrayList<byte[]>();

				list.add(new byte[] { 0x1b, 0x1d, 0x74, 0x22 }); // Code Page #1251 (Windows Latin-1)

				list.add(new byte[] { 0x1b, 0x44, 0x06, 0x0a, 0x10, 0x14, 0x1a, 0x22, 0x24, 0x28, 0x30, 0x3a, 0x40, 0x00 }); // Set horizontal tab

				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x01 });
			    
				list.add(createCp1251("Р  Е  Л  А  К  С\n"));
				
				list.add(createCp1251("ООО “РЕЛАКС”\n"));
				
				list.add(createCp1251("СПб., Малая Балканская, д. 38, лит. А\n"));
				
				list.add(createCp1251("тел. 307-07-12\n"));
				
				list.add(createCp1251("РЕГ №322736 ИНН : 123321\n"));
				
				list.add(createCp1251("01  Белякова И.А.  КАССА: 0020 ОТД.01\n"));
				
				list.add(createCp1251("ЧЕК НА ПРОДАЖУ  No 84373\n"));

				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x00 });

				list.add(createCp1251("---------------------------------------------------------------------\r\n"));
				
				list.add(createCp1251("1. \t Яблоки Айдаред, кг \t\t\t\t\t     144.50\n"));
				
				list.add(createCp1251("2. \t Соус соевый Sen So \t\t\t\t\t      36.40\n"));
				
				list.add(createCp1251("3. \t Соус томатный Клас \t\t\t\t\t      19.90\n"));
				
				list.add(createCp1251("4. \t Ребра свиные в.к м \t\t\t\t\t      78.20\n"));
				
				list.add(createCp1251("5. \t Масло подсол раф д \t\t\t\t\t     114.00\n"));
				
				list.add(createCp1251("6. \t Блокнот 10х14см сп \t\t\t\t\t     164.00\n"));
				
				list.add(createCp1251("7. \t Морс Северная Ягод \t\t\t\t\t      99.90\n"));
				
				list.add(createCp1251("8. \t Активия Биойогурт  \t\t\t\t\t      43.40\n"));
				
				list.add(createCp1251("9. \t Бублики Украинские \t\t\t\t\t      26.90\n"));
				
				list.add(createCp1251("10.\t Активия Биойогурт  \t\t\t\t\t      43.40\n"));
				
				list.add(createCp1251("11.\t Сахар-песок 1кг    \t\t\t\t\t      58.40\n"));
				
				list.add(createCp1251("12.\t Хлопья овсяные Ясн \t\t\t\t\t      38.40\n"));
				
				list.add(createCp1251("13.\t Кинза 50г          \t\t\t\t\t      39.90\n"));
				
				list.add(createCp1251("14.\t Пемза “Сердечко” .Т\t\t\t\t\t      37.90\n"));
				
				list.add(createCp1251("15.\t Приправа Santa Mar \t\t\t\t\t      47.90\n"));
				
				list.add(createCp1251("16.\t Томаты слива Выбор \t\t\t\t\t     162.00\n"));
				
				list.add(createCp1251("17.\t Бонд Стрит Ред Сел \t\t\t\t\t      56.90\n"));
				
				list.add(createCp1251("---------------------------------------------------------------------\r\n"));
				
				list.add(createCp1251("---------------------------------------------------------------------\r\n"));
				
				list.add(createCp1251("ДИСКОНТНАЯ КАРТА  No: 2440012489765\n"));
				
				list.add(createCp1251("---------------------------------------------------------------------\r\n"));
				
				list.add(createCp1251("ИТОГО  К  ОПЛАТЕ \t\t = 1212.00\n"));
				
				list.add(createCp1251("НАЛИЧНЫЕ         \t\t = 1212.00\n"));
				
				list.add(createCp1251("ВАША СКИДКА : 0.41\n"));

				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x01 });

				list.add(createCp1251("ЦЕНЫ УКАЗАНЫ С УЧЕТОМ СКИДКИ\n"));
				
				list.add(createCp1251("08-02-2015 09:49  0254.0130604\n"));
				
				list.add(createCp1251("00083213 #060127\n"));
				
				list.add(createCp1251("СПАСИБО ЗА ПОКУПКУ !\n"));
				
				list.add(createCp1251("МЫ  ОТКРЫТЫ ЕЖЕДНЕВНО С 9 ДО 23\n"));
				
				list.add(createCp1251("СОХРАНЯЙТЕ, ПОЖАЛУЙСТА , ЧЕК\n"));

				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x00 });

				// 1D barcode example
				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x01 });
				list.add(new byte[] { 0x1b, 0x62, 0x06, 0x02, 0x02 });

				list.add(createCp1252(" 12ab34cd56\u001e\r\n"));

				list.add(new byte[] { 0x1b, 0x64, 0x02 }); // Cut
				list.add(new byte[] { 0x07 }); // Kick cash drawer

				sendCommand(context, portName, portSettings, list);
			}
		} else if (commandType == "Raster") {
			if (strPrintArea.equals("2inch (58mm)")) {
				ArrayList<byte[]> list = new ArrayList<byte[]>();

				printableArea = 384; // Printable area in paper is 384(dot)

				RasterDocument rasterDoc = new RasterDocument(RasSpeed.Medium, RasPageEndMode.FeedAndFullCut, RasPageEndMode.FeedAndFullCut, RasTopMargin.Standard, 0, 0, 0);
				
				if (rasterType == RasterCommand.Standard) {
					list.add(rasterDoc.BeginDocumentCommandData());
				}

				String textToPrint = (
						"  Р Е Л А К С  ООО “РЕЛАКС”\r\n" +
						"    СПб., Малая Балканская,\r\n" +
						"                д. 38, лит. А\r\n\r\n" +
						"тел. 307-07-12\r\n" +
						"РЕГ №322736     ИНН:123321\r\n" +
						"01 Белякова И.А.\r\n" +
						"КАССА: 0020 ОТД.01\r\n" +
						"ЧЕК НА ПРОДАЖУ  No 84373\r\n");
				list.add(createRasterCommand(textToPrint, 13, 0, rasterType));

				textToPrint = (
						"---------------------------------------------\n" +
						"Яблоки Айдаред, кг    144.50\r\n" +
						"Соус соевый Sen So       36.40\r\n" +
						"Соус томатный Клас    19.90\r\n" +
						"Ребра свиные в.к м      78.20\r\n" +
						"Масло подсол раф д   114.00\r\n" +
						"Блокнот 10х14см сп   164.00\r\n" +
						"Морс Северная Ягод     99.90\r\n" +
						"Активия Биойогурт     43.40\r\n" +
						"Бублики Украинские  26.90\r\n" +
						"Активия Биойогурт     43.40\r\n" +
						"Сахар-песок 1кг             58.40\r\n" +
						"Хлопья овсяные Ясн    38.40\r\n" +
						"Кинза 50г                        39.90\r\n" +
						"Пемза “Сердечко” .Т     37.90\r\n" +
						"Приправа Santa Mar    47.90\r\n" +
						"Томаты слива Выбор 162.00\r\n" +
						"Бонд Стрит Ред Сел      56.90\r\n" +
						"---------------------------------------------\n" +
						"---------------------------------------------\n" +
						"ДИСКОНТНАЯ КАРТА\n" +
						"                No:2440012489765\n" +
						"---------------------------------------------\n" +
						"ИТОГО К ОПЛАТЕ = 1212.00\n" +
						"НАЛИЧНЫЕ = 1212.00\n" +
						"ВАША СКИДКА : 0.41\r\n" +
						"ЦЕНЫ УКАЗАНЫ С УЧЕТОМ СКИДКИ\n\n" +
						"08-02-2015 09:49  0254.0130604\n" +
						"00083213 #060127\n" +
						"СПАСИБО ЗА ПОКУПКУ !\n\n" +
						"МЫ  ОТКРЫТЫ ЕЖЕДНЕВНО С 9 ДО 23\r\n" +
						"СОХРАНЯЙТЕ, ПОЖАЛУЙСТА , ЧЕК\n");

				list.add(createRasterCommand(textToPrint, 13, 0, rasterType));

				if (rasterType == RasterCommand.Standard) {
					list.add(rasterDoc.EndDocumentCommandData());
					list.add(new byte[] { 0x07 }); // Kick cash drawer
				} else {
					list.add(new byte[] { 0x1b, 0x64, 0x02 }); // Cut
				}

				sendCommand(context, portName, portSettings, list);
			} else if (strPrintArea.equals("3inch (80mm)")) {
				ArrayList<byte[]> list = new ArrayList<byte[]>();

				printableArea = 576; // Printable area in paper is 576(dot)

				RasterDocument rasterDoc = new RasterDocument(RasSpeed.Medium, RasPageEndMode.FeedAndFullCut, RasPageEndMode.FeedAndFullCut, RasTopMargin.Standard, 0, 0, 0);
				
				if (rasterType == RasterCommand.Standard) {
					list.add(rasterDoc.BeginDocumentCommandData());
				}

				String textToPrint = (
						"                    Р Е Л А К С   ООО “РЕЛАКС”\r\n" +
						"      СПб., Малая Балканская, д. 38, лит. А\r\n\r\n" +
						"тел. 307-07-12\r\n" +
						"РЕГ №322736     ИНН:123321\r\n" +
						"01 Белякова И.А. КАССА: 0020 ОТД.01\r\n" +
						"ЧЕК НА ПРОДАЖУ  No 84373\r\n");
				list.add(createRasterCommand(textToPrint, 13, 0, rasterType));

				textToPrint = (
						"---------------------------------------------------------------------\n" +
						" 1. Яблоки Айдаред, кг                            144.50\r\n" +
						" 2. Соус соевый Sen So                               36.40\r\n" +
						" 3. Соус томатный Клас                            19.90\r\n" +
						" 4. Ребра свиные в.к м                              78.20\r\n" +
						" 5. Масло подсол раф д                           114.00\r\n" +
						" 6. Блокнот 10х14см сп                           164.00\r\n" +
						" 7. Морс Северная Ягод                             99.90\r\n" +
						" 8. Активия Биойогурт                              43.40\r\n" +
						" 9. Бублики Украинские                           26.90\r\n" +
						"10. Активия Биойогурт                             43.40\r\n" +
						"11. Сахар-песок 1кг                                    58.40\r\n" +
						"12. Хлопья овсяные Ясн                           38.40\r\n" +
						"13. Кинза 50г                                                39.90\r\n" +
						"14. Пемза “Сердечко” .Т                            37.90\r\n" +
						"15. Приправа Santa Mar                           47.90\r\n" +
						"16. Томаты слива Выбор                        162.00\r\n" +
						"17. Бонд Стрит Ред Сел                             56.90\r\n" +
						"---------------------------------------------------------------------\n" +
						"---------------------------------------------------------------------\n" +
						"ДИСКОНТНАЯ КАРТА       No:2440012489765\n" +
						"---------------------------------------------------------------------\n" +
						"ИТОГО К ОПЛАТЕ = 1212.00\n" +
						"НАЛИЧНЫЕ = 1212.00\n" +
						"ВАША СКИДКА : 0.41\r\n" +
						"ЦЕНЫ УКАЗАНЫ С УЧЕТОМ СКИДКИ\n\n" +
						"08-02-2015 09:49  0254.0130604\n" +
						"00083213 #060127\n" +
						"               СПАСИБО ЗА ПОКУПКУ !\n\n" +
						"    МЫ  ОТКРЫТЫ ЕЖЕДНЕВНО С 9 ДО 23\r\n" +
						"        СОХРАНЯЙТЕ, ПОЖАЛУЙСТА , ЧЕК\n");

				list.add(createRasterCommand(textToPrint, 13, 0, rasterType));

				if (rasterType == RasterCommand.Standard) {
					list.add(rasterDoc.EndDocumentCommandData());
					list.add(new byte[] { 0x07 }); // Kick cash drawer
				} else {
					list.add(new byte[] { 0x1b, 0x64, 0x02 }); // Cut
				}

				sendCommand(context, portName, portSettings, list);
			} else if (strPrintArea.equals("4inch (112mm)")) {
				ArrayList<byte[]> list = new ArrayList<byte[]>();

				printableArea = 832; // Printable area in paper is 832(dot)

				RasterDocument rasterDoc = new RasterDocument(RasSpeed.Medium, RasPageEndMode.FeedAndFullCut, RasPageEndMode.FeedAndFullCut, RasTopMargin.Standard, 0, 0, 0);
				
				if (rasterType == RasterCommand.Standard) {
					list.add(rasterDoc.BeginDocumentCommandData());
				}

				String textToPrint = (
						"                                  Р Е Л А К С   ООО “РЕЛАКС”\r\n" +
						"                    СПб., Малая Балканская, д. 38, лит. А\r\n\r\n" +
						"тел. 307-07-12\r\n" +
						"РЕГ №322736     ИНН:123321\r\n" +
						"01 Белякова И.А. КАССА: 0020 ОТД.01\r\n" +
						"ЧЕК НА ПРОДАЖУ  No 84373\r\n");
				list.add(createRasterCommand(textToPrint, 13, 0, rasterType));

				textToPrint = (
						"-------------------------------------------------------------------------------------------------------\n" +
						" 1.      Яблоки Айдаред, кг                                                         144.50\r\n" +
						" 2.      Соус соевый Sen So                                                            36.40\r\n" +
						" 3.      Соус томатный Клас                                                         19.90\r\n" +
						" 4.      Ребра свиные в.к м                                                           78.20\r\n" +
						" 5.      Масло подсол раф д                                                        114.00\r\n" +
						" 6.      Блокнот 10х14см сп                                                        164.00\r\n" +
						" 7.      Морс Северная Ягод                                                          99.90\r\n" +
						" 8.      Активия Биойогурт                                                           43.40\r\n" +
						" 9.      Бублики Украинские                                                        26.90\r\n" +
						"10.      Активия Биойогурт                                                          43.40\r\n" +
						"11.      Сахар-песок 1кг                                                                 58.40\r\n" +
						"12.      Хлопья овсяные Ясн                                                        38.40\r\n" +
						"13.      Кинза 50г                                                                             39.90\r\n" +
						"14.      Пемза “Сердечко” .Т                                                         37.90\r\n" +
						"15.      Приправа Santa Mar                                                        47.90\r\n" +
						"16.      Томаты слива Выбор                                                     162.00\r\n" +
						"17.      Бонд Стрит Ред Сел                                                          56.90\r\n" +
						"-------------------------------------------------------------------------------------------------------\n" +
						"-------------------------------------------------------------------------------------------------------\n" +
						"ДИСКОНТНАЯ КАРТА                                No:2440012489765\n" +
						"-------------------------------------------------------------------------------------------------------\n" +
						"ИТОГО К ОПЛАТЕ = 1212.00\n" +
						"НАЛИЧНЫЕ = 1212.00\n" +
						"ВАША СКИДКА : 0.41\r\n" +
						"ЦЕНЫ УКАЗАНЫ С УЧЕТОМ СКИДКИ\n\n" +
						"08-02-2015 09:49  0254.0130604\n" +
						"00083213 #060127\n" +
						"                                       СПАСИБО ЗА ПОКУПКУ !\n\n" +
						"                       МЫ  ОТКРЫТЫ ЕЖЕДНЕВНО С 9 ДО 23\r\n" +
						"                           СОХРАНЯЙТЕ, ПОЖАЛУЙСТА , ЧЕК\n");

				list.add(createRasterCommand(textToPrint, 13, 0, rasterType));

				if (rasterType == RasterCommand.Standard) {
					list.add(rasterDoc.EndDocumentCommandData());
					list.add(new byte[] { 0x07 }); // Kick cash drawer
				} else {
					list.add(new byte[] { 0x1b, 0x64, 0x02 }); // Cut
				}

				sendCommand(context, portName, portSettings, list);
			}
		}
	}

	/**
	 * This function shows how to print the receipt data of a thermal POS printer.
	 * 
	 * @param context
	 *     Activity for displaying messages to the user
	 * @param portName
	 *     Port name to use for communication. This should be (TCP:<IPAddress>)
	 * @param portSettings
	 *     Should be blank
	 * @param commandType
	 *     Command type to use for printing. This should be ("Line" or "Raster")
	 * @param res
	 *     The resources object containing the image data. ( e.g.) getResources())
	 * @param strPrintArea
	 *     Printable area size, This should be ("3inch (80mm)" or "4inch (112mm)")
	 */
	public static void PrintSampleReceipt_Portuguese(Context context, String portName, String portSettings, String commandType, Resources res, String strPrintArea, RasterCommand rasterType) {
		if (commandType == "Line") {
			if (strPrintArea.equals("2inch (58mm)")) {
				ArrayList<byte[]> list = new ArrayList<byte[]>();

				list.add(new byte[] { 0x1b, 0x1d, 0x74, 0x20 }); // Code Page #1252 (Windows Latin-1)

				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x01 }); // Alignment (center)

				// list.add("[If loaded.. Logo1 goes here]\r\n".getBytes());

				// list.add(new byte[]{0x1b, 0x1c, 0x70, 0x01, 0x00, '\r', '\n'}); //Stored Logo Printing

				// Notice that we use a unicode representation because that is
				// how Java expresses these bytes as double byte unicode

				// Character expansion
				list.add(new byte[] { 0x06, 0x09, 0x1b, 0x69, 0x01, 0x00 });

				list.add(createCp1252("\nCOMERCIAL DE ALIMENTOS\r\n"));
				list.add(createCp1252("CARREFOUR LTDA.\r\n"));
				
				list.add(new byte[] { 0x1b, 0x69, 0x00, 0x00 }); // Cancel Character Expansion

				list.add(createCp1252("Avenida Moyses Roysen,\r\n"));
				
				list.add(createCp1252("S/N Vila Guilherme\r\n"));
				
				list.add(createCp1252("Cep: 02049-010 – Sao Paulo – SP\r\n"));

				list.add(createCp1252("CNPJ: 62.545.579/0013-69\r\n"));
				list.add(createCp1252("IE:110.819.138.118\r\n"));
				list.add(createCp1252("IM: 9.041.041-5\r\n"));

				
				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x00 }); // Alignment

				list.add(new byte[] { 0x1b, 0x44, 0x02, 0x10, 0x22, 0x00 }); // Set horizontal tab

				list.add(createCp1252("--------------------------------\r\n"));
				
				list.add(createCp1252("MM/DD/YYYY HH:MM:SS\r\n"));
				list.add(createCp1252("CCF:133939 COO:227808\r\n"));

				list.add(createCp1252("--------------------------------\r\n"));

				list.add(createCp1252("CUPOM FISCAL\r\n"));
				
				list.add(createCp1252("--------------------------------\r\n"));

				list.add(createCp1252("001 2505 CAFÉ DO PONTO TRAD A\r\n"));
				list.add(createCp1252("                    1un F1 8,15)\r\n"));

				list.add(createCp1252("002 2505 CAFÉ DO PONTO TRAD A\r\n"));
				list.add(createCp1252("                    1un F1 8,15)\r\n"));

				list.add(createCp1252("003 2505 CAFÉ DO PONTO TRAD A\r\n"));
				list.add(createCp1252("                    1un F1 8,15)\r\n"));

				list.add(createCp1252("004 6129 AGU MIN NESTLE 510ML\r\n"));
				list.add(createCp1252("                    1un F1 1,39)\r\n"));

				list.add(createCp1252("005 6129 AGU MIN NESTLE 510ML\r\n"));
				list.add(createCp1252("                    1un F1 1,39)\r\n"));

				list.add(createCp1252("--------------------------------\r\n"));

				// Character expansion
				list.add(new byte[] { 0x1b, 0x69, 0x00, 0x01 });

				list.add(createCp1252("TOTAL  R$  27,23\r\n"));

				list.add(new byte[] { 0x1b, 0x69, 0x00, 0x00 });

				list.add(createCp1252("DINHEIROv                  29,00\r\n"));

				list.add(createCp1252("TROCO R$                    1,77\r\n"));

				list.add(createCp1252("Valor dos Tributos R$2,15(7,90%)\r\n"));
				
				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x01 }); // Alignment (center)

				list.add(createCp1252("ITEM(S) CINORADIS 5\r\n"));

				list.add(createCp1252("OP.:15326  PDV:9  BR,BF:93466\r\n"));

				list.add(createCp1252("OBRIGADO PERA PREFERENCIA.\r\n"));

				list.add(new byte[] { 0x06, 0x09, 0x1b, 0x69, 0x00, 0x01 });

				list.add(createCp1252("VOLTE SEMPRE!\r\n"));

				list.add(new byte[] { 0x06, 0x09, 0x1b, 0x69, 0x00, 0x00 });

				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x00 });
				
				list.add(createCp1252("SAC 0800 724 2822\r\n"));

				list.add(createCp1252("--------------------------------\r\n"));

				list.add(createCp1252("MD5:\r\n"));
				
				list.add(createCp1252("fe028828a532a7dbaf4271155aa4e2db\r\n"));
				
				list.add(createCp1252("Calypso_CA CA.20.c13\r\n"));
				
				list.add(createCp1252(" – Unisys Brasil\r\n"));
				
				list.add(createCp1252("--------------------------------\r\n"));

				list.add(createCp1252("DARUMA AUTOMAÇÃO   MACH 2\r\n"));

				list.add(createCp1252("ECF-IF VERSÃO:01,00,00 ECF:093\r\n"));
				
				list.add(createCp1252("Lj:0204 OPR:ANGELA JORGE\r\n"));

				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x01 }); // Alignment (center)

				list.add(createCp1252("DDDDDDDDDAEHFGBFCC\r\n"));
				
				list.add(createCp1252("MM/DD/YYYY HH:MM:SS\r\n"));
				
				list.add(createCp1252("FAB:DR0911BR000000275026\r\n"));

				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x00 });

				// 1D barcode example
				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x01 });
				list.add(new byte[] { 0x1b, 0x62, 0x06, 0x02, 0x02 });

				list.add(createCp1252(" 12ab34cd56\u001e\r\n"));

				list.add(new byte[] { 0x1b, 0x64, 0x02 }); // Cut
				list.add(new byte[] { 0x07 }); // Kick cash drawer

				sendCommand(context, portName, portSettings, list);
			} else if (strPrintArea.equals("3inch (80mm)")) {
				ArrayList<byte[]> list = new ArrayList<byte[]>();

				list.add(new byte[] { 0x1b, 0x1d, 0x74, 0x20 }); // Code Page #1252 (Windows Latin-1)

				list.add(new byte[] { 0x1b, 0x44, 0x02, 0x06, 0x0a, 0x10, 0x14, 0x1a, 0x22, 0x24, 0x28, 0x00 }); // Set horizontal tab

				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x01 }); // Alignment (center)

				// list.add("[If loaded.. Logo1 goes here]\r\n".getBytes());

				// list.add(new byte[]{0x1b, 0x1c, 0x70, 0x01, 0x00, '\r', '\n'}); //Stored Logo Printing

				// Notice that we use a unicode representation because that is
				// how Java expresses these bytes as double byte unicode

				// Character expansion
				list.add(new byte[] { 0x1b, 0x69, 0x01, 0x00 });

				list.add(createCp1252("\nCOMERCIAL DE ALIMENTOS CARREFOUR LTDA.\r\n"));
				
				list.add(new byte[] { 0x1b, 0x69, 0x00, 0x00 }); // Cancel Character Expansion

				list.add(createCp1252("Avenida Moyses Roysen, S/N Vila Guilherme\r\n"));
				
				list.add(createCp1252("Cep: 02049-010 – Sao Paulo – SP\r\n"));

				list.add(createCp1252("CNPJ: 62.545.579/0013-69\r\n"));
				list.add(createCp1252("IE:110.819.138.118  IM: 9.041.041-5\r\n"));
				
				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x00 }); // Alignment

				list.add(new byte[] { 0x1b, 0x44, 0x02, 0x10, 0x22, 0x00 }); // Set horizontal tab

				list.add(createCp1252("------------------------------------------------\r\n"));
				
				list.add(createCp1252("MM/DD/YYYY HH:MM:SS  CCF:133939 COO:227808\r\n"));

				list.add(createCp1252("------------------------------------------------\r\n"));

				list.add(createCp1252("CUPOM FISCAL\r\n"));
				
				list.add(createCp1252("------------------------------------------------\r\n"));

				list.add(createCp1252("001  2505  CAFÉ DO PONTO TRAD A  1un F1  8,15)\r\n"));

				list.add(createCp1252("002  2505  CAFÉ DO PONTO TRAD A  1un F1  8,15)\r\n"));

				list.add(createCp1252("003  2505  CAFÉ DO PONTO TRAD A  1un F1  8,15)\r\n"));

				list.add(createCp1252("004  6129  AGU MIN NESTLE 510ML  1un F1  1,39)\r\n"));

				list.add(createCp1252("005  6129  AGU MIN NESTLE 510ML  1un F1  1,39)\r\n"));

				list.add(createCp1252("------------------------------------------------\r\n"));

				// Character expansion
				list.add(new byte[] { 0x1b, 0x69, 0x00, 0x01 });

				list.add(createCp1252("TOTAL  R$ \t\t\t 27,23\r\n"));

				list.add(new byte[] { 0x1b, 0x69, 0x00, 0x00 });

				list.add(createCp1252("DINHEIROv \t\t\t\t\t\t       29,00\r\n"));

				list.add(createCp1252("TROCO R$  \t\t\t\t\t\t        1,77\r\n"));

				list.add(createCp1252("Valor dos Tributos R$2,15 (7,90%)\r\n"));
				
				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x01 }); // Alignment (center)

				list.add(createCp1252("ITEM(S) CINORADIS 5\r\n"));

				list.add(createCp1252("OP.:15326  PDV:9  BR,BF:93466\r\n"));

				list.add(createCp1252("OBRIGADO PERA PREFERENCIA.\r\n"));

				list.add(new byte[] { 0x1b, 0x69, 0x00, 0x01 });

				list.add(createCp1252("VOLTE SEMPRE!\r\n"));

				list.add(new byte[] { 0x1b, 0x69, 0x00, 0x00 });

				list.add(createCp1252("SAC 0800 724 2822\r\n"));

				list.add(createCp1252("------------------------------------------------\r\n"));

				list.add(createCp1252("MD5:fe028828a532a7dbaf4271155aa4e2db\r\n"));
				
				list.add(createCp1252("Calypso_CA CA.20.c13 – Unisys Brasil\r\n"));
				
				list.add(createCp1252("------------------------------------------------\r\n"));

				list.add(createCp1252("DARUMA AUTOMAÇÃO   MACH 2\r\n"));

				list.add(createCp1252("ECF-IF VERSÃO:01,00,00 ECF:093\r\n"));
				
				list.add(createCp1252("Lj:0204 OPR:ANGELA JORGE\r\n"));

				list.add(createCp1252("DDDDDDDDDAEHFGBFCC\r\n"));
				
				list.add(createCp1252("MM/DD/YYYY HH:MM:SS\r\n"));
				
				list.add(createCp1252("FAB:DR0911BR000000275026\r\n"));

				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x00 });

				// 1D barcode example
				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x01 });
				list.add(new byte[] { 0x1b, 0x62, 0x06, 0x02, 0x02 });

				list.add(createCp1252(" 12ab34cd56\u001e\r\n"));

				list.add(new byte[] { 0x1b, 0x64, 0x02 }); // Cut
				list.add(new byte[] { 0x07 }); // Kick cash drawer

				sendCommand(context, portName, portSettings, list);
			} else if (strPrintArea.equals("4inch (112mm)")) {
				ArrayList<byte[]> list = new ArrayList<byte[]>();

				list.add(new byte[] { 0x1b, 0x1d, 0x74, 0x20 }); // Code Page #1252 (Windows Latin-1)

				list.add(new byte[] { 0x1b, 0x44, 0x06, 0x0a, 0x10, 0x14, 0x1a, 0x22, 0x24, 0x28, 0x30, 0x3a, 0x40, 0x00 }); // Set horizontal tab

				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x01 }); // Alignment (center)

				// list.add("[If loaded.. Logo1 goes here]\r\n".getBytes());

				// list.add(new byte[]{0x1b, 0x1c, 0x70, 0x01, 0x00, '\r', '\n'}); //Stored Logo Printing

				// Notice that we use a unicode representation because that is
				// how Java expresses these bytes as double byte unicode

				// Character expansion
				list.add(new byte[] { 0x06, 0x09, 0x1b, 0x69, 0x01, 0x00 });

				list.add(createCp1252("\nCOMERCIAL DE ALIMENTOS CARREFOUR LTDA.\r\n"));
				
				list.add(new byte[] { 0x1b, 0x69, 0x00, 0x00 }); // Cancel Character Expansion

				list.add(createCp1252("Avenida Moyses Roysen, S/N Vila Guilherme\r\n"));
				
				list.add(createCp1252("Cep: 02049-010 – Sao Paulo – SP\r\n"));

				list.add(createCp1252("CNPJ: 62.545.579/0013-69\r\n"));
				list.add(createCp1252("IE:110.819.138.118  IM: 9.041.041-5\r\n"));
				

				list.add(createCp1252("---------------------------------------------------------------------\r\n"));
				
				list.add(createCp1252("MM/DD/YYYY HH:MM:SS  CCF:133939 COO:227808\r\n"));

				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x00 }); // Alignment

				list.add(createCp1252("---------------------------------------------------------------------\r\n"));

				list.add(createCp1252("CUPOM FISCAL\r\n"));
				
				list.add(createCp1252("---------------------------------------------------------------------\r\n"));

				list.add(createCp1252("001  2505 \t CAFÉ DO PONTO TRAD A \t 1un F1 \t 8,15)\r\n"));

				list.add(createCp1252("002  2505 \t CAFÉ DO PONTO TRAD A \t 1un F1 \t 8,15)\r\n"));

				list.add(createCp1252("003  2505 \t CAFÉ DO PONTO TRAD A \t 1un F1 \t 8,15)\r\n"));

				list.add(createCp1252("004  6129 \t AGU MIN NESTLE 510ML \t 1un F1 \t 1,39)\r\n"));

				list.add(createCp1252("005  6129 \t AGU MIN NESTLE 510ML \t 1un F1 \t 1,39)\r\n"));

				list.add(createCp1252("---------------------------------------------------------------------\r\n"));

				// Character expansion
				list.add(new byte[] { 0x1b, 0x69, 0x00, 0x01 });

				list.add(createCp1252("TOTAL  R$ \t\t\t\t\t   27,23\r\n"));

				list.add(new byte[] { 0x1b, 0x69, 0x00, 0x00 });

				list.add(createCp1252("DINHEIROv \t\t\t\t\t\t\t           29,00\r\n"));

				list.add(createCp1252("TROCO R$  \t\t\t\t\t\t\t            1,77\r\n"));

				list.add(createCp1252("Valor dos Tributos R$2,15 (7,90%)\r\n"));
				
				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x01 }); // Alignment (center)

				list.add(createCp1252("ITEM(S) CINORADIS 5\r\n"));

				list.add(createCp1252("OP.:15326  PDV:9  BR,BF:93466\r\n"));

				list.add(createCp1252("OBRIGADO PERA PREFERENCIA.\r\n"));

				list.add(new byte[] { 0x06, 0x09, 0x1b, 0x69, 0x00, 0x01 });

				list.add(createCp1252("VOLTE SEMPRE!\r\n"));

				list.add(new byte[] { 0x06, 0x09, 0x1b, 0x69, 0x00, 0x00 });

				list.add(createCp1252("SAC 0800 724 2822\r\n"));

				list.add(createCp1252("---------------------------------------------------------------------\r\n"));

				list.add(createCp1252("MD5:fe028828a532a7dbaf4271155aa4e2db\r\n"));
				
				list.add(createCp1252("Calypso_CA CA.20.c13 – Unisys Brasil\r\n"));
				
				list.add(createCp1252("---------------------------------------------------------------------\r\n"));

				list.add(createCp1252("DARUMA AUTOMAÇÃO   MACH 2\r\n"));

				list.add(createCp1252("ECF-IF VERSÃO:01,00,00 ECF:093\r\n"));
				
				list.add(createCp1252("Lj:0204 OPR:ANGELA JORGE\r\n"));

				list.add(createCp1252("DDDDDDDDDAEHFGBFCC\r\n"));
				
				list.add(createCp1252("MM/DD/YYYY HH:MM:SS\r\n"));
				
				list.add(createCp1252("FAB:DR0911BR000000275026\r\n"));

				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x00 });

				// 1D barcode example
				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x01 });
				list.add(new byte[] { 0x1b, 0x62, 0x06, 0x02, 0x02 });

				list.add(createCp1252(" 12ab34cd56\u001e\r\n"));

				list.add(new byte[] { 0x1b, 0x64, 0x02 }); // Cut
				list.add(new byte[] { 0x07 }); // Kick cash drawer

				sendCommand(context, portName, portSettings, list);
			}
		} else if (commandType == "Raster") {
			if (strPrintArea.equals("2inch (58mm)")) {
				ArrayList<byte[]> list = new ArrayList<byte[]>();

				printableArea = 384; // Printable area in paper is 384(dot)

				RasterDocument rasterDoc = new RasterDocument(RasSpeed.Medium, RasPageEndMode.FeedAndFullCut, RasPageEndMode.FeedAndFullCut, RasTopMargin.Standard, 0, 0, 0);
				
				if (rasterType == RasterCommand.Standard) {
					list.add(rasterDoc.BeginDocumentCommandData());
				}

				String textToPrint = (
						"COMERCIAL DE ALIMENTOS\r\n" +
						"       CARREFOUR LTDA.\r\n");
				list.add(createRasterCommand(textToPrint, 13, 0, rasterType));

				textToPrint = (
						"Avenida Moyses Roysen,\r\n" +
						"S/N Vila Guilherme\r\n" +
						"Cep: 02049-010–SaoPaulo–SP\n" +
						"CNPJ: 62.545.579/0013-69\n" +
						"IE:110.819.138.118\n" +
						"IM: 9.041.041-5\n" +
						"---------------------------------------------\r\n" +
						"MM/DD/YYYY HH:MM:SS\n" +
						"CCF:133939 COO:227808\n" +
						"---------------------------------------------\r\n" +
						"CUPOM FISCAL\n" +
						"---------------------------------------------\r\n" +
						"01 CAFÉ DO PONTO TRAD A\n" +
						"                              1un F1 8,15)\n" +
						"02 CAFÉ DO PONTO TRAD A\n" +
						"                              1un F1 8,15)\n" +
						"03 CAFÉ DO PONTO TRAD A\n" +
						"                              1un F1 8,15)\n" +
						"04 AGU MIN NESTLE 510ML\n" +
						"                              1un F1 1,39)\n" +
						"05 AGU MIN NESTLE 510ML\n" +
						"                              1un F1 1,39)\n" +
						"---------------------------------------------\r\n" +
						"TOTAL  R$                        27,23\n" +
						"DINHEIROv                     29,00\n\n" +
						"TROCO R$                          1,77\r\n" +
						"Valor dos Tributos R$2,15(7,90%)\n" +
						"ITEM(S) CINORADIS 5\r\n" +
						"OP.:15326  PDV:9  BR,BF:93466\n" +
						"OBRIGADO PERA PREFERENCIA.\r\n" +
						"VOLTE SEMPRE!\n" +
						"SAC 0800 724 2822\n" +
						"---------------------------------------------\r\n" +
						"MD5:\r\n" +
						"fe028828a532a7dbaf4271155aa4e2db\r\n" +
						"Calypso_CA CA.20.c13\r\n" +
						" – Unisys Brasil\r\n" +
						"---------------------------------------------\r\n" +
						"DARUMA AUTOMAÇÃO   MACH 2\r\n" +
						"ECF-IF VERSÃO:01,00,00 ECF:093\r\n" +
						"Lj:0204 OPR:ANGELA JORGE\r\n" +
						"DDDDDDDDDAEHFGBFCC\r\n" +
						"MM/DD/YYYY HH:MM:SS\r\n" +
						"FAB:DR0911BR000000275026\r\n\r\n");

				list.add(createRasterCommand(textToPrint, 13, 0, rasterType));

				if (rasterType == RasterCommand.Standard) {
					list.add(rasterDoc.EndDocumentCommandData());
					list.add(new byte[] { 0x07 }); // Kick cash drawer
				} else {
					list.add(new byte[] { 0x1b, 0x64, 0x02 }); // Cut
				}

				sendCommand(context, portName, portSettings, list);
			} else if (strPrintArea.equals("3inch (80mm)")) {
				ArrayList<byte[]> list = new ArrayList<byte[]>();

				printableArea = 576; // Printable area in paper is 576(dot)

				RasterDocument rasterDoc = new RasterDocument(RasSpeed.Medium, RasPageEndMode.FeedAndFullCut, RasPageEndMode.FeedAndFullCut, RasTopMargin.Standard, 0, 0, 0);
				
				if (rasterType == RasterCommand.Standard) {
					list.add(rasterDoc.BeginDocumentCommandData());
				}

				String textToPrint = (
						"                  COMERCIAL DE ALIMENTOS\r\n" +
						"                          CARREFOUR LTDA.\r\n");
				list.add(createRasterCommand(textToPrint, 13, 0, rasterType));

				textToPrint = (
						"                      Avenida Moyses Roysen,\r\n" +
						"                          S/N Vila Guilherme\r\n" +
						"                 Cep: 02049-010 – Sao Paulo – SP\n" +
						"                      CNPJ: 62.545.579/0013-69\n" +
						"             IE:110.819.138.118    IM: 9.041.041-5\n" +
						"-----------------------------------------------------------------------\r\n" +
						"MM/DD/YYYY HH:MM:SS\n" +
						"CCF:133939   COO:227808\n" +
						"-----------------------------------------------------------------------\r\n" +
						"CUPOM FISCAL\n" +
						"-----------------------------------------------------------------------\r\n" +
						"01   CAFÉ DO PONTO TRAD A    1un F1     8,15)\n" +
						"02   CAFÉ DO PONTO TRAD A    1un F1     8,15)\n" +
						"03   CAFÉ DO PONTO TRAD A    1un F1     8,15)\n" +
						"04   AGU MIN NESTLE 510ML    1un F1     1,39)\n" +
						"05   AGU MIN NESTLE 510ML    1un F1     1,39)\n" +
						"-----------------------------------------------------------------------\r\n" +
						"TOTAL  R$                                                       27,23\n" +
						"DINHEIROv                                                    29,00\n\n" +
						"TROCO R$                                                         1,77\r\n" +
						"Valor dos Tributos R$2,15(7,90%)\n" +
						"ITEM(S) CINORADIS 5\r\n" +
						"OP.:15326  PDV:9  BR,BF:93466\n" +
						"OBRIGADO PERA PREFERENCIA.\r\n" +
						"VOLTE SEMPRE!    SAC 0800 724 2822\n" +
						"-----------------------------------------------------------------------\r\n" +
						"MD5:  fe028828a532a7dbaf4271155aa4e2db\r\n" +
						"Calypso_CA CA.20.c13 – Unisys Brasil\r\n" +
						"-----------------------------------------------------------------------\r\n" +
						"DARUMA AUTOMAÇÃO   MACH 2\r\n" +
						"ECF-IF VERSÃO:01,00,00 ECF:093\r\n" +
						"Lj:0204 OPR:ANGELA JORGE\r\n" +
						"DDDDDDDDDAEHFGBFCC\r\n" +
						"MM/DD/YYYY HH:MM:SS\r\n" +
						"FAB:DR0911BR000000275026\r\n\r\n");

				list.add(createRasterCommand(textToPrint, 13, 0, rasterType));

				if (rasterType == RasterCommand.Standard) {
					list.add(rasterDoc.EndDocumentCommandData());
					list.add(new byte[] { 0x07 }); // Kick cash drawer
				} else {
					list.add(new byte[] { 0x1b, 0x64, 0x02 }); // Cut
				}

				sendCommand(context, portName, portSettings, list);
			} else if (strPrintArea.equals("4inch (112mm)")) {
				ArrayList<byte[]> list = new ArrayList<byte[]>();

				printableArea = 832; // Printable area in paper is 832(dot)

				RasterDocument rasterDoc = new RasterDocument(RasSpeed.Medium, RasPageEndMode.FeedAndFullCut, RasPageEndMode.FeedAndFullCut, RasTopMargin.Standard, 0, 0, 0);
				
				if (rasterType == RasterCommand.Standard) {
					list.add(rasterDoc.BeginDocumentCommandData());
				}

				String textToPrint = (
						"                  COMERCIAL DE ALIMENTOS CARREFOUR LTDA.\r\n");
				list.add(createRasterCommand(textToPrint, 13, 0, rasterType));

				textToPrint = (
						"                      Avenida Moyses Roysen, S/N Vila Guilherme\r\n" +
						"                                Cep: 02049-010 – Sao Paulo – SP\n" +
						"                                     CNPJ: 62.545.579/0013-69\n" +
						"                            IE:110.819.138.118    IM: 9.041.041-5\n" +
						"-------------------------------------------------------------------------------------------------------\n" +
						"                MM/DD/YYYY HH:MM:SS CCF:133939   COO:227808\n" +
						"-------------------------------------------------------------------------------------------------------\n" +
						"CUPOM FISCAL\n" +
						"-------------------------------------------------------------------------------------------------------\n" +
						"01   CAFÉ DO PONTO TRAD A    1un F1                                   8,15)\n" +
						"02   CAFÉ DO PONTO TRAD A    1un F1                                   8,15)\n" +
						"03   CAFÉ DO PONTO TRAD A    1un F1                                   8,15)\n" +
						"04   AGU MIN NESTLE 510ML    1un F1                                   1,39)\n" +
						"05   AGU MIN NESTLE 510ML    1un F1                                   1,39)\n" +
						"-------------------------------------------------------------------------------------------------------\n" +
						"TOTAL  R$                                                                                     27,23\n" +
						"DINHEIROv                                                                                  29,00\n\n" +
						"TROCO R$                                                                                       1,77\r\n" +
						"Valor dos Tributos R$2,15(7,90%)\n" +
						"ITEM(S) CINORADIS 5\r\n" +
						"OP.:15326  PDV:9  BR,BF:93466\n" +
						"OBRIGADO PERA PREFERENCIA.\r\n" +
						"                              VOLTE SEMPRE!    SAC 0800 724 2822\n" +
						"-------------------------------------------------------------------------------------------------------\n" +
						"                    MD5:  fe028828a532a7dbaf4271155aa4e2db\r\n" +
						"                              Calypso_CA CA.20.c13 – Unisys Brasil\r\n" +
						"-------------------------------------------------------------------------------------------------------\n" +
						"DARUMA AUTOMAÇÃO   MACH 2\r\n" +
						"ECF-IF VERSÃO:01,00,00 ECF:093\r\n" +
						"Lj:0204 OPR:ANGELA JORGE\r\n" +
						"DDDDDDDDDAEHFGBFCC\r\n" +
						"MM/DD/YYYY HH:MM:SS\r\n" +
						"FAB:DR0911BR000000275026\r\n\r\n");

				list.add(createRasterCommand(textToPrint, 13, 0, rasterType));

				if (rasterType == RasterCommand.Standard) {
					list.add(rasterDoc.EndDocumentCommandData());
					list.add(new byte[] { 0x07 }); // Kick cash drawer
				} else {
					list.add(new byte[] { 0x1b, 0x64, 0x02 }); // Cut
				}

				sendCommand(context, portName, portSettings, list);
			}
		}
	}

	/**
	 * This function shows how to print the receipt data of a thermal POS printer.
	 * 
	 * @param context
	 *     Activity for displaying messages to the user
	 * @param portName
	 *     Port name to use for communication. This should be (TCP:<IPAddress>)
	 * @param portSettings
	 *     Should be blank
	 * @param commandType
	 *     Command type to use for printing. This should be ("Line" or "Raster")
	 * @param res
	 *     The resources object containing the image data. ( e.g.) getResources())
	 * @param strPrintArea
	 *     Printable area size, This should be ("3inch (80mm)" or "4inch (112mm)")
	 */
	public static void PrintSampleReceipt_Spanish(Context context, String portName, String portSettings, String commandType, Resources res, String strPrintArea, RasterCommand rasterType) {
		if (commandType == "Line") {
			if (strPrintArea.equals("2inch (58mm)")) {
				ArrayList<byte[]> list = new ArrayList<byte[]>();

				list.add(new byte[] { 0x1b, 0x1d, 0x74, 0x20 }); // Code Page #1252 (Windows Latin-1)

				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x01 }); // Alignment (center)

				// list.add("[If loaded.. Logo1 goes here]\r\n".getBytes());

				// list.add(new byte[]{0x1b, 0x1c, 0x70, 0x01, 0x00, '\r', '\n'}); //Stored Logo Printing

				// Notice that we use a unicode representation because that is
				// how Java expresses these bytes as double byte unicode

				// Character expansion
				list.add(new byte[] { 0x06, 0x09, 0x1b, 0x69, 0x01, 0x01 });

				list.add(createCp1252("BAR RESTAURANT\r\n"));
				list.add(createCp1252("EL POZO\r\n"));
				
				list.add(new byte[] { 0x1b, 0x69, 0x00, 0x00 }); // Cancel Character Expansion
				
				list.add(createCp1252("C/.ROCAFORT 187\r\n"));
				
				list.add(createCp1252("08029 BARCELONA\r\n\r\n"));
				
				list.add(createCp1252("NIF :X-3856907Z\r\n"));

				list.add(createCp1252("TEL :934199465\r\n"));
				
				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x00 }); // Alignment

				list.add(new byte[] { 0x1b, 0x44, 0x02, 0x10, 0x22, 0x00 }); // Set horizontal tab

				list.add(createCp1252("--------------------------------\r\n"));
				
				list.add(createCp1252("MESA: 100 P: - FECHA: YYYY-MM-DD\r\n"));
				list.add(createCp1252("CAN P/U DESCRIPCION  SUMA\r\n"));

				list.add(createCp1252("--------------------------------\r\n"));

				list.add(createCp1252(" 4  3,00  JARRA  CERVESA   12,00\r\n"));
				list.add(createCp1252(" 1  1,60  COPA DE CERVESA   1,60\r\n"));
				
				list.add(createCp1252("--------------------------------\r\n"));

				list.add(createCp1252("               SUB TOTAL : 13,60\r\n"));
				
				list.add(new byte[] { 0x09, 0x1b, 0x69, 0x01, 0x00 });
				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x02 });
				list.add(createCp1252("TOTAL:     13,60 EUROS\r\n"));
				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x00 });
				list.add(new byte[] { 0x09, 0x1b, 0x69, 0x00, 0x00 });

				list.add(createCp1252("NO: 000018851     IVA INCLUIDO\r\n"));
				list.add(createCp1252("--------------------------------\r\n"));

				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x01 });
				list.add(createCp1252("**** GRACIAS POR SU VISITA! ****\r\n"));
				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x00 });

				// 1D barcode example
				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x01 });
				list.add(new byte[] { 0x1b, 0x62, 0x06, 0x02, 0x02 });

				list.add(createCp1252(" 12ab34cd56\u001e\r\n"));

				list.add(new byte[] { 0x1b, 0x64, 0x02 }); // Cut
				list.add(new byte[] { 0x07 }); // Kick cash drawer

				sendCommand(context, portName, portSettings, list);
			} else if (strPrintArea.equals("3inch (80mm)")) {
				ArrayList<byte[]> list = new ArrayList<byte[]>();

				list.add(new byte[] { 0x1b, 0x1d, 0x74, 0x20 }); // Code Page #1252 (Windows Latin-1)

				list.add(new byte[] { 0x1b, 0x44, 0x02, 0x06, 0x0a, 0x10, 0x14, 0x1a, 0x22, 0x24, 0x28, 0x00 }); // Set horizontal tab

				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x01 }); // Alignment (center)

				// list.add("[If loaded.. Logo1 goes here]\r\n".getBytes());

				// list.add(new byte[]{0x1b, 0x1c, 0x70, 0x01, 0x00, '\r', '\n'}); //Stored Logo Printing

				// Notice that we use a unicode representation because that is
				// how Java expresses these bytes as double byte unicode

				// Character expansion
				list.add(new byte[] { 0x1b, 0x69, 0x01, 0x01 });

				list.add(createCp1252("BAR RESTAURANT EL POZO\r\n"));
				
				list.add(new byte[] { 0x1b, 0x69, 0x00, 0x00 }); // Cancel Character Expansion
				
				list.add(createCp1252("C/.ROCAFORT 187 08029 BARCELONA\r\n"));
				
				list.add(createCp1252("NIF :X-3856907Z  TEL :934199465\r\n"));

				list.add(createCp1252("------------------------------------------------\r\n"));
				
				list.add(createCp1252("MESA: 100 P: - FECHA: YYYY-MM-DD\r\n"));
				list.add(createCp1252("CAN P/U DESCRIPCION  SUMA\r\n"));

				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x00 }); // Alignment

				list.add(createCp1252("------------------------------------------------\r\n"));

				list.add(createCp1252(" 4\t 3,00\t JARRA  CERVESA \t\t 12,00\r\n"));
				list.add(createCp1252(" 1\t 1,60\t COPA DE CERVESA\t\t  1,60\r\n"));
				
				list.add(createCp1252("------------------------------------------------\r\n"));

				list.add(createCp1252("\t\t\t\t\t SUB TOTAL :\t\t 13,60\r\n"));
				
				list.add(new byte[] { 0x09, 0x1b, 0x69, 0x01, 0x00 });
				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x02 });
				list.add(createCp1252("TOTAL:     13,60 EUROS\r\n"));
				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x00 });
				list.add(new byte[] { 0x1b, 0x69, 0x00, 0x00 });

				list.add(createCp1252("NO: 000018851  IVA INCLUIDO\r\n"));
				list.add(createCp1252("------------------------------------------------\r\n"));

				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x01 });
				list.add(createCp1252("**** GRACIAS POR SU VISITA! ****\r\n"));
				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x00 });

				// 1D barcode example
				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x01 });
				list.add(new byte[] { 0x1b, 0x62, 0x06, 0x02, 0x02 });

				list.add(createCp1252(" 12ab34cd56\u001e\r\n"));

				list.add(new byte[] { 0x1b, 0x64, 0x02 }); // Cut
				list.add(new byte[] { 0x07 }); // Kick cash drawer

				sendCommand(context, portName, portSettings, list);
			} else if (strPrintArea.equals("4inch (112mm)")) {
				ArrayList<byte[]> list = new ArrayList<byte[]>();

				list.add(new byte[] { 0x1b, 0x1d, 0x74, 0x20 }); // Code Page #1252 (Windows Latin-1)

				list.add(new byte[] { 0x1b, 0x44, 0x06, 0x0a, 0x10, 0x14, 0x1a, 0x22, 0x24, 0x28, 0x30, 0x3a, 0x40, 0x00 }); // Set horizontal tab

				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x01 }); // Alignment (center)

				// list.add("[If loaded.. Logo1 goes here]\r\n".getBytes());

				// list.add(new byte[]{0x1b, 0x1c, 0x70, 0x01, 0x00, '\r', '\n'}); //Stored Logo Printing

				// Notice that we use a unicode representation because that is
				// how Java expresses these bytes as double byte unicode

				// Character expansion
				list.add(new byte[] { 0x1b, 0x69, 0x01, 0x01 });

				list.add(createCp1252("BAR RESTAURANT EL POZO\r\n"));
				
				list.add(new byte[] { 0x1b, 0x69, 0x00, 0x00 }); // Cancel Character Expansion
				
				list.add(createCp1252("C/.ROCAFORT 187 08029 BARCELONA\r\n"));
				
				list.add(createCp1252("NIF :X-3856907Z  TEL :934199465\r\n"));

				list.add(createCp1252("---------------------------------------------------------------------\r\n"));
				
				list.add(createCp1252("MESA: 100 P: - FECHA: YYYY-MM-DD\r\n"));
				list.add(createCp1252("CAN P/U DESCRIPCION  SUMA\r\n"));

				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x00 }); // Alignment

				list.add(createCp1252("---------------------------------------------------------------------\r\n"));

				list.add(createCp1252(" 4\t 3,00\t\t JARRA  CERVESA \t\t\t 12,00\r\n"));
				list.add(createCp1252(" 1\t 1,60\t\t COPA DE CERVESA\t\t\t  1,60\r\n"));
				
				list.add(createCp1252("---------------------------------------------------------------------\r\n"));

				list.add(createCp1252("\t\t\t\t\t\t\t\t SUB TOTAL :\t 13,60\r\n"));
				
				list.add(new byte[] { 0x09, 0x1b, 0x69, 0x01, 0x00 });
				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x02 });
				list.add(createCp1252("TOTAL:     13,60 EUROS\r\n"));
				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x00 });
				list.add(new byte[] { 0x1b, 0x69, 0x00, 0x00 });

				list.add(createCp1252("NO: 000018851  IVA INCLUIDO\r\n"));
				list.add(createCp1252("---------------------------------------------------------------------\r\n"));

				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x01 });
				list.add(createCp1252("**** GRACIAS POR SU VISITA! ****\r\n"));
				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x00 });

				// 1D barcode example
				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x01 });
				list.add(new byte[] { 0x1b, 0x62, 0x06, 0x02, 0x02 });

				list.add(createCp1252(" 12ab34cd56\u001e\r\n"));

				list.add(new byte[] { 0x1b, 0x64, 0x02 }); // Cut
				list.add(new byte[] { 0x07 }); // Kick cash drawer

				sendCommand(context, portName, portSettings, list);
			}
		} else if (commandType == "Raster") {
			if (strPrintArea.equals("2inch (58mm)")) {
				ArrayList<byte[]> list = new ArrayList<byte[]>();

				printableArea = 384; // Printable area in paper is 384(dot)

				RasterDocument rasterDoc = new RasterDocument(RasSpeed.Medium, RasPageEndMode.FeedAndFullCut, RasPageEndMode.FeedAndFullCut, RasTopMargin.Standard, 0, 0, 0);

				if (rasterType == RasterCommand.Standard) {
					list.add(rasterDoc.BeginDocumentCommandData());
				}

				String textToPrint = (
						"            BAR RESTAURANT\r\n" +
						"                   EL POZO\r\n");
				list.add(createRasterCommand(textToPrint, 13, 0, rasterType));

				textToPrint = (
						"C/.ROCAFORT 187\r\n" +
						"08029 BARCELONA\r\n" +
						"NIF :X-3856907Z\n" +
						"TEL :934199465\n" +
						"---------------------------------------------\r\n" +
						"MESA: 100 P: - \n" +
						"FECHA: YYYY-MM-DD\n" +
						"CAN P/U DESCRIPCION  SUMA\n" +
						"---------------------------------------------\r\n" +
						"4 3,00 JARRA  CERVESA   12,00\n" +
						"1 1,60 COPA DE CERVESA  1,60\n" +
						"---------------------------------------------\r\n" +
						"                     SUB TOTAL :  13,60\n" +
						"TOTAL:               13,60 EUROS\n" +
						"NO: 000018851 IVA INCLUIDO\n\n" +
						"---------------------------------------------\r\n" +
						"**GRACIAS POR SU VISITA!**\r\n\r\n");

				list.add(createRasterCommand(textToPrint, 13, 0, rasterType));

				if (rasterType == RasterCommand.Standard) {
					list.add(rasterDoc.EndDocumentCommandData());
					list.add(new byte[] { 0x07 }); // Kick cash drawer
				} else {
					list.add(new byte[] { 0x1b, 0x64, 0x02 }); // Cut
				}

				sendCommand(context, portName, portSettings, list);
			} else if (strPrintArea.equals("3inch (80mm)")) {
				ArrayList<byte[]> list = new ArrayList<byte[]>();

				printableArea = 576; // Printable area in paper is 576(dot)

				RasterDocument rasterDoc = new RasterDocument(RasSpeed.Medium, RasPageEndMode.FeedAndFullCut, RasPageEndMode.FeedAndFullCut, RasTopMargin.Standard, 0, 0, 0);
				
				if (rasterType == RasterCommand.Standard) {
					list.add(rasterDoc.BeginDocumentCommandData());
				}

				String textToPrint = (
						"                BAR RESTAURANT EL POZO\r\n");
				list.add(createRasterCommand(textToPrint, 13, 0, rasterType));

				textToPrint = (
						"C/.ROCAFORT 187 08029 BARCELONA\r\n" +
						"NIF :X-3856907Z  TEL :934199465\n" +
						"-----------------------------------------------------------------------\r\n" +
						"MESA: 100 P: - FECHA: YYYY-MM-DD\n" +
						"CAN P/U DESCRIPCION  SUMA\n" +
						"-----------------------------------------------------------------------\r\n" +
						"4    3,00    JARRA  CERVESA                      12,00\n" +
						"1    1,60    COPA DE CERVESA                     1,60\n" +
						"-----------------------------------------------------------------------\r\n" +
						"                                        SUB TOTAL :       13,60\n" +
						"                                        TOTAL :      13,60 EUROS\n" +
						"NO: 000018851 IVA INCLUIDO\n\n" +
						"-----------------------------------------------------------------------\r\n" +
						"              ***GRACIAS POR SU VISITA!***\r\n\r\n");

				list.add(createRasterCommand(textToPrint, 13, 0, rasterType));

				if (rasterType == RasterCommand.Standard) {
					list.add(rasterDoc.EndDocumentCommandData());
					list.add(new byte[] { 0x07 }); // Kick cash drawer
				} else {
					list.add(new byte[] { 0x1b, 0x64, 0x02 }); // Cut
				}

				sendCommand(context, portName, portSettings, list);
			} else if (strPrintArea.equals("4inch (112mm)")) {
				ArrayList<byte[]> list = new ArrayList<byte[]>();

				printableArea = 832; // Printable area in paper is 832(dot)

				RasterDocument rasterDoc = new RasterDocument(RasSpeed.Medium, RasPageEndMode.FeedAndFullCut, RasPageEndMode.FeedAndFullCut, RasTopMargin.Standard, 0, 0, 0);
				
				if (rasterType == RasterCommand.Standard) {
					list.add(rasterDoc.BeginDocumentCommandData());
				}

				String textToPrint = (
						"                                    BAR RESTAURANT EL POZO\r\n");
				list.add(createRasterCommand(textToPrint, 13, 0, rasterType));

				textToPrint = (
						"                         C/.ROCAFORT 187 08029 BARCELONA\r\n" +
						"                              NIF :X-3856907Z  TEL :934199465\n" +
						"-------------------------------------------------------------------------------------------------------\n" +
						"MESA: 100 P: - FECHA: YYYY-MM-DD\n" +
						"CAN P/U DESCRIPCION  SUMA\n" +
						"-------------------------------------------------------------------------------------------------------\n" +
						"4    3,00    JARRA  CERVESA                                                    12,00\n" +
						"1    1,60    COPA DE CERVESA                                                   1,60\n" +
						"-------------------------------------------------------------------------------------------------------\n" +
						"                                                                      SUB TOTAL :       13,60\n" +
						"                                                                      TOTAL :      13,60 EUROS\n" +
						"NO: 000018851 IVA INCLUIDO\n\n" +
						"-------------------------------------------------------------------------------------------------------\n" +
						"                             ***GRACIAS POR SU VISITA!***\r\n\r\n");

				list.add(createRasterCommand(textToPrint, 13, 0, rasterType));

				if (rasterType == RasterCommand.Standard) {
					list.add(rasterDoc.EndDocumentCommandData());
					list.add(new byte[] { 0x07 }); // Kick cash drawer
				} else {
					list.add(new byte[] { 0x1b, 0x64, 0x02 }); // Cut
				}

				sendCommand(context, portName, portSettings, list);
			}
		}
	}

	/**
	 * This function shows how to print the receipt data of a thermal POS printer.
	 * 
	 * @param context
	 *     Activity for displaying messages to the user
	 * @param portName
	 *     Port name to use for communication. This should be (TCP:<IPAddress>)
	 * @param portSettings
	 *     Should be blank
	 * @param commandType
	 *     Command type to use for printing. This should be ("Line" or "Raster")
	 * @param strPrintArea
	 *     Printable area size, This should be ("3inch (80mm)" or "4inch (112mm)")
	 */
	public static void PrintSampleReceiptJp(Context context, String portName, String portSettings, String commandType, String strPrintArea, RasterCommand rasterType) {
		if ("Line" == commandType) {
			if (strPrintArea.equals("2inch (58mm)")) {
				ArrayList<byte[]> list = new ArrayList<byte[]>();

				byte[] outputByteBuffer = null;
				list.add(new byte[] { 0x1b, 0x40 }); // Initialization
				// list.add(new byte[]{0x1d, 0x57, (byte) 0x80, 0x01});
				list.add(new byte[] { 0x1b, 0x73, 0x30, 0x30 });
				list.add(new byte[] { 0x1b, 0x24, 0x31 });
				list.add(new byte[] { 0x1b, 0x44, 0x10, 0x00 });
				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x31 });

				list.add(new byte[] { 0x1b, 0x69, 0x02, 0x00 });
				list.add(new byte[] { 0x1b, 0x45 });

				list.add(createShiftJIS(context.getResources().getString(R.string.title_company_name) + "\n"));

				list.add(new byte[] { 0x1b, 0x69, 0x01, 0x00 });

				list.add(createShiftJIS(context.getResources().getString(R.string.title_receipt_name) + "\n"));

				list.add(new byte[] { 0x1b, 0x69, 0x00, 0x00 });
				list.add(new byte[] { 0x1b, 0x46 });

				list.add(createShiftJIS("--------------------------------\n"));

				Calendar calendar = Calendar.getInstance();
				int year = calendar.get(Calendar.YEAR);
				int month = calendar.get(Calendar.MONTH);
				int day = calendar.get(Calendar.DAY_OF_MONTH);
				String YMD = (year + context.getResources().getString(R.string.year) + (month + 1) + context.getResources().getString(R.string.month) + day + context.getResources().getString(R.string.day)).toString();

				int hour24 = calendar.get(Calendar.HOUR_OF_DAY);
				int minute = calendar.get(Calendar.MINUTE);
				String TIME = (hour24 + context.getResources().getString(R.string.hour) + minute + context.getResources().getString(R.string.min)).toString();

				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x30 });

				list.add(createShiftJIS(context.getResources().getString(R.string.date) + YMD + "  " + TIME + "\n"));

				list.add(createShiftJIS("TEL:054-347-XXXX\n\n"));

				list.add(createShiftJIS(context.getResources().getString(R.string.kana_line) + "\n"));

				list.add(createShiftJIS(context.getResources().getString(R.string.personalInfo)));

				list.add(createShiftJIS(context.getResources().getString(R.string.ItemInfo_2inch_mobile)));

				int sub = 0;
				int tax = 0;
				sub = 10000 + 3800 + 2000 + 15000 + 5000;
				NumberFormat exsub = NumberFormat.getNumberInstance();

				tax = sub * 5 / 100;
				NumberFormat extax = NumberFormat.getNumberInstance();

				outputByteBuffer = createShiftJIS(context.getResources().getString(R.string.sub_2inch_mobile) + exsub.format(sub) + "\n\n" + context.getResources().getString(R.string.tax_2inch_mobile) + extax.format(tax) + "\n\n" + context.getResources().getString(R.string.total_2inch_mobile) + exsub.format(sub) + "\n\n" + context.getResources().getString(R.string.phone) + "\n\n");
				list.add(outputByteBuffer);

				list.add(new byte[] { 0x1b, 0x64, 0x33 }); // Cut
				list.add(new byte[] { 0x07 }); // Kick cash drawer

				sendCommand(context, portName, portSettings, list);
			} else if (strPrintArea.equals("3inch (80mm)")) {
				ArrayList<byte[]> list = new ArrayList<byte[]>();

				byte[] outputByteBuffer = null;
				list.add(new byte[] { 0x1b, 0x40 }); // Initialization
				// list.add(new byte[]{0x1d, 0x57, (byte) 0x80, 0x01});
				list.add(new byte[] { 0x1b, 0x24, 0x31 });
				list.add(new byte[] { 0x1b, 0x44, 0x10, 0x00 });
				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x31 });

				list.add(new byte[] { 0x1b, 0x69, 0x02, 0x00 });
				list.add(new byte[] { 0x1b, 0x45 });

				list.add(createShiftJIS(context.getResources().getString(R.string.title_company_name) + "\n"));

				list.add(new byte[] { 0x1b, 0x69, 0x01, 0x00 });

				list.add(createShiftJIS(context.getResources().getString(R.string.title_receipt_name) + "\n"));

				list.add(new byte[] { 0x1b, 0x69, 0x00, 0x00 });
				list.add(new byte[] { 0x1b, 0x46 });

				list.add(createShiftJIS("------------------------------------------------\n"));

				Calendar calendar = Calendar.getInstance();
				int year = calendar.get(Calendar.YEAR);
				int month = calendar.get(Calendar.MONTH);
				int day = calendar.get(Calendar.DAY_OF_MONTH);
				String YMD = (year + context.getResources().getString(R.string.year) + (month + 1) + context.getResources().getString(R.string.month) + day + context.getResources().getString(R.string.day)).toString();

				int hour24 = calendar.get(Calendar.HOUR_OF_DAY);
				int minute = calendar.get(Calendar.MINUTE);
				String TIME = (hour24 + context.getResources().getString(R.string.hour) + minute + context.getResources().getString(R.string.min)).toString();

				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x30 });

				list.add(createShiftJIS(context.getResources().getString(R.string.date) + YMD + "  " + TIME + "\n"));

				list.add(createShiftJIS("TEL:054-347-XXXX\n\n"));

				list.add(createShiftJIS(context.getResources().getString(R.string.kana_line) + "\n"));

				list.add(createShiftJIS(context.getResources().getString(R.string.personalInfo)));

				list.add(createShiftJIS(context.getResources().getString(R.string.ItemInfo_3inch_line)));

				int sub = 0;
				int tax = 0;
				sub = 10000 + 3800 + 2000 + 15000 + 5000;
				NumberFormat exsub = NumberFormat.getNumberInstance();

				tax = sub * 5 / 100;
				NumberFormat extax = NumberFormat.getNumberInstance();

				outputByteBuffer = createShiftJIS(context.getResources().getString(R.string.sub_3inch_line) + exsub.format(sub) + "\n\n" + context.getResources().getString(R.string.tax_3inch_line) + extax.format(tax) + "\n\n" + context.getResources().getString(R.string.total_3inch_line) + exsub.format(sub) + "\n\n" + context.getResources().getString(R.string.phone) + "\n\n");
				list.add(outputByteBuffer);

				list.add(new byte[] { 0x1b, 0x64, 0x33 }); // Cut
				list.add(new byte[] { 0x07 }); // Kick cash drawer

				sendCommand(context, portName, portSettings, list);
			} else if (strPrintArea.equals("4inch (112mm)")) {
				ArrayList<byte[]> list = new ArrayList<byte[]>();

				byte[] outputByteBuffer = null;
				list.add(new byte[] { 0x1b, 0x40 }); // Initialization
				// list.add(new byte[]{0x1d, 0x57, (byte) 0x80, 0x01});
				list.add(new byte[] { 0x1b, 0x24, 0x31 });
				list.add(new byte[] { 0x1b, 0x44, 0x10, 0x00 });
				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x31 });

				list.add(new byte[] { 0x1b, 0x69, 0x02, 0x00 });
				list.add(new byte[] { 0x1b, 0x45 });

				list.add(createShiftJIS(context.getResources().getString(R.string.title_company_name) + "\n"));

				list.add(new byte[] { 0x1b, 0x69, 0x01, 0x00 });

				list.add(createShiftJIS(context.getResources().getString(R.string.title_receipt_name) + "\n"));

				list.add(new byte[] { 0x1b, 0x69, 0x00, 0x00 });
				list.add(new byte[] { 0x1b, 0x46 });

				list.add(createShiftJIS("--------------------------------------------------------------------\n"));

				Calendar calendar = Calendar.getInstance();
				int year = calendar.get(Calendar.YEAR);
				int month = calendar.get(Calendar.MONTH);
				int day = calendar.get(Calendar.DAY_OF_MONTH);
				String YMD = (year + context.getResources().getString(R.string.year) + (month + 1) + context.getResources().getString(R.string.month) + day + context.getResources().getString(R.string.day)).toString();

				int hour24 = calendar.get(Calendar.HOUR_OF_DAY);
				int minute = calendar.get(Calendar.MINUTE);
				String TIME = (hour24 + context.getResources().getString(R.string.hour) + minute + context.getResources().getString(R.string.min)).toString();

				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x30 });

				list.add(createShiftJIS(context.getResources().getString(R.string.date) + YMD + "  " + TIME + "\n"));

				list.add(createShiftJIS("TEL:054-347-XXXX\n\n"));

				list.add(createShiftJIS(context.getResources().getString(R.string.kana_line) + "\n"));

				list.add(createShiftJIS(context.getResources().getString(R.string.personalInfo)));

				list.add(createShiftJIS(context.getResources().getString(R.string.ItemInfo_4inch_line)));

				int sub = 0;
				int tax = 0;
				sub = 10000 + 3800 + 2000 + 15000 + 5000;
				NumberFormat exsub = NumberFormat.getNumberInstance();

				tax = sub * 5 / 100;
				NumberFormat extax = NumberFormat.getNumberInstance();

				list.add(new byte[] { 0x1b, 0x52, 0x08 });

				outputByteBuffer = createShiftJIS(context.getResources().getString(R.string.sub_4inch_line) + exsub.format(sub) + "\n\n" + context.getResources().getString(R.string.tax_4inch_line) + extax.format(tax) + "\n\n" + context.getResources().getString(R.string.total_4inch_line) + exsub.format(sub) + "\n\n" + context.getResources().getString(R.string.phone) + "\n\n");
				list.add(outputByteBuffer);

				list.add(new byte[] { 0x1b, 0x64, 0x33 });
				list.add(new byte[] { 0x07 }); // Kick cash drawer

				sendCommand(context, portName, portSettings, list);
			}
		} else if ("Raster" == commandType) {
			if (strPrintArea.equals("2inch (58mm)")) {
				ArrayList<byte[]> list = new ArrayList<byte[]>();

				printableArea = 384; // Printable area in paper is 384(dot)

				RasterDocument rasterDoc = new RasterDocument(RasSpeed.Medium, RasPageEndMode.FeedAndFullCut, RasPageEndMode.FeedAndFullCut, RasTopMargin.Standard, 0, 0, 0);
				
				if (rasterType == RasterCommand.Standard) {
					list.add(rasterDoc.BeginDocumentCommandData());
				}

				String textToPrint = (context.getResources().getString(R.string.title_company_name_raster_2inch) + context.getResources().getString(R.string.title_receipt_name_raster_2inch));
				list.add(createRasterCommand(textToPrint, 21, Typeface.BOLD, rasterType));

				list.add(createRasterCommand("-----------------------------------------------", 13, Typeface.BOLD, rasterType));

				Calendar calendar = Calendar.getInstance();
				int year = calendar.get(Calendar.YEAR);
				int month = calendar.get(Calendar.MONTH);
				int day = calendar.get(Calendar.DAY_OF_MONTH);
				String YMD = (year + context.getResources().getString(R.string.year) + (month + 1) + context.getResources().getString(R.string.month) + day + context.getResources().getString(R.string.day)).toString();

				int hour24 = calendar.get(Calendar.HOUR_OF_DAY);
				int minute = calendar.get(Calendar.MINUTE);
				String TIME = (hour24 + context.getResources().getString(R.string.hour) + minute + context.getResources().getString(R.string.min)).toString();

				textToPrint = (context.getResources().getString(R.string.date) + YMD + "  " + TIME + "\n" + "TEL:054-347-XXXX\n\n");
				list.add(createRasterCommand(textToPrint, 13, Typeface.BOLD, rasterType));

				textToPrint = (context.getResources().getString(R.string.kana_raster) + "\n" + context.getResources().getString(R.string.personalInfo));
				list.add(createRasterCommand(textToPrint, 13, Typeface.BOLD, rasterType));

				textToPrint = context.getResources().getString(R.string.ItemInfo_2inch_raster);
				list.add(createRasterCommand(textToPrint, 13, Typeface.BOLD, rasterType));

				int sub = 0;
				int tax = 0;
				sub = 10000 + 3800 + 2000 + 15000 + 5000;
				NumberFormat exsub = NumberFormat.getNumberInstance();

				tax = sub * 5 / 100;
				NumberFormat extax = NumberFormat.getNumberInstance();

				textToPrint = (context.getResources().getString(R.string.sub_2inch_raster) + exsub.format(sub) + "\n" + context.getResources().getString(R.string.tax_2inch_raster) + extax.format(tax) + "\n" + context.getResources().getString(R.string.total_2inch_raster) + exsub.format(sub) + "\n\n" + context.getResources().getString(R.string.phone_2inch) + "\n\n");
				list.add(createRasterCommand(textToPrint, 13, Typeface.BOLD, rasterType));

				if (rasterType == RasterCommand.Standard) {
					list.add(rasterDoc.EndDocumentCommandData());
					list.add(new byte[] { 0x07 }); // Kick cash drawer
				} else {
					list.add(new byte[] { 0x1b, 0x64, 0x02 }); // Cut
				}
			

				sendCommand(context, portName, portSettings, list);
			} else if (strPrintArea.equals("3inch (80mm)")) {
				ArrayList<byte[]> list = new ArrayList<byte[]>();

				printableArea = 576; // Printable area in paper is 576(dot)

				RasterDocument rasterDoc = new RasterDocument(RasSpeed.Medium, RasPageEndMode.FeedAndFullCut, RasPageEndMode.FeedAndFullCut, RasTopMargin.Standard, 0, 0, 0);
				
				if (rasterType == RasterCommand.Standard) {
					list.add(rasterDoc.BeginDocumentCommandData());
				}

				String textToPrint = (context.getResources().getString(R.string.title_company_name_raster_3inch) + context.getResources().getString(R.string.title_receipt_name_raster_3inch));
				list.add(createRasterCommand(textToPrint, 21, Typeface.BOLD, rasterType));

				list.add(createRasterCommand("-----------------------------------------------------------------------", 13, Typeface.BOLD, rasterType));

				Calendar calendar = Calendar.getInstance();
				int year = calendar.get(Calendar.YEAR);
				int month = calendar.get(Calendar.MONTH);
				int day = calendar.get(Calendar.DAY_OF_MONTH);
				String YMD = (year + context.getResources().getString(R.string.year) + (month + 1) + context.getResources().getString(R.string.month) + day + context.getResources().getString(R.string.day)).toString();

				int hour24 = calendar.get(Calendar.HOUR_OF_DAY);
				int minute = calendar.get(Calendar.MINUTE);
				String TIME = (hour24 + context.getResources().getString(R.string.hour) + minute + context.getResources().getString(R.string.min)).toString();

				textToPrint = (context.getResources().getString(R.string.date) + YMD + "  " + TIME + "\n" + "TEL:054-347-XXXX\n\n");
				list.add(createRasterCommand(textToPrint, 13, Typeface.BOLD, rasterType));

				textToPrint = (context.getResources().getString(R.string.kana_raster) + "\n" + context.getResources().getString(R.string.personalInfo));
				list.add(createRasterCommand(textToPrint, 13, Typeface.BOLD, rasterType));

				textToPrint = context.getResources().getString(R.string.ItemInfo_3inch_raster);
				list.add(createRasterCommand(textToPrint, 13, Typeface.BOLD, rasterType));

				int sub = 0;
				int tax = 0;
				sub = 10000 + 3800 + 2000 + 15000 + 5000;
				NumberFormat exsub = NumberFormat.getNumberInstance();

				tax = sub * 5 / 100;
				NumberFormat extax = NumberFormat.getNumberInstance();

				textToPrint = (context.getResources().getString(R.string.sub_3inch_raster) + exsub.format(sub) + "\n" + context.getResources().getString(R.string.tax_3inch_raster) + extax.format(tax) + "\n" + context.getResources().getString(R.string.total_3inch_raster) + exsub.format(sub) + "\n\n" + context.getResources().getString(R.string.phone) + "\n\n");
				list.add(createRasterCommand(textToPrint, 13, Typeface.BOLD, rasterType));

				if (rasterType == RasterCommand.Standard) {
					list.add(rasterDoc.EndDocumentCommandData());
					list.add(new byte[] { 0x07 }); // Kick cash drawer
				} else {
					list.add(new byte[] { 0x1b, 0x64, 0x02 }); // Cut
				}

				sendCommand(context, portName, portSettings, list);
			} else if (strPrintArea.equals("4inch (112mm)")) {
				ArrayList<byte[]> list = new ArrayList<byte[]>();

				printableArea = 832; // Printable area in paper is 832(dot)

				RasterDocument rasterDoc = new RasterDocument(RasSpeed.Medium, RasPageEndMode.FeedAndFullCut, RasPageEndMode.FeedAndFullCut, RasTopMargin.Standard, 0, 0, 0);
				
				if (rasterType == RasterCommand.Standard) {
					list.add(rasterDoc.BeginDocumentCommandData());
				}

				String textToPrint = (context.getResources().getString(R.string.title_company_name_raster_4inch) + context.getResources().getString(R.string.title_receipt_name_raster_4inch));
				list.add(createRasterCommand(textToPrint, 21, Typeface.BOLD, rasterType));

				list.add(createRasterCommand("------------------------------------------------------------------------------------------------------", 13, Typeface.BOLD, rasterType));

				Calendar calendar = Calendar.getInstance();
				int year = calendar.get(Calendar.YEAR);
				int month = calendar.get(Calendar.MONTH);
				int day = calendar.get(Calendar.DAY_OF_MONTH);
				String YMD = (year + context.getResources().getString(R.string.year) + (month + 1) + context.getResources().getString(R.string.month) + day + context.getResources().getString(R.string.day)).toString();

				int hour24 = calendar.get(Calendar.HOUR_OF_DAY);
				int minute = calendar.get(Calendar.MINUTE);
				String TIME = (hour24 + context.getResources().getString(R.string.hour) + minute + context.getResources().getString(R.string.min)).toString();

				textToPrint = (context.getResources().getString(R.string.date) + YMD + "  " + TIME + "\n" + "TEL:054-347-XXXX\n\n");
				list.add(createRasterCommand(textToPrint, 13, Typeface.BOLD, rasterType));

				textToPrint = (context.getResources().getString(R.string.kana_raster) + "\n" + context.getResources().getString(R.string.personalInfo));
				list.add(createRasterCommand(textToPrint, 13, Typeface.BOLD, rasterType));

				textToPrint = context.getResources().getString(R.string.ItemInfo_4inch_raster);
				list.add(createRasterCommand(textToPrint, 13, Typeface.BOLD, rasterType));

				int sub = 0;
				int tax = 0;
				sub = 10000 + 3800 + 2000 + 15000 + 5000;
				NumberFormat exsub = NumberFormat.getNumberInstance();

				tax = sub * 5 / 100;
				NumberFormat extax = NumberFormat.getNumberInstance();

				list.add(new byte[] { 0x1b, 0x52, 0x08 });

				textToPrint = (context.getResources().getString(R.string.sub_4inch_raster) + exsub.format(sub) + "\n" + context.getResources().getString(R.string.tax_4inch_raster) + extax.format(tax) + "\n" + context.getResources().getString(R.string.total_4inch_raster) + exsub.format(sub) + "\n\n" + context.getResources().getString(R.string.phone) + "\n\n\n\n");
				list.add(createRasterCommand(textToPrint, 13, Typeface.BOLD, rasterType));

				if (rasterType == RasterCommand.Standard) {
					list.add(rasterDoc.EndDocumentCommandData());
					list.add(new byte[] { 0x07 }); // Kick cash drawer
				} else {
					list.add(new byte[] { 0x1b, 0x64, 0x02 }); // Cut
				}

				sendCommand(context, portName, portSettings, list);
			}
		}
	}

	/**
	 * This function shows how to print the receipt data of a thermal POS printer.
	 * 
	 * @param context
	 *     Activity for displaying messages to the user
	 * @param portName
	 *     Port name to use for communication. This should be (TCP:<IPAddress>)
	 * @param portSettings
	 *     Should be blank
	 * @param commandType
	 *     Command type to use for printing. This should be ("Line" or "Raster")
	 * @param strPrintArea
	 *     Printable area size, This should be ("3inch (80mm)" or "4inch (112mm)")
	 */
	public static void PrintSampleReceiptCHS(Context context, String portName, String portSettings, String commandType, String strPrintArea, RasterCommand rasterType) {
		if ("Line" == commandType) {
			if (strPrintArea.equals("2inch (58mm)")) {
				ArrayList<byte[]> list = new ArrayList<byte[]>();

				list.add(new byte[] { 0x1b, 0x40 }); // Initialization
				// list.add(new byte[]{0x1d, 0x57, (byte) 0x80, 0x01});
				// list.add(new byte[]{0x1b, 0x24, 0x31});
				list.add(new byte[] { 0x1b, 0x44, 0x10, 0x00 }); // <ESC> <D> n1 n2 nk <NUL>
				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x31 }); // <ESC> <GS> a n

				list.add(new byte[] { 0x1b, 0x69, 0x02, 0x00 }); // <ESC> <i> n1 n2
				list.add(new byte[] { 0x1b, 0x45 }); // <ESC> <E>

				list.add(createGB2312(context.getResources().getString(R.string.title_company_name_chs) + "\n"));

				list.add(new byte[] { 0x1b, 0x69, 0x01, 0x00 }); // <ESC> <i> n1 n2

				list.add(createGB2312(context.getResources().getString(R.string.title_receipt_name_chs) + "\n"));

				list.add(new byte[] { 0x1b, 0x69, 0x00, 0x00 }); // <ESC> <i> n1 n2
				list.add(new byte[] { 0x1b, 0x46 }); // <ESC> <F>

				list.add(createGB2312(context.getResources().getString(R.string.address_2inch_line_chs)));

				list.add(createGB2312(context.getResources().getString(R.string.phone_chs) + "\n"));

				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x30 }); // <ESC> <GS> a n

				list.add(createGB2312(context.getResources().getString(R.string.ItemInfo_2inch_line_chs)));

				list.add(createGB2312(context.getResources().getString(R.string.total_2inch_line_chs) + "\n"));

				list.add(createGB2312(context.getResources().getString(R.string.cash_2inch_line_chs) + "\n"));

				list.add(createGB2312(context.getResources().getString(R.string.findforeclosure_2inch_line_chs) + "\n"));

				list.add(createGB2312(context.getResources().getString(R.string.cardnumber_2inch_line_chs) + "\n"));

				list.add(createGB2312(context.getResources().getString(R.string.cardbalance_2inch_line_chs) + "\n"));

				list.add(createGB2312(context.getResources().getString(R.string.machinenumber_2inch_line_chs) + "\n"));

				list.add(createGB2312(context.getResources().getString(R.string.receiptinfo_2inch_line_chs) + "\n"));

				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x31 }); // <ESC> <GS> a n

				list.add(createGB2312(context.getResources().getString(R.string.cashier_2inch_line_chs) + "\n"));

				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x30 }); // <ESC> <GS> a n

				list.add(new byte[] { 0x1b, 0x64, 0x33 }); // Cut
				list.add(new byte[] { 0x07 }); // Kick cash drawer

				sendCommand(context, portName, portSettings, list);
			} else if (strPrintArea.equals("3inch (80mm)")) {
				ArrayList<byte[]> list = new ArrayList<byte[]>();

				list.add(new byte[] { 0x1b, 0x40 }); // Initialization
				// list.add(new byte[]{0x1d, 0x57, (byte) 0x80, 0x01});
				// list.add(new byte[]{0x1b, 0x24, 0x31});
				list.add(new byte[] { 0x1b, 0x44, 0x10, 0x00 }); // <ESC> <D> n1 n2 nk <NUL>
				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x31 }); // <ESC> <GS> a n

				list.add(new byte[] { 0x1b, 0x69, 0x02, 0x00 }); // <ESC> <i> n1 n2
				list.add(new byte[] { 0x1b, 0x45 }); // <ESC> <E>

				list.add(createGB2312(context.getResources().getString(R.string.title_company_name_chs) + "\n"));

				list.add(new byte[] { 0x1b, 0x69, 0x01, 0x00 }); // <ESC> <i> n1 n2

				list.add(createGB2312(context.getResources().getString(R.string.title_receipt_name_chs) + "\n"));

				list.add(new byte[] { 0x1b, 0x69, 0x00, 0x00 }); // <ESC> <i> n1 n2
				list.add(new byte[] { 0x1b, 0x46 }); // <ESC> <F>

				list.add(createGB2312(context.getResources().getString(R.string.address_chs)));

				list.add(createGB2312(context.getResources().getString(R.string.phone_chs) + "\n"));

				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x30 }); // <ESC> <GS> a n

				list.add(createGB2312(context.getResources().getString(R.string.ItemInfo_3inch_line_chs)));

				list.add(createGB2312(context.getResources().getString(R.string.total_3inch_line_chs) + "\n"));

				list.add(createGB2312(context.getResources().getString(R.string.cash_3inch_line_chs) + "\n"));

				list.add(createGB2312(context.getResources().getString(R.string.findforeclosure_3inch_line_chs) + "\n"));

				list.add(createGB2312(context.getResources().getString(R.string.cardnumber_3inch_line_chs) + "\n"));

				list.add(createGB2312(context.getResources().getString(R.string.cardbalance_3inch_line_chs) + "\n"));

				list.add(createGB2312(context.getResources().getString(R.string.machinenumber_3inch_line_chs) + "\n"));

				list.add(createGB2312(context.getResources().getString(R.string.receiptinfo_3inch_line_chs) + "\n"));

				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x31 }); // <ESC> <GS> a n

				list.add(createGB2312(context.getResources().getString(R.string.cashier_3inch_line_chs) + "\n"));

				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x30 }); // <ESC> <GS> a n

				list.add(new byte[] { 0x1b, 0x64, 0x33 }); // Cut
				list.add(new byte[] { 0x07 }); // Kick cash drawer

				sendCommand(context, portName, portSettings, list);
			} else if (strPrintArea.equals("4inch (112mm)")) {
				ArrayList<byte[]> list = new ArrayList<byte[]>();

				list.add(new byte[] { 0x1b, 0x40 }); // Initialization
				// list.add(new byte[]{0x1d, 0x57, (byte) 0x80, 0x01});
				// list.add(new byte[]{0x1b, 0x24, 0x31});
				list.add(new byte[] { 0x1b, 0x44, 0x10, 0x00 }); // <ESC> <D> n1 n2 nk <NUL>
				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x31 }); // <ESC> <GS> a n

				list.add(new byte[] { 0x1b, 0x69, 0x02, 0x00 }); // <ESC> <i> n1 n2
				list.add(new byte[] { 0x1b, 0x45 }); // <ESC> <E>

				list.add(createGB2312(context.getResources().getString(R.string.title_company_name_chs) + "\n"));

				list.add(new byte[] { 0x1b, 0x69, 0x01, 0x00 }); // <ESC> <i> n1 n2

				list.add(createGB2312(context.getResources().getString(R.string.title_receipt_name_chs) + "\n"));

				list.add(new byte[] { 0x1b, 0x69, 0x00, 0x00 }); // <ESC> <i> n1 n2
				list.add(new byte[] { 0x1b, 0x46 }); // <ESC> <F>

				list.add(createGB2312(context.getResources().getString(R.string.address_chs)));

				list.add(createGB2312(context.getResources().getString(R.string.phone_chs) + "\n"));

				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x30 }); // <ESC> <GS> a n

				list.add(createGB2312(context.getResources().getString(R.string.ItemInfo_4inch_line_chs)));

				list.add(createGB2312(context.getResources().getString(R.string.total_4inch_line_chs) + "\n"));

				list.add(createGB2312(context.getResources().getString(R.string.cash_4inch_line_chs) + "\n"));

				list.add(createGB2312(context.getResources().getString(R.string.findforeclosure_4inch_line_chs) + "\n"));

				list.add(createGB2312(context.getResources().getString(R.string.cardnumber_4inch_line_chs) + "\n"));

				list.add(createGB2312(context.getResources().getString(R.string.cardbalance_4inch_line_chs) + "\n"));

				list.add(createGB2312(context.getResources().getString(R.string.machinenumber_4inch_line_chs) + "\n"));

				list.add(createGB2312(context.getResources().getString(R.string.receiptinfo_4inch_line_chs) + "\n"));

				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x31 }); // <ESC> <GS> a n

				list.add(createGB2312(context.getResources().getString(R.string.cashier_4inch_line_chs) + "\n"));

				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x30 }); // <ESC> <GS> a n

				list.add(new byte[] { 0x1b, 0x64, 0x33 }); // Cut
				list.add(new byte[] { 0x07 }); // Kick cash drawer

				sendCommand(context, portName, portSettings, list);
			}
		} else if ("Raster" == commandType) {
			if (strPrintArea.equals("2inch (58mm)")) {
				ArrayList<byte[]> list = new ArrayList<byte[]>();

				printableArea = 384; // Printable area in paper is 384(dot)

				RasterDocument rasterDoc = new RasterDocument(RasSpeed.Medium, RasPageEndMode.FeedAndFullCut, RasPageEndMode.FeedAndFullCut, RasTopMargin.Standard, 0, 0, 0);
				if (rasterType == RasterCommand.Standard) {
					list.add(rasterDoc.BeginDocumentCommandData());
				}

				String textToPrint = (context.getResources().getString(R.string.title_company_name_raster_2inch_chs) + context.getResources().getString(R.string.title_receipt_name_raster_2inch_chs));
				list.add(createRasterCommand(textToPrint, 21, Typeface.BOLD, rasterType));

				textToPrint = (context.getResources().getString(R.string.address_2inch_chs) + context.getResources().getString(R.string.phone_chs));
				list.add(createRasterCommand(textToPrint, 13, Typeface.BOLD, rasterType));

				textToPrint = context.getResources().getString(R.string.ItemInfo_2inch_raster_chs);
				list.add(createRasterCommand(textToPrint, 13, Typeface.BOLD, rasterType));

				textToPrint = (context.getResources().getString(R.string.total_2inch_raster_chs) + "\n" + context.getResources().getString(R.string.cash_2inch_raster_chs) + "\n" + context.getResources().getString(R.string.findforeclosure_2inch_raster_chs) + "\n\n");
				list.add(createRasterCommand(textToPrint, 13, Typeface.BOLD, rasterType));

				textToPrint = (context.getResources().getString(R.string.cardnumber_2inch_raster_chs) + "\n" + context.getResources().getString(R.string.cardbalance_2inch_raster_chs) + "\n" + context.getResources().getString(R.string.machinenumber_2inch_raster_chs) + "\n\n");
				list.add(createRasterCommand(textToPrint, 13, Typeface.BOLD, rasterType));

				textToPrint = context.getResources().getString(R.string.receiptinfo_2inch_raster_chs);
				list.add(createRasterCommand(textToPrint, 13, Typeface.BOLD, rasterType));

				textToPrint = context.getResources().getString(R.string.cashier_2inch_raster_chs);
				list.add(createRasterCommand(textToPrint, 13, Typeface.BOLD, rasterType));

				if (rasterType == RasterCommand.Standard) {
					list.add(rasterDoc.EndDocumentCommandData());
					list.add(new byte[] { 0x07 }); // Kick cash drawer
				} else {
					list.add(new byte[] { 0x1b, 0x64, 0x02 }); // Cut
				}

				sendCommand(context, portName, portSettings, list);
			} else if (strPrintArea.equals("3inch (80mm)")) {
				ArrayList<byte[]> list = new ArrayList<byte[]>();

				printableArea = 576; // Printable area in paper is 576(dot)

				RasterDocument rasterDoc = new RasterDocument(RasSpeed.Medium, RasPageEndMode.FeedAndFullCut, RasPageEndMode.FeedAndFullCut, RasTopMargin.Standard, 0, 0, 0);
				if (rasterType == RasterCommand.Standard) {
					list.add(rasterDoc.BeginDocumentCommandData());
				}

				String textToPrint = (context.getResources().getString(R.string.title_company_name_raster_3inch_chs) + context.getResources().getString(R.string.title_receipt_name_raster_3inch_chs));
				list.add(createRasterCommand(textToPrint, 21, Typeface.BOLD, rasterType));

				textToPrint = (context.getResources().getString(R.string.address_chs) + context.getResources().getString(R.string.phone_chs));
				list.add(createRasterCommand(textToPrint, 13, Typeface.BOLD, rasterType));

				textToPrint = context.getResources().getString(R.string.ItemInfo_3inch_raster_chs);
				list.add(createRasterCommand(textToPrint, 13, Typeface.BOLD, rasterType));

				textToPrint = (context.getResources().getString(R.string.total_3inch_raster_chs) + "\n" + context.getResources().getString(R.string.cash_3inch_raster_chs) + "\n" + context.getResources().getString(R.string.findforeclosure_3inch_raster_chs) + "\n\n");
				list.add(createRasterCommand(textToPrint, 13, Typeface.BOLD, rasterType));

				textToPrint = (context.getResources().getString(R.string.cardnumber_3inch_raster_chs) + "\n" + context.getResources().getString(R.string.cardbalance_3inch_raster_chs) + "\n" + context.getResources().getString(R.string.machinenumber_3inch_raster_chs) + "\n\n");
				list.add(createRasterCommand(textToPrint, 13, Typeface.BOLD, rasterType));

				textToPrint = context.getResources().getString(R.string.receiptinfo_3inch_raster_chs);
				list.add(createRasterCommand(textToPrint, 13, Typeface.BOLD, rasterType));

				textToPrint = context.getResources().getString(R.string.cashier_3inch_raster_chs);
				list.add(createRasterCommand(textToPrint, 13, Typeface.BOLD, rasterType));

				if (rasterType == RasterCommand.Standard) {
					list.add(rasterDoc.EndDocumentCommandData());
					list.add(new byte[] { 0x07 }); // Kick cash drawer
				} else {
					list.add(new byte[] { 0x1b, 0x64, 0x02 }); // Cut
				}

				sendCommand(context, portName, portSettings, list);
			} else if (strPrintArea.equals("4inch (112mm)")) {
				ArrayList<byte[]> list = new ArrayList<byte[]>();

				printableArea = 832; // Printable area in paper is 832(dot)

				RasterDocument rasterDoc = new RasterDocument(RasSpeed.Medium, RasPageEndMode.FeedAndFullCut, RasPageEndMode.FeedAndFullCut, RasTopMargin.Standard, 0, 0, 0);
				if (rasterType == RasterCommand.Standard) {
					list.add(rasterDoc.BeginDocumentCommandData());
				}

				String textToPrint = (context.getResources().getString(R.string.title_company_name_raster_4inch_chs) + context.getResources().getString(R.string.title_receipt_name_raster_4inch_chs));
				list.add(createRasterCommand(textToPrint, 21, Typeface.BOLD, rasterType));

				textToPrint = (context.getResources().getString(R.string.address_4inch_raster_chs) + context.getResources().getString(R.string.phone_chs));
				list.add(createRasterCommand(textToPrint, 13, Typeface.BOLD, rasterType));

				textToPrint = context.getResources().getString(R.string.ItemInfo_4inch_raster_chs);
				list.add(createRasterCommand(textToPrint, 13, Typeface.BOLD, rasterType));

				textToPrint = (context.getResources().getString(R.string.total_4inch_raster_chs) + "\n" + context.getResources().getString(R.string.cash_4inch_raster_chs) + "\n" + context.getResources().getString(R.string.findforeclosure_4inch_raster_chs) + "\n\n");
				list.add(createRasterCommand(textToPrint, 13, Typeface.BOLD, rasterType));

				textToPrint = (context.getResources().getString(R.string.cardnumber_4inch_raster_chs) + "\n" + context.getResources().getString(R.string.cardbalance_4inch_raster_chs) + "\n" + context.getResources().getString(R.string.machinenumber_4inch_raster_chs) + "\n\n");
				list.add(createRasterCommand(textToPrint, 13, Typeface.BOLD, rasterType));

				textToPrint = context.getResources().getString(R.string.receiptinfo_4inch_raster_chs);
				list.add(createRasterCommand(textToPrint, 13, Typeface.BOLD, rasterType));

				textToPrint = context.getResources().getString(R.string.cashier_4inch_raster_chs);
				list.add(createRasterCommand(textToPrint, 13, Typeface.BOLD, rasterType));

				if (rasterType == RasterCommand.Standard) {
					list.add(rasterDoc.EndDocumentCommandData());
					list.add(new byte[] { 0x07 }); // Kick cash drawer
				} else {
					list.add(new byte[] { 0x1b, 0x64, 0x02 }); // Cut
				}

				sendCommand(context, portName, portSettings, list);
			}
		}
	}

	/**
	 * This function shows how to print the receipt data of a thermal POS printer.
	 * 
	 * @param context
	 *     Activity for displaying messages to the user
	 * @param portName
	 *     Port name to use for communication. This should be (TCP:<IPAddress>)
	 * @param portSettings
	 *     Should be blank
	 * @param commandType
	 *     Command type to use for printing. This should be ("Line" or "Raster")
	 * @param res
	 *     The resources object containing the image data. ( e.g.) getResources())
	 * @param strPrintArea
	 *     Printable area size, This should be ("3inch (80mm)" or "4inch (112mm)")
	 */
	public static void PrintSampleReceiptCHT(Context context, String portName, String portSettings, String commandType, Resources res, String strPrintArea, RasterCommand rasterType) {
		if ("Line" == commandType) {
			if (strPrintArea.equals("3inch (80mm)")) {
				ArrayList<byte[]> list = new ArrayList<byte[]>();

				list.add(new byte[] { 0x1b, 0x40 }); // Initialization
				// list.add(new byte[]{0x1d, 0x57, (byte) 0x80, 0x01});
				// list.add(new byte[]{0x1b, 0x24, 0x31});
				list.add(new byte[] { 0x1b, 0x44, 0x10, 0x00 }); // <ESC> <D> n1 n2 nk <NUL>
				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x31 }); // <ESC> <GS> a n

				list.add(new byte[] { 0x1b, 0x69, 0x02, 0x00 }); // <ESC> <i> n1 n2
				list.add(new byte[] { 0x1b, 0x45 }); // <ESC> <E>

				// list.add("[If loaded.. Logo1 goes here]\r\n".getBytes());
				// list.add(new byte[]{0x1b, 0x1c, 0x70, 0x01, 0x00, '\r', '\n'}); //Stored Logo Printing

				list.add(createBIG5(context.getResources().getString(R.string.title_company_name_cht) + "\n"));

				list.add(new byte[] { 0x1b, 0x69, 0x00, 0x00 }); // <ESC> <i> n1 n2
				list.add(new byte[] { 0x1b, 0x46 }); // <ESC> <F>

				list.add(createBIG5("--------------------------------------------"));

				list.add(new byte[] { 0x1b, 0x69, 0x01, 0x01 }); // <ESC> <i> n1 n2

				list.add(createBIG5(context.getResources().getString(R.string.title_receipt_name_cht) + "\n"));

				list.add(createBIG5(context.getResources().getString(R.string.cht_103) + "\n"));

				list.add(createBIG5(context.getResources().getString(R.string.ev_99999999_cht) + "\n"));

				list.add(new byte[] { 0x1b, 0x69, 0x00, 0x00 }); // <ESC> <i> n1 n2
				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x30 }); // <ESC> <GS> a n

				list.add(createBIG5(context.getResources().getString(R.string.date_cht) + "\n"));

				list.add(createBIG5(context.getResources().getString(R.string.random_code_cht) + "\n"));

				list.add(createBIG5(context.getResources().getString(R.string.seller_cht) + "\n"));

				// 1D barcode example
				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x01 });
				list.add(new byte[] { 0x1b, 0x62, 0x34, 0x31, 0x32, 0x50 });

				list.add("999999999\u001e\r\n".getBytes());

				// QR code
				list.add(new byte[] { 0x1b, 0x1d, 0x79, 0x53, 0x30, 0x02 });
				list.add(new byte[] { 0x1b, 0x1d, 0x79, 0x53, 0x31, 0x02 });
				list.add(new byte[] { 0x1b, 0x1d, 0x79, 0x53, 0x32, 0x05 });
				list.add(new byte[] { 0x1b, 0x1d, 0x79, 0x44, 0x31, 0x00, 0x23, 0x00 });

				list.add("http://www.star-m.jp/eng/index.html".getBytes());

				list.add(new byte[] { 0x1b, 0x1d, 0x79, 0x50, 0x0a });

				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x30 }); // <ESC> <GS> a n

				list.add(createBIG5(context.getResources().getString(R.string.Item_list_cht) + "\n"));

				list.add(createBIG5(context.getResources().getString(R.string.Item_list_Number_cht) + "\n\n\n"));

				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x31 }); // <ESC> <GS> a n

				list.add(createBIG5(context.getResources().getString(R.string.Sales_schedules_cht) + "\n"));

				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x30 }); // <ESC> <GS> a n

				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x32 }); // <ESC> <GS> a n

				list.add(createBIG5(context.getResources().getString(R.string.date_2_cht) + "\n"));

				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x30 }); // <ESC> <GS> a n

				list.add(createBIG5(context.getResources().getString(R.string.ItemInfo_3inch_line_cht) + "\n"));

				list.add(new byte[] { 0x1b, 0x45 }); // <ESC> <E>

				list.add(createBIG5(context.getResources().getString(R.string.sub_3inch_line_cht) + "\n"));

				list.add(createBIG5(context.getResources().getString(R.string.total_3inch_line_cht) + "\n"));

				list.add(new byte[] { 0x1b, 0x46 }); // <ESC> <F>

				list.add(createBIG5("--------------------------------------------\n"));

				list.add(createBIG5(context.getResources().getString(R.string.cash_3inch_line_cht) + "\n"));

				list.add(createBIG5(context.getResources().getString(R.string.change_3inch_line_cht) + "\n"));

				list.add(new byte[] { 0x1b, 0x45 }); // <ESC> <E>

				list.add(createBIG5(context.getResources().getString(R.string.Invoice_3inch_line_cht) + "\n"));

				list.add(new byte[] { 0x1b, 0x46 }); // <ESC> <F>

				list.add(createBIG5(context.getResources().getString(R.string.date_3_cht) + "\n"));

				// 1D barcode example
				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x01 });
				list.add(new byte[] { 0x1b, 0x62, 0x34, 0x31, 0x32, 0x50 });

				list.add("999999999\u001e\r\n".getBytes());

				list.add(new byte[] { 0x1b, 0x1d, 0x61, 0x30 }); // <ESC> <GS> a n

				list.add(createBIG5(context.getResources().getString(R.string.info_cht) + "\n"));

				list.add(createBIG5(context.getResources().getString(R.string.info_number_cht) + "\n"));

				list.add(new byte[] { 0x1b, 0x64, 0x33 }); // Cut
				list.add(new byte[] { 0x07 }); // Kick cash drawer

				sendCommand(context, portName, portSettings, list);
			}

		} else if ("Raster" == commandType) {
			if (strPrintArea.equals("2inch (58mm)")) {
				ArrayList<byte[]> list = new ArrayList<byte[]>();

				printableArea = 384; // Printable area in paper is 384(dot)

				RasterDocument rasterDoc = new RasterDocument(RasSpeed.Medium, RasPageEndMode.FeedAndFullCut, RasPageEndMode.FeedAndFullCut, RasTopMargin.Standard, 0, 0, 0);
				if (rasterType == RasterCommand.Standard) {
					list.add(rasterDoc.BeginDocumentCommandData());
				}

				String textToPrint = (context.getResources().getString(R.string.title_company_name_raster_2inch_cht));
				list.add(createRasterCommand(textToPrint, 21, Typeface.BOLD, rasterType));

				list.add(createRasterCommand("-----------------------------------------------", 13, 0, rasterType));

				textToPrint = context.getResources().getString(R.string.title_receipt_name_raster_2inch_cht);
				list.add(createRasterCommand(textToPrint, 27, 0, rasterType));

				textToPrint = (context.getResources().getString(R.string.cht_103_raster_2inch_cht));
				list.add(createRasterCommand(textToPrint, 28, 0, rasterType));

				textToPrint = (context.getResources().getString(R.string.ev_99999999_raster_2inch_cht));
				list.add(createRasterCommand(textToPrint, 28, 0, rasterType));

				textToPrint = (context.getResources().getString(R.string.date_raster_2inch_cht));
				list.add(createRasterCommand(textToPrint, 13, 0, rasterType));

				textToPrint = (context.getResources().getString(R.string.random_code_raster_2inch_cht));
				list.add(createRasterCommand(textToPrint, 13, 0, rasterType));

				textToPrint = (context.getResources().getString(R.string.seller_raster_2inch_cht));
				list.add(createRasterCommand(textToPrint, 13, 0, rasterType));

				// BarCode
				Bitmap bm = BitmapFactory.decodeResource(res, R.drawable.code39);
				StarBitmap starbitmap = new StarBitmap(bm, false, 200);
				if (rasterType == RasterCommand.Standard) {
					list.add(starbitmap.getImageRasterDataForPrinting_Standard(true));
				} else {
					list.add(starbitmap.getImageRasterDataForPrinting_graphic(true));
				}

				// QRCode
				Bitmap bitmap = BitmapFactory.decodeResource(res, R.drawable.qrcode);
				StarBitmap starbitmap2 = new StarBitmap(bitmap, false, 146);
				if (rasterType == RasterCommand.Standard) {
					list.add(starbitmap2.getImageRasterDataForPrinting_Standard(true));
				} else {
					list.add(starbitmap2.getImageRasterDataForPrinting_graphic(true));
				}

				textToPrint = (context.getResources().getString(R.string.Item_list_raster_2inch_cht));
				list.add(createRasterCommand(textToPrint, 13, 0, rasterType));

				textToPrint = (context.getResources().getString(R.string.Item_list_Number_raster_2inch_cht));
				list.add(createRasterCommand(textToPrint, 13, 0, rasterType));

				textToPrint = (context.getResources().getString(R.string.Sales_schedules_raster_2inch_cht));
				list.add(createRasterCommand(textToPrint, 13, 0, rasterType));

				textToPrint = (context.getResources().getString(R.string.date_2_raster_2inch_cht));
				list.add(createRasterCommand(textToPrint, 13, 0, rasterType));

				textToPrint = (context.getResources().getString(R.string.ItemInfo_raster_2inch_cht));
				list.add(createRasterCommand(textToPrint, 13, 0, rasterType));

				textToPrint = (context.getResources().getString(R.string.sub_raster_2inch_cht));
				list.add(createRasterCommand(textToPrint, 13, Typeface.BOLD, rasterType));

				textToPrint = (context.getResources().getString(R.string.total_raster_2inch_cht));
				list.add(createRasterCommand(textToPrint, 13, Typeface.BOLD, rasterType));

				list.add(createRasterCommand("-----------------------------------------------", 13, 0, rasterType));

				textToPrint = (context.getResources().getString(R.string.cash_raster_2inch_cht));
				list.add(createRasterCommand(textToPrint, 13, 0, rasterType));

				textToPrint = (context.getResources().getString(R.string.change_raster_2inch_cht));
				list.add(createRasterCommand(textToPrint, 13, 0, rasterType));

				textToPrint = (context.getResources().getString(R.string.Invoice_raster_2inch_cht));
				list.add(createRasterCommand(textToPrint, 13, Typeface.BOLD, rasterType));

				textToPrint = (context.getResources().getString(R.string.date_3_raster_2inch_cht));
				list.add(createRasterCommand(textToPrint, 13, 0, rasterType));

				// BarCode
				Bitmap bimap3 = BitmapFactory.decodeResource(res, R.drawable.code39);
				StarBitmap starbitmap3 = new StarBitmap(bimap3, false, 200);
				if (rasterType == RasterCommand.Standard) {
					list.add(starbitmap3.getImageRasterDataForPrinting_Standard(true));
				} else {
					list.add(starbitmap3.getImageRasterDataForPrinting_graphic(true));
				}

				textToPrint = (context.getResources().getString(R.string.info_raster_2inch_cht));
				list.add(createRasterCommand(textToPrint, 13, 0, rasterType));

				textToPrint = (context.getResources().getString(R.string.info_number_raster_2inch_cht));
				list.add(createRasterCommand(textToPrint, 13, 0, rasterType));

				if (rasterType == RasterCommand.Standard) {
					list.add(rasterDoc.EndDocumentCommandData());
					list.add(new byte[] { 0x07 }); // Kick cash drawer
				} else {
					list.add(new byte[] { 0x1b, 0x64, 0x02 }); // Cut
				}

				sendCommand(context, portName, portSettings, list);
			} else if (strPrintArea.equals("3inch (80mm)")) {
				ArrayList<byte[]> list = new ArrayList<byte[]>();

				printableArea = 576; // Printable area in paper is 576(dot)

				RasterDocument rasterDoc = new RasterDocument(RasSpeed.Medium, RasPageEndMode.FeedAndFullCut, RasPageEndMode.FeedAndFullCut, RasTopMargin.Standard, 0, 0, 0);
				if (rasterType == RasterCommand.Standard) {
					list.add(rasterDoc.BeginDocumentCommandData());
				}

				String textToPrint = (context.getResources().getString(R.string.title_company_name_raster_3inch_cht));
				list.add(createRasterCommand(textToPrint, 21, Typeface.BOLD, rasterType));

				list.add(createRasterCommand("-----------------------------------------------------------------------", 13, 0, rasterType));

				textToPrint = context.getResources().getString(R.string.title_receipt_name_raster_3inch_cht);
				list.add(createRasterCommand(textToPrint, 28, 0, rasterType));

				textToPrint = (context.getResources().getString(R.string.cht_103_raster_3inch_cht));
				list.add(createRasterCommand(textToPrint, 28, 0, rasterType));

				textToPrint = (context.getResources().getString(R.string.ev_99999999_raster_3inch_cht));
				list.add(createRasterCommand(textToPrint, 28, 0, rasterType));

				textToPrint = (context.getResources().getString(R.string.date_raster_3inch_cht));
				list.add(createRasterCommand(textToPrint, 13, 0, rasterType));

				textToPrint = (context.getResources().getString(R.string.random_code_raster_3inch_cht));
				list.add(createRasterCommand(textToPrint, 13, 0, rasterType));

				textToPrint = (context.getResources().getString(R.string.seller_raster_3inch_cht));
				list.add(createRasterCommand(textToPrint, 13, 0, rasterType));

				// BarCode
				Bitmap bm = BitmapFactory.decodeResource(res, R.drawable.code39);
				StarBitmap starbitmap = new StarBitmap(bm, false, 200);
				if (rasterType == RasterCommand.Standard) {
					list.add(starbitmap.getImageRasterDataForPrinting_Standard(true));
				} else {
					list.add(starbitmap.getImageRasterDataForPrinting_graphic(true));
				}

				// QRCode
				Bitmap bitmap = BitmapFactory.decodeResource(res, R.drawable.qrcode);
				StarBitmap starbitmap2 = new StarBitmap(bitmap, false, 146);
				if (rasterType == RasterCommand.Standard) {
					list.add(starbitmap2.getImageRasterDataForPrinting_Standard(true));
				} else {
					list.add(starbitmap2.getImageRasterDataForPrinting_graphic(true));
				}

				textToPrint = (context.getResources().getString(R.string.Item_list_raster_3inch_cht));
				list.add(createRasterCommand(textToPrint, 13, 0, rasterType));

				textToPrint = (context.getResources().getString(R.string.Item_list_Number_raster_3inch_cht));
				list.add(createRasterCommand(textToPrint, 13, 0, rasterType));

				textToPrint = (context.getResources().getString(R.string.Sales_schedules_raster_3inch_cht));
				list.add(createRasterCommand(textToPrint, 13, 0, rasterType));

				textToPrint = (context.getResources().getString(R.string.date_2_raster_3inch_cht));
				list.add(createRasterCommand(textToPrint, 13, 0, rasterType));

				textToPrint = (context.getResources().getString(R.string.ItemInfo_raster_3inch_cht));
				list.add(createRasterCommand(textToPrint, 13, 0, rasterType));

				textToPrint = (context.getResources().getString(R.string.sub_raster_3inch_cht));
				list.add(createRasterCommand(textToPrint, 13, Typeface.BOLD, rasterType));

				textToPrint = (context.getResources().getString(R.string.total_raster_3inch_cht));
				list.add(createRasterCommand(textToPrint, 13, Typeface.BOLD, rasterType));

				list.add(createRasterCommand("-----------------------------------------------------------------------", 13, 0, rasterType));

				textToPrint = (context.getResources().getString(R.string.cash_raster_3inch_cht));
				list.add(createRasterCommand(textToPrint, 13, 0, rasterType));

				textToPrint = (context.getResources().getString(R.string.change_raster_3inch_cht));
				list.add(createRasterCommand(textToPrint, 13, 0, rasterType));

				textToPrint = (context.getResources().getString(R.string.Invoice_raster_3inch_cht));
				list.add(createRasterCommand(textToPrint, 13, Typeface.BOLD, rasterType));

				textToPrint = (context.getResources().getString(R.string.date_3_raster_3inch_cht));
				list.add(createRasterCommand(textToPrint, 13, 0, rasterType));

				// BarCode
				Bitmap bimap3 = BitmapFactory.decodeResource(res, R.drawable.code39);
				StarBitmap starbitmap3 = new StarBitmap(bimap3, false, 200);
				if (rasterType == RasterCommand.Standard) {
					list.add(starbitmap3.getImageRasterDataForPrinting_Standard(true));
				} else {
					list.add(starbitmap3.getImageRasterDataForPrinting_graphic(true));
				}

				textToPrint = (context.getResources().getString(R.string.info_raster_3inch_cht));
				list.add(createRasterCommand(textToPrint, 13, 0, rasterType));

				textToPrint = (context.getResources().getString(R.string.info_number_raster_3inch_cht));
				list.add(createRasterCommand(textToPrint, 13, 0, rasterType));

				if (rasterType == RasterCommand.Standard) {
					list.add(rasterDoc.EndDocumentCommandData());
					list.add(new byte[] { 0x07 }); // Kick cash drawer
				} else {
					list.add(new byte[] { 0x1b, 0x64, 0x02 }); // Cut
				}

				sendCommand(context, portName, portSettings, list);
			} else if (strPrintArea.equals("4inch (112mm)")) {
				ArrayList<byte[]> list = new ArrayList<byte[]>();

				printableArea = 832; // Printable area in paper is 832(dot)

				RasterDocument rasterDoc = new RasterDocument(RasSpeed.Medium, RasPageEndMode.FeedAndFullCut, RasPageEndMode.FeedAndFullCut, RasTopMargin.Standard, 0, 0, 0);
				if (rasterType == RasterCommand.Standard) {
					list.add(rasterDoc.BeginDocumentCommandData());
				}

				String textToPrint = (context.getResources().getString(R.string.title_company_name_raster_4inch_cht));
				list.add(createRasterCommand(textToPrint, 21, Typeface.BOLD, rasterType));

				textToPrint = ("-----------------------------------------------------------------------------------------------------");
				list.add(createRasterCommand(textToPrint, 13, 0, rasterType));

				textToPrint = (context.getResources().getString(R.string.title_receipt_name_raster_4inch_cht));
				list.add(createRasterCommand(textToPrint, 28, 0, rasterType));

				textToPrint = (context.getResources().getString(R.string.cht_103_raster_4inch_cht));
				list.add(createRasterCommand(textToPrint, 28, 0, rasterType));

				textToPrint = (context.getResources().getString(R.string.ev_99999999_raster_4inch_cht));
				list.add(createRasterCommand(textToPrint, 28, 0, rasterType));

				textToPrint = (context.getResources().getString(R.string.date_raster_4inch_cht));
				list.add(createRasterCommand(textToPrint, 13, 0, rasterType));

				textToPrint = (context.getResources().getString(R.string.random_code_raster_4inch_cht));
				list.add(createRasterCommand(textToPrint, 13, 0, rasterType));

				textToPrint = (context.getResources().getString(R.string.seller_raster_4inch_cht));
				list.add(createRasterCommand(textToPrint, 13, 0, rasterType));

				// BarCode
				Bitmap bm = BitmapFactory.decodeResource(res, R.drawable.code39);
				StarBitmap starbitmap = new StarBitmap(bm, false, 200);
				if (rasterType == RasterCommand.Standard) {
					list.add(starbitmap.getImageRasterDataForPrinting_Standard(true));
				} else {
					list.add(starbitmap.getImageRasterDataForPrinting_graphic(true));
				}

				// QRCode
				Bitmap bitmap = BitmapFactory.decodeResource(res, R.drawable.qrcode);
				StarBitmap starbitmap2 = new StarBitmap(bitmap, false, 146);
				if (rasterType == RasterCommand.Standard) {
					list.add(starbitmap2.getImageRasterDataForPrinting_Standard(true));
				} else {
					list.add(starbitmap2.getImageRasterDataForPrinting_graphic(true));
				}

				textToPrint = (context.getResources().getString(R.string.Item_list_raster_4inch_cht));
				list.add(createRasterCommand(textToPrint, 13, 0, rasterType));

				textToPrint = (context.getResources().getString(R.string.Item_list_Number_raster_4inch_cht));
				list.add(createRasterCommand(textToPrint, 13, 0, rasterType));

				textToPrint = (context.getResources().getString(R.string.Sales_schedules_raster_4inch_cht));
				list.add(createRasterCommand(textToPrint, 13, 0, rasterType));

				textToPrint = (context.getResources().getString(R.string.date_2_raster_4inch_cht));
				list.add(createRasterCommand(textToPrint, 13, 0, rasterType));

				textToPrint = (context.getResources().getString(R.string.ItemInfo_raster_4inch_cht));
				list.add(createRasterCommand(textToPrint, 13, 0, rasterType));

				textToPrint = (context.getResources().getString(R.string.sub_raster_4inch_cht));
				list.add(createRasterCommand(textToPrint, 13, Typeface.BOLD, rasterType));

				textToPrint = (context.getResources().getString(R.string.total_raster_4inch_cht));
				list.add(createRasterCommand(textToPrint, 13, Typeface.BOLD, rasterType));

				textToPrint = ("-----------------------------------------------------------------------------------------------------");
				list.add(createRasterCommand(textToPrint, 13, 0, rasterType));

				textToPrint = (context.getResources().getString(R.string.cash_raster_4inch_cht));
				list.add(createRasterCommand(textToPrint, 13, 0, rasterType));

				textToPrint = (context.getResources().getString(R.string.change_raster_4inch_cht));
				list.add(createRasterCommand(textToPrint, 13, 0, rasterType));

				textToPrint = (context.getResources().getString(R.string.Invoice_raster_4inch_cht));
				list.add(createRasterCommand(textToPrint, 13, Typeface.BOLD, rasterType));

				textToPrint = (context.getResources().getString(R.string.date_3_raster_4inch_cht));
				list.add(createRasterCommand(textToPrint, 13, 0, rasterType));

				// BarCode
				Bitmap bimap3 = BitmapFactory.decodeResource(res, R.drawable.code39);
				StarBitmap starbitmap3 = new StarBitmap(bimap3, false, 200);
				if (rasterType == RasterCommand.Standard) {
					list.add(starbitmap3.getImageRasterDataForPrinting_Standard(true));
				} else {
					list.add(starbitmap3.getImageRasterDataForPrinting_graphic(true));
				}

				textToPrint = (context.getResources().getString(R.string.info_raster_4inch_cht));
				list.add(createRasterCommand(textToPrint, 13, 0, rasterType));

				textToPrint = (context.getResources().getString(R.string.info_number_raster_4inch_cht));
				list.add(createRasterCommand(textToPrint, 13, 0, rasterType));

				if (rasterType == RasterCommand.Standard) {
					list.add(rasterDoc.EndDocumentCommandData());
					list.add(new byte[] { 0x07 }); // Kick cash drawer
				} else {
					list.add(new byte[] { 0x1b, 0x64, 0x02 }); // Cut
				}

				sendCommand(context, portName, portSettings, list);
			}
		}
	}

	private static byte[] createShiftJIS(String inputText) {
		byte[] byteBuffer = null;

		try {
			byteBuffer = inputText.getBytes("Shift_JIS");
		} catch (UnsupportedEncodingException e) {
			byteBuffer = inputText.getBytes();
		}

		return byteBuffer;
	}

	private static byte[] createGB2312(String inputText) {
		byte[] byteBuffer = null;

		try {
			byteBuffer = inputText.getBytes("GB2312");
		} catch (UnsupportedEncodingException e) {
			byteBuffer = inputText.getBytes();
		}

		return byteBuffer;
	}

	private static byte[] createBIG5(String inputText) {
		byte[] byteBuffer = null;

		try {
			byteBuffer = inputText.getBytes("Big5");
		} catch (UnsupportedEncodingException e) {
			byteBuffer = inputText.getBytes();
		}

		return byteBuffer;
	}

	private static byte[] createCp1251(String inputText) {
		byte[] byteBuffer = null;
		
		try {
			byteBuffer = inputText.getBytes("Windows-1251");
		} catch (UnsupportedEncodingException e) {
			byteBuffer = inputText.getBytes();
		}
		
		return byteBuffer;
	}

	private static byte[] createCp1252(String inputText) {
		byte[] byteBuffer = null;
		
		try {
			byteBuffer = inputText.getBytes("Windows-1252");
		} catch (UnsupportedEncodingException e) {
			byteBuffer = inputText.getBytes();
		}
		
		return byteBuffer;
	}

	private static byte[] createRasterCommand(String printText, int textSize, int bold, RasterCommand rasterType) {
		byte[] command;

		Paint paint = new Paint();
		paint.setStyle(Paint.Style.FILL);
		paint.setColor(Color.BLACK);
		paint.setAntiAlias(true);

		Typeface typeface;

		try {
			typeface = Typeface.create(Typeface.SERIF, bold);
		} catch (Exception e) {
			typeface = Typeface.create(Typeface.DEFAULT, bold);
		}

		paint.setTypeface(typeface);
		paint.setTextSize(textSize * 2);
		paint.setLinearText(true);

		TextPaint textpaint = new TextPaint(paint);
		textpaint.setLinearText(true);
		StaticLayout staticLayout = new StaticLayout(printText, textpaint, printableArea, Layout.Alignment.ALIGN_NORMAL, 1, 0, false);
		int height = staticLayout.getHeight();

		Bitmap bitmap = Bitmap.createBitmap(staticLayout.getWidth(), height, Bitmap.Config.RGB_565);
		Canvas c = new Canvas(bitmap);
		c.drawColor(Color.WHITE);
		c.translate(0, 0);
		staticLayout.draw(c);

		StarBitmap starbitmap = new StarBitmap(bitmap, false, printableArea);

		if (rasterType == RasterCommand.Standard) {
			command = starbitmap.getImageRasterDataForPrinting_Standard(true);
		} else {
			command = starbitmap.getImageRasterDataForPrinting_graphic(true);
		}

		return command;
	}

	private static byte[] convertFromListByteArrayTobyteArray(List<byte[]> ByteArray) {
		int dataLength = 0;
		for (int i = 0; i < ByteArray.size(); i++) {
			dataLength += ByteArray.get(i).length;
		}

		int distPosition = 0;
		byte[] byteArray = new byte[dataLength];
		for (int i = 0; i < ByteArray.size(); i++) {
			System.arraycopy(ByteArray.get(i), 0, byteArray, distPosition, ByteArray.get(i).length);
			distPosition += ByteArray.get(i).length;
		}

		return byteArray;
	}

	private static void sendCommand(Context context, String portName, String portSettings, ArrayList<byte[]> byteList) {
		StarIOPort port = null;
		try {
			/*
			 * using StarIOPort3.1.jar (support USB Port) Android OS Version: upper 2.2
			 */
			port = StarIOPort.getPort(portName, portSettings, 10000, context);
			/*
			 * using StarIOPort.jar Android OS Version: under 2.1 port = StarIOPort.getPort(portName, portSettings, 10000);
			 */
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}

			/*
			 * Using Begin / End Checked Block method When sending large amounts of raster data, 
			 * adjust the value in the timeout in the "StarIOPort.getPort" in order to prevent 
			 * "timeout" of the "endCheckedBlock method" while a printing.
			 * 
			 * If receipt print is success but timeout error occurs(Show message which is "There 
			 * was no response of the printer within the timeout period." ), need to change value 
			 * of timeout more longer in "StarIOPort.getPort" method. 
			 * (e.g.) 10000 -> 30000
			 */
			StarPrinterStatus status = port.beginCheckedBlock();

			if (true == status.offline) {
				throw new StarIOPortException("A printer is offline");
			}

			byte[] commandToSendToPrinter = convertFromListByteArrayTobyteArray(byteList);
			port.writePort(commandToSendToPrinter, 0, commandToSendToPrinter.length);

			port.setEndCheckedBlockTimeoutMillis(30000);// Change the timeout time of endCheckedBlock method.
			status = port.endCheckedBlock();

			if (status.coverOpen == true) {
				throw new StarIOPortException("Printer cover is open");
			} else if (status.receiptPaperEmpty == true) {
				throw new StarIOPortException("Receipt paper is empty");
			} else if (status.offline == true) {
				throw new StarIOPortException("Printer is offline");
			}
		} catch (StarIOPortException e) {
			Builder dialog = new Builder(context);
			dialog.setNegativeButton("OK", null);
			AlertDialog alert = dialog.create();
			alert.setTitle("Failure");
			alert.setMessage(e.getMessage());
			alert.setCancelable(false);
			alert.show();
		} finally {
			if (port != null) {
				try {
					StarIOPort.releasePort(port);
				} catch (StarIOPortException e) {
				}
			}
		}
	}
}
