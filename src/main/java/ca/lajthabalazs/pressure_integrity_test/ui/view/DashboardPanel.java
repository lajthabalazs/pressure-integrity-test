package ca.lajthabalazs.pressure_integrity_test.ui.view;

import ca.lajthabalazs.pressure_integrity_test.measurement.Humidity;
import ca.lajthabalazs.pressure_integrity_test.measurement.Measurement;
import ca.lajthabalazs.pressure_integrity_test.measurement.Pressure;
import ca.lajthabalazs.pressure_integrity_test.measurement.Temperature;
import ca.lajthabalazs.pressure_integrity_test.measurement.streaming.MeasurementVectorStream;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Shape;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;

/**
 * Dashboard panel that shows the first pressure, temperature, and humidity values and charts of all
 * measurements. Shown when a measurement vector stream is active; updates on each new vector.
 */
public class DashboardPanel extends JPanel {

  private static final ZoneId DISPLAY_ZONE = ZoneId.of("Europe/Budapest");
  private static final String NO_VALUE = "—";

  private final JLabel firstPressureLabel;
  private final JLabel firstTemperatureLabel;
  private final JLabel firstHumidityLabel;
  private final JLabel firstHumidityAgainLabel;

  private final TimeSeriesCollection pressureDataset;
  private final TimeSeriesCollection temperatureDataset;
  private final TimeSeriesCollection humidityDataset;
  private final TimeSeriesCollection humidityAgainDataset;

  private final Map<String, TimeSeries> pressureSeriesById = new HashMap<>();
  private final Map<String, TimeSeries> temperatureSeriesById = new HashMap<>();
  private final Map<String, TimeSeries> humiditySeriesById = new HashMap<>();
  private final Map<String, TimeSeries> humidityAgainSeriesById = new HashMap<>();

  private MeasurementVectorStream.Subscription subscription;

  public DashboardPanel() {
    setLayout(new BorderLayout(8, 8));
    setBackground(Color.WHITE);
    setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

    JPanel valuesRow = new JPanel(new GridLayout(1, 4, 12, 0));
    valuesRow.setBackground(Color.WHITE);
    firstPressureLabel = addValueCell(valuesRow, "Pressure");
    firstTemperatureLabel = addValueCell(valuesRow, "Temperature");
    firstHumidityLabel = addValueCell(valuesRow, "Humidity");
    firstHumidityAgainLabel = addValueCell(valuesRow, "Humidity");
    add(valuesRow, BorderLayout.NORTH);

    pressureDataset = new TimeSeriesCollection();
    temperatureDataset = new TimeSeriesCollection();
    humidityDataset = new TimeSeriesCollection();
    humidityAgainDataset = new TimeSeriesCollection();

    JPanel chartsPanel = new JPanel(new GridLayout(2, 2, 8, 8));
    chartsPanel.setBackground(Color.WHITE);
    chartsPanel.add(createChartPanel("Pressure", pressureDataset, "Pa"));
    chartsPanel.add(createChartPanel("Temperature", temperatureDataset, "°C"));
    chartsPanel.add(createChartPanel("Humidity", humidityDataset, "%"));
    chartsPanel.add(createChartPanel("Humidity", humidityAgainDataset, "%"));
    add(chartsPanel, BorderLayout.CENTER);
  }

  private static JLabel addValueCell(JPanel row, String title) {
    JPanel cell = new JPanel(new BorderLayout(4, 4));
    cell.setBackground(Color.WHITE);
    cell.setBorder(BorderFactory.createTitledBorder(title));
    JLabel value = new JLabel(NO_VALUE);
    value.setFont(value.getFont().deriveFont(18f));
    cell.add(value, BorderLayout.CENTER);
    row.add(cell);
    return value;
  }

  private static String timeZoneLabel() {
    ZoneOffset offset = DISPLAY_ZONE.getRules().getOffset(Instant.now());
    int seconds = offset.getTotalSeconds();
    int hours = seconds / 3600;
    int mins = Math.abs((seconds % 3600) / 60);
    if (mins == 0) {
      return String.format("Time (UTC%+d)", hours);
    }
    return String.format("Time (UTC%+d:%02d)", hours, mins);
  }

  /**
   * Renderer that highlights one data point (larger shape) and provides tooltip text with series
   * color, sensor id, and value.
   */
  private static final class HighlightingRenderer extends XYLineAndShapeRenderer {
    private int highlightedSeries = -1;
    private int highlightedItem = -1;
    private final String yUnit;
    private static final int HIGHLIGHT_SHAPE_SIZE = 10;
    private static final int DEFAULT_SHAPE_SIZE = 4;

    HighlightingRenderer(String yUnit) {
      super(true, true);
      this.yUnit = yUnit;
    }

    void setHighlighted(int series, int item) {
      if (this.highlightedSeries != series || this.highlightedItem != item) {
        this.highlightedSeries = series;
        this.highlightedItem = item;
        fireChangeEvent();
      }
    }

