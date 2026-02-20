package ca.lajthabalazs.pressure_integrity_test.serial;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Abstraction over a serial port (real or demo). Allows configuring, opening, closing, and
 * accessing I/O streams. Parameter constants (parity, stop bits) are the same as jSerialComm's
 * {@link com.fazecast.jSerialComm.SerialPort} for compatibility.
 */
public interface SerialPortHandle {

  /**
   * Sets serial port parameters. Must be called before {@link #openPort()} when opening a real
   * port.
   *
   * @param baudRate baud rate (e.g. 9600)
   * @param dataBits data bits (e.g. 7, 8)
   * @param stopBits stop bits (e.g. SerialPort.ONE_STOP_BIT, SerialPort.TWO_STOP_BITS)
   * @param parity parity (e.g. SerialPort.NO_PARITY, SerialPort.EVEN_PARITY)
   * @return true if parameters were set successfully
   */
  boolean setComPortParameters(int baudRate, int dataBits, int stopBits, int parity);

  /**
   * Opens the port. After a successful open, {@link #isOpen()} returns true and I/O streams are
   * available.
   *
   * @return true if the port was opened
   */
  boolean openPort();

  /**
   * Closes the port and releases resources.
   *
   * @return true if the port was closed
   */
  boolean closePort();

  /** Returns true if the port is currently open. */
  boolean isOpen();

  /** System port name (e.g. "COM3", "DemoPort"). */
  String getSystemPortName();

  /**
   * Input stream for reading from the port. Only valid while the port is open. Caller must not
   * close the stream; closing the port handles cleanup.
   */
  InputStream getInputStream();

  /**
   * Output stream for writing to the port. Only valid while the port is open. Caller must not close
   * the stream; closing the port handles cleanup.
   */
  OutputStream getOutputStream();
}
