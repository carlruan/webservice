package edu.neu.webapp.service;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.AmazonS3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


@Service
public class S3BucketStorageService {
    @Autowired
    private AmazonS3 amazonS3Client;

    private Logger logger = LoggerFactory.getLogger(S3BucketStorageService.class);

    @Value("${bucketName}")
    private String bucketName;
    public PutObjectResult uploadFile(String keyName, MultipartFile file) {
        PutObjectResult putObjectResult = null;
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            putObjectResult = amazonS3Client.putObject(bucketName, keyName, file.getInputStream(), metadata);
        } catch (Exception e) {
            logger.error("IOException: " + e.getMessage());
        }
        return putObjectResult;
    }

    public String deleteFile(final String fileName) {
        amazonS3Client.deleteObject(bucketName, fileName);
        return "Deleted File: " + fileName;
    }
}
