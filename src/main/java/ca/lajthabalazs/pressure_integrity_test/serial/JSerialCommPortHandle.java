package ca.lajthabalazs.pressure_integrity_test.serial;

import com.fazecast.jSerialComm.SerialPort;
import java.io.InputStream;
import java.io.OutputStream;

/** Wraps a jSerialComm {@link SerialPort} as a {@link SerialPortHandle}. */
public final class JSerialCommPortHandle implements SerialPortHandle {

  private final SerialPort port;

  public JSerialCommPortHandle(SerialPort port) {
    this.port = port;
  }

  @Override
  public boolean setComPortParameters(int baudRate, int dataBits, int stopBits, int parity) {
    return port.setComPortParameters(baudRate, dataBits, stopBits, parity);
  }

  @Override
  public boolean openPort() {
    return port.openPort();
  }

  @Override
  public boolean closePort() {
    return port.closePort();
  }

  @Override
  public boolean isOpen() {
    return port.isOpen();
  }

  @Override
  public String getSystemPortName() {
    return port.getSystemPortName();
  }

  @Override
  public InputStream getInputStream() {
    return port.getInputStream();
  }

  @Override
  public OutputStream getOutputStream() {
    return port.getOutputStream();
  }
}
