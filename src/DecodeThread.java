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

import javax.microedition.lcdui.*;
import javax.microedition.media.MediaException;
import javax.microedition.media.control.VideoControl;
import jp.sourceforge.qrcode.QRCodeDecoder;

class DecodeThread implements Runnable {
	private QRMidlet mQRMidlet;
        private VideoControl videoControl;
        private boolean running = false;
        private String captureString;
	DecodeThread(QRMidlet midlet, String captureString) {
		mQRMidlet = midlet;
		this.captureString = captureString;
		
	}

	public void run() {
		running = true;
		while(running)
		{
            try {
                    String result = null;
                    // get capture settings
                    // try to capture image
                    byte[] raw = captureSnapshot(captureString);
           //         mQRMidlet.closeCamera();
                    // if image capture was successful, start processing
                    if (raw != null) {
                        Image image = Image.createImage(raw, 0, raw.length);
                       // Image image = Image.createImage("/qr.png");
                        // initialize decoder and decode image matrix
                        QRCodeDecoder decoder = new QRCodeDecoder();
                        try {
                            result = new String(decoder.decode(new J2MEImage(image)));
                            
                        } catch (Exception e) {
                            // continue
                        }
                    } else {
                        //result = "Error: Could not capture image.";
                    }
                    if(result!=null && result.length() > 0){
                    	mQRMidlet.closeCamera();
                    	mQRMidlet.showResult(result);
                    }
                    mQRMidlet.continueViewFinder();
                    Thread.sleep(750);
                } catch (Exception me) {
                	mQRMidlet.handleException(me);
                }
		}
	}
	public void stop(){
		running = false;
	}
    
    public byte[] captureSnapshot(String captureString) {
        byte[] raw = null;
        videoControl = mQRMidlet.getVideoControl();
      
        
        // if there are specific capture settings, try them first
      
        if (captureString != null) {
            try {
                raw = videoControl.getSnapshot(captureString);
            } catch (Exception e) {
                // continue
            }
        }
        // if there are no settings or no image has been captured
        // fall back to automatic settings
        if (raw == null) {
            try {
                raw = videoControl.getSnapshot(null);
            } catch (Exception e) {
                // continue
            }
        }
        // if automatic settings fail, try to enforce jpeg encoding
        if (raw == null) {
            try {
                raw = videoControl.getSnapshot("encoding=jpeg");
            } catch (Exception e) {
                // continue
            }
        }            
        
        return raw;
    }
}