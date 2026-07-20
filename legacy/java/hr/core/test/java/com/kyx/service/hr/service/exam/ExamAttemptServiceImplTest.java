package com.kyx.service.hr.service.exam;

import com.kyx.service.hr.dal.dataobject.exam.ExamPaperItemDO;
import junit.framework.TestCase;

import java.lang.reflect.Method;

public class ExamAttemptServiceImplTest extends TestCase {

    private Method calcScoreMethod;
    private ExamAttemptServiceImpl service;

    @Override
    protected void setUp() throws Exception {
        service = new ExamAttemptServiceImpl();
        calcScoreMethod = ExamAttemptServiceImpl.class
                .getDeclaredMethod("calcScore", ExamPaperItemDO.class, String.class, String.class);
        calcScoreMethod.setAccessible(true);
    }

    public void testCalcScoreParsesJsonStringLiteralUserAnswer() throws Exception {
        ExamPaperItemDO item = buildItem("single",
                "{\"value\":\"3-7天\",\"analysis\":\"规则说明\",\"shuffleOptions\":true}");

        assertEquals(Integer.valueOf(5), calcScore(item, "\"3-7天\"", ""));
    }

    public void testCalcScoreParsesJudgeAnswerFromValuePayload() throws Exception {
        ExamPaperItemDO item = buildItem("judge",
                "{\"value\":\"错误\",\"analysis\":\"规则说明\",\"shuffleOptions\":false}");

        assertEquals(Integer.valueOf(5), calcScore(item, "\"错误\"", ""));
    }

    public void testCalcScoreNormalizesJudgeBooleanAnswer() throws Exception {
        ExamPaperItemDO item = buildItem("judge",
                "{\"value\":true,\"analysis\":\"规则说明\",\"shuffleOptions\":false}");

        assertEquals(Integer.valueOf(5), calcScore(item, "\"正确\"", ""));
    }

    public void testCalcScoreMatchesChoiceLetterToOptionText() throws Exception {
        ExamPaperItemDO item = buildItem("single",
                "{\"value\":\"与门店协商最低价格\",\"shuffleOptions\":true}");
        item.setOptionsJson("["
                + "{\"text\":\"直接要求门店按原价施工\"},"
                + "{\"text\":\"与门店协商最低价格\"}] ");

        assertEquals(Integer.valueOf(5), calcScore(item, "\"B\"", ""));
    }

    public void testCalcScoreSplitsMultiChoiceLetters() throws Exception {
        ExamPaperItemDO item = buildItem("multi",
                "{\"value\":[\"提交走保赔付申请时按实际金额填写\",\"遗漏填写走保金额会被驳回\"],\"shuffleOptions\":true}");
        item.setOptionsJson("["
                + "{\"text\":\"提交走保赔付申请时按实际金额填写\"},"
                + "{\"text\":\"遗漏填写走保金额会被驳回\"},"
                + "{\"text\":\"可以不填写走保金额\"}] ");

        assertEquals(Integer.valueOf(5), calcScore(item, "\"AB\"", ""));
    }

    private ExamPaperItemDO buildItem(String itemType, String answerJson) {
        ExamPaperItemDO item = new ExamPaperItemDO();
        item.setItemType(itemType);
        item.setAnswerJson(answerJson);
        item.setScore(5);
        return item;
    }

    private Integer calcScore(ExamPaperItemDO item, String answerJson, String answerText) throws Exception {
        return (Integer) calcScoreMethod.invoke(service, item, answerJson, answerText);
    }
}
