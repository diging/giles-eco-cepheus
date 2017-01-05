package edu.asu.diging.gilesecosystem.cepheus.rest;

import java.io.IOException;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import edu.asu.diging.gilesecosystem.util.files.IFileStorageManager;

public class DownloadFileControllerTest {

    @Mock
    private IFileStorageManager storageManager;

    @Mock
    private HttpServletResponse response;

    @Mock
    private HttpServletRequest request;
    
    @Mock
    private ServletOutputStream stream;

    @InjectMocks
    private DownloadFileController controllerToTest;

    @Before
    public void setUp() {
        controllerToTest = new DownloadFileController();
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void test_getFile_doesNotExist() {
        String FILENAME = "test.jpg";
        String DOC_ID = "DOC1";
        String REQ_ID = "REQ1";
        
        Mockito.when(storageManager.getFileContent(REQ_ID, DOC_ID, null, FILENAME)).thenReturn(null);
        
        ResponseEntity<String> responseEntity = controllerToTest.getFile(FILENAME, DOC_ID, REQ_ID, response, request);
        Assert.assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
    }
    
    @Test
    public void test_getFile_fileExists() throws IOException {
        String FILENAME = "testImage.jpg";
        String DOC_ID = "DOC1";
        String REQ_ID = "REQ1";
        
        byte[] content = IOUtils.toByteArray(getClass().getResourceAsStream("/testImage.jpg"));
        Mockito.when(storageManager.getFileContent(REQ_ID, DOC_ID, null, FILENAME)).thenReturn(content);
        Mockito.when(response.getOutputStream()).thenReturn(stream);
        
        ResponseEntity<String> responseEntity = controllerToTest.getFile(FILENAME, DOC_ID, REQ_ID, response, request);
        Mockito.verify(storageManager).deleteFile(REQ_ID, DOC_ID, null, FILENAME, true);
        Mockito.verify(response).setContentType("image/jpeg");
        Mockito.verify(response).setContentLength(content.length);
        Mockito.verify(response).setHeader("Content-disposition", "filename=\"" + FILENAME + "\"");
        Mockito.verify(stream).write(content);
        
        Assert.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    }
}
