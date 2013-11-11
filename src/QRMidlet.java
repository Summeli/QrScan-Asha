/*
 * Copyright (c) 2005 - 2008 Christoph Wimmer
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

import javax.microedition.amms.control.camera.FlashControl;
import javax.microedition.amms.control.camera.FocusControl;
import javax.microedition.content.Invocation;
import javax.microedition.content.Registry;
import javax.microedition.io.*;
import javax.microedition.lcdui.*;
import javax.microedition.media.*;
import javax.microedition.media.control.*;
import javax.microedition.midlet.MIDlet;
import javax.microedition.rms.*;

public class QRMidlet extends MIDlet implements CommandListener {
    // Midlet name
	private String midletName = "QrScan";
	private String version = "1.0.0";
	
	// UI Elements
	private Display display;
	private Form resultForm;
	private Form waitForm;
    private Form settingsForm;
    private Form messageForm;
	private Command exitCommand;
	private Command cameraCommand;
	private Command cancelCommand;
	private Command callCommand;
	private Command openCommand;
	private Command smsCommand;
	private Command shareCommand;
	private Command instructionsCommand;
	private Command aboutCommand;
	private StringItem resultStringItem;
	private ChoiceGroup resolutionChoiceGroup;
        private Gauge waitGauge;
	private Player player;
	private VideoControl videoControl;
	private FocusControl focusControl;
	FlashControl flashControl;
    
	private Canvas canvas;
	// Settings & initialization with default values
    private int captureProfile = 0;
	// Settings record store stuff
	private RecordStore db = null;
    // Midlet started or resumed, initiallize to not started
	private boolean started = false;
    private Thread decodeThread = null;
    private DecodeThread decoder = null;
    
    //sharing stuff
    private Registry registry = null;
    private String result = null;
    
	public QRMidlet() {
            // nothing to do here
	}

	public void startApp() {
            // initialize one-time resources on startup
            if (started == false) {
                started = true;
                // get display handle
		display = Display.getDisplay(this);
		// load settings
		loadSettings();
                
                // initialize commands
		exitCommand = new Command("Exit", Command.EXIT, 1);
		cameraCommand = new Command("Camera", Command.SCREEN, 2);
		cancelCommand = new Command("Cancel", Command.CANCEL, 2);
		callCommand = new Command("Call", Command.ITEM, 1);
		openCommand = new Command("Open Link", Command.ITEM, 1);
        smsCommand = new Command("Send SMS", Command.ITEM, 1);
        shareCommand =  new Command("Share", Command.ITEM, 3);
        instructionsCommand =  new Command("Instructions", Command.ITEM, 1);
        aboutCommand =  new Command("About", Command.ITEM, 2);
		// initialize settings form
		settingsForm = new Form(midletName);
		String[] resolutions = {"Automatic", "160x120", "320x240", "640x480"};
		resolutionChoiceGroup = new ChoiceGroup("Resolution: ", Choice.EXCLUSIVE, resolutions, null);
		settingsForm.append(resolutionChoiceGroup);
		settingsForm.addCommand(cancelCommand);
		settingsForm.setCommandListener(this);
		// initialize waiting form
                waitForm = new Form(midletName);
                waitGauge = new Gauge("Processing, please wait...", false, Gauge.INDEFINITE, Gauge.CONTINUOUS_RUNNING);
		waitForm.append(waitGauge);
                
        // initialize player, videocontrol, canvas
        try {
            // create player and realize it
            player = createPlayer();
            player.realize();
            
            // get videocontrol
            videoControl = (VideoControl)player.getControl("VideoControl");
            // initialize camera canvas
            canvas = new CameraCanvas(this);
            // initialize video control
            videoControl.initDisplayMode(VideoControl.USE_DIRECT_VIDEO, canvas);
            focusControl = (FocusControl)player.getControl("javax.microedition.amms.control.camera.FocusContro");
            if(focusControl != null)
            	focusControl.setMacro(true);
            flashControl = 	(FlashControl) player.getControl("javax.microedition.amms.control.camera.FlashControl");
            if(flashControl != null)
            	flashControl.setMode(FlashControl.FORCE);
            videoControl.setDisplayFullScreen(true);
            videoControl.setVisible(true);
        } catch (Exception e) {
            handleException(e);
        }
        canvas.addCommand(instructionsCommand);
        canvas.addCommand(aboutCommand);
        canvas.setCommandListener(this);
        showCamera();
            } else {
                // when resuming from paused state
		
            	if (display.getCurrent() == canvas) {
                    try {
                        player.start();
                    } catch (Exception e) {
                        handleException(e);
                    }
		
            	}
            	display.setCurrent(display.getCurrent());
            }
            capture();
	}
	
	public void continueViewFinder(){
         try {
			videoControl.setDisplayFullScreen(true);
		} catch (MediaException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
         videoControl.setVisible(true);
	}
	public void pauseApp() {
            if (player != null) {
                try {
                    player.stop();
                } catch (Exception e) {
                    handleException(e);
                }
            }
        }

	public void destroyApp(boolean unconditional) {
            if (player != null) {
                videoControl = null;
                try {
                    player.stop();
                } catch (Exception e) {
                    handleException(e);
                }
                player.deallocate();
                player.close();
                player = null;
            }
        }

	public void commandAction(Command c, Displayable s) {
		//handle result form
		if(s == resultForm){
			if(c.getCommandType() == Command.BACK){
				showCamera();
			}else if( c == shareCommand){
				startShare();
			}else if (c == cameraCommand) {
				showCamera();
			}else if (c == callCommand) {
				dispatchPlatformRequest();
			} else if (c == openCommand) {
				dispatchPlatformRequest();
			} else if (c == smsCommand) {
				dispatchPlatformRequest();
			}
		}else if( s == messageForm){
			if(c.getCommandType() == Command.BACK){
				showCamera();
			}
		}else{
			if (c.getCommandType() == Command.EXIT) {
				destroyApp(true);
				notifyDestroyed();
			} else if (c == cancelCommand) {
				showCamera();
			}else if (c == instructionsCommand) {
				showInstructionsDialog();
			}else if (c == aboutCommand) {
				showAboutDialog();
			}
		}
	}
        
    private Player createPlayer() {
        Player mPlayer = null;
        // try capture://image first for series 40 phones
        try {
            mPlayer = Manager.createPlayer("capture://image");
            
        } catch (Exception e) {
            handleException(e);
        } catch (Error e) {
            // continue
        }
        // if capture://image failed, try capture://video
        if (mPlayer == null) {
            try {
                mPlayer = Manager.createPlayer("capture://video");
            } catch (Exception e) {
                handleException(e);
            }
        }
        return mPlayer;
    }

	private void showCamera() {
		try {
			display.setCurrent(canvas);
			player.start();
		} catch (Exception e) {
			handleException(e);
		}
	}
	
	public void closeCamera() {
            try {
                player.stop();
            } catch (Exception e) {
                handleException(e);
            }
	}

	public void capture() {
		display.setCurrent(canvas);
		try {
			// display waiting screen during decoding
			//display.setCurrent(waitForm);
			// capture and decode image in its own thread
			if(decodeThread == null){
				decoder = new DecodeThread(this,getCaptureSettings());
				decodeThread = new Thread(decoder);
				decodeThread.start();
			}
		} catch (Exception me) {
			handleException(me);
		}
	}
	
	public void showResult(String result) {
		this.result = result;
		// initialize result form
		resultForm = new Form(midletName);
		resultStringItem = new StringItem("Result: ", result);
		//resultForm.addCommand(exitCommand);
		// do some simple result parsing
		if (result.startsWith("tel:") || result.startsWith("TEL:")) {
			resultForm.addCommand(callCommand);
		} else if (result.startsWith("http://")) {
			resultForm.addCommand(openCommand);
		} else if (result.startsWith("sms:") || result.startsWith("SMS:")) {
			resultForm.addCommand(smsCommand);
		}
		resultForm.addCommand(new Command("Back", Command.BACK, 0));
		resultForm.addCommand(cameraCommand);
		resultForm.addCommand(shareCommand);
		resultForm.append(resultStringItem);
		resultForm.setCommandListener(this);
		// display result form.
		display.setCurrent(resultForm);
	}
        
	private void dispatchPlatformRequest() {
		try {
			platformRequest(resultStringItem.getText());
		} catch (ConnectionNotFoundException cnfe) {
			handleException(cnfe);
		}
		destroyApp(true);
		notifyDestroyed();
	}

        public String getCaptureSettings() {
        	//return "encoding=png&width=480&height=640";
        	return "encoding=png&width=960&height=1280";
        }
        
	private void loadSettings() {
		try {
			db = RecordStore.openRecordStore("settings", true);
			if (db.getNumRecords() > 0) {
				byte[] record = db.getRecord(1);
				String cp = new String(record);
                                captureProfile = Integer.parseInt(cp);
			} else {
				captureProfile = 0;
			}
			db.closeRecordStore();
		} catch (RecordStoreException rse) {
			handleException(rse);
		}
	}

	private void saveSettings() {
		int choice = resolutionChoiceGroup.getSelectedIndex();
		boolean settingsChanged = false;
		if ((choice == 0) && (captureProfile != 0)) {
			captureProfile = 0;
			settingsChanged = true;
		} else if ((choice == 1) && (captureProfile != 1)) {
			captureProfile = 1;
			settingsChanged = true;
		} else if ((choice == 2) && (captureProfile != 2)) {
			captureProfile = 2;
			settingsChanged = true;
		} else if ((choice == 3) && (captureProfile != 3)) {
			captureProfile = 3;
			settingsChanged = true;
		}
		if (settingsChanged == true) {
			try {
				db = RecordStore.openRecordStore("settings", true);
                                String cp = Integer.toString(captureProfile);
				byte[] record = (cp).getBytes();
				if (db.getNumRecords() == 0) {
					db.addRecord(record, 0, record.length);
				} else {
					db.setRecord(1, record, 0, record.length);
				}
				db.closeRecordStore();
			} catch (RecordStoreException rse) {
				handleException(rse);
			}
		}
	}
	
	private void showSettings() {
		resolutionChoiceGroup.setSelectedIndex(captureProfile, true);
		display.setCurrent(settingsForm);
	}
        
    public VideoControl getVideoControl() {
        return videoControl;
    }
        
	public void handleException(Exception e) {
		System.err.println(e);
	}
	
	private void startShare(){
	   	registry = Registry.getRegistry(this.getClass().getName());
    	Invocation invocation = new Invocation(null, "text/plain", "com.nokia.share");
    	invocation.setAction("share");
        String[] args = new String[1]; // Only the first element is required and used
        if (result.startsWith("http://"))
        	args[0] = new String("url="+result); // Content to share
        else
        	args[0] = new String("text="+result);
    	invocation.setArgs(args);
    	invocation.setResponseRequired(false);
    	try {
			registry.invoke(invocation);
		} catch (Exception e) {
		}
	}
	
	private void showInstructionsDialog(){
		showMessage("Instructions", "Point the camera into QrCode to scan it \n" +
				"Try to get the image as sharp as possible by moving the phone into optimal distance for the best results \n" +
				"If you get a valid looking HTTP link, then you can open it in the web browser by pressing open link");
	}

	private void showAboutDialog(){		
		showMessage(midletName, midletName + " " + version + " for S40 and Nokia Aha \n" +
                "by: Antti Pohjola, summeli@summeli.fi \nhttp://www.summeli.fi\n"+
                "QrScan is licenced under GPLv2 licence \n" +
                "You can get the source code from: http://github.com/Summeli/QrScan-Asha \n" +
                "the icons is derived from the work of  P.J. Onori https://github.com/somerandomdude \n");
	}
	
	private void showMessage(String title, String message) {
		messageForm = new Form(title);
		messageForm.append(message);
		messageForm.setCommandListener(this);
		messageForm.addCommand(new Command("Back", Command.BACK, 0));
		display.setCurrent(messageForm);
	}
}
