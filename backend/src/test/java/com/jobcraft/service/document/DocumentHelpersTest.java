package com.jobcraft.service.document;

import static org.assertj.core.api.Assertions.assertThat;

import com.jobcraft.TestPdfs;
import com.jobcraft.model.DocumentCategory;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

class DocumentHelpersTest {

    @ParameterizedTest
    @CsvSource({
        "Lebenslauf_Lena_Hoffmann_2026.pdf, CV",
        "CV-2026.docx, CV",
        "Skill-Profil_Fullstack.docx, SKILLS",
        "Arbeitszeugnis_Kern_Lauth.pdf, CERTIFICATE",
        "Zertifikat_AWS_Developer.pdf, OTHER"
    })
    void detectsCategoryFromFileName(String fileName, DocumentCategory expected) {
        assertThat(DocumentCategoryDetector.detect(fileName)).isEqualTo(expected);
    }

    @Test
    void detectsFileTypeBySignatureAndExtension() {
        byte[] pdf = "%PDF-1.4 ...".getBytes(StandardCharsets.US_ASCII);
        byte[] zip = new byte[] {'P', 'K', 3, 4};

        assertThat(FileType.detect("a.pdf", pdf)).contains(FileType.PDF);
        assertThat(FileType.detect("a.docx", zip)).contains(FileType.DOCX);
        assertThat(FileType.detect("a.pdf", zip)).isEmpty();
        assertThat(FileType.detect("a.exe", pdf)).isEmpty();
    }

    @Test
    void extractsTextAndPagesFromPdf() throws IOException {
        byte[] pdf = TestPdfs.withLines(List.of("LENA HOFFMANN", "Java 21, Spring Boot"));

        TextExtractionService.ExtractionResult result = new TextExtractionService().extract(pdf, FileType.PDF);

        assertThat(result.pages()).isEqualTo(1);
        assertThat(result.text()).contains("LENA HOFFMANN").contains("Java 21, Spring Boot");
    }
}
