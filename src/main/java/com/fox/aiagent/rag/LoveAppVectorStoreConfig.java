//package com.fox.aiagent.rag;
//
//import jakarta.annotation.Resource;
//import org.springframework.ai.document.Document;
//import org.springframework.ai.embedding.EmbeddingModel;
//import org.springframework.ai.vectorstore.SimpleVectorStore;
//import org.springframework.ai.vectorstore.VectorStore;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import java.util.List;
//
///**
// * 恋爱大师向量数据库配置（初始化基于内存的向量数据库 Bean）
// */
//@Configuration
//public class LoveAppVectorStoreConfig {
//
//    @Resource
//    private LoveAppDocumentLoader loveAppDocumentLoader;
//
//    @Resource
//    private MyTokenTextSplitter myTokenTextSplitter;
//
//    @Resource
//    private MyKeywordEnricher myKeywordEnricher;
//
//    @Bean
//    VectorStore loveAppVectorStore(EmbeddingModel dashscopeEmbeddingModel) {
//        SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(dashscopeEmbeddingModel).build();
//        // 加载文档
//        List<Document> documentList = loveAppDocumentLoader.loadMarkdowns();
////        // 自主切分文档
////        List<Document> splitDocuments = myTokenTextSplitter.splitCustomized(documentList);
//        // 自动补充关键词元信息
//        List<Document> enrichedDocuments = myKeywordEnricher.enrichDocuments(documentList);
//        simpleVectorStore.add(enrichedDocuments);
//        return simpleVectorStore;
//    }
//}


package com.fox.aiagent.rag;

import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

//@Configuration
public class LoveAppVectorStoreConfig {

    @Resource
    private LoveAppDocumentLoader loveAppDocumentLoader;

    @Resource
    private MyTokenTextSplitter myTokenTextSplitter;

    @Resource
    private MyKeywordEnricher myKeywordEnricher;

    @Bean
    VectorStore loveAppVectorStore(EmbeddingModel dashscopeEmbeddingModel) {
        SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(dashscopeEmbeddingModel).build();

        // 修改这里：使用 loadAllDocuments() 替代 loadMarkdowns()
        List<Document> documentList = loveAppDocumentLoader.loadAllDocuments();

        // 打印加载统计信息
        printDocumentStats(documentList);

        // 自主切分文档（可选）
        // List<Document> splitDocuments = myTokenTextSplitter.splitCustomized(documentList);

        // 自动补充关键词元信息
        List<Document> enrichedDocuments = myKeywordEnricher.enrichDocuments(documentList);

        simpleVectorStore.add(enrichedDocuments);

        // 打印最终结果
        System.out.println("✅ 向量存储初始化完成");
        System.out.println("   加载文档总数: " + documentList.size());
        System.out.println("   向量化文档数: " + enrichedDocuments.size());

        return simpleVectorStore;
    }

    /**
     * 打印文档统计信息
     */
    private void printDocumentStats(List<Document> documents) {
        long markdownCount = documents.stream()
                .filter(d -> "markdown".equals(d.getMetadata().get("source_type")))
                .count();

        long excelCount = documents.stream()
                .filter(d -> "excel".equals(d.getMetadata().get("source_type")))
                .count();

        long csvCount = documents.stream()
                .filter(d -> "csv".equals(d.getMetadata().get("source_type")))
                .count();

        System.out.println("📊 文档加载统计:");
        System.out.println("   Markdown 文档: " + markdownCount + " 个");
        System.out.println("   Excel 文档: " + excelCount + " 个");
        System.out.println("   CSV 文档: " + csvCount + " 个");
        System.out.println("   总计: " + documents.size() + " 个文档");

//        // 显示一些示例
//        if (excelCount > 0) {
//            System.out.println("\n📋 Excel 数据示例:");
//            documents.stream()
//                    .filter(d -> "excel".equals(d.getMetadata().get("source_type")))
//                    .limit(1)
//                    .forEach(doc -> {
//                        System.out.println("   文件: " + doc.getMetadata().get("filename"));
//                        System.out.println("   内容预览: " +
//                                doc.getText().substring(0, Math.min(100, doc.getText().length())) + "...");
//                    });
//        }
    }
}