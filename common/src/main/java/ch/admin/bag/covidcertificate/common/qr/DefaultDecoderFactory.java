package ch.admin.bag.covidcertificate.common.qr;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.journeyapps.barcodescanner.Decoder;
import com.journeyapps.barcodescanner.DecoderFactory;
import com.journeyapps.barcodescanner.InvertedDecoder;
import com.journeyapps.barcodescanner.MixedDecoder;

public class DefaultDecoderFactory implements DecoderFactory {
	private Collection<BarcodeFormat> decodeFormats;
	private Map<DecodeHintType, ?> hints;
	private String characterSet;
	private int scanType;

	public DefaultDecoderFactory() {
	}


	public DefaultDecoderFactory(Collection<BarcodeFormat> decodeFormats) {
		this.decodeFormats = decodeFormats;
	}

	public DefaultDecoderFactory(Collection<BarcodeFormat> decodeFormats, Map<DecodeHintType, ?> hints, String characterSet,
			int scanType) {
		this.decodeFormats = decodeFormats;
		this.hints = hints;
		this.characterSet = characterSet;
		this.scanType = scanType;
	}

	@Override
	public Decoder createDecoder(Map<DecodeHintType, ?> baseHints) {
		Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);

		hints.putAll(baseHints);

		if (this.hints != null) {
			hints.putAll(this.hints);
		}

		if (this.decodeFormats != null) {
			hints.put(DecodeHintType.POSSIBLE_FORMATS, decodeFormats);
		}

		if (characterSet != null) {
			hints.put(DecodeHintType.CHARACTER_SET, characterSet);
		}

		MultiFormatReader reader = new MultiFormatReader();
		reader.setHints(hints);

		switch (scanType) {
			case 0:
				return new Decoder(reader);
			case 1:
				return new InvertedDecoder(reader);
			case 2:
				return new MixedDecoder(reader);
			default:
				return new Decoder(reader);
		}
	}

}

