package com.asteria.server;

import com.asteria.common.Tool.FileTextReader;
import com.asteria.common.Tool.QuestionParser;
import com.asteria.common.Tool.RawQuestion;
import com.asteria.pojo.entity.VO.BanksVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static com.asteria.common.Tool.QuestionParser.QUESTION_HEADER;


public class Text {

@Test
    public void Text() throws IOException {
        FileTextReader fileTextReader=new FileTextReader();
        String text=fileTextReader.read(Path.of("D:\\FinalText\\题库脚本源码\\1.8.0\\学习通题库_20260906_000106.txt"));

        QuestionParser questionParser=new QuestionParser();
       // questionParser.debugHeader(List.of(text.split("\n")));
        List<RawQuestion> questions=questionParser.parse(text);
            System.out.println(questions);

    }


}
