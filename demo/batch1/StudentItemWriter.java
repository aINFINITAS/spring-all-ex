package com.example.demo.batch1;

import com.example.demo.dto.Student;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StudentItemWriter implements ItemWriter<Student> {

    private final StudentExcelExportService excelService;


    @Override
    public void write(Chunk<? extends Student> chunk) {
        log.info("Total size: {}", chunk.getItems().size());
        String outputFile = System.getProperty("java.io.tmpdir") + LocalDateTime.now().getNano() + "students.xlsx";
        excelService.appendAndMaybeExport((List<Student>) chunk.getItems(), 1000, outputFile);
        log.info("Excel exported to: {}", outputFile);
    }
}
