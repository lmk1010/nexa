package com.kyx.service.hr.service.questionnaire;

import junit.framework.TestCase;

import java.lang.reflect.Method;
import java.util.List;

public class QuestionnaireAnswerServiceImplTest extends TestCase {

    private Method parseAnswerValuesMethod;
    private QuestionnaireAnswerServiceImpl service;

    @Override
    protected void setUp() throws Exception {
        service = new QuestionnaireAnswerServiceImpl();
        parseAnswerValuesMethod = QuestionnaireAnswerServiceImpl.class
                .getDeclaredMethod("parseAnswerValues", String.class, boolean.class);
        parseAnswerValuesMethod.setAccessible(true);
    }

    public void testParseSingleAnswerFromJsonStringLiteral() throws Exception {
        List<String> values = parseAnswerValues("\"善于领导下属提高工作效率\"", false);

        assertEquals(1, values.size());
        assertEquals("善于领导下属提高工作效率", values.get(0));
    }

    public void testParseSingleAnswerFromRawText() throws Exception {
        List<String> values = parseAnswerValues("善于领导下属提高工作效率", false);

        assertEquals(1, values.size());
        assertEquals("善于领导下属提高工作效率", values.get(0));
    }

    public void testParseMultiAnswerFromJsonArray() throws Exception {
        List<String> values = parseAnswerValues("[\"A\",\"B\"]", true);

        assertEquals(2, values.size());
        assertEquals("A", values.get(0));
        assertEquals("B", values.get(1));
    }

    @SuppressWarnings("unchecked")
    private List<String> parseAnswerValues(String answerJson, boolean allowMulti) throws Exception {
        return (List<String>) parseAnswerValuesMethod.invoke(service, answerJson, allowMulti);
    }
}
