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

public class CameraCanvas extends Canvas {
	private QRMidlet mQRMidlet;
	
	public CameraCanvas(QRMidlet midlet) {
		mQRMidlet = midlet;
	}
	
	public void paint(Graphics g) {
            // clear screen with black background
            g.setColor( 0x0000000 );
            g.fillRect(0, 0, this.getWidth(), this.getHeight());
        };

	public void keyPressed(int keyCode) {
		int action = getGameAction(keyCode);
		if (action == FIRE) {
			mQRMidlet.capture();
		}
	}
}