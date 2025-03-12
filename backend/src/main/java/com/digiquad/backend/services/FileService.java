package com.digiquad.backend.services;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.opencsv.CSVReader;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public class FileService {
    private static final Logger logger = LoggerFactory.getLogger(FileService.class);

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";

        try {
            switch (cell.getCellType()) {
                case STRING:
                    return cell.getStringCellValue();
                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell)) {
                        return cell.getDateCellValue().toString();
                    }
                    return String.valueOf(cell.getNumericCellValue());
                case BOOLEAN:
                    return String.valueOf(cell.getBooleanCellValue());
                case FORMULA:
                    return cell.getCellFormula();
                default:
                    return "";
            }
        } catch (Exception e) {
            logger.error("Error reading cell value: " + e.getMessage(), e);
            return "";
        }
    }

    public ResponseEntity<List<List<String>>> parseFile(MultipartFile file, Integer startRow) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Collections.singletonList(Collections.singletonList("File is empty")));
        }

        try {
            String fileExtension = getFileExtension(file.getOriginalFilename());

            if ("xlsx".equalsIgnoreCase(fileExtension)) {
                return handleExcelFile(file, startRow);
            } else if ("csv".equalsIgnoreCase(fileExtension)) {
                return handleCsvFile(file);
            } else {
                return ResponseEntity.badRequest().body(Collections.singletonList(Collections.singletonList("Unsupported file format")));
            }

        } catch (Exception e) {
            logger.error("Unexpected error: " + e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Collections.singletonList(Collections.singletonList("Internal server error")));
        }
    }

    private ResponseEntity<List<List<String>>> handleExcelFile(MultipartFile file, Integer startRow) {
        List<List<String>> tableData = new ArrayList<>();
        try (Workbook wb = new XSSFWorkbook(file.getInputStream())) {
            Sheet firstSheet = wb.getSheetAt(0);
            Row headerRow = firstSheet.getRow(0);

            if (headerRow != null) {
                List<String> headerRowData = new ArrayList<>();
                for (Cell cell : headerRow) {
                    headerRowData.add(getCellValueAsString(cell));
                }
                tableData.add(headerRowData);
                if(startRow == 0)
                    startRow++;
            }

            for (int i = startRow; i <= firstSheet.getLastRowNum(); i++) {
                Row currentRow = firstSheet.getRow(i);
                if (currentRow == null) continue;

                List<String> rowData = new ArrayList<>();
                for (Cell cell : currentRow) {
                    rowData.add(getCellValueAsString(cell));
                }
                tableData.add(rowData);
            }

            return ResponseEntity.ok(tableData);

        } catch (IOException e) {
            logger.error("Error processing Excel file: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(Collections.singletonList(Collections.singletonList("Error reading Excel file")));
        } catch (Exception e) {
            logger.error("Unexpected error in Excel processing: " + e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Collections.singletonList(Collections.singletonList("Internal server error")));
        }
    }

    private ResponseEntity<List<List<String>>> handleCsvFile(MultipartFile file) {
        List<List<String>> tableData = new ArrayList<>();
        try (CSVReader csvReader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            String[] values;
            while ((values = csvReader.readNext()) != null) {
                tableData.add(Arrays.asList(values));
            }
            return ResponseEntity.ok(tableData);

        } catch (IOException e) {
            logger.error("Error processing CSV file: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(Collections.singletonList(Collections.singletonList("Error reading CSV file")));
        } catch (Exception e) {
            logger.error("Unexpected error in CSV processing: " + e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Collections.singletonList(Collections.singletonList("Internal server error")));
        }
    }

    private String getFileExtension(String fileName) {
        if (fileName != null && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf('.') + 1);
        }
        return "";
    }

    public String generateJSON(MultipartFile file) {
        String json = "{}";
        try {
            String xml = new String(file.getBytes(), StandardCharsets.UTF_8);

            XmlMapper xmlMapper = new XmlMapper();
            ObjectNode dataInstance = xmlMapper.readValue(xml.getBytes(), ObjectNode.class);

            JsonMapper jsonMapper = new JsonMapper();
            json = jsonMapper.writeValueAsString(dataInstance);

            return json;
        } catch (IOException e) {
            logger.error("Error processing Excel file: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        return json;
    }
}
