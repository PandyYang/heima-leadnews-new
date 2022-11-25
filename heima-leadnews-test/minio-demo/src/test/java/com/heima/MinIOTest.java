package com.heima;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class MinIOTest {

    public static void main(String[] args) throws Exception {

        MinioClient minioClient = MinioClient.builder().credentials("admin", "admin123456").
                endpoint("http://hadoop102:9000").build();

        FileInputStream fileInputStream = new FileInputStream("E:\\IdeaProjects\\heima-leadnews\\heima-leadnews\\heima-leadnews-test\\minio-demo\\src\\test\\java\\com\\heima\\list.html");

        PutObjectArgs putObjectArgs = PutObjectArgs.builder()
                .object("list.html")
                .contentType("text/html")
                .bucket("leadnews")
                .stream(fileInputStream, fileInputStream.available(), -1).build();

        minioClient.putObject(putObjectArgs);
    }
}