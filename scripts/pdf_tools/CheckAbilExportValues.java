import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDCheckBox;

import java.io.File;

public class CheckAbilExportValues {
    public static void main(String[] args) throws Exception {
        PDDocument doc = Loader.loadPDF(new File("src/main/resources/interactive_sheet.pdf"));
        PDAcroForm form = doc.getDocumentCatalog().getAcroForm();
        for (int i = 73; i <= 77; i++) {
            PDCheckBox cb = (PDCheckBox) form.getField("dot" + i);
            System.out.println("dot" + i + " OnValue: " + cb.getOnValue() + " exportValues: " + cb.getExportValues());
        }
        for (int i = 1; i <= 5; i++) {
            PDCheckBox cb = (PDCheckBox) form.getField("dot" + i);
            System.out.println("dot" + i + " OnValue: " + cb.getOnValue() + " exportValues: " + cb.getExportValues());
        }
        doc.close();
    }
}
