package com.asteria.server.parser;

import com.asteria.common.Tool.QuestionOption;
import com.asteria.common.Tool.RawQuestion;
import com.asteria.pojo.enums.QuestionType;

import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 判型器：把「原始材料」判成 {@link QuestionType}。
 *
 * <p>判定顺序（从最可靠往下，越靠前越优先）：
 * <ol>
 *   <li>文件里写了「题型：xxx」——最可靠，直接映射；</li>
 *   <li>选项恰好 2 个，且文本都是对错词（对/错/正确/错误…）——判断题；</li>
 *   <li>有选项块 + 答案含多个字母——多选题；</li>
 *   <li>有选项块 + 答案单个字母——单选题；</li>
 *   <li>没有选项块——答案短算填空、答案长算简答；</li>
 *   <li>都判不出来——返回 null，由调用方标记"类型待确认"，不要瞎猜。</li>
 * </ol>
 */
@Component
public class QuestionClassifier {

    /** 没有选项块时：答案不超过这个长度当填空题，超过当简答题 */
    private static final int FILL_BLANK_MAX_LENGTH = 20;

    /** 判型；判不出来返回 null */
    public QuestionType classify(RawQuestion raw) {
        // ① 文件写了题型 → 用中文名直接映射
        QuestionType byLabel = QuestionType.ofChinese(raw.getRawType());
        if (byLabel != null) {
            return byLabel;
        }

        // ② 选项是两个对错词 → 判断题（必须排在③④前面，否则答案 A/B 会被当单选）
        if (isTrueFalseOptions(raw.getRawOptions())) {
            return QuestionType.TRUE_FALSE;
        }

        // ③④ 有选项块 → 按答案里的字母个数分单选/多选
        if (!raw.getRawOptions().isEmpty()) {
            return AnswerTexts.letters(raw.getRawAnswer()).length() > 1
                    ? QuestionType.MULTIPLE
                    : QuestionType.SINGLE;
        }

        // ⑤ 没有选项块 → 看答案长短
        if (raw.getRawAnswer() != null && !raw.getRawAnswer().isBlank()) {
            return raw.getRawAnswer().trim().length() <= FILL_BLANK_MAX_LENGTH
                    ? QuestionType.FILL_BLANK
                    : QuestionType.ESSAY;
        }

        // ⑥ 判不出来
        return null;
    }

    /** 选项恰好 2 个，且文本都是对错词 */
    private boolean isTrueFalseOptions(List<QuestionOption> options) {
        if (options == null || options.size() != 2) {
            return false;
        }
        return options.stream().allMatch(option -> AnswerTexts.isTrueFalseWord(option.getText()));
    }
}