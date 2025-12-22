package com.accounting.chart;

import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import java.util.List;

public class LineChartView implements Chart {
    private final LineChart<Number, Number> chart;
    public LineChartView(List<Double> values) {
        NumberAxis x = new NumberAxis();
        NumberAxis y = new NumberAxis();
        chart = new LineChart<>(x, y);
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        for (int i = 0; i < values.size(); i++) {
            series.getData().add(new XYChart.Data<>(i + 1, values.get(i)));
        }
        chart.getData().add(series);
    }
    @Override
    public Node getView() {
        return chart;
    }
}
