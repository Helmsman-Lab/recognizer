package com.rec.recognizer.tool;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class PdfRecTask implements CommandLineRunner {

    @Autowired
    private RecService recService;


    @Override
    public void run(String... args) throws Exception {
        String root = "D:\\data\\png";
        File rootFile = new File(root);
        File[] pngFiles = rootFile.listFiles();
        for (File file : pngFiles) {
            recService.recognize(file.getAbsolutePath());
        }
    }

}
