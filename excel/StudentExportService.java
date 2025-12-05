package com.example.demo.excel;

import com.example.demo.Student;
import jakarta.annotation.PostConstruct;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class StudentExportService {

    private static final String TEMPLATE_PATH = "/templates/student_template.xlsx";

    private SXSSFWorkbook workbook;
    private Sheet sheet;
    private int currentRowIndex;

    // Chỉ cần một style cho header
    private final List<String> headerValues = new ArrayList<>();
    private final List<CellStyle> cachedHeaderStyles = new ArrayList<>();

    @PostConstruct
    public void init() {
        startExport();

        appendStudents(List.of(
                new Student("Johngfd", 54),
                new Student("Annagf", 543)
        ));

        appendStudents(List.of(
                new Student("Bogfb", 54),
                new Student("Helgfden", 5435)
        ));

        appendStudents(List.of(
                new Student("Dagfdvid", 344),
                new Student("Lina", 6521)
        ));
        appendStudents(List.of(
                new Student("Dagfdvid", 21),
                new Student("Lina", 32)
        ));

        // Save
        String outputPath = System.getProperty("java.io.tmpdir") + File.separator + "students.xlsx";
        finishExportToFile(outputPath);

        System.out.println("Excel saved at: " + outputPath);
    }

    /**
     * Load template header và tạo SXSSFWorkbook streaming
     */
    public void startExport() {
        try (XSSFWorkbook templateWb = new XSSFWorkbook(
                Objects.requireNonNull(getClass().getResourceAsStream(TEMPLATE_PATH)))) {

            Sheet templateSheet = templateWb.getSheetAt(0);
            Row templateHeader = templateSheet.getRow(0);

            headerValues.clear();
            cachedHeaderStyles.clear();

            // Lưu header text và style (style tách riêng để clone sang SXSSF)
            for (Cell cell : templateHeader) {
                headerValues.add(cell.getStringCellValue());
                cachedHeaderStyles.add(cell.getCellStyle());
            }

            // SXSSF: window size nhỏ để giảm RAM
            workbook = new SXSSFWorkbook(200);
            workbook.setCompressTempFiles(true);

            sheet = workbook.createSheet("Students");

            // Tạo header mới từ template
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headerValues.size(); i++) {

                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headerValues.get(i));

                CellStyle newStyle = workbook.createCellStyle();
                newStyle.cloneStyleFrom(cachedHeaderStyles.get(i));

                cell.setCellStyle(newStyle);
            }

            currentRowIndex = 1;

        } catch (Exception e) {
            throw new RuntimeException("Error loading template", e);
        }
    }

    /**
     * Append data (SXSSF streaming → không tích RAM)
     */
    public void appendStudents(List<Student> students) {
        for (Student s : students) {
            Row row = sheet.createRow(currentRowIndex++);
            row.createCell(0).setCellValue(s.getName());
            row.createCell(1).setCellValue(s.getAge());
        }
    }


    public void finishExportToFile(String filePath) {
        File file = new File(filePath);

        try {

            if (file.exists()) file.delete();

            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }

        } catch (Exception e) {
            throw new RuntimeException("Error writing Excel", e);
        } finally {
            try {
                workbook.dispose(); // xóa temp files
                workbook.close();   // giải phóng workbook
            } catch (Exception ignore) {
            }
        }
    }
}
