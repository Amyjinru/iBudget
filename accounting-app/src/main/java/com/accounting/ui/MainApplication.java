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
import com.accounting.chart.ChartAnalyzer;
import java.time.YearMonth;
import java.util.List;
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
        stage.setTitle("记账软件");
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
        Map<String, Double> catData = analyzer.categoryExpense(username.getText().isEmpty() ? "demo" : username.getText(), YearMonth.now());
        List<Double> series = analyzer.monthlyExpensesSeries(username.getText().isEmpty() ? "demo" : username.getText(), 6);
        VBox chartBox = new VBox();
        chartBox.setSpacing(10);
        chartBox.setStyle("-fx-padding: 16px;");
        chartBox.getChildren().addAll(
                new Label("分类支出饼图"),
                new PieChartView(catData).getView(),
                new Label("分类支出柱状图"),
                new BarChartView(catData).getView(),
                new Label("月度支出折线图"),
                new LineChartView(series).getView()
        );
        Button btnRefreshCharts = new Button("刷新图表");
        btnRefreshCharts.getStyleClass().add("button");
        btnRefreshCharts.setOnAction(e -> {
            Map<String, Double> d = analyzer.categoryExpense(username.getText().isEmpty() ? "demo" : username.getText(), YearMonth.now());
            List<Double> s = analyzer.monthlyExpensesSeries(username.getText().isEmpty() ? "demo" : username.getText(), 6);
            chartBox.getChildren().setAll(new Label("分类支出饼图"), new PieChartView(d).getView(), new Label("分类支出柱状图"), new BarChartView(d).getView(), new Label("月度支出折线图"), new LineChartView(s).getView(), btnRefreshCharts);
        });
        chartBox.getChildren().add(btnRefreshCharts);
        Tab authTab = new Tab("账号", authBox);
        Tab transactionsTab = new Tab("交易", txBox);
        Tab budgetTab = new Tab("预算", budgetBox);
        Tab chartsTab = new Tab("图表", chartBox);
        authTab.setClosable(false);
        transactionsTab.setClosable(false);
        budgetTab.setClosable(false);
        chartsTab.setClosable(false);
        tabPane.getTabs().addAll(authTab, transactionsTab, budgetTab, chartsTab);
        Scene scene = new Scene(tabPane, 1000, 700);
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
