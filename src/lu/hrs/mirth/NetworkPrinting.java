package lu.hrs.mirth;

import java.awt.print.PrinterException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

import javax.print.DocFlavor;
import javax.print.SimpleDoc;
import javax.print.StreamPrintServiceFactory;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.PageRanges;
import javax.print.attribute.standard.Sides;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.printing.PDFPrintable;
import org.apache.pdfbox.printing.Scaling;

import com.itextpdf.html2pdf.HtmlConverter;

/**
 * Prints certain types of documents on a network printer.<br>
 * <br>
 * The specialty about this class is that the printer does not have to have the driver for the desired printers configured on the
 * client system. <br><br>
 * <i><b><u>This Class needs the following libraries:</u></b><br><br>
 * <u>For transforming PDF to postscript:</u><br>
 * pdfbox-2.0.jar<br>
 * fontbox-2.0.jar<br><br>
 * <u>For transforming html to PDF:</u><br>
 * commons-logging-1.1.1.jar<br>
 * html2pdf-2.1.1.jar<br>
 * io-7.1.4.jar<br>
 * kernel-7.1.4.jar<br>
 * layout-7.1.4.jar<br>
 * slf4j-api-1.7.25.jar<br>
 * slf4j-simple-1.7.25.jar<br>
 * styled-xml-parser-7.1.4.jar<br>
 * svg-7.1.4.jar<br>
 * </i><br>
 * (newer versions of the libraries might work as well)
 * 
 * @author ortwin.donak
 *
 */
public class NetworkPrinting {

	/**
	 * Sends a text to a network printer using UTF-8 encoding
	 * 
	 * @param text
	 *            The text that should be printed
	 * @param printerIdentifier
	 *            The ip or qualified name of the printer
	 * @throws Exception
	 *             View exception message for details
	 */
	public static void printText(String text, String printerIdentifier) throws Exception {
		printText(text, null, printerIdentifier);
	}

	/**
	 * Sends a text to a network printer
	 * 
	 * @param text
	 *            The text that should be printed
	 * @param encoding
	 *            The encoding of the text <i>(DEFAULT: "UTF-8")</i>
	 * @param printerIdentifier
	 *            The ip or qualified name of the printer
	 * @throws Exception
	 *             View exception message for details
	 */
	public static void printText(String text, String printerIdentifier, String encoding) throws Exception {
		if (encoding == null) {
			encoding = "URF-8";
		}

		// try to create a connection to the printer
		Socket printer = new Socket(printerIdentifier, 9100);
		// and get a handle to the output stream
		DataOutputStream printerFeed = new DataOutputStream(printer.getOutputStream());
		printerFeed.write(text.getBytes(encoding));

		// clean up
		printer.close();
	}

	/**
	 * Prints an html file on a network printer
	 * 
	 * @param html
	 *            The self-containing html file (images, etc. must be included)
	 * @param printerIdentifier
	 *            The fully qualified name or ip of the network printer
	 * @throws Exception
	 *             View exception message for details
	 */
	public static void printHtml(String html, String printerIdentifier) throws Exception {
		// convert the html page to pdf
		ByteArrayOutputStream pdf = new ByteArrayOutputStream();
		HtmlConverter.convertToPdf(new ByteArrayInputStream(html.getBytes(StandardCharsets.UTF_8)), pdf);
		// print the pdf file
		printPdf(pdf.toByteArray(), printerIdentifier);
	}

	/**
	 * Prints a base64-encoded document on a network printer. The network printer does not need to be configured at the server.<br>
	 * <br>
	 * <i>This functionality needs the following Apache libraries: pdfbox-2.0.jar, fontbox-2.0.jar (or higher)</i>
	 * 
	 * @param encodedDocumentToPrint
	 *            The base64-encoded document that should be printed
	 * @param printerIdentifier
	 *            The name or ip of the network printer to which the document should be sent
	 * @throws Exception
	 *             Mainly if the printer is not known or not accessible,
	 */
	public static void printPdfBase64(String encodedDocumentToPrint, String printerIdentifier) throws Exception {
		printPdf(Base64.getDecoder().decode(encodedDocumentToPrint), printerIdentifier);
	}

