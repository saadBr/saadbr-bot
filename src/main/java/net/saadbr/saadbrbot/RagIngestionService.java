package net.saadbr.saadbrbot;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class RagIngestionService implements CommandLineRunner {

    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;

    @Value("${rag.ingest.on-startup:true}")
    private boolean ingestOnStartup;

    @Value("${rag.ingest.skip-if-not-empty:true}")
    private boolean skipIfNotEmpty;

    public RagIngestionService(VectorStore vectorStore, JdbcTemplate jdbcTemplate) {
        this.vectorStore = vectorStore;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        if (!ingestOnStartup) {
            System.out.println("[RAG] ingestion disabled (rag.ingest.on-startup=false)");
            return;
        }

        if (skipIfNotEmpty && !isVectorStoreEmpty()) {
            System.out.println("[RAG] vector_store not empty -> skipping ingestion");
            return;
        }

        Resource[] resources = new PathMatchingResourcePatternResolver()
                .getResources("classpath:rag-docs/*.*");

        if (resources.length == 0) {
            System.out.println("[RAG] No documents found in classpath:rag-docs/");
            return;
        }

        List<Document> docs = new ArrayList<>();

        for (Resource r : resources) {
            String filename = (r.getFilename() == null) ? "unknown" : r.getFilename();
            String lower = filename.toLowerCase();

            if (lower.endsWith(".pdf")) {
                String text = sanitize(readPdfAsText(r));
                if (text.isBlank()) {
                    System.out.println("[RAG] Skipping empty PDF: " + filename);
                    continue;
                }
                docs.add(new Document(text, Map.of("source", filename, "type", "pdf")));
            } else {
                List<Document> readDocs = new TextReader(r).get();
                for (Document d : readDocs) {
                    String content = sanitize(d.getText());
                    if (content.isBlank()) continue;

                    docs.add(new Document(content, Map.of("source", filename, "type", "text")));
                }
            }
        }

        if (docs.isEmpty()) {
            System.out.println("[RAG] No valid documents to index.");
            return;
        }

        List<Document> chunks = new TokenTextSplitter().apply(docs);

        // ensure no null bytes end up in DB
        List<Document> cleanedChunks = chunks.stream()
                .map(d -> new Document(sanitize(d.getText()), d.getMetadata()))
                .toList();

        vectorStore.add(cleanedChunks);

        System.out.println("[RAG] indexed docs = " + docs.size() + ", chunks = " + cleanedChunks.size());
    }

    private boolean isVectorStoreEmpty() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM public.vector_store", Integer.class);
        return count == null || count == 0;
    }

    private String readPdfAsText(Resource resource) throws Exception {
        try (var is = resource.getInputStream(); var doc = PDDocument.load(is)) {
            return new PDFTextStripper().getText(doc);
        }
    }

    private String sanitize(String s) {
        if (s == null) return "";
        return s.replace("\u0000", "").trim();
    }
}