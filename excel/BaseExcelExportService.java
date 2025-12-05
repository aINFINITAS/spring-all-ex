package com.example.demo.excel;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public abstract class BaseExcelExportService<T> {

    private SXSSFWorkbook workbook;
    protected Sheet sheet;
    protected int currentRowIndex;

    private final List<String> headerValues = new ArrayList<>();
    private final List<CellStyle> headerStyles = new ArrayList<>();

    protected abstract String getTemplatePath();          // Template file

    protected abstract String getSheetName();              // Tên sheet

    protected abstract void writeDataRow(T data, Row row); // Ghi 1 dòng data

    public void startExport() {
        try (XSSFWorkbook templateWb = new XSSFWorkbook(
                Objects.requireNonNull(getClass().getResourceAsStream(getTemplatePath()))
        )) {
            Sheet templateSheet = templateWb.getSheetAt(0);
            Row templateHeader = templateSheet.getRow(0);

            headerValues.clear();
            headerStyles.clear();

            for (Cell cell : templateHeader) {
                headerValues.add(cell.getStringCellValue());
                headerStyles.add(cell.getCellStyle());
            }

            workbook = new SXSSFWorkbook(200);
            workbook.setCompressTempFiles(true);

            sheet = workbook.createSheet(getSheetName());

            // Clone header
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headerValues.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headerValues.get(i));

                CellStyle newStyle = workbook.createCellStyle();
                newStyle.cloneStyleFrom(headerStyles.get(i));
                cell.setCellStyle(newStyle);
            }

            currentRowIndex = 1;

        } catch (Exception e) {
            throw new RuntimeException("Error reading template", e);
        }
    }

    public void append(List<T> records) {
        for (T record : records) {
            Row row = sheet.createRow(currentRowIndex++);
            writeDataRow(record, row);
        }
    }

    public void finish(String filePath) {
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
                workbook.dispose();
                workbook.close();
            } catch (Exception ignore) {
            }
        }
    }
}
