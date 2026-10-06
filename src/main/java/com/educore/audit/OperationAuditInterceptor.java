package com.educore.audit;

import com.educore.audit.service.OperationAuditService;
import com.educore.common.RequestIdFilter;
import com.educore.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class OperationAuditInterceptor implements HandlerInterceptor {
    private static final Logger log=LoggerFactory.getLogger(OperationAuditInterceptor.class);
    private final OperationAuditService audit;
    public OperationAuditInterceptor(OperationAuditService audit){this.audit=audit;}

    @Override
    public void afterCompletion(HttpServletRequest request,HttpServletResponse response,Object handler,Exception exception) {
        String path=request.getRequestURI();String method=request.getMethod();
        if(path==null||!path.startsWith("/api/")||path.startsWith("/api/auth/")||!isMutation(method))return;
        Authentication authentication=SecurityContextHolder.getContext().getAuthentication();
        if(authentication==null||!(authentication.getPrincipal() instanceof AuthenticatedUser actor))return;
        int status=response.getStatus();String outcome=exception==null&&status<400?"SUCCESS":"FAILED";
        Object requestId=request.getAttribute(RequestIdFilter.ATTRIBUTE);
        String safePath=path.length()>255?path.substring(0,255):path;
        try { audit.record(actor.id(),actor.role().name(),method,safePath,status,outcome,requestId==null?null:requestId.toString()); }
        catch(RuntimeException e){log.error("Could not persist operation audit record for request {}",requestId,e);}
    }

    private boolean isMutation(String method){return "POST".equals(method)||"PUT".equals(method)||"PATCH".equals(method)||"DELETE".equals(method);}
}
