package edu.asu.diging.gilesecosystem.cepheus.kafka;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import edu.asu.diging.gilesecosystem.cepheus.exceptions.CepheusExtractionException;
import edu.asu.diging.gilesecosystem.cepheus.service.IPropertiesManager;
import edu.asu.diging.gilesecosystem.cepheus.service.pdf.IImageExtractionManager;
import edu.asu.diging.gilesecosystem.cepheus.service.pdf.ITextExtractionManager;
import edu.asu.diging.gilesecosystem.requests.impl.ImageExtractionRequest;
import edu.asu.diging.gilesecosystem.requests.impl.TextExtractionRequest;

public class ExtractionRequestReceiverTest {

    @Mock
    private ITextExtractionManager textExtractionManager;

    @Mock
    private IImageExtractionManager imageExtractionManager;

    @Mock
    private IPropertiesManager propertiesManager;

    @InjectMocks
    private ExtractionRequestReceiver receiverToTest;
    
    private String REQID = "REQID";
    private String UPID = "UPID";
    private String DOCID = "DOCID";
    private String FILEID = "FILEID";
    private String URL = "url";
    private String PATH = "Path";
    private String FILENAME = "filename";

    @Before
    public void setUp() {
        receiverToTest = new ExtractionRequestReceiver();
        MockitoAnnotations.initMocks(this);

        Mockito.when(
                propertiesManager.getProperty(IPropertiesManager.KAFKA_EXTRACTION_TOPIC))
                .thenReturn("geco.requests.pdf.extract");
        Mockito.when(
                propertiesManager
                        .getProperty(IPropertiesManager.KAFKA_IMAGE_EXTRACTION_TOPIC))
                .thenReturn("geco.requests.pdf.toimages");

    }

    @Test
    public void test_receiveMessage_textExtraction() throws CepheusExtractionException {
        String TYPE = "giles.request_type.text_extraction";
        
        receiverToTest
                .receiveMessage(
                        "{\"requestId\":\"" + REQID + "\",\"requestType\":\"" + TYPE + "\"," + 
                         "\"uploadId\":\"" + UPID + "\",\"documentId\":\"" + DOCID + "\"," + 
                         "\"fileId\":\"" + FILEID + "\",\"downloadUrl\":\"" + URL + "\"," + 
                         "\"downloadPath\":\""+ PATH + "\",\"filename\":\"" + FILENAME + "\"}",
                        "geco.requests.pdf.extract");
        ArgumentCaptor<TextExtractionRequest> argumentCaptor = ArgumentCaptor.forClass(TextExtractionRequest.class);
        Mockito.verify(textExtractionManager).extractText(argumentCaptor.capture());
        
        TextExtractionRequest request = argumentCaptor.getValue();
        Assert.assertEquals(REQID, request.getRequestId());
        Assert.assertEquals(TYPE, request.getType());
        Assert.assertEquals(UPID, request.getUploadId());
        Assert.assertEquals(DOCID, request.getDocumentId());
        Assert.assertEquals(FILEID, request.getFileId());
        Assert.assertEquals(URL, request.getDownloadUrl());
        Assert.assertEquals(PATH, request.getDownloadPath());
        Assert.assertEquals(FILENAME, request.getFilename());
    }
    
    @Test
    public void test_receiveMessage_imageExtraction() throws CepheusExtractionException {
        String TYPE = "giles.request_type.image_extraction";
        
        receiverToTest
                .receiveMessage(
                        "{\"requestId\":\"" + REQID + "\",\"requestType\":\"" + TYPE + "\"," + 
                         "\"uploadId\":\"" + UPID + "\",\"documentId\":\"" + DOCID + "\"," + 
                         "\"fileId\":\"" + FILEID + "\",\"downloadUrl\":\"" + URL + "\"," + 
                         "\"downloadPath\":\""+ PATH + "\",\"filename\":\"" + FILENAME + "\"}",
                        "geco.requests.pdf.toimages");
        ArgumentCaptor<ImageExtractionRequest> argumentCaptor = ArgumentCaptor.forClass(ImageExtractionRequest.class);
        Mockito.verify(imageExtractionManager).extractImages(argumentCaptor.capture());
        
        ImageExtractionRequest request = argumentCaptor.getValue();
        Assert.assertEquals(REQID, request.getRequestId());
        Assert.assertEquals(TYPE, request.getType());
        Assert.assertEquals(UPID, request.getUploadId());
        Assert.assertEquals(DOCID, request.getDocumentId());
        Assert.assertEquals(FILEID, request.getFileId());
        Assert.assertEquals(URL, request.getDownloadUrl());
        Assert.assertEquals(PATH, request.getDownloadPath());
        Assert.assertEquals(FILENAME, request.getFilename());
    }
}
