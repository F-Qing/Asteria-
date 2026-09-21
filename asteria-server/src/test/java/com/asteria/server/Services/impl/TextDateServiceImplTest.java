package com.asteria.server.Services.impl;

import com.asteria.pojo.entity.DTO.TextDateTimeDTO;
import com.asteria.pojo.entity.TextDateTime;
import com.asteria.pojo.entity.VO.TextDateTimeVO;
import com.asteria.server.mapper.TextDateTimeMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TextDateServiceImpl 单元测试（教程 08 入门篇）。
 * 只测"业务判断"，数据库被 @Mock 顶替——不连库、不建表，毫秒级跑完。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("考试倒计时服务")
class TextDateServiceImplTest {

    /** 数据库层被顶替：所有方法默认返回 null/0/空，我们按用例逐条"剧本化" */
    @Mock
    private TextDateTimeMapper textDateTimeMapper;

    /** 被测对象：Mockito 自动把上面的 mock 塞进它的构造器 */
    @InjectMocks
    private TextDateServiceImpl textDateService;

    /** 测试数据工厂：用例里不手写一遍遍 set，只改关心的字段 */
    static class TestData {
        static TextDateTime entity(Long id, String name, LocalDate date) {
            TextDateTime e = new TextDateTime();
            e.setId(id);
            e.setName(name);
            e.setDate(date);
            return e;
        }

        static TextDateTimeDTO dto(String name, String date) {
            TextDateTimeDTO d = new TextDateTimeDTO();
            d.setName(name);
            d.setDate(date);
            return d;
        }
    }

    @Nested
    @DisplayName("新增考试 addText")
    class AddText {

        @Test
        @DisplayName("日期字符串正确解析后写入数据库")
        void should_insertParsedDate_when_dateIsValid() {
            // Given
            TextDateTimeDTO dto = TestData.dto("期中考试", "2026-10-01");

            // When
            textDateService.addText(dto);

            // Then：捕获真正传给 insert() 的实体，逐字段核对
            ArgumentCaptor<TextDateTime> captor = ArgumentCaptor.forClass(TextDateTime.class);
            verify(textDateTimeMapper).insert(captor.capture());
            TextDateTime saved = captor.getValue();
            assertEquals("期中考试", saved.getName());
            assertEquals(LocalDate.of(2026, 10, 1), saved.getDate());
        }

        @Test
        @DisplayName("日期带首尾空格也能解析（trim 生效）")
        void should_trimInput_when_dateHasSpaces() {
            textDateService.addText(TestData.dto("期末", "  2026-12-31  "));

            ArgumentCaptor<TextDateTime> captor = ArgumentCaptor.forClass(TextDateTime.class);
            verify(textDateTimeMapper).insert(captor.capture());
            assertEquals(LocalDate.of(2026, 12, 31), captor.getValue().getDate());
        }

        @Test
        @DisplayName("非法日期抛异常，且不碰数据库")
        void should_notTouchDb_when_dateIsInvalid() {
            // "13 月"不存在——暴露一个现状：异常裸奔到调用方（见教程 §7 的改进建议）
            TextDateTimeDTO bad = TestData.dto("乱填", "2026-13-45");

            assertThrows(DateTimeParseException.class, () -> textDateService.addText(bad));

            verify(textDateTimeMapper, never()).insert(any(TextDateTime.class));
        }
    }

    @Nested
    @DisplayName("列表 listAll（重点：daysLeft 与紧急等级换算）")
    class ListAll {

        @ParameterizedTest(name = "还有 {0} 天 → 等级 {1}")
        @CsvSource({
                "0,  red",     // 今天考试：红
                "7,  red",     // 边界：≤7 天红
                "8,  orange",  // 刚过红线：橙
                "14, orange",  // 边界：≤14 天橙
                "15, green",   // 刚过橙线：绿
                "-1, red"      // 已考完（负数）也算红
        })
        @DisplayName("剩余天数按边界映射到正确等级")
        void should_mapLevelByDaysLeft_when_daysCrossBoundaries(int daysLeft, String expectedLevel) {
            // Given：日期用"今天+n天"构造，任何时候跑结果都一样（幂等）
            TextDateTime e = TestData.entity(1L, "考试", LocalDate.now().plusDays(daysLeft));
            when(textDateTimeMapper.selectList(any())).thenReturn(List.of(e));

            // When
            List<TextDateTimeVO> result = textDateService.listAll();

            // Then
            assertEquals(1, result.size());
            assertEquals(daysLeft, result.get(0).getDaysLeft());
            assertEquals(expectedLevel, result.get(0).getLevel());
        }

        @Test
        @DisplayName("日期字段转成字符串给前端")
        void should_formatDateToString_when_mappingToVO() {
            when(textDateTimeMapper.selectList(any()))
                    .thenReturn(List.of(TestData.entity(9L, "月考", LocalDate.of(2026, 11, 11))));

            TextDateTimeVO vo = textDateService.listAll().get(0);
            assertEquals("2026-11-11", vo.getDate());
        }

        @Test
        @DisplayName("库里没数据时返回空列表而不是 null")
        void should_returnEmptyList_when_noRows() {
            when(textDateTimeMapper.selectList(any())).thenReturn(List.of());

            assertNotNull(textDateService.listAll());
            assertTrue(textDateService.listAll().isEmpty());
        }
    }

    @Nested
    @DisplayName("修改 updateText")
    class UpdateText {

        @Test
        @DisplayName("考试不存在：返回 false 且不执行更新")
        void should_returnFalseAndUpdateNever_when_entityNotFound() {
            when(textDateTimeMapper.selectById(999L)).thenReturn(null);

            assertFalse(textDateService.updateText(999L, TestData.dto("X", "2026-10-01")));
            verify(textDateTimeMapper, never()).updateById(any(TextDateTime.class));
        }

        @Test
        @DisplayName("考试存在：改名改日期并保存")
        void should_overwriteFields_when_entityExists() {
            TextDateTime existing = TestData.entity(1L, "旧名字", LocalDate.of(2026, 1, 1));
            when(textDateTimeMapper.selectById(1L)).thenReturn(existing);

            assertTrue(textDateService.updateText(1L, TestData.dto("新名字", "2026-10-01")));

            assertEquals("新名字", existing.getName());
            assertEquals(LocalDate.of(2026, 10, 1), existing.getDate());
            verify(textDateTimeMapper).updateById(existing);
        }
    }

    @Nested
    @DisplayName("删除 deleteText")
    class DeleteText {

        @Test
        @DisplayName("影响行数为 0（id 不存在）→ false")
        void should_returnFalse_when_zeroRowsAffected() {
            when(textDateTimeMapper.deleteById(404L)).thenReturn(0);
            assertFalse(textDateService.deleteText(404L));
        }

        @Test
        @DisplayName("影响行数 1 → true")
        void should_returnTrue_when_rowDeleted() {
            when(textDateTimeMapper.deleteById(1L)).thenReturn(1);
            assertTrue(textDateService.deleteText(1L));
        }
    }
}
