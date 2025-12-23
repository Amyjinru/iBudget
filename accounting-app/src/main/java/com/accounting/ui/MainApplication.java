package com.accounting.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import com.accounting.storage.StorageManager;
import com.accounting.service.TransactionService;
import com.accounting.service.BudgetService;
import com.accounting.service.StatisticService;
import com.accounting.service.local.LocalBudgetService;
import com.accounting.service.local.LocalStatisticService;
import com.accounting.service.local.LocalTransactionService;
import com.accounting.chart.BarChartView;
import com.accounting.chart.PieChartView;
import com.accounting.chart.LineChartView;
import com.accounting.chart.ComboBarChartView;
import com.accounting.chart.ChartAnalyzer;
import java.time.YearMonth;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.time.LocalDateTime;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.stage.FileChooser;
import java.io.File;
import com.accounting.model.Transaction;
import com.accounting.model.Transaction.TransactionType;

public class MainApplication extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("iBudget");
        TabPane tabPane = new TabPane();
        StorageManager storage = new StorageManager();
        LocalTransactionService ts = new LocalTransactionService(storage);
        LocalBudgetService bs = new LocalBudgetService(storage, ts);
        LocalStatisticService ss = new LocalStatisticService(ts);
        ChartAnalyzer analyzer = new ChartAnalyzer(ss);
        ApiClient api = new ApiClient("http://localhost:8080");
        VBox authBox = new VBox();
        authBox.setSpacing(10);
        authBox.setStyle("-fx-padding: 16px;");
        TextField username = new TextField();
        username.setPromptText("用户名");
        TextField email = new TextField();
        email.setPromptText("邮箱");
        TextField password = new TextField();
        password.setPromptText("密码");
        Label authStatus = new Label("未登录");
        Button btnRegister = new Button("注册");
        btnRegister.getStyleClass().add("button");
        Button btnLogin = new Button("登录");
        btnLogin.getStyleClass().add("button");
        btnLogin.getStyleClass().add("primary");
        btnRegister.setOnAction(e -> {
            try {
                api.register(username.getText(), email.getText(), password.getText());
                authStatus.setText("注册成功");
            } catch (Exception ex) {
                authStatus.setText("注册失败");
            }
        });
        btnLogin.setOnAction(e -> {
            try {
                api.login(username.getText(), password.getText());
                authStatus.setText("已登录");
            } catch (Exception ex) {
                authStatus.setText("登录失败");
            }
        });
        HBox authInputs = new HBox(username, email, password);
        authInputs.setSpacing(10);
        HBox authActions = new HBox(btnRegister, btnLogin);
        authActions.setSpacing(10);
        authBox.getChildren().addAll(new Label("账号登录/注册"), authInputs, authActions, authStatus);
        VBox txBox = new VBox();
        txBox.setSpacing(10);
        txBox.setStyle("-fx-padding: 16px;");
        TableView<Transaction> table = new TableView<>();
        ObservableList<Transaction> data = FXCollections.observableArrayList(ts.getAllTransactions());
        table.setItems(data);
        TableColumn<Transaction, String> colType = new TableColumn<>("类型");
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        TableColumn<Transaction, Double> colAmount = new TableColumn<>("金额");
        colAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        TableColumn<Transaction, String> colCategory = new TableColumn<>("分类");
        colCategory.setCellValueFactory(new PropertyValueFactory<>("categoryId"));
        TableColumn<Transaction, String> colDesc = new TableColumn<>("描述");
        colDesc.setCellValueFactory(new PropertyValueFactory<>("description"));
        table.getColumns().addAll(colType, colAmount, colCategory, colDesc);
        ComboBox<TransactionType> typeBox = new ComboBox<>();
        typeBox.getItems().addAll(TransactionType.EXPENSE, TransactionType.INCOME);
        TextField amountField = new TextField();
        amountField.setPromptText("金额");
        TextField categoryField = new TextField();
        categoryField.setPromptText("分类ID");
        TextField descField = new TextField();
        descField.setPromptText("描述");
        Button btnAdd = new Button("添加");
        btnAdd.getStyleClass().add("button");
        btnAdd.getStyleClass().add("primary");
        btnAdd.setOnAction(e -> {
            try {
                Transaction t = new Transaction(username.getText(), typeBox.getValue(), Double.parseDouble(amountField.getText()), categoryField.getText(), descField.getText());
                t.setDate(LocalDateTime.now());
                ts.addTransaction(t);
                data.setAll(ts.getAllTransactions());
            } catch (Exception ignored) {}
        });
        Button btnDelete = new Button("删除选中");
        btnDelete.getStyleClass().add("button");
        btnDelete.setOnAction(e -> {
            Transaction sel = table.getSelectionModel().getSelectedItem();
            if (sel != null) {
                ts.deleteTransaction(sel.getId());
                data.setAll(ts.getAllTransactions());
            }
        });
        Button btnExport = new Button("导出CSV");
        btnExport.getStyleClass().add("button");
        btnExport.setOnAction(e -> {
            try {
                FileChooser fc = new FileChooser();
                fc.setTitle("导出CSV");
                fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
                File f = fc.showSaveDialog(stage);
                if (f != null) ts.exportToCSV(f.getAbsolutePath(), ts.getAllTransactions());
            } catch (Exception ignored) {}
        });
        Button btnImport = new Button("导入CSV");
        btnImport.getStyleClass().add("button");
        btnImport.setOnAction(e -> {
            try {
                FileChooser fc = new FileChooser();
                fc.setTitle("导入CSV");
                fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
                File f = fc.showOpenDialog(stage);
                if (f != null) {
                    ts.importFromCSV(f.getAbsolutePath());
                    data.setAll(ts.getAllTransactions());
                }
            } catch (Exception ignored) {}
        });
        Button btnPull = new Button("拉取远端");
        btnPull.getStyleClass().add("button");
        btnPull.setOnAction(e -> {
            try {
                if (api.isLoggedIn()) {
                    List<Transaction> remote = api.listTransactions();
                    ts.clearAllTransactions();
                    ts.addTransactions(remote);
                    data.setAll(ts.getAllTransactions());
                }
            } catch (Exception ignored) {}
        });
        Button btnPush = new Button("上传本地");
        btnPush.getStyleClass().add("button");
        btnPush.setOnAction(e -> {
            try {
                if (api.isLoggedIn()) {
                    api.uploadTransactions(ts.getAllTransactions());
                }
            } catch (Exception ignored) {}
        });
        HBox txForm = new HBox(typeBox, amountField, categoryField, descField, btnAdd, btnDelete);
        txForm.setSpacing(10);
        HBox txActions = new HBox(btnExport, btnImport, btnPull, btnPush);
        txActions.setSpacing(10);
        txBox.getChildren().addAll(new Label("交易管理"), table, txForm, txActions);
        VBox budgetBox = new VBox();
        budgetBox.setSpacing(10);
        budgetBox.setStyle("-fx-padding: 16px;");
        TextField yearField = new TextField();
        yearField.setPromptText("年份");
        TextField monthField = new TextField();
        monthField.setPromptText("月份");
        TextField budgetCatField = new TextField();
        budgetCatField.setPromptText("分类ID(留空表示总预算)");
        TextField budgetAmountField = new TextField();
        budgetAmountField.setPromptText("预算金额");
        Label budgetInfo = new Label();
        Button btnSetBudget = new Button("设置预算");
        btnSetBudget.getStyleClass().add("button");
        btnSetBudget.getStyleClass().add("primary");
        btnSetBudget.setOnAction(e -> {
            try {
                int y = Integer.parseInt(yearField.getText());
                int m = Integer.parseInt(monthField.getText());
                String cat = budgetCatField.getText();
                double amt = Double.parseDouble(budgetAmountField.getText());
                bs.setMonthlyBudget(username.getText(), cat != null && cat.isEmpty() ? null : cat, amt, y, m);
                double used = bs.calculateUsedAmount(username.getText(), cat != null && cat.isEmpty() ? null : cat, y, m);
                boolean over = bs.isOverBudget(username.getText(), cat != null && cat.isEmpty() ? null : cat, y, m);
                budgetInfo.setText("已用: " + used + (over ? " 超额" : ""));
            } catch (Exception ignored) {}
        });
        HBox budgetForm = new HBox(yearField, monthField, budgetCatField, budgetAmountField, btnSetBudget);
        budgetForm.setSpacing(10);
        budgetBox.getChildren().addAll(new Label("预算设置"), budgetForm, budgetInfo);
        // 图表页面
        Map<String, Double> catExpenseData = analyzer.categoryExpense(username.getText().isEmpty() ? "demo" : username.getText(), YearMonth.now());
        Map<String, Double> catIncomeData = analyzer.categoryIncome(username.getText().isEmpty() ? "demo" : username.getText(), YearMonth.now());
        List<Double> expenseSeries = analyzer.monthlyExpensesSeries(username.getText().isEmpty() ? "demo" : username.getText(), 12);
        List<Double> incomeSeries = analyzer.monthlyIncomeSeries(username.getText().isEmpty() ? "demo" : username.getText(), 12);
        List<Double> netSeries = analyzer.monthlyNetSeries(username.getText().isEmpty() ? "demo" : username.getText(), 12);
        
        VBox chartBox = new VBox();
        chartBox.setSpacing(15);
        chartBox.setStyle("-fx-padding: 16px;");
        
        // 饼图卡片
        VBox pieCard = new VBox();
        pieCard.setSpacing(10);
        pieCard.setStyle("-fx-padding: 12px; -fx-background-color: -fx-background; -fx-border-color: -fx-border; -fx-border-radius: 8px; -fx-background-radius: 8px;");
        Label pieTitle = new Label("📊 饼状图");
        pieTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");
        HBox pieCharts = new HBox();
        pieCharts.setSpacing(20);
        VBox expensePieBox = new VBox();
        expensePieBox.setSpacing(5);
        Label expensePieLabel = new Label("支出分类");
        expensePieLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: gray;");
        expensePieBox.getChildren().addAll(expensePieLabel, new PieChartView(catExpenseData).getView());
        VBox incomePieBox = new VBox();
        incomePieBox.setSpacing(5);
        Label incomePieLabel = new Label("收入分类");
        incomePieLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: gray;");
        incomePieBox.getChildren().addAll(incomePieLabel, new PieChartView(catIncomeData).getView());
        pieCharts.getChildren().addAll(expensePieBox, incomePieBox);
        pieCard.getChildren().addAll(pieTitle, pieCharts);
        
        // 折线图卡片
        VBox lineCard = new VBox();
        lineCard.setSpacing(10);
        lineCard.setStyle("-fx-padding: 12px; -fx-background-color: -fx-background; -fx-border-color: -fx-border; -fx-border-radius: 8px; -fx-background-radius: 8px;");
        HBox lineHeader = new HBox();
        lineHeader.setSpacing(10);
        Label lineTitle = new Label("📈 折线图");
        lineTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");
        ComboBox<String> linePeriodBox = new ComboBox<>();
        linePeriodBox.getItems().addAll("月度统计", "年度统计");
        linePeriodBox.setValue("月度统计");
        ComboBox<Integer> lineRangeBox = new ComboBox<>();
        lineRangeBox.getItems().addAll(6, 12, 24);
        lineRangeBox.setValue(12);
        lineHeader.getChildren().addAll(lineTitle, linePeriodBox, lineRangeBox);
        HBox lineCharts = new HBox();
        lineCharts.setSpacing(20);
        VBox expenseLineBox = new VBox();
        expenseLineBox.setSpacing(5);
        Label expenseLineLabel = new Label("支出趋势");
        expenseLineLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: gray;");
        expenseLineBox.getChildren().addAll(expenseLineLabel, new LineChartView(expenseSeries).getView());
        VBox incomeLineBox = new VBox();
        incomeLineBox.setSpacing(5);
        Label incomeLineLabel = new Label("收入趋势");
        incomeLineLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: gray;");
        incomeLineBox.getChildren().addAll(incomeLineLabel, new LineChartView(incomeSeries).getView());
        VBox netLineBox = new VBox();
        netLineBox.setSpacing(5);
        Label netLineLabel = new Label("净收入趋势");
        netLineLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: gray;");
        netLineBox.getChildren().addAll(netLineLabel, new LineChartView(netSeries).getView());
        lineCharts.getChildren().addAll(expenseLineBox, incomeLineBox, netLineBox);
        lineCard.getChildren().addAll(lineHeader, lineCharts);
        
        // 柱状图卡片
        VBox barCard = new VBox();
        barCard.setSpacing(10);
        barCard.setStyle("-fx-padding: 12px; -fx-background-color: -fx-background; -fx-border-color: -fx-border; -fx-border-radius: 8px; -fx-background-radius: 8px;");
        HBox barHeader = new HBox();
        barHeader.setSpacing(10);
        Label barTitle = new Label("📊 月度收支对比柱状图");
        barTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");
        ComboBox<Integer> barYearBox = new ComboBox<>();
        int currentYear = java.time.Year.now().getValue();
        for (int y = currentYear; y >= currentYear - 5; y--) {
            barYearBox.getItems().add(y);
        }
        barYearBox.setValue(currentYear);
        ComboBox<Integer> barMonthsBox = new ComboBox<>();
        barMonthsBox.getItems().addAll(6, 12, 24);
        barMonthsBox.setValue(12);
        barHeader.getChildren().addAll(barTitle, barYearBox, barMonthsBox);
        List<String> monthLabels = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            monthLabels.add(i + "月");
        }
        ComboBarChartView comboBar = new ComboBarChartView(incomeSeries, expenseSeries, monthLabels);
        barCard.getChildren().addAll(barHeader, comboBar.getView());
        
        // 刷新按钮
        Button btnRefreshCharts = new Button("刷新图表");
        btnRefreshCharts.getStyleClass().add("button");
        btnRefreshCharts.setOnAction(e -> {
            String userId = username.getText().isEmpty() ? "demo" : username.getText();
            String period = linePeriodBox.getValue();
            int range = lineRangeBox.getValue();
            int barMonths = barMonthsBox.getValue();
            
            Map<String, Double> expCat = analyzer.categoryExpense(userId, YearMonth.now());
            Map<String, Double> incCat = analyzer.categoryIncome(userId, YearMonth.now());
            
            List<Double> expLine, incLine, netLine;
            if ("年度统计".equals(period)) {
                expLine = analyzer.yearlyExpensesSeries(userId, range);
                incLine = analyzer.yearlyIncomeSeries(userId, range);
                netLine = analyzer.yearlyNetSeries(userId, range);
            } else {
                expLine = analyzer.monthlyExpensesSeries(userId, range);
                incLine = analyzer.monthlyIncomeSeries(userId, range);
                netLine = analyzer.monthlyNetSeries(userId, range);
            }
            
            List<Double> barExp = analyzer.monthlyExpensesSeries(userId, barMonths);
            List<Double> barInc = analyzer.monthlyIncomeSeries(userId, barMonths);
            
            // 重新创建图表
            pieCharts.getChildren().clear();
            VBox newExpensePieBox = new VBox();
            newExpensePieBox.setSpacing(5);
            Label newExpensePieLabel = new Label("支出分类");
            newExpensePieLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: gray;");
            newExpensePieBox.getChildren().addAll(newExpensePieLabel, new PieChartView(expCat).getView());
            VBox newIncomePieBox = new VBox();
            newIncomePieBox.setSpacing(5);
            Label newIncomePieLabel = new Label("收入分类");
            newIncomePieLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: gray;");
            newIncomePieBox.getChildren().addAll(newIncomePieLabel, new PieChartView(incCat).getView());
            pieCharts.getChildren().addAll(newExpensePieBox, newIncomePieBox);
            
            lineCharts.getChildren().clear();
            VBox newExpenseLineBox = new VBox();
            newExpenseLineBox.setSpacing(5);
            Label newExpenseLineLabel = new Label("支出趋势");
            newExpenseLineLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: gray;");
            newExpenseLineBox.getChildren().addAll(newExpenseLineLabel, new LineChartView(expLine).getView());
            VBox newIncomeLineBox = new VBox();
            newIncomeLineBox.setSpacing(5);
            Label newIncomeLineLabel = new Label("收入趋势");
            newIncomeLineLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: gray;");
            newIncomeLineBox.getChildren().addAll(newIncomeLineLabel, new LineChartView(incLine).getView());
            VBox newNetLineBox = new VBox();
            newNetLineBox.setSpacing(5);
            Label newNetLineLabel = new Label("净收入趋势");
            newNetLineLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: gray;");
            newNetLineBox.getChildren().addAll(newNetLineLabel, new LineChartView(netLine).getView());
            lineCharts.getChildren().addAll(newExpenseLineBox, newIncomeLineBox, newNetLineBox);
            
            List<String> newLabels = new ArrayList<>();
            for (int i = 1; i <= barMonths; i++) {
                newLabels.add(i + "月");
            }
            barCard.getChildren().set(1, new ComboBarChartView(barInc, barExp, newLabels).getView());
        });
        
        // 月度/年度切换事件
        linePeriodBox.setOnAction(e -> {
            if ("月度统计".equals(linePeriodBox.getValue())) {
                lineRangeBox.getItems().setAll(6, 12, 24);
                lineRangeBox.setValue(12);
            } else {
                lineRangeBox.getItems().setAll(5, 10);
                lineRangeBox.setValue(5);
            }
        });
        
        chartBox.getChildren().addAll(pieCard, lineCard, barCard, btnRefreshCharts);
        Tab authTab = new Tab("账号", authBox);
        Tab transactionsTab = new Tab("交易", txBox);
        Tab budgetTab = new Tab("预算", budgetBox);
        Tab chartsTab = new Tab("图表", chartBox);
        authTab.setClosable(false);
        transactionsTab.setClosable(false);
        budgetTab.setClosable(false);
        chartsTab.setClosable(false);
        tabPane.getTabs().addAll(authTab, transactionsTab, budgetTab, chartsTab);
        Scene scene = new Scene(tabPane, 1200, 800);
        String lightCss = MainApplication.class.getResource("/ui.css").toExternalForm();
        String darkCss = MainApplication.class.getResource("/ui-dark.css").toExternalForm();
        scene.getStylesheets().setAll(lightCss);
        CheckBox darkToggle = new CheckBox("暗色主题");
        darkToggle.setOnAction(e -> {
            if (darkToggle.isSelected()) {
                scene.getStylesheets().setAll(darkCss);
            } else {
                scene.getStylesheets().setAll(lightCss);
            }
        });
        authBox.getChildren().add(0, darkToggle);
        stage.setScene(scene);
        stage.show();
    }
}
