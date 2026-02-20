package ca.lajthabalazs.pressure_integrity_test.serial;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A {@link SerialPortHandle} that does not connect to hardware. Accepts parameter changes and
 * open/close; reads return EOF, writes are discarded. Used for UI testing and demo mode.
 */
public final class DemoPortHandle implements SerialPortHandle {

  private static final String DEMO_PORT_NAME = "DemoPort";

  private final AtomicBoolean open = new AtomicBoolean(false);

  @Override
  public boolean setComPortParameters(int baudRate, int dataBits, int stopBits, int parity) {
    return true;
  }

  @Override
  public boolean openPort() {
    open.set(true);
    return true;
  }

  @Override
  public boolean closePort() {
    open.set(false);
    return true;
  }

  @Override
  public boolean isOpen() {
    return open.get();
  }

  @Override
  public String getSystemPortName() {
    return DEMO_PORT_NAME;
  }

  @Override
  public InputStream getInputStream() {
    return new ByteArrayInputStream(new byte[0]);
  }

  @Override
  public OutputStream getOutputStream() {
    return new OutputStream() {
      @Override
      public void write(int b) throws IOException {
        // discard
      }
    };
  }
}
