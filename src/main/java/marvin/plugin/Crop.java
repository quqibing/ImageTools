package marvin.plugin;

import marvin.image.MarvinImage;

public class Crop {

	public static void crop(MarvinImage imageIn, MarvinImage imageOut, int x, int y, int width, int height) {

		imageOut.setDimension(width, height);

		for (int i = x; i < x + width; i++)
			for (int j = y; j < y + height; j++)
				imageOut.setIntColor(i - x, j - y, imageIn.getIntColor(i, j));
	}

}
