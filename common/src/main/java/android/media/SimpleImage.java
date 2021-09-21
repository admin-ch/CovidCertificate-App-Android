package android.media;

import java.nio.ByteBuffer;

public class SimpleImage extends android.media.Image {

	int format;
	int width;
	int height;
	long timestamp;
	SimplePlane[] planes;

	public SimpleImage(Image image) {
		super();
		this.format = image.getFormat();
		this.width = image.getWidth();
		this.height = image.getHeight();
		this.timestamp = image.getTimestamp();
		this.planes = new SimplePlane[image.getPlanes().length];
		for (int i = 0; i < image.getPlanes().length; i++) {
			Plane plane = image.getPlanes()[i];
			byte[] arr = new byte[plane.getBuffer().remaining()];
			plane.getBuffer().get(arr);
			this.planes[i] = new SimplePlane(plane.getRowStride(), plane.getPixelStride(), arr);
		}
	}

	@Override
	public int getFormat() {
		return format;
	}

	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public int getHeight() {
		return height;
	}

	@Override
	public long getTimestamp() {
		return timestamp;
	}

	@Override
	public Plane[] getPlanes() {
		return planes;
	}

	@Override
	public void close() {

	}

	public class SimplePlane extends Plane {

		int rowStride;
		int pixelStride;
		byte[] bytes;

		private SimplePlane(int rowStride, int pixelStride, byte[] bytes) {
			this.rowStride = rowStride;
			this.pixelStride = pixelStride;
			this.bytes = bytes;
		}

		@Override
		public int getRowStride() {
			return rowStride;
		}

		@Override
		public int getPixelStride() {
			return pixelStride;
		}

		@Override
		public ByteBuffer getBuffer() {
			return ByteBuffer.wrap(bytes);
		}

	}

}
