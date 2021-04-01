package edu.asu.diging.gilesecosystem.cepheus.service.pdf.impl;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.asu.diging.gilesecosystem.cepheus.exceptions.CepheusExtractionException;
import edu.asu.diging.gilesecosystem.cepheus.rest.DownloadFileController;
import edu.asu.diging.gilesecosystem.cepheus.service.Properties;
import edu.asu.diging.gilesecosystem.cepheus.service.pdf.IImageExtractionManager;
import edu.asu.diging.gilesecosystem.cepheus.service.progress.IProgressManager;
import edu.asu.diging.gilesecosystem.cepheus.service.progress.ProgressPhase;
import edu.asu.diging.gilesecosystem.requests.ICompletedImageExtractionRequest;
import edu.asu.diging.gilesecosystem.requests.IImageExtractionRequest;
import edu.asu.diging.gilesecosystem.requests.IRequestFactory;
import edu.asu.diging.gilesecosystem.requests.PageStatus;
import edu.asu.diging.gilesecosystem.requests.RequestStatus;
import edu.asu.diging.gilesecosystem.requests.exceptions.MessageCreationException;
import edu.asu.diging.gilesecosystem.requests.impl.CompletedImageExtractionRequest;
import edu.asu.diging.gilesecosystem.requests.kafka.IRequestProducer;
import edu.asu.diging.gilesecosystem.util.files.IFileStorageManager;
import edu.asu.diging.gilesecosystem.util.properties.IPropertiesManager;
import edu.asu.diging.gilesecosystem.septemberutil.properties.MessageType;
import edu.asu.diging.gilesecosystem.septemberutil.service.ISystemMessageHandler;

@Service
public class ImageExtractionManager extends AExtractionManager implements IImageExtractionManager {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private IPropertiesManager propertiesManager;

    @Autowired
    private ISystemMessageHandler messageHandler;

    @Autowired
    private IFileStorageManager fileStorageManager;

    @Autowired
    private IRequestFactory<ICompletedImageExtractionRequest, CompletedImageExtractionRequest> requestFactory;

    @Autowired
    private IRequestProducer requestProducer;
    
    @Autowired
    private IProgressManager progressManager;

