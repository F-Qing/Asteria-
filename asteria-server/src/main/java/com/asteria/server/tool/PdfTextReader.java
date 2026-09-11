package com.asteria.server.tool;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;

/**
 * 读 .pdf 的文本。
 *
 * <p>为什么需要它：PDF 是**二进制格式**（页面内容存在内容流里，还常带压缩），
 * 和 docx 一样不能按编码当纯文本读，必须用 PDFBox 抽取文字，再交给 {@code QuestionParser}。
 *
 * <p>⚠️ 局限：**扫描件（图片型 PDF）抽不出任何文字**，这种情况只能提示用户改用 txt/docx。
 */
@Component
public class PdfTextReader {

    /** 读成字符串；读不了抛 IOException，由调用方翻译成业务异常 */
    public String read(Path path) throws IOException {
        try (PDDocument document = PDDocument.load(path.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            // 按页面坐标排序，尽量还原"从上到下、从左到右"的阅读顺序
            stripper.setSortByPosition(true);

            String text = stripper.getText(document);
            if (text == null) {
                return "";
            }
            // 去 BOM（和其它读取器保持一致）
            if (text.startsWith("\uFEFF")) {
                text = text.substring(1);
            }
            // 统一换行：\r\n 和单独的 \r 都变成 \n
            return text.replace("\r\n", "\n").replace('\r', '\n');
        }
    }
}
