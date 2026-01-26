//package com.fox.aiagent.rag;
//
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.ai.document.Document;
//import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
//import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
//import org.springframework.core.io.Resource;
//import org.springframework.core.io.support.ResourcePatternResolver;
//import org.springframework.stereotype.Component;
//
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;
//
///**
// * 恋爱大师应用文档加载器
// */
//@Component
//@Slf4j
//public class LoveAppDocumentLoader {
//
//    private final ResourcePatternResolver resourcePatternResolver;
//
//    public LoveAppDocumentLoader(ResourcePatternResolver resourcePatternResolver) {
//        this.resourcePatternResolver = resourcePatternResolver;
//    }
//
//    /**
//     * 加载多篇 Markdown 文档
//     * @return
//     */
//    public List<Document> loadMarkdowns() {
//        List<Document> allDocuments = new ArrayList<>();
//        try {
//            Resource[] resources = resourcePatternResolver.getResources("classpath:document/*.md");
//            for (Resource resource : resources) {
//                String filename = resource.getFilename();
//                // 提取文档倒数第 3 和第 2 个字作为标签
//                String status = filename.substring(filename.length() - 6, filename.length() - 4);
//                MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
//                        .withHorizontalRuleCreateDocument(true)
//                        .withIncludeCodeBlock(false)
//                        .withIncludeBlockquote(false)
//                        .withAdditionalMetadata("filename", filename)
//                        .withAdditionalMetadata("status", status)
//                        .build();
//                MarkdownDocumentReader markdownDocumentReader = new MarkdownDocumentReader(resource, config);
//                allDocuments.addAll(markdownDocumentReader.get());
//            }
//        } catch (IOException e) {
//           log.error("Markdown 文档加载失败", e);
//        }
//        return allDocuments;
//    }
//}


package com.fox.aiagent.rag;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 恋爱大师应用文档加载器（支持 Markdown 和 Excel）
 */
@Component
@Slf4j
public class LoveAppDocumentLoader {

    private final ResourcePatternResolver resourcePatternResolver;

    public LoveAppDocumentLoader(ResourcePatternResolver resourcePatternResolver) {
        this.resourcePatternResolver = resourcePatternResolver;
    }

    /**
     * 加载多篇 Markdown 文档（保持原有方法）
     */
    public List<Document> loadMarkdowns() {
        List<Document> allDocuments = new ArrayList<>();
        try {
            Resource[] resources = resourcePatternResolver.getResources("classpath:document/*.md");
            for (Resource resource : resources) {
                allDocuments.addAll(loadMarkdownFile(resource));
            }
        } catch (IOException e) {
            log.error("Markdown 文档加载失败", e);
        }
        return allDocuments;
    }

    /**
     * 新增：加载 Excel 文档
     */
    public List<Document> loadExcels() {
        List<Document> allDocuments = new ArrayList<>();
        try {
            // 支持 .xlsx 和 .xls 格式
            Resource[] resources = resourcePatternResolver.getResources("classpath:document/*.xlsx");
            Resource[] xlsResources = resourcePatternResolver.getResources("classpath:document/*.xls");

            List<Resource> allExcelResources = new ArrayList<>();
            allExcelResources.addAll(List.of(resources));
            allExcelResources.addAll(List.of(xlsResources));

            for (Resource resource : allExcelResources) {
                allDocuments.addAll(loadExcelFile(resource));
            }
        } catch (IOException e) {
            log.error("Excel 文档加载失败", e);
        }
        return allDocuments;
    }

    /**
     * 新增：加载所有文档（Markdown + Excel）
     */
    public List<Document> loadAllDocuments() {
        List<Document> allDocuments = new ArrayList<>();
        allDocuments.addAll(loadMarkdowns());
        allDocuments.addAll(loadExcels());

        log.info("文档加载完成 - Markdown: {}个, Excel: {}个, 总计: {}个",
                loadMarkdowns().size(), loadExcels().size(), allDocuments.size());

        return allDocuments;
    }

