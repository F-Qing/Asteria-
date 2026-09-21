package com.asteria.server.tool;

import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 读 .docx 的文本。
 *
 * <p>为什么需要它：{@code .docx} 本质是一个 **ZIP 压缩包**（里面是 word/document.xml 等 XML），
 * 不是纯文本。如果像 txt 那样按 UTF-8/GBK 解码，只会得到一堆乱码，解析器自然一道题都切不出来。
 * 所以必须先用 POI 把文字抽出来，再交给 {@code QuestionParser}。
 *
 * <p>抽完之后的文本处理和 txt 完全一样（去 BOM、统一换行），后面的切块/判型/归一化一行都不用改。
 */
@Component
public class DocxTextReader {

    /** 读成字符串；读不了抛 IOException，由调用方翻译成业务异常 */
    public String read(Path path) throws IOException {
        try (InputStream in = Files.newInputStream(path);
             XWPFDocument document = new XWPFDocument(in);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {

            String text = extractor.getText();
            if (text == null) {
                return "";
            }
            // 去掉 BOM（和 FileTextReader 保持一致的处理）
            if (text.startsWith("\uFEFF")) {
                text = text.substring(1);
            }
            // 统一换行：\r\n 和单独的 \r 都变成 \n
            text = text.replace("\r\n", "\n").replace('\r', '\n');
            // 制表符也变成换行：Word 里排版选项时习惯用 Tab 把 A．/B．/C．/D． 并在同一个段落里，
            // POI 抽出来就是 "A．甲\tB．乙\tC．丙\tD．丁" 一行。解析器是**按行**切选项的，
            // 不拆开的话整行只会被认成一个选项（后面的选项全被并进 A 的内容里）。
            return text.replace("\t", "\n");
        }
    }
}
