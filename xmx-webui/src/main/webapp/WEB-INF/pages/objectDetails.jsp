<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jstl/fmt" %>
<fmt:setBundle basename="messages"/>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>XMX Object Details</title>

<link href="./css/main.css" rel="stylesheet" type="text/css" />

<script type="text/javascript">
	function callSetField(fieldId) {
		var value = document.getElementById("value_" + fieldId).value; 
		window.location = "${pageContext.request.contextPath}/setObjectField?objectId=${objectId}&fieldId=" + fieldId + "&value=" + encodeURIComponent(value);
	}

    function invokeMethod(objId, methodId) {
        var inputs = document.getElementById("m" + methodId).getElementsByTagName("input");
        var form = document.createElement("form");
        form.method = 'POST';
        form.action = 'invokeMethod';
        form.style = 'display: none;';
        form.target = '_blank';
        addFormData(form, 'objectId', objId);
        addFormData(form, 'methodId', methodId);
        for (var i = 0; i < inputs.length; i++) {
            var argInput = inputs[i];
            addFormData(form, 'arg', argInput.value);
        }
        document.body.appendChild(form);
        form.submit();
    }

    function addFormData(form, name, value) {
        var input = document.createElement('input');
        input.type = "text";
        input.name = name;
        input.value = value;
        form.appendChild(input);
    }

    function loadFullJson(fieldId) {
	    var url = "${pageContext.request.contextPath}/getFullJson?objectId=${objectId}" +
            (fieldId === undefined ? "" : "&fieldId=" + fieldId);
        window.open(url, '_blank');
    }

</script>
</head>

<h1>Details of object #${objectId} (${className})</h1>

<table border="2">
    <tr>
        <td><b>toString()</b></td>
        <td width="90%"><input style="width: 99%" type="text" readonly="readonly" value="<c:out value="${details.text.toStringValue}"/>"/></td>
    </tr>
    <tr>
        <td><b>JSON</b></td>
        <td width="90%"><input style="width: 99%" type="text" readonly="readonly" value="<c:out value="${details.text.jsonValue}"/>"/>
            <c:if test="${details.text.jsonTruncated}">
                <span class="jsonTruncatedWarning" title="<fmt:message key='jsonTruncated.tooltip'/>">
                    <img src="./images/alert.red.png" alt="Warning!">
                    <input type="button" onclick="loadFullJson();" value="<fmt:message key='jsonTruncated.loadFull'/>" >
                </span>
            </c:if>
        </td>
    </tr>
</table>

<h2>Fields</h2>

<table border="2">
    <thead>
    <tr>
        <td>Name</td>
        <td>Value</td>
        <td></td>
    </tr>
    </thead>
    
<c:forEach items="${details.fieldsByClass}" var="entry">
    <tr>
        <td colspan="3"><b>${entry.key}</b></td> <%--className--%>
    </tr>
  <c:forEach items="${entry.value}" var="fieldInfo">
    <tr>
        <td>${fieldInfo.name}</td>
        <td><input type="text" id="value_${fieldInfo.id}" value="${fn:escapeXml(fieldInfo.text.smartTextValue)}"/></td>
        <td>
            <input type="button" onclick="callSetField(${fieldInfo.id});" value="Set">
            <c:if test="${!fieldInfo.text.toStringDeclared && fieldInfo.text.jsonTruncated}">
                <span class="jsonTruncatedWarning" title="<fmt:message key='jsonTruncated.tooltip'/>">
                    <img src="./images/alert.red.png" alt="Warning!">
                    <input type="button" onclick="loadFullJson(${fieldInfo.id});" value="<fmt:message key='jsonTruncated.loadFull'/>" >
                </span>
            </c:if>
        </td>
        </td>
    </tr>
  </c:forEach>
</c:forEach>
</table>

<h2>Methods</h2>
<table border="2">
    <thead>
    <tr>
        <td>Name</td>
        <td>Signature</td>
        <td></td>
    </tr>
    </thead>
    
<c:forEach items="${details.methodsByClass}" var="entry">
    <tr>
        <td colspan="3"><b>${entry.key}</b></td> <%--className--%>
    </tr>
  <c:forEach items="${entry.value}" var="methodInfo">
    <tr>
        <td>${methodInfo.name}</td>
        <td id="m${methodInfo.id}">
            ${methodInfo.nameTypeSignature}(
            <c:forEach items="${methodInfo.parameters}" var="p" varStatus="status">
                <input type="text" id="m${methodInfo.id}_p${status.index}" value="${p}"/>
                <c:if test="${not status.last}">,</c:if>
            </c:forEach>
            )
        </td>
        <td>
            <input type="button" onclick="invokeMethod(${objectId}, ${methodInfo.id});" value="Invoke"/>
        </td>
    </tr>
  </c:forEach>
</c:forEach>
</table>

<form id="frmInvMethod" action="/invokeMethod" method="post" style="display: none;">
    <input type="text" name="objectId" value="18"/>
    <input type="text" name="methodId" value="1"/>
    <input type="text" name="args" value="123"/>
    <input type="submit" value="Submit"/>
</form>

</body>
</html>