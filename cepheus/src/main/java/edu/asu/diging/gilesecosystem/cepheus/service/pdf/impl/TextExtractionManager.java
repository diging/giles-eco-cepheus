package edu.asu.diging.gilesecosystem.cepheus.service.pdf.impl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.asu.diging.gilesecosystem.cepheus.exceptions.CepheusExtractionException;
import edu.asu.diging.gilesecosystem.cepheus.rest.DownloadFileController;
import edu.asu.diging.gilesecosystem.cepheus.service.IPropertiesManager;
import edu.asu.diging.gilesecosystem.cepheus.service.pdf.ITextExtractionManager;
import edu.asu.diging.gilesecosystem.requests.ICompletedTextExtractionRequest;
import edu.asu.diging.gilesecosystem.requests.IRequestFactory;
import edu.asu.diging.gilesecosystem.requests.ITextExtractionRequest;
import edu.asu.diging.gilesecosystem.requests.RequestStatus;
import edu.asu.diging.gilesecosystem.requests.exceptions.MessageCreationException;
import edu.asu.diging.gilesecosystem.requests.impl.CompletedTextExtractionRequest;
import edu.asu.diging.gilesecosystem.requests.kafka.IRequestProducer;

@Service
public class TextExtractionManager extends AExtractionManager implements ITextExtractionManager {

    @Autowired
    private IRequestFactory<ICompletedTextExtractionRequest, CompletedTextExtractionRequest> requestFactory;
    
    @Autowired
    private IRequestProducer requestProducer;
    
    
    @PostConstruct
    public void init() {
        requestFactory.config(CompletedTextExtractionRequest.class);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * edu.asu.diging.cepheus.service.pdf.impl.ITextExtractionManager#extractText
     * (edu.asu.diging.gilesecosystem.requests.ITextExtractionRequest)
     */
    @Override
    public void extractText(ITextExtractionRequest request) throws CepheusExtractionException {
        PDDocument pdfDocument;
        try {
            pdfDocument = PDDocument.load(downloadFile(request.getDownloadUrl()));
        } catch (IOException e) {
            throw new CepheusExtractionException(e);
        }

        String fileName = request.getFilename() + ".txt";
        Text extractedText = extractText(pdfDocument, request.getRequestId(),
                request.getDocumentId(), fileName);

        String restEndpoint = getRestEndpoint();
        
        int numPages = pdfDocument.getNumberOfPages();
        List<edu.asu.diging.gilesecosystem.requests.impl.Page> pages = new ArrayList<>();
        for (int i = 0; i < numPages; i++) {
            // if there is embedded text, let's use that one before OCRing
            if (extractedText != null) {
                Page page = extractPageText(pdfDocument, i, request.getRequestId(), request.getDocumentId(), request.getFilename());
                if (page != null) {
                    edu.asu.diging.gilesecosystem.requests.impl.Page requestPage = new edu.asu.diging.gilesecosystem.requests.impl.Page();
                    requestPage.setDownloadUrl(restEndpoint + DownloadFileController.GET_FILE_URL
                            .replace(DownloadFileController.REQUEST_ID_PLACEHOLDER, request.getRequestId())
                            .replace(DownloadFileController.DOCUMENT_ID_PLACEHOLDER, request.getDocumentId())
                            .replace(DownloadFileController.FILENAME_PLACEHOLDER, page.filename));
                    requestPage.setPathToFile(page.path);
                    requestPage.setFilename(page.filename);
                    requestPage.setPageNr(i);
                    pages.add(requestPage);
                }
            } 
        }

        try {
            pdfDocument.close();
        } catch (IOException e) {
            logger.error("Error closing document.", e);
        }
        
        ICompletedTextExtractionRequest completedRequest = null;
        try {
            completedRequest = requestFactory.createRequest(request.getUploadId());
        } catch (InstantiationException | IllegalAccessException e) {
            logger.error("Could not create request.", e);
            // this should never happen if used correctly
        }
        
        
        String fileEndpoint = restEndpoint + DownloadFileController.GET_FILE_URL
                .replace(DownloadFileController.REQUEST_ID_PLACEHOLDER, request.getRequestId())
                .replace(DownloadFileController.DOCUMENT_ID_PLACEHOLDER, request.getDocumentId())
                .replace(DownloadFileController.FILENAME_PLACEHOLDER, fileName);
        
        completedRequest.setDocumentId(request.getDocumentId());
        completedRequest.setFilename(request.getFilename());
        completedRequest.setRequestId(request.getRequestId());
        completedRequest.setStatus(RequestStatus.COMPLETE);
        completedRequest.setExtractionDate(OffsetDateTime.now(ZoneId.of("UTC")).toString());
        completedRequest.setPages(pages);
        
        if (extractedText != null) {
            completedRequest.setDownloadPath(extractedText.path);
            completedRequest.setSize(extractedText.size);
            completedRequest.setDownloadUrl(fileEndpoint);
            completedRequest.setTextFilename(fileName);
        }
        
        try {
            requestProducer.sendRequest(completedRequest, propertiesManager.getProperty(IPropertiesManager.KAFKA_EXTRACTION_COMPLETE_TOPIC));
        } catch (MessageCreationException e) {
            logger.error("Could not send message.", e);
        }
    }

    private Text extractText(PDDocument pdfDocument, String requestId,
            String documentId, String filename) {
        String docFolder = fileStorageManager.getAndCreateStoragePath(requestId,
                documentId, null);
        String filePath = docFolder + File.separator + filename;
        File fileObject = new File(filePath);
        try {
            fileObject.createNewFile();
        } catch (IOException e) {
            logger.error("Could not create file.", e);
            return null;
        }

        try {
            FileWriter writer = new FileWriter(fileObject);
            BufferedWriter bfWriter = new BufferedWriter(writer);
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.writeText(pdfDocument, bfWriter);
            bfWriter.close();
            writer.close();
        } catch (IOException e) {
            logger.error("Could not extract text.", e);
            return null;
        }

        String contents = null;
        try {
            contents = FileUtils.readFileToString(fileObject);
        } catch (IOException e) {
            logger.error("Could not get contents.", e);
        }

        if (contents == null || contents.trim().isEmpty()) {
            fileStorageManager.deleteFile(requestId, documentId, null, filename, true);
            return null;
        }
 
        Text text = new Text();
        text.size = fileObject.length();
        
        String relativePath = fileStorageManager.getFileFolderPathInBaseFolder(requestId, documentId, null);
        text.path = relativePath + File.separator + filename;
        
        return text;
    }

    private Page extractPageText(PDDocument pdfDocument, int pageNr, String requestId,
            String documentId, String filename) {

        String pageText = null;

        PDFTextStripper stripper;
        try {
            stripper = new PDFTextStripper();
            // pdfbox starts counting at 1 for the text extraction
            stripper.setStartPage(pageNr + 1);
            stripper.setEndPage(pageNr + 1);

            pageText = stripper.getText(pdfDocument);
        } catch (IOException e) {
            logger.error("Could not get contents of page " + pageNr + ".", e);
        }

        if (pageText == null) {
            return null;
        }

        return saveTextToFile(pageNr, requestId, documentId, pageText, filename,
                ".txt");
    }
    
    class Text {
        public String path;
        public long size;
    }
}
