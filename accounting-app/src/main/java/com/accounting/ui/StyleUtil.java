package com.accounting.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileWriter;
import java.util.Map;

public class StyleUtil {
    public static String buildCss(Map<String, String> t) {
        String bg=t.get("bg"), card=t.get("card"), text=t.get("text"), muted=t.get("muted"), line=t.get("line"), accent=t.get("accent");
        return String.join("\n",
                ".root { -fx-font-family: -apple-system, 'Segoe UI', Helvetica, Arial, system-ui; -fx-background-color: "+bg+"; }",
                ".label { -fx-text-fill: "+text+"; }",
                ".tab-pane { -fx-background-color: "+bg+"; -fx-padding: 16px; }",
                ".tab-pane .tab-content-area { -fx-background-color: "+bg+"; }",
                ".tab { -fx-background-color: "+card+"; }",
                ".vbox, .hbox { -fx-background-color: "+card+"; -fx-border-color: "+line+"; -fx-background-radius: 16px; -fx-border-radius: 16px; -fx-padding: 16px; }",
                ".text-field, .combo-box { -fx-background-color: "+card+"; -fx-border-color: "+line+"; -fx-text-fill: "+text+"; -fx-background-radius: 12px; -fx-border-radius: 12px; -fx-padding: 8px 12px; }",
                ".text-field:focused, .combo-box:focused { -fx-border-color: "+accent+"; }",
                ".button { -fx-background-color: "+card+"; -fx-border-color: "+line+"; -fx-text-fill: "+text+"; -fx-background-radius: 12px; -fx-border-radius: 12px; -fx-padding: 8px 14px; }",
                ".button.primary { -fx-background-color: "+accent+"; -fx-border-color: "+accent+"; -fx-text-fill: white; }",
                ".table-view { -fx-background-color: transparent; -fx-table-cell-border-color: "+line+"; -fx-padding: 8px; }",
                ".table-view .column-header-background { -fx-background-color: "+card+"; -fx-border-color: "+line+"; -fx-background-radius: 12px 12px 0 0; -fx-border-radius: 12px 12px 0 0; }",
                ".table-view .column-header { -fx-background-color: "+card+"; }",
                ".table-row-cell:filled:selected, .table-row-cell:filled:hover { -fx-background-color: #f0f7ff; }"
        );
    }
    @SuppressWarnings("unchecked")
    public static File generateStylesheet(boolean dark) {
        try {
            var mapper=new ObjectMapper();
            var is=StyleUtil.class.getResourceAsStream("/design-tokens.json");
            Map<String,Object> root=mapper.readValue(is, Map.class);
            Map<String,String> t=(Map<String,String>) (dark? root.get("dark") : root.get("light"));
            String css=buildCss(t);
            File f=File.createTempFile(dark?"ui-dark-":"ui-",".css");
            try(FileWriter w=new FileWriter(f)){w.write(css);} 
            return f;
        } catch(Exception e){
            return null;
        }
    }
}
