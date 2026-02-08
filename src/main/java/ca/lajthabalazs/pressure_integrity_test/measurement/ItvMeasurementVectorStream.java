package ca.lajthabalazs.pressure_integrity_test.measurement;

import ca.lajthabalazs.pressure_integrity_test.io.ItvFileReader;
import ca.lajthabalazs.pressure_integrity_test.io.TextFileReader;
import java.util.List;

/**
 * A {@link MeasurementVectorStream} that loads measurements from an ITV file and, when {@link
 * #start()} is called, publishes all measurement vectors to its subscribers in sequence.
 *
 * <p>Times in the ITV file are interpreted as Paks, Hungary local time.
 */
public class ItvMeasurementVectorStream extends MeasurementVectorStream {

  private final ItvFileReader reader;
  private final String filePath;
  private List<MeasurementVector> vectors;

  /**
   * Creates an ItvMeasurementVectorStream that will read from the given file.
   *
   * @param textFileReader the text file reader used to load the ITV file
   * @param filePath path to the ITV file
   */
  public ItvMeasurementVectorStream(TextFileReader textFileReader, String filePath) {
    this.reader = new ItvFileReader(textFileReader);
    this.filePath = filePath;
  }

  /**
   * Loads the ITV file and publishes all measurement vectors to all subscribed handlers. Each
   * vector is delivered immediately in order.
   *
   * @throws TextFileReader.FailedToReadFileException if the file cannot be read
   */
  public void start() throws TextFileReader.FailedToReadFileException {
    vectors = reader.read(filePath);
    for (MeasurementVector vector : vectors) {
      publish(vector);
    }
  }

  /** Returns the loaded measurement vectors, or null if {@link #start()} has not been called. */
  public List<MeasurementVector> getVectors() {
    return vectors;
  }
}
