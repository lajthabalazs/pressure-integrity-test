package ca.lajthabalazs.pressure_integity_test.measurement.streaming;

import ca.lajthabalazs.pressure_integity_test.io.ResourceTextFileReader;
import ca.lajthabalazs.pressure_integrity_test.io.TextFileReader;
import ca.lajthabalazs.pressure_integrity_test.measurement.MeasurementVector;
import ca.lajthabalazs.pressure_integrity_test.measurement.streaming.ItvMeasurementVectorStream;
import ca.lajthabalazs.pressure_integrity_test.measurement.streaming.MeasurementVectorStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** Tests for {@link ItvMeasurementVectorStream}. */
public class ItvMeasurementVectorStreamTest {

  @Test
  public void start_dumpsAllVectorsToListeners() throws Exception {
    TextFileReader reader = new ResourceTextFileReader(ItvMeasurementVectorStreamTest.class);
    ItvMeasurementVectorStream stream =
        new ItvMeasurementVectorStream(reader, "/itv/itv-sample.ITV");

    List<MeasurementVector> received = new ArrayList<>();
    MeasurementVectorStream.Subscription sub = stream.subscribe(vector -> received.add(vector));

    stream.start();

    Assertions.assertEquals(10, received.size());
    Assertions.assertEquals(10, stream.getVectors().size());
    Assertions.assertEquals(received.get(0).getTimeUtc(), stream.getVectors().get(0).getTimeUtc());
    Assertions.assertEquals(received.get(1).getTimeUtc(), stream.getVectors().get(1).getTimeUtc());

    sub.unsubscribe();
  }
}
