package com.asteria.common.Tool;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileTextReader {
    public String read(Path filePath) throws IOException {
        byte[] bytes = Files.readAllBytes(filePath);
        String text;                    // ← 必须声明在 try 外面
        try {
            CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT);
            text = decoder.decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException e) {
            text = new String(bytes, Charset.forName("GBK"));
        }
        //数据清洗：去掉BOM头
        if (text.startsWith("\uFEFF")) {
            text = text.substring(1);//从索引1开始截取，去掉BOM头
        }
        // 统一换行：\r\n 和单独的 \r 都变成 \n
        text = text.replace("\r\n", "\n").replace('\r', '\n');

        return text;
    }
}