/*
 * MIT License
 *
 * Copyright 2021 Myndigheten för digital förvaltning (DIGG)
 */
package ch.admin.bag.covidcertificate.eval.utils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Implementation of Base45 encoding/decoding according to <a href=
 * "https://datatracker.ietf.org/doc/draft-faltstrom-base45/">https://datatracker.ietf.org/doc/draft-faltstrom-base45/</a>.
 *
 * @author Martin Lindström (martin@idsec.se)
 * @author Henrik Bengtsson (extern.henrik.bengtsson@digg.se)
 * @author Henric Norlander (extern.henric.norlander@digg.se)
 */
public class Base45 {

  /**
   * The Base45 Alphabet.
   *
   * <pre>
   * 00 0    12 C    24 O    36 Space
   * 01 1    13 D    25 P    37 $
   * 02 2    14 E    26 Q    38 %
   * 03 3    15 F    27 R    39 *
   * 04 4    16 G    28 S    40 +
   * 05 5    17 H    29 T    41 -
   * 06 6    18 I    30 U    42 .
   * 07 7    19 J    31 V    43 /
   * 08 8    20 K    32 W    44 :
   * 09 9    21 L    33 X
   * 10 A    22 M    34 Y
   * 11 B    23 N    35 Z
   * </pre>
   */
  public static final byte[] ALPHABET = {
      '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H',
      'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
      ' ', '$', '%', '*', '+', '-', '.', '/', ':'
  };

  // Hidden constructor
  private Base45() {
  }

  /**
   * Gets a Base45 encoder.
   *
   * @return a Base45 encoder
   */
  public static Encoder getEncoder() {
    return Encoder.ENCODER;
  }

  /**
   * Gets a Base45 decoder.
   *
   * @return a Base45 decoder
   */
  public static Decoder getDecoder() {
    return Decoder.DECODER;
  }

  /**
   * A Base45 encoder.
   */
  public static class Encoder {

    /** Static encoder instance. */
    public static final Encoder ENCODER = new Encoder();

    // Hidden constructor
    private Encoder() {
    }

    /**
     * Encodes the supplied bytes into its Base45 encoding.
     *
     * @param src
     *          the bytes to encode
     * @return an allocated byte array holding the Base45 encoding
     */
    public byte[] encode(final byte[] src) {
      final int length = src.length / 2 * 3 + src.length % 2 * 2;
      final byte[] result = new byte[length];
      int ip = 0;
      int op = 0;
      while (ip < src.length) {
        final int i0 = src[ip++] & 0xff;
        final int i1 = ip < src.length ? src[ip] & 0xff : 0;
        final int out = ip < src.length ? i0 * 256 + i1 : i0;
        final int o0 = out % 45;
        final int o1 = out / 45 % 45;
        final int o2 = out / 45 / 45;

        result[op++] = ALPHABET[o0];
        result[op] = op < length ? ALPHABET[o1] : ALPHABET[o2];
        op++;
        if (op < length) {
          result[op++] = ALPHABET[o2];
        }
        ip++;
      }
      return result;
    }

    /**
     * Encodes the supplied bytes into its corresponding Base45 string.
     *
     * @param src
     *          the bytes to encode
     * @return a Base45 string
     */
    public String encodeToString(final byte src[]) {
      return new String(this.encode(src), StandardCharsets.US_ASCII);
    }

  }

  /**
   * A Base45 decoder.
   */
  public static class Decoder {

    /** Static decoder instance. */
    public static final Decoder DECODER = new Decoder();

    /** Table for decoding characters from the Base45 alphabet. */
    private static final int[] DECODING_TABLE = new int[256];

    static {
      // Initialize the decoding table.
      Arrays.fill(DECODING_TABLE, -1);
      for (int i = 0; i < ALPHABET.length; i++) {
        DECODING_TABLE[ALPHABET[i]] = i;
      }
    }

    // Hidden constructor
    private Decoder() {
    }

    /**
     * Decodes the supplied input (which is the byte array representation of a Base45 string).
     *
     * @param src
     *          the Base45 string to decode
     * @return an allocated byte array
     */
    public byte[] decode(final byte[] src) {
      final int resultLength = src.length / 3 * 2 + src.length % 3 / 2;
      final byte[] result = new byte[resultLength];
      int ip = 0;
      int op = 0;
      while (ip < src.length) {
        final int i0 = src[ip++];
        final int i1 = src[ip++];
        final int i2 = ip < src.length ? src[ip] : 0;
        if (i0 > 127 || i1 > 127 || i2 > 127) {
          throw new IllegalArgumentException("Illegal character in Base45 encoded data.");
        }
        final int b0 = DECODING_TABLE[i0];
        final int b1 = ip <= src.length ? DECODING_TABLE[i1] : 0;
        final int b2 = ip < src.length ? DECODING_TABLE[i2] : 0;
        if (b0 < 0 || b1 < 0 || b2 < 0) {
          throw new IllegalArgumentException("Illegal character in Base45 encoded data.");
        }
        final int value = b0 + 45 * b1 + 45 * 45 * b2;
        final int o0 = value / 256;
        final int o1 = value % 256;
        result[op++] = op < resultLength ? (byte) o0 : (byte) o1;
        if (op < resultLength) {
          result[op++] = (byte) o1;
        }
        ip++;
      }
      return result;
    }

    /**
     * Decodes the supplied Base45 string.
     *
     * @param src
     *          the Base45 string to decode
     * @return an allocated byte array
     */
    public byte[] decode(final String src) {
      return this.decode(src.getBytes(StandardCharsets.US_ASCII));
    }

  }

}
