package uk.co.certait.test;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.PrintWriter;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import gui.ava.html.image.generator.HtmlImageGenerator;
import lombok.extern.slf4j.Slf4j;
@Slf4j
class XlsxToPdfConverterTwoTest {
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

	@Test
	public void testMain() throws Exception {
		try(InputStream in = this.ctx.getResource("classpath:test.xlsx").getInputStream();){
			File outFile = new File(this.tmp, "test-xlsx.html");
			PrintWriter out = new PrintWriter(new FileWriter(outFile));
			
			// this class is based on code found at
			// https://svn.apache.org/repos/asf/poi/trunk/src/examples/src/org/apache/poi/ss/examples/html/ToHtml.java
			// and will convert .xlsx files
			ExcelToHtmlConverter toHtml = ExcelToHtmlConverter.create(in, out);
			toHtml.setCompleteHTML(true);
			toHtml.printPage();
			
			// rather than writing to file get the HTML in memory and use
			// FlyingSaucer or OpenHTMlToPdf

			in.close();
		}
		 
		

		
	}

}
