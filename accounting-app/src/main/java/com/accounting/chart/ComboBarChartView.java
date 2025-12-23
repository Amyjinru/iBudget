package com.accounting.chart;

import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import java.util.List;

public class ComboBarChartView implements Chart {
    private final BarChart<String, Number> chart;
    
    public ComboBarChartView(List<Double> incomes, List<Double> expenses, List<String> labels) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("时间");
        yAxis.setLabel("金额");
        
        chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle("收支对比");
        
        XYChart.Series<String, Number> incomeSeries = new XYChart.Series<>();
        incomeSeries.setName("收入");
        
        XYChart.Series<String, Number> expenseSeries = new XYChart.Series<>();
        expenseSeries.setName("支出");
        
        for (int i = 0; i < Math.min(incomes.size(), labels.size()); i++) {
            incomeSeries.getData().add(new XYChart.Data<>(labels.get(i), incomes.get(i)));
            expenseSeries.getData().add(new XYChart.Data<>(labels.get(i), expenses.get(i)));
        }
        
        chart.getData().addAll(incomeSeries, expenseSeries);
    }
    
    @Override
    public Node getView() {
        return chart;
    }
}
