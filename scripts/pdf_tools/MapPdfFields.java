import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;

import java.io.File;
import java.util.*;

public class MapPdfFields {
    public static void main(String[] args) throws Exception {
        PDDocument doc = Loader.loadPDF(new File("src/main/resources/interactive_sheet.pdf"));
        PDAcroForm form = doc.getDocumentCatalog().getAcroForm();
        
        List<FieldInfo> fields = new ArrayList<>();
        
        for (PDField field : form.getFields()) {
            String name = field.getFullyQualifiedName();
            String type = field.getFieldType();
            List<PDAnnotationWidget> widgets = field.getWidgets();
            if (widgets != null && !widgets.isEmpty()) {
                float y = widgets.get(0).getRectangle().getLowerLeftY();
                float x = widgets.get(0).getRectangle().getLowerLeftX();
                fields.add(new FieldInfo(name, type, x, y));
            }
        }
        
        fields.sort(Comparator.comparing((FieldInfo f) -> -Math.round(f.y / 5.0f) * 5.0f)
                              .thenComparing(f -> f.x));
                              
        for (FieldInfo f : fields) {
            System.out.println(String.format("Y:%5.1f, X:%5.1f, Type:%s -> %s", f.y, f.x, f.type, f.name));
        }
        
        doc.close();
    }
    
    static class FieldInfo {
        String name, type;
        float x, y;
        FieldInfo(String n, String t, float x, float y) { this.name = n; this.type = t; this.x = x; this.y = y; }
    }
}