    /**
     * 加载单个 Markdown 文件
     */
    private List<Document> loadMarkdownFile(Resource resource) throws IOException {
        String filename = resource.getFilename();
        String status = filename.substring(filename.length() - 6, filename.length() - 4);
        MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                .withHorizontalRuleCreateDocument(true)
                .withIncludeCodeBlock(false)
                .withIncludeBlockquote(false)
                .withAdditionalMetadata("filename", filename)
                .withAdditionalMetadata("status", status)
                .withAdditionalMetadata("source_type", "markdown")
                .build();
        MarkdownDocumentReader markdownDocumentReader = new MarkdownDocumentReader(resource, config);
        return markdownDocumentReader.get();
    }

    /**
     * 加载单个 Excel 文件
     */
    private List<Document> loadExcelFile(Resource resource) {
        List<Document> documents = new ArrayList<>();
        String filename = resource.getFilename();

        try (InputStream is = resource.getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) {

            // 读取所有工作表
            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                String sheetName = sheet.getSheetName();

                log.debug("正在读取工作表: {}", sheetName);

                Iterator<Row> rowIterator = sheet.iterator();
                List<String> headers = new ArrayList<>();
                int rowNum = 0;

                while (rowIterator.hasNext()) {
                    Row row = rowIterator.next();
                    rowNum++;

                    // 第一行作为表头
                    if (rowNum == 1) {
                        for (Cell cell : row) {
                            headers.add(getCellValueAsString(cell));
                        }
                        continue;
                    }

                    // 数据行
                    StringBuilder contentBuilder = new StringBuilder();
                    contentBuilder.append("嘉宾信息：\n");

                    // 存储行数据用于元数据
                    List<String> rowValues = new ArrayList<>();

                    for (int colIndex = 0; colIndex < headers.size(); colIndex++) {
                        Cell cell = row.getCell(colIndex, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                        String cellValue = getCellValueAsString(cell);
                        rowValues.add(cellValue);

                        // 构建内容
                        String header = headers.get(colIndex);
                        if (!header.trim().isEmpty() && !cellValue.trim().isEmpty()) {
                            contentBuilder.append(header).append("：").append(cellValue).append("\n");
                        }
                    }

                    // 如果行有内容，创建文档
                    if (contentBuilder.length() > "嘉宾信息：\n".length()) {
                        Document doc = new Document(contentBuilder.toString());

                        // 添加基础元数据
                        doc.getMetadata().put("filename", filename);
                        doc.getMetadata().put("sheet_name", sheetName);
                        doc.getMetadata().put("row_number", rowNum);
                        doc.getMetadata().put("source_type", "excel");
                        doc.getMetadata().put("data_type", "guest_profile");

                        // 为每个字段添加单独元数据
                        for (int i = 0; i < Math.min(headers.size(), rowValues.size()); i++) {
                            String header = headers.get(i);
                            String value = rowValues.get(i);
                            if (!header.trim().isEmpty()) {
                                // 清理表头中的空格和特殊字符
                                String cleanHeader = header.trim()
                                        .replaceAll("\\s+", "_")
                                        .replaceAll("[^a-zA-Z0-9_\\u4e00-\\u9fa5]", "");
                                doc.getMetadata().put("excel_" + cleanHeader, value);
                            }
                        }

                        documents.add(doc);
                    }
                }
            }

            log.info("从 Excel 文件 {} 加载了 {} 条记录", filename, documents.size());

        } catch (IOException e) {
            log.error("读取 Excel 文件失败: {}", filename, e);
        } catch (Exception e) {
            log.error("解析 Excel 文件失败: {}", filename, e);
        }

        return documents;
    }

    /**
     * 获取单元格值作为字符串
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    // 日期格式
                    return cell.getDateCellValue().toString();
                } else {
                    // 数字格式
                    double value = cell.getNumericCellValue();
                    // 如果是整数，不显示小数部分
                    if (value == Math.floor(value)) {
                        return String.valueOf((long) value);
                    } else {
                        return String.valueOf(value);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (IllegalStateException e) {
                    // 如果公式结果是数字
                    return String.valueOf(cell.getNumericCellValue());
                }
            case BLANK:
                return "";
            default:
                return "";
        }
    }
}