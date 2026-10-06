package com.educore.common;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.educore.common.web.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class RequestLoggingTest {
    @Test void completionLogCarriesResponseIdAndExcludesSensitiveRequestData() throws Exception {
        Logger logger=(Logger)LoggerFactory.getLogger(RequestIdFilter.class);
        ListAppender<ILoggingEvent> capture=new ListAppender<>() {
            @Override protected void append(ILoggingEvent event) { event.prepareForDeferredProcessing(); super.append(event); }
        };
        capture.start();logger.addAppender(capture);
        try {
            var request=new MockHttpServletRequest("POST","/api/test");
            request.addHeader("X-Request-Id","test-request-42");
            request.addHeader("Authorization","Bearer secret-token");
            request.setQueryString("password=secret-password");
            request.setContent("private-body".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            var response=new MockHttpServletResponse();
            new RequestIdFilter().doFilter(request,response,(r,s)->{
                assertEquals("test-request-42",MDC.get("requestId"));
                ((jakarta.servlet.http.HttpServletResponse)s).setStatus(409);
            });
            assertEquals("test-request-42",response.getHeader("X-Request-Id"));
            assertEquals("test-request-42",request.getAttribute("requestId"));
            assertNull(MDC.get("requestId"));
            var event=capture.list.getFirst();
            assertEquals("test-request-42",event.getMDCPropertyMap().get("requestId"));
            assertTrue(event.getFormattedMessage().contains("POST /api/test status=409"));
            assertFalse(event.getFormattedMessage().contains("secret"));
            assertFalse(event.getFormattedMessage().contains("private-body"));
        } finally {logger.detachAppender(capture);capture.stop();MDC.clear();}
    }

    @Test void invalidCallerIdIsReplacedAndContextIsClearedEvenOnFailure() {
        var request=new MockHttpServletRequest("GET","/api/test");
        request.addHeader("X-Request-Id","bad\r\nid");
        var response=new MockHttpServletResponse();
        assertThrows(IllegalStateException.class,()->new RequestIdFilter().doFilter(request,response,(r,s)->{throw new IllegalStateException("test failure");}));
        assertDoesNotThrow(()->UUID.fromString(response.getHeader("X-Request-Id")));
        assertNull(MDC.get("requestId"));
    }

    @Test void unexpectedErrorLogsStackButKeepsResponseGeneric() {
        Logger logger=(Logger)LoggerFactory.getLogger(GlobalExceptionHandler.class);
        ListAppender<ILoggingEvent> capture=new ListAppender<>();capture.start();logger.addAppender(capture);
        try {
            var request=new MockHttpServletRequest("GET","/api/test");request.setAttribute("requestId","failure-42");
            var result=new GlobalExceptionHandler().unexpected(new IllegalStateException("test stack marker"),request);
            assertEquals(500,result.getStatusCode().value());
            assertEquals("系统内部错误",result.getBody().message());
            assertEquals("failure-42",result.getBody().requestId());
            assertNotNull(capture.list.getFirst().getThrowableProxy());
            assertEquals("test stack marker",capture.list.getFirst().getThrowableProxy().getMessage());
        } finally {logger.detachAppender(capture);capture.stop();}
    }
}
