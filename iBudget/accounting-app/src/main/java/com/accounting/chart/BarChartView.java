package com.accounting.chart;

import java.util.Map;

import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;

public class BarChartView implements Chart {
    private final BarChart<String, Number> chart;
    public BarChartView(Map<String, Double> data) {
        CategoryAxis x = new CategoryAxis();
        NumberAxis y = new NumberAxis();
        chart = new BarChart<>(x, y);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        data.forEach((k, v) -> series.getData().add(new XYChart.Data<>(k, v)));
        chart.getData().add(series);
    }
    @Override
    public Node getView() {
        return chart;
    }
}
