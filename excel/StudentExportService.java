package com.example.demo.excel;

import com.example.demo.Student;
import jakarta.annotation.PostConstruct;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StudentExportService extends BaseExcelExportService<Student> {

    @Override
    protected String getTemplatePath() {
        return "/templates/student_template.xlsx";
    }

    @Override
    protected String getSheetName() {
        return "Students";
    }

    @Override
    protected void writeDataRow(Student s, Row row) {
        row.createCell(0).setCellValue(s.getName());
        row.createCell(1).setCellValue(s.getAge());
    }

    @PostConstruct
    public void init() {
        startExport();

        append(List.of(
                new Student("John", 20),
                new Student("Anna", 21)
        ));

        append(List.of(
                new Student("Bob", 19),
                new Student("Helen", 22)
        ));

        append(List.of(
                new Student("David", 23),
                new Student("Lina", 55)
        ));

        finish(System.getProperty("java.io.tmpdir") + "/students.xlsx");
    }
}