    void clearHighlight() {
      setHighlighted(-1, -1);
    }

    @Override
    public Shape getItemShape(int series, int item) {
      Shape base = super.getItemShape(series, item);
      if (series == highlightedSeries && item == highlightedItem) {
        return new java.awt.geom.Ellipse2D.Double(
            -HIGHLIGHT_SHAPE_SIZE / 2.0,
            -HIGHLIGHT_SHAPE_SIZE / 2.0,
            HIGHLIGHT_SHAPE_SIZE,
            HIGHLIGHT_SHAPE_SIZE);
      }
      return base;
    }

    String getToolTipText(int series, int item, XYDataset dataset) {
      if (series < 0 || item < 0 || dataset == null) {
        return null;
      }
      String sensorId =
          dataset instanceof TimeSeriesCollection
              ? ((TimeSeriesCollection) dataset).getSeries(series).getKey().toString()
              : "Series " + series;
      double value = dataset.getYValue(series, item);
      Paint paint = getSeriesPaint(series);
      String colorStr = paint instanceof Color ? colorToHex((Color) paint) : String.valueOf(paint);
      return String.format(
          "<html><span style='color:%s'>■</span> <b>%s</b>: %s %s</html>",
          colorStr, sensorId, value, yUnit);
    }

    private static String colorToHex(Color c) {
      return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }
  }

  /** Custom tooltip popup shown near the cursor; avoids reliance on Swing ToolTipManager. */
  private static final class ChartTooltipPopup {
    private final JWindow window;
    private final JLabel label;
    private static final int OFFSET_X = 16;
    private static final int OFFSET_Y = 16;

    ChartTooltipPopup(Window parent) {
      window = new JWindow(parent);
      label = new JLabel();
      label.setOpaque(true);
      label.setBackground(Color.WHITE);
      label.setBorder(
          BorderFactory.createCompoundBorder(
              BorderFactory.createLineBorder(Color.GRAY),
              BorderFactory.createEmptyBorder(12, 16, 12, 16)));
      window.getContentPane().add(label);
    }

    void show(String htmlText, Point screenLocation) {
      if (htmlText == null || htmlText.isEmpty()) {
        hide();
        return;
      }
      label.setText(htmlText);
      window.pack();
      window.setLocation(screenLocation.x + OFFSET_X, screenLocation.y + OFFSET_Y);
      window.setVisible(true);
    }

    void hide() {
      window.setVisible(false);
    }
  }

  private ChartPanel createChartPanel(String title, TimeSeriesCollection dataset, String yUnit) {
    DateAxis xAxis = new DateAxis(timeZoneLabel());
    xAxis.setTimeZone(java.util.TimeZone.getTimeZone(DISPLAY_ZONE));

    NumberAxis yAxis = new NumberAxis(yUnit);
    yAxis.setAutoRangeIncludesZero(false);
    yAxis.setAutoRangeStickyZero(false);

    HighlightingRenderer renderer = new HighlightingRenderer(yUnit);
    renderer.setDefaultToolTipGenerator(
        (dataset1, series, item) -> renderer.getToolTipText(series, item, dataset1));
    XYPlot plot = new XYPlot(dataset, xAxis, yAxis, renderer);
    plot.setBackgroundPaint(Color.WHITE);
    JFreeChart chart = new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, false);
    ChartPanel chartPanel = new ChartPanel(chart);
    chartPanel.setBackground(Color.WHITE);

    final ChartTooltipPopup[] popupHolder = new ChartTooltipPopup[1];

    chartPanel.addChartMouseListener(
        new ChartMouseListener() {
          @Override
          public void chartMouseClicked(ChartMouseEvent event) {}

          @Override
          public void chartMouseMoved(ChartMouseEvent event) {
            MouseEvent me = event.getTrigger();
            Point2D java2D = chartPanel.translateScreenToJava2D(me.getPoint());
            Rectangle2D dataArea = chartPanel.getScreenDataArea();
            if (java2D == null || !dataArea.contains(java2D)) {
              renderer.clearHighlight();
              if (popupHolder[0] != null) popupHolder[0].hide();
              return;
            }
            double xValue =
                plot.getDomainAxis()
                    .java2DToValue(java2D.getX(), dataArea, plot.getDomainAxisEdge());
            double yValue =
                plot.getRangeAxis().java2DToValue(java2D.getY(), dataArea, plot.getRangeAxisEdge());

            int bestSeries = -1;
            int bestItem = -1;
            double bestDistSq = Double.POSITIVE_INFINITY;
            for (int s = 0; s < dataset.getSeriesCount(); s++) {
              for (int i = 0; i < dataset.getItemCount(s); i++) {
                double dx = dataset.getXValue(s, i) - xValue;
                double dy = dataset.getYValue(s, i) - yValue;
                double dSq = dx * dx + dy * dy;
                if (dSq < bestDistSq) {
                  bestDistSq = dSq;
                  bestSeries = s;
                  bestItem = i;
                }
              }
            }
            if (bestSeries >= 0) {
              renderer.setHighlighted(bestSeries, bestItem);
              long timeMs = (long) dataset.getXValue(bestSeries, bestItem);
              String dateTimeStr =
                  DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                      .withZone(DISPLAY_ZONE)
                      .format(Instant.ofEpochMilli(timeMs));
              String body = renderer.getToolTipText(bestSeries, bestItem, dataset);
              String content =
                  body.startsWith("<html>") && body.endsWith("</html>")
                      ? body.substring(6, body.length() - 7).trim()
                      : body;
              String text = "<html><b>" + dateTimeStr + "</b><br/>" + content + "</html>";
              Window parent = SwingUtilities.getWindowAncestor(chartPanel);
              if (parent != null) {
                if (popupHolder[0] == null) popupHolder[0] = new ChartTooltipPopup(parent);
                Point onScreen = chartPanel.getLocationOnScreen();
                popupHolder[0].show(
                    text, new Point(onScreen.x + me.getX(), onScreen.y + me.getY()));
              }
            } else {
              renderer.clearHighlight();
              if (popupHolder[0] != null) popupHolder[0].hide();
            }
          }
        });

