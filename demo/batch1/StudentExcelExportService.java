package com.example.demo.batch1;

import com.example.demo.dto.Student;
import com.example.demo.excel.BaseExcelExportService;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.stereotype.Service;

@Service
public class StudentExcelExportService extends BaseExcelExportService<Student> {

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
        row.createCell(1).setCellValue(s.getSalary().doubleValue());
        row.createCell(2).setCellValue(s.getAge());
    }
}