    @PostConstruct
    public void init() {
        /*
         * Recommended fix for performance issues due to colors: "Due to the change of
         * the java color management module towards “LittleCMS”, users can experience
         * slow performance in color operations. Solution: disable LittleCMS in favour
         * of the old KCMS (Kodak Color Management System)"
         */
        System.setProperty("sun.java2d.cmm", "sun.java2d.cmm.kcms.KcmsServiceProvider");

        requestFactory.config(CompletedImageExtractionRequest.class);
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.asu.diging.gilesecosystem.cepheus.service.pdf.impl.
     * IImageExtractionManager #extractImages(edu.asu.diging.gilesecosystem.requests
     * .IImageExtractionRequest)
     */
    @Override
    public void extractImages(IImageExtractionRequest request) throws CepheusExtractionException {
        logger.info("Extracting images for: " + request.getDownloadUrl());
        
        String dpi = propertiesManager.getProperty(Properties.PDF_TO_IMAGE_DPI).trim();
        String type = propertiesManager.getProperty(Properties.PDF_TO_IMAGE_TYPE).trim();
        String format = propertiesManager.getProperty(Properties.PDF_TO_IMAGE_FORMAT).trim();
        
        progressManager.setPhase(ProgressPhase.RAMP_UP);
        progressManager.startNewRequest(request);

        PDDocument pdfDocument = null;
        RequestStatus status = RequestStatus.COMPLETE;
        try {
            pdfDocument = PDDocument.load(new ByteArrayInputStream(downloadFile(request.getDownloadUrl())),
                    MemoryUsageSetting.setupTempFileOnly());
        } catch (IOException e) {
            messageHandler.handleMessage("Could not extract text.", e, MessageType.ERROR);
            status = RequestStatus.FAILED;
        }

        List<edu.asu.diging.gilesecosystem.requests.impl.Page> pages = new ArrayList<>();
        if (pdfDocument != null) {
            progressManager.setPhase(ProgressPhase.PROCESSING);
            
            int numPages = pdfDocument.getNumberOfPages();
            progressManager.setTotalPages(numPages);
            PDFRenderer renderer = new PDFRenderer(pdfDocument);

            String restEndpoint = getRestEndpoint();

            for (int i = 0; i < numPages; i++) {
                progressManager.updateCurrentPage(i+1);
                edu.asu.diging.gilesecosystem.requests.impl.Page requestPage = new edu.asu.diging.gilesecosystem.requests.impl.Page();
                requestPage.setPageNr(i);

                try {
                    BufferedImage image = renderer.renderImageWithDPI(i, Float.parseFloat(dpi),
                            ImageType.valueOf(type));
                    String fileName = request.getFilename() + "." + i + "." + format;
                    Page pageImage = saveImage(request.getRequestId(), request.getDocumentId(), image, fileName);
                    
                    requestPage.setDownloadUrl(
                            restEndpoint + DownloadFileController.GET_FILE_URL
                                    .replace(
                                            DownloadFileController.REQUEST_ID_PLACEHOLDER,
                                            request.getRequestId())
                                    .replace(
                                            DownloadFileController.DOCUMENT_ID_PLACEHOLDER,
                                            request.getDocumentId())
                                    .replace(DownloadFileController.FILENAME_PLACEHOLDER,
                                            pageImage.filename));
                    requestPage.setPathToFile(pageImage.path);
                    requestPage.setFilename(pageImage.filename);
                    requestPage.setContentType(pageImage.contentType);
                    requestPage.setSize(pageImage.size);
                    requestPage.setStatus(PageStatus.COMPLETE);
                } catch (IOException | RuntimeException e) {
                    messageHandler.handleMessage("Could not render image.", e, MessageType.ERROR);
                    requestPage.setStatus(PageStatus.FAILED);
                    requestPage.setErrorMsg(e.getMessage());
                }

                pages.add(requestPage);
            }
            
            try {
                pdfDocument.close();
            } catch (IOException e) {
                messageHandler.handleMessage("Error closing document.", e, MessageType.ERROR);
            }
        }

        progressManager.setPhase(ProgressPhase.WIND_DOWN);
        ICompletedImageExtractionRequest completedRequest = null;
        try {
            completedRequest = requestFactory.createRequest(request.getRequestId(), request.getUploadId());
        } catch (InstantiationException | IllegalAccessException e) {
            messageHandler.handleMessage("Could not create request.", e, MessageType.ERROR);
            // this should never happen if used correctly
        }

        completedRequest.setDocumentId(request.getDocumentId());
        completedRequest.setStatus(status);
        completedRequest.setExtractionDate(OffsetDateTime.now(ZoneId.of("UTC")).toString());
        completedRequest.setPages(pages);

        progressManager.setPhase(ProgressPhase.DONE);
        try {
            requestProducer.sendRequest(completedRequest,
                    propertiesManager.getProperty(Properties.KAFKA_IMAGE_EXTRACTION_COMPLETE_TOPIC));
        } catch (MessageCreationException e) {
            messageHandler.handleMessage("Could not send message.", e, MessageType.ERROR);
        }
        
        progressManager.reset();
    }

    private Page saveImage(String requestId, String documentId, BufferedImage image, String fileName)
            throws IOException, FileNotFoundException {
        String dpi = propertiesManager.getProperty(Properties.PDF_TO_IMAGE_DPI).trim();
        String format = propertiesManager.getProperty(Properties.PDF_TO_IMAGE_FORMAT).trim();

        String dirFolder = fileStorageManager.getAndCreateStoragePath(requestId, documentId, null);
        String filePath = dirFolder + File.separator + fileName;
        File fileObject = new File(filePath);
        OutputStream output = new FileOutputStream(fileObject);
        boolean success = false;
        try {
            success = ImageIOUtil.writeImage(image, format, output, new Integer(dpi));
        } finally {
            output.close();
        }
        if (!success) {
            return null;
        }

        Page page = new Page(dirFolder + File.separator + fileName, fileName);
        page.contentType = "image/" + format;
        page.size = fileObject.length();

        return page;
    }
}
