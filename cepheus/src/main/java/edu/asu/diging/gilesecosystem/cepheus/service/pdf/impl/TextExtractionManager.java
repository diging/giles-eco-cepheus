package edu.asu.diging.gilesecosystem.cepheus.service.pdf.impl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import edu.asu.diging.gilesecosystem.cepheus.exceptions.CepheusTextExtractionException;
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
import edu.asu.diging.gilesecosystem.util.files.IFileStorageManager;

@Service
public class TextExtractionManager implements ITextExtractionManager {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private IPropertiesManager propertiesManager;

    @Autowired
    private IFileStorageManager fileStorageManager;
    
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
    public void extractText(ITextExtractionRequest request) throws CepheusTextExtractionException {
        PDDocument pdfDocument;
        try {
            pdfDocument = PDDocument.load(downloadFile(request.getDownloadUrl()));
        } catch (IOException e) {
            throw new CepheusTextExtractionException(e);
        }

        String fileName = request.getFilename() + ".txt";
        String pathMainText = extractText(pdfDocument, request.getRequestId(),
                request.getDocumentId(), fileName);

        String restEndpoint = propertiesManager.getProperty(IPropertiesManager.CEPHEUS_URL);
        if (restEndpoint.endsWith("/")) {
            restEndpoint = restEndpoint.substring(0, restEndpoint.length()-1);
        }
        
        int numPages = pdfDocument.getNumberOfPages();
        List<String> pagePaths = new ArrayList<String>();
        List<String> pageUrls = new ArrayList<>();
        for (int i = 0; i < numPages; i++) {
            // if there is embedded text, let's use that one before OCRing
            if (pathMainText != null) {
                Page page = extractPageText(pdfDocument, i, request.getRequestId(), request.getDocumentId(), request.getFilename());
                if (page != null) {
                    pagePaths.add(page.path);
                    pageUrls.add(restEndpoint + DownloadFileController.GET_FILE_URL
                        .replace(DownloadFileController.REQUEST_ID_PLACEHOLDER, request.getRequestId())
                        .replace(DownloadFileController.DOCUMENT_ID_PLACEHOLDER, request.getDocumentId())
                        .replace(DownloadFileController.FILENAME_PLACEHOLDER, page.filename));
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
        completedRequest.setDownloadPath(pathMainText);
        completedRequest.setDownloadUrl(fileEndpoint);
        completedRequest.setPagesDownloadPaths(pagePaths);
        completedRequest.setPagesDownloadUrls(pageUrls);
        completedRequest.setFilename(request.getFilename());
        completedRequest.setRequestId(request.getRequestId());
        completedRequest.setStatus(RequestStatus.COMPLETE);
        completedRequest.setExtractionDate(OffsetDateTime.now(ZoneId.of("UTC")).toString());
        
        try {
            requestProducer.sendRequest(completedRequest, propertiesManager.getProperty(IPropertiesManager.KAFKA_EXTRACTION_COMPLETE_TOPIC));
        } catch (MessageCreationException e) {
            logger.error("Could not send message.", e);
        }
    }

    private String extractText(PDDocument pdfDocument, String requestId,
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

        String relativePath = fileStorageManager.getFileFolderPathInBaseFolder(requestId, documentId, null);
        return relativePath + File.separator + filename;
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

    protected Page saveTextToFile(int pageNr, String requestId,
            String documentId, String pageText, String filename, String fileExtentions) {
        String docFolder = fileStorageManager.getAndCreateStoragePath(requestId,
                documentId, null);

        if (pageNr > -1) {
            filename = filename + "." + pageNr;
        }

        if (!fileExtentions.startsWith(".")) {
            fileExtentions = "." + fileExtentions;
        }
        filename = filename + fileExtentions;

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
            bfWriter.write(pageText);
            bfWriter.close();
            writer.close();
        } catch (IOException e) {
            logger.error("Could not write text to file.", e);
            return null;
        }

        String relativePath = fileStorageManager.getFileFolderPathInBaseFolder(requestId, documentId, null);
        return new Page(relativePath + File.separator + filename, filename);
    }

    private byte[] downloadFile(String url) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new ByteArrayHttpMessageConverter());

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM));
        headers.set(
                "Authorization",
                "token "
                        + propertiesManager
                                .getProperty(IPropertiesManager.GILES_ACCESS_TOKEN));
        HttpEntity<String> entity = new HttpEntity<String>(headers);

        ResponseEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.GET,
                entity, byte[].class);
        if (response.getStatusCode().equals(HttpStatus.OK)) {
            return response.getBody();
        }
        return null;
    }
    
    class Page {
        public String path;
        public String filename;
        
        public Page(String path, String filename) {
            super();
            this.path = path;
            this.filename = filename;
        }  
    }
}
