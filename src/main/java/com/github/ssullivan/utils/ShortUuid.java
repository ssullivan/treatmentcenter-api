package com.github.ssullivan.utils;

import io.github.novacrypto.base58.Base58;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShortUuid {

  public static final Pattern ShortUuidValidator = Pattern
      .compile("^[123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz]{1,128}$");
  private static final Logger LOGGER = LoggerFactory.getLogger(ShortUuid.class);
  private static final BigInteger MIN_128_INV = BigInteger.ONE.shiftLeft(127);
  private static final char[] BASE_58_ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
      .toCharArray();
  private static final BigInteger TargetBase = BigInteger.valueOf(BASE_58_ALPHABET.length);

  public static boolean isValid(final String encoded) {
    return ShortUuidValidator.matcher(encoded).matches();
  }

  public static String randomShortUuid() {
    return encode(UUID.randomUUID());
  }

  public static String encode(final UUID uuid) {
    byte[] uuidBytes = asBytes(uuid);
    return Base58.base58Encode(uuidBytes);
  }

  public static UUID decode(final String encoded) {
    byte[] buf = Base58.base58Decode(encoded);
    return toUUID(buf);
  }

  public static UUID _decode(final String encoded) {
    byte[] original = decode(encoded.toCharArray()).toByteArray();
    byte[] padded = new byte[16];
    int delta = 16 - original.length;
    byte zero = 0x00;
    for (int i = 0; i < delta; ++i) {
      padded[i] = 0;
    }

    for (int i = 0; i < original.length; ++i) {
      padded[i + delta] = original[i];
    }

    return toUUID(padded);
  }

  public static byte[] asBytes(final UUID uuid) {
    long lsb = uuid.getLeastSignificantBits();
    long msb = uuid.getMostSignificantBits();
    final byte[] bytes = new byte[16];
    bytes[7] = (byte) (msb >> 1 * 8 & 0xFF);
    bytes[6] = (byte) (msb >> 0 * 8 & 0xFF);
    bytes[5] = (byte) (msb >> 3 * 8 & 0xFF);
    bytes[4] = (byte) (msb >> 2 * 8 & 0xFF);
    bytes[3] = (byte) (msb >> 7 * 8 & 0xFF);
    bytes[2] = (byte) (msb >> 6 * 8 & 0xFF);
    bytes[1] = (byte) (msb >> 5 * 8 & 0xFF);
    bytes[0] = (byte) (msb >> 4 * 8 & 0xFF);
    for (int i = 0; i < 8; i++) {
      bytes[15 - i] = (byte) (lsb & 0xFF);
      lsb = lsb >> 8;
    }
    return bytes;
  }

  private static String encode(final BigInteger bigInt) {
    BigInteger value = bigInt.add(ShortUuid.MIN_128_INV);
    final StringBuilder stringBuilder = new StringBuilder();
    do {
      final BigInteger[] fracAndRemainder = value.divideAndRemainder(TargetBase);
      stringBuilder.append(BASE_58_ALPHABET[fracAndRemainder[1].intValue()]);
      value = fracAndRemainder[0];
    }
    while (value.compareTo(BigInteger.ZERO) > 0);
    return stringBuilder.toString();
  }

  private static BigInteger decode(final char[] encoded) {
    BigInteger sum = BigInteger.ZERO;
    final int charLen = encoded.length;
    for (int i = 0; i < charLen; ++i) {
      sum = sum.add(TargetBase.pow(i)
          .multiply(BigInteger.valueOf(Arrays.binarySearch(BASE_58_ALPHABET, encoded[i]))));
    }
    return sum.subtract(ShortUuid.MIN_128_INV);
  }

  private static UUID toUUID(final byte[] data) {
    assert data.length == 16 : "data must be 16 bytes in length";
    try {

      long lsb = 0;
      long msb = 0;
      long highInt = 0;
      long midShort = 0;
      long lowShort = 0;
      for (int i = 3; i >= 0; i--) {
        highInt = highInt << 8 | data[i] & 0xff;
      }
      for (int i = 5; i >= 4; i--) {
        midShort = midShort << 8 | data[i] & 0xff;
      }
      for (int i = 7; i >= 6; i--) {
        lowShort = lowShort << 8 | data[i] & 0xff;
      }
      msb = highInt << 32 | midShort << 16 | lowShort;
      for (int i = 8; i < 16; i++) {
        lsb = lsb << 8 | data[i] & 0xff;
      }
      return new UUID(msb, lsb);
    } catch (ArrayIndexOutOfBoundsException e) {
      LOGGER.error("Failed to convert toUUID {}", data, e);
      throw e;
    }
  }

}
