package uk.co.certait.test;

import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage; 
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream; 
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

 
import flying.saucer.*; 

import javax.imageio.ImageIO;
import javax.swing.JEditorPane;

import org.apache.commons.io.FileUtils;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.util.IOUtils;
import org.apache.poi.xssf.usermodel.XSSFWorkbook; 
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import gui.ava.html.image.generator.HtmlImageGenerator;
import lombok.extern.slf4j.Slf4j;
 
@Slf4j
class ExcelToHtmlConverterTest {
	private ApplicationContext ctx; 
	private File tmp ; 
	@BeforeEach
	protected void setUp() throws Exception { 
		this.ctx = new AnnotationConfigApplicationContext();
		this.tmp = new File( FileUtils.getTempDirectory(),"testExcel2Images");
	}

	@AfterEach
	protected void tearDown() throws Exception {
	}

	protected String basicExcelToHtmlTag() {
		StringWriter stringWriter = new StringWriter();
    	
        try (InputStream inputStream = this.ctx.getResource("classpath:test.xlsx")
				.getInputStream();){
        	Workbook tmp = WorkbookFactory.create(inputStream); 
        	 
        	XSSFWorkbook workbook =  (XSSFWorkbook) tmp;
        	
        	ExcelToHtmlConverter convertor = ExcelToHtmlConverter.create(workbook, stringWriter);
        	convertor.setCompleteHTML(true);
        	
        	Sheet sheet = tmp.getSheetAt(0);
//        	convertor.printSheet(sheet);  
        	convertor.printPage();
        	
        } catch ( Exception exc ) {
        	log.error(exc.getMessage() ,exc);
             
        } 		
        System.out.print(tmp.getAbsolutePath());
        return stringWriter.toString() ;
	}
	
	@Test
	public void testExcel2Html() throws Exception {	
		File htmlFile = new File(tmp.getAbsolutePath() +File.separator+"test02.html");	
		
		String htmlTags = basicExcelToHtmlTag() ;
		FileUtils.writeStringToFile(htmlFile, htmlTags,StandardCharsets.UTF_8);
		
//		String htmlTags = FileUtils.readFileToString(htmlFile,StandardCharsets.UTF_8);
		
		File pngFilev1 = new File(tmp.getAbsolutePath() +File.separator+"test02-v1.png");
		
		BufferedImage image = imageconvert2Img(htmlTags);
		ImageIO.write(image, "png", pngFilev1);
		
		HtmlImageGenerator imageGenerator = new HtmlImageGenerator();
        imageGenerator.loadHtml(htmlTags);
        imageGenerator.saveAsImage(pngFilev1);
        
        File pdfFile = new File(tmp.getAbsolutePath() +File.separator+"test02.pdf");
        
        File pngFilev2 = new File(tmp.getAbsolutePath() +File.separator+"test02-v2.png");
         
        FlyingSaucer.generateHtmlToPdf(htmlFile, pdfFile);
     
        image =  FlyingSaucer.imageRender(  new  FileInputStream(htmlFile));
        ImageIO.write(image, "png", pngFilev2);
        
        //https://www.baeldung.com/java-html-to-pdf
//        OpenHtml.generateHtmlToPdf(htmlFile, pdfFile);
	}
	
	public BufferedImage imageconvert2Img(String htmlTags) {
		int width = 800, height = 600;

		final GraphicsEnvironment lge = GraphicsEnvironment.getLocalGraphicsEnvironment();

		// JVM argument: -Djava.awt.headless=false
		final GraphicsDevice dsd = lge.getDefaultScreenDevice();

		final GraphicsConfiguration dfc = dsd.getDefaultConfiguration();

		BufferedImage image = dfc.createCompatibleImage(width, height);

		Graphics graphics = image.createGraphics();

		JEditorPane jep = new JEditorPane("text/html", htmlTags);
		jep.setSize(width, height);
		jep.print(graphics);
		return image;
	}
    
	@Test
	@Disabled
	public void testMain() throws Exception {
		InputStream inputStream = this.ctx.getResource("classpath:test.xlsx")
				.getInputStream();
		IOUtils.setByteArrayMaxOverride(1000000000);
		byte[] bytes = IOUtils.toByteArray(inputStream);
		File srcXSSFWB = new File(tmp.getAbsolutePath() +File.separator+"test.xlsx");
		File htmlFile = new File(tmp.getAbsolutePath() +File.separator+"test02.html");
		FileUtils.writeByteArrayToFile(srcXSSFWB, bytes);
		String[] args = {srcXSSFWB.getAbsolutePath(),htmlFile.getAbsolutePath()};
		
		ExcelToHtmlConverter.main(args);;
	}

}
