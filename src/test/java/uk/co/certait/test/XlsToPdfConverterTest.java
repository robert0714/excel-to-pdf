package uk.co.certait.test;
 
import java.io.InputStream; 
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.w3c.dom.Document;

class XlsToPdfConverterTest {
	private ApplicationContext ctx; 
	
	XlsToPdfConverter sample ; 
	@BeforeEach
	protected void setUp() throws Exception {
		this.ctx = new AnnotationConfigApplicationContext();
		this.sample = new XlsToPdfConverter();
	}

	@AfterEach
	protected void tearDown() throws Exception {
	}

	@Test
	public void testMain() throws  Exception {
		InputStream inputStream = this.ctx.getResource("classpath:test.xls")
				.getInputStream();
		
		//only works with .xls - see the other example for .xlsx files
		Document doc = org.apache.poi.hssf.converter.ExcelToHtmlConverter.process(inputStream);

		this.sample.debugHtml(doc);
		this.sample.writePdf(doc);
	} 

}
