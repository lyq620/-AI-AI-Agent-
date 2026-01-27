//package com.fox.aiagent.tools;
//
//import cn.hutool.core.io.FileUtil;
//import com.itextpdf.kernel.font.PdfFont;
//import com.itextpdf.kernel.font.PdfFontFactory;
//import com.itextpdf.kernel.pdf.PdfDocument;
//import com.itextpdf.kernel.pdf.PdfWriter;
//import com.itextpdf.layout.Document;
//import com.itextpdf.layout.element.Paragraph;
//import com.fox.aiagent.constant.FileConstant;
//import org.springframework.ai.tool.annotation.Tool;
//import org.springframework.ai.tool.annotation.ToolParam;
//
//import java.io.IOException;
//
///**
// * PDF 生成工具
// */
//public class PDFGenerationTool {
//
//    @Tool(description = "Generate a PDF file with given content", returnDirect = false)
//    public String generatePDF(
//            @ToolParam(description = "Name of the file to save the generated PDF") String fileName,
//            @ToolParam(description = "Content to be included in the PDF") String content) {
//        String fileDir = FileConstant.FILE_SAVE_DIR + "/pdf";
//        String filePath = fileDir + "/" + fileName;
//        try {
//            // 创建目录
//            FileUtil.mkdir(fileDir);
//            // 创建 PdfWriter 和 PdfDocument 对象
//            try (PdfWriter writer = new PdfWriter(filePath);
//                 PdfDocument pdf = new PdfDocument(writer);
//                 Document document = new Document(pdf)) {
//                // 自定义字体（需要人工下载字体文件到特定目录）
////                String fontPath = Paths.get("src/main/resources/static/fonts/simsun.ttf")
////                        .toAbsolutePath().toString();
////                PdfFont font = PdfFontFactory.createFont(fontPath,
////                        PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
//                // 使用内置中文字体
//                PdfFont font = PdfFontFactory.createFont("STSongStd-Light", "UniGB-UCS2-H");
//                document.setFont(font);
//                // 创建段落
//                Paragraph paragraph = new Paragraph(content);
//                // 添加段落并关闭文档
//                document.add(paragraph);
//            }
//            return "PDF generated successfully to: " + filePath;
//        } catch (IOException e) {
//            return "Error generating PDF: " + e.getMessage();
//        }
//    }
//}

package com.fox.aiagent.tools;

import cn.hutool.core.io.FileUtil;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.fox.aiagent.constant.FileConstant;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.IOException;

/**
 * PDF 生成工具 - 修复版本
 */
public class PDFGenerationTool {
    // TODO 待完善
    @Tool(description = "Generate a PDF file with given content", returnDirect = false)
    public String generatePDF(
            @ToolParam(description = "Name of the file to save the generated PDF") String fileName,
            @ToolParam(description = "Content to be included in the PDF") String content) {

        String fileDir = FileConstant.FILE_SAVE_DIR + "/pdf";
        String filePath = fileDir + "/" + fileName;

        // 1. 重要：预处理内容，过滤掉导致报错的 Emoji 和非 BMP 字符
        // iText 默认编码器不支持这些，AI 计划书中经常带玫瑰、心形等图标
        String cleanedContent = content.replaceAll("[^\\u0000-\\uFFFF]", "");

        try {
            FileUtil.mkdir(fileDir);

            try (PdfWriter writer = new PdfWriter(filePath);
                 PdfDocument pdf = new PdfDocument(writer);
                 Document document = new Document(pdf)) {

                // 2. 建议方案：使用本地字体文件（如中文字体 simsun.ttf 或思源黑体）
                // 这样能完美支持中文，且比内置的 STSongStd 稳定得多
                // 请确保 resources/fonts 目录下有该文件
                /*
                String fontPath = "src/main/resources/fonts/SourceHanSansCN-Regular.ttf";
                PdfFont font = PdfFontFactory.createFont(fontPath, PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
                */

                // 如果暂时没有字体文件，继续使用内置，但必须配合上面的 cleanedContent 过滤
                PdfFont font = PdfFontFactory.createFont("STSongStd-Light", "UniGB-UCS2-H");

                document.setFont(font);
                document.add(new Paragraph(cleanedContent));
            }
            return "PDF generated successfully to: " + filePath;
        } catch (Exception e) {
            // 捕获所有异常，防止 ToolExecutionException 导致整个 AI 链路中断
            return "Error generating PDF: " + e.getMessage();
        }
    }
}
