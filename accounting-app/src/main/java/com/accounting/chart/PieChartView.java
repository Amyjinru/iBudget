package com.accounting.chart;

import javafx.scene.Node;
import javafx.scene.chart.PieChart;
import java.util.Map;

public class PieChartView implements Chart {
    private final PieChart chart;
    public PieChartView(Map<String, Double> data) {
        chart = new PieChart();
        data.forEach((k, v) -> chart.getData().add(new PieChart.Data(k, v)));
    }
    @Override
    public Node getView() {
        return chart;
    }
}
