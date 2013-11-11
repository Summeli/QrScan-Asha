import javax.microedition.lcdui.Image;
import jp.sourceforge.qrcode.data.QRCodeImage;

public class J2MEImage implements QRCodeImage {
	Image image;
	int[] intImage;
        
	public J2MEImage(Image image) {
		this.image = image;
		intImage = new int[image.getWidth()*image.getHeight()];
		image.getRGB(this.intImage, 0, image.getWidth(), 0, 0, image.getWidth(), image.getHeight());
	}
        
	public int getHeight() {
		return image.getHeight();
	}
        
	public int getWidth() {
		return image.getWidth();
	}
        
	public int getPixel(int x, int y) {
		return intImage[x + y*image.getWidth()];
	}
}