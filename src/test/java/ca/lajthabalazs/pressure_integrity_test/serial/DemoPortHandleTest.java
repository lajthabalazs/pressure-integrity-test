package ca.lajthabalazs.pressure_integrity_test.serial;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class DemoPortHandleTest {

  @Test
  void setComPortParameters_returnsTrue() {
    DemoPortHandle handle = new DemoPortHandle();
    assertTrue(handle.setComPortParameters(9600, 8, 1, 0));
  }

  @Test
  void getSystemPortName_returnsDemoPort() {
    DemoPortHandle handle = new DemoPortHandle();
    assertEquals("DemoPort", handle.getSystemPortName());
  }

  @Test
  void isOpen_beforeOpen_returnsFalse() {
    DemoPortHandle handle = new DemoPortHandle();
    assertFalse(handle.isOpen());
  }

  @Test
  void openPort_returnsTrue_andIsOpenTrue() {
    DemoPortHandle handle = new DemoPortHandle();
    assertTrue(handle.openPort());
    assertTrue(handle.isOpen());
  }

  @Test
  void openPort_twice_returnsTrueBothTimes() {
    DemoPortHandle handle = new DemoPortHandle();
    assertTrue(handle.openPort());
    assertTrue(handle.openPort());
    assertTrue(handle.isOpen());
  }

  @Test
  void closePort_afterOpen_returnsTrue_andIsOpenFalse() {
    DemoPortHandle handle = new DemoPortHandle();
    handle.openPort();
    assertTrue(handle.closePort());
    assertFalse(handle.isOpen());
  }

  @Test
  void closePort_whenNotOpen_returnsTrue() {
    DemoPortHandle handle = new DemoPortHandle();
    assertTrue(handle.closePort());
  }

  @Test
  void echo_writtenBytesAppearAsReceivedPrefixOnInputStream() throws Exception {
    DemoPortHandle handle = new DemoPortHandle();
    assertTrue(handle.openPort());
    byte[] toSend = "PA\r".getBytes(StandardCharsets.UTF_8);
    OutputStream out = handle.getOutputStream();
    out.write(toSend);
    out.flush();
    InputStream in = handle.getInputStream();
    byte[] buf = new byte[256];
    int n = in.read(buf);
    assertTrue(n > 0);
    String received = new String(buf, 0, n, StandardCharsets.UTF_8);
    assertEquals("Received: PA\r", received);
  }

  @Test
  void echo_multipleWrites_bothEchoedWithPrefix() throws Exception {
    DemoPortHandle handle = new DemoPortHandle();
    assertTrue(handle.openPort());
    OutputStream out = handle.getOutputStream();
    InputStream in = handle.getInputStream();
    out.write("A".getBytes(StandardCharsets.UTF_8));
    out.flush();
    out.write("B".getBytes(StandardCharsets.UTF_8));
    out.flush();
    byte[] buf = new byte[256];
    int n = in.read(buf);
    assertTrue(n > 0, "expected at least one echo");
    String received = new String(buf, 0, n, StandardCharsets.UTF_8);
    assertTrue(
        received.contains("Received: A"), "expected to contain 'Received: A', got: " + received);
    assertTrue(
        received.contains("Received: B"), "expected to contain 'Received: B', got: " + received);
  }

  @Test
  void getInputStream_beforeOpen_returnsStreamThatReadsEof() throws Exception {
    DemoPortHandle handle = new DemoPortHandle();
    InputStream in = handle.getInputStream();
    assertEquals(-1, in.read());
  }

  @Test
  void getOutputStream_beforeOpen_acceptsWriteWithoutThrowing() throws Exception {
    DemoPortHandle handle = new DemoPortHandle();
    OutputStream out = handle.getOutputStream();
    out.write(65);
    out.write("x".getBytes(StandardCharsets.UTF_8));
  }
}
