package ca.lajthabalazs.pressure_integrity_test.serial;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A {@link SerialPortHandle} that does not connect to hardware. Echoes whatever is written to
 * {@link #getOutputStream()} back on {@link #getInputStream()} as "Received: " + sent message.
 */
public final class DemoPortHandle implements SerialPortHandle {

  private static final String DEMO_PORT_NAME = "DemoPort";
  private static final String ECHO_PREFIX = "Received: ";

  private final AtomicBoolean open = new AtomicBoolean(false);
  private PipedInputStream pipeIn;
  private PipedOutputStream pipeOut;

  @Override
  public boolean setComPortParameters(int baudRate, int dataBits, int stopBits, int parity) {
    return true;
  }

  @Override
  public boolean openPort() {
    if (open.getAndSet(true)) {
      return true;
    }
    try {
      pipeIn = new PipedInputStream();
      pipeOut = new PipedOutputStream(pipeIn);
      return true;
    } catch (IOException e) {
      open.set(false);
      return false;
    }
  }

  @Override
  public boolean closePort() {
    if (!open.getAndSet(false)) {
      return true;
    }
    try {
      if (pipeOut != null) pipeOut.close();
    } catch (IOException ignored) {
    }
    try {
      if (pipeIn != null) pipeIn.close();
    } catch (IOException ignored) {
    }
    pipeOut = null;
    pipeIn = null;
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
    return pipeIn != null
        ? pipeIn
        : new InputStream() {
          @Override
          public int read() {
            return -1;
          }
        };
  }

  @Override
  public OutputStream getOutputStream() {
    if (pipeOut == null) {
      return new OutputStream() {
        @Override
        public void write(int b) {}
      };
    }
    return new OutputStream() {
      @Override
      public void write(int b) throws IOException {
        write(new byte[] {(byte) b});
      }

      @Override
      public void write(byte[] b, int off, int len) throws IOException {
        if (b == null || len <= 0) return;
        String sent = new String(b, off, len, StandardCharsets.UTF_8);
        byte[] echoed = (ECHO_PREFIX + sent).getBytes(StandardCharsets.UTF_8);
        pipeOut.write(echoed);
        pipeOut.flush();
      }
    };
  }
}
