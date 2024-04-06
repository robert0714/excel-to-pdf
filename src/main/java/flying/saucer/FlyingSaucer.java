package flying.saucer;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.xhtmlrenderer.layout.SharedContext;
import org.xhtmlrenderer.pdf.ITextRenderer;
import org.xhtmlrenderer.swing.Java2DRenderer; 
import org.w3c.tidy.Tidy;

import lombok.extern.slf4j.Slf4j;
@Slf4j
public class FlyingSaucer {
	public static void generateHtmlToPdf(final File inputHTML , final  File outputPdf) throws Exception {        
        Document inputHtml = createWellFormedHtml(inputHTML); 
        xhtmlToPdf(inputHtml, outputPdf);
    }

    private static Document createWellFormedHtml(File inputHTML) throws IOException {
        Document document = Jsoup.parse(inputHTML, "UTF-8");
        document.outputSettings()
            .syntax(Document.OutputSettings.Syntax.xml);
        return document;
    }

    private static void xhtmlToPdf(Document xhtml, File outputPdf) throws Exception {
        try (OutputStream outputStream = new FileOutputStream(outputPdf)) {
            ITextRenderer renderer = new ITextRenderer();
            SharedContext sharedContext = renderer.getSharedContext();
            sharedContext.setPrint(true);
            sharedContext.setInteractive(false);
            sharedContext.setReplacedElementFactory(new CustomElementFactoryImpl());
            renderer.setDocumentFromString(xhtml.html());
             
            renderer.layout();
            renderer.createPDF(outputStream);
        }
    }

	public static BufferedImage imageRender( InputStream htmlStream ) {
		//https://flyingsaucerproject.github.io/flyingsaucer/r8/guide/users-guide-R8.html#xil_29
		// Generate an image from a file:
		int width = 800, height = 600;
		
		// can specify width alone, or width + height
		// constructing does not render; not until getImage() is called
		Tidy tidy = new Tidy();
		
		org.w3c.dom.Document doc = tidy.parseDOM(new InputStreamReader(htmlStream, StandardCharsets.UTF_8), null);
		Java2DRenderer renderer = new Java2DRenderer(doc, width, height);
  

		// this renders and returns the image, which is stored in the J2R; will not
		// be re-rendered, calls to getImage() return the same instance
		BufferedImage img = renderer.getImage();
		return img;
	}
}