	/**
	 * Prints a PDF-document on a network printer. The network printer does not need to be configured at the server.<br>
	 * <br>
	 * <i>This functionality needs the following Apache libraries: pdfbox-2.0.jar, fontbox-2.0.jar (or higher)</i><br>
	 * <br>
	 * <p style="color:Gainsboro;">
	 * This is necessary as we bypassing the printers driver (as there is none installed on the requesting system). Thus, the function has to take
	 * care for transforming the file to postscript and to directly communicate with the printer. For doing so, it hijacks an existing postscript
	 * driver of another printer.
	 * </p>
	 * 
	 * @param documentToPrint
	 *            The PDF-document that should be printed
	 * @param printerIdentifier
	 *            The name or ip of the network printer to which the document should be sent
	 * @throws Exception
	 *             Mainly if the printer is not known or not accessible
	 */
	public static void printPdf(byte[] documentToPrint, String printerIdentifier) throws Exception {

		// speed the rendering up
		System.setProperty("sun.java2d.cmm", "sun.java2d.cmm.kcms.KcmsServiceProvider");

		// check for any driver that are capable of postscript printing
		DocFlavor flavor = DocFlavor.SERVICE_FORMATTED.PRINTABLE;
		StreamPrintServiceFactory[] factories = StreamPrintServiceFactory.lookupStreamPrintServiceFactories(flavor,
				DocFlavor.BYTE_ARRAY.POSTSCRIPT.getMimeType());
		// are there any?
		if (factories.length == 0) {
			// nope - sorry but won't work on this system. Install postscript printer driver first...
			throw new PrinterException("No PostScript factories available");
		}

		// try to create a connection to the printer
		Socket printer = new Socket(printerIdentifier, 9100);
		// and get a handle to the output stream
		DataOutputStream printerFeed = new DataOutputStream(printer.getOutputStream());

		// now get the attachment content
		ByteArrayInputStream documentData = new ByteArrayInputStream(documentToPrint);
		// load the pdf document
		PDDocument document = PDDocument.load(documentData);
		documentData.close();

		// Attributes are specified by https://docs.oracle.com/javase/7/docs/api/
		// see package javax.print.attribute.standard
		PrintRequestAttributeSet printingAttributes = new HashPrintRequestAttributeSet();
		// set European page size
		printingAttributes.add(MediaSizeName.ISO_A4);
		// printing should be double sided
		printingAttributes.add(Sides.DUPLEX);
		// set number of pages to be printed
		printingAttributes.add(new PageRanges(1, document.getNumberOfPages()));
		// create an implicit print job and print the document to the anonymous network printer
		factories[0].getPrintService(printerFeed).createPrintJob()
				.print(new SimpleDoc(new PDFPrintable(document, Scaling.ACTUAL_SIZE, false), flavor, null), printingAttributes);

		// clean up
		printer.close();
		document.close();
	}

	/**
	 * Prints a document on a network printer
	 * 
	 * @param documentToPrint
	 *            The document that should be printed
	 * @param printerIdentifier
	 *            The name or ip of the network printer to which the document should be sent
	 * @throws Exception
	 *             Mainly if the printer is not known or not accessible
	 */
	public static void printOnNetworkPrinter(Object documentToPrint, String printerIdentifier) throws Exception {

		// check for any driver that are capable of postscript printing
		DocFlavor flavor = DocFlavor.SERVICE_FORMATTED.PRINTABLE;
		StreamPrintServiceFactory[] factories = StreamPrintServiceFactory.lookupStreamPrintServiceFactories(flavor,
				DocFlavor.BYTE_ARRAY.POSTSCRIPT.getMimeType());
		// are there any?
		if (factories.length == 0) {
			// nope - sorry but won't work on this system. Install postscript printer driver first...
			throw new PrinterException("No PostScript factories available");
		}

		// try to create a connection to the printer
		Socket printer = new Socket(printerIdentifier, 9100);
		// and get a handle to the output stream
		DataOutputStream printerFeed = new DataOutputStream(printer.getOutputStream());

		// now get the document content
		ByteArrayInputStream document = new ByteArrayInputStream((byte[]) documentToPrint);

		// Attributes are specified by https://docs.oracle.com/javase/7/docs/api/
		// see package javax.print.attribute.standard
		PrintRequestAttributeSet printingAttributes = new HashPrintRequestAttributeSet();
		// set European page size
		printingAttributes.add(MediaSizeName.ISO_A4);
		// printing should be double sided
		printingAttributes.add(Sides.DUPLEX);
		// set number of pages to be printed
		// printingAttributes.add(new PageRanges(1, document.getNumberOfPages()));
		// just 1 printout
		printingAttributes.add(new Copies(1));
		// create an implicit print job and print the document to the anonymous network printer
		factories[0].getPrintService(printerFeed).createPrintJob().print(new SimpleDoc(document, flavor, null), printingAttributes);

		// clean up
		printer.close();
		document.close();
	}

	public static void main(String[] args) throws Exception {
		
		// String printer = "SOME_PRINTER.YOUR.DOMAIN"; (e.g. "192.168.12.10" or "PrinterLab02.someHospital.com")
		
		// The following example loads a pdf from file system 
		// byte[] testfile = Files.readAllBytes(Paths.get("c:/temp/PrintTest.pdf"));
		// NetworkPrinting.printPdf(testfile, printer);
		
		// IF you want to do it on the fly, use
		// NetworkPrinting.printPdfBase64(Base64Attachment, printer);
		
		// You can also print an HTML document
		// NetworkPrinting.printHtml(html, printer);
		
		// Or simply text:
		// NetworkPrinting.printText("### THIS IS A TEST ###\n\nTEST TEST TEST", printer);
	}

}