    return chartPanel;
  }

  /**
   * Subscribes to the given stream. Updates the four value labels and the four charts on each new
   * measurement vector. Call {@link #clear()} before starting a new stream if you want a fresh
   * dashboard.
   */
  public void subscribe(MeasurementVectorStream stream) {
    if (subscription != null) {
      subscription.unsubscribe();
    }
    subscription =
        stream.subscribe(
            vector ->
                SwingUtilities.invokeLater(
                    () -> {
                      Measurement firstPressure = null;
                      Measurement firstTemperature = null;
                      Measurement firstHumidity = null;
                      for (Measurement m : vector.getMeasurements()) {
                        if (firstPressure == null && m instanceof Pressure) firstPressure = m;
                        if (firstTemperature == null && m instanceof Temperature)
                          firstTemperature = m;
                        if (firstHumidity == null && m instanceof Humidity) firstHumidity = m;
                        if (firstPressure != null
                            && firstTemperature != null
                            && firstHumidity != null) {
                          break;
                        }
                      }

                      long timeUtc = vector.getTimeUtc();
                      Date date = new Date(timeUtc);
                      Millisecond period = new Millisecond(date);

                      for (Measurement m : vector.getMeasurements()) {
                        double value = m.getValueInDefaultUnit().doubleValue();
                        String id = m.getSourceId();
                        if (m instanceof Pressure) {
                          pressureSeriesById
                              .computeIfAbsent(
                                  id,
                                  k -> {
                                    TimeSeries s = new TimeSeries(id);
                                    pressureDataset.addSeries(s);
                                    return s;
                                  })
                              .addOrUpdate(period, value);
                        } else if (m instanceof Temperature) {
                          temperatureSeriesById
                              .computeIfAbsent(
                                  id,
                                  k -> {
                                    TimeSeries s = new TimeSeries(id);
                                    temperatureDataset.addSeries(s);
                                    return s;
                                  })
                              .addOrUpdate(period, value);
                        } else if (m instanceof Humidity) {
                          humiditySeriesById
                              .computeIfAbsent(
                                  id,
                                  k -> {
                                    TimeSeries s = new TimeSeries(id);
                                    humidityDataset.addSeries(s);
                                    return s;
                                  })
                              .addOrUpdate(period, value);
                          humidityAgainSeriesById
                              .computeIfAbsent(
                                  id,
                                  k -> {
                                    TimeSeries s = new TimeSeries(id);
                                    humidityAgainDataset.addSeries(s);
                                    return s;
                                  })
                              .addOrUpdate(period, value);
                        }
                      }

                      updateLabel(firstPressureLabel, firstPressure);
                      updateLabel(firstTemperatureLabel, firstTemperature);
                      updateLabel(firstHumidityLabel, firstHumidity);
                      updateLabel(firstHumidityAgainLabel, firstHumidity);
                    }));
  }

  private static void updateLabel(JLabel label, Measurement m) {
    label.setText(
        m != null
            ? m.getValueInDefaultUnit().toPlainString() + " " + m.getDefaultUnit()
            : NO_VALUE);
  }

  /** Stops receiving updates and clears all displayed values and chart data. */
  public void clear() {
    if (subscription != null) {
      subscription.unsubscribe();
      subscription = null;
    }
    firstPressureLabel.setText(NO_VALUE);
    firstTemperatureLabel.setText(NO_VALUE);
    firstHumidityLabel.setText(NO_VALUE);
    firstHumidityAgainLabel.setText(NO_VALUE);
    pressureSeriesById.clear();
    temperatureSeriesById.clear();
    humiditySeriesById.clear();
    humidityAgainSeriesById.clear();
    pressureDataset.removeAllSeries();
    temperatureDataset.removeAllSeries();
    humidityDataset.removeAllSeries();
    humidityAgainDataset.removeAllSeries();
  }
}
