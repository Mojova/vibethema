import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;

import java.io.File;
import java.util.*;

public class GenerateDotArrays {
    public static void main(String[] args) throws Exception {
        PDDocument doc = Loader.loadPDF(new File("src/main/resources/interactive_sheet.pdf"));
        PDAcroForm form = doc.getDocumentCatalog().getAcroForm();
        
        // Group fields by rounded Y coordinate
        Map<Integer, List<FieldInfo>> rows = new HashMap<>();
        
        for (PDField field : form.getFields()) {
            String name = field.getFullyQualifiedName();
            if (name.startsWith("dot")) {
                List<PDAnnotationWidget> widgets = field.getWidgets();
                if (widgets != null && !widgets.isEmpty()) {
                    float y = widgets.get(0).getRectangle().getLowerLeftY();
                    float x = widgets.get(0).getRectangle().getLowerLeftX();
                    if (x > 163 && x < 200) { // Only first column dots
                        int roundedY = Math.round(y);
                        rows.computeIfAbsent(roundedY, k -> new ArrayList<>()).add(new FieldInfo(name, x, y));
                    }
                }
            }
        }
        
        List<Integer> sortedYs = new ArrayList<>(rows.keySet());
        sortedYs.sort(Collections.reverseOrder());
        
        int abilityCount = 0;
        for (Integer y : sortedYs) {
            if (y > 600) continue; // Skip physical attributes
            
            List<FieldInfo> rowFields = rows.get(y);
            rowFields.sort(Comparator.comparing(f -> f.x));
            
            System.out.print("new String[]{");
            for (int i = 0; i < rowFields.size(); i++) {
                System.out.print("\"" + rowFields.get(i).name + "\"");
                if (i < rowFields.size() - 1) System.out.print(", ");
            }
            System.out.println("}, // Y=" + y + ", Ability " + (abilityCount + 1));
            
            abilityCount++;
            if (abilityCount >= 26) break;
        }
        doc.close();
    }
    
    static class FieldInfo {
        String name;
        float x, y;
        FieldInfo(String n, float x, float y) { this.name = n; this.x = x; this.y = y; }
    }
}
