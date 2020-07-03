package org.apache.hadoop.security;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.classification.InterfaceStability;

/** Authentication method */
@InterfaceStability.Evolving
public enum AuthMethod {
  SIMPLE((byte) 80, ""),
  KERBEROS((byte) 81, "GSSAPI"),
  @Deprecated
  DIGEST((byte) 82, "DIGEST-MD5"),
  TOKEN((byte) 82, "DIGEST-MD5"),
  PLAIN((byte) 83, "PLAIN");

  /** The code for this method. */
  public final byte code;
  public final String mechanismName;

  private AuthMethod(byte code, String mechanismName) {
    this.code = code;
    this.mechanismName = mechanismName;
  }

  private static final int FIRST_CODE = values()[0].code;

  /** Return the object represented by the code. */
  private static AuthMethod valueOf(byte code) {
    final int i = (code & 0xff) - FIRST_CODE;
    return i < 0 || i >= values().length ? null : values()[i];
  }

  /** Return the SASL mechanism name */
  public String getMechanismName() {
    return mechanismName;
  }

  /** Read from in */
  public static AuthMethod read(DataInput in) throws IOException {
    return valueOf(in.readByte());
  }

  /** Write to out */
  public void write(DataOutput out) throws IOException {
    out.write(code);
  }
};
