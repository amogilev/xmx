<%@ page language="java" contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jstl/fmt" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>XMX Method Invocation Result</title>

<c:url var="css" value="/css" />
<c:url var="images" value="/images" />

<link href="${css}/main.css" rel="stylesheet" type="text/css" />
<link href="${css}/xmx-icons.css" rel="stylesheet" type="text/css" />

</head>
<body>

<h1>Method invocation</h1>
<table border="2">
    <tr>
        <td><b>Class</b></td>
        <td width="90%"><input style="width: 99%" type="text" readonly="readonly" value="<c:out value="${methodAndResult.className}"/>"/></td>
    </tr>
    <tr>
        <td><b>Method</b></td>
        <td width="90%"><input style="width: 99%" type="text" readonly="readonly" value="<c:out value="${methodAndResult.methodInfo.methodDesc}"/>"/></td>
    </tr>
    <tr>
        <td><b>Target RefPath</b></td>
        <td width="90%"><input style="width: 99%" type="text" readonly="readonly" value="<c:out value="${refpath}"/>"/></td>
    </tr>
</table>


<c:if test="${methodAndResult.exception != null}">
<h2>Result (Exception):</h2>
    <div>
        <c:set var="exception" value="${methodAndResult.exception}"/>
        <pre>${pageContext.out.flush(); exception.printStackTrace(pageContext.response.writer)}</pre>
    </div>
</c:if>
<c:if test="${methodAndResult.exception == null}">
<h2>Result:</h2>
<table border="2">
    <tr>
        <td><b>toString()</b></td>
        <td width="90%"><input style="width: 99%" type="text" readonly="readonly" value="<c:out value="${methodAndResult.result.toStringValue}"/>"/></td>
    </tr>
    <tr>
        <td><b>JSON</b></td>
        <td width="90%" class="supportsTruncationWarning">
            <input style="width: 99%" type="text" readonly="readonly" value="<c:out value="${methodAndResult.result.jsonValue}"/>"/>
            <c:if test="${methodAndResult.result.jsonTruncated}">
                <table class="truncationWarning" title="<fmt:message key='jsonTruncated.tooltip'/>">
                    <tr>
                        <td><img src="${images}/alert.red.png" alt="Warning!"/></td>
                    </tr>
                </table>
            </c:if>
        </td>
    </tr>
</table>
</c:if>

</body>
</html>